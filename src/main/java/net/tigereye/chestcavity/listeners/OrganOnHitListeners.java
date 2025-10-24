package net.tigereye.chestcavity.listeners;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.registration.CCOrganScores;

public class OrganOnHitListeners {

  public static void callMethods(
      LivingEntity attacker, LivingEntity target, ChestCavityInstance cc) {
    TickLaunching(attacker, target, cc);
  }

  private static void TickLaunching(
      LivingEntity attacker, LivingEntity target, ChestCavityInstance cc) {
    float launchingDiff =
        cc.getOrganScore(CCOrganScores.LAUNCHING)
            - cc.getChestCavityType().getDefaultOrganScore(CCOrganScores.LAUNCHING);
    if (launchingDiff != 0 && attacker.closerThan(target, 4)) {
      double KBRes = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
      target.push(
          0, Math.max(0, ChestCavity.config.LAUNCHING_POWER * launchingDiff * (1 - KBRes)), 0);
    }
  }
}
