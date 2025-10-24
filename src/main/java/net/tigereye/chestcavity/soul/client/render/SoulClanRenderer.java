package net.tigereye.chestcavity.soul.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.soul.entity.SoulClanEntity;

/** 专为 SoulClanEntity 提供的渲染器实现，复用僵尸模型并根据实体变体自动切换贴图。 */
public final class SoulClanRenderer
    extends MobRenderer<SoulClanEntity, HumanoidModel<SoulClanEntity>> {

  private static ResourceLocation rl(String path) {
    return ResourceLocation.fromNamespaceAndPath("chestcavity", path);
  }

  /** 长老变体的渲染贴图。 */
  private static final ResourceLocation TEX_ELDER = rl("textures/entity/soul_elder.png");

  /** 警卫变体的渲染贴图。 */
  private static final ResourceLocation TEX_GUARD = rl("textures/entity/soul_guard.png");

  /** 商人变体的渲染贴图。 */
  private static final ResourceLocation TEX_TRADER = rl("textures/entity/soul_trader.png");

  public SoulClanRenderer(EntityRendererProvider.Context ctx) {
    super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
  }

  /** 根据实体当前的变体返回对应的贴图，确保三种变体共享模型但呈现不同外观。 */
  @Override
  public ResourceLocation getTextureLocation(SoulClanEntity entity) {
    return switch (entity.getVariant()) {
      case ELDER -> TEX_ELDER;
      case GUARD -> TEX_GUARD;
      case TRADER -> TEX_TRADER;
    };
  }
}
