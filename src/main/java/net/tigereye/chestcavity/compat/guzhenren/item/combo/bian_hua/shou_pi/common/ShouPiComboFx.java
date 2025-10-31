package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.common;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class ShouPiComboFx {
  private ShouPiComboFx() {}

  public static void playCrash(ServerLevel level, double x, double y, double z) {
    level.playSound(null, x, y, z, SoundEvents.ANVIL_FALL, SoundSource.PLAYERS, 0.7F, 1.1F);
  }

  public static void playFasciaLatch(ServerLevel level, double x, double y, double z) {
    level.playSound(
        null,
        x,
        y,
        z,
        SoundEvents.SHIELD_BLOCK,
        SoundSource.PLAYERS,
        0.8F,
        0.95F);
  }

  public static void playStoicRelease(ServerLevel level, double x, double y, double z) {
    level.playSound(
        null,
        x,
        y,
        z,
        SoundEvents.ANVIL_USE,
        SoundSource.PLAYERS,
        0.7F,
        1.05F);
  }
}
