package net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.utility;

import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.ai.SoulAIOrders;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.BrainActionStep;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrainContext;
import net.tigereye.chestcavity.soul.navigation.SoulNavigationMirror;
import net.tigereye.chestcavity.soul.util.SoulLook;
import net.tigereye.chestcavity.soul.navigation.SoulNavigationMirror.GoalPriority;

/**
 * Simple follow behaviour: keep within a small radius of the owner while ORDER == FOLLOW.
 */
public final class FollowSubBrain extends SubBrain {

    private static final double FOLLOW_TRIGGER_DIST = 5.0; // blocks
    private static final double STOP_DIST = 2.0;            // blocks
    private static final double SPEED = 1.2;               // nav speed modifier

    public FollowSubBrain() {
        super("utility.follow");
        addStep(BrainActionStep.always(this::tickFollow));
    }

    @Override
    public boolean shouldTick(SubBrainContext ctx) {
        if (ctx.owner() == null || !ctx.soul().isAlive()) return false;
        return SoulAIOrders.get(ctx.owner(), ctx.soul().getSoulId()) == SoulAIOrders.Order.FOLLOW;
    }

    private void tickFollow(SubBrainContext ctx) {
        var owner = ctx.owner();
        var soul = ctx.soul();
        if (owner == null) return;
        double dist = soul.distanceTo(owner);
        if (dist > FOLLOW_TRIGGER_DIST) {
            Vec3 target = owner.position();
            SoulNavigationMirror.setGoal(soul, target, SPEED, STOP_DIST, GoalPriority.LOW);
        } else {
            SoulNavigationMirror.clearGoal(soul);
        }
        SoulLook.faceTowards(soul, owner.position());
    }
}
