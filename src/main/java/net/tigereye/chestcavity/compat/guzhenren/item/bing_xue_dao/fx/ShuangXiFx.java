package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.fx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 霜息蛊：音效与粒子封装 */
public final class ShuangXiFx {
  private ShuangXiFx() {}

  public static void spawnBreathParticles(
      ServerLevel server, Vec3 origin, Vec3 look, Entity focus, int steps, double spacing) {
    Vec3 direction = look.normalize();
    double gap = spacing <= 0.0D ? 0.1D : spacing;
    for (int i = 0; i < Math.max(0, steps); i++) {
      double scale = (i + 1) * gap;
      Vec3 point = origin.add(direction.scale(scale));
      server.sendParticles(
          ParticleTypes.SNOWFLAKE, point.x, point.y, point.z, 6, 0.1, 0.1, 0.1, 0.02);
    }
    if (focus != null) {
      server.sendParticles(
          ParticleTypes.SNOWFLAKE,
          focus.getX(),
          focus.getY() + focus.getBbHeight() * 0.5,
          focus.getZ(),
          12,
          0.25,
          0.25,
          0.25,
          0.04);
    }
  }

  public static void playBreathSound(Level level, LivingEntity entity, boolean hit) {
    float volume = hit ? 0.9f : 0.6f;
    float pitch = hit ? 0.8f : 1.1f;
    level.playSound(
        null,
        entity.getX(),
        entity.getY(),
        entity.getZ(),
        SoundEvents.GENERIC_EXTINGUISH_FIRE,
        SoundSource.PLAYERS,
        volume,
        pitch);
    if (hit) {
      level.playSound(
          null,
          entity.getX(),
          entity.getY(),
          entity.getZ(),
          SoundEvents.GLASS_HIT,
          SoundSource.PLAYERS,
          0.5f,
          1.3f);
    }
  }
}

