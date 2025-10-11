package net.tigereye.chestcavity.soul.fakeplayer.brain.intent;

import net.minecraft.world.phys.Vec3;

/** 固守/驻留意图：在某个锚点附近保留位置。 */
public record HoldIntent(Vec3 anchor, int ttlTicks) implements BrainIntent {
    public static HoldIntent of(Vec3 pos, int ttl) {
        return new HoldIntent(pos, ttl);
    }
}

