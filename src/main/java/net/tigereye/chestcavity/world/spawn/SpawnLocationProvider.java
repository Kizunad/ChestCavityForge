package net.tigereye.chestcavity.world.spawn;

import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;

@FunctionalInterface
public interface SpawnLocationProvider {
  Optional<SpawnLocation> find(
      MinecraftServer server, CustomMobSpawnDefinition definition, RandomSource random);
}
