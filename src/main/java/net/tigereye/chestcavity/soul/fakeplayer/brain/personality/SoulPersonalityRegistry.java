package net.tigereye.chestcavity.soul.fakeplayer.brain.personality;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceLocation;

/**
 * Personality 注册表。支持运行时新增/覆盖，便于脚本或配置扩展。
 */
public final class SoulPersonalityRegistry {

    private static final Map<ResourceLocation, SoulPersonality> REGISTRY = new ConcurrentHashMap<>();

    public static final ResourceLocation DEFAULT_ID = ResourceLocation.fromNamespaceAndPath("chestcavity", "default");
    public static final ResourceLocation CAUTIOUS_ID = ResourceLocation.fromNamespaceAndPath("chestcavity", "cautious");

    private static final SoulPersonality DEFAULT_PERSONALITY = SoulPersonality.builder(DEFAULT_ID)
            .setDouble(BrainTuningKeys.SURVIVAL_RETREAT_MIN_DISTANCE, 10.0)
            .setDouble(BrainTuningKeys.SURVIVAL_RETREAT_MAX_DISTANCE, 14.0)
            .setDouble(BrainTuningKeys.SURVIVAL_RETREAT_JITTER_RADIANS, Math.PI / 6.0)
            .setDouble(BrainTuningKeys.SURVIVAL_THREAT_SCAN_RADIUS, 18.0)
            .setInt(BrainTuningKeys.SURVIVAL_SAFE_WINDOW_TICKS, 40)
            .setDouble(BrainTuningKeys.FOLLOW_OWNER_DISTANCE, 4.0)
            .setDouble(BrainTuningKeys.EXPLORATION_WANDER_RADIUS, 6.0)
            .build();

    private static final SoulPersonality CAUTIOUS_PERSONALITY = SoulPersonality.builder(CAUTIOUS_ID)
            .setDouble(BrainTuningKeys.SURVIVAL_RETREAT_MIN_DISTANCE, 14.0)
            .setDouble(BrainTuningKeys.SURVIVAL_RETREAT_MAX_DISTANCE, 20.0)
            .setDouble(BrainTuningKeys.SURVIVAL_RETREAT_JITTER_RADIANS, Math.PI / 5.0)
            .setDouble(BrainTuningKeys.SURVIVAL_THREAT_SCAN_RADIUS, 22.0)
            .setInt(BrainTuningKeys.SURVIVAL_SAFE_WINDOW_TICKS, 60)
            .setDouble(BrainTuningKeys.FOLLOW_OWNER_DISTANCE, 6.0)
            .setDouble(BrainTuningKeys.EXPLORATION_WANDER_RADIUS, 5.0)
            .build();

    static {
        register(DEFAULT_PERSONALITY);
        register(CAUTIOUS_PERSONALITY);
    }

    private SoulPersonalityRegistry() {
    }

    public static void register(SoulPersonality personality) {
        if (personality == null) {
            throw new IllegalArgumentException("personality cannot be null");
        }
        REGISTRY.put(personality.id(), personality);
    }

    public static SoulPersonality resolve(ResourceLocation id) {
        if (id == null) {
            return DEFAULT_PERSONALITY;
        }
        return REGISTRY.getOrDefault(id, DEFAULT_PERSONALITY);
    }

    public static SoulPersonality defaults() {
        return DEFAULT_PERSONALITY;
    }

    public static Collection<SoulPersonality> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static Optional<SoulPersonality> lookup(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String normalized = token.trim().toLowerCase(Locale.ROOT);
        for (SoulPersonality personality : REGISTRY.values()) {
            if (personality.id().toString().equalsIgnoreCase(normalized) ||
                    personality.id().getPath().equalsIgnoreCase(normalized)) {
                return Optional.of(personality);
            }
        }
        return Optional.empty();
    }
}
