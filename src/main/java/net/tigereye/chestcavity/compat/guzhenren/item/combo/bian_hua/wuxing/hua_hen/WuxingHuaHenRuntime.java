package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.hua_hen;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.hua_hen.state.WuxingHuaHenAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.hua_hen.state.WuxingHuaHenAttachment.Element;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.hua_hen.state.WuxingHuaHenAttachment.Mode;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.util.YinYangDualityOps;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.framework.ComboSkillContext;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.framework.ComboSkillMessenger;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.DaoHenResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.skill.ComboSkillRegistry;
import net.tigereye.chestcavity.skill.ComboSkillRegistry;
import net.tigereye.chestcavity.skill.ComboSkillRegistry.ComboSkillEntry;
import net.tigereye.chestcavity.skill.ComboSkillRegistry.OrganCheckResult;

/**
 * 五行化痕 核心逻辑。
 */
public final class WuxingHuaHenRuntime {

  private WuxingHuaHenRuntime() {}

  public static void handleSlowTick(ServerPlayer player, long now) {
    WuxingHuaHenAttachment attachment = CCAttachments.getWuxingHuaHen(player);
    WuxingHuaHenAttachment.UndoSnapshot snapshot = attachment.undoSnapshot();
    if (!snapshot.isValid(now)) {
      return;
    }

    long remainingTicks = snapshot.expireTick() - now;
    if (remainingTicks <= 0) {
      snapshot.clear();
      return;
    }

    if (remainingTicks <= WuxingHuaHenTuning.UNDO_WARNING_TICKS
        && remainingTicks > WuxingHuaHenTuning.UNDO_WARNING_TICKS - 20) {
      long remainingSeconds = remainingTicks / 20L;
      WuxingHuaHenMessages.warnUndoExpiry(player, snapshot.element(), remainingSeconds);
    }
  }

  public static void activateTransmute(ComboSkillContext context, ResourceLocation skillId) {
    ServerPlayer player = context.player();
    long now = context.gameTime();

    Optional<ComboSkillEntry> entryOpt = ComboSkillRegistry.get(skillId);
    if (entryOpt.isPresent()) {
      OrganCheckResult result = ComboSkillRegistry.checkOrgans(player, entryOpt.get());
      if (!result.canActivate()) {
        WuxingHuaHenMessages.sendFailure(
            player, "缺少必需器官：" + formatMissingOrgans(result.missingOrgans()));
        return;
      }
    }

    Optional<YinYangDualityAttachment> yinyangOpt = YinYangDualityOps.get(player);
    if (yinyangOpt.isEmpty()) {
      WuxingHuaHenMessages.sendFailure(player, "未找到阴阳转身蛊数据。");
      return;
    }
    YinYangDualityAttachment yinyang = yinyangOpt.get();

    long readyAt = yinyang.getCooldown(skillId);
    if (readyAt > now) {
      WuxingHuaHenMessages.sendFailure(player, "技能冷却中...");
      return;
    }
    if (yinyang.sealEndTick() > now) {
      WuxingHuaHenMessages.sendFailure(player, "封印期间无法施展五行化痕。");
      return;
    }

    WuxingHuaHenAttachment attachment = CCAttachments.getWuxingHuaHen(player);
    Optional<ResourceHandle> handleOpt = context.openResources();
    if (handleOpt.isEmpty()) {
      WuxingHuaHenMessages.sendFailure(player, "无法读取资源。");
      return;
    }
    ResourceHandle handle = handleOpt.get();

    if (!WuxingHuaHenCostService.tryConsumeBaseCost(handle)) {
      WuxingHuaHenMessages.sendFailure(player, "资源不足以支付技能消耗。");
      return;
    }

    double bianhuaAmount = DaoHenResourceOps.get(handle, WuxingHuaHenTuning.BIANHUA_DAO_KEY);
    if (bianhuaAmount <= 0.0) {
      WuxingHuaHenMessages.sendFailure(player, "变化道道痕不足。");
      WuxingHuaHenCostService.refundBaseCost(handle);
      return;
    }

    Mode mode = attachment.lastMode();
    double amountReq =
        WuxingHuaHenCalculator.resolveAmount(mode, attachment.lastFixedAmount(), bianhuaAmount);
    if (amountReq <= 0.0 || amountReq > WuxingHuaHenTuning.SINGLE_CAST_CAP) {
      WuxingHuaHenMessages.sendFailure(
          player, "转化数量无效（范围: 0~" + WuxingHuaHenTuning.SINGLE_CAST_CAP + "）。");
      WuxingHuaHenCostService.refundBaseCost(handle);
      return;
    }
    amountReq = Math.min(amountReq, bianhuaAmount);

    Element element = attachment.lastElement();
    double tax = WuxingHuaHenCalculator.calculateTax(element, yinyang.currentMode());
    double amountOut = WuxingHuaHenCalculator.applyTax(amountReq, tax);
    if (amountOut <= 0.0) {
      WuxingHuaHenMessages.sendFailure(player, "损耗过高，无有效产出。");
      WuxingHuaHenCostService.refundBaseCost(handle);
      return;
    }

    DaoHenResourceOps.consume(handle, WuxingHuaHenTuning.BIANHUA_DAO_KEY, amountReq);
    DaoHenResourceOps.add(handle, element.poolKey(), amountOut);

    long cooldown = WuxingHuaHenTuning.BASE_COOLDOWN_TICKS;
    if (amountReq > WuxingHuaHenTuning.LARGE_AMOUNT_THRESHOLD) {
      cooldown += WuxingHuaHenTuning.LARGE_AMOUNT_COOLDOWN_BONUS;
    }
    long readyTick = now + cooldown;
    yinyang.setCooldown(skillId, readyTick);
    ComboSkillRegistry.scheduleReadyToast(player, skillId, readyTick, now);

    long undoExpireTick = now + WuxingHuaHenTuning.UNDO_WINDOW_TICKS;
    attachment.undoSnapshot().set(element, amountReq, amountOut, undoExpireTick);

    long undoSeconds = WuxingHuaHenTuning.UNDO_WINDOW_TICKS / 20L;
    WuxingHuaHenMessages.sendTransmuteSuccess(player, element, amountReq, amountOut, tax, undoSeconds);
    WuxingHuaHenFx.playTransmute(player, element);
  }

