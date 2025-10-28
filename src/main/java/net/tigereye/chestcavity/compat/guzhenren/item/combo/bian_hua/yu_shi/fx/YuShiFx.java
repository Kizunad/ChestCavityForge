package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_shi.fx;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

public final class YuShiFx {
  private YuShiFx() {}

  public static void playCast(ServerPlayer player) {
    if (player == null) return;
    player.level().playSound(
        null, player.blockPosition(), SoundEvents.DOLPHIN_SPLASH, SoundSource.PLAYERS, 1.0f, 0.9f);
  }

  public static void playSummoned(ServerLevel level, LivingEntity e) {
    // 预留：后续可加水花/气泡
  }
}

