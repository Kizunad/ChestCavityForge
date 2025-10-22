package net.tigereye.chestcavity.soul.fakeplayer.brain.debug;

import net.minecraft.world.phys.Vec3;

/** 记录探索决策时的关键指标，便于调试。 */
public record ExplorationTelemetry(
    Vec3 target,
    double score,
    String rationale,
    double healthRatio,
    boolean inDanger,
    long gameTime) {}
