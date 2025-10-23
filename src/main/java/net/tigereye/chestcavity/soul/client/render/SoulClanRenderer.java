package net.tigereye.chestcavity.soul.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.soul.entity.SoulClanEntity;

/** 客户端渲染器：为魂族三大分支（长老、护卫、商贾）复用同一人形模型，并根据变体切换贴图。 */
public final class SoulClanRenderer
    extends MobRenderer<SoulClanEntity, HumanoidModel<SoulClanEntity>> {

  /**
   * 构造一个命名空间位于 chestcavity 下的资源定位器，便于维护贴图路径。
   *
   * @param path 贴图相对路径
   * @return 对应的资源定位器
   */
  private static ResourceLocation rl(String path) {
    return ResourceLocation.fromNamespaceAndPath("chestcavity", path);
  }

  private static final ResourceLocation TEX_ELDER = rl("textures/entity/soul_elder.png");
  private static final ResourceLocation TEX_GUARD = rl("textures/entity/soul_guard.png");
  private static final ResourceLocation TEX_TRADER = rl("textures/entity/soul_trader.png");

  /**
   * 使用原版僵尸的人形模型层，确保无需自定义模型即可完成渲染。
   *
   * @param ctx 渲染器构造上下文，提供模型层和渲染器注册能力
   */
  public SoulClanRenderer(EntityRendererProvider.Context ctx) {
    super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
  }

  /**
   * 根据实体当前的变体返回对应的纹理位置，实现“同模不同皮”。
   *
   * @param entity 魂族实体实例
   * @return 对应的纹理资源定位器
   */
  @Override
  public ResourceLocation getTextureLocation(SoulClanEntity entity) {
    return switch (entity.getVariant()) {
      case ELDER -> TEX_ELDER;
      case GUARD -> TEX_GUARD;
      case TRADER -> TEX_TRADER;
    };
  }
}