  public static void activateUndo(ComboSkillContext context) {
    ServerPlayer player = context.player();
    long now = context.gameTime();

    WuxingHuaHenAttachment attachment = CCAttachments.getWuxingHuaHen(player);
    WuxingHuaHenAttachment.UndoSnapshot snapshot = attachment.undoSnapshot();
    if (!snapshot.isValid(now)) {
      if (snapshot.expireTick() > 0 && now >= snapshot.expireTick()) {
        WuxingHuaHenMessages.sendUndoExpired(player);
      } else {
        WuxingHuaHenMessages.sendUndoMissing(player);
      }
      return;
    }

    Optional<ResourceHandle> handleOpt = ResourceOps.openHandle(player);
    if (handleOpt.isEmpty()) {
      WuxingHuaHenMessages.sendFailure(player, "无法读取资源。");
      return;
    }
    ResourceHandle handle = handleOpt.get();

    Element element = snapshot.element();
    double amountOut = snapshot.amountOut();
    double returnAmount = WuxingHuaHenCalculator.computeUndoReturn(amountOut);

    double currentElement = DaoHenResourceOps.get(handle, element.poolKey());
    if (currentElement < amountOut) {
      WuxingHuaHenMessages.sendUndoInsufficient(player, amountOut, currentElement);
      return;
    }

    DaoHenResourceOps.consume(handle, element.poolKey(), amountOut);
    DaoHenResourceOps.add(handle, WuxingHuaHenTuning.BIANHUA_DAO_KEY, returnAmount);
    long remainingSeconds = Math.max(0L, (snapshot.expireTick() - now) / 20L);
    snapshot.clear();
    WuxingHuaHenMessages.sendUndoSuccess(player, element, amountOut, returnAmount, remainingSeconds);
    WuxingHuaHenFx.playUndo(player, element);
  }

  public static void activateCheck(ComboSkillContext context) {
    ServerPlayer player = context.player();
    long now = context.gameTime();

    WuxingHuaHenAttachment attachment = CCAttachments.getWuxingHuaHen(player);
    WuxingHuaHenAttachment.UndoSnapshot snapshot = attachment.undoSnapshot();
    if (!snapshot.isValid(now)) {
      WuxingHuaHenMessages.sendNoUndoStatus(player);
      return;
    }

    long remainingSeconds = Math.max(0L, (snapshot.expireTick() - now) / 20L);
    double returnAmount = WuxingHuaHenCalculator.computeUndoReturn(snapshot.amountOut());
    WuxingHuaHenMessages.sendUndoStatus(
        player, snapshot.element(), snapshot.amountOut(), returnAmount, remainingSeconds);
  }

  public static void openConfig(ComboSkillContext context) {
    ServerPlayer player = context.player();
    WuxingHuaHenAttachment attachment = CCAttachments.getWuxingHuaHen(player);
    Element currentElement = attachment.lastElement();
    Mode currentMode = attachment.lastMode();

    WuxingHuaHenMessages.sendConfigHeader(player, currentElement, currentMode);
    ComboSkillMessenger.sendSystem(player, "§a【选择目标元素】");
    ComboSkillMessenger.send(player, WuxingHuaHenMessages.buildElementOptions(currentElement));
    ComboSkillMessenger.sendSystem(player, "");
    ComboSkillMessenger.sendSystem(player, "§a【选择转化模式】");
    ComboSkillMessenger.send(player, WuxingHuaHenMessages.buildRatioModes(currentMode));
    ComboSkillMessenger.send(player, WuxingHuaHenMessages.buildFixedModes(currentMode));
    WuxingHuaHenMessages.sendConfigFooter(player);
  }

  private static String formatMissingOrgans(List<ResourceLocation> organs) {
    return organs.stream().map(ResourceLocation::toString).collect(Collectors.joining("、"));
  }
}
