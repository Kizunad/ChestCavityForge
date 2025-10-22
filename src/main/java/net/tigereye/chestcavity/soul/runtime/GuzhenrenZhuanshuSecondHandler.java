package net.tigereye.chestcavity.soul.runtime;

import java.util.Optional;
import java.util.OptionalDouble;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.registry.SoulPerSecondListener;
import net.tigereye.chestcavity.soul.util.SoulLog;

/**
 * Per-second hook for Guzhenren fields of interest.
 *
 * <p>Current requirement: only check {@code zhuanshu} each second and invoke a handler if non-zero.
 * Implementation is intentionally lightweight: early-return if Guzhenren is absent or the
 * attachment is not present on the SoulPlayer; otherwise read the value and fire a stub handler.
 */
public final class GuzhenrenZhuanshuSecondHandler implements SoulPerSecondListener {

  private static final boolean LOG_PERIODIC = Boolean.getBoolean("chestcavity.debugSoul.periodic");

  private static final String FIELD_ZHUANSHU = "zhuanshu";
  private static final double ZHENYUAN_RECOVERY = 1.0D;
  private static final double HUNPO_RECOVERY = 0.05D;
  private static final double JINGLI_RECOVERY = 0.5D;
  private static final double NIANTOU_RECOVERY = 1.0D;

  @Override
  public void onSecond(SoulPlayer player, long gameTime) {
    if (!GuzhenrenResourceBridge.isAvailable()) {
      return;
    }
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt =
        GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
    OptionalDouble newNiantou =
        handle.adjustDouble("niantou", NIANTOU_RECOVERY, true, "niantou_zuida");
    OptionalDouble newHunpo = handle.adjustDouble("hunpo", HUNPO_RECOVERY, true, "zuida_hunpo");
    OptionalDouble newJingli = handle.adjustDouble("jingli", JINGLI_RECOVERY, true, "zuida_jingli");
    double zhuanshu = handle.read(FIELD_ZHUANSHU).orElse(0.0D);
    OptionalDouble newZhenyuan = OptionalDouble.empty();
    if (zhuanshu > 0.0D) {
      newZhenyuan = handle.adjustDouble("zhenyuan", ZHENYUAN_RECOVERY, true, "zuida_zhenyuan");
    }
    if (LOG_PERIODIC) {
      logRecovery(player, zhuanshu, gameTime, newZhenyuan, newHunpo, newJingli, newNiantou);
    }
  }

  private static void logRecovery(
      SoulPlayer player,
      double zhuanshu,
      long gameTime,
      OptionalDouble newZhenyuan,
      OptionalDouble newHunpo,
      OptionalDouble newJingli,
      OptionalDouble newNiantou) {
    SoulLog.info(
        "[soul][periodic] resource tick owner={} soul={} zhuanshu={} t={} zhenyuan={} hunpo={} jingli={} niantou={}",
        player.getOwnerId().orElse(null),
        player.getSoulId(),
        zhuanshu,
        gameTime,
        newZhenyuan.isPresent() ? newZhenyuan.getAsDouble() : "skip",
        newHunpo.orElse(Double.NaN),
        newJingli.orElse(Double.NaN),
        newNiantou.orElse(Double.NaN));
  }
}
