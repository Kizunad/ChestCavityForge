package net.tigereye.chestcavity.soul.fakeplayer.brain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.actions.state.ActionStateManager;
import net.tigereye.chestcavity.soul.fakeplayer.brain.intent.IntentSnapshot;

/**
 * Execution context for a sub-brain: provides convenient access to world, actors and
 * the action state manager for orchestration.
 */
public record BrainContext(ServerLevel level, SoulPlayer soul, ServerPlayer owner,
                           ActionStateManager actions, IntentSnapshot intent) {
}
