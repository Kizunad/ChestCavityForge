package net.tigereye.chestcavity.soul.fakeplayer.brain.intent;

import java.util.UUID;

/** 跟随意图：跟随 owner 或指定实体，保持一定距离。 */
public record FollowIntent(UUID target, double distance, int ttlTicks) implements BrainIntent {
    public static FollowIntent of(UUID target, double distance, int ttl) {
        return new FollowIntent(target, distance, ttl);
    }
}

