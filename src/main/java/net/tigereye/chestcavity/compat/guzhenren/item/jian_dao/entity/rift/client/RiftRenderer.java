package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.rift.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.rift.RiftEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.rift.RiftType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 裂隙渲染器
 *
 * <p>渲染PNG贴图，使用Billboard效果（永远面向相机）
 */
public class RiftRenderer extends EntityRenderer<RiftEntity> {

  /** 裂隙纹理 */
  private static final ResourceLocation RIFT_TEXTURE =
      ResourceLocation.fromNamespaceAndPath(
          "guzhenren", "textures/rift/sword_rift_transparent_aggressive.png");

  /** 渲染类型（支持透明） */
  private static final RenderType RENDER_TYPE = RenderType.entityTranslucent(RIFT_TEXTURE);
  private static final RenderType LINE_TYPE = RenderType.lines();

  public RiftRenderer(EntityRendererProvider.Context context) {
    super(context);
  }

  @Override
  public void render(
      RiftEntity entity,
      float entityYaw,
      float partialTicks,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight) {

    poseStack.pushPose();

    // Billboard效果：面向相机
    poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

    // 根据裂隙类型缩放
    RiftType type = entity.getRiftType();
    poseStack.scale(type.displayWidth, type.displayHeight, 0.1f);

    // 调整透明度（根据剩余时间）
    float alpha = calculateAlpha(entity);

    // 渲染四边形
    renderQuad(poseStack, buffer, packedLight, alpha);

    // 渲染共鸣链“光脉”线段（≤6格）
    renderResonanceLines(entity, poseStack, buffer, packedLight);

    poseStack.popPose();

    super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
  }

  private void renderResonanceLines(
      RiftEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
    final double radius = 6.0;
    Vec3 self = entity.position();

    // 搜索附近其他裂隙（客户端世界）
    java.util.List<RiftEntity> nearby =
        entity.level().getEntitiesOfClass(
            RiftEntity.class,
            entity.getBoundingBox().inflate(radius),
            e -> e != entity && e.isAlive() && e.position().distanceTo(self) <= radius);

    if (nearby.isEmpty()) return;

    VertexConsumer line = buffer.getBuffer(LINE_TYPE);
    PoseStack.Pose pose = poseStack.last();
    Matrix4f m = pose.pose();
    Matrix3f n = pose.normal();

    // 为避免重复，仅绘制到“UUID字典序更大”的目标
    java.util.UUID selfId = entity.getUUID();
    for (RiftEntity other : nearby) {
      if (selfId.compareTo(other.getUUID()) >= 0) continue;
      // 局部坐标：顶点以当前实体为原点
      Vec3 d = other.position().subtract(entity.position());
      addLine(line, m, n, packedLight, 0f, 0.4f, 1f, 0.9f, 0, 0, 0, (float) d.x, (float) (d.y + 0.2), (float) d.z);
    }
  }

  private void addLine(
      VertexConsumer consumer,
      Matrix4f m,
      Matrix3f n,
      int packedLight,
      float r,
      float g,
      float b,
      float a,
      float x0,
      float y0,
      float z0,
      float x1,
      float y1,
      float z1) {
    // lines: 定义两端点
    consumer.addVertex(m, x0, y0, z0).setColor(r, g, b, a).setNormal(0, 1, 0).setLight(packedLight);
    consumer.addVertex(m, x1, y1, z1).setColor(r, g, b, a).setNormal(0, 1, 0).setLight(packedLight);
  }

  /**
   * 渲染四边形
   */
  private void renderQuad(
      PoseStack poseStack, MultiBufferSource buffer, int packedLight, float alpha) {

    VertexConsumer consumer = buffer.getBuffer(RENDER_TYPE);
    PoseStack.Pose pose = poseStack.last();
    Matrix4f matrix4f = pose.pose();
    Matrix3f matrix3f = pose.normal();

    // 定义四个顶点（中心对齐）
    float halfWidth = 0.5f;
    float halfHeight = 0.5f;

    // 顶点顺序：左下、右下、右上、左上
    vertex(
        consumer,
        matrix4f,
        matrix3f,
        packedLight,
        -halfWidth,
        -halfHeight,
        0,
        0,
        1,
        alpha); // 左下
    vertex(
        consumer,
        matrix4f,
        matrix3f,
        packedLight,
        halfWidth,
        -halfHeight,
        0,
        1,
        1,
        alpha); // 右下
    vertex(
        consumer, matrix4f, matrix3f, packedLight, halfWidth, halfHeight, 0, 1, 0, alpha); // 右上
    vertex(
        consumer,
        matrix4f,
        matrix3f,
        packedLight,
        -halfWidth,
        halfHeight,
        0,
        0,
        0,
        alpha); // 左上
  }

  /**
   * 添加顶点
   */
  private void vertex(
      VertexConsumer consumer,
      Matrix4f matrix4f,
      Matrix3f matrix3f,
      int packedLight,
      float x,
      float y,
      float z,
      float u,
      float v,
      float alpha) {

    consumer
        .addVertex(matrix4f, x, y, z) // 位置
        .setColor(1.0f, 1.0f, 1.0f, alpha) // 颜色（白色 + 透明度）
        .setUv(u, v) // UV坐标
        .setOverlay(OverlayTexture.NO_OVERLAY) // 无覆盖层
        .setLight(packedLight) // 光照
        .setNormal(0, 1, 0); // 法线（向上）
  }

  /**
   * 计算透明度
   *
   * <p>根据剩余时间渐隐
   */
  private float calculateAlpha(RiftEntity entity) {
    RiftType type = entity.getRiftType();
    int remaining = entity.getRemainingTicks();
    int baseDuration = type.baseDuration;

    // 基础透明度
    float baseAlpha = 0.8f;

    // 最后10秒渐隐
    if (remaining < 10 * 20) {
      float fadeRatio = remaining / (10.0f * 20);
      return baseAlpha * fadeRatio;
    }

    return baseAlpha;
  }

  @Override
  public ResourceLocation getTextureLocation(RiftEntity entity) {
    return RIFT_TEXTURE;
  }
}
