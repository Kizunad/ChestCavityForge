package net.tigereye.chestcavity.compat.guzhenren.item.kongqiao.behavior;

/**
 * High-level behaviour entrypoint for Kongqiao (空窍) features.
 * Delegates specific behaviours (e.g., DaoHen tracking) to dedicated classes.
 */
public final class KongqiaoBehavior {

    private KongqiaoBehavior() {}

    public static void bootstrap() {
        DaoHenBehavior.bootstrap();
    }
}

