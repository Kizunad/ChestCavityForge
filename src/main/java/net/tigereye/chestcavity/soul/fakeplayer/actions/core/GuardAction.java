package net.tigereye.chestcavity.soul.fakeplayer.actions.core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.soul.ai.SoulAIOrders;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.actions.api.*;
import net.tigereye.chestcavity.soul.fakeplayer.actions.registry.ActionRegistry;

/**
 * Wrapper action for GUARD order. Keeps the AI order set and runs continuously.
 * Exclusive by default (does not allow concurrent actions that also require navigation control).
 */
public final class GuardAction implements Action {
    private static final ActionId ID = new ActionId(ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "action/guard"));

    @Override public ActionId id() { return ID; }
    @Override public ActionPriority priority() { return ActionPriority.HIGH; }
    @Override public boolean allowConcurrent() { return false; }

    @Override
    public boolean canRun(ActionContext ctx) {
        return ctx.owner() != null && ctx.soul().isAlive();
    }

    @Override
    public void start(ActionContext ctx) {
        applyOrder(ctx.level(), ctx.soul(), ctx.owner(), SoulAIOrders.Order.GUARD);
        // Opportunistically ensure concurrent healing is running if needed
        tryStartHealing(ctx);
    }

    @Override
    public ActionResult tick(ActionContext ctx) {
        // Reassert order to be safe; AI handler executes actual logic.
        applyOrder(ctx.level(), ctx.soul(), ctx.owner(), SoulAIOrders.Order.GUARD);
        // Keep healing running while guarding when below threshold
        tryStartHealing(ctx);
        return ActionResult.RUNNING;
    }

    @Override
    public void cancel(ActionContext ctx) {
        // Drop to IDLE only if we were the one holding GUARD; simpler: always set IDLE.
        applyOrder(ctx.level(), ctx.soul(), ctx.owner(), SoulAIOrders.Order.IDLE);
    }

    @Override
    public String cooldownKey() { return null; }

    @Override
    public long nextReadyAt(ActionContext ctx, long now) { return now + 20; }

    private static void applyOrder(ServerLevel level, SoulPlayer soul, ServerPlayer owner, SoulAIOrders.Order order) {
        if (owner == null) return;
        SoulAIOrders.set(owner, soul.getSoulId(), order, "action-guard");
    }

    private static void tryStartHealing(ActionContext ctx) {
        var heal = ActionRegistry.find(ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "action/heal"));
        if (heal == null) return;
        var mgr = net.tigereye.chestcavity.soul.fakeplayer.actions.state.ActionStateManager.of(ctx.soul());
        if (mgr.isActive(heal.id())) return;
        if (!heal.canRun(ctx)) return;
        mgr.tryStart(ctx.level(), ctx.soul(), heal, ctx.owner());
    }
}
