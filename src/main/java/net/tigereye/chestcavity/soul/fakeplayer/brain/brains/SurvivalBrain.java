package net.tigereye.chestcavity.soul.fakeplayer.brain.brains;

import java.util.List;

import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode;
import net.tigereye.chestcavity.soul.fakeplayer.brain.HierarchicalBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.combat.HealingSupportSubBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.survival.SurvivalAssessmentSubBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.survival.SurvivalRetreatSubBrain;

/** Coordinates retreat/avoidance behaviours while keeping healing running. */
public final class SurvivalBrain extends HierarchicalBrain {

    private static final List<SubBrain> PIPELINE = List.of(
            new SurvivalAssessmentSubBrain(),
            new SurvivalRetreatSubBrain(),
            new HealingSupportSubBrain()
    );

    public SurvivalBrain() {
        super("survival", BrainMode.SURVIVAL, PIPELINE);
    }
}
