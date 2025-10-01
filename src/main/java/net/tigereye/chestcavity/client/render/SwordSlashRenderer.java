package net.tigereye.chestcavity.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.entity.SwordSlashProjectile;
import org.joml.Matrix4f;

/**
 * Minimal glowing renderer for sword slash projectiles.
 */
public class SwordSlashRenderer extends EntityRenderer<SwordSlashProjectile> {

    private static final CCConfig.SwordSlashConfig DEFAULTS = new CCConfig.SwordSlashConfig();

    public SwordSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(SwordSlashProjectile entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        Vec3 motion = entity.getDeltaMovement();
        Vec3 direction = motion.lengthSqr() > 1.0E-4 ? motion.normalize() : entity.getForward();
        if (direction.lengthSqr() < 1.0E-4) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }
        float yaw = (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
        float pitch = (float) Math.toDegrees(Math.asin(Mth.clamp(direction.y, -1.0D, 1.0D)));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));

        float length = (float) entity.getLength();
        float halfWidth = (float) (entity.getThickness() * 0.5D);
        CCConfig.SwordSlashConfig config = config();
        float alphaStart = Mth.clamp((float) config.visuals.slashAlpha, 0.0F, 1.0F);
        float alphaEnd = Mth.clamp((float) config.visuals.slashEndAlpha, 0.0F, alphaStart);
        int color = config.visuals.slashColor;
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());

        addQuad(consumer, pose, -halfWidth, halfWidth, 0.0F, length, red, green, blue, alphaStart, alphaEnd);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose = poseStack.last();
        addQuad(consumer, pose, -halfWidth, halfWidth, 0.0F, length, red, green, blue, alphaStart * 0.6F, alphaEnd * 0.6F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void addQuad(VertexConsumer consumer, PoseStack.Pose pose, float minX, float maxX, float minZ, float maxZ,
                          float red, float green, float blue, float alphaStart, float alphaEnd) {
        addVertex(consumer, pose, minX, 0.0F, minZ, red, green, blue, alphaStart);
        addVertex(consumer, pose, maxX, 0.0F, minZ, red, green, blue, alphaStart);
        addVertex(consumer, pose, maxX, 0.0F, maxZ, red, green, blue, alphaEnd);
        addVertex(consumer, pose, minX, 0.0F, maxZ, red, green, blue, alphaEnd);
    }

    private void addVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                            float red, float green, float blue, float alpha) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setOverlay(0)
                .setLight(0x00F000F0)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static CCConfig.SwordSlashConfig config() {
        if (ChestCavity.config != null && ChestCavity.config.SWORD_SLASH != null) {
            return ChestCavity.config.SWORD_SLASH;
        }
        return DEFAULTS;
    }

    @Override
    public ResourceLocation getTextureLocation(SwordSlashProjectile entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
    }
}
