package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior;

import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
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
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.WuxingHuaHenAttachment.Element;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.WuxingHuaHenAttachment.Mode;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.YinYangDualityAttachment;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.util.YinYangDualityOps;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.DaoHenResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

/**
 * 五行化痕（变化道 → 金/木/水/炎/土）
 * Transmutes Bianhua Dao marks into Five Elements dao marks.
 */
public final class WuxingHuaHenBehavior extends AbstractGuzhenrenOrganBehavior
    implements net.tigereye.chestcavity.listeners.OrganSlowTickListener {

  public static final WuxingHuaHenBehavior INSTANCE = new WuxingHuaHenBehavior();

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation SKILL_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "wuxing_hua_hen");
  private static final ResourceLocation SKILL_UNDO_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "wuxing_hua_hen_undo");
  private static final ResourceLocation SKILL_CHECK_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "wuxing_hua_hen_check");
  private static final ResourceLocation SKILL_CONFIG_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "wuxing_hua_hen_config");

  private static final long BASE_COOLDOWN_TICKS = 15L * 20L; // 15秒
  private static final long LARGE_AMOUNT_COOLDOWN_BONUS = 5L * 20L; // 大量转化+5秒
  private static final long UNDO_WINDOW_TICKS = 10 * 60L * 20L; // 10分钟撤销窗口
  private static final long UNDO_WARNING_TICKS = 10L * 20L; // 剩余10秒时警告

  private static final double BASE_TAX = 0.05; // 基础税5%
  private static final double MODE_TAX_REDUCTION = 0.02; // 态加成2%
  private static final double MAX_TAX = 0.20; // 最大税20%
  private static final double SINGLE_CAST_CAP = 200.0; // 单次上限200
  private static final double LARGE_AMOUNT_THRESHOLD = 100.0; // 大量阈值100
  private static final double UNDO_RETURN_RATIO = 0.80; // 撤销返还80%

  private static final String BIANHUA_DAO_KEY = "daohen_bianhuadao";

  // BaseCost at rank 1, stage 1
  private static final double COST_ZHENYUAN = 120.0;
  private static final double COST_NIANTOU = 4.0;
  private static final double COST_HUNPO = 2.0;

  static {
    OrganActivationListeners.register(
        SKILL_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateTransmute(player, cc);
          }
        });
    OrganActivationListeners.register(
        SKILL_UNDO_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateUndo(player, cc);
          }
        });
    OrganActivationListeners.register(
        SKILL_CHECK_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateCheck(player, cc);
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

  private WuxingHuaHenBehavior() {}

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof ServerPlayer player)) {
      return;
    }
    if (player.level().isClientSide()) {
      return;
    }

    // 检查撤销窗口是否即将过期
    WuxingHuaHenAttachment attachment = getOrCreateAttachment(player);
    WuxingHuaHenAttachment.UndoSnapshot snapshot = attachment.undoSnapshot();

    long now = player.level().getGameTime();
    if (!snapshot.isValid(now)) {
      return;
    }

    long remainingTicks = snapshot.expireTick() - now;

    // 在剩余10秒时警告（只警告一次）
    if (remainingTicks > 0 && remainingTicks <= UNDO_WARNING_TICKS && remainingTicks > UNDO_WARNING_TICKS - 20) {
      long remainingSeconds = remainingTicks / 20L;
      Element element = snapshot.element();
      player.displayClientMessage(
          Component.literal(
              String.format(
                  "§e⚠ 警告：§b%s §f转化将在 §c%d秒 §f后无法撤销！",
                  getElementName(element), remainingSeconds)),
          true);
    }
  }

  private void activateTransmute(ServerPlayer player, ChestCavityInstance cc) {
    if (player.level().isClientSide() || cc == null) {
      return;
    }

    long now = player.level().getGameTime();

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
      sendFailure(player, "封印期间无法施展五行化痕。");
      return;
    }

    // 获取attachment（TODO: 这里需要注册到NeoForge attachment system）
    WuxingHuaHenAttachment attachment = getOrCreateAttachment(player);

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

    // 获取变化道存量
    double bianhuaAmount = DaoHenResourceOps.get(handle, BIANHUA_DAO_KEY);
    if (bianhuaAmount <= 0.0) {
      sendFailure(player, "变化道道痕不足。");
      // 返还BaseCost
      handle.adjustZhenyuan(COST_ZHENYUAN, true);
      handle.adjustNiantou(COST_NIANTOU, true);
      handle.adjustHunpo(COST_HUNPO, true);
      return;
    }

    // 解析转化数量
    Mode mode = attachment.lastMode();
    double amountReq = resolveAmount(mode, attachment.lastFixedAmount(), bianhuaAmount);
    if (amountReq <= 0.0 || amountReq > SINGLE_CAST_CAP) {
      sendFailure(player, "转化数量无效（范围: 0~" + SINGLE_CAST_CAP + "）。");
      // 返还BaseCost
      handle.adjustZhenyuan(COST_ZHENYUAN, true);
      handle.adjustNiantou(COST_NIANTOU, true);
      handle.adjustHunpo(COST_HUNPO, true);
      return;
    }
    amountReq = Math.min(amountReq, bianhuaAmount);

    // 计算税率
    Element element = attachment.lastElement();
    double tax = calculateTax(element, yinyang.currentMode());
    double amountOut = Math.floor(amountReq * (1.0 - tax));

    if (amountOut <= 0.0) {
      sendFailure(player, "损耗过高，无有效产出。");
      // 返还BaseCost
      handle.adjustZhenyuan(COST_ZHENYUAN, true);
      handle.adjustNiantou(COST_NIANTOU, true);
      handle.adjustHunpo(COST_HUNPO, true);
      return;
    }

    // 执行转化
    DaoHenResourceOps.consume(handle, BIANHUA_DAO_KEY, amountReq);
    DaoHenResourceOps.add(handle, element.poolKey(), amountOut);

    // 设置冷却
    long cooldown = BASE_COOLDOWN_TICKS;
    if (amountReq > LARGE_AMOUNT_THRESHOLD) {
      cooldown += LARGE_AMOUNT_COOLDOWN_BONUS;
    }
    long readyTick = now + cooldown;
    yinyang.setCooldown(SKILL_ID, readyTick);
    ActiveSkillRegistry.scheduleReadyToast(player, SKILL_ID, readyTick, now);

    // 记录撤销快照
    long undoExpireTick = now + UNDO_WINDOW_TICKS;
    attachment.undoSnapshot().set(element, amountReq, amountOut, undoExpireTick);

    // 成功提示
    long undoSeconds = UNDO_WINDOW_TICKS / 20L;
    sendSuccess(
        player,
        String.format(
            "§a五行化痕：§f变化道 §e%.1f §f→ §b%s §e%.1f §7(税率 %.1f%%) §8[%d秒内可撤销]",
            amountReq, getElementName(element), amountOut, tax * 100, undoSeconds));

    // FX粒子效果
    playTransmuteFx(player, element);
  }

  private double resolveAmount(Mode mode, int fixedAmount, double available) {
    return switch (mode) {
      case LAST -> fixedAmount; // 默认使用固定数量
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

  private double calculateTax(Element element, YinYangDualityAttachment.Mode yinyangMode) {
    double tax = BASE_TAX;

    // 态加成
    if (yinyangMode == YinYangDualityAttachment.Mode.YIN) {
      // 阴身（攻伐）→ 金/炎 税减2%
      if (element == Element.JIN || element == Element.YAN) {
        tax -= MODE_TAX_REDUCTION;
      }
    } else {
      // 阳身（防御）→ 木/水/土 税减2%
      if (element == Element.MU || element == Element.SHUI || element == Element.TU) {
        tax -= MODE_TAX_REDUCTION;
      }
    }

    // 限制范围0~20%
    return Math.max(0.0, Math.min(tax, MAX_TAX));
  }

  private boolean consumeBaseCost(ServerPlayer player, ResourceHandle handle) {
    // 尝试扣除真元
    if (handle.adjustZhenyuan(-COST_ZHENYUAN, true).isEmpty()) {
      return false;
    }
    // 尝试扣除念头
    if (handle.adjustNiantou(-COST_NIANTOU, true).isEmpty()) {
      handle.adjustZhenyuan(COST_ZHENYUAN, true); // 回滚
      return false;
    }
    // 尝试扣除魂魄
    if (handle.adjustHunpo(-COST_HUNPO, true).isEmpty()) {
      handle.adjustZhenyuan(COST_ZHENYUAN, true); // 回滚
      handle.adjustNiantou(COST_NIANTOU, true); // 回滚
      return false;
    }
    return true;
  }

  private WuxingHuaHenAttachment getOrCreateAttachment(ServerPlayer player) {
    return CCAttachments.getWuxingHuaHen(player);
  }

  private String getElementName(Element element) {
    return switch (element) {
      case JIN -> "金道";
      case MU -> "木道";
      case SHUI -> "水道";
      case YAN -> "炎道";
      case TU -> "土道";
    };
  }

  private void sendFailure(ServerPlayer player, String message) {
    player.displayClientMessage(Component.literal("[五行化痕] " + message), true);
  }

  private void sendSuccess(ServerPlayer player, String message) {
    player.displayClientMessage(Component.literal(message), true);
  }

  // ========== FX 粒子效果 ==========

  private void playTransmuteFx(ServerPlayer player, Element element) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 pos = player.position();
    double x = pos.x;
    double y = pos.y;
    double z = pos.z;

    // 启动：ENCHANT 由胸口向外10粒
    for (int i = 0; i < 10; i++) {
      double angle = (i / 10.0) * Math.PI * 2;
      double radius = 0.3 + Math.random() * 0.3;
      double offsetX = Math.cos(angle) * radius;
      double offsetZ = Math.sin(angle) * radius;
      level.sendParticles(
          ParticleTypes.ENCHANT,
          x + offsetX,
          y + 1.2,
          z + offsetZ,
          1,
          offsetX * 0.5,
          0.05,
          offsetZ * 0.5,
          0.1);
    }

    // 手前元素粒子
    Vec3 lookDir = player.getLookAngle();
    double handX = x + lookDir.x * 0.8;
    double handY = y + 1.3 + lookDir.y * 0.8;
    double handZ = z + lookDir.z * 0.8;

    switch (element) {
      case JIN -> {
        // 金：ELECTRIC_SPARK
        for (int i = 0; i < 15; i++) {
          double offsetX = (Math.random() - 0.5) * 0.4;
          double offsetY = (Math.random() - 0.5) * 0.4;
          double offsetZ = (Math.random() - 0.5) * 0.4;
          level.sendParticles(
              ParticleTypes.ELECTRIC_SPARK,
              handX + offsetX,
              handY + offsetY,
              handZ + offsetZ,
              1,
              0.0,
              0.0,
              0.0,
              0.05);
        }
      }
      case MU -> {
        // 木：HAPPY_VILLAGER
        for (int i = 0; i < 12; i++) {
          double offsetX = (Math.random() - 0.5) * 0.4;
          double offsetY = (Math.random() - 0.5) * 0.4;
          double offsetZ = (Math.random() - 0.5) * 0.4;
          level.sendParticles(
              ParticleTypes.HAPPY_VILLAGER,
              handX + offsetX,
              handY + offsetY,
              handZ + offsetZ,
              1,
              0.0,
              0.05,
              0.0,
              0.02);
        }
      }
      case SHUI -> {
        // 水：FALLING_WATER
        for (int i = 0; i < 15; i++) {
          double offsetX = (Math.random() - 0.5) * 0.4;
          double offsetY = Math.random() * 0.5;
          double offsetZ = (Math.random() - 0.5) * 0.4;
          level.sendParticles(
              ParticleTypes.FALLING_WATER,
              handX + offsetX,
              handY + offsetY,
              handZ + offsetZ,
              1,
              0.0,
              -0.1,
              0.0,
              0.0);
        }
      }
      case YAN -> {
        // 炎：SMALL_FLAME
        for (int i = 0; i < 18; i++) {
          double offsetX = (Math.random() - 0.5) * 0.4;
          double offsetY = (Math.random() - 0.5) * 0.4;
          double offsetZ = (Math.random() - 0.5) * 0.4;
          level.sendParticles(
              ParticleTypes.SMALL_FLAME,
              handX + offsetX,
              handY + offsetY,
              handZ + offsetZ,
              1,
              0.0,
              0.1,
              0.0,
              0.03);
        }
      }
      case TU -> {
        // 土：CLOUD + 少量BLOCK粒子
        for (int i = 0; i < 12; i++) {
          double offsetX = (Math.random() - 0.5) * 0.5;
          double offsetY = (Math.random() - 0.5) * 0.3;
          double offsetZ = (Math.random() - 0.5) * 0.5;
          level.sendParticles(
              ParticleTypes.CLOUD,
              handX + offsetX,
              handY + offsetY,
              handZ + offsetZ,
              1,
              0.0,
              0.02,
              0.0,
              0.01);
        }
        for (int i = 0; i < 6; i++) {
          double offsetX = (Math.random() - 0.5) * 0.4;
          double offsetY = (Math.random() - 0.5) * 0.3;
          double offsetZ = (Math.random() - 0.5) * 0.4;
          level.sendParticles(
              ParticleTypes.CRIT,
              handX + offsetX,
              handY + offsetY,
              handZ + offsetZ,
              1,
              0.0,
              0.0,
              0.0,
              0.02);
        }
      }
    }

    // 完成音效：AMETHYST_BLOCK_CHIME（音高按元素调整）
    float pitch = 1.4f + element.ordinal() * 0.1f; // 1.4~1.8
    level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, pitch);
  }

  // ========== 撤销机制 ==========

  /**
   * 撤销上次转化，返还80%到变化道
   */
  private void activateUndo(ServerPlayer player, ChestCavityInstance cc) {
    if (player.level().isClientSide() || cc == null) {
      return;
    }

    long now = player.level().getGameTime();

    // 获取attachment
    WuxingHuaHenAttachment attachment = getOrCreateAttachment(player);
    WuxingHuaHenAttachment.UndoSnapshot snapshot = attachment.undoSnapshot();

    // 检查快照是否有效
    if (!snapshot.isValid(now)) {
      if (snapshot.expireTick() > 0 && now >= snapshot.expireTick()) {
        sendFailure(player, "§c撤销窗口已过期，无法撤销上次转化。");
      } else {
        sendFailure(player, "§c无可撤销的转化记录。");
      }
      return;
    }

    // 显示剩余时间
    long remainingTicks = snapshot.expireTick() - now;
    long remainingSeconds = remainingTicks / 20L;

    // 获取资源句柄
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      sendFailure(player, "无法读取资源。");
      return;
    }
    ResourceHandle handle = handleOpt.get();

    Element element = snapshot.element();
    double amountOut = snapshot.amountOut();
    double returnAmount = Math.floor(amountOut * UNDO_RETURN_RATIO);

    // 检查当前是否还有足够的目标道痕
    double currentElement = DaoHenResourceOps.get(handle, element.poolKey());
    if (currentElement < amountOut) {
      sendFailure(player, "目标道痕不足，无法撤销（需要 " + amountOut + "，当前 " + currentElement + "）。");
      return;
    }

    // 执行撤销：扣除目标道痕，返还变化道
    DaoHenResourceOps.consume(handle, element.poolKey(), amountOut);
    DaoHenResourceOps.add(handle, BIANHUA_DAO_KEY, returnAmount);

    // 清除快照
    snapshot.clear();

    // 成功提示
    sendSuccess(
        player,
        String.format(
            "§a撤销成功：§b%s §e%.1f §f→ 变化道 §e%.1f §7(返还率 80%%) §8[剩余 %d 秒]",
            getElementName(element), amountOut, returnAmount, remainingSeconds));

    // FX粒子效果
    playUndoFx(player, element);
  }

  private void playUndoFx(ServerPlayer player, Element element) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 pos = player.position();
    double x = pos.x;
    double y = pos.y;
    double z = pos.z;

    // 回收效果：ENCHANT 向内回收
    for (int i = 0; i < 20; i++) {
      double angle = (i / 20.0) * Math.PI * 2;
      double radius = 1.5;
      double offsetX = Math.cos(angle) * radius;
      double offsetZ = Math.sin(angle) * radius;
      level.sendParticles(
          ParticleTypes.ENCHANT,
          x + offsetX,
          y + 1.0,
          z + offsetZ,
          1,
          -offsetX * 0.2,
          0.0,
          -offsetZ * 0.2,
          0.1);
    }

    // SOUL 粒子上升
    for (int i = 0; i < 10; i++) {
      double offsetX = (Math.random() - 0.5) * 0.8;
      double offsetZ = (Math.random() - 0.5) * 0.8;
      level.sendParticles(
          ParticleTypes.SOUL,
          x + offsetX,
          y + 0.5,
          z + offsetZ,
          1,
          0.0,
          0.15,
          0.0,
          0.05);
    }

    // 音效：较低音调
    level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5f, 0.8f);
  }

  /**
   * 查询撤销状态
   */
  private void activateCheck(ServerPlayer player, ChestCavityInstance cc) {
    if (player.level().isClientSide() || cc == null) {
      return;
    }

    long now = player.level().getGameTime();

    // 获取attachment
    WuxingHuaHenAttachment attachment = getOrCreateAttachment(player);
    WuxingHuaHenAttachment.UndoSnapshot snapshot = attachment.undoSnapshot();

    // 检查快照是否有效
    if (!snapshot.isValid(now)) {
      player.displayClientMessage(
          Component.literal("§7当前无可撤销的转化记录。"),
          false);
      return;
    }

    // 显示详细信息
    long remainingTicks = snapshot.expireTick() - now;
    long remainingSeconds = remainingTicks / 20L;
    Element element = snapshot.element();
    double amountOut = snapshot.amountOut();
    double returnAmount = Math.floor(amountOut * UNDO_RETURN_RATIO);

    player.displayClientMessage(
        Component.literal(
            String.format(
                "§6═══ 撤销状态 ═══\n" +
                "§f上次转化：§b%s §e%.1f\n" +
                "§f可返还：变化道 §e%.1f §7(80%%)\n" +
                "§f剩余时间：§%s%d秒\n" +
                "§7提示：使用撤销技能可返还道痕",
                getElementName(element),
                amountOut,
                returnAmount,
                (remainingSeconds <= 10 ? "c" : "a"),
                remainingSeconds)),
        false);
  }

  /**
   * 配置菜单
   */
  private void activateConfig(ServerPlayer player, ChestCavityInstance cc) {
    if (player.level().isClientSide() || cc == null) {
      return;
    }

    WuxingHuaHenAttachment attachment = getOrCreateAttachment(player);
    Element currentElement = attachment.lastElement();
    Mode currentMode = attachment.lastMode();

    // 标题
    player.sendSystemMessage(Component.literal("§6═══════ 五行化痕配置 ═══════"));

    // 当前配置
    player.sendSystemMessage(
        Component.literal(
            String.format(
                "§f当前配置：§b%s §7| §e%s",
                getElementName(currentElement), getModeName(currentMode))));

    player.sendSystemMessage(Component.literal(""));

    // 元素选择
    player.sendSystemMessage(Component.literal("§a【选择目标元素】"));
    MutableComponent elementLine = Component.literal("");
    elementLine.append(createElementOption(Element.JIN, currentElement));
    elementLine.append(Component.literal(" §8| "));
    elementLine.append(createElementOption(Element.MU, currentElement));
    elementLine.append(Component.literal(" §8| "));
    elementLine.append(createElementOption(Element.SHUI, currentElement));
    elementLine.append(Component.literal(" §8| "));
    elementLine.append(createElementOption(Element.YAN, currentElement));
    elementLine.append(Component.literal(" §8| "));
    elementLine.append(createElementOption(Element.TU, currentElement));
    player.sendSystemMessage(elementLine);

    player.sendSystemMessage(Component.literal(""));

    // 模式选择
    player.sendSystemMessage(Component.literal("§a【选择转化模式】"));

    // 比例模式行
    MutableComponent ratioLine = Component.literal("§7比例：");
    ratioLine.append(createModeOption(Mode.ALL, currentMode));
    ratioLine.append(Component.literal(" §8| "));
    ratioLine.append(createModeOption(Mode.RATIO_25, currentMode));
    ratioLine.append(Component.literal(" §8| "));
    ratioLine.append(createModeOption(Mode.RATIO_50, currentMode));
    ratioLine.append(Component.literal(" §8| "));
    ratioLine.append(createModeOption(Mode.RATIO_100, currentMode));
    player.sendSystemMessage(ratioLine);

    // 固定数量行
    MutableComponent fixedLine = Component.literal("§7固定：");
    fixedLine.append(createModeOption(Mode.FIXED_10, currentMode));
    fixedLine.append(Component.literal(" §8| "));
    fixedLine.append(createModeOption(Mode.FIXED_25, currentMode));
    fixedLine.append(Component.literal(" §8| "));
    fixedLine.append(createModeOption(Mode.FIXED_50, currentMode));
    fixedLine.append(Component.literal(" §8| "));
    fixedLine.append(createModeOption(Mode.FIXED_100, currentMode));
    player.sendSystemMessage(fixedLine);

    player.sendSystemMessage(Component.literal("§6═══════════════════════════"));
  }

  private MutableComponent createElementOption(Element element, Element current) {
    boolean isCurrent = (element == current);
    String color = isCurrent ? "§b§l" : "§f";
    String elementName = switch (element) {
      case JIN -> "金";
      case MU -> "木";
      case SHUI -> "水";
      case YAN -> "炎";
      case TU -> "土";
    };

    String command = "/wuxing_config element " + element.name().toLowerCase();

    MutableComponent component = Component.literal(color + "[" + elementName + "]");
    component.setStyle(
        component
            .getStyle()
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
            .withHoverEvent(
                new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Component.literal("§a点击切换到 " + getElementName(element)))));

    return component;
  }

  private MutableComponent createModeOption(Mode mode, Mode current) {
    boolean isCurrent = (mode == current);
    String color = isCurrent ? "§e§l" : "§7";
    String modeName = switch (mode) {
      case ALL -> "全部";
      case RATIO_25 -> "25%";
      case RATIO_50 -> "50%";
      case RATIO_100 -> "100%";
      case FIXED_10 -> "10";
      case FIXED_25 -> "25";
      case FIXED_50 -> "50";
      case FIXED_100 -> "100";
      case LAST -> "上次"; // 不应该出现在菜单中
    };

    String command = "/wuxing_config mode " + mode.name().toLowerCase();

    MutableComponent component = Component.literal(color + "[" + modeName + "]");
    component.setStyle(
        component
            .getStyle()
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
            .withHoverEvent(
                new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Component.literal("§a点击切换到 " + getModeName(mode)))));

    return component;
  }

  private String getModeName(Mode mode) {
    return switch (mode) {
      case LAST -> "使用上次配置";
      case ALL -> "全部转化";
      case RATIO_25 -> "25%";
      case RATIO_50 -> "50%";
      case RATIO_100 -> "100%";
      case FIXED_10 -> "固定10";
      case FIXED_25 -> "固定25";
      case FIXED_50 -> "固定50";
      case FIXED_100 -> "固定100";
    };
  }
}
