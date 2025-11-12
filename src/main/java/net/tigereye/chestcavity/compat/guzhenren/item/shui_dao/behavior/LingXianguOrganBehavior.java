package net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.fx.ShuiFx;
import net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.tuning.ShuiTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper.ConsumptionResult;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;

/**
 * Behaviour for 灵涎蛊 (Ling Xian Gu).
 *
 * <p>Healing cadence: - Players: attempts a gentle mend every 30s when injured, consuming 30 base
 * zhenyuan per organ. - Emergency response (\"应激分泌\"): when health falls below 30% the organ
 * attempts a stronger burst costing 60 base zhenyuan per organ and granting Weakness I for 5s. -
 * Non-player entities do not spend resources but operate on doubled cooldowns and receive Weakness
 * III on emergency discharges to reflect the harsher backlash.
 *
 * <p>Visual/audio cues favour cool aqua droplets with a soft mist. Player activations glow brighter
 * and pulse more frequently than the muted non-player variant.
 */
public enum LingXianguOrganBehavior implements OrganSlowTickListener {
  INSTANCE;

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation SHUI_DAO_INCREASE_EFFECT =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/shui_dao_increase_effect");
  private static final ResourceLocation XUE_DAO_INCREASE_EFFECT =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/xue_dao_increase_effect");

  private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

  private static final String STATE_KEY = "LingXiangu";
  private static final String NORMAL_COOLDOWN_KEY = "NormalCooldown";
  private static final String STRESS_COOLDOWN_KEY = "StressCooldown";
  private static final String NORMAL_READY_AT_KEY = "NormalReadyAt";
  private static final String STRESS_READY_AT_KEY = "StressReadyAt";

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (entity == null || entity.level().isClientSide()) {
      return;
    }
    if (!entity.isAlive() || organ == null || organ.isEmpty()) {
      return;
    }

    Player player = entity instanceof Player ? (Player) entity : null;
    int normalInterval = getNormalIntervalSeconds(player);
    int stressInterval = getStressIntervalSeconds(player);

    OrganState state = OrganState.of(organ, STATE_KEY);
    MultiCooldown cooldown =
        MultiCooldown.builder(state)
            .withSync(cc, organ)
            .withIntClamp(value -> Math.max(0, value), 0)
            .build();

