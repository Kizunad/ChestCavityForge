package net.tigereye.chestcavity.world.spawn;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public record SpawnLocation(ServerLevel level, Vec3 position, float yaw, float pitch) {}
