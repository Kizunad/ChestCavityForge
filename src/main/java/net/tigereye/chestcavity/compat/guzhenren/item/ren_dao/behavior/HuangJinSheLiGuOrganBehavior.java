package net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;

/**
 * 黄金舍利蛊（四转）： - 被动：持续性伤害 -25%（火焰/凋零/中毒类/流血等），按 OnIncomingDamage 拦截 - 主动（60s冷却）：金刚定身：6s 免击退 +
 * 抗性III，自身缓慢III；并对 8 格内敌对目标施加缓慢IV 6s
 */
public enum HuangJinSheLiGuOrganBehavior
    implements OrganSlowTickListener, OrganIncomingDamageListener {
  INSTANCE;

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "huang_jin_she_li_gu");
  public static final ResourceLocation ABILITY_ID = ORGAN_ID;

  private static final String STATE_ROOT = "HuangJinSheLiGu";
  private static final String KEY_ACTIVE_COOLDOWN_UNTIL = "ActiveCooldownUntil";

  private static final int ACTIVE_DURATION_TICKS = 6 * 20;
  private static final int ACTIVE_COOLDOWN_TICKS = 60 * 20;
  private static final double DOT_REDUCTION_RATIO = 0.25; // -25%
  private static final ResourceLocation BLEED_EFFECT_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "lliuxue");

  private static final ResourceLocation KNOCKBACK_RESIST_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/no_knockback_huang_jin");

  static {
    OrganActivationListeners.register(ABILITY_ID, HuangJinSheLiGuOrganBehavior::activateAbility);
  }

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    // 无持续tick需求
  }

  public static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    if (entity == null || cc == null || entity.level().isClientSide()) return;
    ItemStack organ = findPrimaryOrgan(cc);
    if (organ.isEmpty()) return;
    ServerLevel server = entity.level() instanceof ServerLevel s ? s : null;
    if (server == null) return;
    MultiCooldown cd = createCooldown(cc, organ);
    long now = server.getGameTime();
    long cdUntil = Math.max(0L, cd.entry(KEY_ACTIVE_COOLDOWN_UNTIL).getReadyTick());
    if (now < cdUntil) return;

    // 自身：6s 抗性III + 缓慢III + 免击退
    entity.addEffect(
        new MobEffectInstance(
            MobEffects.DAMAGE_RESISTANCE, ACTIVE_DURATION_TICKS, 2, false, true, true));
    entity.addEffect(
        new MobEffectInstance(
            MobEffects.MOVEMENT_SLOWDOWN, ACTIVE_DURATION_TICKS, 2, false, true, true));
    grantNoKnockback(entity, ACTIVE_DURATION_TICKS);
    ReactionTagOps.add(entity, ReactionTagKeys.HUMAN_AEGIS, ACTIVE_DURATION_TICKS);

    // 敌对：半径8内缓慢IV 6s
    AABB area = entity.getBoundingBox().inflate(8.0);
    for (LivingEntity target :
        entity
            .level()
            .getEntitiesOfClass(
                LivingEntity.class, area, t -> t != null && t.isAlive() && t != entity)) {
      if (isHostileTo(entity, target)) {
        target.addEffect(
            new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, ACTIVE_DURATION_TICKS, 3, false, true, true));
      }
    }

    long readyAt = now + ACTIVE_COOLDOWN_TICKS;
    cd.entry(KEY_ACTIVE_COOLDOWN_UNTIL).setReadyAt(readyAt);
    if (entity instanceof net.minecraft.server.level.ServerPlayer sp) {
      ActiveSkillRegistry.scheduleReadyToast(sp, ABILITY_ID, readyAt, now);
    }
  }

  @Override
  public float onIncomingDamage(
      DamageSource source,
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (victim == null
        || victim.level().isClientSide()
        || cc == null
        || organ == null
        || organ.isEmpty()) {
      return damage;
    }
    if (!matchesOrgan(organ) || damage <= 0.0f) {
      return damage;
    }
    if (isDotLike(source, victim)) {
      return Math.max(0.0f, damage * (float) (1.0 - DOT_REDUCTION_RATIO));
    }
    return damage;
  }

  private static boolean isDotLike(DamageSource source, LivingEntity victim) {
    if (source == null) return false;
    // 粗略判定：实体处于着火、身上有凋零/中毒/流血等持续性效果
    if (victim != null) {
      if (victim.isOnFire()) return true;
      if (victim.hasEffect(MobEffects.WITHER) || victim.hasEffect(MobEffects.POISON)) return true;
      try {
        var bleedHolder = BuiltInRegistries.MOB_EFFECT.getHolder(BLEED_EFFECT_ID);
        if (bleedHolder.isPresent()) {
          Holder.Reference<net.minecraft.world.effect.MobEffect> ref = bleedHolder.get();
          if (victim.hasEffect(ref)) return true;
        }
      } catch (Throwable ignored) {
      }
    }
    // 兜底：无施害者/无直接施害者的 generic 环境伤害，视作DoT
    return source.getEntity() == null && source.getDirectEntity() == null;
  }

  private static boolean isHostileTo(LivingEntity self, LivingEntity other) {
    if (other == null || !other.isAlive()) return false;
    if (other == self) return false;
    if (other.isAlliedTo(self)) return false;
    if (other instanceof Mob mob && mob.getTarget() != null) return true;
    // 简单近似：非玩家、非同阵营视为潜在敌对
    return true;
  }

  private static void grantNoKnockback(LivingEntity entity, int durationTicks) {
    var attr =
        entity.getAttribute(
            net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
    if (attr == null) return;
    if (attr.hasModifier(KNOCKBACK_RESIST_ID)) {
      attr.removeModifier(KNOCKBACK_RESIST_ID);
    }
    var mod =
        new net.minecraft.world.entity.ai.attributes.AttributeModifier(
            KNOCKBACK_RESIST_ID,
            1.0,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
    attr.addTransientModifier(mod);
    if (entity.level() instanceof ServerLevel server) {
      net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps.schedule(
          server,
          () -> {
            var a =
                entity.getAttribute(
                    net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
            if (a != null && a.hasModifier(KNOCKBACK_RESIST_ID)) {
              a.removeModifier(KNOCKBACK_RESIST_ID);
            }
          },
          Math.max(1, durationTicks));
    }
  }

  private static boolean matchesOrgan(ItemStack stack) {
    if (stack == null || stack.isEmpty()) return false;
    ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
    return ORGAN_ID.equals(id);
  }

  private static ItemStack findPrimaryOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) return ItemStack.EMPTY;
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack candidate = cc.inventory.getItem(i);
      if (matchesOrgan(candidate)) {
        return candidate;
      }
    }
    return ItemStack.EMPTY;
  }

  private static MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ) {
    MultiCooldown.Builder b =
        MultiCooldown.builder(OrganState.of(organ, STATE_ROOT))
            .withLongClamp(value -> Math.max(0L, value), 0L);
    if (cc != null) {
      b.withSync(cc, organ);
    } else {
      b.withOrgan(organ);
    }
    return b.build();
  }
}
