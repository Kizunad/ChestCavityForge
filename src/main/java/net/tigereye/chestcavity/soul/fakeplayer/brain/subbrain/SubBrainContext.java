package net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.actions.state.ActionStateManager;
import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainContext;
import net.tigereye.chestcavity.soul.fakeplayer.brain.intent.IntentSnapshot;

/**
 * Wrapper around {@link BrainContext} that provides convenient, sub-brain scoped
 * helpers and a dedicated memory store.
 */
public final class SubBrainContext {

    private final BrainContext brainContext;
    private final SubBrainMemory memory;
    private final SubBrain subBrain;
    private final BrainSharedMemory sharedMemory;

    public SubBrainContext(BrainContext brainContext, SubBrain subBrain, SubBrainMemory memory,
                           BrainSharedMemory sharedMemory) {
        this.brainContext = brainContext;
        this.subBrain = subBrain;
        this.memory = memory;
        this.sharedMemory = sharedMemory;
    }

    public BrainContext brain() { return brainContext; }
    public SubBrainMemory memory() { return memory; }
    public SubBrain subBrain() { return subBrain; }
    public BrainSharedMemory sharedMemory() { return sharedMemory; }

    public ServerLevel level() { return brainContext.level(); }
    public SoulPlayer soul() { return brainContext.soul(); }
    public ServerPlayer owner() { return brainContext.owner(); }
    public ActionStateManager actions() { return brainContext.actions(); }
    public IntentSnapshot intent() { return brainContext.intent(); }
}
