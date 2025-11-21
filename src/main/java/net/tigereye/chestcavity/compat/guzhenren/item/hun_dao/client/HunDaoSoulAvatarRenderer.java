package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client;

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
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.entity.HunDaoSoulAvatarEntity;

/** Renderer for Hun Dao soul avatars (player-like translucent replicas). */
public class HunDaoSoulAvatarRenderer
    extends HumanoidMobRenderer<HunDaoSoulAvatarEntity, PlayerModel<HunDaoSoulAvatarEntity>> {

  public HunDaoSoulAvatarRenderer(EntityRendererProvider.Context context) {
    super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.25f);
    this.addLayer(new TintLayer(this, context));
  }

  @Override
  protected boolean shouldShowName(HunDaoSoulAvatarEntity entity) {
    return false;
  }

  @Override
  public ResourceLocation getTextureLocation(HunDaoSoulAvatarEntity entity) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft != null && minecraft.level != null) {
      ResourceLocation rawFallback = entity.getSkinTexture();
      String model = entity.getSkinModel();
      String skinUrl = guessSkinUrl(rawFallback);
      ResourceLocation fallback = rawFallback;
      if ("minecraft".equals(fallback.getNamespace()) && fallback.getPath().startsWith("skins/")) {
        fallback = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
      }
      var handle =
          new SkinHandle(
              entity.getUUID(),
              "",
              "",
              model,
              skinUrl,
              fallback,
              null);
      var layers = SkinResolver.resolve(handle);
      if (layers.base() != null) {
        return layers.base();
      }
    }
    return entity.getSkinTexture();
  }

  @Override
  public void render(
      HunDaoSoulAvatarEntity entity,
      float entityYaw,
      float partialTicks,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight) {
    boolean previous = entity.isInvisible();
    entity.setInvisible(true);
    float originalShadow = this.shadowRadius;
    float scale = (float) entity.getHunpoScaleMultiplier();
    this.shadowRadius = 0.25f * scale;
    try {
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    } finally {
      this.shadowRadius = originalShadow;
    }
    entity.setInvisible(previous);
  }

  @Override
  protected void scale(
      HunDaoSoulAvatarEntity entity, PoseStack poseStack, float partialTickTime) {
    super.scale(entity, poseStack, partialTickTime);
    float scale = (float) entity.getHunpoScaleMultiplier();
    poseStack.scale(scale, scale, scale);
  }

  private static final class TintLayer
      extends RenderLayer<HunDaoSoulAvatarEntity, PlayerModel<HunDaoSoulAvatarEntity>> {
    private final HunDaoSoulAvatarRenderer parent;
    private final PlayerModel<HunDaoSoulAvatarEntity> slimModel;

    private TintLayer(HunDaoSoulAvatarRenderer renderer, EntityRendererProvider.Context context) {
      super(renderer);
      this.parent = renderer;
      this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public void render(
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        HunDaoSoulAvatarEntity entity,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ageInTicks,
        float netHeadYaw,
        float headPitch) {
      PlayerModel<HunDaoSoulAvatarEntity> base = this.getParentModel();
      PlayerModel<HunDaoSoulAvatarEntity> model =
          "slim".equals(entity.getSkinModel()) ? slimModel : base;
      base.copyPropertiesTo(model);
      float[] tint = entity.getTintComponents();
      int argb =
          FastColor.ARGB32.color(
              (int) (tint[3] * 255.0f),
              (int) (tint[0] * 255.0f),
              (int) (tint[1] * 255.0f),
              (int) (tint[2] * 255.0f));
      ResourceLocation texture = parent.getTextureLocation(entity);
      VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
      model.renderToBuffer(
          poseStack,
          consumer,
          packedLight,
          LivingEntityRenderer.getOverlayCoords(entity, 0.0f),
          argb);
    }
  }

  private static String guessSkinUrl(ResourceLocation texture) {
    if (texture == null) {
      return null;
    }
    if ("minecraft".equals(texture.getNamespace())) {
      String path = texture.getPath();
      if (path != null && path.startsWith("skins/")) {
        int idx = path.lastIndexOf('/') + 1;
        if (idx > 0 && idx < path.length()) {
          String hash = path.substring(idx);
          if (!hash.isBlank()) {
            return "https://textures.minecraft.net/texture/" + hash;
          }
        }
      }
    }
    return null;
  }
}
