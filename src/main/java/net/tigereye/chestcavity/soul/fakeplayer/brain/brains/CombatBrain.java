package net.tigereye.chestcavity.soul.fakeplayer.brain.brains;

import java.util.List;

import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainMode;
import net.tigereye.chestcavity.soul.fakeplayer.brain.HierarchicalBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.combat.CombatStanceSubBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.combat.HealingSupportSubBrain;

/**
 * Combat brain now explicitly wires the stance and sustain sub-brains together.
 */
public final class CombatBrain extends HierarchicalBrain {

    private static final List<SubBrain> PIPELINE = List.of(
        new CombatStanceSubBrain(),
        new HealingSupportSubBrain()
    );

    public CombatBrain() {
        super("combat", BrainMode.COMBAT, PIPELINE);
    }
}
