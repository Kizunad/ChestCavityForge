package net.tigereye.chestcavity.soul.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.tigereye.chestcavity.soul.entity.TestSoulEntity;

/**
 * 简单的人形渲染器，复用僵尸模型。
 */
public final class TestSoulRenderer extends MobRenderer<TestSoulEntity, HumanoidModel<TestSoulEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("chestcavity", "textures/entity/yuedao_entity.png");

    public TestSoulRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(TestSoulEntity entity) {
        return TEXTURE;
    }
}
