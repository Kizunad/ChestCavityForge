package net.tigereye.chestcavity.soul.fakeplayer.brain.brains;

import java.util.List;

import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode;
import net.tigereye.chestcavity.soul.fakeplayer.brain.HierarchicalBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.exploration.ExplorationPatrolSubBrain;

/** Owner-centric wandering brain used for exploration loops. */
public final class ExplorationBrain extends HierarchicalBrain {

    private static final List<SubBrain> PIPELINE = List.of(
            new ExplorationPatrolSubBrain()
    );

    public ExplorationBrain() {
        super("exploration", BrainMode.IDLE, PIPELINE);
    }
}
