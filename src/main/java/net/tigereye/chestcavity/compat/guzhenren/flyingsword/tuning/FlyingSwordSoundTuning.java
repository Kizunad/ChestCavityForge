package net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning;

import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;

/**
 * 飞剑音效参数（总开关、分类音量、随机音高等）。
 */
public final class FlyingSwordSoundTuning {
  private FlyingSwordSoundTuning() {}

  // 总开关
  public static final boolean ENABLE_SOUNDS = configBool("ENABLE_SOUNDS", true);

  // 分类开关
  public static final boolean PLAY_SPAWN = configBool("PLAY_SPAWN", true);
  public static final boolean PLAY_RECALL = configBool("PLAY_RECALL", true);
  public static final boolean PLAY_SWING = configBool("PLAY_SWING", false);
  public static final boolean PLAY_HIT = configBool("PLAY_HIT", true);
  public static final boolean PLAY_BLOCK_BREAK = configBool("PLAY_BLOCK_BREAK", true);
  public static final boolean PLAY_OUT_OF_ENERGY = configBool("PLAY_OUT_OF_ENERGY", true);

  // 音量（0-1）
  public static final float VOL_SPAWN = configFloat("VOL_SPAWN", 0.9f);
  public static final float VOL_RECALL = configFloat("VOL_RECALL", 0.9f);
  public static final float VOL_SWING = configFloat("VOL_SWING", 0.6f);
  public static final float VOL_HIT = configFloat("VOL_HIT", 0.9f);
  public static final float VOL_BLOCK_BREAK = configFloat("VOL_BLOCK_BREAK", 0.7f);
  public static final float VOL_OUT_OF_ENERGY = configFloat("VOL_OUT_OF_ENERGY", 1.0f);

  // 音高
  public static final float PITCH_BASE = configFloat("PITCH_BASE", 1.0f);
  public static final float PITCH_VAR = configFloat("PITCH_VAR", 0.08f);

  private static float configFloat(String key, float def) {
    return BehaviorConfigAccess.getFloat(FlyingSwordSoundTuning.class, key, def);
  }

  private static boolean configBool(String key, boolean def) {
    return BehaviorConfigAccess.getBoolean(FlyingSwordSoundTuning.class, key, def);
  }
}

