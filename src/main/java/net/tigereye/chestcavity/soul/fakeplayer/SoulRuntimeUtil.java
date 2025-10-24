package net.tigereye.chestcavity.soul.fakeplayer;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.gameevent.GameEvent;

/** Utility helpers for default soul runtime behaviour. */
public final class SoulRuntimeUtil {

  private SoulRuntimeUtil() {}

  public static void ensureSurvivalTick(SoulPlayer player) {
    if (player.level().isClientSide()) {
      return;
    }
    long now = player.level().getGameTime();
    if (now != player.getLastFoodTick()) {
      player.getFoodData().tick(player);
      player.setLastFoodTick(now);
    }
  }

  public static boolean applyVanillaHurt(SoulPlayer player, DamageSource source, float amount) {
    if (player.level().isClientSide() || player.isRemoved()) {
      return false;
    }
    if (player.isInvulnerableTo(source)) {
      return false;
    }
    float remaining = Math.max(0.0F, amount);
    if (remaining == 0.0F) {
      return false;
    }
    float absorption = player.getAbsorptionAmount();
    if (absorption > 0.0F) {
      float absorbed = Math.min(absorption, remaining);
      player.setAbsorptionAmount(absorption - absorbed);
      remaining -= absorbed;
    }
    if (remaining <= 0.0F) {
      return true;
    }
    float currentHealth = player.getHealth();
    float newHealth = Math.max(0.0F, currentHealth - remaining);
    player.getCombatTracker().recordDamage(source, remaining);
    player.setHealth(newHealth);
    player.invulnerableTime = 20;
    player.gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
    if (newHealth <= 0.0F) {
      player.die(source);
    }
    return true;
  }
}
