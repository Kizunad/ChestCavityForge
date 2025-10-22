package net.tigereye.chestcavity.world.spawn;

@FunctionalInterface
public interface SpawnConfigurator {
  void configure(SpawnedMobContext context);
}
