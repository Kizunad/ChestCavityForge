package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;

import java.util.Optional;

/**
 * Renders the guzhenren bone spear item slightly embedded into the player's main arm.
 */
public class GuQiangguRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation GU_QIANG_ID = ResourceLocation.fromNamespaceAndPath("guzhenren", "gu_qiang");
    private static final ResourceLocation GU_QIANG_GU_ID = ResourceLocation.fromNamespaceAndPath("guzhenren", "gu_qiang_gu");
    private static final ItemStack RENDER_STACK = BuiltInRegistries.ITEM.getOptional(GU_QIANG_ID)
            .map(ItemStack::new)
            .orElse(ItemStack.EMPTY);

    public GuQiangguRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (RENDER_STACK.isEmpty() || player.isSpectator()) {
            return;
        }
        if (!hasGuQiangOrgan(player)) {
            return;
        }

        boolean rightArm = player.getMainArm() == HumanoidArm.RIGHT;

        poseStack.pushPose();
        if (rightArm) {
            getParentModel().rightArm.translateAndRotate(poseStack);
        } else {
            getParentModel().leftArm.translateAndRotate(poseStack);
        }

        float xOffset = rightArm ? -0.08f : 0.08f;
        float zOffset = rightArm ? -0.12f : 0.12f;
        float zRot = rightArm ? 35f : -35f;

        poseStack.translate(xOffset, 0.32f, zOffset);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));

        Minecraft.getInstance().getItemRenderer().renderStatic(player, RENDER_STACK, ItemDisplayContext.GROUND,
                false, poseStack, buffer, player.level(), packedLight, OverlayTexture.NO_OVERLAY,
                player.getId());
        poseStack.popPose();
    }

    private static boolean hasGuQiangOrgan(AbstractClientPlayer player) {
        Optional<ChestCavityEntity> optional = ChestCavityEntity.of(player);
        if (optional.isEmpty()) {
            return false;
        }
        ChestCavityInstance cc = optional.get().getChestCavityInstance();
        if (cc == null) {
            return false;
        }
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(GU_QIANG_GU_ID)) {
                return true;
            }
        }
        return false;
    }
}
