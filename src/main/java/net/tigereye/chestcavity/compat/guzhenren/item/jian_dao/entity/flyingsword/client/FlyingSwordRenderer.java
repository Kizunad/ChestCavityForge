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

    // 获取速度方向
    Vec3 velocity = entity.getDeltaMovement();
    Vec3 look;

    if (velocity.lengthSqr() > 1.0e-4) {
      // 使用速度方向
      look = velocity.normalize();
    } else {
      // 静止时使用实体朝向
      look = entity.getLookAngle();
    }

    // 计算yaw和pitch
    float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
    float pitch =
        (float) Math.toDegrees(Math.atan2(-look.y, Math.sqrt(look.x * look.x + look.z * look.z)));

    // 应用旋转
    poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
    poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

    // 模型基准修正：让剑尖朝前
    poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0f));

    // 轻微旋转（飞行时的自旋）
    float spinAngle = (entity.tickCount + partialTicks) * 20.0f;
    poseStack.mulPose(Axis.ZP.rotationDegrees(spinAngle));

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
