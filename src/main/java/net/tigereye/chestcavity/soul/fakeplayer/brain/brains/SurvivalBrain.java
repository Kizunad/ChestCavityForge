package net.tigereye.chestcavity.soul.fakeplayer.brain.brains;

import java.util.List;

import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode;
import net.tigereye.chestcavity.soul.fakeplayer.brain.HierarchicalBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrain;

/**
 * 占位式求生子大脑。当前仍未接线具体的求生 SubBrain，
 * 但需要占位以便后续串行工作能够直接挂载行为。
 */
public final class SurvivalBrain extends HierarchicalBrain {

    private static final List<SubBrain> PIPELINE = List.of();

    public SurvivalBrain() {
        super("survival", BrainMode.SURVIVAL, PIPELINE);
    }
}
