package net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.combat;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.soul.ai.SoulAIOrders;
import net.tigereye.chestcavity.soul.fakeplayer.actions.api.ActionContext;
import net.tigereye.chestcavity.soul.fakeplayer.actions.registry.ActionRegistry;
import net.tigereye.chestcavity.soul.fakeplayer.brain.intent.CombatIntent;
import net.tigereye.chestcavity.soul.fakeplayer.brain.intent.CombatStyle;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrainContext;

/**
 * Maintains the primary combat stance action (force fight or guard) based on the
 * most up-to-date intent snapshot and legacy order system.
 */
public final class CombatStanceSubBrain implements SubBrain {

    private static final ResourceLocation FORCE_FIGHT = ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "action/force_fight");
    private static final ResourceLocation GUARD = ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "action/guard");

    @Override public String id() { return "combat.stance"; }

    @Override
    public void tick(SubBrainContext ctx) {
        var mgr = ctx.actions();
        ResourceLocation desired = resolveDesiredAction(ctx);
        var action = ActionRegistry.find(desired);
        if (action == null) {
            return;
        }
        if (mgr.isActive(action.id())) {
            return;
        }
        var actionCtx = new ActionContext(ctx.level(), ctx.soul(), ctx.owner());
        if (action.canRun(actionCtx)) {
            mgr.tryStart(ctx.level(), ctx.soul(), action, ctx.owner());
        }
    }

    private ResourceLocation resolveDesiredAction(SubBrainContext ctx) {
        if (ctx.intent() != null && ctx.intent().isPresent() && ctx.intent().intent() instanceof CombatIntent combatIntent) {
            return combatIntent.style() == CombatStyle.GUARD ? GUARD : FORCE_FIGHT;
        }
        SoulAIOrders.Order order = SoulAIOrders.get(ctx.soul().getSoulId());
        return order == SoulAIOrders.Order.GUARD ? GUARD : FORCE_FIGHT;
    }
}
