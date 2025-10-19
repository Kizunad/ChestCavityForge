package net.tigereye.chestcavity.world.spawn;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

@FunctionalInterface
public interface SpawnValidator {

    SpawnValidator ALWAYS_ALLOW = (definition, level, spawnPos, random) -> true;

    boolean test(CustomMobSpawnDefinition definition, ServerLevel level, Vec3 spawnPos, RandomSource random);
}
