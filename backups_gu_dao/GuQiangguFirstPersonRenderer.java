package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.tigereye.chestcavity.ChestCavity;

@EventBusSubscriber(modid = ChestCavity.MODID, value = Dist.CLIENT)
final class GuQiangguFirstPersonRenderer {

    private GuQiangguFirstPersonRenderer() {
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        var player = event.getPlayer();
        if (!GuQiangguRenderUtil.hasActiveCharge(player)) {
            return;
        }
        ItemStack renderStack = GuQiangguRenderUtil.getRenderStack();
        if (renderStack.isEmpty()) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        boolean rightArm = event.getArm() == HumanoidArm.RIGHT;
        GuQiangguDebugTuner.applyTransform(poseStack, rightArm, true);
        MultiBufferSource buffers = event.getMultiBufferSource();
        Minecraft.getInstance().getItemRenderer().renderStatic(player, renderStack, ItemDisplayContext.GROUND,
                false, poseStack, buffers, player.level(), event.getPackedLight(), OverlayTexture.NO_OVERLAY,
                player.getId());
        poseStack.popPose();
    }
}
