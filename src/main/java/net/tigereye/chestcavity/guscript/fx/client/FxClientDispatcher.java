package net.tigereye.chestcavity.guscript.fx.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.fx.FxDefinition;
import net.tigereye.chestcavity.guscript.fx.FxDefinition.ParticleModule;
import net.tigereye.chestcavity.guscript.fx.FxDefinition.ParticleSettings;
import net.tigereye.chestcavity.guscript.fx.FxDefinition.ScreenShakeModule;
import net.tigereye.chestcavity.guscript.fx.FxDefinition.SoundModule;
import net.tigereye.chestcavity.guscript.fx.FxDefinition.TrailModule;
import net.tigereye.chestcavity.guscript.fx.FxRegistry;
import net.tigereye.chestcavity.guscript.network.packets.FxEventPayload;
import org.joml.Vector3f;

/**
 * Applies incoming FX network payloads on the client.
 */
public final class FxClientDispatcher {

    private FxClientDispatcher() {}

    public static void handle(FxEventPayload payload) {
        Vec3 origin = new Vec3(payload.originX(), payload.originY(), payload.originZ());
        Vec3 fallback = new Vec3(payload.directionX(), payload.directionY(), payload.directionZ());
        Vec3 look = new Vec3(payload.lookX(), payload.lookY(), payload.lookZ());
        Vec3 target = payload.hasTarget() ? new Vec3(payload.targetX(), payload.targetY(), payload.targetZ()) : null;
        FxRegistry.FxContext context = new FxRegistry.FxContext(origin, fallback, look, target, payload.intensity(), payload.performerId(), payload.targetId());
        play(payload.effectId(), context);
    }

    public static void play(ResourceLocation id, FxRegistry.FxContext context) {
        if (id == null || context == null) {
            return;
        }
        FxDefinition definition = FxRegistry.definition(id).orElse(null);
        if (definition == null) {
            ChestCavity.LOGGER.warn("[GuScript] Unknown FX id {}", id);
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        RandomSource random = level.getRandom();
        for (FxDefinition.FxModule module : definition.modules()) {
            if (module instanceof SoundModule soundModule) {
                playSound(level, context, soundModule);
            } else if (module instanceof ParticleModule particleModule) {
                spawnParticles(level, context, particleModule, random);
            } else if (module instanceof TrailModule trailModule) {
                spawnTrail(level, context, trailModule, random);
            } else if (module instanceof ScreenShakeModule screenShake) {
                FxClientHooks.addScreenShake(screenShake.intensity() * context.intensity(), screenShake.durationTicks());
            }
        }
    }

    private static void playSound(ClientLevel level, FxRegistry.FxContext context, SoundModule module) {
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(module.soundId());
        if (event == null) {
            ChestCavity.LOGGER.warn("[GuScript] Unknown sound event {}", module.soundId());
            return;
        }
        level.playLocalSound(context.origin().x, context.origin().y, context.origin().z, event, SoundSource.PLAYERS, module.volume() * context.intensity(), module.pitch(), false);
    }

    private static void spawnParticles(ClientLevel level, FxRegistry.FxContext context, ParticleModule module, RandomSource random) {
        ParticleOptions options = resolveParticleOptions(module.settings());
        if (options == null) {
            return;
        }
        int count = Math.max(1, Math.round(module.count() * context.intensity()));
        Vec3 origin = context.origin().add(module.offset());
        Vec3 spread = module.settings().spread();
        double speed = module.settings().speed();
        for (int i = 0; i < count; i++) {
            double dx = random.nextGaussian() * spread.x;
            double dy = random.nextGaussian() * spread.y;
            double dz = random.nextGaussian() * spread.z;
            level.addParticle(options, origin.x + dx, origin.y + dy, origin.z + dz, dx * speed, dy * speed, dz * speed);
        }
    }

    private static void spawnTrail(ClientLevel level, FxRegistry.FxContext context, TrailModule module, RandomSource random) {
        ParticleOptions options = resolveParticleOptions(module.settings());
        if (options == null) {
            return;
        }
        Vec3 direction = context.resolveDirection();
        if (direction.lengthSqr() < 1.0E-4) {
            direction = new Vec3(0.0, 1.0, 0.0);
        }
        direction = direction.normalize().scale(module.spacing());
        Vec3 start = context.origin().add(module.offset());
        Vec3 current = start;
        int segments = Math.max(1, Math.round(module.segments() * context.intensity()));
        Vec3 spread = module.settings().spread();
        double speed = module.settings().speed();
        for (int i = 0; i < segments; i++) {
            double dx = random.nextGaussian() * spread.x;
            double dy = random.nextGaussian() * spread.y;
            double dz = random.nextGaussian() * spread.z;
            level.addParticle(options, current.x + dx, current.y + dy, current.z + dz, dx * speed, dy * speed, dz * speed);
            current = current.add(direction);
        }
    }

    private static ParticleOptions resolveParticleOptions(ParticleSettings settings) {
        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(settings.particleId());
        if (type == null) {
            ChestCavity.LOGGER.warn("[GuScript] Unknown particle type {}", settings.particleId());
            return null;
        }
        if (type instanceof SimpleParticleType simple) {
            return simple;
        }
        if ("minecraft:dust".equals(settings.particleId().toString()) && settings.color() != null) {
            float r = ((settings.color() >> 16) & 0xFF) / 255.0F;
            float g = ((settings.color() >> 8) & 0xFF) / 255.0F;
            float b = (settings.color() & 0xFF) / 255.0F;
            float size = settings.size() <= 0.0F ? 1.0F : settings.size();
            return new DustParticleOptions(new Vector3f(r, g, b), size);
        }
        ChestCavity.LOGGER.warn("[GuScript] Unsupported particle type {} for FX", settings.particleId());
        return null;

    }
}
