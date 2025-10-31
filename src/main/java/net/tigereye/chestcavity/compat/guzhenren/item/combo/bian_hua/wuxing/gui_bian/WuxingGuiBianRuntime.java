package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.gui_bian;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.hua_hen.state.WuxingHuaHenAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityOps;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.framework.ComboSkillContext;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.framework.ComboSkillMessenger;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.gui_bian.state.WuxingGuiBianAttachment;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.DaoHenResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.skill.ComboSkillRegistry;
import net.tigereye.chestcavity.skill.ComboSkillRegistry;
import net.tigereye.chestcavity.skill.ComboSkillRegistry.ComboSkillEntry;
import net.tigereye.chestcavity.skill.ComboSkillRegistry.OrganCheckResult;

/**
 * 五行归变·逆转 核心逻辑。
 *
 * <p>拆分出纯逻辑，便于单元测试与后续 Combo 扩展复用。
 */
public final class WuxingGuiBianRuntime {

  private WuxingGuiBianRuntime() {}

  public static void handleSlowTick(ServerPlayer player, long now) {
    WuxingGuiBianAttachment attachment = CCAttachments.getWuxingGuiBian(player);
    WuxingGuiBianAttachment.FreezeSnapshot snapshot = attachment.freezeSnapshot();

    if (!snapshot.isValid(now)) {
      return;
    }

    long remainingTicks = snapshot.expireTick() - now;
    if (remainingTicks <= 0) {
      autoReturnTemporaryConversion(player, snapshot);
      return;
    }

    if (remainingTicks <= WuxingGuiBianTuning.FREEZE_WARNING_TICKS
        && remainingTicks > WuxingGuiBianTuning.FREEZE_WARNING_TICKS - 20) {
      long remainingSeconds = remainingTicks / 20L;
      WuxingGuiBianMessages.sendPendingWarning(
          player, snapshot.element(), remainingSeconds);
    }
  }

  public static void activate(ComboSkillContext context, ResourceLocation skillId) {
    ServerPlayer player = context.player();
    long now = context.gameTime();

    Optional<ComboSkillEntry> entryOpt = ComboSkillRegistry.get(skillId);
    if (entryOpt.isEmpty()) {
      ComboSkillMessenger.sendFailure(player, "杀招未注册。");
      return;
    }
    ComboSkillEntry entry = entryOpt.get();

    OrganCheckResult organCheck = ComboSkillRegistry.checkOrgans(player, entry);
    if (!organCheck.canActivate()) {
      ComboSkillMessenger.sendFailure(
          player, "缺少必需器官：" + formatMissingOrgans(organCheck.missingOrgans()));
      return;
    }

    Optional<YinYangDualityAttachment> yinyangOpt = YinYangDualityOps.get(player);
    if (yinyangOpt.isEmpty()) {
      ComboSkillMessenger.sendFailure(player, "未找到阴阳转身蛊数据。");
      return;
    }
    YinYangDualityAttachment yinyang = yinyangOpt.get();

    long readyAt = yinyang.getCooldown(skillId);
    if (readyAt > now) {
      ComboSkillMessenger.sendFailure(player, "技能冷却中...");
      return;
    }

    if (yinyang.sealEndTick() > now) {
      ComboSkillMessenger.sendFailure(player, "封印期间无法施展五行归变·逆转。");
      return;
    }

    WuxingGuiBianAttachment attachment = CCAttachments.getWuxingGuiBian(player);
    WuxingHuaHenAttachment wuxingConfig = CCAttachments.getWuxingHuaHen(player);

    Optional<ResourceHandle> handleOpt = context.openResources();
    if (handleOpt.isEmpty()) {
      ComboSkillMessenger.sendFailure(player, "无法读取资源。");
      return;
    }
    ResourceHandle handle = handleOpt.get();

    if (!WuxingGuiBianCostService.tryConsumeBaseCost(handle)) {
      ComboSkillMessenger.sendFailure(player, "资源不足以支付技能消耗。");
      return;
    }

    WuxingHuaHenAttachment.Element element = wuxingConfig.lastElement();
    WuxingHuaHenAttachment.Mode mode = wuxingConfig.lastMode();

    double elementAmount = DaoHenResourceOps.get(handle, element.poolKey());
    if (elementAmount <= 0.0) {
      ComboSkillMessenger.sendFailure(player, WuxingGuiBianMessages.getElementName(element) + "道痕不足。");
      WuxingGuiBianCostService.refundBaseCost(handle);
      return;
    }

    double amountReq =
        WuxingGuiBianCalculator.resolveAmount(mode, wuxingConfig.lastFixedAmount(), elementAmount);
    if (amountReq <= 0.0 || amountReq > WuxingGuiBianTuning.SINGLE_CAST_CAP) {
      ComboSkillMessenger.sendFailure(
          player, "转化数量无效（范围: 0~" + WuxingGuiBianTuning.SINGLE_CAST_CAP + "）。");
      WuxingGuiBianCostService.refundBaseCost(handle);
      return;
    }
    amountReq = Math.min(amountReq, elementAmount);

    int anchorCount = ComboSkillRegistry.countEquippedOrgans(player, WuxingGuiBianTuning.ELEMENT_ANCHORS);
    double tax =
        WuxingGuiBianCalculator.calculateTax(
            element, yinyang.currentMode(), anchorCount, attachment.lastMode() == WuxingGuiBianAttachment.ConversionMode.TEMPORARY);
    double amountOut = WuxingGuiBianCalculator.applyTax(amountReq, tax);

    if (amountOut <= 0.0) {
      ComboSkillMessenger.sendFailure(player, "损耗过高，无有效产出。");
      WuxingGuiBianCostService.refundBaseCost(handle);
      return;
    }

    DaoHenResourceOps.consume(handle, element.poolKey(), amountReq);
    DaoHenResourceOps.add(handle, WuxingGuiBianTuning.BIANHUA_DAO_KEY, amountOut);

    long cooldown = WuxingGuiBianTuning.BASE_COOLDOWN_TICKS;
    if (amountReq > WuxingGuiBianTuning.LARGE_AMOUNT_THRESHOLD) {
      cooldown += WuxingGuiBianTuning.LARGE_AMOUNT_COOLDOWN_BONUS;
    }
    long readyTick = now + cooldown;
    yinyang.setCooldown(skillId, readyTick);
    ComboSkillRegistry.scheduleReadyToast(player, skillId, readyTick, now);

    if (attachment.lastMode() == WuxingGuiBianAttachment.ConversionMode.TEMPORARY) {
      long expireTick = now + WuxingGuiBianTuning.TEMPORARY_FREEZE_DURATION;
      attachment.freezeSnapshot().set(element, amountReq, amountOut, expireTick);
      long freezeSeconds = WuxingGuiBianTuning.TEMPORARY_FREEZE_DURATION / 20L;
      WuxingGuiBianMessages.sendSuccessConversion(
          player, element, amountReq, amountOut, tax, anchorCount, true, freezeSeconds);
    } else {
      WuxingGuiBianMessages.sendSuccessConversion(
          player, element, amountReq, amountOut, tax, anchorCount, false, 0L);
    }

    WuxingGuiBianFx.playConversion(player, element);
  }

