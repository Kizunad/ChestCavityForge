package net.tigereye.chestcavity.guscript.fx;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Data-driven FX definition parsed from resource JSON.
 */
public record FxDefinition(List<FxModule> modules) {

    public FxDefinition {
        modules = modules == null ? List.of() : List.copyOf(modules);
    }

    public interface FxModule {
    }

    public record ParticleModule(ParticleSettings settings, Vec3 offset, int count) implements FxModule {
        public ParticleModule {
            settings = settings == null ? ParticleSettings.DEFAULT : settings;
            offset = offset == null ? Vec3.ZERO : offset;
            count = Math.max(1, count);
        }
    }

    public record SoundModule(ResourceLocation soundId, float volume, float pitch) implements FxModule {
        public SoundModule {
            volume = Math.max(0.0F, volume);
            pitch = Math.max(0.0F, pitch);
        }
    }

    public record ScreenShakeModule(float intensity, int durationTicks) implements FxModule {
        public ScreenShakeModule {
            intensity = Math.max(0.0F, intensity);
            durationTicks = Math.max(0, durationTicks);
        }
    }

    public record TrailModule(ParticleSettings settings, Vec3 offset, int segments, double spacing) implements FxModule {
        public TrailModule {
            settings = settings == null ? ParticleSettings.DEFAULT : settings;
            offset = offset == null ? Vec3.ZERO : offset;
            segments = Math.max(1, segments);
            spacing = spacing <= 0.0D ? 0.2D : spacing;
        }
    }

    public record ParticleSettings(ResourceLocation particleId, double speed, Vec3 spread, Integer primaryColor, Integer secondaryColor, float size) {
        static final ParticleSettings DEFAULT = new ParticleSettings(ResourceLocation.fromNamespaceAndPath("minecraft", "crit"), 0.0D, Vec3.ZERO, null, null, 1.0F);

        public ParticleSettings {
            speed = Math.max(0.0D, speed);
            spread = spread == null ? Vec3.ZERO : spread;
            size = Math.max(0.0F, size);
        }
    }
}
