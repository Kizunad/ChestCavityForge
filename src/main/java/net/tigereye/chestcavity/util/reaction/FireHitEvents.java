package net.tigereye.chestcavity.util.reaction;

import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;

/** 捕获“非火衣”的通用火系伤害，刷新 Ignite DoT（若未处于 fire_immune）。 */
public final class FireHitEvents {
  private FireHitEvents() {}

  @SubscribeEvent
  public static void onIncomingDamage(LivingIncomingDamageEvent event) {
    var source = event.getSource();
    var target = event.getEntity();
    if (source == null || target == null) return;
    if (!source.is(DamageTypeTags.IS_FIRE)) return;
    if (ReactionTagOps.has(target, ReactionTagKeys.FIRE_IMMUNE)) return;
    var attacker =
        source.getEntity() instanceof net.minecraft.world.entity.LivingEntity le ? le : null;
    ReactionRegistry.scheduleOrRefreshIgnite(attacker, target);
  }
}
