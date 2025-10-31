package net.tigereye.chestcavity.compat.guzhenren.item.tian_dao.behavior;

import com.mojang.logging.LogUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.item.tian_dao.tuning.ShouGuTuning;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.tian_dao.calculator.ShouGuCalculator;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * 寿蛊（天道·寿命）三阶段行为：寿纹、寿债、GraceShift 与主动技「换命・续命」。
 *
 * <p>该类集中处理：
 *
 * <ul>
 *   <li>寿纹生成 / 消耗、受禁疗影响的自愈与利息倍率。
 *   <li>GraceShift 致死拦截、寿债阈值溢出与摘除惩罚。
 *   <li>主动技换命・续命（含资源扣除、治疗、冷却缩减、寿债回填）。
 * </ul>
 */
public final class ShouGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener,
        OrganIncomingDamageListener,
        OrganOnHitListener,
        OrganRemovalListener {

  public static final ShouGuOrganBehavior INSTANCE = new ShouGuOrganBehavior();

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final String LOG_PREFIX = "[compat/guzhenren][tian_dao][shou_gu]";
  private static final String MOD_ID = "guzhenren";

  private static final String STATE_ROOT = "ShouGu"; // OrganState 根键
  private static final String KEY_MARKS = "LongevityMarks"; // 当前寿纹层数
  private static final String KEY_DEFERRED_DAMAGE = "DeferredDamage"; // 累积寿债
  private static final String KEY_LAST_COMBAT_TICK = "LastCombatTick"; // 上一次参与战斗时间
  private static final String KEY_NEXT_MARK_TICK = "NextMarkTick"; // 下次寿纹结算 tick
  private static final String KEY_NEXT_INTEREST_TICK = "NextInterestTick"; // 下次寿债利息 tick
  private static final String KEY_ACTIVE_UNTIL = "ActiveUntil"; // 主动技持续截止 tick
  private static final String KEY_ACTIVE_MARKS_SPENT = "ActiveMarksSpent"; // 本次主动技消耗寿纹层数
  private static final String KEY_ACTIVE_NEXT_HEAL = "ActiveNextHeal"; // 主动技下一次回血 tick
  private static final String KEY_ACTIVE_DAMAGE_TOTAL = "ActiveDamageTotal"; // 主动技期间累计承伤

  private static final String KEY_GRACE_SHIFT_READY = "GraceShiftReadyAt"; // GraceShift 冷却就绪 tick
  private static final String KEY_ABILITY_READY = "AbilityReadyAt"; // 主动技冷却就绪 tick

  // 参数已迁移至 ShouGuTuning；本类不再保留静态数值常量。

  private static final ResourceLocation JIN_LIAO_EFFECT_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jin_liao");
  private static Holder<MobEffect> cachedJinLiaoEffect;
  private static boolean triedResolveJinLiao;

  // organId -> 分阶段参数，用于在 tick / 触发时快速查表。
  private static final Map<ResourceLocation, TierParameters> TIERS_BY_ORGAN = new LinkedHashMap<>();
  private static final Map<ResourceLocation, TierParameters> TIERS_BY_ABILITY =
      new LinkedHashMap<>();

  static {
    // 三个阶段的基线数据：寿纹上限 / 阈值 / 冷却时间等，与文档保持同步。
    registerTier(
        new TierParameters(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "shou_gu"),
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "shou_gu"),
            "shou_gu_1",
            ShouGuTuning.S1_MAX_MARKS,
            ShouGuTuning.S1_BASE_DEBT_THRESHOLD,
            ShouGuTuning.S1_DEBT_THRESHOLD_PER_MARK,
            ShouGuTuning.S1_GRACE_SHIFT_COOLDOWN_TICKS,
            ShouGuTuning.S1_ACTIVE_ABILITY_COOLDOWN_TICKS,
            ShouGuTuning.S1_ABILITY_DEFERRED_RATIO));
    registerTier(
        new TierParameters(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "shi_nian_shou_gu"),
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "shi_nian_shou_gu"),
            "shou_gu_10",
            ShouGuTuning.S2_MAX_MARKS,
            ShouGuTuning.S2_BASE_DEBT_THRESHOLD,
            ShouGuTuning.S2_DEBT_THRESHOLD_PER_MARK,
            ShouGuTuning.S2_GRACE_SHIFT_COOLDOWN_TICKS,
            ShouGuTuning.S2_ACTIVE_ABILITY_COOLDOWN_TICKS,
            ShouGuTuning.S2_ABILITY_DEFERRED_RATIO));
    registerTier(
        new TierParameters(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "bainianshougu"),
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "bainianshougu"),
            "shou_gu_100",
            ShouGuTuning.S3_MAX_MARKS,
            ShouGuTuning.S3_BASE_DEBT_THRESHOLD,
            ShouGuTuning.S3_DEBT_THRESHOLD_PER_MARK,
            ShouGuTuning.S3_GRACE_SHIFT_COOLDOWN_TICKS,
            ShouGuTuning.S3_ACTIVE_ABILITY_COOLDOWN_TICKS,
            ShouGuTuning.S3_ABILITY_DEFERRED_RATIO));

    for (TierParameters tier : TIERS_BY_ABILITY.values()) {
      OrganActivationListeners.register(
          tier.abilityId(),
          (entity, cc) -> {
            // ActiveSkillRegistry 触发时统一回调此处；仅在实体确实携带对应寿蛊时继续。
            if (!(entity instanceof ServerPlayer player) || player.level().isClientSide()) {
              return;
            }
            ItemStack organ = findOrgan(cc, tier.organId());
            if (organ.isEmpty()) {
              return;
            }
            INSTANCE.handleAbilityTrigger(player, cc, organ, tier);
          });
    }
  }

  private ShouGuOrganBehavior() {}

  public static Optional<TierParameters> tierForOrgan(ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return Optional.empty();
    }
    ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
    return Optional.ofNullable(TIERS_BY_ORGAN.get(id));
  }

  public static Optional<TierParameters> tierForAbility(ResourceLocation abilityId) {
    if (abilityId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(TIERS_BY_ABILITY.get(abilityId));
  }

  public static Set<TierParameters> tiers() {
    return Set.copyOf(TIERS_BY_ORGAN.values());
  }

  public void onEquip(
      ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
    if (cc == null || organ == null || organ.isEmpty() || staleRemovalContexts == null) {
      return;
    }
    Optional<TierParameters> tierOpt = tierForOrgan(organ);
    if (tierOpt.isEmpty()) {
      return;
    }
    registerRemovalHook(cc, organ, this, staleRemovalContexts);
    OrganState state = organState(organ, STATE_ROOT);
    ensureDefaults(state, cc, organ, tierOpt.get());
  }

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof Player player) || player.level().isClientSide()) {
      return;
    }
    // 每秒处理寿纹生成、寿债偿还/利息、主动技续命治疗与阈值溢出。
    Optional<TierParameters> tierOpt = tierForOrgan(organ);
    if (tierOpt.isEmpty()) {
      return;
    }
    TierParameters tier = tierOpt.get();
    Level level = player.level();
    long now = level.getGameTime();

    OrganState state = organState(organ, STATE_ROOT);
    ensureDefaults(state, cc, organ, tier);
    MultiCooldown cooldown = cooldown(state, cc, organ);
    boolean jinliao = isJinLiaoActive(player);

    tickAbility(player, cc, organ, state, tier, cooldown, now, jinliao);
    tickMarks(player, cc, organ, state, tier, now, jinliao);
    tickDeferredDebt(player, cc, organ, state, tier, now, jinliao);
    monitorThreshold(player, cc, organ, state, tier, now);
  }

  @Override
  public float onIncomingDamage(
      DamageSource source,
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!(victim instanceof Player player) || player.level().isClientSide()) {
      return damage;
    }
    // 三个核心路径：
    // 1) 环境减伤：寿纹数量对火/掉落/雷击等提供减免。
    // 2) 主动技开启期间：按阶段系数把部分伤害累积到寿债，剩余部分走正常承伤。
    // 3) 致死判定：GraceShift 冷却完毕时劫杀转为寿债并重置生命值。
    Optional<TierParameters> tierOpt = tierForOrgan(organ);
    if (tierOpt.isEmpty()) {
      return damage;
    }
    TierParameters tier = tierOpt.get();
    OrganState state = organState(organ, STATE_ROOT);
    ensureDefaults(state, cc, organ, tier);
    MultiCooldown cooldown = cooldown(state, cc, organ);
    long now = player.level().getGameTime();

    logCombat(state, cc, organ, now);

    int marks = state.getInt(KEY_MARKS, 0);
    if (marks > 0 && isEnvironmental(source)) {
      double factor =
          ShouGuCalculator.environmentReductionFactor(
              ShouGuTuning.ENVIRONMENT_REDUCTION_PER_MARK, marks);
      damage *= (float) factor;
    }

    if (damage <= 0.0F) {
      return 0.0F;
    }

    double deferred = state.getDouble(KEY_DEFERRED_DAMAGE, 0.0);

    if (isAbilityActive(state, now)) {
      double ratio = tier.abilityDeferredRatio();
      double converted = damage * ratio;
      double residual = damage - converted;
      deferred = Math.max(0.0, deferred + converted);
      OrganStateOps.setDouble(
          state, cc, organ, KEY_DEFERRED_DAMAGE, deferred, value -> Math.max(0.0, value), 0.0);
      double totalDamage = state.getDouble(KEY_ACTIVE_DAMAGE_TOTAL, 0.0);
      OrganStateOps.setDouble(
          state,
          cc,
          organ,
          KEY_ACTIVE_DAMAGE_TOTAL,
          totalDamage + damage,
          value -> Math.max(0.0, value),
          0.0);
      damage = (float) Math.max(0.0, residual);
    }

    float remainingHealth = player.getHealth() - damage;
    MultiCooldown.Entry graceEntry = cooldown.entry(KEY_GRACE_SHIFT_READY).withDefault(0L);
    if (remainingHealth <= 0.0F && graceEntry.isReady(now)) {
      // GraceShift：将致死伤害加入寿债，设置冷却并播放保护效果。
      deferred = Math.max(0.0, deferred + damage);
      OrganStateOps.setDouble(
          state, cc, organ, KEY_DEFERRED_DAMAGE, deferred, value -> Math.max(0.0, value), 0.0);
      graceEntry.setReadyAt(now + tier.graceShiftCooldownTicks());
      applyGraceShift(player, tier, cooldown, now);
      return 0.0F;
    }

    OrganStateOps.setDouble(
        state, cc, organ, KEY_DEFERRED_DAMAGE, deferred, value -> Math.max(0.0, value), 0.0);
    return damage;
  }

  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!(attacker instanceof Player) || attacker.level().isClientSide()) {
      return damage;
    }
    Optional<TierParameters> tierOpt = tierForOrgan(organ);
    if (tierOpt.isEmpty()) {
      return damage;
    }
    OrganState state = organState(organ, STATE_ROOT);
    long now = attacker.level().getGameTime();
    logCombat(state, cc, organ, now);
    return damage;
  }

  @Override
  public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof Player player) || player.level().isClientSide()) {
      return;
    }
    Optional<TierParameters> tierOpt = tierForOrgan(organ);
    if (tierOpt.isEmpty()) {
      return;
    }
    // 摘除时立即清算寿债：超阈值一定比例会直接判死，否则按倍数补偿并施加凋零。
    TierParameters tier = tierOpt.get();
    OrganState state = organState(organ, STATE_ROOT);
    double deferred = state.getDouble(KEY_DEFERRED_DAMAGE, 0.0);
    if (deferred <= 0.0) {
      return;
    }
    int marks = state.getInt(KEY_MARKS, 0);
    double threshold = tier.computeDebtThreshold(marks);
    if (deferred > threshold * ShouGuTuning.REMOVAL_OVERFLOW_FACTOR) {
      killWithDebt(player);
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "{} removal overflow triggered lethal fail for {}", LOG_PREFIX, describeStack(organ));
      }
      return;
    }
    double damage = deferred * ShouGuTuning.REMOVAL_DAMAGE_MULTIPLIER;
    applyDebtDamage(player, damage);
    player.addEffect(
        new MobEffectInstance(
            MobEffects.WITHER,
            (int) (ShouGuTuning.REMOVAL_WITHER_SECONDS * 20),
            3,
            false,
            false,
            true));
    OrganStateOps.setDouble(
        state, cc, organ, KEY_DEFERRED_DAMAGE, 0.0, value -> Math.max(0.0, value), 0.0);
    OrganStateOps.setInt(
        state, cc, organ, KEY_MARKS, 0, value -> Mth.clamp(value, 0, tier.maxLongevityMarks()), 0);
  }

  private void tickMarks(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      TierParameters tier,
      long now,
      boolean jinliao) {
    int marks = state.getInt(KEY_MARKS, 0);
    long lastCombat = state.getLong(KEY_LAST_COMBAT_TICK, 0L);
    boolean inCombat = now - lastCombat <= ShouGuTuning.COMBAT_WINDOW_TICKS;
    long nextTick = state.getLong(KEY_NEXT_MARK_TICK, 0L);
    if (!jinliao && marks < tier.maxLongevityMarks() && now >= nextTick) {
      marks = Mth.clamp(marks + 1, 0, tier.maxLongevityMarks());
      OrganStateOps.setInt(
          state,
          cc,
          organ,
          KEY_MARKS,
          marks,
          value -> Mth.clamp(value, 0, tier.maxLongevityMarks()),
          0);
      nextTick =
          ShouGuCalculator.nextMarkTick(
              now,
              inCombat,
              ShouGuTuning.MARK_INTERVAL_COMBAT_TICKS,
              ShouGuTuning.MARK_INTERVAL_OUT_OF_COMBAT_TICKS);
      OrganStateOps.setLong(
          state, cc, organ, KEY_NEXT_MARK_TICK, nextTick, value -> Math.max(0L, value), 0L);
    } else if (now >= nextTick) {
      nextTick = now + 20L;
      OrganStateOps.setLong(
          state, cc, organ, KEY_NEXT_MARK_TICK, nextTick, value -> Math.max(0L, value), 0L);
    }

    if (!jinliao && marks > 0 && !player.isDeadOrDying()) {
      double healAmount = marks * ShouGuTuning.MARK_SELF_HEAL_PER_SECOND;
      player.heal((float) healAmount);
    }
  }

  private void tickDeferredDebt(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      TierParameters tier,
      long now,
      boolean jinliao) {
    double deferred = state.getDouble(KEY_DEFERRED_DAMAGE, 0.0);
    if (deferred <= 0.0 || player.isDeadOrDying()) {
      return;
    }
    int marks = state.getInt(KEY_MARKS, 0);
    double repay =
        ShouGuCalculator.computeRepay(
            ShouGuTuning.REPAY_BASE,
            ShouGuTuning.REPAY_PER_MARK,
            marks,
            jinliao,
            ShouGuTuning.REPAY_JINLIAO_MULTIPLIER,
            deferred);
    if (repay <= 0.0) {
      return;
    }
    float before = player.getHealth();
    player.hurt(player.damageSources().magic(), (float) repay);
    float after = player.getHealth();
    double actual = Math.max(0.0, before - after);
    if (actual <= 0.0) {
      actual = repay;
    }
    deferred = Math.max(0.0, deferred - actual);
    OrganStateOps.setDouble(
        state, cc, organ, KEY_DEFERRED_DAMAGE, deferred, value -> Math.max(0.0, value), 0.0);

    long nextInterest =
        state.getLong(KEY_NEXT_INTEREST_TICK, now + ShouGuTuning.INTEREST_INTERVAL_TICKS);
    if (now >= nextInterest && deferred > 0.0) {
      double rate =
          ShouGuCalculator.interestRate(
              ShouGuTuning.BASE_INTEREST_RATE,
              ShouGuTuning.INTEREST_REDUCTION_PER_MARK,
              marks,
              jinliao);
      deferred = ShouGuCalculator.applyInterest(deferred, rate);
      OrganStateOps.setDouble(
          state, cc, organ, KEY_DEFERRED_DAMAGE, deferred, value -> Math.max(0.0, value), 0.0);
      OrganStateOps.setLong(
          state,
          cc,
          organ,
          KEY_NEXT_INTEREST_TICK,
          now + ShouGuTuning.INTEREST_INTERVAL_TICKS,
          value -> Math.max(0L, value),
          now + ShouGuTuning.INTEREST_INTERVAL_TICKS);
    }
  }

  private void monitorThreshold(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      TierParameters tier,
      long now) {
    double deferred = state.getDouble(KEY_DEFERRED_DAMAGE, 0.0);
    if (deferred <= 0.0) {
      return;
    }
    int marks = state.getInt(KEY_MARKS, 0);
    double threshold = tier.computeDebtThreshold(marks);
    if (deferred <= threshold) {
      return;
    }
    double overflow = deferred - threshold;
    applyDebtDamage(player, overflow);
    player.addEffect(
        new MobEffectInstance(
            MobEffects.WITHER,
            (int) (ShouGuTuning.DEBT_OVERFLOW_WITHER_SECONDS * 20),
            1,
            false,
            false,
            true));
    OrganStateOps.setDouble(
        state, cc, organ, KEY_DEFERRED_DAMAGE, threshold, value -> Math.max(0.0, value), 0.0);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "{} threshold overflow triggered: deferred={} threshold={} organ={}",
          LOG_PREFIX,
          deferred,
          threshold,
          describeStack(organ));
    }
  }

  private void tickAbility(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      TierParameters tier,
      MultiCooldown cooldown,
      long now,
      boolean jinliao) {
    long activeUntil = state.getLong(KEY_ACTIVE_UNTIL, 0L);
    if (activeUntil > 0L && now >= activeUntil) {
      finishAbility(player, cc, organ, state, tier, cooldown, now);
      activeUntil = 0L;
    }
    if (activeUntil <= 0L) {
      return;
    }
    long nextHeal = state.getLong(KEY_ACTIVE_NEXT_HEAL, now);
    if (now >= nextHeal) {
      int spent = Math.max(0, state.getInt(KEY_ACTIVE_MARKS_SPENT, 0));
      double heal =
          ShouGuCalculator.healPerTick(
              ShouGuTuning.ACTIVE_BASE_HEAL, ShouGuTuning.ACTIVE_HEAL_PER_CONSUMED, spent);
      if (!player.isDeadOrDying()) {
        player.heal((float) heal);
      }
      OrganStateOps.setLong(
          state,
          cc,
          organ,
          KEY_ACTIVE_NEXT_HEAL,
          ShouGuCalculator.nextHealTick(now, ShouGuTuning.ACTIVE_HEAL_INTERVAL_TICKS),
          value -> Math.max(0L, value),
          ShouGuCalculator.nextHealTick(now, ShouGuTuning.ACTIVE_HEAL_INTERVAL_TICKS));
    }
  }

  private void finishAbility(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      TierParameters tier,
      MultiCooldown cooldown,
      long now) {
    double totalDamage = state.getDouble(KEY_ACTIVE_DAMAGE_TOTAL, 0.0);
    if (totalDamage > 0.0) {
      double extra = totalDamage * ShouGuTuning.FINISH_EXTRA_DEFERRED_RATIO;
      double deferred = state.getDouble(KEY_DEFERRED_DAMAGE, 0.0);
      deferred = Math.max(0.0, deferred + extra);
      OrganStateOps.setDouble(
          state, cc, organ, KEY_DEFERRED_DAMAGE, deferred, value -> Math.max(0.0, value), 0.0);
    }
    OrganStateOps.setLong(state, cc, organ, KEY_ACTIVE_UNTIL, 0L, value -> Math.max(0L, value), 0L);
    OrganStateOps.setInt(
        state, cc, organ, KEY_ACTIVE_MARKS_SPENT, 0, value -> Math.max(0, value), 0);
    OrganStateOps.setLong(
        state, cc, organ, KEY_ACTIVE_NEXT_HEAL, 0L, value -> Math.max(0L, value), 0L);
    OrganStateOps.setDouble(
        state, cc, organ, KEY_ACTIVE_DAMAGE_TOTAL, 0.0, value -> Math.max(0.0, value), 0.0);

    MultiCooldown.Entry graceEntry = cooldown.entry(KEY_GRACE_SHIFT_READY).withDefault(0L);
    long readyAt = graceEntry.getReadyTick();
    long reduced = Math.max(now, readyAt - ShouGuTuning.GRACE_COOLDOWN_REDUCTION_TICKS);
    graceEntry.setReadyAt(Math.max(reduced, 0L));

    if (player instanceof ServerPlayer serverPlayer) {
      MultiCooldown.Entry abilityEntry = cooldown.entry(KEY_ABILITY_READY).withDefault(0L);
      ActiveSkillRegistry.scheduleReadyToast(
          serverPlayer, tier.abilityId(), abilityEntry.getReadyTick(), now);
    }
  }

  private void applyGraceShift(
      Player player, TierParameters tier, MultiCooldown cooldown, long now) {
    float targetHealth =
        (float) (ShouGuTuning.GRACE_HEALTH_BASE
            + player.getAbsorptionAmount() * ShouGuTuning.GRACE_HEALTH_ABSORB_SCALE);
    player.setHealth(targetHealth);
    player.invulnerableTime = Math.max(player.invulnerableTime, ShouGuTuning.GRACE_INVULN_TICKS);
    player.addEffect(
        new MobEffectInstance(
            MobEffects.DAMAGE_RESISTANCE,
            ShouGuTuning.GRACE_RESIST_TICKS,
            Math.max(0, ShouGuTuning.GRACE_RESIST_AMPLIFIER),
            false,
            false,
            true));
    player.addEffect(
        new MobEffectInstance(
            MobEffects.WEAKNESS,
            ShouGuTuning.GRACE_WEAKNESS_TICKS,
            Math.max(0, ShouGuTuning.GRACE_WEAKNESS_AMPLIFIER),
            false,
            false,
            true));
    player.addEffect(
        new MobEffectInstance(
            MobEffects.MOVEMENT_SLOWDOWN,
            ShouGuTuning.GRACE_SLOW_TICKS,
            Math.max(0, ShouGuTuning.GRACE_SLOW_AMPLIFIER),
            false,
            false,
            true));
    player
        .level()
        .playSound(
            null,
            player.blockPosition(),
            SoundEvents.TOTEM_USE,
            SoundSource.PLAYERS,
            ShouGuTuning.GRACE_SOUND_VOLUME,
            ShouGuTuning.GRACE_SOUND_PITCH);
    ReactionTagOps.add(player, ReactionTagKeys.HEAVEN_GRACE, ShouGuTuning.GRACE_TAG_DURATION_TICKS);
  }

  private void handleAbilityTrigger(
      ServerPlayer player, ChestCavityInstance cc, ItemStack organ, TierParameters tier) {
    Level level = player.level();
    long now = level.getGameTime();
    OrganState state = organState(organ, STATE_ROOT);
    ensureDefaults(state, cc, organ, tier);
    MultiCooldown cooldown = cooldown(state, cc, organ);
    MultiCooldown.Entry abilityEntry = cooldown.entry(KEY_ABILITY_READY).withDefault(0L);
    if (!abilityEntry.isReady(now)) {
      return;
    }

    int marks = state.getInt(KEY_MARKS, 0);
    if (marks <= 0) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "{} activation blocked: no longevity marks (organ={})",
            LOG_PREFIX,
            describeStack(organ));
      }
      return;
    }
    int consumed = Math.min(marks, ShouGuTuning.MAX_MARKS_SPENT_PER_CAST);

    // 分步扣除：先真元（按单位用量），再精力；若精力失败，回滚真元
    var handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    var handle = handleOpt.get();
    var zhenResult =
        ResourceOps.tryConsumeScaledZhenyuan(
            handle, ShouGuTuning.DESIGN_ZHUANSHU, ShouGuTuning.DESIGN_JIEDUAN, ShouGuTuning.ABILITY_ZHENYUAN_UNITS);
    if (zhenResult.isEmpty()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "{} activation blocked: zhenyuan insufficient organ={}",
            LOG_PREFIX,
            describeStack(organ));
      }
      return;
    }
    var jingAfter = ResourceOps.tryAdjustJingli(handle, -ShouGuTuning.ABILITY_JINGLI_COST, true);
    if (jingAfter.isEmpty()) {
      // 回滚真元
      ResourceOps.tryReplenishScaledZhenyuan(handle, ShouGuTuning.ABILITY_ZHENYUAN_BASE_COST, true);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("{} activation blocked: jingli insufficient organ={}", LOG_PREFIX, describeStack(organ));
      }
      return;
    }

    OrganStateOps.setInt(
        state,
        cc,
        organ,
        KEY_MARKS,
        marks - consumed,
        value -> Mth.clamp(value, 0, tier.maxLongevityMarks()),
        0);
    double deferred = state.getDouble(KEY_DEFERRED_DAMAGE, 0.0);
    deferred = Math.max(0.0, deferred - consumed * ShouGuTuning.MARK_CONSUME_DEBT_REDUCTION);
    OrganStateOps.setDouble(
        state, cc, organ, KEY_DEFERRED_DAMAGE, deferred, value -> Math.max(0.0, value), 0.0);
    if (!player.isDeadOrDying()) {
      player.heal((float) (ShouGuTuning.MARK_CONSUME_HEAL * consumed));
    }
    OrganStateOps.setInt(
        state, cc, organ, KEY_ACTIVE_MARKS_SPENT, consumed, value -> Math.max(0, value), 0);
    OrganStateOps.setLong(
        state,
        cc,
        organ,
        KEY_ACTIVE_UNTIL,
        now + ShouGuTuning.ACTIVE_DURATION_TICKS,
        value -> Math.max(0L, value),
        now + ShouGuTuning.ACTIVE_DURATION_TICKS);
    OrganStateOps.setLong(
        state,
        cc,
        organ,
        KEY_ACTIVE_NEXT_HEAL,
        ShouGuCalculator.nextHealTick(now, ShouGuTuning.ACTIVE_HEAL_INTERVAL_TICKS),
        value -> Math.max(0L, value),
        ShouGuCalculator.nextHealTick(now, ShouGuTuning.ACTIVE_HEAL_INTERVAL_TICKS));
    OrganStateOps.setDouble(
        state, cc, organ, KEY_ACTIVE_DAMAGE_TOTAL, 0.0, value -> Math.max(0.0, value), 0.0);

    abilityEntry.setReadyAt(now + tier.activeAbilityCooldownTicks());
    player
        .level()
        .playSound(
            null,
            player.blockPosition(),
            SoundEvents.BEACON_POWER_SELECT,
            SoundSource.PLAYERS,
            0.6F,
            0.9F);
    ActiveSkillRegistry.scheduleReadyToast(
        player, tier.abilityId(), abilityEntry.getReadyTick(), now);
  }

  private void applyDebtDamage(Player player, double amount) {
    if (amount <= 0.0) {
      return;
    }
    float before = player.getHealth();
    player.hurt(player.damageSources().wither(), (float) amount);
    float actual = Math.max(0.0F, before - player.getHealth());
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "{} debt applied: requested={} actual={} player={}",
          LOG_PREFIX,
          amount,
          actual,
          player.getScoreboardName());
    }
  }

  private void killWithDebt(Player player) {
    player.hurt(player.damageSources().magic(), Float.MAX_VALUE);
  }

  private void logCombat(OrganState state, ChestCavityInstance cc, ItemStack organ, long now) {
    OrganStateOps.setLong(
        state, cc, organ, KEY_LAST_COMBAT_TICK, now, value -> Math.max(0L, value), now);
  }

  private static MultiCooldown cooldown(OrganState state, ChestCavityInstance cc, ItemStack organ) {
    return MultiCooldown.builder(state)
        .withSync(cc, organ)
        .withLongClamp(value -> Math.max(0L, value), 0L)
        .withIntClamp(value -> Math.max(0, value), 0)
        .build();
  }

  private static boolean isAbilityActive(OrganState state, long now) {
    return now < state.getLong(KEY_ACTIVE_UNTIL, 0L);
  }

  private static ItemStack findOrgan(@Nullable ChestCavityInstance cc, ResourceLocation organId) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack candidate = cc.inventory.getItem(i);
      if (candidate.isEmpty()) continue;
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(candidate.getItem());
      if (organId.equals(id)) {
        return candidate;
      }
    }
    return ItemStack.EMPTY;
  }

  private static boolean isEnvironmental(DamageSource source) {
    if (source == null) {
      return false;
    }
    return source.is(DamageTypeTags.IS_FIRE)
        || source.is(DamageTypeTags.IS_FALL)
        || source.is(DamageTypeTags.IS_LIGHTNING)
        || source.is(DamageTypeTags.IS_FREEZING);
  }

  private static boolean isJinLiaoActive(LivingEntity entity) {
    if (entity == null) {
      return false;
    }
    Holder<MobEffect> effect = resolveJinLiaoEffect();
    return effect != null && entity.hasEffect(effect);
  }

  private static Holder<MobEffect> resolveJinLiaoEffect() {
    if (!triedResolveJinLiao) {
      cachedJinLiaoEffect = BuiltInRegistries.MOB_EFFECT.getHolder(JIN_LIAO_EFFECT_ID).orElse(null);
      triedResolveJinLiao = true;
      if (cachedJinLiaoEffect == null && LOGGER.isDebugEnabled()) {
        LOGGER.debug("{} could not resolve jin_liao effect from registry", LOG_PREFIX);
      }
    }
    return cachedJinLiaoEffect;
  }

  private static void ensureDefaults(
      OrganState state, ChestCavityInstance cc, ItemStack organ, TierParameters tier) {
    int marks = state.getInt(KEY_MARKS, 0);
    if (marks < 0 || marks > tier.maxLongevityMarks()) {
      OrganStateOps.setInt(
          state,
          cc,
          organ,
          KEY_MARKS,
          Mth.clamp(marks, 0, tier.maxLongevityMarks()),
          value -> Mth.clamp(value, 0, tier.maxLongevityMarks()),
          0);
    }
    double deferred = state.getDouble(KEY_DEFERRED_DAMAGE, 0.0);
    if (deferred < 0.0) {
      OrganStateOps.setDouble(
          state,
          cc,
          organ,
          KEY_DEFERRED_DAMAGE,
          Math.max(0.0, deferred),
          value -> Math.max(0.0, value),
          0.0);
    }
  }

  private static void registerTier(TierParameters parameters) {
    TIERS_BY_ORGAN.put(parameters.organId(), parameters);
    TIERS_BY_ABILITY.put(parameters.abilityId(), parameters);
  }

  /** 每一阶段的核心数值配置。 */
  public static final class TierParameters {
    private final ResourceLocation organId;
    private final ResourceLocation abilityId;
    private final String stageKey;
    private final int maxLongevityMarks;
    private final int baseDebtThreshold;
    private final int debtThresholdPerMark;
    private final int graceShiftCooldownTicks;
    private final int activeAbilityCooldownTicks;
    private final double abilityDeferredRatio;

    private TierParameters(
        ResourceLocation organId,
        ResourceLocation abilityId,
        String stageKey,
        int maxLongevityMarks,
        int baseDebtThreshold,
        int debtThresholdPerMark,
        int graceShiftCooldownTicks,
        int activeAbilityCooldownTicks,
        double abilityDeferredRatio) {
      this.organId = Objects.requireNonNull(organId, "organId");
      this.abilityId = Objects.requireNonNull(abilityId, "abilityId");
      this.stageKey = Objects.requireNonNull(stageKey, "stageKey");
      this.maxLongevityMarks = maxLongevityMarks;
      this.baseDebtThreshold = baseDebtThreshold;
      this.debtThresholdPerMark = debtThresholdPerMark;
      this.graceShiftCooldownTicks = graceShiftCooldownTicks;
      this.activeAbilityCooldownTicks = activeAbilityCooldownTicks;
      this.abilityDeferredRatio = abilityDeferredRatio;
    }

    public ResourceLocation organId() {
      return organId;
    }

    public ResourceLocation abilityId() {
      return abilityId;
    }

    public String stageKey() {
      return stageKey;
    }

    public int maxLongevityMarks() {
      return maxLongevityMarks;
    }

    public int graceShiftCooldownTicks() {
      return graceShiftCooldownTicks;
    }

    public int activeAbilityCooldownTicks() {
      return activeAbilityCooldownTicks;
    }

    public double abilityDeferredRatio() {
      return abilityDeferredRatio;
    }

    public int computeDebtThreshold(int longevityMarks) {
      return baseDebtThreshold + Math.max(0, longevityMarks) * debtThresholdPerMark;
    }
  }
}
