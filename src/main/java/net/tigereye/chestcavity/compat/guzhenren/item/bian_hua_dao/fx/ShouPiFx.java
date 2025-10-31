package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.fx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

public final class ShouPiFx {
  private ShouPiFx() {}

  public static void playDrumSound(ServerLevel level, LivingEntity entity) {
    level.playSound(
        null,
        entity.getX(),
        entity.getY(),
        entity.getZ(),
        SoundEvents.GENERIC_HURT,
        SoundSource.PLAYERS,
        1.0F,
        0.5F);
  }

  public static void playRollSound(ServerLevel level, LivingEntity entity) {
    level.playSound(
        null,
        entity.getX(),
        entity.getY(),
        entity.getZ(),
        SoundEvents.PLAYER_ATTACK_SWEEP,
        SoundSource.PLAYERS,
        1.0F,
        1.0F);
  }

  public static void playCrashSound(ServerLevel level, LivingEntity entity) {
    level.playSound(
        null,
        entity.getX(),
        entity.getY(),
        entity.getZ(),
        SoundEvents.GENERIC_EXPLODE.value(),
        SoundSource.PLAYERS,
        1.0F,
        1.0F);
  }

  public static void drumBurst(ServerLevel level, double x, double y, double z) {
    level.sendParticles(ParticleTypes.CRIT, x, y + 1.0, z, 20, 0.5, 0.5, 0.5, 0.1);
  }

  public static void crashBurst(ServerLevel level, double x, double y, double z) {
    level.sendParticles(ParticleTypes.EXPLOSION, x, y + 0.2, z, 1, 0.0, 0.0, 0.0, 0.0);
  }
}
