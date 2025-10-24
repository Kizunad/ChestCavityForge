package net.tigereye.chestcavity.world.spawn;

import net.minecraft.world.entity.ai.Brain;

@FunctionalInterface
public interface BrainConfigurator {
  void configure(SpawnedMobContext context, Brain<?> brain);
}
