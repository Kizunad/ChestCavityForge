package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning.FlyingSwordModelTuning;

/**
 * 飞剑渲染器（Flying Sword Renderer）
 *
 * <p>基础渲染实现：
 * <ul>
 *   <li>使用铁剑物品模型</li>
 *   <li>根据移动方向旋转</li>
 *   <li>支持发光效果</li>
 * </ul>
 */
public class FlyingSwordRenderer extends EntityRenderer<FlyingSwordEntity> {

  private final ItemRenderer itemRenderer;
  // 由实体提供显示用物品栈；保留铁剑作为兜底
  private static final ItemStack FALLBACK_DISPLAY_ITEM = new ItemStack(Items.IRON_SWORD);

  public FlyingSwordRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.itemRenderer = context.getItemRenderer();
    this.shadowRadius = 0.15f;
  }

  @Override
  public void render(
      FlyingSwordEntity entity,
      float entityYaw,
      float partialTicks,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight) {

    poseStack.pushPose();

    // 使用平滑后的朝向向量，避免因速度微小变化导致的抖动
    Vec3 look = entity.getSmoothedLookAngle();

    // 先做一次"本体刀面"纠正：绕本地 X 轴（模型前向）预旋
    // 这样可避免出现刀面以“/”倾斜显示的情况
    poseStack.mulPose(Axis.XP.rotationDegrees(FlyingSwordModelTuning.BLADE_ROLL_DEGREES));

    // 计算 yaw / pitch，使“前进方向”对齐速度方向
    // yaw: 水平角（绕 Y 轴）、pitch: 俯仰角
    float yaw = (float) Math.toDegrees(Math.atan2(look.x, look.z));
    double horizontalLength = Math.sqrt(look.x * look.x + look.z * look.z);
    float pitch = (float) Math.toDegrees(Math.atan2(-look.y, horizontalLength));

    // 说明：Item 模型（如剑）其“长度轴”通常沿 X 轴定义；
    // 参照箭矢/三叉戟的朝向做法：
    //   - 先绕 Y 轴旋转 (yaw - 90°)，把模型 X 轴映射到世界前进方向（-Z）
    //   - 再绕 Z 轴旋转 pitch，完成俯仰对齐
    // 这样可确保“剑尖”（模型 X 轴正向）永远指向运动路径
    poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0f));
    poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));

    // 取消自旋效果，保持剑头朝向路线
    // float spinAngle = (entity.tickCount + partialTicks) * 20.0f;
    // poseStack.mulPose(Axis.ZP.rotationDegrees(spinAngle));

    // 缩放
    poseStack.scale(1.0f, 1.0f, 1.0f);

    // 渲染实体指定的物品模型
    ItemStack display = entity.getDisplayItemStack();
    if (display == null || display.isEmpty()) {
      display = FALLBACK_DISPLAY_ITEM;
    }

    this.itemRenderer.renderStatic(
        display,
        ItemDisplayContext.NONE,
        packedLight,
        OverlayTexture.NO_OVERLAY,
        poseStack,
        buffer,
        entity.level(),
        entity.getId());

    poseStack.popPose();

    super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
  }

  @Override
  public ResourceLocation getTextureLocation(FlyingSwordEntity entity) {
    return InventoryMenu.BLOCK_ATLAS;
  }
}
