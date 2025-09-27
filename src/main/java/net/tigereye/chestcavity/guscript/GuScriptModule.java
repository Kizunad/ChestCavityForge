package net.tigereye.chestcavity.guscript;

import net.tigereye.chestcavity.guscript.runtime.action.ActionRegistry;

/**
 * Central bootstrap for GuScript systems.
 */
public final class GuScriptModule {

    private GuScriptModule() {}

    public static void bootstrap() {
        ActionRegistry.registerDefaults();
    }
}
