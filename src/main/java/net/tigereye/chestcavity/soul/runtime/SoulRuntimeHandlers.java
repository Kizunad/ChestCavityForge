package net.tigereye.chestcavity.soul.runtime;

import net.tigereye.chestcavity.soul.registry.SoulRuntimeHandlerRegistry;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 灵魂运行时组件的统一引导入口。
 *
 * <p>在服务器生命周期内调用一次 {@link #bootstrap()}，注册 AI、动作、战斗与周期处理器，确保 FakePlayer
 * 所需的各项子系统全部就绪。</p>
 */
public final class SoulRuntimeHandlers {

    private static final AtomicBoolean BOOT = new AtomicBoolean(false);

    private SoulRuntimeHandlers() {}

    /**
     * 安装所有默认的灵魂运行时处理器。通过 {@link AtomicBoolean} 保证仅执行一次。
     */
    public static void bootstrap() {
        if (BOOT.compareAndSet(false, true)) {
            // Install Actions registry (self-contained autonomy API)
            net.tigereye.chestcavity.soul.fakeplayer.actions.SoulActions.bootstrap();
            // Install unified Brain controller (AUTO/COMBAT orchestration over Actions)
            net.tigereye.chestcavity.soul.registry.SoulRuntimeHandlerRegistry.register(
                    net.tigereye.chestcavity.soul.fakeplayer.brain.BrainController.get()
            );
            // LLM 指令对接
            net.tigereye.chestcavity.soul.registry.SoulRuntimeHandlerRegistry.register(
                    new net.tigereye.chestcavity.soul.runtime.SoulLLMControlHandler()
            );
            // Reactive hurt must run before default APPLY handler
            SoulRuntimeHandlerRegistry.register(new HurtRetaliateOrFleeHandler());
            SoulRuntimeHandlerRegistry.register(new DefaultSoulRuntimeHandler());
            // Periodic callbacks (per-second / per-minute)
            SoulRuntimeHandlerRegistry.register(new net.tigereye.chestcavity.soul.runtime.SoulPeriodicDispatcher());
            // Per-second Guzhenren hooks (e.g., zhuanshu gate, passive cultivation)
            net.tigereye.chestcavity.soul.registry.SoulPeriodicRegistry.registerPerSecond(
                    new net.tigereye.chestcavity.soul.runtime.GuzhenrenZhuanshuSecondHandler()
            );
            net.tigereye.chestcavity.soul.registry.SoulPeriodicRegistry.registerPerSecond(
                    new net.tigereye.chestcavity.soul.runtime.CultivationHandler()
            );
            // Opportunistic self-heal (food/potion/golden apples)
            SoulRuntimeHandlerRegistry.register(new net.tigereye.chestcavity.soul.runtime.SelfHealHandler());
            // Item vacuum (optional, default off)
            SoulRuntimeHandlerRegistry.register(new net.tigereye.chestcavity.soul.runtime.ItemVacuumHandler());
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
        }
    }
}
