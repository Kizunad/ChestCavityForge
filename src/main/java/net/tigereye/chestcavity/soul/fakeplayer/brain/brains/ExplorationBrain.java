package net.tigereye.chestcavity.soul.fakeplayer.brain.brains;

import java.util.List;

import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode;
import net.tigereye.chestcavity.soul.fakeplayer.brain.HierarchicalBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrain;

/**
 * 探索子大脑的占位实现。真正的探索行为将在后续串行步骤中填充。
 * 由于 BrainMode 目前尚未暴露专门的探索枚举，这里暂时
 * 以 IDLE 作为占位模式，保持编译通过与后续兼容性。
 */
public final class ExplorationBrain extends HierarchicalBrain {

    private static final List<SubBrain> PIPELINE = List.of();

    public ExplorationBrain() {
        super("exploration", BrainMode.IDLE, PIPELINE);
    }
}
