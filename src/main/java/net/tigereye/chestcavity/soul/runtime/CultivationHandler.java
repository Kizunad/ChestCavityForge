package net.tigereye.chestcavity.soul.runtime;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.registry.SoulPerSecondListener;

/** 被动修炼：真元≥95%时每秒消耗 10 并+10 修炼进度（不打断行为）。 */
public final class CultivationHandler implements SoulPerSecondListener {

  private static final double PASSIVE_THRESHOLD_FRAC = 0.95;
  private static final double COST_PER_SECOND = 10.0;
  private static final double PROGRESS_PER_SECOND = 10.0;

  @Override
  public void onSecond(SoulPlayer soul, long gameTime) {
    ServerPlayer owner = resolveOwner(soul);
    if (owner == null) return;
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt =
        GuzhenrenResourceBridge.open(owner);
    if (handleOpt.isEmpty()) return;
    var handle = handleOpt.get();

    double cur = handle.read("zhenyuan").orElse(0.0);
    double max = handle.read("zuida_zhenyuan").orElse(0.0);
    if (!(max > 0.0) || cur / max < PASSIVE_THRESHOLD_FRAC) return;

    OptionalDouble after =
        net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps
            .tryConsumeScaledZhenyuan(handle, COST_PER_SECOND);
    if (after.isEmpty()) return;

    net.tigereye.chestcavity.guzhenren.util.CultivationHelper.tickProgress(
        handle, PROGRESS_PER_SECOND);
  }

  private static ServerPlayer resolveOwner(SoulPlayer soul) {
    var server = soul.serverLevel().getServer();
    var opt = soul.getOwnerId();
    if (opt == null || opt.isEmpty()) return null;
    UUID ownerId = opt.get();
    return ownerId == null ? null : server.getPlayerList().getPlayer(ownerId);
  }

  private static void tryPromoteStage(GuzhenrenResourceBridge.ResourceHandle handle) {
    /* moved to CultivationHelper */
  }
}
