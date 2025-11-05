package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ops;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.registration.CCSoundEvents;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordSoundTuning;

/**
 * 飞剑音效操作封装。
 */
public final class SoundOps {
  private SoundOps() {}

  private static float pitch() {
    float base = FlyingSwordSoundTuning.PITCH_BASE;
    float var = FlyingSwordSoundTuning.PITCH_VAR;
    if (var <= 0) return base;
    return base + (float) (ThreadLocalRandom.current().nextDouble(-var, var));
  }

  private static void play(Level level, Vec3 pos, net.neoforged.neoforge.registries.DeferredHolder<?, ?> evt,
      float vol) {
    if (!FlyingSwordSoundTuning.ENABLE_SOUNDS || level == null || evt == null) return;
    if (!(evt.get() instanceof net.minecraft.sounds.SoundEvent s)) return;
    level.playSound(null, pos.x, pos.y, pos.z, s, SoundSource.PLAYERS, vol, pitch());
  }

  public static void playSpawn(FlyingSwordEntity sword) {
    if (!FlyingSwordSoundTuning.PLAY_SPAWN) return;
    play(sword.level(), sword.position(), CCSoundEvents.CUSTOM_FLYINGSWORD_SPAWN, FlyingSwordSoundTuning.VOL_SPAWN);
  }

  public static void playRecall(FlyingSwordEntity sword) {
    if (!FlyingSwordSoundTuning.PLAY_RECALL) return;
    play(sword.level(), sword.position(), CCSoundEvents.CUSTOM_FLYINGSWORD_RECALL, FlyingSwordSoundTuning.VOL_RECALL);
  }

  public static void playSwing(FlyingSwordEntity sword) {
    if (!FlyingSwordSoundTuning.PLAY_SWING) return;
    play(sword.level(), sword.position(), CCSoundEvents.CUSTOM_FLYINGSWORD_SWING, FlyingSwordSoundTuning.VOL_SWING);
  }

  public static void playHit(FlyingSwordEntity sword) {
    if (!FlyingSwordSoundTuning.PLAY_HIT) return;
    play(sword.level(), sword.position(), CCSoundEvents.CUSTOM_FLYINGSWORD_HIT, FlyingSwordSoundTuning.VOL_HIT);
  }

  public static void playBlockBreak(FlyingSwordEntity sword) {
    if (!FlyingSwordSoundTuning.PLAY_BLOCK_BREAK) return;
    play(sword.level(), sword.position(), CCSoundEvents.CUSTOM_FLYINGSWORD_BLOCK_BREAK,
        FlyingSwordSoundTuning.VOL_BLOCK_BREAK);
  }

  public static void playOutOfEnergy(FlyingSwordEntity sword) {
    if (!FlyingSwordSoundTuning.PLAY_OUT_OF_ENERGY) return;
    play(sword.level(), sword.position(), CCSoundEvents.CUSTOM_FLYINGSWORD_OUT_OF_ENERGY,
        FlyingSwordSoundTuning.VOL_OUT_OF_ENERGY);
  }
}

