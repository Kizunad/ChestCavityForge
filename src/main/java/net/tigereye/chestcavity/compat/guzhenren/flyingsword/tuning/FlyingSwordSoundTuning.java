package net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning;

/** 飞剑音效参数（总开关、分类音量、随机音高等）。 */
public final class FlyingSwordSoundTuning {
  private FlyingSwordSoundTuning() {}

  // 总开关
  public static final boolean ENABLE_SOUNDS = true;

  // 分类开关
  public static final boolean PLAY_SPAWN = true;
  public static final boolean PLAY_RECALL = true;
  public static final boolean PLAY_SWING = false;
  public static final boolean PLAY_HIT = true;
  public static final boolean PLAY_BLOCK_BREAK = true;
  public static final boolean PLAY_OUT_OF_ENERGY = true;

  // 音量（0-1）
  public static final float VOL_SPAWN = 0.9f;
  public static final float VOL_RECALL = 0.9f;
  public static final float VOL_SWING = 0.6f;
  public static final float VOL_HIT = 0.9f;
  public static final float VOL_BLOCK_BREAK = 0.7f;
  public static final float VOL_OUT_OF_ENERGY = 1.0f;

  // 音高
  public static final float PITCH_BASE = 1.0f;
  public static final float PITCH_VAR = 0.08f;
}
