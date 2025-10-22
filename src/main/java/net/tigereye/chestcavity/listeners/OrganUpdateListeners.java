package net.tigereye.chestcavity.listeners;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.registration.CCOrganScores;
import net.tigereye.chestcavity.registration.CCStatusEffects;

public class OrganUpdateListeners {

  private static final ResourceLocation APPENDIX_ID = ChestCavity.id("modifiers/appendix_luck");
  private static final ResourceLocation HEART_ID = ChestCavity.id("modifiers/heart_max_hp");
  private static final ResourceLocation MUSCLE_STRENGTH_ID =
      ChestCavity.id("modifiers/muscle_attack_damage");
  private static final ResourceLocation MUSCLE_SPEED_ID =
      ChestCavity.id("modifiers/muscle_movement_speed");
  private static final ResourceLocation SPINE_ATTACK_SPEED_ID =
      ChestCavity.id("modifiers/spine_attack_speed");
  private static final ResourceLocation SPINE_MOVEMENT_ID =
      ChestCavity.id("modifiers/spine_movement_speed");
  private static final ResourceLocation KNOCKBACK_RESISTANCE_ID =
      ChestCavity.id("modifiers/knockback_resistance");

  public static void callMethods(LivingEntity entity, ChestCavityInstance cc) {
    UpdateAppendix(entity, cc);
    UpdateHeart(entity, cc);
    UpdateStrength(entity, cc);
    UpdateSpeed(entity, cc);
    UpdateSpine(entity, cc);
    UpdateKnockbackResistance(entity, cc);
    UpdateIncompatibility(entity, cc);
    OrganScoreEffects.applyAll(entity, cc);
  }

  public static void UpdateAppendix(LivingEntity entity, ChestCavityInstance cc) {
    // Update Max Health Modifier
    if (cc.getOldOrganScore(CCOrganScores.LUCK) != cc.getOrganScore(CCOrganScores.LUCK)) {
      AttributeInstance att = entity.getAttribute(Attributes.LUCK);
      if (att != null) {
        double amount =
            (cc.getOrganScore(CCOrganScores.LUCK)
                    - cc.getChestCavityType().getDefaultOrganScore(CCOrganScores.LUCK))
                * ChestCavity.config.APPENDIX_LUCK;
        AttributeModifier mod =
            new AttributeModifier(APPENDIX_ID, amount, AttributeModifier.Operation.ADD_VALUE);
        ReplaceAttributeModifier(att, mod);
      }
    }
  }

  public static void UpdateHeart(LivingEntity entity, ChestCavityInstance cc) {
    // Update Max Health Modifier
    if (cc.getOldOrganScore(CCOrganScores.HEALTH) != cc.getOrganScore(CCOrganScores.HEALTH)) {
      AttributeInstance att = entity.getAttribute(Attributes.MAX_HEALTH);
      if (att != null) {
        double amount =
            (cc.getOrganScore(CCOrganScores.HEALTH)
                    - cc.getChestCavityType().getDefaultOrganScore(CCOrganScores.HEALTH))
                * ChestCavity.config.HEART_HP;
        AttributeModifier mod =
            new AttributeModifier(HEART_ID, amount, AttributeModifier.Operation.ADD_VALUE);
        ReplaceAttributeModifier(att, mod);
      }
    }
  }

  public static void UpdateStrength(LivingEntity entity, ChestCavityInstance cc) {
    if (cc.getOldOrganScore(CCOrganScores.STRENGTH) != cc.getOrganScore(CCOrganScores.STRENGTH)) {
      // Update Damage Modifier and Speed Modifier
      AttributeInstance att = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      if (att != null) {
        double amount =
            (cc.getOrganScore(CCOrganScores.STRENGTH)
                    - cc.getChestCavityType().getDefaultOrganScore(CCOrganScores.STRENGTH))
                * ChestCavity.config.MUSCLE_STRENGTH
                / 8;
        AttributeModifier mod =
            new AttributeModifier(
                MUSCLE_STRENGTH_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        ReplaceAttributeModifier(att, mod);
      }
    }
  }

  public static void UpdateSpeed(LivingEntity entity, ChestCavityInstance cc) {
    if (cc.getOldOrganScore(CCOrganScores.SPEED) != cc.getOrganScore(CCOrganScores.SPEED)) {
      // Update Damage Modifier and Speed Modifier
      AttributeInstance att = entity.getAttribute(Attributes.MOVEMENT_SPEED);
      if (att != null) {
        double amount =
            (cc.getOrganScore(CCOrganScores.SPEED)
                    - cc.getChestCavityType().getDefaultOrganScore(CCOrganScores.SPEED))
                * ChestCavity.config.MUSCLE_SPEED
                / 8;
        AttributeModifier mod =
            new AttributeModifier(
                MUSCLE_SPEED_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        ReplaceAttributeModifier(att, mod);
      }
    }
  }

  public static void UpdateSpine(LivingEntity entity, ChestCavityInstance cc) {
    if (cc.getOldOrganScore(CCOrganScores.NERVES) != cc.getOrganScore(CCOrganScores.NERVES)
        && cc.getChestCavityType().getDefaultOrganScore(CCOrganScores.NERVES) != 0) {
      // Update Speed Modifier. No spine? NO MOVING.
      AttributeInstance att = entity.getAttribute(Attributes.MOVEMENT_SPEED);
      if (att != null) {
        double amount = cc.getOrganScore(CCOrganScores.NERVES) > 0 ? 0D : -1D;
        AttributeModifier mod =
            new AttributeModifier(
                SPINE_MOVEMENT_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        ReplaceAttributeModifier(att, mod);
      }
      // Update Attack Speed Modifier.
      att = entity.getAttribute(Attributes.ATTACK_SPEED);
      if (att != null) {
        double amount =
            (cc.getOrganScore(CCOrganScores.NERVES)
                    - cc.getChestCavityType().getDefaultOrganScore(CCOrganScores.NERVES))
                * ChestCavity.config.NERVES_HASTE;
        AttributeModifier mod =
            new AttributeModifier(
                SPINE_ATTACK_SPEED_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        ReplaceAttributeModifier(att, mod);
      }
    }
  }

  public static void UpdateKnockbackResistance(LivingEntity entity, ChestCavityInstance cc) {
    if (cc.getOldOrganScore(CCOrganScores.KNOCKBACK_RESISTANT)
        != cc.getOrganScore(CCOrganScores.KNOCKBACK_RESISTANT)) {
      // Update Knockback Res Modifier
      AttributeInstance att = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
      if (att != null) {
        double amount =
            (cc.getOrganScore(CCOrganScores.KNOCKBACK_RESISTANT)
                    - cc.getChestCavityType()
                        .getDefaultOrganScore(CCOrganScores.KNOCKBACK_RESISTANT))
                * 0.1D;
        AttributeModifier mod =
            new AttributeModifier(
                KNOCKBACK_RESISTANCE_ID, amount, AttributeModifier.Operation.ADD_VALUE);
        ReplaceAttributeModifier(att, mod);
      }
    }
  }

  public static void UpdateIncompatibility(LivingEntity entity, ChestCavityInstance cc) {
    if (cc.getOldOrganScore(CCOrganScores.INCOMPATIBILITY)
        != cc.getOrganScore(CCOrganScores.INCOMPATIBILITY)) {
      try {
        entity.removeEffect(CCStatusEffects.ORGAN_REJECTION);
      } catch (Exception ignore) {
      }
    }
  }

  private static void ReplaceAttributeModifier(AttributeInstance att, AttributeModifier mod) {
    att.addOrReplacePermanentModifier(mod);
  }
}
