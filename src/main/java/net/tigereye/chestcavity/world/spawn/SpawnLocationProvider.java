package net.tigereye.chestcavity.world.spawn;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;

import java.util.Optional;

@FunctionalInterface
public interface SpawnLocationProvider {
    Optional<SpawnLocation> find(MinecraftServer server, CustomMobSpawnDefinition definition, RandomSource random);
}
