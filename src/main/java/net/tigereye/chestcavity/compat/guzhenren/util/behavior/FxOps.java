package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Lightweight FX helpers for behaviors (particles/sounds). Keep simple and side-effect local. */
public final class FxOps {

  private FxOps() {}

  public static void playSound(
      Level level, Vec3 pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
    if (level == null || sound == null || pos == null) {
      return;
    }
    level.playSound(null, pos.x, pos.y, pos.z, sound, source, Math.max(0.0f, volume), pitch);
  }

  public static void particles(
      ServerLevel server,
      ParticleOptions particle,
      Vec3 center,
      int count,
      double dx,
      double dy,
      double dz,
      double speed) {
    if (server == null || particle == null || center == null || count <= 0) {
      return;
    }
    server.sendParticles(particle, center.x, center.y, center.z, count, dx, dy, dz, speed);
  }
}
