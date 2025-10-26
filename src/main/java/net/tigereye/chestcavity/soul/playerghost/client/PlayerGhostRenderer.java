package net.tigereye.chestcavity.soul.playerghost.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.soul.playerghost.PlayerGhostEntity;

/**
 * 玩家幽灵渲染器
 *
 * <p>职责：
 * - 使用玩家模型渲染幽灵实体
 * - 根据实体的皮肤数据动态选择纹理
 * - 支持 "default" 和 "slim" 两种模型
 */
public class PlayerGhostRenderer
    extends HumanoidMobRenderer<PlayerGhostEntity, PlayerModel<PlayerGhostEntity>> {

  public PlayerGhostRenderer(EntityRendererProvider.Context context) {
    // 使用默认玩家模型（Steve 型，手臂粗）
    super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
  }

  @Override
  public ResourceLocation getTextureLocation(PlayerGhostEntity entity) {
    return entity.getSkinTexture();
  }
}
