package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.engine.dot.DoTEngine;
import net.tigereye.chestcavity.guscript.ability.AbilityFxDispatcher;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper.ConsumptionResult;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.registration.CCSoundEvents;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.DoTTypes;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import org.slf4j.Logger;

/** Behaviour for 火衣蛊 – provides a pulsing fire aura and an activated flame burst. */
public enum HuoYiGuOrganBehavior implements OrganSlowTickListener {
  INSTANCE;

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_gu");
  public static final ResourceLocation ABILITY_ID = ORGAN_ID; // attack ability trigger key

  private static final ResourceLocation YAN_DAO_INCREASE_CHANNEL =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/yan_dao_increase_effect");
  private static final ResourceLocation FIRE_HUO_YI_FX =
      ResourceLocation.parse("chestcavity:fire_huo_yi");

  private static final String STATE_ROOT = "HuoYiGu";
  private static final String KEY_COOLDOWN_UNTIL = "CooldownUntil";
  private static final String KEY_ACTIVE_UNTIL = "ActiveUntil";
  private static final String KEY_ACTIVE_NEXT_TICK = "ActiveNextTick";
  private static final String KEY_PASSIVE_ACTIVE = "PassiveActive";
  private static final String KEY_PASSIVE_TOGGLE_TICK = "PassiveToggleTick";
  private static final String KEY_PASSIVE_NEXT_TICK = "PassiveNextTick";

  private static final double ACTIVE_ZHENYUAN_COST = 50.0;
  private static final int ACTIVE_HUNGER_COST =
      BehaviorConfigAccess.getInt(
          HuoYiGuOrganBehavior.class, "ACTIVE_HUNGER_COST", 5); // hunger points
  private static final int ACTIVE_DURATION_TICKS =
      BehaviorConfigAccess.getInt(HuoYiGuOrganBehavior.class, "ACTIVE_DURATION_TICKS", 200); // 10s
  private static final int ACTIVE_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(
          HuoYiGuOrganBehavior.class,
          "ACTIVE_COOLDOWN_TICKS",
          220); // 11s (10s active + 1s downtime)
  private static final double ACTIVE_RADIUS = 6.0;
  private static final double ACTIVE_DAMAGE_PER_SECOND = 5.0;
  private static final double ACTIVE_SLOWNESS_BASE = 1.0;
  private static final int ACTIVE_SLOWNESS_DURATION_TICKS =
      BehaviorConfigAccess.getInt(
          HuoYiGuOrganBehavior.class, "ACTIVE_SLOWNESS_DURATION_TICKS", 200);

  private static final double PASSIVE_RADIUS = 10.0;
  private static final double PASSIVE_DAMAGE_PER_SECOND = 0.5;
  private static final int PASSIVE_ACTIVE_DURATION_TICKS =
      BehaviorConfigAccess.getInt(
          HuoYiGuOrganBehavior.class, "PASSIVE_ACTIVE_DURATION_TICKS", 100); // 5s on
  private static final int PASSIVE_DOWNTIME_TICKS =
      BehaviorConfigAccess.getInt(
          HuoYiGuOrganBehavior.class, "PASSIVE_DOWNTIME_TICKS", 100); // 5s off

  private static final Predicate<LivingEntity> HOSTILE_TARGET =
      entity -> entity != null && entity.isAlive();
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final String LOG_PREFIX = "[compat/guzhenren][yan_dao][huo_yi_gu]";

  static {
    // Register attack ability activation following JianYing pattern
    OrganActivationListeners.register(ABILITY_ID, HuoYiGuOrganBehavior::activateAbility);
  }

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (entity == null || cc == null || organ == null || organ.isEmpty()) {
      return;
    }
    Level level = entity.level();
    if (!(level instanceof ServerLevel serverLevel) || level.isClientSide()) {
      return;
    }
    if (!matchesOrganSimple(organ, ORGAN_ID)) {
      return;
    }
    OrganState state = OrganState.of(organ, STATE_ROOT);
    MultiCooldown cooldown = createCooldown(cc, organ);
    long gameTime = level.getGameTime();
    double multiplier = resolveYanDaoMultiplier(cc);

