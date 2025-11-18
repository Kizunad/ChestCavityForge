package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.runtime;

import com.mojang.logging.LogUtils;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.tuning.HunDaoTuning;
import org.slf4j.Logger;

/**
 * Scheduler for periodic hunpo drainage during soul beast transformation.
 *
 * <p>This scheduler ticks once per second (every 20 game ticks) and drains hunpo from players in
 * soul beast mode. If hunpo is exhausted, it automatically deactivates soul beast transformation.
 *
 * <p>This class is registered to the NeoForge event bus and listens to {@link LevelTickEvent.Post}
 * events on the server side.
 */
public final class HunPoDrainScheduler {

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final int TICKS_PER_SECOND = 20;

  public static final HunPoDrainScheduler INSTANCE = new HunPoDrainScheduler();

  private boolean enabled = true;

  private HunPoDrainScheduler() {}

  /** Enable the scheduler. */
  public void enable() {
    this.enabled = true;
    LOGGER.debug("[hun_dao][scheduler] Enabled hunpo drain scheduler");
  }

  /** Disable the scheduler. */
  public void disable() {
    this.enabled = false;
    LOGGER.debug("[hun_dao][scheduler] Disabled hunpo drain scheduler");
  }

  /**
   * Check if the scheduler is enabled.
   *
   * @return true if enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Tick event handler - called every game tick.
   *
   * @param event level tick event
   */
  @SubscribeEvent
  public void onLevelTick(LevelTickEvent.Post event) {
    if (!enabled) {
      return;
    }
    if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
      return;
    }
    // Only tick once per second
    long gameTime = serverLevel.getGameTime();
    if (gameTime % TICKS_PER_SECOND != 0) {
      return;
    }
    // Process all players in this level
    for (ServerPlayer player : serverLevel.players()) {
      tickPlayer(player, gameTime);
    }
  }

  /**
   * Tick a single player's hunpo drain.
   *
   * @param player the player
   * @param gameTime current game time
   */
  private void tickPlayer(ServerPlayer player, long gameTime) {
    HunDaoRuntimeContext context = HunDaoRuntimeContext.get(player);
    HunDaoStateMachine stateMachine = context.getStateMachine();

    // Only drain if in draining state (soul beast active but not permanent)
    if (!stateMachine.isDraining()) {
      return;
    }

    // Leak hunpo per second
    double leakAmount = HunDaoTuning.SoulBeast.HUNPO_LEAK_PER_SEC;
    double currentHunpo = context.getResourceOps().readHunpo(player);

    if (currentHunpo < leakAmount) {
      // Hunpo exhausted - deactivate soul beast
      LOGGER.debug(
          "[hun_dao][scheduler] {} hunpo exhausted ({}), deactivating soul beast",
          describe(player),
          format(currentHunpo));
      stateMachine.deactivateSoulBeast();
      stateMachine.syncToClient();
      return;
    }

    // Perform the leak
    context.getResourceOps().leakHunpoPerSecond(player, leakAmount);
    LOGGER.trace(
        "[hun_dao][scheduler] {} leaked {} hunpo ({} -> {})",
        describe(player),
        format(leakAmount),
        format(currentHunpo),
        format(currentHunpo - leakAmount));
  }

  // ===== Utilities =====

  private String describe(@Nullable Player player) {
    if (player == null) {
      return "<null>";
    }
    return String.format(Locale.ROOT, "%s(%s)", player.getScoreboardName(), player.getUUID());
  }

  private String format(double value) {
    return String.format(Locale.ROOT, "%.2f", value);
  }
}
