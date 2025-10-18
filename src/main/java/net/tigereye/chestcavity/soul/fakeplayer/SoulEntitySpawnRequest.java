package net.tigereye.chestcavity.soul.fakeplayer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.guscript.runtime.flow.fx.GeckoFxAnchor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 请求在灵魂外壳生成时附带执行的额外效果。目前支持 Gecko FX。
 */
public record SoulEntitySpawnRequest(@Nullable GeckoFx geckoFx) {

    /**
     * 便捷构造：返回空请求实例。
     */
    public static SoulEntitySpawnRequest empty() {
        return new SoulEntitySpawnRequest(null);
    }

    /**
     * Gecko FX 参数，复用 FlowActions.emitGecko 的结构，并允许覆盖资源。
     */
    public record GeckoFx(
            ResourceLocation fxId,
            @Nullable ResourceLocation modelOverride,
            @Nullable ResourceLocation animationOverride,
            @Nullable ResourceLocation textureOverride,
            @Nullable GeckoFxAnchor anchor,
            @Nullable Vec3 offset,
            @Nullable Vec3 relativeOffset,
            @Nullable Vec3 worldPosition,
            @Nullable Float yaw,
            @Nullable Float pitch,
            @Nullable Float roll,
            float scale,
            int tint,
            float alpha,
            boolean loop,
            int durationTicks,
            int attachedEntityId,
            @Nullable UUID attachedEntityUuid
    ) {
        public GeckoFx {
            if (fxId == null) {
                throw new IllegalArgumentException("Gecko FX id must be provided");
            }
            scale = scale <= 0.0F ? 1.0F : scale;
            alpha = Mth.clamp(alpha <= 0.0F ? 1.0F : alpha, 0.0F, 1.0F);
            durationTicks = Math.max(1, durationTicks);
            if (attachedEntityId < 0) {
                attachedEntityId = -1;
            }
        }

        public static Builder builder(ResourceLocation fxId) {
            return new Builder(fxId);
        }

        public static final class Builder {
            private final ResourceLocation fxId;
            private ResourceLocation modelOverride;
            private ResourceLocation animationOverride;
            private ResourceLocation textureOverride;
            private GeckoFxAnchor anchor;
            private Vec3 offset = Vec3.ZERO;
            private Vec3 relativeOffset = Vec3.ZERO;
            private Vec3 worldPosition;
            private Float yaw;
            private Float pitch;
            private Float roll;
            private float scale = 1.0F;
            private int tint = 0xFFFFFF;
            private float alpha = 1.0F;
            private boolean loop;
            private int durationTicks = 40;
            private int attachedEntityId = -1;
            private UUID attachedEntityUuid;

            private Builder(ResourceLocation fxId) {
                this.fxId = fxId;
            }

            public Builder modelOverride(ResourceLocation modelOverride) {
                this.modelOverride = modelOverride;
                return this;
            }

            public Builder animationOverride(ResourceLocation animationOverride) {
                this.animationOverride = animationOverride;
                return this;
            }

            public Builder textureOverride(ResourceLocation textureOverride) {
                this.textureOverride = textureOverride;
                return this;
            }

            public Builder anchor(GeckoFxAnchor anchor) {
                this.anchor = anchor;
                return this;
            }

            public Builder offset(Vec3 offset) {
                this.offset = offset == null ? Vec3.ZERO : offset;
                return this;
            }

            public Builder relativeOffset(Vec3 relativeOffset) {
                this.relativeOffset = relativeOffset == null ? Vec3.ZERO : relativeOffset;
                return this;
            }

            public Builder worldPosition(Vec3 worldPosition) {
                this.worldPosition = worldPosition;
                return this;
            }

            public Builder yaw(Float yaw) {
                this.yaw = yaw;
                return this;
            }

            public Builder pitch(Float pitch) {
                this.pitch = pitch;
                return this;
            }

            public Builder roll(Float roll) {
                this.roll = roll;
                return this;
            }

            public Builder scale(float scale) {
                this.scale = scale;
                return this;
            }

            public Builder tint(int tint) {
                this.tint = tint;
                return this;
            }

            public Builder alpha(float alpha) {
                this.alpha = alpha;
                return this;
            }

            public Builder loop(boolean loop) {
                this.loop = loop;
                return this;
            }

            public Builder durationTicks(int durationTicks) {
                this.durationTicks = durationTicks;
                return this;
            }

            public Builder attachedEntityId(int attachedEntityId) {
                this.attachedEntityId = attachedEntityId;
                return this;
            }

            public Builder attachedEntityUuid(UUID attachedEntityUuid) {
                this.attachedEntityUuid = attachedEntityUuid;
                return this;
            }

            public GeckoFx build() {
                return new GeckoFx(
                        fxId,
                        modelOverride,
                        animationOverride,
                        textureOverride,
                        anchor,
                        offset,
                        relativeOffset,
                        worldPosition,
                        yaw,
                        pitch,
                        roll,
                        scale,
                        tint,
                        alpha,
                        loop,
                        durationTicks,
                        attachedEntityId,
                        attachedEntityUuid
                );
            }
        }
    }
}
