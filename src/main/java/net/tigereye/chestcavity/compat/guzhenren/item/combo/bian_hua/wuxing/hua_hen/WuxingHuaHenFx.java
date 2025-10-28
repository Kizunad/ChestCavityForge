package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.hua_hen;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.wuxing.hua_hen.state.WuxingHuaHenAttachment.Element;

/**
 * 五行化痕 视觉/音效。
 */
public final class WuxingHuaHenFx {

  private WuxingHuaHenFx() {}

  public static void playTransmute(ServerPlayer player, Element element) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 pos = player.position();
    double x = pos.x;
    double y = pos.y;
    double z = pos.z;

    for (int i = 0; i < 10; i++) {
      double angle = (i / 10.0) * Math.PI * 2;
      double radius = 0.3 + Math.random() * 0.3;
      double offsetX = Math.cos(angle) * radius;
      double offsetZ = Math.sin(angle) * radius;
      level.sendParticles(
          ParticleTypes.ENCHANT,
          x + offsetX,
          y + 1.2,
          z + offsetZ,
          1,
          offsetX * 0.5,
          0.05,
          offsetZ * 0.5,
          0.1);
    }

    Vec3 lookDir = player.getLookAngle();
    double handX = x + lookDir.x * 0.8;
    double handY = y + 1.3 + lookDir.y * 0.8;
    double handZ = z + lookDir.z * 0.8;

    switch (element) {
      case JIN -> spawnJinParticles(level, handX, handY, handZ);
      case MU -> spawnMuParticles(level, handX, handY, handZ);
      case SHUI -> spawnShuiParticles(level, handX, handY, handZ);
      case YAN -> spawnYanParticles(level, handX, handY, handZ);
      case TU -> spawnTuParticles(level, handX, handY, handZ);
    }

    float pitch = 1.4f + element.ordinal() * 0.1f;
    level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, pitch);
  }

  public static void playUndo(ServerPlayer player, Element element) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 pos = player.position();
    double x = pos.x;
    double y = pos.y;
    double z = pos.z;

    for (int i = 0; i < 20; i++) {
      double angle = (i / 20.0) * Math.PI * 2;
      double radius = 1.5;
      double offsetX = Math.cos(angle) * radius;
      double offsetZ = Math.sin(angle) * radius;
      level.sendParticles(
          ParticleTypes.ENCHANT,
          x + offsetX,
          y + 1.0,
          z + offsetZ,
          1,
          -offsetX * 0.2,
          0.0,
          -offsetZ * 0.2,
          0.1);
    }

    for (int i = 0; i < 10; i++) {
      double offsetX = (Math.random() - 0.5) * 0.8;
      double offsetZ = (Math.random() - 0.5) * 0.8;
      level.sendParticles(
          ParticleTypes.SOUL,
          x + offsetX,
          y + 0.5,
          z + offsetZ,
          1,
          0.0,
          0.15,
          0.0,
          0.05);
    }

    level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5f, 0.8f);
  }

  private static void spawnJinParticles(ServerLevel level, double x, double y, double z) {
    for (int i = 0; i < 15; i++) {
      double offsetX = (Math.random() - 0.5) * 0.4;
      double offsetY = (Math.random() - 0.5) * 0.4;
      double offsetZ = (Math.random() - 0.5) * 0.4;
      level.sendParticles(
          ParticleTypes.ELECTRIC_SPARK,
          x + offsetX,
          y + offsetY,
          z + offsetZ,
          1,
          0.0,
          0.0,
          0.0,
          0.05);
    }
  }

  private static void spawnMuParticles(ServerLevel level, double x, double y, double z) {
    for (int i = 0; i < 12; i++) {
      double offsetX = (Math.random() - 0.5) * 0.4;
      double offsetY = (Math.random() - 0.5) * 0.4;
      double offsetZ = (Math.random() - 0.5) * 0.4;
      level.sendParticles(
          ParticleTypes.HAPPY_VILLAGER,
          x + offsetX,
          y + offsetY,
          z + offsetZ,
          1,
          0.0,
          0.05,
          0.0,
          0.02);
    }
  }

  private static void spawnShuiParticles(ServerLevel level, double x, double y, double z) {
    for (int i = 0; i < 15; i++) {
      double offsetX = (Math.random() - 0.5) * 0.4;
      double offsetY = Math.random() * 0.5;
      double offsetZ = (Math.random() - 0.5) * 0.4;
      level.sendParticles(
          ParticleTypes.FALLING_WATER,
          x + offsetX,
          y + offsetY,
          z + offsetZ,
          1,
          0.0,
          -0.1,
          0.0,
          0.0);
    }
  }

  private static void spawnYanParticles(ServerLevel level, double x, double y, double z) {
    for (int i = 0; i < 18; i++) {
      double offsetX = (Math.random() - 0.5) * 0.4;
      double offsetY = (Math.random() - 0.5) * 0.4;
      double offsetZ = (Math.random() - 0.5) * 0.4;
      level.sendParticles(
          ParticleTypes.SMALL_FLAME,
          x + offsetX,
          y + offsetY,
          z + offsetZ,
          1,
          0.0,
          0.1,
          0.0,
          0.03);
    }
  }

  private static void spawnTuParticles(ServerLevel level, double x, double y, double z) {
    for (int i = 0; i < 12; i++) {
      double offsetX = (Math.random() - 0.5) * 0.5;
      double offsetY = (Math.random() - 0.5) * 0.3;
      double offsetZ = (Math.random() - 0.5) * 0.5;
      level.sendParticles(
          ParticleTypes.CLOUD,
          x + offsetX,
          y + offsetY,
          z + offsetZ,
          1,
          0.0,
          0.02,
          0.0,
          0.01);
    }
    for (int i = 0; i < 6; i++) {
      double offsetX = (Math.random() - 0.5) * 0.4;
      double offsetY = (Math.random() - 0.5) * 0.3;
      double offsetZ = (Math.random() - 0.5) * 0.4;
      level.sendParticles(
          ParticleTypes.CRIT,
          x + offsetX,
          y + offsetY,
          z + offsetZ,
          1,
          0.0,
          0.0,
          0.0,
          0.02);
    }
  }
}
