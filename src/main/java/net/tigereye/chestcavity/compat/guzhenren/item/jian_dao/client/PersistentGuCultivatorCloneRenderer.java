package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.client;

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
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.PersistentGuCultivatorClone;

/**
 * 渲染持久化蛊修分身，复用 SwordShadowCloneRenderer 的逻辑。
 * 显示为半透明、带颜色的玩家皮肤版本。
 */
public class PersistentGuCultivatorCloneRenderer
    extends HumanoidMobRenderer<PersistentGuCultivatorClone, PlayerModel<PersistentGuCultivatorClone>> {

  public PersistentGuCultivatorCloneRenderer(EntityRendererProvider.Context context) {
    super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    this.addLayer(new TintLayer(this, context));
  }

  @Override
  protected boolean shouldShowName(PersistentGuCultivatorClone entity) {
    // 显示名称（可选，便于调试）
    return Minecraft.getInstance().options.hideGui == false && entity.hasCustomName();
  }

  @Override
  public ResourceLocation getTextureLocation(PersistentGuCultivatorClone entity) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft != null && minecraft.level != null) {
      ResourceLocation fallback = entity.getSkinTexture();
      String model = entity.getSkinModel();
      String skinUrl = guessSkinUrl(fallback);
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
      PersistentGuCultivatorClone entity,
      float entityYaw,
      float partialTicks,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight) {
    // 设置实体为隐形，但通过 TintLayer 渲染半透明版本
    boolean previous = entity.isInvisible();
    entity.setInvisible(true);
    super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    entity.setInvisible(previous);
  }

  private static final class TintLayer
      extends RenderLayer<PersistentGuCultivatorClone, PlayerModel<PersistentGuCultivatorClone>> {
    private final PersistentGuCultivatorCloneRenderer parent;
    private final PlayerModel<PersistentGuCultivatorClone> slimModel;

    private TintLayer(PersistentGuCultivatorCloneRenderer renderer, EntityRendererProvider.Context context) {
      super(renderer);
      this.parent = renderer;
      this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public void render(
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        PersistentGuCultivatorClone entity,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ageInTicks,
        float netHeadYaw,
        float headPitch) {
      PlayerModel<PersistentGuCultivatorClone> base = this.getParentModel();
      PlayerModel<PersistentGuCultivatorClone> model = "slim".equals(entity.getSkinModel()) ? slimModel : base;
      base.copyPropertiesTo(model);
      float[] tint = entity.getTintComponents();
      int argb =
          FastColor.ARGB32.color(
              (int) (tint[3] * 255.0f),  // Alpha
              (int) (tint[0] * 255.0f),  // Red
              (int) (tint[1] * 255.0f),  // Green
              (int) (tint[2] * 255.0f)); // Blue
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
    if (texture == null) return null;
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
