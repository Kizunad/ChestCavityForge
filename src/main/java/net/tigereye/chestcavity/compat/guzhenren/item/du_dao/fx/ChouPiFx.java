package net.tigereye.chestcavity.compat.guzhenren.item.du_dao.fx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.du_dao.tuning.ChouPiTuning;

public final class ChouPiFx {
  private ChouPiFx() {}

  public static void playSounds(Level level, LivingEntity entity, RandomSource random) {
    double x = entity.getX();
    double y = entity.getY();
    double z = entity.getZ();
    float puffPitch = 0.65f + random.nextFloat() * 0.15f;
    float squishPitch = 0.5f + random.nextFloat() * 0.2f;
    level.playSound(
        null, x, y, z, SoundEvents.PUFFER_FISH_BLOW_UP, SoundSource.PLAYERS, 0.9f, puffPitch);
    level.playSound(
        null, x, y, z, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.6f, squishPitch);
  }

  public static void spawnParticles(ServerLevel level, LivingEntity entity, RandomSource random) {
    Vec3 look = entity.getLookAngle();
    Vec3 back = look.normalize().scale(-ChouPiTuning.PARTICLE_BACK_OFFSET);
    Vec3 base =
        entity
            .position()
            .add(back)
            .add(0.0, (double) ChouPiTuning.PARTICLE_VERTICAL_OFFSET, 0.0);
    Vec3 lateral = new Vec3(look.z, 0.0, -look.x);
    if (lateral.lengthSqr() < 1.0E-4) {
      lateral = new Vec3(1.0, 0.0, 0.0);
    }
    lateral = lateral.normalize();

    for (int i = 0; i < ChouPiTuning.PARTICLE_SMOKE_COUNT; i++) {
      double sideways = (random.nextDouble() - 0.5) * 0.8;
      double vertical = (random.nextDouble() - 0.5) * 0.2;
      Vec3 offset = lateral.scale(sideways).add(0.0, vertical, 0.0);
      Vec3 pos = base.add(offset);
      double speed = 0.02 + random.nextDouble() * 0.02;
      level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, speed);
    }
    level.sendParticles(
        ParticleTypes.SNEEZE,
        base.x,
        base.y,
        base.z,
        ChouPiTuning.PARTICLE_SNEEZE_COUNT,
        0.35,
        0.15,
        0.35,
        0.01);
    level.sendParticles(ParticleTypes.ASH, base.x, base.y, base.z, 3, 0.2, 0.1, 0.2, 0.005);
  }
}

