package net.tigereye.chestcavity.soul.runtime;

import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.registry.SoulPerSecondListener;
import net.tigereye.chestcavity.soul.util.SoulLog;

import java.util.Optional;

/**
 * Per-second hook for Guzhenren fields of interest.
 *
 * Current requirement: only check {@code zhuanshu} each second and invoke a handler if non-zero.
 * Implementation is intentionally lightweight: early-return if Guzhenren is absent or the
 * attachment is not present on the SoulPlayer; otherwise read the value and fire a stub handler.
 */
public final class GuzhenrenZhuanshuSecondHandler implements SoulPerSecondListener {

    private static final String FIELD_ZHUANSHU = "zhuanshu";

    @Override
    public void onSecond(SoulPlayer player, long gameTime) {
        if (!GuzhenrenResourceBridge.isAvailable()) {
            return;
        }
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }
        double zhuanshu = handleOpt.get().read(FIELD_ZHUANSHU).orElse(0.0D);
        if (zhuanshu != 0.0D) {
            onNonZeroZhuanshu(player, zhuanshu, gameTime);
        }
    }

    /**
     * Placeholder for downstream logic when {@code zhuanshu} is non-zero.
     * Keep side-effects minimal; downstream modules can replace/extend this behavior later.
     */
    private static void onNonZeroZhuanshu(SoulPlayer player, double zhuanshu, long gameTime) {
        // For now just emit a debug log (gated by -Dchestcavity.debugSoul=true)
        SoulLog.info("[soul][periodic] zhuanshu>0 owner={} soul={} value={} t={}",
                player.getOwnerId().orElse(null), player.getSoulId(), zhuanshu, gameTime);
        // Future: invoke specific handlers (gates, effects, etc.) here.
    }
}

