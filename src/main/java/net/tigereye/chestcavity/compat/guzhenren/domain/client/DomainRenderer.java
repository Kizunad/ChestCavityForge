package net.tigereye.chestcavity.compat.guzhenren.domain.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * 通用领域PNG渲染器
 *
 * <p>渲染领域的PNG纹理图案，支持：
 * <ul>
 *   <li>自定义纹理路径</li>
 *   <li>自定义高度偏移</li>
 *   <li>自动缩放到领域半径</li>
 *   <li>旋转动画</li>
 *   <li>透明度控制</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class DomainRenderer {

  private DomainRenderer() {}

  /** 默认高度偏移（格） */
  public static final double DEFAULT_HEIGHT_OFFSET = 20.0;

  /** 客户端领域数据缓存 */
  private static final Map<UUID, ClientDomainData> clientDomains = new ConcurrentHashMap<>();

  /**
   * 注册或更新客户端领域
   *
   * @param domainId 领域UUID
   * @param ownerUuid 主人UUID
   * @param centerX 中心X
   * @param centerY 中心Y
   * @param centerZ 中心Z
   * @param radius 半径
   * @param level 等级
   * @param texturePath 纹理路径
   * @param heightOffset 高度偏移（格）
   * @param alpha 透明度（0.0-1.0）
   * @param rotationSpeed 旋转速度（度/tick）
   */
  public static void registerDomain(
      UUID domainId,
      UUID ownerUuid,
      double centerX,
      double centerY,
      double centerZ,
      double radius,
      int level,
      ResourceLocation texturePath,
      double heightOffset,
      float alpha,
      float rotationSpeed) {
    clientDomains.put(
        domainId,
        new ClientDomainData(
            domainId,
            ownerUuid,
            centerX,
            centerY,
            centerZ,
            radius,
            level,
            texturePath,
            heightOffset,
            alpha,
            rotationSpeed));
  }

  /**
   * 移除客户端领域
   *
   * @param domainId 领域UUID
   */
  public static void removeDomain(UUID domainId) {
    clientDomains.remove(domainId);
  }

  /** 清空所有客户端领域（切换世界时调用） */
  public static void clearAll() {
    clientDomains.clear();
  }

  /**
   * 渲染所有领域
   *
   * @param event 渲染事件
   */
  public static void render(RenderLevelStageEvent event) {
    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
      return;
    }

    if (clientDomains.isEmpty()) {
      return;
    }

    Minecraft minecraft = Minecraft.getInstance();
    PoseStack poseStack = event.getPoseStack();
    Camera camera = event.getCamera();
    Vec3 cameraPos = camera.getPosition();
    MultiBufferSource buffer = minecraft.renderBuffers().bufferSource();

    for (ClientDomainData domain : clientDomains.values()) {
      renderDomain(poseStack, buffer, cameraPos, domain);
    }
  }

  /**
   * 渲染单个领域
   *
   * @param poseStack 姿态栈
   * @param buffer 缓冲区
   * @param cameraPos 相机位置
   * @param domain 领域数据
   */
  private static void renderDomain(
      PoseStack poseStack,
      MultiBufferSource buffer,
      Vec3 cameraPos,
      ClientDomainData domain) {
    poseStack.pushPose();

    // 移动到领域中心 + 高度偏移
    double renderX = domain.centerX - cameraPos.x;
    double renderY = domain.centerY + domain.heightOffset - cameraPos.y;
    double renderZ = domain.centerZ - cameraPos.z;

    poseStack.translate(renderX, renderY, renderZ);

    // 旋转动画
    long gameTime =
        Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
    float rotation = (gameTime % 360) * domain.rotationSpeed;
    poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

    // 缩放到领域半径大小
    float scale = (float) domain.radius;
    poseStack.scale(scale, scale, scale);

    // 渲染纹理（尝试去除棋盘格背景）
    ResourceLocation resolved =
        TransparentTextureResolver.getOrProcess(domain.texturePath);
    RenderType renderType = RenderType.entityTranslucent(resolved);
    VertexConsumer consumer = buffer.getBuffer(renderType);
    Matrix4f matrix = poseStack.last().pose();

    // 水平四边形（顶点顺序：左下、右下、右上、左上）
    addVertex(consumer, matrix, -1, 0, -1, 0, 1, domain.alpha); // 左下
    addVertex(consumer, matrix, 1, 0, -1, 1, 1, domain.alpha); // 右下
    addVertex(consumer, matrix, 1, 0, 1, 1, 0, domain.alpha); // 右上
    addVertex(consumer, matrix, -1, 0, 1, 0, 0, domain.alpha); // 左上

    poseStack.popPose();
  }

  /**
   * 添加顶点
   *
   * @param consumer 顶点消费者
   * @param matrix 变换矩阵
   * @param x X坐标
   * @param y Y坐标
   * @param z Z坐标
   * @param u 纹理U
   * @param v 纹理V
   * @param alpha 透明度
   */
  private static void addVertex(
      VertexConsumer consumer,
      Matrix4f matrix,
      float x,
      float y,
      float z,
      float u,
      float v,
      float alpha) {
    consumer
        .addVertex(matrix, x, y, z)
        .setColor(1.0f, 1.0f, 1.0f, alpha)
        .setUv(u, v)
        .setOverlay(0)
        .setLight(0xF000F0) // 最大亮度
        .setNormal(0, 1, 0); // 法线向上
  }

  /** 客户端领域数据 */
  private record ClientDomainData(
      UUID domainId,
      UUID ownerUuid,
      double centerX,
      double centerY,
      double centerZ,
      double radius,
      int level,
      ResourceLocation texturePath,
      double heightOffset,
      float alpha,
      float rotationSpeed) {}
}
