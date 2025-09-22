package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;

import java.util.Optional;

class GuQiangguRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    GuQiangguRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        ItemStack renderStack = GuQiangguRenderUtil.getRenderStack();
        if (renderStack.isEmpty() || player.isSpectator()) {
            return;
        }
        Optional<ChestCavityEntity> optional = ChestCavityEntity.of(player);
        if (optional.isEmpty()) {
            return;
        }
        ChestCavityInstance cc = optional.get().getChestCavityInstance();
        if (!GuQiangguRenderUtil.hasActiveCharge(cc)) {
            return;
        }

        renderSpear(poseStack, buffer, packedLight, player, renderStack, true);
        renderSpear(poseStack, buffer, packedLight, player, renderStack, false);
    }

    private void renderSpear(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                             AbstractClientPlayer player, ItemStack renderStack, boolean rightArm) {
        poseStack.pushPose();
        if (rightArm) {
            getParentModel().rightArm.translateAndRotate(poseStack);
        } else {
            getParentModel().leftArm.translateAndRotate(poseStack);
        }
        GuQiangguDebugTuner.applyTransform(poseStack, rightArm, false);
        Minecraft.getInstance().getItemRenderer().renderStatic(player, renderStack, ItemDisplayContext.GROUND,
                false, poseStack, buffer, player.level(), packedLight, OverlayTexture.NO_OVERLAY, player.getId());
        poseStack.popPose();
    }
}
