package net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.behavior;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper.ConsumptionResult;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import org.slf4j.Logger;

/** Behaviour for 竭泽蛊 (Jie Ze Gu). */
public final class JiezeguOrganBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener, OrganOnHitListener {

  public static final JiezeguOrganBehavior INSTANCE = new JiezeguOrganBehavior();

  private static final Logger LOGGER = LogUtils.getLogger();

  private JiezeguOrganBehavior() {}

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jiezegu");
  private static final ResourceLocation SHUI_DAO_INCREASE_EFFECT =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/shui_dao_increase_effect");
  private static final ResourceLocation SUIJIA_EFFECT_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "suijia");

  private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

  private static final String STATE_ROOT = "Jiezegu";
  private static final String ACTIVE_KEY = "Active";

  private static final double BONUS_TRIGGER_CHANCE = 0.4;
  private static final double BONUS_DAMAGE_RATIO = 0.238;
  private static final double FLOW_BREAK_TRIGGER_CHANCE = 0.08;
  private static final int FLOW_BREAK_DURATION_TICKS =
      BehaviorConfigAccess.getInt(JiezeguOrganBehavior.class, "FLOW_BREAK_DURATION_TICKS", 4 * 20);

  private static final double BASE_ZHENYUAN_COST_PER_SECOND = 500.0;
  private static final float BASE_HEALTH_COST_PER_SECOND =
      BehaviorConfigAccess.getFloat(
          JiezeguOrganBehavior.class, "BASE_HEALTH_COST_PER_SECOND", 25.0f);
  private static final float MIN_HEALTH_RESERVE =
      BehaviorConfigAccess.getFloat(JiezeguOrganBehavior.class, "MIN_HEALTH_RESERVE", 1.0f);

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (entity == null || entity.level().isClientSide()) {
      return;
    }
    if (organ == null || organ.isEmpty()) {
      return;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return;
    }

    OrganState state = organState(organ, STATE_ROOT);
    boolean previous = state.getBoolean(ACTIVE_KEY, false);
    boolean active = upkeepZhenyuan(entity, organ);
    if (previous != active) {
      OrganStateOps.setBooleanSync(cc, organ, STATE_ROOT, ACTIVE_KEY, active, false);
    }
  }

  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (attacker == null || target == null || attacker.level().isClientSide()) {
      return damage;
    }
    if (organ == null || organ.isEmpty()) {
      return damage;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return damage;
    }
    if (damage <= 0.0f) {
      return damage;
    }

    OrganState state = organState(organ, STATE_ROOT);
    if (!state.getBoolean(ACTIVE_KEY, false)) {
      return damage;
    }

    RandomSource random = attacker.getRandom();
    if (random.nextDouble() >= BONUS_TRIGGER_CHANCE) {
      return damage;
    }

    double increase = resolveWaterIncrease(cc);
    double efficiency = Math.max(0.0, 1.0 + increase);
    float bonus = (float) (damage * BONUS_DAMAGE_RATIO * efficiency);

    // 大于 0.0 才会触发
    if (bonus > 0.0f) {
      playRefluxSound(attacker.level(), attacker, target);
      if (random.nextDouble() < FLOW_BREAK_TRIGGER_CHANCE) {
        if (upkeepHealth(attacker)) {
          applyFlowBreak(target, increase);
        }
      }
      return damage + bonus;
    }

    playRefluxSound(attacker.level(), attacker, target);
    if (random.nextDouble() < FLOW_BREAK_TRIGGER_CHANCE) {
      if (upkeepHealth(attacker)) {
        applyFlowBreak(target, increase);
      }
    }
    return damage;
  }

  /** Ensures linkage channels exist for downstream consumers. */
  public void ensureAttached(ChestCavityInstance cc) {
    if (cc == null) {
      return;
    }
    ActiveLinkageContext context = LinkageManager.getContext(cc);
    if (context == null) {
      return;
    }
    context.getOrCreateChannel(SHUI_DAO_INCREASE_EFFECT).addPolicy(NON_NEGATIVE);
  }

  private boolean upkeepZhenyuan(LivingEntity entity, ItemStack organ) {
    if (entity == null || organ == null || organ.isEmpty()) return false;
    int stacks = Math.max(1, organ.getCount());
    double zhenyuanCost = BASE_ZHENYUAN_COST_PER_SECOND * stacks;
    ConsumptionResult payment;
    if (entity instanceof Player player) {
      payment = ResourceOps.consumeStrict(player, zhenyuanCost, 0.0);
    } else {
      payment = ResourceOps.consumeWithFallback(entity, zhenyuanCost, 0.0);
    }
    if (!payment.succeeded()) {
      logUpkeepFailure(entity, payment);
      return false;
    }
    return true;
  }

  private boolean upkeepHealth(LivingEntity entity) {
    if (entity == null) return false;
    // Health cost uses 1 stack baseline per trigger of Flow Break; scale by 1 for now.
    float healthCost = BASE_HEALTH_COST_PER_SECOND;
    boolean drained =
        ResourceOps.drainHealth(
            entity, healthCost, MIN_HEALTH_RESERVE, entity.damageSources().generic());
    if (!drained) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "[compat/guzhenren][shui_dao][jiezegu] upkeepHealth failed for {}",
            entity.getName().getString());
      }
      return false;
    }
    return true;
  }

  private static double resolveWaterIncrease(ChestCavityInstance cc) {
    if (cc == null) {
      return 0.0;
    }
    ActiveLinkageContext context = LinkageManager.getContext(cc);
    if (context == null) {
      return 0.0;
    }
    Optional<LinkageChannel> channel = context.lookupChannel(SHUI_DAO_INCREASE_EFFECT);
    return channel.map(LinkageChannel::get).orElse(0.0);
  }

  private static void applyFlowBreak(LivingEntity target, double increase) {
    if (target == null || target.level().isClientSide()) {
      return;
    }
    Optional<Holder.Reference<MobEffect>> effectHolder =
        BuiltInRegistries.MOB_EFFECT.getHolder(SUIJIA_EFFECT_ID);
    effectHolder.ifPresent(
        effect -> {
          int amplifier = Mth.clamp((int) Math.round(Math.max(0.0, increase)), 0, Short.MAX_VALUE);
          target.addEffect(
              new MobEffectInstance(
                  effect, FLOW_BREAK_DURATION_TICKS, amplifier, false, true, true));
          ReactionTagOps.add(target, ReactionTagKeys.WATER_VEIL, FLOW_BREAK_DURATION_TICKS);
        });
  }

  private static void playRefluxSound(Level level, LivingEntity attacker, LivingEntity target) {
    if (level == null || level.isClientSide()) {
      return;
    }
    double x = target == null ? attacker.getX() : target.getX();
    double y = target == null ? attacker.getY() : target.getY();
    double z = target == null ? attacker.getZ() : target.getZ();
    SoundSource source = attacker instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
    level.playSound(
        null,
        x,
        y,
        z,
        SoundEvents.GENERIC_SPLASH,
        source,
        0.65f,
        0.9f + level.getRandom().nextFloat() * 0.2f);
  }

  private void logUpkeepFailure(LivingEntity entity, ConsumptionResult payment) {
    if (entity == null || payment == null) {
      return;
    }
    LOGGER.info(
        "[compat/guzhenren][shui_dao][jiezegu] upkeep failed for {} (mode={}, reason={})",
        entity.getName().getString(),
        payment.mode(),
        payment.failureReason());
  }
}
