package net.tigereye.chestcavity.guscript.fx;

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
import net.tigereye.chestcavity.guscript.fx.FxDefinition.ParticleModule;
import net.tigereye.chestcavity.guscript.fx.FxDefinition.ParticleSettings;
import net.tigereye.chestcavity.guscript.fx.FxDefinition.ScreenShakeModule;
import net.tigereye.chestcavity.guscript.fx.FxDefinition.SoundModule;
import net.tigereye.chestcavity.guscript.fx.FxDefinition.TrailModule;
import net.tigereye.chestcavity.guscript.fx.client.FxClientHooks;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry storing FX definitions and providing client-side playback utilities.
 */
public final class FxRegistry {

    private static final Map<ResourceLocation, FxDefinition> DEFINITIONS = new HashMap<>();

    private FxRegistry() {}

    public static synchronized void updateDefinitions(Map<ResourceLocation, FxDefinition> definitions) {
        DEFINITIONS.clear();
        DEFINITIONS.putAll(definitions);
        ChestCavity.LOGGER.info("[GuScript] Loaded {} FX definitions", DEFINITIONS.size());
    }

    public static synchronized Optional<FxDefinition> definition(ResourceLocation id) {
        return Optional.ofNullable(DEFINITIONS.get(id));
    }

    public static synchronized Map<ResourceLocation, FxDefinition> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(DEFINITIONS));
    }

    public static void play(ResourceLocation id, FxContext context) {
        if (id == null || context == null) {
            return;
        }
        FxDefinition definition = definition(id).orElse(null);
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

    private static void playSound(ClientLevel level, FxContext context, SoundModule module) {
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(module.soundId());
        if (event == null) {
            ChestCavity.LOGGER.warn("[GuScript] Unknown sound event {}", module.soundId());
            return;
        }
        level.playLocalSound(context.origin().x, context.origin().y, context.origin().z, event, SoundSource.PLAYERS, module.volume() * context.intensity(), module.pitch(), false);
    }

    private static void spawnParticles(ClientLevel level, FxContext context, ParticleModule module, RandomSource random) {
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

    private static void spawnTrail(ClientLevel level, FxContext context, TrailModule module, RandomSource random) {
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

    public record FxContext(Vec3 origin, Vec3 fallbackDirection, Vec3 look, Vec3 target, float intensity, int performerId, int targetId) {
        public FxContext {
            origin = origin == null ? Vec3.ZERO : origin;
            fallbackDirection = fallbackDirection == null ? Vec3.ZERO : fallbackDirection;
            look = look == null ? Vec3.ZERO : look;
            intensity = Float.isNaN(intensity) || intensity <= 0.0F ? 1.0F : intensity;
        }

        public Vec3 resolveDirection() {
            if (target != null) {
                return target.subtract(origin);
            }
            if (fallbackDirection.lengthSqr() > 1.0E-4) {
                return fallbackDirection;
            }
            return look;
        }
    }
}
