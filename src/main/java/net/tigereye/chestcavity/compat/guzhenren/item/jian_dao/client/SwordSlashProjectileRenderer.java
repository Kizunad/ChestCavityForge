package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SwordSlashProjectile;

/**
 * Placeholder renderer – visuals are supplied by FX modules and particles spawned client-side.
 */
public class SwordSlashProjectileRenderer extends EntityRenderer<SwordSlashProjectile> {

    public SwordSlashProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(SwordSlashProjectile entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // No direct geometry – FX pipeline handles visuals for the sword light.
    }

    @Override
    public ResourceLocation getTextureLocation(SwordSlashProjectile entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
