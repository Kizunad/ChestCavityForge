package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.rarity.HunDaoSoulForm;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.rarity.HunDaoSoulRarityBonuses;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.runtime.HunDaoRuntimeContext;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.runtime.HunDaoStateMachine;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.tuning.HunDaoRuntimeTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.AttributeOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.DaoHenResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.entity.HunDaoSoulAvatarResourceState;

/** Default server-side behaviour hook for soul avatars. */
public final class HunDaoSoulAvatarDefaultHook implements HunDaoSoulAvatarHook {

  private static final ResourceLocation ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "hun_dao/default_avatar_hook");
  private static final ResourceLocation MOVE_MODIFIER_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "hun_dao/avatar_move_bonus");
  private static final ResourceLocation ATTACK_MODIFIER_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "hun_dao/avatar_attack_bonus");

  private static final String FIELD_HUNPO = "hunpo";
  private static final String FIELD_HUNPO_MAX = "zuida_hunpo";

  public static void register() {
    HunDaoSoulAvatarHookRegistry.register(ID, new HunDaoSoulAvatarDefaultHook());
  }

  @Override
  public void onSpawn(HunDaoSoulAvatarEntity avatar) {
    ensureSoulBeastState(avatar);
    syncAttributes(avatar);
  }

  @Override
  public void onSyncHealth(HunDaoSoulAvatarEntity avatar) {
    if (avatar.level().isClientSide) {
      return;
    }
    HunDaoSoulAvatarResourceState state = avatar.getResourceState();
    double maxHunpo = Math.max(1.0D, state.getMaxHunpo());
    double currentHunpo = Math.max(0.0D, state.getHunpo());

    AttributeInstance maxHealthAttr = avatar.getAttribute(Attributes.MAX_HEALTH);
    if (maxHealthAttr != null) {
      if (Math.abs(maxHealthAttr.getBaseValue() - maxHunpo) > 1e-3) {
        maxHealthAttr.setBaseValue(maxHunpo);
      }
    }

    if (Math.abs(avatar.getHealth() - currentHunpo) > 1e-3) {
      avatar.setHealth((float) currentHunpo);
    }
  }

  @Override
  public double modifyDamage(HunDaoSoulAvatarEntity avatar, double damage) {
    if (!(damage > 0.0D)) {
      return 0.0D;
    }
    double scar =
        ResourceOps.openHandle(avatar)
            .map(handle -> DaoHenResourceOps.get(handle, "daohen_hundao"))
            .orElse(0.0D);
    double ratio =
        Math.min(
            1.0D,
            Math.max(
                0.0D, scar / HunDaoRuntimeTuning.SoulBeastDefense.SCAR_SOFTCAP));
    double reduction = ratio * HunDaoRuntimeTuning.SoulBeastDefense.MAX_REDUCTION;
    double multiplier = 1.0D - reduction;
    if (multiplier < 0.0D) {
      multiplier = 0.0D;
    }
    return damage * multiplier;
  }

  @Override
  public void onServerTick(HunDaoSoulAvatarEntity avatar) {
    tickLifetime(avatar);
    long gameTime = avatar.level().getGameTime();
    if (gameTime % 20L == 0L) {
      LivingEntity target = resolveOwnerOrSelf(avatar);
      HunDaoSoulRarityBonuses bonuses = resolveBonuses(target);
      syncAttributes(avatar, bonuses);
      applyHunpoRegen(avatar, bonuses);
      clampHunpoCapacity(avatar, bonuses);
    }
  }

  private void tickLifetime(HunDaoSoulAvatarEntity avatar) {
    int remaining = avatar.getLifetimeTicks();
    if (remaining < 0) {
      return;
    }
    if (remaining == 0) {
      avatar.remove(Entity.RemovalReason.KILLED);
      return;
    }
    avatar.setLifetimeTicks(remaining - 1);
  }

  private void syncAttributes(HunDaoSoulAvatarEntity avatar) {
    syncAttributes(avatar, resolveBonuses(resolveOwnerOrSelf(avatar)));
  }

  private void syncAttributes(HunDaoSoulAvatarEntity avatar, HunDaoSoulRarityBonuses bonuses) {
    applyMovementBonus(avatar, bonuses);
    applyAttackBonus(avatar, bonuses);
  }

  private void ensureSoulBeastState(HunDaoSoulAvatarEntity avatar) {
    HunDaoRuntimeContext context = HunDaoRuntimeContext.get(avatar);
    HunDaoStateMachine stateMachine = context.getStateMachine();
    if (stateMachine.isSoulBeastMode()) {
      return;
    }
    if (!stateMachine.makePermanent()) {
      stateMachine.activateSoulBeast();
    }
    stateMachine.syncToClient();
  }

  private void applyMovementBonus(HunDaoSoulAvatarEntity avatar, HunDaoSoulRarityBonuses bonuses) {
    AttributeInstance instance = avatar.getAttribute(Attributes.MOVEMENT_SPEED);
    if (instance == null) {
      return;
    }
    double multiplier = bonuses.movementSpeedMultiplier();
    if (multiplier <= 1.0D + 1.0E-4D) {
      AttributeOps.removeById(instance, MOVE_MODIFIER_ID);
      return;
    }
    AttributeModifier modifier =
        new AttributeModifier(
            MOVE_MODIFIER_ID, multiplier - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    AttributeOps.replaceTransient(instance, MOVE_MODIFIER_ID, modifier);
  }

  private void applyAttackBonus(HunDaoSoulAvatarEntity avatar, HunDaoSoulRarityBonuses bonuses) {
    AttributeInstance instance = avatar.getAttribute(Attributes.ATTACK_DAMAGE);
    if (instance == null) {
      return;
    }
    double multiplier = bonuses.attackMultiplier() * avatar.getHunpoAttackMultiplier();
    if (multiplier <= 1.0D + 1.0E-4D) {
      AttributeOps.removeById(instance, ATTACK_MODIFIER_ID);
      return;
    }
    AttributeModifier modifier =
        new AttributeModifier(
            ATTACK_MODIFIER_ID,
            multiplier - 1.0D,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    AttributeOps.replaceTransient(instance, ATTACK_MODIFIER_ID, modifier);
  }

  private void applyHunpoRegen(HunDaoSoulAvatarEntity avatar, HunDaoSoulRarityBonuses bonuses) {
    double perSecond = Math.max(0.0D, bonuses.hunPoRegenPerSecond());
    if (perSecond <= 0.0D) {
      return;
    }
    ResourceOps.openHandle(avatar)
        .ifPresent(handle -> handle.adjustHunpo(perSecond / 20.0D, true));
  }

  private void clampHunpoCapacity(HunDaoSoulAvatarEntity avatar, HunDaoSoulRarityBonuses bonuses) {
    double limit = computeSoftCap(avatar, bonuses);
    if (!(limit > 0.0D)) {
      return;
    }
    ResourceOps.openHandle(avatar)
        .ifPresent(
            handle -> {
              double maxHunpo = handle.read(FIELD_HUNPO_MAX).orElse(0.0D);
              if (!(maxHunpo > limit + HunDaoRuntimeTuning.MortalShell.EPSILON)) {
                return;
              }
              double excess = maxHunpo - limit;
              double decay =
                  Math.max(
                      HunDaoRuntimeTuning.MortalShell.MIN_DECAY_PER_SECOND,
                      excess * HunDaoRuntimeTuning.MortalShell.DECAY_FRACTION);
              double target = Math.max(limit, maxHunpo - decay);
              double newMax = handle.writeDouble(FIELD_HUNPO_MAX, target).orElse(target);
              double current = handle.read(FIELD_HUNPO).orElse(0.0D);
              if (current > newMax + HunDaoRuntimeTuning.MortalShell.EPSILON) {
                handle.writeDouble(FIELD_HUNPO, newMax);
              }
            });
  }

  private double computeSoftCap(HunDaoSoulAvatarEntity avatar, HunDaoSoulRarityBonuses bonuses) {
    double maxHealth = Math.max(0.0D, avatar.getAttributeValue(Attributes.MAX_HEALTH));
    double baseCap = maxHealth * HunDaoRuntimeTuning.MortalShell.HUNPO_PER_HP;
    double bonus = Math.max(0.0D, bonuses.hunPoMaxBonus());
    double combined = baseCap + bonus;
    return Math.min(combined, HunDaoRuntimeTuning.MortalShell.ABSOLUTE_MAX_HUNPO);
  }

  private HunDaoSoulRarityBonuses resolveBonuses(LivingEntity entity) {
    HunDaoRuntimeContext context = HunDaoRuntimeContext.get(entity);
    return context.getRarityOps().getBonuses(entity, HunDaoSoulForm.HUMAN);
  }

  private LivingEntity resolveOwnerOrSelf(HunDaoSoulAvatarEntity avatar) {
    LivingEntity owner = avatar.getOwner();
    return owner == null ? avatar : owner;
  }
}
