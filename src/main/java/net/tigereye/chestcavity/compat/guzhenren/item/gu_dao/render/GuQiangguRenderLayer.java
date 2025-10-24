package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.util.NBTCharge;

/** Placeholder render layer for骨枪蛊。后续将根据充能状态渲染骨枪模型。 */
public final class GuQiangguRenderLayer
    extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

  public GuQiangguRenderLayer(
      RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
    super(parent);
  }

  @Override
  public void render(
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      AbstractClientPlayer player,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch) {
    if (!shouldRender(player)) {
      return;
    }
    ItemStack renderStack = resolveRenderStack();
    if (renderStack.isEmpty() || player.isSpectator()) {
      return;
    }
    Optional<ChestCavityEntity> optional = ChestCavityEntity.of(player);
    if (optional.isEmpty()) {
      return;
    }
    ChestCavityInstance cc = optional.get().getChestCavityInstance();

    renderSpear(poseStack, buffer, packedLight, player, renderStack, true);
    renderSpear(poseStack, buffer, packedLight, player, renderStack, false);
  }

  private void renderSpear(
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      AbstractClientPlayer player,
      ItemStack renderStack,
      boolean rightArm) {
    poseStack.pushPose();
    if (rightArm) {
      getParentModel().rightArm.translateAndRotate(poseStack);
    } else {
      getParentModel().leftArm.translateAndRotate(poseStack);
    }
    // Render spear directly without debug adjustments
    Minecraft.getInstance()
        .getItemRenderer()
        .renderStatic(
            player,
            renderStack,
            ItemDisplayContext.GROUND,
            false,
            poseStack,
            buffer,
            player.level(),
            packedLight,
            OverlayTexture.NO_OVERLAY,
            player.getId());
    poseStack.popPose();
  }

  private static boolean shouldRender(AbstractClientPlayer player) {
    if (player == null) {
      return false;
    }
    if (player.isSpectator()) {
      return false;
    }
    Optional<ChestCavityEntity> optional = ChestCavityEntity.of(player);
    if (optional.isEmpty()) {
      return false;
    }
    for (int i = 0; i < optional.get().getChestCavityInstance().inventory.getContainerSize(); i++) {
      ItemStack stack = optional.get().getChestCavityInstance().inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (ORGAN_ITEM_ID.equals(id) && NBTCharge.getCharge(stack, STATE_KEY) > 0) {
        return true;
      }
    }
    return false;
  }

  private static final ResourceLocation ORGAN_ITEM_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "gu_qiang_gu");
  private static final String STATE_KEY = "GuQiangCharge";
  private static final ResourceLocation RENDER_ITEM_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "gu_qiang");

  private static ItemStack resolveRenderStack() {
    return BuiltInRegistries.ITEM
        .getOptional(RENDER_ITEM_ID)
        .map(ItemStack::new)
        .orElse(ItemStack.EMPTY);
  }
}
