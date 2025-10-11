package net.tigereye.chestcavity.soul.fakeplayer.actions.core;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.soul.fakeplayer.actions.api.*;
import net.tigereye.chestcavity.soul.runtime.SelfHealHandler;

/**
 * Opportunistic healing: attempts to use a healing item once per schedule until HP meets threshold.
 * Can run concurrently with other actions (does not directly control navigation).
 */
public final class HealingAction implements Action {
    private static final ActionId ID = new ActionId(ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "action/heal"));

    // Defaults mirror SelfHealHandler thresholds; can be moved to config later
    private static final float HEALTH_FRAC_TARGET = 0.80f; // stop early once >= 80%
    private static final int RECHECK_TICKS = 20; // once per second

    @Override public ActionId id() { return ID; }
    @Override public ActionPriority priority() { return ActionPriority.EMERGENCY; }
    @Override public boolean allowConcurrent() { return true; }

    @Override
    public boolean canRun(ActionContext ctx) {
        float hp = ctx.soul().getHealth();
        float max = ctx.soul().getMaxHealth();
        return max > 0 && hp / max < HEALTH_FRAC_TARGET && ctx.soul().isAlive();
    }

    @Override
    public void start(ActionContext ctx) {
        // no-op; we attempt on tick
    }

    @Override
    public ActionResult tick(ActionContext ctx) {
        float hp = ctx.soul().getHealth();
        float max = ctx.soul().getMaxHealth();
        if (max <= 0) return ActionResult.FAILED;
        if (hp / max >= HEALTH_FRAC_TARGET) return ActionResult.SUCCESS;
        // Attempt once; success or not, we’ll reschedule
        SelfHealHandler.tryUseAnyHealingItem(ctx.soul());
        return ActionResult.RUNNING;
    }

    @Override
    public void cancel(ActionContext ctx) { /* no-op */ }

    @Override
    public String cooldownKey() { return null; }

    @Override
    public long nextReadyAt(ActionContext ctx, long now) { return now + RECHECK_TICKS; }
}

