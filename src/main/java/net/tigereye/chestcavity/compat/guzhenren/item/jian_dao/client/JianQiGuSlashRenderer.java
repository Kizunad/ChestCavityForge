package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.JianQiGuSlashProjectile;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 剑气蛊剑光渲染器。
 *
 * <p>渲染一个扁平的发光剑光 Quad，根据威能动态缩放长度、宽度和透明度。
 */
public class JianQiGuSlashRenderer extends EntityRenderer<JianQiGuSlashProjectile> {

  /** 剑光纹理（使用半透明白色纹理） */
  private static final ResourceLocation TEXTURE =
      ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "textures/entity/sword_slash.png");

  /** 基础长度（blocks） */
  private static final float BASE_LENGTH = 2.5f;

  /** 最大额外长度（随威能增加） */
  private static final float MAX_EXTRA_LENGTH = 3.5f;

  /** 基础宽度（blocks） */
  private static final float BASE_WIDTH = 0.3f;

  /** 最大额外宽度（随威能增加） */
  private static final float MAX_EXTRA_WIDTH = 0.4f;

  /** 基础透明度 */
  private static final float BASE_ALPHA = 0.4f;

  /** 最大额外透明度 */
  private static final float MAX_EXTRA_ALPHA = 0.5f;

  public JianQiGuSlashRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.shadowRadius = 0.0f;
  }

  @Override
  public void render(
      JianQiGuSlashProjectile entity,
      float entityYaw,
      float partialTicks,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight) {

    // 获取视觉威能（0-1）
    float visualPower = entity.getVisualPower();

    // 计算长度、宽度、透明度
    float length = BASE_LENGTH + visualPower * MAX_EXTRA_LENGTH;
    float width = BASE_WIDTH + visualPower * MAX_EXTRA_WIDTH;
    float alpha = BASE_ALPHA + visualPower * MAX_EXTRA_ALPHA;

    // 获取方向
    Vec3 direction = entity.getDirection();
    if (direction.lengthSqr() < 1.0E-6) {
      direction = entity.getViewVector(partialTicks);
    }

    poseStack.pushPose();

    // 对齐到移动方向
    float yaw = (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
    float pitch =
        (float)
            Math.toDegrees(
                Math.asin(Mth.clamp(direction.y, -1.0, 1.0)));

    poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
    poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

    // 渲染剑光 Quad
    renderSlashQuad(poseStack, buffer, length, width, alpha, packedLight);

    poseStack.popPose();
  }

  /**
   * 渲染剑光四边形。
   *
   * @param poseStack 姿态栈
   * @param buffer 缓冲源
   * @param length 长度
   * @param width 宽度
   * @param alpha 透明度
   * @param packedLight 光照值
   */
  private void renderSlashQuad(
      PoseStack poseStack,
      MultiBufferSource buffer,
      float length,
      float width,
      float alpha,
      int packedLight) {

    // 使用发光半透明渲染类型
    VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));

    Matrix4f matrix4f = poseStack.last().pose();
    Matrix3f matrix3f = poseStack.last().normal();

    // 颜色（淡蓝白色）
    float r = 0.8f + 0.2f * alpha; // 白色偏蓝
    float g = 0.9f + 0.1f * alpha;
    float b = 1.0f;

    // 增强光照（模拟自发光）
    int enhancedLight = 15728880; // 最大亮度

    // 构造四边形顶点
    // 剑光沿 Z 轴正向延伸，宽度在 X 轴上

    float halfWidth = width * 0.5f;
    float z0 = 0.0f;
    float z1 = length;

    // 顶点定义（逆时针）
    // v0: 左下
    // v1: 右下
    // v2: 右上
    // v3: 左上

    // 顶点 0：(-halfWidth, 0, z0)
    consumer
        .addVertex(matrix4f, -halfWidth, 0.0f, z0)
        .setColor(r, g, b, alpha)
        .setUv(0.0f, 0.0f)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(enhancedLight)
        .setNormal(matrix3f, 0.0f, 1.0f, 0.0f);

    // 顶点 1：(halfWidth, 0, z0)
    consumer
        .addVertex(matrix4f, halfWidth, 0.0f, z0)
        .setColor(r, g, b, alpha)
        .setUv(1.0f, 0.0f)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(enhancedLight)
        .setNormal(matrix3f, 0.0f, 1.0f, 0.0f);

    // 顶点 2：(halfWidth, 0, z1)
    consumer
        .addVertex(matrix4f, halfWidth, 0.0f, z1)
        .setColor(r, g, b, alpha)
        .setUv(1.0f, 1.0f)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(enhancedLight)
        .setNormal(matrix3f, 0.0f, 1.0f, 0.0f);

    // 顶点 3：(-halfWidth, 0, z1)
    consumer
        .addVertex(matrix4f, -halfWidth, 0.0f, z1)
        .setColor(r, g, b, alpha)
        .setUv(0.0f, 1.0f)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(enhancedLight)
        .setNormal(matrix3f, 0.0f, 1.0f, 0.0f);

    // 双面渲染（反向）
    consumer
        .addVertex(matrix4f, -halfWidth, 0.0f, z1)
        .setColor(r, g, b, alpha)
        .setUv(0.0f, 1.0f)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(enhancedLight)
        .setNormal(matrix3f, 0.0f, -1.0f, 0.0f);

    consumer
        .addVertex(matrix4f, halfWidth, 0.0f, z1)
        .setColor(r, g, b, alpha)
        .setUv(1.0f, 1.0f)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(enhancedLight)
        .setNormal(matrix3f, 0.0f, -1.0f, 0.0f);

    consumer
        .addVertex(matrix4f, halfWidth, 0.0f, z0)
        .setColor(r, g, b, alpha)
        .setUv(1.0f, 0.0f)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(enhancedLight)
        .setNormal(matrix3f, 0.0f, -1.0f, 0.0f);

    consumer
        .addVertex(matrix4f, -halfWidth, 0.0f, z0)
        .setColor(r, g, b, alpha)
        .setUv(0.0f, 0.0f)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(enhancedLight)
        .setNormal(matrix3f, 0.0f, -1.0f, 0.0f);
  }

  @Override
  public ResourceLocation getTextureLocation(JianQiGuSlashProjectile entity) {
    return TEXTURE;
  }
}
