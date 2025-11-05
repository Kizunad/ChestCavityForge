package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.gecko;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.override.SwordModelOverrideDef;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.Color;

/** 基于覆盖定义的通用 GeoObject 渲染器。 */
public class SwordModelObjectRenderer extends GeoObjectRenderer<SwordModelObject> {

  public SwordModelObjectRenderer() {
    super(new Model());
  }

  @Override
  public RenderType getRenderType(
      SwordModelObject animatable,
      ResourceLocation texture,
      MultiBufferSource bufferSource,
      float partialTick) {
    // 贴图一般包含透明像素，使用 Cutout 更常见；必要时外部可切换
    return RenderType.entityCutout(texture);
  }

  @Override
  public Color getRenderColor(SwordModelObject animatable, float partialTick, int packedLight) {
    return Color.WHITE;
  }

  private static final class Model extends GeoModel<SwordModelObject> {
    @Override
    @SuppressWarnings("removal")
    public ResourceLocation getModelResource(SwordModelObject animatable) {
      SwordModelOverrideDef def = animatable.def();
      return def.model;
    }

    @Override
    @SuppressWarnings("removal")
    public ResourceLocation getTextureResource(SwordModelObject animatable) {
      SwordModelOverrideDef def = animatable.def();
      return def.texture;
    }

    @Override
    public ResourceLocation getAnimationResource(SwordModelObject animatable) {
      SwordModelOverrideDef def = animatable.def();
      return def.animation;
    }
  }
}

