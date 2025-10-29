package net.tigereye.chestcavity.soul.util;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Utilities for orienting SoulPlayer or other living entities. */
public final class SoulLook {
  private SoulLook() {}

  /** Instantly faces the target position with both head and body. */
  public static void faceTowards(LivingEntity entity, Vec3 target) {
    if (entity == null || target == null) {
      return;
    }
    entity.lookAt(EntityAnchorArgument.Anchor.EYES, target);
  }
}
