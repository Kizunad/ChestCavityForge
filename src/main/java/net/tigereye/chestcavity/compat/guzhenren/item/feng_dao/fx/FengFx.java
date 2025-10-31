package net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.fx;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.tuning.FengTuning;
import org.joml.Vector3f;

public final class FengFx {
  private FengFx() {}

  public static void runMilestoneFx(ServerLevel level, Player player, Vec3 dir) {
    if (!FengTuning.FX_ENABLED) return;
    if (FengTuning.SOUND_ENABLED) {
      try {
        level.playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.WIND_CHARGE_BURST.value(),
            SoundSource.PLAYERS,
            0.28F,
            1.05F);
      } catch (Throwable ignored) {
        level.playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.ELYTRA_FLYING,
            SoundSource.PLAYERS,
            0.22F,
            1.25F);
      }
    }
    if (FengTuning.PARTICLE_ENABLED) {
      Vec3 base = player.position().add(0.0D, 0.1D, 0.0D);
      for (int i = 0; i < 8; i++) {
        double t = 0.12D * i;
        double ox = -dir.x * t + (level.random.nextDouble() - 0.5D) * 0.15D;
        double oz = -dir.z * t + (level.random.nextDouble() - 0.5D) * 0.15D;
        double px = base.x + ox;
        double py = base.y + 0.02D * i;
        double pz = base.z + oz;
        level.sendParticles(ParticleTypes.CLOUD, px, py, pz, 1, 0.0D, 0.01D, 0.0D, 0.01D);
        level.sendParticles(
            new DustColorTransitionOptions(
                new Vector3f(0.75F, 0.90F, 1.00F), new Vector3f(1.00F, 1.00F, 1.00F), 0.55F),
            px,
            py + 0.03D,
            pz,
            1,
            0.0D,
            0.0D,
            0.0D,
            0.0D);
      }
    }
  }

  public static void ringBlockFx(ServerLevel level, Vec3 pos) {
    if (!FengTuning.FX_ENABLED) return;
    if (FengTuning.PARTICLE_ENABLED) {
      for (int i = 0; i < 12; i++) {
        double angle = (Math.PI * 2 * i) / 12.0D;
        double radius = 1.1D;
        double x = pos.x + Math.cos(angle) * radius;
        double z = pos.z + Math.sin(angle) * radius;
        level.sendParticles(ParticleTypes.CLOUD, x, pos.y + 1.0D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
      }
    }
    if (FengTuning.SOUND_ENABLED) {
      level.playSound(
          null,
          pos.x,
          pos.y,
          pos.z,
          SoundEvents.WIND_CHARGE_BURST,
          SoundSource.PLAYERS,
          0.8F,
          1.0F);
    }
  }

  public static void dashTrailFx(ServerLevel level, Vec3 start, Vec3 end) {
    if (!FengTuning.FX_ENABLED) return;
    if (FengTuning.PARTICLE_ENABLED) {
      Vec3 delta = end.subtract(start);
      int steps = 12;
      for (int i = 0; i < steps; i++) {
        double t = i / (double) steps;
        Vec3 pos = start.add(delta.scale(t));
        level.sendParticles(
            ParticleTypes.CLOUD, pos.x, pos.y + 0.1D, pos.z, 4, 0.0D, 0.0D, 0.0D, 0.01D);
      }
    }
    if (FengTuning.SOUND_ENABLED) {
      level.playSound(
          null,
          start.x,
          start.y,
          start.z,
          SoundEvents.ELYTRA_FLYING,
          SoundSource.PLAYERS,
          0.6F,
          1.2F);
    }
  }

  public static void windSlashFx(ServerLevel level, Vec3 origin) {
    if (!FengTuning.FX_ENABLED || !FengTuning.SOUND_ENABLED) return;
    level.playSound(
        null,
        origin.x,
        origin.y,
        origin.z,
        SoundEvents.PLAYER_ATTACK_SWEEP,
        SoundSource.PLAYERS,
        0.8F,
        1.3F);
  }
}
