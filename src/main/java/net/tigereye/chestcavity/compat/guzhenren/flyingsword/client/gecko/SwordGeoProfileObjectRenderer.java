package net.tigereye.chestcavity.compat.guzhenren.flyingsword.client.gecko;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.Color;

/** Geo 渲染器：资源直接来自对象实例（profile）。 */
public class SwordGeoProfileObjectRenderer extends GeoObjectRenderer<SwordGeoProfileObject> {

  public SwordGeoProfileObjectRenderer() {
    super(new Model());
  }

  @Override
  public RenderType getRenderType(
      SwordGeoProfileObject animatable,
      ResourceLocation texture,
      MultiBufferSource bufferSource,
      float partialTick) {
    return RenderType.entityCutout(texture);
  }

  @Override
  public Color getRenderColor(SwordGeoProfileObject animatable, float partialTick, int packedLight) {
    return Color.WHITE;
  }

  private static final class Model extends GeoModel<SwordGeoProfileObject> {
    @Override
    @SuppressWarnings("removal")
    public ResourceLocation getModelResource(SwordGeoProfileObject anim) {
      return anim.model();
    }

    @Override
    @SuppressWarnings("removal")
    public ResourceLocation getTextureResource(SwordGeoProfileObject anim) {
      return anim.texture();
    }

    @Override
    public ResourceLocation getAnimationResource(SwordGeoProfileObject anim) {
      return anim.animation();
    }
  }
}

