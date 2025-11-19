package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.middleware;

import java.util.Locale;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.fx.HunDaoSoulFlameFx;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.network.HunDaoNetworkHelper;
import net.tigereye.chestcavity.compat.guzhenren.util.SaturationHelper;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.engine.dot.DoTEngine;
import net.tigereye.chestcavity.engine.dot.DoTEngine.FxAnchor;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.registration.CCSoundEvents;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Hun Dao 行为与底层系统（DoT、资源）之间的中间层桥接器.
 *
 * <p>职责：
 *
 * <ul>
 *   <li>调度 Damage-over-Time（DoT）效果
 *   <li>维护魂魄（hunpo）资源与饱食度
 *   <li>为玩家/非玩家提供统一的中间层入口
 * </ul>
 */
public final class HunDaoMiddleware {

  public static final HunDaoMiddleware INSTANCE = new HunDaoMiddleware();

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final ResourceLocation SOUL_FLAME_FX = ChestCavity.id("soulbeast_dot_tick");
  private static final SoundEvent SOUL_FLAME_SOUND = CCSoundEvents.CUSTOM_SOULBEAST_DOT.get();

  private HunDaoMiddleware() {}

  /**
   * 对目标施加魂焰（Soul Flame）持续伤害效果.
   *
   * @param source 施加效果的玩家
   * @param target 受到效果的实体
   * @param perSecondDamage 每秒伤害
   * @param seconds 持续秒数
   */
  public void applySoulFlame(
      Player source, LivingEntity target, double perSecondDamage, int seconds) {
    if (source == null || target == null || !target.isAlive()) {
      return;
    }
    if (perSecondDamage <= 0 || seconds <= 0) {
      return;
    }
    // Apply Soul Mark for reaction triggers
    try {
      int mark =
          ChestCavity.config != null
              ? Math.max(20, ChestCavity.config.REACTION.soulMarkDurationTicks)
              : 200;
      net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.add(
          target, net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys.SOUL_MARK, mark);
    } catch (Throwable ignored) {
      // Ignored
    }
    // Schedule DoT damage
    DoTEngine.schedulePerSecond(
        source,
        target,
        perSecondDamage,
        seconds,
        SOUL_FLAME_SOUND,
        0.6f,
        1.0f,
        net.tigereye.chestcavity.util.DoTTypes.HUN_DAO_SOUL_FLAME,
        SOUL_FLAME_FX,
        FxAnchor.TARGET,
        Vec3.ZERO,
        0.7f,
        true);
    // Play soul flame particles and sound
    HunDaoSoulFlameFx.playSoulFlame(target, SOUL_FLAME_FX, seconds);
    LOGGER.debug(
        "[hun_dao][middleware] DoT={}s @{} -> {}",
        seconds,
        format(perSecondDamage),
        target.getName().getString());

    // Sync soul flame to clients for HUD display
    int ticks = seconds * 20;
    HunDaoNetworkHelper.syncSoulFlame(target, 1, ticks); // Stack count simplified to 1
  }

  /**
   * 以每秒固定速率泄露玩家的魂魄值.
   *
   * @param player 被泄露魂魄的玩家
   * @param amount 每秒泄露的魂魄量
   */
  public void leakHunpoPerSecond(Player player, double amount) {
    if (player == null || amount <= 0.0D) {
      return;
    }
    adjustHunpo(player, -amount, "leak");
    SaturationHelper.gentlyTopOff(player, 18, 0.5f);
  }

  /**
   * 尝试消耗指定数量的魂魄值.
   *
   * @param player 被消耗魂魄的玩家
   * @param amount 需要消耗的魂魄量
   * @return true 如果成功消耗；否则返回 false
   */
  public boolean consumeHunpo(Player player, double amount) {
    if (player == null || amount <= 0.0D) {
      return false;
    }
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt =
        GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return false;
    }
    GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
    double current = handle.read("hunpo").orElse(0.0D);
    if (current < amount) {
      return false;
    }
    ResourceOps.tryAdjustDouble(handle, "hunpo", -amount, true, "zuida_hunpo");
    return true;
  }

  /**
   * 处理玩家特定的维护逻辑.
   *
   * @param player 需要维护的玩家
   */
  public void handlerPlayer(Player player) {
    SaturationHelper.gentlyTopOff(player, 18, 0.5f);
  }

  /**
   * 处理非玩家实体特定的维护逻辑.
   *
   * @param entity 需要维护的实体
   */
  public void handlerNonPlayer(LivingEntity entity) {
    // placeholder for non-player upkeep paths
  }

  /**
   * 调整玩家魂魄值并同步到客户端.
   *
   * @param player 目标玩家
   * @param amount 调整量（正为增加，负为减少）
   * @param reason 日志中的调整原因
   */
  private void adjustHunpo(Player player, double amount, String reason) {
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt =
        GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
    ResourceOps.tryAdjustDouble(handle, "hunpo", amount, true, "zuida_hunpo");
    LOGGER.trace(
        "[hun_dao][middleware] adjusted {} hunpo for {} ({})",
        format(amount),
        player.getScoreboardName(),
        reason);

    // Sync hun po to client for HUD display
    double current = handle.read("hunpo").orElse(0.0);
    double max = handle.read("zuida_hunpo").orElse(100.0);
    HunDaoNetworkHelper.syncHunPo(player, current, max);
  }

  /**
   * 以固定格式格式化数值.
   *
   * @param value 数值
   * @return 格式化后的字符串
   */
  private String format(double value) {
    return String.format(Locale.ROOT, "%.2f", value);
  }
}
