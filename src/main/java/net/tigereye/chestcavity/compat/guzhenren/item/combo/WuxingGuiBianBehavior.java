package net.tigereye.chestcavity.compat.guzhenren.item.combo;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.WuxingHuaHenAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.util.YinYangDualityOps;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.state.WuxingGuiBianAttachment;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.DaoHenResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.skill.ComboSkillRegistry;

/**
 * 五行归变·逆转（组合杀招）
 * 将五行道痕（金/木/水/炎/土）逆转回变化道
 *
 * 必需器官：虹变蛊 + 阴阳转身蛊
 * 元素锚（任意一个）：金肺蛊/木肝蛊/水肾蛊/火心蛊/土脾蛊
 *
 * 锚点越多，税率越低：
 * - 1个锚：8%税
 * - 2个锚：6%税
 * - 3个锚：4%税
 * - 4个锚：2%税
 * - 5个锚：0%税（免税）
 */
public final class WuxingGuiBianBehavior implements OrganSlowTickListener {

  public static final WuxingGuiBianBehavior INSTANCE = new WuxingGuiBianBehavior();

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation SKILL_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "wuxing_gui_bian");
  private static final ResourceLocation SKILL_CONFIG_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "wuxing_gui_bian_config");

  private static final long BASE_COOLDOWN_TICKS = 30L * 20L; // 30秒
  private static final long LARGE_AMOUNT_COOLDOWN_BONUS = 10L * 20L; // 大量转化+10秒
  private static final long TEMPORARY_FREEZE_DURATION = 20L * 20L; // 暂时模式20秒
  private static final long FREEZE_WARNING_TICKS = 5L * 20L; // 剩余5秒时警告

  private static final double BASE_TAX = 0.08; // 基础税8%（1个锚）
  private static final double TAX_REDUCTION_PER_ANCHOR = 0.02; // 每个额外锚-2%
  private static final double YINYANG_TAX_REDUCTION = 0.02; // 阴阳联动-2%
  private static final double SINGLE_CAST_CAP = 120.0; // 单次上限120
  private static final double LARGE_AMOUNT_THRESHOLD = 100.0; // 大量阈值100

  private static final String BIANHUA_DAO_KEY = "daohen_bianhuadao";

  // BaseCost at rank 1, stage 1
  private static final double COST_ZHENYUAN = 180.0;
  private static final double COST_NIANTOU = 6.0;
  private static final double COST_HUNPO = 3.0;
  private static final double COST_JINGLI = 6.0;

  // 五行元素锚列表
  private static final List<ResourceLocation> ELEMENT_ANCHORS =
      List.of(
          ResourceLocation.fromNamespaceAndPath(MOD_ID, "jinfeigu"),    // 金肺蛊
          ResourceLocation.fromNamespaceAndPath(MOD_ID, "mugangu"),     // 木肝蛊
          ResourceLocation.fromNamespaceAndPath(MOD_ID, "shuishengu"),  // 水肾蛊
          ResourceLocation.fromNamespaceAndPath(MOD_ID, "huoxingu"),    // 火心蛊
          ResourceLocation.fromNamespaceAndPath(MOD_ID, "tupigu"));     // 土脾蛊

  static {
    OrganActivationListeners.register(
        SKILL_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateCombo(player, cc);
          }
        });
    OrganActivationListeners.register(
        SKILL_CONFIG_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateConfig(player, cc);
          }
        });
  }

  private WuxingGuiBianBehavior() {}

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof ServerPlayer player)) {
      return;
    }
    if (player.level().isClientSide()) {
      return;
    }

    // 检查冻结窗口是否即将过期（暂时模式）
    WuxingGuiBianAttachment attachment = getOrCreateAttachment(player);
    WuxingGuiBianAttachment.FreezeSnapshot snapshot = attachment.freezeSnapshot();

    long now = player.level().getGameTime();
    if (!snapshot.isValid(now)) {
      return;
    }

    long remainingTicks = snapshot.expireTick() - now;

    // 在剩余5秒时警告
    if (remainingTicks > 0
        && remainingTicks <= FREEZE_WARNING_TICKS
        && remainingTicks > FREEZE_WARNING_TICKS - 20) {
      long remainingSeconds = remainingTicks / 20L;
      WuxingHuaHenAttachment.Element element = snapshot.element();
      player.displayClientMessage(
          Component.literal(
              String.format(
                  "§e⚠ 警告：§b%s §f→ 变化道 暂时转化将在 §c%d秒 §f后自动返还！",
                  getElementName(element), remainingSeconds)),
          true);
    }

    // 自动返还（时间到期）
    if (remainingTicks <= 0) {
      autoReturnTemporaryConversion(player, snapshot);
    }
  }

  private void activateCombo(ServerPlayer player, ChestCavityInstance cc) {
    if (player.level().isClientSide() || cc == null) {
      return;
    }

    long now = player.level().getGameTime();

    // 检查器官条件
    Optional<ComboSkillRegistry.ComboSkillEntry> entryOpt = ComboSkillRegistry.get(SKILL_ID);
    if (entryOpt.isEmpty()) {
      sendFailure(player, "杀招未注册。");
      return;
    }
    ComboSkillRegistry.ComboSkillEntry entry = entryOpt.get();
    ComboSkillRegistry.OrganCheckResult organCheck = ComboSkillRegistry.checkOrgans(player, entry);
    if (!organCheck.canActivate()) {
      sendFailure(player, "缺少必需器官：" + formatMissingOrgans(organCheck.missingOrgans()));
      return;
    }

    // 检查冷却
    Optional<YinYangDualityAttachment> yinyangOpt = YinYangDualityOps.get(player);
    if (yinyangOpt.isEmpty()) {
      sendFailure(player, "未找到阴阳转身蛊数据。");
      return;
    }
    YinYangDualityAttachment yinyang = yinyangOpt.get();
    long readyAt = yinyang.getCooldown(SKILL_ID);
    if (readyAt > now) {
      sendFailure(player, "技能冷却中...");
      return;
    }

    // 检查封印
    if (yinyang.sealEndTick() > now) {
      sendFailure(player, "封印期间无法施展五行归变·逆转。");
      return;
    }

    // 获取附加数据
    WuxingGuiBianAttachment attachment = getOrCreateAttachment(player);
    WuxingHuaHenAttachment wuxingConfig = CCAttachments.getWuxingHuaHen(player);

    // 获取资源句柄
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      sendFailure(player, "无法读取资源。");
      return;
    }
    ResourceHandle handle = handleOpt.get();

    // 检查并扣除BaseCost
    if (!consumeBaseCost(player, handle)) {
      sendFailure(player, "资源不足以支付技能消耗。");
      return;
    }

    // 获取目标元素与数量
    WuxingHuaHenAttachment.Element element = wuxingConfig.lastElement();
    WuxingHuaHenAttachment.Mode mode = wuxingConfig.lastMode();
    double elementAmount = DaoHenResourceOps.get(handle, element.poolKey());
    if (elementAmount <= 0.0) {
      sendFailure(player, getElementName(element) + "道痕不足。");
      // 返还BaseCost
      returnBaseCost(handle);
      return;
    }

    // 解析转化数量
    double amountReq = resolveAmount(mode, wuxingConfig.lastFixedAmount(), elementAmount);
    if (amountReq <= 0.0 || amountReq > SINGLE_CAST_CAP) {
      sendFailure(player, "转化数量无效（范围: 0~" + SINGLE_CAST_CAP + "）。");
      returnBaseCost(handle);
      return;
    }
    amountReq = Math.min(amountReq, elementAmount);

    // 计算税率（锚点越多越低）
    int anchorCount = ComboSkillRegistry.countEquippedOrgans(player, ELEMENT_ANCHORS);
    double tax = calculateTax(element, yinyang.currentMode(), anchorCount, attachment.lastMode());
    double amountOut = Math.floor(amountReq * (1.0 - tax));

    if (amountOut <= 0.0) {
      sendFailure(player, "损耗过高，无有效产出。");
      returnBaseCost(handle);
      return;
    }

    // 执行转化
    DaoHenResourceOps.consume(handle, element.poolKey(), amountReq);
    DaoHenResourceOps.add(handle, BIANHUA_DAO_KEY, amountOut);

    // 设置冷却
    long cooldown = BASE_COOLDOWN_TICKS;
    if (amountReq > LARGE_AMOUNT_THRESHOLD) {
      cooldown += LARGE_AMOUNT_COOLDOWN_BONUS;
    }
    long readyTick = now + cooldown;
    yinyang.setCooldown(SKILL_ID, readyTick);
    ActiveSkillRegistry.scheduleReadyToast(player, SKILL_ID, readyTick, now);

    // 暂时模式：记录冻结快照
    if (attachment.lastMode() == WuxingGuiBianAttachment.ConversionMode.TEMPORARY) {
      long expireTick = now + TEMPORARY_FREEZE_DURATION;
      attachment.freezeSnapshot().set(element, amountReq, amountOut, expireTick);
      long freezeSeconds = TEMPORARY_FREEZE_DURATION / 20L;
      sendSuccess(
          player,
          String.format(
              "§a五行归变·逆转：§b%s §e%.1f §f→ 变化道 §e%.1f §7(税率 %.1f%%, %d锚) §8[暂时模式: %d秒后返还]",
              getElementName(element), amountReq, amountOut, tax * 100, anchorCount, freezeSeconds));
    } else {
      // 永久模式
      sendSuccess(
          player,
          String.format(
              "§a五行归变·逆转：§b%s §e%.1f §f→ 变化道 §e%.1f §7(税率 %.1f%%, %d锚) §6[永久模式]",
              getElementName(element), amountReq, amountOut, tax * 100, anchorCount));
    }

    // FX粒子效果
    playConversionFx(player, element);
  }

  private void activateConfig(ServerPlayer player, ChestCavityInstance cc) {
    if (player.level().isClientSide() || cc == null) {
      return;
    }

    long now = player.level().getGameTime();
    WuxingGuiBianAttachment attachment = getOrCreateAttachment(player);
    WuxingGuiBianAttachment.ConversionMode currentMode = attachment.lastMode();
    WuxingGuiBianAttachment.FreezeSnapshot snapshot = attachment.freezeSnapshot();

    player.sendSystemMessage(Component.literal("§6═══════ 五行归变·逆转 配置 ═══════"));
    player.sendSystemMessage(
        Component.literal("§f当前转化模式：§b" + getConversionModeName(currentMode)));

    if (snapshot.isValid(now)) {
      long remainingSeconds = Math.max(0L, (snapshot.expireTick() - now) / 20L);
      player.sendSystemMessage(
          Component.literal(
              String.format(
                  "§d暂时模式冻结：§b%s §e%.1f §7→ 变化道 §e%.1f §7(剩余 §c%d秒§7)",
                  getElementName(snapshot.element()),
                  snapshot.amountConsumed(),
                  snapshot.amountOut(),
                  remainingSeconds)));
    } else {
      player.sendSystemMessage(Component.literal("§7当前无暂时模式冻结记录。"));
    }

    player.sendSystemMessage(Component.literal(""));
    player.sendSystemMessage(Component.literal("§a【选择转化模式】"));

    MutableComponent modeLine = Component.literal("");
    modeLine.append(
        createConversionOption(
            WuxingGuiBianAttachment.ConversionMode.TEMPORARY, currentMode));
    modeLine.append(Component.literal(" §8| "));
    modeLine.append(
        createConversionOption(
            WuxingGuiBianAttachment.ConversionMode.PERMANENT, currentMode));
    player.sendSystemMessage(modeLine);

    player.sendSystemMessage(Component.literal(""));
    player.sendSystemMessage(
        Component.literal("§7元素与数量沿用 §a五行化痕·配置§7，可通过该技能调整。"));
    player.sendSystemMessage(Component.literal("§6═══════════════════════════"));
  }

  private double resolveAmount(WuxingHuaHenAttachment.Mode mode, int fixedAmount, double available) {
    return switch (mode) {
      case LAST -> fixedAmount;
      case ALL -> Math.min(available, SINGLE_CAST_CAP);
      case RATIO_25 -> Math.min(available * 0.25, SINGLE_CAST_CAP);
      case RATIO_50 -> Math.min(available * 0.50, SINGLE_CAST_CAP);
      case RATIO_100 -> Math.min(available, SINGLE_CAST_CAP);
      case FIXED_10 -> 10.0;
      case FIXED_25 -> 25.0;
      case FIXED_50 -> 50.0;
      case FIXED_100 -> 100.0;
    };
  }

  /**
   * 计算税率
   * 锚点越多越低：1个=8%，2个=6%，3个=4%，4个=2%，5个=0%
   * 阴阳联动：阴身→金/炎 -2%，阳身→木/水/土 -2%
   * 永久模式才有税，暂时模式无税
   */
  private double calculateTax(
      WuxingHuaHenAttachment.Element element,
      YinYangDualityAttachment.Mode yinyangMode,
      int anchorCount,
      WuxingGuiBianAttachment.ConversionMode conversionMode) {
    // 暂时模式无税
    if (conversionMode == WuxingGuiBianAttachment.ConversionMode.TEMPORARY) {
      return 0.0;
    }

    // 基础税8%（1个锚）
    double tax = BASE_TAX;

    // 每多一个锚-2%（最多5个锚=免税）
    if (anchorCount > 1) {
      tax -= (anchorCount - 1) * TAX_REDUCTION_PER_ANCHOR;
    }

    // 阴阳联动加成
    if (yinyangMode == YinYangDualityAttachment.Mode.YIN) {
      // 阴身（攻伐）→ 金/炎 -2%
      if (element == WuxingHuaHenAttachment.Element.JIN
          || element == WuxingHuaHenAttachment.Element.YAN) {
        tax -= YINYANG_TAX_REDUCTION;
      }
    } else {
      // 阳身（防御）→ 木/水/土 -2%
      if (element == WuxingHuaHenAttachment.Element.MU
          || element == WuxingHuaHenAttachment.Element.SHUI
          || element == WuxingHuaHenAttachment.Element.TU) {
        tax -= YINYANG_TAX_REDUCTION;
      }
    }

    // 限制范围0~100%
    return Math.max(0.0, Math.min(tax, 1.0));
  }

  private boolean consumeBaseCost(ServerPlayer player, ResourceHandle handle) {
    if (handle.adjustZhenyuan(-COST_ZHENYUAN, true).isEmpty()) {
      return false;
    }
    if (handle.adjustNiantou(-COST_NIANTOU, true).isEmpty()) {
      handle.adjustZhenyuan(COST_ZHENYUAN, true);
      return false;
    }
    if (handle.adjustHunpo(-COST_HUNPO, true).isEmpty()) {
      handle.adjustZhenyuan(COST_ZHENYUAN, true);
      handle.adjustNiantou(COST_NIANTOU, true);
      return false;
    }
    if (handle.adjustJingli(-COST_JINGLI, true).isEmpty()) {
      handle.adjustZhenyuan(COST_ZHENYUAN, true);
      handle.adjustNiantou(COST_NIANTOU, true);
      handle.adjustHunpo(COST_HUNPO, true);
      return false;
    }
    return true;
  }

  private void returnBaseCost(ResourceHandle handle) {
    handle.adjustZhenyuan(COST_ZHENYUAN, true);
    handle.adjustNiantou(COST_NIANTOU, true);
    handle.adjustHunpo(COST_HUNPO, true);
    handle.adjustJingli(COST_JINGLI, true);
  }

  private void autoReturnTemporaryConversion(
      ServerPlayer player, WuxingGuiBianAttachment.FreezeSnapshot snapshot) {
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      snapshot.clear();
      return;
    }
    ResourceHandle handle = handleOpt.get();

    WuxingHuaHenAttachment.Element element = snapshot.element();
    double amountConsumed = snapshot.amountConsumed();
    double amountOut = snapshot.amountOut();

    // 检查当前变化道是否足够返还
    double currentBianhua = DaoHenResourceOps.get(handle, BIANHUA_DAO_KEY);
    if (currentBianhua < amountOut) {
      sendFailure(
          player,
          String.format(
              "§c暂时转化到期，但变化道不足以返还（需要 %.1f，当前 %.1f）。转化已作废。",
              amountOut, currentBianhua));
      snapshot.clear();
      return;
    }

    // 执行返还
    DaoHenResourceOps.consume(handle, BIANHUA_DAO_KEY, amountOut);
    DaoHenResourceOps.add(handle, element.poolKey(), amountConsumed);

    snapshot.clear();

    sendSuccess(
        player,
        String.format(
            "§a暂时转化到期：变化道 §e%.1f §f→ §b%s §e%.1f §7(全额返还)",
            amountOut, getElementName(element), amountConsumed));

    // FX返还效果
    playReturnFx(player, element);
  }

  private WuxingGuiBianAttachment getOrCreateAttachment(ServerPlayer player) {
    return CCAttachments.getWuxingGuiBian(player);
  }

  private String getElementName(WuxingHuaHenAttachment.Element element) {
    return switch (element) {
      case JIN -> "金道";
      case MU -> "木道";
      case SHUI -> "水道";
      case YAN -> "炎道";
      case TU -> "土道";
    };
  }

  private String formatMissingOrgans(List<ResourceLocation> missingOrgans) {
    if (missingOrgans.isEmpty()) {
      return "无";
    }
    return missingOrgans.stream()
        .map(id -> id.getPath())
        .reduce((a, b) -> a + ", " + b)
        .orElse("");
  }

  private void sendFailure(ServerPlayer player, String message) {
    player.displayClientMessage(Component.literal("[五行归变·逆转] " + message), true);
  }

  private void sendSuccess(ServerPlayer player, String message) {
    player.displayClientMessage(Component.literal(message), true);
  }

  private MutableComponent createConversionOption(
      WuxingGuiBianAttachment.ConversionMode option,
      WuxingGuiBianAttachment.ConversionMode current) {
    boolean isCurrent = option == current;
    String color = isCurrent ? "§b§l" : "§f";
    String label = switch (option) {
      case TEMPORARY -> "暂时模式";
      case PERMANENT -> "永久模式";
    };
    String command =
        "/wuxing_gui_config mode " + (option == WuxingGuiBianAttachment.ConversionMode.TEMPORARY ? "temporary" : "permanent");

    MutableComponent component = Component.literal(color + "[" + label + "]");
    component.setStyle(
        component
            .getStyle()
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
            .withHoverEvent(
                new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Component.literal("§a" + getConversionModeHover(option)))));
    return component;
  }

  private String getConversionModeName(WuxingGuiBianAttachment.ConversionMode mode) {
    return switch (mode) {
      case TEMPORARY -> "暂时模式（20秒后自动返还，不收税）";
      case PERMANENT -> "永久模式（立即入账，按锚点计税）";
    };
  }

  private String getConversionModeHover(WuxingGuiBianAttachment.ConversionMode mode) {
    return switch (mode) {
      case TEMPORARY -> "20秒窗口内储存变化道，时间到返还原五行道痕";
      case PERMANENT -> "立即转化为变化道痕，按锚点计算损耗税率";
    };
  }

  // ========== FX 粒子效果 ==========

  private void playConversionFx(ServerPlayer player, WuxingHuaHenAttachment.Element element) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 pos = player.position();
    double x = pos.x;
    double y = pos.y;
    double z = pos.z;

    // 启动：ENCHANT 环绕
    for (int i = 0; i < 15; i++) {
      double angle = (i / 15.0) * Math.PI * 2;
      double radius = 0.5;
      double offsetX = Math.cos(angle) * radius;
      double offsetZ = Math.sin(angle) * radius;
      level.sendParticles(
          ParticleTypes.ENCHANT, x + offsetX, y + 1.0, z + offsetZ, 1, 0.0, 0.1, 0.0, 0.05);
    }

    // 元素特色粒子
    switch (element) {
      case JIN -> {
        for (int i = 0; i < 10; i++) {
          level.sendParticles(
              ParticleTypes.ELECTRIC_SPARK,
              x + (Math.random() - 0.5) * 0.6,
              y + 1.2,
              z + (Math.random() - 0.5) * 0.6,
              1,
              0.0,
              0.0,
              0.0,
              0.05);
        }
      }
      case MU -> {
        for (int i = 0; i < 8; i++) {
          level.sendParticles(
              ParticleTypes.HAPPY_VILLAGER,
              x + (Math.random() - 0.5) * 0.6,
              y + 1.2,
              z + (Math.random() - 0.5) * 0.6,
              1,
              0.0,
              0.05,
              0.0,
              0.02);
        }
      }
      case SHUI -> {
        for (int i = 0; i < 10; i++) {
          level.sendParticles(
              ParticleTypes.FALLING_WATER,
              x + (Math.random() - 0.5) * 0.6,
              y + 1.5,
              z + (Math.random() - 0.5) * 0.6,
              1,
              0.0,
              -0.1,
              0.0,
              0.0);
        }
      }
      case YAN -> {
        for (int i = 0; i < 12; i++) {
          level.sendParticles(
              ParticleTypes.SMALL_FLAME,
              x + (Math.random() - 0.5) * 0.6,
              y + 1.2,
              z + (Math.random() - 0.5) * 0.6,
              1,
              0.0,
              0.1,
              0.0,
              0.03);
        }
      }
      case TU -> {
        for (int i = 0; i < 8; i++) {
          level.sendParticles(
              ParticleTypes.CLOUD,
              x + (Math.random() - 0.5) * 0.6,
              y + 1.0,
              z + (Math.random() - 0.5) * 0.6,
              1,
              0.0,
              0.02,
              0.0,
              0.01);
        }
      }
    }

    // 音效
    level.playSound(
        null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.2f);
  }

  private void playReturnFx(ServerPlayer player, WuxingHuaHenAttachment.Element element) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 pos = player.position();
    double x = pos.x;
    double y = pos.y;
    double z = pos.z;

    // 回收粒子
    for (int i = 0; i < 12; i++) {
      double angle = (i / 12.0) * Math.PI * 2;
      double radius = 1.2;
      double offsetX = Math.cos(angle) * radius;
      double offsetZ = Math.sin(angle) * radius;
      level.sendParticles(
          ParticleTypes.SOUL,
          x + offsetX,
          y + 0.5,
          z + offsetZ,
          1,
          -offsetX * 0.2,
          0.1,
          -offsetZ * 0.2,
          0.05);
    }

    // 低音调
    level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5f, 0.7f);
  }
}
