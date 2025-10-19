package net.tigereye.chestcavity.soul.fakeplayer.brain.brains;

import java.util.List;

import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode;
import net.tigereye.chestcavity.soul.fakeplayer.brain.HierarchicalBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.llm.LLMDrivenSubBrain;

/**
 * 专用于 LLM 驱动模式的脑实现，当前仅串接一个指令消费子脑，未来可追加动作调度。 */
public final class LLMBrain extends HierarchicalBrain {

    private static final List<SubBrain> PIPELINE = List.of(
            new LLMDrivenSubBrain()
    );

    public LLMBrain() {
        super("llm", BrainMode.LLM, PIPELINE);
    }
}

