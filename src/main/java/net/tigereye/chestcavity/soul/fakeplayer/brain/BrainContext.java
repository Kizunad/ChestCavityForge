package net.tigereye.chestcavity.soul.fakeplayer.brain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.actions.state.ActionStateManager;
import net.tigereye.chestcavity.soul.fakeplayer.brain.intent.IntentSnapshot;
import net.tigereye.chestcavity.soul.fakeplayer.brain.personality.SoulPersonality;

/** Execution context shared by the top-level brain and all nested sub-brains. */
public record BrainContext(
    ServerLevel level,
    SoulPlayer soul,
    ServerPlayer owner,
    ActionStateManager actions,
    IntentSnapshot intent,
    SoulPersonality personality) {}
