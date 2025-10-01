package net.tigereye.chestcavity.guscript.fx.gecko;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;

/**
 * Data definition for GeckoLib-based FX instances.
 */
public record GeckoFxDefinition(
        ResourceLocation id,
        ResourceLocation model,
        ResourceLocation texture,
        ResourceLocation animation,
        String defaultAnimation,
        float defaultScale,
        int defaultTint,
        float defaultAlpha,
        BlendMode blendMode
) {

    public GeckoFxDefinition {
        if (id == null) {
            throw new IllegalArgumentException("Gecko FX id cannot be null");
        }
        if (model == null) {
            throw new IllegalArgumentException("Gecko FX model cannot be null for " + id);
        }
        if (texture == null) {
            throw new IllegalArgumentException("Gecko FX texture cannot be null for " + id);
        }
        if (animation == null) {
            throw new IllegalArgumentException("Gecko FX animation cannot be null for " + id);
        }
        if (blendMode == null) {
            throw new IllegalArgumentException("Gecko FX blend mode cannot be null for " + id);
        }
        defaultScale = defaultScale <= 0.0F ? 1.0F : defaultScale;
        defaultAlpha = defaultAlpha <= 0.0F ? 1.0F : Math.min(defaultAlpha, 1.0F);
    }

    public enum BlendMode {
        OPAQUE,
        CUTOUT,
        TRANSLUCENT;

        public static BlendMode fromString(String raw, BlendMode fallback) {
            if (raw == null) {
                return fallback;
            }
            return switch (raw.trim().toLowerCase()) {
                case "opaque", "solid" -> OPAQUE;
                case "cutout", "alpha" -> CUTOUT;
                case "translucent", "transparent" -> TRANSLUCENT;
                default -> {
                    ChestCavity.LOGGER.warn("[GuScript] Unknown Gecko FX blend mode {}", raw);
                    yield fallback;
                }
            };
        }
    }
}
