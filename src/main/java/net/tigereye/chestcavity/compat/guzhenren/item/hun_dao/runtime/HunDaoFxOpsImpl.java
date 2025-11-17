package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.runtime;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.fx.HunDaoFxDescriptors;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.fx.HunDaoFxRouter;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.fx.HunDaoSoulFlameFx;
import org.slf4j.Logger;

/**
 * Implementation of HunDaoFxOps using the data-driven FX router.
 *
 * <p>Routes all FX requests through HunDaoFxRouter for centralized dispatch. Falls back to
 * HunDaoSoulFlameFx for soul flame effects to maintain compatibility with existing DoT system.
 *
 * <p>Phase 5: Server-side FX operations decoupled from middleware.
 */
public final class HunDaoFxOpsImpl implements HunDaoFxOps {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static final HunDaoFxOpsImpl INSTANCE = new HunDaoFxOpsImpl();

  private HunDaoFxOpsImpl() {}

  @Override
  public void applySoulFlame(
      Player source, LivingEntity target, double perSecondDamage, int seconds) {
    if (source == null || target == null || !target.isAlive()) {
      return;
    }
    if (perSecondDamage <= 0 || seconds <= 0) {
      return;
    }

    if (!(target.level() instanceof ServerLevel level)) {
      return;
    }

    // Use HunDaoSoulFlameFx which already integrates with DoTEngine and FxEngine
    HunDaoSoulFlameFx.playSoulFlame(
        target, HunDaoFxDescriptors.SOUL_FLAME_TICK, seconds);

    LOGGER.debug(
        "[hun_dao][fx_ops] Soul flame applied: {}s @{}/s -> {}",
        seconds,
        String.format("%.2f", perSecondDamage),
        target.getName().getString());
  }

  @Override
  public void playSoulBeastActivate(Player player) {
    if (player == null || !(player.level() instanceof ServerLevel level)) {
      return;
    }

    boolean dispatched = HunDaoFxRouter.dispatch(level, player, HunDaoFxDescriptors.SOUL_BEAST_ACTIVATE);
    LOGGER.debug(
        "[hun_dao][fx_ops] Soul beast activate: {} (dispatched={})",
        player.getScoreboardName(),
        dispatched);
  }

  @Override
  public void playSoulBeastDeactivate(Player player) {
    if (player == null || !(player.level() instanceof ServerLevel level)) {
      return;
    }

    boolean dispatched = HunDaoFxRouter.dispatch(level, player, HunDaoFxDescriptors.SOUL_BEAST_DEACTIVATE);
    LOGGER.debug(
        "[hun_dao][fx_ops] Soul beast deactivate: {} (dispatched={})",
        player.getScoreboardName(),
        dispatched);
  }

  @Override
  public void playSoulBeastHit(Player player, LivingEntity target) {
    if (player == null || target == null || !(target.level() instanceof ServerLevel level)) {
      return;
    }

    boolean dispatched = HunDaoFxRouter.dispatch(level, target, HunDaoFxDescriptors.SOUL_BEAST_HIT);
    LOGGER.debug(
        "[hun_dao][fx_ops] Soul beast hit: {} -> {} (dispatched={})",
        player.getScoreboardName(),
        target.getName().getString(),
        dispatched);
  }

  @Override
  public void playGuiWuActivate(Player player, Vec3 position, double radius) {
    if (player == null || position == null || !(player.level() instanceof ServerLevel level)) {
      return;
    }

    boolean dispatched = HunDaoFxRouter.dispatch(level, position, HunDaoFxDescriptors.GUI_WU_ACTIVATE);
    LOGGER.debug(
        "[hun_dao][fx_ops] Gui wu activate: {} @{} r={} (dispatched={})",
        player.getScoreboardName(),
        position,
        String.format("%.1f", radius),
        dispatched);
  }

  @Override
  public void playGuiWuDissipate(Player player, Vec3 position) {
    if (player == null || position == null || !(player.level() instanceof ServerLevel level)) {
      return;
    }

    boolean dispatched = HunDaoFxRouter.dispatch(level, position, HunDaoFxDescriptors.GUI_WU_DISSIPATE);
    LOGGER.debug(
        "[hun_dao][fx_ops] Gui wu dissipate: {} @{} (dispatched={})",
        player.getScoreboardName(),
        position,
        dispatched);
  }

  @Override
  public void playHunPoLeakWarning(Player player) {
    if (player == null || !(player.level() instanceof ServerLevel level)) {
      return;
    }

    boolean dispatched = HunDaoFxRouter.dispatch(level, player, HunDaoFxDescriptors.HUN_PO_LOW_WARNING);
    LOGGER.debug(
        "[hun_dao][fx_ops] Hun po leak warning: {} (dispatched={})",
        player.getScoreboardName(),
        dispatched);
  }

  @Override
  public void playHunPoRecovery(Player player, double amount) {
    if (player == null || amount <= 0 || !(player.level() instanceof ServerLevel level)) {
      return;
    }

    boolean dispatched = HunDaoFxRouter.dispatch(level, player, HunDaoFxDescriptors.HUN_PO_RECOVERY);
    LOGGER.debug(
        "[hun_dao][fx_ops] Hun po recovery: {} +{} (dispatched={})",
        player.getScoreboardName(),
        String.format("%.2f", amount),
        dispatched);
  }
}
