package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.runtime;

import java.util.Locale;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.rarity.HunDaoSoulForm;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.rarity.HunDaoSoulRarityBonuses;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.tuning.HunDaoRuntimeTuning;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Applies the "mortal shell" hun po capacity softcap when players stay outside soul beast form.
 *
 * <p>Players exceeding the HP-based cap will gradually lose their excess maximum hun po so that the
 * UI soul level reflects the physical body limit (20 HP → 十人魂, 1000 hun po).
 */
public final class HunPoCapacityScheduler {

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final int TICKS_PER_SECOND = 20;
  private static final String FIELD_HUNPO = "hunpo";
  private static final String FIELD_HUNPO_MAX = "zuida_hunpo";

  public static final HunPoCapacityScheduler INSTANCE = new HunPoCapacityScheduler();

  private boolean enabled = true;

  private HunPoCapacityScheduler() {}

  /** Enable the softcap scheduler. */
  public void enable() {
    enabled = true;
    LOGGER.debug("[hun_dao][capacity] Enabled mortal shell softcap scheduler");
  }

  /** Disable the softcap scheduler. */
  public void disable() {
    enabled = false;
    LOGGER.debug("[hun_dao][capacity] Disabled mortal shell softcap scheduler");
  }

  /** @return true if the scheduler is enabled. */
  public boolean isEnabled() {
    return enabled;
  }

  @SubscribeEvent
  public void onLevelTick(LevelTickEvent.Post event) {
    if (!enabled) {
      return;
    }
    if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
      return;
    }
    long gameTime = serverLevel.getGameTime();
    if (gameTime % TICKS_PER_SECOND != 0) {
      return;
    }
    for (ServerPlayer player : serverLevel.players()) {
      clampPlayer(player);
    }
  }

  private void clampPlayer(ServerPlayer player) {
    if (player == null || !player.isAlive()) {
      return;
    }
    HunDaoRuntimeContext context = HunDaoRuntimeContext.get(player);
    HunDaoStateMachine stateMachine = context.getStateMachine();
    if (stateMachine.isSoulBeastMode()) {
      return; // Soul beast form ignores the mortal shell limit
    }

    double limit = computeSoftCap(player, context);
    if (!(limit > 0.0D)) {
      return;
    }

    double maxHunpo = context.getResourceOps().readMaxHunpo(player);
    if (!(maxHunpo > limit + HunDaoRuntimeTuning.MortalShell.EPSILON)) {
      return;
    }
    if (!Double.isFinite(maxHunpo)) {
      return;
    }

    double excess = maxHunpo - limit;
    double decay =
        Math.max(
            HunDaoRuntimeTuning.MortalShell.MIN_DECAY_PER_SECOND,
            excess * HunDaoRuntimeTuning.MortalShell.DECAY_FRACTION);
    double targetMax = Math.max(limit, maxHunpo - decay);

    context
        .getResourceOps()
        .openHandle(player)
        .ifPresent(handle -> applyClamp(player, handle, maxHunpo, targetMax, limit, excess));
  }

  private void applyClamp(
      ServerPlayer player,
      GuzhenrenResourceBridge.ResourceHandle handle,
      double previousMax,
      double targetMax,
      double limit,
      double previousExcess) {
    var newMaxOpt = handle.writeDouble(FIELD_HUNPO_MAX, targetMax);
    if (newMaxOpt.isEmpty()) {
      return;
    }
    double newMax = newMaxOpt.getAsDouble();
    double epsilon = HunDaoRuntimeTuning.MortalShell.EPSILON;
    double currentHunpo = handle.read(FIELD_HUNPO).orElse(0.0D);
    if (currentHunpo > newMax + epsilon) {
      handle.writeDouble(FIELD_HUNPO, newMax);
    }
    LOGGER.trace(
        "[hun_dao][capacity] {} clamped hunpo max {} -> {} (limit={} excess={})",
        describe(player),
        format(previousMax),
        format(newMax),
        format(limit),
        format(previousExcess));
  }

  private double computeSoftCap(ServerPlayer player, HunDaoRuntimeContext context) {
    double maxHealth = Math.max(0.0D, player.getAttributeValue(Attributes.MAX_HEALTH));
    double baseCap = maxHealth * HunDaoRuntimeTuning.MortalShell.HUNPO_PER_HP;
    HunDaoSoulRarityBonuses bonuses =
        context.getRarityOps().getBonuses(player, HunDaoSoulForm.HUMAN);
    double bonus = Math.max(0.0D, bonuses.hunPoMaxBonus());
    double combined = baseCap + bonus;
    return Math.min(combined, HunDaoRuntimeTuning.MortalShell.ABSOLUTE_MAX_HUNPO);
  }

  private String describe(ServerPlayer player) {
    if (player == null) {
      return "<null>";
    }
    return String.format(Locale.ROOT, "%s(%s)", player.getScoreboardName(), player.getUUID());
  }

  private String format(double value) {
    return String.format(Locale.ROOT, "%.2f", value);
  }
}
