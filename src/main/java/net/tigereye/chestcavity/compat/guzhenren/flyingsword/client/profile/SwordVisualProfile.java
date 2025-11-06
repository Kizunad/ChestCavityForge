package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.profile;

import java.util.Collections;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.orientation.OrientationMode;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.orientation.UpMode;

/** 数据驱动的飞剑视觉配置（新一代 Profile）。 */
public final class SwordVisualProfile {
  public enum RendererKind { ITEM, GECKO }
  public enum AlignMode { VELOCITY, TARGET, OWNER, NONE }
  public enum GlintMode { INHERIT, FORCE_ON, FORCE_OFF }

  public final String key;
  public final boolean enabled;
  public final RendererKind renderer;
  public final ResourceLocation model; // gecko专用，可空
  public final List<ResourceLocation> textures; // 支持分层，可空
  public final ResourceLocation animation; // gecko可选

  // 姿态/尺度
  public final AlignMode align;
  public final float preRollDeg;
  public final float yawOffsetDeg;
  public final float pitchOffsetDeg;
  public final float scale;

  // Phase 8: 姿态计算模式
  public final OrientationMode orientationMode;
  public final UpMode upMode;

  // 附魔荧光策略
  public final GlintMode glint;

  // 条件（最小化：modelKey 精确匹配）
  public final List<String> matchModelKeys;

  public SwordVisualProfile(
      String key,
      boolean enabled,
      RendererKind renderer,
      ResourceLocation model,
      List<ResourceLocation> textures,
      ResourceLocation animation,
      AlignMode align,
      float preRollDeg,
      float yawOffsetDeg,
      float pitchOffsetDeg,
      float scale,
      OrientationMode orientationMode,
      UpMode upMode,
      GlintMode glint,
      List<String> matchModelKeys) {
    this.key = key;
    this.enabled = enabled;
    this.renderer = renderer;
    this.model = model;
    this.textures = textures == null ? List.of() : Collections.unmodifiableList(textures);
    this.animation = animation;
    this.align = align;
    this.preRollDeg = preRollDeg;
    this.yawOffsetDeg = yawOffsetDeg;
    this.pitchOffsetDeg = pitchOffsetDeg;
    this.scale = scale;
    this.orientationMode = orientationMode != null ? orientationMode : OrientationMode.BASIS;
    this.upMode = upMode != null ? upMode : UpMode.WORLD_Y;
    this.glint = glint;
    this.matchModelKeys = matchModelKeys == null ? List.of() : Collections.unmodifiableList(matchModelKeys);
  }

  // 便捷默认
  public static SwordVisualProfile defaults(String key) {
    return new SwordVisualProfile(
        key,
        false,
        RendererKind.ITEM,
        null,
        List.of(),
        null,
        AlignMode.VELOCITY,
        -45.0f,
        -90.0f,
        0.0f,
        1.0f,
        OrientationMode.BASIS,
        UpMode.WORLD_Y,
        GlintMode.INHERIT,
        List.of(key));
  }
}

