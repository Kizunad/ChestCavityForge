package net.tigereye.chestcavity.soul.playerghost.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.client.skin.SkinHandle;
import net.tigereye.chestcavity.compat.guzhenren.client.skin.SkinResolver;
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
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft != null && minecraft.level != null) {
      ResourceLocation rawFallback = entity.getSkinTexture();
      String model = entity.getSkinModel();
      String skinUrl = guessSkinUrl(rawFallback);
      // 避免在下载完成前请求不存在的 minecraft:skins/<hash> 资源
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
