package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.fx.yu;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/** 负责鱼鳞蛊相关的声音/特效播放。 */
public final class YuLinGuFx {

  private YuLinGuFx() {}

  public static void playArmorReady(ServerLevel level, Player player) {
    if (level == null || player == null) {
      return;
    }
    level.playSound(
        null, player.blockPosition(), SoundEvents.TURTLE_EGG_CRACK, SoundSource.PLAYERS, 0.7f, 1.2f);
  }

  public static void playArmorLost(ServerLevel level, Player player) {
    if (level == null || player == null) {
      return;
    }
    level.playSound(
        null, player.blockPosition(), SoundEvents.FISH_SWIM, SoundSource.PLAYERS, 0.5f, 0.8f);
  }

  public static void playWaterHeal(ServerLevel level, Player player) {
    if (level == null || player == null) {
      return;
    }
    level.playSound(
        null, player.blockPosition(), SoundEvents.DOLPHIN_SPLASH, SoundSource.PLAYERS, 0.6f, 1.0f);
  }
}
