package net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.combat;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.soul.fakeplayer.actions.api.ActionContext;
import net.tigereye.chestcavity.soul.fakeplayer.actions.registry.ActionRegistry;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.BrainActionStep;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrainContext;

/** Ensures the passive healing routine is running while in combat. */
public final class HealingSupportSubBrain extends SubBrain {

  private static final ResourceLocation HEAL =
      ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "action/heal");

  public HealingSupportSubBrain() {
    super("combat.heal");
    addStep(BrainActionStep.always(this::ensureHealingAction));
  }

  @Override
  public boolean shouldTick(SubBrainContext ctx) {
    return ActionRegistry.find(HEAL) != null;
  }

  private void ensureHealingAction(SubBrainContext ctx) {
    var action = ActionRegistry.find(HEAL);
    if (action == null) {
      return;
    }
    var mgr = ctx.actions();
    if (mgr.isActive(action.id())) {
      return;
    }
    var actionCtx = new ActionContext(ctx.level(), ctx.soul(), ctx.owner());
    if (action.canRun(actionCtx)) {
      mgr.tryStart(ctx.level(), ctx.soul(), action, ctx.owner());
    }
  }
}
