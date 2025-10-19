package net.tigereye.chestcavity.soul.fakeplayer.brain.personality;

import net.minecraft.resources.ResourceLocation;

/**
 * 常量键表：用于在 {@link SoulPersonality} 中存储/检索可调参数。
 */
public final class BrainTuningKeys {

    private static final String NAMESPACE = "chestcavity";

    private BrainTuningKeys() {
    }

    public static final ResourceLocation SURVIVAL_RETREAT_MIN_DISTANCE =
            ResourceLocation.fromNamespaceAndPath(NAMESPACE, "brain/survival/retreat_min_distance");
    public static final ResourceLocation SURVIVAL_RETREAT_MAX_DISTANCE =
            ResourceLocation.fromNamespaceAndPath(NAMESPACE, "brain/survival/retreat_max_distance");
    public static final ResourceLocation SURVIVAL_RETREAT_JITTER_RADIANS =
            ResourceLocation.fromNamespaceAndPath(NAMESPACE, "brain/survival/retreat_jitter_radians");
    public static final ResourceLocation SURVIVAL_THREAT_SCAN_RADIUS =
            ResourceLocation.fromNamespaceAndPath(NAMESPACE, "brain/survival/threat_scan_radius");
    public static final ResourceLocation SURVIVAL_SAFE_WINDOW_TICKS =
            ResourceLocation.fromNamespaceAndPath(NAMESPACE, "brain/survival/safe_window_ticks");
    public static final ResourceLocation FOLLOW_OWNER_DISTANCE =
            ResourceLocation.fromNamespaceAndPath(NAMESPACE, "brain/follow/owner_distance");
    public static final ResourceLocation EXPLORATION_WANDER_RADIUS =
            ResourceLocation.fromNamespaceAndPath(NAMESPACE, "brain/exploration/wander_radius");
}
