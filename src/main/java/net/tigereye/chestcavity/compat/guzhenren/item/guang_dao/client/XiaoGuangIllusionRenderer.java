package net.tigereye.chestcavity.compat.guzhenren.item.guang_dao.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.tigereye.chestcavity.compat.guzhenren.client.skin.SkinHandle;
import net.tigereye.chestcavity.compat.guzhenren.client.skin.SkinResolver;
import net.tigereye.chestcavity.compat.guzhenren.item.guang_dao.entity.XiaoGuangIllusionEntity;

/** 光蛊幻象渲染：沿用玩家皮肤并追加发光层。 */
public class XiaoGuangIllusionRenderer
    extends HumanoidMobRenderer<XiaoGuangIllusionEntity, PlayerModel<XiaoGuangIllusionEntity>> {

  private static final ResourceLocation FALLBACK_TEXTURE =
      ResourceLocation.parse("minecraft:textures/entity/steve.png");

  public XiaoGuangIllusionRenderer(EntityRendererProvider.Context context) {
    super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.25f);
    this.addLayer(new AuraLayer(this));
  }

  @Override
  protected boolean shouldShowName(XiaoGuangIllusionEntity entity) {
    return false;
  }

  @Override
  public ResourceLocation getTextureLocation(XiaoGuangIllusionEntity entity) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft != null && minecraft.level != null) {
      var layers = SkinResolver.resolve(SkinHandle.from(entity));
      if (layers.base() != null) {
        return layers.base();
      }
    }
    ResourceLocation snapshotTexture = entity.getSkinTexture();
    return snapshotTexture != null && !snapshotTexture.getPath().isBlank()
        ? snapshotTexture
        : FALLBACK_TEXTURE;
  }

  private static final class AuraLayer
      extends RenderLayer<XiaoGuangIllusionEntity, PlayerModel<XiaoGuangIllusionEntity>> {

    private final XiaoGuangIllusionRenderer parent;

    AuraLayer(XiaoGuangIllusionRenderer parent) {
      super(parent);
      this.parent = parent;
    }

    @Override
    public void render(
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        XiaoGuangIllusionEntity entity,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ageInTicks,
        float netHeadYaw,
        float headPitch) {
      PlayerModel<XiaoGuangIllusionEntity> base = this.getParentModel();
      PlayerModel<XiaoGuangIllusionEntity> model = parent.getModel();
      base.copyPropertiesTo(model);
      float[] tint = entity.getAuraColorComponents();
      int color =
          FastColor.ARGB32.color(
              (int) (tint[3] * 255.0f),
              (int) (tint[0] * 255.0f),
              (int) (tint[1] * 255.0f),
              (int) (tint[2] * 255.0f));
      ResourceLocation texture = parent.getTextureLocation(entity);
      VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(texture));
      model.renderToBuffer(
          poseStack,
          consumer,
          packedLight,
          LivingEntityRenderer.getOverlayCoords(entity, 0.0f),
          color);
    }
  }
}
