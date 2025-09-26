package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SingleSwordProjectile;

/**
 * Minimal renderer that relies on the projectile's particle system for visuals. The renderer itself
 * returns the particle texture so the entity remains invisible while still being tracked by the
 * client.
 */
public class SingleSwordProjectileRenderer extends EntityRenderer<SingleSwordProjectile> {

    public SingleSwordProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            SingleSwordProjectile entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        // Intentionally left blank – particles provide the visual feedback.
    }

    @Override
    public ResourceLocation getTextureLocation(SingleSwordProjectile entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}

