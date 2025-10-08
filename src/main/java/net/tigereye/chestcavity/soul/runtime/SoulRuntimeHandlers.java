package net.tigereye.chestcavity.soul.runtime;

import net.tigereye.chestcavity.soul.registry.SoulRuntimeHandlerRegistry;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SoulRuntimeHandlers {

    private static final AtomicBoolean BOOT = new AtomicBoolean(false);

    private SoulRuntimeHandlers() {}

    public static void bootstrap() {
        if (BOOT.compareAndSet(false, true)) {
            // Reactive hurt must run before default APPLY handler
            SoulRuntimeHandlerRegistry.register(new HurtRetaliateOrFleeHandler());
            SoulRuntimeHandlerRegistry.register(new DefaultSoulRuntimeHandler());
            // Periodic callbacks (per-second / per-minute)
            SoulRuntimeHandlerRegistry.register(new net.tigereye.chestcavity.soul.runtime.SoulPeriodicDispatcher());
            // Opportunistic self-heal (food/potion/golden apples)
            SoulRuntimeHandlerRegistry.register(new net.tigereye.chestcavity.soul.runtime.SelfHealHandler());
            // Minimal AI orders (FOLLOW/IDLE/GUARD)
            SoulRuntimeHandlerRegistry.register(new net.tigereye.chestcavity.soul.ai.SoulAIOrderHandler());
            // Install default combat registries (buff items + guzhenren actives + melee + simple flee)
            net.tigereye.chestcavity.soul.combat.SoulAttackRegistry.register(
                    new net.tigereye.chestcavity.soul.combat.handlers.GuzhenrenBuffItemAttackHandler()
            );
            net.tigereye.chestcavity.soul.combat.SoulAttackRegistry.register(
                    new net.tigereye.chestcavity.soul.combat.handlers.GuzhenrenAttackAbilityHandler()
            );
            net.tigereye.chestcavity.soul.combat.SoulAttackRegistry.register(
                    new net.tigereye.chestcavity.soul.combat.handlers.MeleeAttackHandler()
            );
            net.tigereye.chestcavity.soul.combat.SoulFleeRegistry.register(
                    new net.tigereye.chestcavity.soul.combat.handlers.SimpleFleeHandler()
            );
        }
    }
}
