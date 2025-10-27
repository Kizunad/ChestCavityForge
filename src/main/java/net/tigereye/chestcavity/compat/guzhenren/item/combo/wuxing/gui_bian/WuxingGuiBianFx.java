package net.tigereye.chestcavity.compat.guzhenren.item.combo.wuxing.gui_bian;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.state.WuxingHuaHenAttachment;

/**
 * 五行归变·逆转 视觉/音效。
 */
public final class WuxingGuiBianFx {

  private WuxingGuiBianFx() {}

  public static void playConversion(ServerPlayer player, WuxingHuaHenAttachment.Element element) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 pos = player.position();
    double x = pos.x;
    double y = pos.y;
    double z = pos.z;

    for (int i = 0; i < 15; i++) {
      double angle = (i / 15.0) * Math.PI * 2;
      double radius = 0.5;
      double offsetX = Math.cos(angle) * radius;
      double offsetZ = Math.sin(angle) * radius;
      level.sendParticles(
          ParticleTypes.ENCHANT, x + offsetX, y + 1.0, z + offsetZ, 1, 0.0, 0.1, 0.0, 0.05);
    }

    switch (element) {
      case JIN -> spawnJinParticles(level, x, y, z);
      case MU -> spawnMuParticles(level, x, y, z);
      case SHUI -> spawnShuiParticles(level, x, y, z);
      case YAN -> spawnYanParticles(level, x, y, z);
      case TU -> spawnTuParticles(level, x, y, z);
    }

    level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.2f);
  }

  public static void playReturn(ServerPlayer player, WuxingHuaHenAttachment.Element element) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 pos = player.position();
    double x = pos.x;
    double y = pos.y;
    double z = pos.z;

    for (int i = 0; i < 12; i++) {
      double angle = (i / 12.0) * Math.PI * 2;
      double radius = 1.2;
      double offsetX = Math.cos(angle) * radius;
      double offsetZ = Math.sin(angle) * radius;
      level.sendParticles(
          ParticleTypes.SOUL,
          x + offsetX,
          y + 0.5,
          z + offsetZ,
          1,
          -offsetX * 0.2,
          0.1,
          -offsetZ * 0.2,
          0.05);
    }

    level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5f, 0.7f);
  }

  private static void spawnJinParticles(ServerLevel level, double x, double y, double z) {
    for (int i = 0; i < 10; i++) {
      level.sendParticles(
          ParticleTypes.ELECTRIC_SPARK,
          x + (Math.random() - 0.5) * 0.6,
          y + 1.2,
          z + (Math.random() - 0.5) * 0.6,
          1,
          0.0,
          0.0,
          0.0,
          0.05);
    }
  }

  private static void spawnMuParticles(ServerLevel level, double x, double y, double z) {
    for (int i = 0; i < 8; i++) {
      level.sendParticles(
          ParticleTypes.HAPPY_VILLAGER,
          x + (Math.random() - 0.5) * 0.6,
          y + 1.2,
          z + (Math.random() - 0.5) * 0.6,
          1,
          0.0,
          0.05,
          0.0,
          0.02);
    }
  }

  private static void spawnShuiParticles(ServerLevel level, double x, double y, double z) {
    for (int i = 0; i < 10; i++) {
      level.sendParticles(
          ParticleTypes.FALLING_WATER,
          x + (Math.random() - 0.5) * 0.6,
          y + 1.5,
          z + (Math.random() - 0.5) * 0.6,
          1,
          0.0,
          -0.1,
          0.0,
          0.0);
    }
  }

  private static void spawnYanParticles(ServerLevel level, double x, double y, double z) {
    for (int i = 0; i < 12; i++) {
      level.sendParticles(
          ParticleTypes.SMALL_FLAME,
          x + (Math.random() - 0.5) * 0.6,
          y + 1.2,
          z + (Math.random() - 0.5) * 0.6,
          1,
          0.0,
          0.1,
          0.0,
          0.03);
    }
  }

  private static void spawnTuParticles(ServerLevel level, double x, double y, double z) {
    for (int i = 0; i < 8; i++) {
      level.sendParticles(
          ParticleTypes.CLOUD,
          x + (Math.random() - 0.5) * 0.6,
          y + 1.0,
          z + (Math.random() - 0.5) * 0.6,
          1,
          0.0,
          0.02,
          0.0,
          0.01);
    }
  }
}

