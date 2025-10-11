package net.tigereye.chestcavity.soul.fakeplayer.brain.brains;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.soul.fakeplayer.brain.Brain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainContext;
import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode;
import net.tigereye.chestcavity.soul.fakeplayer.actions.registry.ActionRegistry;
import net.tigereye.chestcavity.soul.fakeplayer.brain.intent.CombatIntent;
import net.tigereye.chestcavity.soul.fakeplayer.brain.intent.CombatStyle;

/**
 * Minimal combat brain: ensures ForceFightAction is active and HealingAction runs concurrently.
 * Detailed combat movement/evade logic is encapsulated inside ForceFightAction/SoulCombatOps.
 */
public final class CombatBrain implements Brain {

    private static final ResourceLocation FORCE_FIGHT = ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "action/force_fight");
    private static final ResourceLocation GUARD = ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "action/guard");
    private static final ResourceLocation HEAL = ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "action/heal");

    @Override public String id() { return "combat"; }
    @Override public BrainMode mode() { return BrainMode.COMBAT; }

    @Override public void onEnter(BrainContext ctx) { /* no-op */ }
    @Override public void onExit(BrainContext ctx) { /* no-op */ }

    @Override
    public void tick(BrainContext ctx) {
        var mgr = ctx.actions();
        // 优先读取 IntentSnapshot（兼容：无意图时退回旧 Order）
        ResourceLocation desired;
        if (ctx.intent() != null && ctx.intent().isPresent() && ctx.intent().intent() instanceof CombatIntent ci) {
            desired = (ci.style() == CombatStyle.GUARD) ? GUARD : FORCE_FIGHT;
        } else {
            var order = net.tigereye.chestcavity.soul.ai.SoulAIOrders.get(ctx.soul().getSoulId());
            desired = (order == net.tigereye.chestcavity.soul.ai.SoulAIOrders.Order.GUARD) ? GUARD : FORCE_FIGHT;
        }
        var act = ActionRegistry.find(desired);
        if (act != null && !mgr.isActive(act.id()) && act.canRun(new net.tigereye.chestcavity.soul.fakeplayer.actions.api.ActionContext(ctx.level(), ctx.soul(), ctx.owner()))) {
            mgr.tryStart(ctx.level(), ctx.soul(), act, ctx.owner());
        }
        var heal = ActionRegistry.find(HEAL);
        if (heal != null && !mgr.isActive(heal.id()) && heal.canRun(new net.tigereye.chestcavity.soul.fakeplayer.actions.api.ActionContext(ctx.level(), ctx.soul(), ctx.owner()))) {
            mgr.tryStart(ctx.level(), ctx.soul(), heal, ctx.owner());
        }
    }
}
