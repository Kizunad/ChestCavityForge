package net.tigereye.chestcavity.soul.fakeplayer.brain.brains;

import java.util.List;

import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode;
import net.tigereye.chestcavity.soul.fakeplayer.brain.HierarchicalBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.utility.FollowSubBrain;

/**
 * Idle/utility brain that owns non-combat behaviours like FOLLOW.
 */
public final class IdleBrain extends HierarchicalBrain {

    private static final List<SubBrain> PIPELINE = List.of(
        new FollowSubBrain()
    );

    public IdleBrain() {
        super("idle", BrainMode.IDLE, PIPELINE);
    }
}

