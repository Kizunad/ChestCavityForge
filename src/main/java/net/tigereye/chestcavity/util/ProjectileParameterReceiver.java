package net.tigereye.chestcavity.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/** Marker interface for projectiles that can be configured via GuScript before being spawned. */
public interface ProjectileParameterReceiver {

  void applyProjectileParameters(
      @Nullable LivingEntity owner, CompoundTag parameters, double baseDamage);
}
