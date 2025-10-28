package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_qun.fx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/** 鱼群·组合 FX：轻量粒子与音效（示范骨架）。 */
public final class YuQunFx {
  private YuQunFx() {}

  public static void playCast(ServerPlayer player) {
    if (player == null) return;
    player.level().playSound(
        null, player.blockPosition(), SoundEvents.SALMON_FLOP, SoundSource.PLAYERS, 0.9f, 1.05f);
  }

  public static void playHit(ServerLevel level, LivingEntity target) {
    if (level == null || target == null) return;
    level.sendParticles(
        ParticleTypes.SPLASH, target.getX(), target.getEyeY(), target.getZ(), 5, 0.2, 0.3, 0.2, 0.02);
  }
}

