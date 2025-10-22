package net.tigereye.chestcavity.soul.fakeplayer.brain.intent;

import java.util.UUID;

/** 战斗意图：指明战斗风格、可选聚焦目标与 TTL。 */
public record CombatIntent(CombatStyle style, UUID focusTarget, int ttlTicks)
    implements BrainIntent {
  public static CombatIntent of(CombatStyle style, int ttl) {
    return new CombatIntent(style, null, ttl);
  }
}