    boolean dirty = false;
    dirty |= tickActiveAura(serverLevel, entity, cooldown, gameTime, multiplier);
    dirty |= tickPassivePulse(serverLevel, entity, state, cooldown, gameTime, multiplier);

    if (dirty) {
      NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }
  }

  private static boolean matchesOrganSimple(ItemStack stack, ResourceLocation organId) {
    if (stack == null || stack.isEmpty() || organId == null) {
      return false;
    }
    ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
    return organId.equals(id);
  }

  private boolean tickActiveAura(
      ServerLevel level,
      LivingEntity user,
      MultiCooldown cooldown,
      long gameTime,
      double multiplier) {
    boolean dirty = false;
    MultiCooldown.Entry activeUntilEntry = cooldown.entry(KEY_ACTIVE_UNTIL);
    MultiCooldown.Entry activeNextEntry = cooldown.entry(KEY_ACTIVE_NEXT_TICK);
    long activeUntil = activeUntilEntry.getReadyTick();
    if (activeUntil <= gameTime) {
      if (activeUntil != 0L) {
        activeUntilEntry.setReadyAt(0L);
        activeNextEntry.setReadyAt(0L);
        dirty = true;
      }
      return dirty;
    }

    long nextTick = activeNextEntry.getReadyTick();
    if (nextTick > gameTime) {
      return dirty;
    }

    double damage = Math.max(0.0, ACTIVE_DAMAGE_PER_SECOND * multiplier);
    double slowLevel = Math.max(0.0, ACTIVE_SLOWNESS_BASE * multiplier);
    if (damage <= 0.0) {
      activeNextEntry.setReadyAt(gameTime + 20L);
      dirty = true;
      return dirty;
    }

    List<LivingEntity> targets = collectTargets(level, user, ACTIVE_RADIUS, HOSTILE_TARGET);
    DamageSource source = resolveDamageSource(user);
    int slowAmplifier = Math.max(0, Mth.floor(slowLevel) - 1);
    for (LivingEntity target : targets) {
      if (target == user || target.isAlliedTo(user)) {
        continue;
      }
      // 以 DoT 统一调度 1s 脉冲并携带类型标识；保留燃烧与减速效果
      DoTEngine.schedulePerSecond(
          user,
          target,
          damage,
          1,
          CCSoundEvents.CUSTOM_FIRE_HUO_YI.get(),
          0.6f,
          1.0f,
          DoTTypes.YAN_DAO_HUO_YI_AURA,
          null,
          DoTEngine.FxAnchor.TARGET,
          Vec3.ZERO,
          1.0f);
      target.setRemainingFireTicks(4 * 20);
      target.addEffect(
          new MobEffectInstance(
              MobEffects.MOVEMENT_SLOWDOWN,
              ACTIVE_SLOWNESS_DURATION_TICKS,
              slowAmplifier,
              false,
              true,
              true));
      ReactionTagOps.add(target, ReactionTagKeys.FLAME_TRAIL, 80);
    }

    activeNextEntry.setReadyAt(gameTime + 20L);
    dirty = true;
    return dirty;
  }

  private boolean tickPassivePulse(
      ServerLevel level,
      LivingEntity user,
      OrganState state,
      MultiCooldown cooldown,
      long gameTime,
      double multiplier) {
    boolean dirty = false;
    boolean passiveActive = state.getBoolean(KEY_PASSIVE_ACTIVE, true);
    MultiCooldown.Entry passiveToggleEntry = cooldown.entry(KEY_PASSIVE_TOGGLE_TICK);
    MultiCooldown.Entry passiveNextEntry = cooldown.entry(KEY_PASSIVE_NEXT_TICK);
    long toggleTick = passiveToggleEntry.getReadyTick();
    if (toggleTick <= 0L) {
      passiveActive = true;
      dirty |= state.setBoolean(KEY_PASSIVE_ACTIVE, true, true).changed();
      passiveToggleEntry.setReadyAt(gameTime + PASSIVE_ACTIVE_DURATION_TICKS);
      passiveNextEntry.setReadyAt(gameTime);
      dirty = true;
      toggleTick = gameTime + PASSIVE_ACTIVE_DURATION_TICKS;
    } else if (gameTime >= toggleTick) {
      passiveActive = !passiveActive;
      dirty |= state.setBoolean(KEY_PASSIVE_ACTIVE, passiveActive, true).changed();
      long nextToggle =
          gameTime + (passiveActive ? PASSIVE_ACTIVE_DURATION_TICKS : PASSIVE_DOWNTIME_TICKS);
      passiveToggleEntry.setReadyAt(nextToggle);
      dirty = true;
      if (passiveActive) {
        passiveNextEntry.setReadyAt(gameTime);
        dirty = true;
      }
    }

    if (!passiveActive) {
      return dirty;
    }

    long nextTick = passiveNextEntry.getReadyTick();
    if (nextTick > gameTime) {
      return dirty;
    }

    double damage = Math.max(0.0, PASSIVE_DAMAGE_PER_SECOND * multiplier);
    if (damage > 0.0) {
      List<LivingEntity> targets = collectTargets(level, user, PASSIVE_RADIUS, HOSTILE_TARGET);
      for (LivingEntity target : targets) {
        if (target == user || target.isAlliedTo(user)) {
          continue;
        }
        DoTEngine.schedulePerSecond(
            user,
            target,
            damage,
            1,
            CCSoundEvents.CUSTOM_FIRE_HUO_YI.get(),
            0.35f,
            1.1f,
            DoTTypes.YAN_DAO_HUO_YI_AURA,
            null,
            DoTEngine.FxAnchor.TARGET,
            Vec3.ZERO,
            0.8f);
        target.setRemainingFireTicks(2 * 20);
      }
    }

    passiveNextEntry.setReadyAt(gameTime + 20L);
    dirty = true;
    return dirty;
  }

  private double resolveYanDaoMultiplier(ChestCavityInstance cc) {
    if (cc == null) {
      return 1.0;
    }
    ActiveLinkageContext context = LinkageManager.getContext(cc);
    if (context == null) {
      return 1.0;
    }
    LinkageChannel channel = context.lookupChannel(YAN_DAO_INCREASE_CHANNEL).orElse(null);
    double increase = channel == null ? 0.0 : Math.max(0.0, channel.get());
    return Math.max(0.0, 1.0 + increase);
  }

  private static DamageSource resolveDamageSource(LivingEntity user) {
    if (user instanceof Player player) {
      return player.damageSources().playerAttack(player);
    }
    return user.damageSources().mobAttack(user);
  }

  private MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ) {
    MultiCooldown.Builder builder =
        MultiCooldown.builder(OrganState.of(organ, STATE_ROOT))
            .withLongClamp(value -> Math.max(0L, value), 0L);
    if (cc != null) {
      builder.withSync(cc, organ);
    } else {
      builder.withOrgan(organ);
    }
    return builder.build();
  }

  private static List<LivingEntity> collectTargets(
      ServerLevel level, LivingEntity user, double radius, Predicate<LivingEntity> predicate) {
    Vec3 center = user.position();
    double radiusSq = radius * radius;
    AABB box =
        new AABB(
            center.x - radius,
            center.y - radius,
            center.z - radius,
            center.x + radius,
            center.y + radius,
            center.z + radius);
    return level.getEntitiesOfClass(
        LivingEntity.class,
        box,
        entity ->
            entity != null
                && entity.isAlive()
                && entity != user
                && entity.distanceToSqr(center) <= radiusSq
                && predicate.test(entity));
  }

  private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof Player player) || cc == null || entity.level().isClientSide()) {
      LOGGER.debug(
          "{} early-return: not valid server player or cc missing (isPlayer={}, ccNull={}, clientSide={})",
          LOG_PREFIX,
          (entity instanceof Player),
          (cc == null),
          entity.level().isClientSide());
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      LOGGER.debug(
          "{} early-return: organ not found in chest cavity for {}",
          LOG_PREFIX,
          player.getScoreboardName());
      return;
    }
    Level level = entity.level();
    if (!(level instanceof ServerLevel serverLevel)) {
      LOGGER.debug(
          "{} early-return: not a server level (clientSide={})", LOG_PREFIX, level.isClientSide());
      return;
    }
    long gameTime = level.getGameTime();
    OrganState state = OrganState.of(organ, STATE_ROOT);
    MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ);
    MultiCooldown.Entry cooldownUntilEntry = cooldown.entry(KEY_COOLDOWN_UNTIL);
    MultiCooldown.Entry activeUntilEntry = cooldown.entry(KEY_ACTIVE_UNTIL);
    MultiCooldown.Entry activeNextEntry = cooldown.entry(KEY_ACTIVE_NEXT_TICK);
    long cooldownUntil = Math.max(0L, cooldownUntilEntry.getReadyTick());
    if (cooldownUntil > gameTime) {
      long remaining = cooldownUntil - gameTime;
      long clamped = Mth.clamp(remaining, 0L, ACTIVE_COOLDOWN_TICKS);
      if (clamped != remaining) {
        cooldownUntilEntry.setReadyAt(gameTime + clamped);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
      }
      LOGGER.debug(
          "{} early-return: on cooldown (remaining={}t clamped={}t)",
          LOG_PREFIX,
          remaining,
          clamped);
      return;
    }

    if (cooldownUntil > 0L) {
      cooldownUntilEntry.setReadyAt(0L);
      NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    ConsumptionResult payment = ResourceOps.consumeStrict(player, ACTIVE_ZHENYUAN_COST, 0.0);
    if (!payment.succeeded()) {
      LOGGER.debug(
          "{} early-return: zhenyuan payment failed (required={})",
          LOG_PREFIX,
          ACTIVE_ZHENYUAN_COST);
      return;
    }
    if (!tryConsumeHunger(player, ACTIVE_HUNGER_COST)) {
      ResourceOps.refund(player, payment);
      LOGGER.debug(
          "{} early-return: hunger insufficient (required={})", LOG_PREFIX, ACTIVE_HUNGER_COST);
      return;
    }

    long activeUntil = gameTime + ACTIVE_DURATION_TICKS;
    activeUntilEntry.setReadyAt(activeUntil);
    activeNextEntry.setReadyAt(gameTime);
    long readyAt = gameTime + ACTIVE_COOLDOWN_TICKS;
    cooldownUntilEntry.setReadyAt(readyAt);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);

    // Visual feedback: a small burst of flames on activation
    spawnActivationParticles(serverLevel, entity);

    LOGGER.debug(
        "{} activated: duration={}t cooldown={}t radius={} damagePerSec={} player={}",
        LOG_PREFIX,
        ACTIVE_DURATION_TICKS,
        ACTIVE_COOLDOWN_TICKS,
        ACTIVE_RADIUS,
        ACTIVE_DAMAGE_PER_SECOND,
        player.getScoreboardName());

    if (player instanceof ServerPlayer serverPlayer) {
      AbilityFxDispatcher.play(serverPlayer, FIRE_HUO_YI_FX, Vec3.ZERO, 1.0F);
    } else {
      BuiltInRegistries.SOUND_EVENT
          .getOptional(FIRE_HUO_YI_FX)
          .ifPresent(
              sound ->
                  serverLevel.playSound(
                      null, entity.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, 1.0F));
    }

    if (player instanceof ServerPlayer sp) {
      ActiveSkillRegistry.scheduleReadyToast(sp, ABILITY_ID, readyAt, gameTime);
    }
  }

  private static void spawnActivationParticles(ServerLevel level, LivingEntity user) {
    if (level == null || user == null) {
      return;
    }
    double x = user.getX();
    double y = user.getY() + user.getBbHeight() * 0.6;
    double z = user.getZ();
    // A small, subtle flame burst around the torso
    level.sendParticles(ParticleTypes.FLAME, x, y, z, 14, 0.35, 0.20, 0.35, 0.01);
    level.sendParticles(ParticleTypes.SMALL_FLAME, x, y + 0.1, z, 8, 0.25, 0.15, 0.25, 0.005);
  }

  private static boolean tryConsumeHunger(Player player, int hungerCost) {
    if (player == null || hungerCost <= 0) {
      return true;
    }
    FoodData foodData = player.getFoodData();
    if (foodData == null) {
      return false;
    }
    if (foodData.getFoodLevel() < hungerCost) {
      return false;
    }
    foodData.setFoodLevel(Math.max(0, foodData.getFoodLevel() - hungerCost));
    if (foodData.getSaturationLevel() > foodData.getFoodLevel()) {
      foodData.setSaturation(foodData.getFoodLevel());
    }
    return true;
  }

  private static ItemStack findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (Objects.equals(id, ORGAN_ID)) {
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }
}