  public static void openConfig(ComboSkillContext context) {
    ServerPlayer player = context.player();
    long now = context.gameTime();

    WuxingGuiBianAttachment attachment = CCAttachments.getWuxingGuiBian(player);
    WuxingGuiBianAttachment.ConversionMode currentMode = attachment.lastMode();
    WuxingGuiBianAttachment.FreezeSnapshot snapshot = attachment.freezeSnapshot();

    WuxingGuiBianMessages.sendConfigHeader(player, currentMode);

    if (snapshot.isValid(now)) {
      long remainingSeconds = Math.max(0L, (snapshot.expireTick() - now) / 20L);
      WuxingGuiBianMessages.sendFreezeDetails(player, snapshot, remainingSeconds);
    } else {
      ComboSkillMessenger.sendSystem(player, "§7当前无暂时模式冻结记录。");
    }

    ComboSkillMessenger.sendSystem(player, "");
    ComboSkillMessenger.sendSystem(player, "§a【选择转化模式】");

    MutableComponent modeLine = Component.literal("");
    modeLine.append(
        WuxingGuiBianMessages.buildConversionOptionComponent(
            WuxingGuiBianAttachment.ConversionMode.TEMPORARY, currentMode));
    modeLine.append(Component.literal(" §8| "));
    modeLine.append(
        WuxingGuiBianMessages.buildConversionOptionComponent(
            WuxingGuiBianAttachment.ConversionMode.PERMANENT, currentMode));
    ComboSkillMessenger.send(player, modeLine);

    ComboSkillMessenger.sendSystem(player, "");
    WuxingGuiBianMessages.sendConfigFooter(player);
  }

  private static void autoReturnTemporaryConversion(
      ServerPlayer player, WuxingGuiBianAttachment.FreezeSnapshot snapshot) {
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      snapshot.clear();
      return;
    }
    ResourceHandle handle = handleOpt.get();

    double amountOut = snapshot.amountOut();
    double currentBianhua =
        DaoHenResourceOps.get(handle, WuxingGuiBianTuning.BIANHUA_DAO_KEY);
    if (currentBianhua < amountOut) {
      WuxingGuiBianMessages.sendTempReturnFailure(player, amountOut, currentBianhua);
      snapshot.clear();
      return;
    }

    DaoHenResourceOps.consume(handle, WuxingGuiBianTuning.BIANHUA_DAO_KEY, amountOut);
    DaoHenResourceOps.add(handle, snapshot.element().poolKey(), snapshot.amountConsumed());

    WuxingGuiBianMessages.sendTempReturnSuccess(
        player, snapshot.element(), amountOut, snapshot.amountConsumed());
    WuxingGuiBianFx.playReturn(player, snapshot.element());
    snapshot.clear();
  }

  private static String formatMissingOrgans(List<ResourceLocation> missing) {
    return missing.stream().map(ResourceLocation::toString).collect(Collectors.joining("、"));
  }
}
