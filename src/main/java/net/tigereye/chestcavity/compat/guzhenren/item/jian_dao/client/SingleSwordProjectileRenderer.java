package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.client;

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
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SingleSwordProjectile;

/**
 * Renders the Jian Dao single sword projectile by drawing its configured ItemStack with a
 * directional transform. The particles emitted by the entity remain for additional flair, but the
 * core sword model is now visible via this renderer.
 */
public class SingleSwordProjectileRenderer extends EntityRenderer<SingleSwordProjectile> {

  private final ItemRenderer itemRenderer;

  public SingleSwordProjectileRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.itemRenderer = context.getItemRenderer();
    this.shadowRadius = 0.0f;
  }

  @Override
  public void render(
      SingleSwordProjectile entity,
      float entityYaw,
      float partialTicks,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight) {
    ItemStack stack = entity.getDisplayItem();
    if (stack.isEmpty()) {
      return;
    }

    poseStack.pushPose();
    poseStack.translate(0.0, 0.1, 0.0);

    // --- (0) 模型基准修正：把“模型自身的前向”旋到世界 +Z ---
    // poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f));
    // 如果刀面方向不对，可以再开这一行试试：
    poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0f));

    // --- (1) 对齐到玩家前方（用相机视线更稳） ---
    var camEntity = this.entityRenderDispatcher.camera.getEntity();
    var look = camEntity.getViewVector(partialTicks).normalize();

    float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
    float pitch =
        (float) Math.toDegrees(Math.atan2(-look.y, Math.sqrt(look.x * look.x + look.z * look.z)));

    poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
    poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

    // --- (2) 叠加“前下刺”的动画偏移 ---
    poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
    // 如需左右偏移，可以加：
    // poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));

    // --- (3) 位置和缩放 ---
    poseStack.translate(0.0, 0.0, 0.55); // 向前推一点
    poseStack.scale(1.25f, 1.25f, 1.25f);

    this.itemRenderer.renderStatic(
        stack,
        ItemDisplayContext.NONE, // 避免 GROUND 默认的旋转/缩放
        packedLight,
        OverlayTexture.NO_OVERLAY,
        poseStack,
        buffer,
        entity.level(),
        entity.getId());

    poseStack.popPose();
  }

  @Override
  public ResourceLocation getTextureLocation(SingleSwordProjectile entity) {
    return InventoryMenu.BLOCK_ATLAS;
  }
}
