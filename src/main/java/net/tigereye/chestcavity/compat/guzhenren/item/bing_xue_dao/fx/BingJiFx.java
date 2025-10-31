package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.fx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/** 冰肌蛊：音效与粒子封装（服务端调用） */
public final class BingJiFx {
  private BingJiFx() {}

  public static void playBurstSound(ServerLevel level, LivingEntity entity) {
    level.playSound(
        null,
        entity.blockPosition(),
        SoundEvents.GENERIC_EXPLODE.value(),
        SoundSource.PLAYERS,
        1.0F,
        1.0F);
  }

  public static void snowflakeBurst(ServerLevel level, double x, double y, double z) {
    level.sendParticles(ParticleTypes.SNOWFLAKE, x, y + 0.2, z, 12, 0.4, 0.2, 0.4, 0.02);
  }
}

