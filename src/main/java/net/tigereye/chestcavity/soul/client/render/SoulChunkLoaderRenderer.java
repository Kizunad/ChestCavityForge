package net.tigereye.chestcavity.soul.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.soul.entity.SoulChunkLoaderEntity;

/** 渲染为空的区块加载器，避免客户端缺失 renderer 导致崩溃。 */
public final class SoulChunkLoaderRenderer extends EntityRenderer<SoulChunkLoaderEntity> {

  public SoulChunkLoaderRenderer(EntityRendererProvider.Context context) {
    super(context);
  }

  @Override
  public void render(
      SoulChunkLoaderEntity entity,
      float yaw,
      float partialTicks,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      int packedLight) {
    // 不渲染任何内容
  }

  @Override
  public ResourceLocation getTextureLocation(SoulChunkLoaderEntity entity) {
    return TextureAtlas.LOCATION_BLOCKS;
  }

  @Override
  public boolean shouldRender(
      SoulChunkLoaderEntity entity, Frustum frustum, double x, double y, double z) {
    return false;
  }
}
