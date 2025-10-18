package net.tigereye.chestcavity.guscript.fx.gecko.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.fx.gecko.GeckoFxDefinition;
import net.tigereye.chestcavity.guscript.fx.gecko.GeckoFxRegistry;
import net.tigereye.chestcavity.guscript.network.packets.GeckoFxEventPayload;
import net.tigereye.chestcavity.guscript.runtime.flow.fx.GeckoFxAnchor;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.Color;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Client-side holder for GeckoLib FX events dispatched from the server.
 */
@OnlyIn(Dist.CLIENT)
public final class GeckoFxClient {

    private static final GeckoFxManager MANAGER = new GeckoFxManager();

    private GeckoFxClient() {
    }

    public static void handle(GeckoFxEventPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level != null) {
            MANAGER.enqueue(level, level.getGameTime(), payload);
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level != null) {
            MANAGER.tick(level, level.getGameTime());
        }
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return;
        }
        MANAGER.render(level, event);
    }

    /** Visible for unit tests. */
    static GeckoFxManager managerForTests() {
        return MANAGER;
    }

    @OnlyIn(Dist.CLIENT)
    public static final class GeckoFxManager {

        private final Map<UUID, ActiveFx> active = new HashMap<>();
        private final FxRenderer renderer = new FxRenderer();

        public void enqueue(Level level, long gameTime, GeckoFxEventPayload payload) {
            if (payload == null) {
                return;
            }
            Optional<GeckoFxDefinition> definitionOptional = GeckoFxRegistry.definition(payload.fxId());
            if (definitionOptional.isEmpty()) {
                ChestCavity.LOGGER.warn("[GuScript] Missing Gecko FX definition {}", payload.fxId());
                return;
            }
            GeckoFxDefinition definition = mergeDefinition(definitionOptional.get(), payload);
            float payloadScale = payload.scale() <= 0.0F ? 1.0F : payload.scale();
            float combinedScale = payloadScale * Math.max(0.0001F, definition.defaultScale());
            float combinedAlpha = Mth.clamp(payload.alpha() * definition.defaultAlpha(), 0.0F, 1.0F);
            int combinedTint = multiplyTints(definition.defaultTint(), payload.tint());
            ActiveFx fx = new ActiveFx(payload, definition, gameTime, combinedScale, combinedTint, combinedAlpha);
            active.put(payload.eventId(), fx);
        }

        public void tick(Level level, long gameTime) {
            Iterator<Map.Entry<UUID, ActiveFx>> iterator = active.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, ActiveFx> entry = iterator.next();
                ActiveFx fx = entry.getValue();
                int ttl = Math.max(1, fx.payload.duration());
                if (gameTime - fx.lastRefreshedAtTick > ttl) {
                    iterator.remove();
                }
            }
        }

        public void render(Level level, RenderLevelStageEvent event) {
            if (active.isEmpty()) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            PoseStack poseStack = event.getPoseStack();
            Camera camera = event.getCamera();
            Vec3 cameraPos = camera.getPosition();
            MultiBufferSource buffer = minecraft.renderBuffers().bufferSource();
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);

            Iterator<Map.Entry<UUID, ActiveFx>> iterator = active.entrySet().iterator();
            while (iterator.hasNext()) {
                ActiveFx fx = iterator.next().getValue();
                GeckoFxEventPayload payload = fx.payload;
                Vec3 basePosition;
                if (payload.anchor() == GeckoFxAnchor.WORLD) {
                    basePosition = new Vec3(payload.basePosX(), payload.basePosY(), payload.basePosZ());
                } else {
                    Entity anchor = level.getEntity(payload.attachedEntityId());
                    if ((anchor == null || anchor.isRemoved()) && payload.attachedEntityUuid() != null && level instanceof ClientLevel clientLevel) {
                        anchor = clientLevel.getEntity(payload.attachedEntityUuid());
                    }
                    if (anchor == null || anchor.isRemoved()) {
                        iterator.remove();
                        continue;
                    }
                    double x = Mth.lerp(partialTick, anchor.xo, anchor.getX());
                    double y = Mth.lerp(partialTick, anchor.yo, anchor.getY());
                    double z = Mth.lerp(partialTick, anchor.zo, anchor.getZ());
                    basePosition = new Vec3(x, y, z);
                }

                poseStack.pushPose();
                poseStack.translate(basePosition.x - cameraPos.x, basePosition.y - cameraPos.y, basePosition.z - cameraPos.z);
                poseStack.translate(payload.offsetX(), payload.offsetY(), payload.offsetZ());
                poseStack.mulPose(Axis.YP.rotationDegrees(payload.yaw()));
                poseStack.mulPose(Axis.XP.rotationDegrees(payload.pitch()));
                poseStack.mulPose(Axis.ZP.rotationDegrees(payload.roll()));
                poseStack.translate(payload.relativeOffsetX(), payload.relativeOffsetY(), payload.relativeOffsetZ());
                poseStack.scale(fx.scale, fx.scale, fx.scale);

                FxAnimatable animatable = fx.animatable;
                animatable.updateVisuals(fx.tint, fx.alpha);
                animatable.updateTick(level.getGameTime() + partialTick);

                RenderType renderType = renderer.getRenderType(animatable, animatable.definition().texture(), buffer, partialTick);
                renderer.render(poseStack, animatable, buffer, renderType, buffer.getBuffer(renderType), LightTexture.FULL_BRIGHT, partialTick);

                poseStack.popPose();
            }
        }

        public int activeCount() {
            return active.size();
        }

        public void clear() {
            active.clear();
        }

        private static GeckoFxDefinition mergeDefinition(GeckoFxDefinition base, GeckoFxEventPayload payload) {
            ResourceLocation model = payload.modelOverride() != null ? payload.modelOverride() : base.model();
            ResourceLocation texture = payload.textureOverride() != null ? payload.textureOverride() : base.texture();
            ResourceLocation animation = payload.animationOverride() != null ? payload.animationOverride() : base.animation();
            return new GeckoFxDefinition(
                    base.id(),
                    model,
                    texture,
                    animation,
                    base.defaultAnimation(),
                    base.defaultScale(),
                    base.defaultTint(),
                    base.defaultAlpha(),
                    base.blendMode()
            );
        }

        private static int multiplyTints(int base, int overlay) {
            int baseR = (base >> 16) & 0xFF;
            int baseG = (base >> 8) & 0xFF;
            int baseB = base & 0xFF;
            int overR = (overlay >> 16) & 0xFF;
            int overG = (overlay >> 8) & 0xFF;
            int overB = overlay & 0xFF;
            int r = (baseR * overR) / 255;
            int g = (baseG * overG) / 255;
            int b = (baseB * overB) / 255;
            return (r << 16) | (g << 8) | b;
        }

        private static final class FxRenderer extends GeoObjectRenderer<FxAnimatable> {
            FxRenderer() {
                super(new FxModel());
            }

            @Override
            public RenderType getRenderType(FxAnimatable animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
                return switch (animatable.definition().blendMode()) {
                    case OPAQUE -> RenderType.entitySolid(texture);
                    case CUTOUT -> RenderType.entityCutout(texture);
                    case TRANSLUCENT -> RenderType.entityTranslucent(texture);
                };
            }

            @Override
            public Color getRenderColor(FxAnimatable animatable, float partialTick, int packedLight) {
                return Color.ofRGBA(animatable.red(), animatable.green(), animatable.blue(), animatable.alpha());
            }
        }

        private static final class FxModel extends GeoModel<FxAnimatable> {
            @Override
            public ResourceLocation getModelResource(FxAnimatable animatable) {
                return animatable.definition().model();
            }

            @Override
            public ResourceLocation getTextureResource(FxAnimatable animatable) {
                return animatable.definition().texture();
            }

            @Override
            public ResourceLocation getAnimationResource(FxAnimatable animatable) {
                return animatable.definition().animation();
            }
        }

        private static final class FxAnimatable implements SingletonGeoAnimatable {
            private final GeckoFxDefinition definition;
            private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
            private final RawAnimation loopAnimation;
            private double animationTick;
            private float red = 1.0F;
            private float green = 1.0F;
            private float blue = 1.0F;
            private float alpha = 1.0F;

            private FxAnimatable(GeckoFxDefinition definition) {
                this.definition = definition;
                if (definition.defaultAnimation() != null && !definition.defaultAnimation().isBlank()) {
                    this.loopAnimation = RawAnimation.begin().thenLoop(definition.defaultAnimation());
                } else {
                    this.loopAnimation = null;
                }
            }

            void updateVisuals(int tint, float alpha) {
                this.red = ((tint >> 16) & 0xFF) / 255.0F;
                this.green = ((tint >> 8) & 0xFF) / 255.0F;
                this.blue = (tint & 0xFF) / 255.0F;
                this.alpha = Mth.clamp(alpha, 0.0F, 1.0F);
            }

            void updateTick(double tick) {
                this.animationTick = tick;
            }

            GeckoFxDefinition definition() {
                return definition;
            }

            float red() {
                return red;
            }

            float green() {
                return green;
            }

            float blue() {
                return blue;
            }

            float alpha() {
                return alpha;
            }

            @Override
            public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
                if (loopAnimation != null) {
                    controllers.add(new AnimationController<>(this, "fx", state -> {
                        state.setAnimation(loopAnimation);
                        return PlayState.CONTINUE;
                    }));
                }
            }

            @Override
            public AnimatableInstanceCache getAnimatableInstanceCache() {
                return cache;
            }

            @Override
            public double getTick(Object object) {
                return animationTick;
            }
        }

        private static final class ActiveFx {
            private final GeckoFxEventPayload payload;
            private final FxAnimatable animatable;
            private final float scale;
            private final int tint;
            private final float alpha;
            private final long lastRefreshedAtTick;

            private ActiveFx(GeckoFxEventPayload payload, GeckoFxDefinition definition, long refreshedAtTick, float scale, int tint, float alpha) {
                this.payload = payload;
                this.animatable = new FxAnimatable(definition);
                this.scale = scale;
                this.tint = tint;
                this.alpha = alpha;
                this.lastRefreshedAtTick = refreshedAtTick;
            }
        }
    }
}
