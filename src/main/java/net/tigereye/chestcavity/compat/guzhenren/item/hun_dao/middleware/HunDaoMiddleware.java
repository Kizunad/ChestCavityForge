package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.middleware;

import com.mojang.logging.LogUtils;
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
import org.slf4j.Logger;

/**
 * The middleware bridge between Hun Dao behavior and underlying systems (DoT, resources).
 *
 * <p>- Schedules Damage-over-Time (DoT) effects. - Manages Hunpo resource and saturation upkeep. -
 * Provides placeholders for player/non-player specific handling.
 */
public final class HunDaoMiddleware {

  public static final HunDaoMiddleware INSTANCE = new HunDaoMiddleware();

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final ResourceLocation SOUL_FLAME_FX = ChestCavity.id("soulbeast_dot_tick");
  private static final SoundEvent SOUL_FLAME_SOUND = CCSoundEvents.CUSTOM_SOULBEAST_DOT.get();

  private HunDaoMiddleware() {}

  /**
   * Applies the Soul Flame DoT effect to a target.
   *
   * @param source The player applying the effect.
   * @param target The entity to apply the effect to.
   * @param perSecondDamage The damage per second.
   * @param seconds The duration in seconds.
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
   * Leaks a specified amount of Hunpo per second from a player.
   *
   * @param player The player to drain hunpo from.
   * @param amount The amount of hunpo to drain per second.
   */
  public void leakHunpoPerSecond(Player player, double amount) {
    if (player == null || amount <= 0.0D) {
      return;
    }
    adjustHunpo(player, -amount, "leak");
    SaturationHelper.gentlyTopOff(player, 18, 0.5f);
  }

  /**
   * Consumes a specified amount of Hunpo from a player.
   *
   * @param player The player to consume hunpo from.
   * @param amount The amount of hunpo to consume.
   * @return True if the hunpo was consumed, false otherwise.
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
   * Handles player-specific upkeep tasks.
   *
   * @param player The player to handle.
   */
  public void handlerPlayer(Player player) {
    SaturationHelper.gentlyTopOff(player, 18, 0.5f);
  }

  /**
   * Handles non-player-specific upkeep tasks.
   *
   * @param entity The entity to handle.
   */
  public void handlerNonPlayer(LivingEntity entity) {
    // placeholder for non-player upkeep paths
  }

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

  private String format(double value) {
    return String.format(Locale.ROOT, "%.2f", value);
  }
}