    // Switch to readyAt + onReady chain for both tracks
    scheduleBaselineIfNeeded(entity, cc, organ, cooldown, normalInterval);
    scheduleStressIfNeeded(entity, cc, organ, cooldown, stressInterval);
  }

  /** Ensures linkage channels exist for downstream consumers. */
  public void ensureAttached(ChestCavityInstance cc) {
    if (cc == null) {
      return;
    }
    ActiveLinkageContext context = LinkageManager.getContext(cc);
    LinkageChannel shuiDao = context.getOrCreateChannel(SHUI_DAO_INCREASE_EFFECT);
    shuiDao.addPolicy(NON_NEGATIVE);
    LinkageChannel xueDao = context.getOrCreateChannel(XUE_DAO_INCREASE_EFFECT);
    xueDao.addPolicy(NON_NEGATIVE);
  }

  private static void scheduleBaselineIfNeeded(
      LivingEntity entity,
      ChestCavityInstance cc,
      ItemStack organ,
      MultiCooldown cooldown,
      int intervalSeconds) {
    if (!(entity.level() instanceof ServerLevel server)) return;
    long now = server.getGameTime();
    MultiCooldown.Entry ready = cooldown.entry(NORMAL_READY_AT_KEY);
    if (ready.getReadyTick() <= now) {
      // randomize first trigger within interval
      int offset = entity.getRandom().nextInt(intervalSeconds + 1);
      ready.setReadyAt(now + offset);
    }
    ready.onReady(
        server,
        now,
        () -> {
          try {
            Player player = entity instanceof Player ? (Player) entity : null;
            if (shouldTriggerBaseline(entity)) {
              if (attemptBaselineHeal(entity, organ, player)) {
                // success: re-arm next tick
                long nextAt = server.getGameTime() + intervalSeconds;
                MultiCooldown.Entry e = cooldown.entry(NORMAL_READY_AT_KEY);
                e.setReadyAt(nextAt);
                e.onReady(server, server.getGameTime(), () -> {});
                return;
              }
            }
            // if not triggered due to guard, retry shortly (1s) to check again
            MultiCooldown.Entry e = cooldown.entry(NORMAL_READY_AT_KEY);
            e.setReadyAt(server.getGameTime() + 20);
            e.onReady(server, server.getGameTime(), () -> {});
          } catch (Throwable ignored) {
          }
        });
  }

  private static void scheduleStressIfNeeded(
      LivingEntity entity,
      ChestCavityInstance cc,
      ItemStack organ,
      MultiCooldown cooldown,
      int intervalSeconds) {
    if (!(entity.level() instanceof ServerLevel server)) return;
    long now = server.getGameTime();
    MultiCooldown.Entry ready = cooldown.entry(STRESS_READY_AT_KEY);
    if (ready.getReadyTick() <= now) {
      int offset = entity.getRandom().nextInt(intervalSeconds + 1);
      ready.setReadyAt(now + offset);
    }
    ready.onReady(
        server,
        now,
        () -> {
          try {
            Player player = entity instanceof Player ? (Player) entity : null;
            if (shouldTriggerStress(entity)) {
              if (attemptStressResponse(entity, organ, player)) {
                long nextAt = server.getGameTime() + intervalSeconds;
                MultiCooldown.Entry e = cooldown.entry(STRESS_READY_AT_KEY);
                e.setReadyAt(nextAt);
                e.onReady(server, server.getGameTime(), () -> {});
                return;
              }
            }
            // Retry check in 1s until guard satisfied
            MultiCooldown.Entry e = cooldown.entry(STRESS_READY_AT_KEY);
            e.setReadyAt(server.getGameTime() + 20);
            e.onReady(server, server.getGameTime(), () -> {});
          } catch (Throwable ignored) {
          }
        });
  }

  private static boolean attemptBaselineHeal(LivingEntity entity, ItemStack organ, Player player) {
    int stackCount = Math.max(1, organ.getCount());
    float healAmount = ShuiTuning.LING_XIAN_BASE_NORMAL_HEAL * stackCount;
    if (!canHeal(entity, healAmount)) {
      return false;
    }
    ConsumptionResult payment = null;
    if (player != null) {
      double cost = ShuiTuning.LING_XIAN_BASE_NORMAL_ZHENYUAN_COST * stackCount;
      payment = ResourceOps.consumeStrict(player, cost, 0.0);
      if (!payment.succeeded()) {
        return false;
      }
    }
    float healed = applyHealing(entity, healAmount);
    if (healed <= 0.0f) {
      if (player != null && payment != null) {
        ResourceOps.refund(player, payment);
      }
      return false;
    }
    boolean isPlayer = player != null;
    ShuiFx.playHealingFx(entity, stackCount, isPlayer, false);
    ReactionTagOps.add(entity, ReactionTagKeys.WATER_VEIL, 100);
    return true;
  }

  private static boolean attemptStressResponse(
      LivingEntity entity, ItemStack organ, Player player) {
    int stackCount = Math.max(1, organ.getCount());
    float healAmount = ShuiTuning.LING_XIAN_BASE_STRESS_HEAL * stackCount;
    if (!canHeal(entity, healAmount)) {
      return false;
    }
    ConsumptionResult payment = null;
    if (player != null) {
      double cost = ShuiTuning.LING_XIAN_BASE_STRESS_ZHENYUAN_COST * stackCount;
      payment = ResourceOps.consumeStrict(player, cost, 0.0);
      if (!payment.succeeded()) {
        return false;
      }
    }
    float healed = applyHealing(entity, healAmount);
    if (healed <= 0.0f) {
      if (player != null && payment != null) {
        ResourceOps.refund(player, payment);
      }
      return false;
    }
    boolean isPlayer = player != null;
    ShuiFx.playHealingFx(entity, stackCount, isPlayer, true);
    applyWeakness(entity, isPlayer);
    ReactionTagOps.add(entity, ReactionTagKeys.WATER_VEIL, 140);
    return true;
  }

  private static boolean shouldTriggerBaseline(LivingEntity entity) {
    if (entity == null) {
      return false;
    }
    return entity.getHealth() < entity.getMaxHealth();
  }

  private static boolean shouldTriggerStress(LivingEntity entity) {
    if (entity == null) {
      return false;
    }
    float max = entity.getMaxHealth();
    if (max <= 0.0f) {
      return false;
    }
    float ratio = entity.getHealth() / max;
    return ratio < ShuiTuning.LING_XIAN_STRESS_THRESHOLD_RATIO;
  }

  private static float applyHealing(LivingEntity entity, float amount) {
    float before = entity.getHealth();
    entity.heal(amount);
    return entity.getHealth() - before;
  }

  private static boolean canHeal(LivingEntity entity, float amount) {
    if (entity == null || amount <= 0.0f) {
      return false;
    }
    return entity.getHealth() < entity.getMaxHealth();
  }

  private static void applyWeakness(LivingEntity entity, boolean isPlayer) {
    int amplifier =
        isPlayer
            ? ShuiTuning.LING_XIAN_PLAYER_STRESS_AMPLIFIER
            : ShuiTuning.LING_XIAN_NON_PLAYER_STRESS_AMPLIFIER;
    MobEffectInstance effect =
        new MobEffectInstance(
            MobEffects.WEAKNESS,
            ShuiTuning.LING_XIAN_WEAKNESS_DURATION_TICKS,
            amplifier,
            false,
            true,
            true);
    entity.addEffect(effect);
  }

  private static int getNormalIntervalSeconds(Player player) {
    return player != null
        ? ShuiTuning.LING_XIAN_PLAYER_INTERVAL_SECONDS
        : ShuiTuning.LING_XIAN_NON_PLAYER_INTERVAL_SECONDS;
  }

  private static int getStressIntervalSeconds(Player player) {
    return player != null
        ? ShuiTuning.LING_XIAN_PLAYER_INTERVAL_SECONDS
        : ShuiTuning.LING_XIAN_NON_PLAYER_INTERVAL_SECONDS;
  }
}
