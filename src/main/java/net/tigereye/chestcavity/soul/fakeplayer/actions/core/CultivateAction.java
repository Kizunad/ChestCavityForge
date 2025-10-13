package net.tigereye.chestcavity.soul.fakeplayer.actions.core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.soul.fakeplayer.actions.api.*;
import net.tigereye.chestcavity.soul.fakeplayer.actions.state.ActionStateManager;
import net.tigereye.chestcavity.soul.navigation.SoulNavigationMirror;

import java.util.Optional;
import java.util.OptionalDouble;

/** 主动修炼动作：每秒扣 10 真元并+10 修炼进度，直至真元不足。 */
public final class CultivateAction implements Action {

    private static final ActionId ID = new ActionId(ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "action/cultivate_active"));
    private static final int INTERVAL_TICKS = 20;
    private static final double COST_PER_SECOND = 10.0;
    private static final double PROGRESS_PER_SECOND = 10.0;

    @Override public ActionId id() { return ID; }
    @Override public ActionPriority priority() { return ActionPriority.HIGH; }
    @Override public boolean allowConcurrent() { return false; }

    @Override
    public boolean canRun(ActionContext ctx) {
        return ctx.owner() != null && ctx.soul().isAlive();
    }

    @Override
    public void start(ActionContext ctx) {
        var mgr = ActionStateManager.of(ctx.soul());
        for (var rt : new java.util.ArrayList<>(mgr.active())) {
            var action = net.tigereye.chestcavity.soul.fakeplayer.actions.registry.ActionRegistry.find(rt.id);
            if (action != null && !ID.equals(action.id())) {
                mgr.cancel(ctx.level(), ctx.soul(), action, ctx.owner());
            }
        }
        SoulNavigationMirror.clearGoal(ctx.soul());
    }

    @Override
    public ActionResult tick(ActionContext ctx) {
        ServerPlayer owner = ctx.owner();
        if (owner == null) return ActionResult.FAILED;
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(owner);
        if (handleOpt.isEmpty()) return ActionResult.FAILED;
        var handle = handleOpt.get();
        OptionalDouble after = net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps
                .tryConsumeScaledZhenyuan(handle, COST_PER_SECOND);
        if (after.isEmpty()) return ActionResult.SUCCESS;
        net.tigereye.chestcavity.guzhenren.util.CultivationHelper.tickProgress(handle, PROGRESS_PER_SECOND);
        return ActionResult.RUNNING;
    }

    @Override
    public void cancel(ActionContext ctx) { }

    @Override
    public String cooldownKey() { return null; }

    @Override
    public long nextReadyAt(ActionContext ctx, long now) { return now + INTERVAL_TICKS; }

    private static void tryPromoteStage(GuzhenrenResourceBridge.ResourceHandle handle) { /* moved to CultivationHelper */ }
}
