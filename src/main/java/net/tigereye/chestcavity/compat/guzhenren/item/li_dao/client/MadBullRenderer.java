package net.tigereye.chestcavity.compat.guzhenren.item.li_dao.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cow;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.entity.MadBullEntity;

/**
 * 极简可见化：使用原版牛模型和贴图渲染 MadBullEntity，使其“看得见”。
 * - 不参与动画骨骼，仅按实体移动方向朝向；
 * - 轻量缩放，避免与实体碰撞盒大小不符；
 * - 未来如需自定义外观，可替换为专用模型和材质。
 */
public class MadBullRenderer extends EntityRenderer<MadBullEntity> {

    private static final ResourceLocation COW_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/cow/cow.png");
    private final CowModel<Cow> model;

    public MadBullRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new CowModel<>(context.bakeLayer(ModelLayers.COW));
        this.shadowRadius = 0.35f;
    }

    @Override
    public void render(MadBullEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // 平移到实体中心（牛模型原点在脚下，略微上抬）
        poseStack.translate(0.0, 0.5, 0.0);

        // 朝向：优先用速度方向，其次用 entityYaw
        var motion = entity.getDeltaMovement();
        float yaw;
        if (motion.lengthSqr() > 1.0e-4) {
            yaw = (float) (Math.toDegrees(Math.atan2(-motion.x, motion.z)));
        } else {
            yaw = entityYaw;
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));

        // 轻量缩放至与碰撞盒相仿
        poseStack.scale(0.8f, 0.8f, 0.8f);

        // 渲染
        var vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(COW_TEXTURE));
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MadBullEntity entity) {
        return COW_TEXTURE;
    }
}
