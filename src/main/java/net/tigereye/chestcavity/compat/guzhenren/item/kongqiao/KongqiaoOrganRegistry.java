package net.tigereye.chestcavity.compat.guzhenren.item.kongqiao;

import net.tigereye.chestcavity.compat.guzhenren.item.kongqiao.behavior.KongqiaoDaoHenLogger;

/**
 * Registry hook for 空窍（Kong Qiao） organs. Currently only installs Dao Hen logging behaviour.
 */
public final class KongqiaoOrganRegistry {

    private KongqiaoOrganRegistry() {
    }

    static {
        KongqiaoDaoHenLogger.bootstrap();
    }

    public static void bootstrap() {
        // no-op: forces static initialisation
    }
}
