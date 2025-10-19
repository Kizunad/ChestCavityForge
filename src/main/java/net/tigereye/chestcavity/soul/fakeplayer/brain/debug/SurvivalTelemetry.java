package net.tigereye.chestcavity.soul.fakeplayer.brain.debug;

import net.minecraft.world.phys.Vec3;

/**
 * 生存模式中的逃生/防御决策快照。
 */
public record SurvivalTelemetry(Vec3 safePoint, double healthRatio, boolean retreated, long gameTime) {
}
