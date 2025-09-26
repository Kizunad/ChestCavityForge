package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.FastColor;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SwordShadowClone;

/**
 * Renders sword shadow clones as translucent, tinted versions of the owning player's skin.
 */
public class SwordShadowCloneRenderer extends HumanoidMobRenderer<SwordShadowClone, PlayerModel<SwordShadowClone>> {

    public SwordShadowCloneRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.3f);
        this.addLayer(new TintLayer(this, context));
    }

    @Override
    protected boolean shouldShowName(SwordShadowClone entity) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(SwordShadowClone entity) {
        return entity.getSkinTexture();
    }

    @Override
    public void render(
            SwordShadowClone entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        boolean previous = entity.isInvisible();
        entity.setInvisible(true);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        entity.setInvisible(previous);
    }

    private static final class TintLayer extends RenderLayer<SwordShadowClone, PlayerModel<SwordShadowClone>> {
        private final PlayerModel<SwordShadowClone> slimModel;

        private TintLayer(SwordShadowCloneRenderer renderer, EntityRendererProvider.Context context) {
            super(renderer);
            this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        }

        @Override
        public void render(
                PoseStack poseStack,
                MultiBufferSource buffer,
                int packedLight,
                SwordShadowClone entity,
                float limbSwing,
                float limbSwingAmount,
                float partialTicks,
                float ageInTicks,
                float netHeadYaw,
                float headPitch
        ) {
            PlayerModel<SwordShadowClone> base = this.getParentModel();
            PlayerModel<SwordShadowClone> model = "slim".equals(entity.getSkinModel()) ? slimModel : base;
            base.copyPropertiesTo(model);
            float[] tint = entity.getTintComponents();
            int argb = FastColor.ARGB32.color(
                    (int) (tint[3] * 255.0f),
                    (int) (tint[0] * 255.0f),
                    (int) (tint[1] * 255.0f),
                    (int) (tint[2] * 255.0f)
            );
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(entity.getSkinTexture()));
            model.renderToBuffer(
                    poseStack,
                    consumer,
                    packedLight,
                    LivingEntityRenderer.getOverlayCoords(entity, 0.0f),
                    argb
            );
        }
    }
}
