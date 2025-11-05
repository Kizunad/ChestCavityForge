package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.override;

import java.util.Collections;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * 模型覆盖定义（从资源JSON加载），用于在不改代码的情况下为不同飞剑提供专用模型/朝向参数。
 */
public final class SwordModelOverrideDef {
  public enum RendererKind { ITEM, GECKO }
  public enum AlignMode { VELOCITY, TARGET, NONE }

  public final String key; // 与 FlyingSwordEntity.getModelKey() 对应
  public final RendererKind renderer;

  // Gecko 专用资源（renderer==GECKO 时生效）
  public final ResourceLocation model;
  public final ResourceLocation texture;
  public final ResourceLocation animation; // 可为 null
  public final ResourceLocation displayItem; // 覆盖显示的物品（可为 null）
  public final List<ResourceLocation> textures; // 分层贴图（可为空列表）

  // 姿态/朝向参数
  public final AlignMode alignMode;
  public final float preRollDeg;   // 渲染前的本体 X 轴预旋（刀面纠正）
  public final float yawOffsetDeg; // 额外Yaw偏移（度）
  public final float pitchOffsetDeg; // 额外Pitch偏移（度）
  public final float scale; // 统一缩放

  public SwordModelOverrideDef(
      String key,
      RendererKind renderer,
      ResourceLocation model,
      ResourceLocation texture,
      ResourceLocation animation,
      AlignMode alignMode,
      float preRollDeg,
      float yawOffsetDeg,
      float pitchOffsetDeg,
      float scale) {
    this.key = key;
    this.renderer = renderer;
    this.model = model;
    this.texture = texture;
    this.animation = animation;
    this.alignMode = alignMode;
    this.preRollDeg = preRollDeg;
    this.yawOffsetDeg = yawOffsetDeg;
    this.pitchOffsetDeg = pitchOffsetDeg;
    this.scale = scale;
    this.displayItem = null;
    this.textures = List.of();
  }

  public SwordModelOverrideDef(
      String key,
      RendererKind renderer,
      ResourceLocation model,
      ResourceLocation texture,
      ResourceLocation animation,
      ResourceLocation displayItem,
      AlignMode alignMode,
      float preRollDeg,
      float yawOffsetDeg,
      float pitchOffsetDeg,
      float scale) {
    this.key = key;
    this.renderer = renderer;
    this.model = model;
    this.texture = texture;
    this.animation = animation;
    this.displayItem = displayItem;
    this.alignMode = alignMode;
    this.preRollDeg = preRollDeg;
    this.yawOffsetDeg = yawOffsetDeg;
    this.pitchOffsetDeg = pitchOffsetDeg;
    this.scale = scale;
    this.textures = List.of();
  }

  public SwordModelOverrideDef(
      String key,
      RendererKind renderer,
      ResourceLocation model,
      List<ResourceLocation> textures,
      ResourceLocation animation,
      ResourceLocation displayItem,
      AlignMode alignMode,
      float preRollDeg,
      float yawOffsetDeg,
      float pitchOffsetDeg,
      float scale) {
    this.key = key;
    this.renderer = renderer;
    this.model = model;
    this.texture = textures != null && !textures.isEmpty() ? textures.get(0) : null;
    this.animation = animation;
    this.displayItem = displayItem;
    this.alignMode = alignMode;
    this.preRollDeg = preRollDeg;
    this.yawOffsetDeg = yawOffsetDeg;
    this.pitchOffsetDeg = pitchOffsetDeg;
    this.scale = scale;
    this.textures = textures == null ? List.of() : Collections.unmodifiableList(textures);
  }
}
