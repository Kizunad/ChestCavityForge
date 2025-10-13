package net.tigereye.chestcavity.soul.fakeplayer.actions;

import net.tigereye.chestcavity.soul.fakeplayer.actions.core.ForceFightAction;
import net.tigereye.chestcavity.soul.fakeplayer.actions.core.GuardAction;
import net.tigereye.chestcavity.soul.fakeplayer.actions.core.HealingAction;
import net.tigereye.chestcavity.soul.fakeplayer.actions.registry.ActionRegistry;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SoulActions {
    private static final AtomicBoolean BOOT = new AtomicBoolean(false);

    private SoulActions() {}

    public static void bootstrap() {
        if (!BOOT.compareAndSet(false, true)) return;
        ActionRegistry.register(new GuardAction());
        ActionRegistry.register(new ForceFightAction());
        ActionRegistry.register(new HealingAction());
        ActionRegistry.register(new net.tigereye.chestcavity.soul.fakeplayer.actions.core.CultivateAction());
        ActionRegistry.register(new net.tigereye.chestcavity.soul.fakeplayer.actions.core.RefineGuAction());
        ActionRegistry.registerFactory(new net.tigereye.chestcavity.soul.fakeplayer.actions.core.UseItemActionFactory());
        ActionRegistry.registerFactory(new net.tigereye.chestcavity.soul.fakeplayer.actions.core.RefineGuActionFactory());
    }
}
