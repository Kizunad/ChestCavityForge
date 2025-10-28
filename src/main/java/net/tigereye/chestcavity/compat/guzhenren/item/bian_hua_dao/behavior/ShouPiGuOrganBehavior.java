package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.OptionalDouble;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.AttributeOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.registration.CCDamageSources;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.AbsorptionHelper;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;

/**
 * 兽皮蛊（变化道核心防具）。
 *
 * <p>实现要点：
 *
 * <ul>
 *   <li>软反伤：把本器官造成的减免量按百分比回溅给攻击者，并在 5 阶时溅射周围目标。
 *   <li>皮厚 / 筋膜 / 坚忍计：持续记录受击与净承伤，用于后续组合杀招触发，不再由器官被动自动引爆。
 *   <li>主动技：硬皮鼓动（1 阶起）。
 *   <li>联动：软反伤命中后在 3 秒窗口内为目标挂载“皮虎同纹”标记。
 * </ul>
 */
public final class ShouPiGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganIncomingDamageListener, OrganSlowTickListener, OrganOnHitListener {

  public static final ShouPiGuOrganBehavior INSTANCE = new ShouPiGuOrganBehavior();

  private static final String MOD_ID = "guzhenren";

  public static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "shou_pi_gu");
  public static final ResourceLocation HUPI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "hupigu");
  public static final ResourceLocation TIE_GU_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "tie_gu_gu");

  private static final ResourceLocation ACTIVE_DRUM_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "skill/shou_pi_gu_drum");

  private static final ResourceLocation KNOCKBACK_MODIFIER_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/shou_pi_gu_knockback");
  private static final ResourceLocation STOIC_ABSORBTION_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/shou_pi_gu_stoic");

  public static final double THICK_SKIN_REDUCTION = 0.08D;
  public static final int THICK_SKIN_WINDOW_TICKS = 20;
  public static final int FASCIA_TRIGGER = 5;
  public static final long FASCIA_COOLDOWN_TICKS = 200L; // 10s
  public static final double FASCIA_ACTIVE_REDUCTION = 0.12D;
  public static final int STOIC_MAX_STACKS = 6;
  public static final long STOIC_DEFAULT_LOCK_TICKS = 8 * 20L;
  public static final long SOFT_POOL_WINDOW_TICKS = 5 * 20L;
  private static final long S1_MARK_DURATION_TICKS = 3 * 20L;
  public static final long ROLL_DAMAGE_WINDOW_TICKS = 12L; // 0.6s
  public static final double ROLL_DAMAGE_REDUCTION = 0.6D;
  public static final double ROLL_DISTANCE = 3.0D;
  public static final double CRASH_DISTANCE = 4.0D;
  public static final long CRASH_IMMUNE_TICKS = 10L;
  public static final double CRASH_SPLASH_RADIUS = 1.5D;
  public static final double STOIC_SLOW_RADIUS = 3.0D;
  public static final int STOIC_SLOW_TICKS = 40;
  public static final int STOIC_SLOW_AMPLIFIER = 0;
  public static final long SOFT_PROJECTILE_COOLDOWN_TICKS = 12L; // 0.6s shared thorns window
  public static final double STOIC_ACTIVE_SOFT_BONUS = 0.05D;

  private static final double ACTIVE_DRUM_DEFENSE_BONUS = 0.06D;
  private static final double ACTIVE_DRUM_SOFT_BONUS = 0.10D;
  private static final int ACTIVE_DRUM_DURATION_TICKS = 5 * 20;
  private static final long ACTIVE_DRUM_COOLDOWN_TICKS = 20 * 20L;
  private static final double ACTIVE_DRUM_KNOCKBACK_RESIST = 0.5D;
  private static final double ACTIVE_DRUM_BASE_COST = 40.0D;

  public static final double ACTIVE_ROLL_BASE_COST = 25.0D;
  public static final long ACTIVE_ROLL_COOLDOWN_TICKS = 14 * 20L;

  public static final double SYNERGY_CRASH_BASE_COST = 60.0D;
  public static final long SYNERGY_CRASH_COOLDOWN_TICKS = 18 * 20L;

  public static final String STATE_ROOT = "ShouPiGu";
  public static final String KEY_STAGE = "Stage";
  public static final String KEY_SOFT_READY = "SoftThornsReady";
  public static final String KEY_SOFT_PROJECTILE_READY = "SoftThornsProjectileReady";
  public static final String KEY_LAST_HIT = "LastHitTick";
  public static final String KEY_THICK_SKIN_EXPIRE = "ThickSkinExpire";
  public static final String KEY_THICK_SKIN_READY = "ThickSkinReady";
  public static final String KEY_FASCIA_COOLDOWN = "FasciaCooldown";
  public static final String KEY_FASCIA_COUNT = "FasciaHitCount";
  public static final String KEY_FASCIA_ACTIVE_UNTIL = "FasciaActiveUntil";
  public static final String KEY_STOIC_ACCUM = "StoicAccum";
  public static final String KEY_STOIC_STACKS = "StoicStacks";
  public static final String KEY_STOIC_READY = "StoicReady";
  public static final String KEY_STOIC_LOCK_UNTIL = "StoicLockUntil";
  public static final String KEY_STOIC_ACTIVE_UNTIL = "StoicActiveUntil";
  public static final String KEY_SOFT_POOL_VALUE = "SoftReflectPool";
  public static final String KEY_SOFT_POOL_EXPIRE = "SoftReflectExpire";
  public static final String KEY_SOFT_TEMP_BONUS = "SoftTempBonus";
  public static final String KEY_SOFT_TEMP_BONUS_EXPIRE = "SoftTempBonusExpire";
  public static final String KEY_ACTIVE_DRUM_EXPIRE = "DrumExpire";
  public static final String KEY_ACTIVE_DRUM_READY = "DrumReady";
  public static final String KEY_ROLL_READY = "RollReady";
  public static final String KEY_ROLL_EXPIRE = "RollExpire";
  public static final String KEY_CRASH_READY = "CrashReady";
  public static final String KEY_CRASH_IMMUNE = "CrashImmuneExpire";

  private static final EnumMap<Tier, TierParameters> TIER_PARAMS = new EnumMap<>(Tier.class);
  private static final String MARK_NBT_KEY = "guzhenren:shou_pi_gu_marks";

  static {
    TIER_PARAMS.put(
        Tier.STAGE1,
        new TierParameters(
            Tier.STAGE1,
            0.25D,
            20L,
            0.0D,
            0,
            Double.POSITIVE_INFINITY,
            0.0D,
            STOIC_DEFAULT_LOCK_TICKS));
    TIER_PARAMS.put(
        Tier.STAGE2,
        new TierParameters(
            Tier.STAGE2, 0.30D, 18L, 0.15D, 3, 100.0D, 0.0D, STOIC_DEFAULT_LOCK_TICKS));
    TIER_PARAMS.put(
        Tier.STAGE3,
        new TierParameters(
            Tier.STAGE3, 0.35D, 16L, 0.18D, 4, 100.0D, 0.0D, STOIC_DEFAULT_LOCK_TICKS));
    TIER_PARAMS.put(
        Tier.STAGE4,
        new TierParameters(
            Tier.STAGE4, 0.35D, 16L, 0.18D, 4, 100.0D, 0.40D, STOIC_DEFAULT_LOCK_TICKS));
    TIER_PARAMS.put(
        Tier.STAGE5,
        new TierParameters(Tier.STAGE5, 0.45D, 14L, 0.22D, 5, 200.0D, 0.45D, 10 * 20L));

    OrganActivationListeners.register(
        ACTIVE_DRUM_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateDrum(player, cc);
          }
        });
  }

  private ShouPiGuOrganBehavior() {}

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (entity == null || cc == null || organ == null || organ.isEmpty()) {
      return;
    }
    Level level = entity.level();
    if (!(level instanceof ServerLevel serverLevel)) {
      return;
    }
    OrganState state = organState(organ, STATE_ROOT);
    ensureStage(state, cc, organ);

    MultiCooldown cooldown = cooldown(cc, organ, state);
    long now = serverLevel.getGameTime();

    long drumExpire = state.getLong(KEY_ACTIVE_DRUM_EXPIRE, 0L);
    if (drumExpire > 0L && now >= drumExpire) {
      // 清理鼓动效果：重置持续时间即会终止减伤，并同步移除临时击退抗性
      state.setLong(KEY_ACTIVE_DRUM_EXPIRE, 0L, value -> Math.max(0L, value), 0L);
      removeDrumKnockback(entity);
      NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    boolean updated = false;

    long rollExpire = state.getLong(KEY_ROLL_EXPIRE, 0L);
    if (rollExpire > 0L && now >= rollExpire) {
      state.setLong(KEY_ROLL_EXPIRE, 0L, value -> Math.max(0L, value), 0L);
      updated = true;
    }

    long immuneExpire = state.getLong(KEY_CRASH_IMMUNE, 0L);
    if (immuneExpire > 0L && now >= immuneExpire) {
      state.setLong(KEY_CRASH_IMMUNE, 0L, value -> Math.max(0L, value), 0L);
      updated = true;
    }

    long fasciaExpire = state.getLong(KEY_FASCIA_ACTIVE_UNTIL, 0L);
    if (fasciaExpire > 0L && now >= fasciaExpire) {
      state.setLong(KEY_FASCIA_ACTIVE_UNTIL, 0L, value -> Math.max(0L, value), 0L);
      updated = true;
    }

    long stoicExpire = state.getLong(KEY_STOIC_ACTIVE_UNTIL, 0L);
    if (stoicExpire > 0L && now >= stoicExpire) {
      state.setLong(KEY_STOIC_ACTIVE_UNTIL, 0L, value -> Math.max(0L, value), 0L);
      updated = true;
    }

    long bonusExpire = state.getLong(KEY_SOFT_TEMP_BONUS_EXPIRE, 0L);
    if (bonusExpire > 0L && now >= bonusExpire) {
      state.setDouble(
          KEY_SOFT_TEMP_BONUS, 0.0D, value -> Math.max(0.0D, value), 0.0D);
      state.setLong(KEY_SOFT_TEMP_BONUS_EXPIRE, 0L, value -> Math.max(0L, value), 0L);
      updated = true;
    }

    if (updated) {
      NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    cooldown.entry(KEY_FASCIA_COOLDOWN).withDefault(0L);
  }

  @Override
  public float onIncomingDamage(
      DamageSource source,
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (victim == null || victim.level().isClientSide()) {
      return damage;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return damage;
    }
    OrganState state = organState(organ, STATE_ROOT);
    ensureStage(state, cc, organ);
    TierParameters params = tierParameters(state);
    if (params == null) {
      return damage;
    }
    MultiCooldown cooldown = cooldown(cc, organ, state);
    long now = victim.level().getGameTime();
    OrganStateOps.Collector collector = OrganStateOps.collector(cc, organ);

    double initialDamage = damage;
    double workingDamage = damage;
    double mitigatedFromOrgan = 0.0D;

    boolean bypassesArmor = source.is(DamageTypeTags.BYPASSES_ARMOR);

    if (state.getLong(KEY_ROLL_EXPIRE, 0L) > now) {
      double before = workingDamage;
      workingDamage *= 1.0D - ROLL_DAMAGE_REDUCTION;
      mitigatedFromOrgan += Math.max(0.0D, before - workingDamage);
    }

    if (state.getLong(KEY_ACTIVE_DRUM_EXPIRE, 0L) > now) {
      // 鼓动期间提供 6% 伤害系数加成，使用 ACTIVE_DRUM_DEFENSE_BONUS 以保持与常量一致
      double before = workingDamage;
      workingDamage *= 1.0D - ACTIVE_DRUM_DEFENSE_BONUS;
      mitigatedFromOrgan += Math.max(0.0D, before - workingDamage);
    }

    if (state.getLong(KEY_THICK_SKIN_EXPIRE, 0L) > now) {
      collector.record(state.setBoolean(KEY_THICK_SKIN_READY, true, false));
    }

    if (!bypassesArmor && state.getBoolean(KEY_THICK_SKIN_READY, false)) {
      double before = workingDamage;
      workingDamage *= 1.0D - THICK_SKIN_REDUCTION;
      mitigatedFromOrgan += Math.max(0.0D, before - workingDamage);
      collector.record(state.setBoolean(KEY_THICK_SKIN_READY, false, false));
      collector.record(state.setLong(KEY_THICK_SKIN_EXPIRE, 0L, value -> Math.max(0L, value), 0L));
    }

    int fasciaCount = Math.max(0, state.getInt(KEY_FASCIA_COUNT, 0) + 1);
    int clampedFascia = Math.min(fasciaCount, FASCIA_TRIGGER);
    collector.record(
        state.setInt(
            KEY_FASCIA_COUNT,
            clampedFascia,
            value -> Math.max(0, Math.min(value, FASCIA_TRIGGER)),
            0));

    long fasciaActiveUntil = state.getLong(KEY_FASCIA_ACTIVE_UNTIL, 0L);
    if (fasciaActiveUntil > now) {
      double before = workingDamage;
      workingDamage *= 1.0D - FASCIA_ACTIVE_REDUCTION;
      mitigatedFromOrgan += Math.max(0.0D, before - workingDamage);
    }

    long stoicActiveUntil = state.getLong(KEY_STOIC_ACTIVE_UNTIL, 0L);
    if (stoicActiveUntil > now) {
      double beforeStoic = workingDamage;
      workingDamage *= 1.0D - params.stoicMitigation();
      mitigatedFromOrgan += Math.max(0.0D, beforeStoic - workingDamage);
    }

    if (state.getLong(KEY_CRASH_IMMUNE, 0L) > now) {
      return 0.0F;
    }

    double finalDamage = Math.max(0.0D, workingDamage);
    double organMitigated = Math.max(0.0D, initialDamage - finalDamage);
    mitigatedFromOrgan = Math.max(mitigatedFromOrgan, organMitigated);

    updateStoicProgress(state, cc, organ, params, finalDamage, now);

    double reflect =
        attemptSoftReflect(
            source, victim, cc, organ, state, params, cooldown, now, mitigatedFromOrgan);
    if (reflect > 0.0D) {
      bumpSoftPool(state, cc, organ, reflect, now);
    }

    collector.record(state.setLong(KEY_LAST_HIT, now, value -> Math.max(0L, value), 0L));
    collector.record(
        state.setLong(
            KEY_THICK_SKIN_EXPIRE,
            now + THICK_SKIN_WINDOW_TICKS,
            value -> Math.max(0L, value),
            0L));

    collector.commit();
    return (float) finalDamage;
  }

  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (attacker == null || attacker.level().isClientSide()) {
      return damage;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return damage;
    }
    if (!(attacker instanceof Player player) || target == null) {
      return damage;
    }
    OrganState state = organState(organ, STATE_ROOT);
    ensureStage(state, cc, organ);

    // 从目标实体的持久化数据中读取标记
    CompoundTag persistentData = target.getPersistentData();
    if (persistentData.contains(MARK_NBT_KEY)) {
      CompoundTag marks = persistentData.getCompound(MARK_NBT_KEY);
      String playerKey = player.getUUID().toString();
      if (marks.contains(playerKey)) {
        long expireTick = marks.getLong(playerKey);
        long now = attacker.level().getGameTime();
        if (expireTick > now) {
          damage *= 1.10F;
          target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 16, 0, false, true));
        }
        // The mark is intentionally not removed here to allow it to work for the entire 3-second
        // window.
        // It will be cleaned up later by registerS1Mark when a new mark is applied.
      }
    }

    return damage;
  }

  private void activateDrum(ServerPlayer player, ChestCavityInstance cc) {
    if (player == null || cc == null) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    OrganState state = organState(organ, STATE_ROOT);
    MultiCooldown cooldown = cooldown(cc, organ, state);
    long now = player.level().getGameTime();
    MultiCooldown.Entry entry = cooldown.entry(KEY_ACTIVE_DRUM_READY).withDefault(0L);
    if (!entry.isReady(now)) {
      return;
    }
    OptionalDouble consumed = ResourceOps.tryConsumeScaledZhenyuan(player, ACTIVE_DRUM_BASE_COST);
    if (consumed.isEmpty()) {
      return;
    }
    entry.setReadyAt(now + ACTIVE_DRUM_COOLDOWN_TICKS);
    state.setLong(
        KEY_ACTIVE_DRUM_EXPIRE, now + ACTIVE_DRUM_DURATION_TICKS, value -> Math.max(0L, value), 0L);
    applyDrumBuff(player);
    ActiveSkillRegistry.scheduleReadyToast(player, ACTIVE_DRUM_ID, entry.getReadyTick(), now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }

  private void applyDrumBuff(ServerPlayer player) {
    AttributeInstance attribute = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
    if (attribute != null) {
      AttributeModifier modifier =
          new AttributeModifier(
              KNOCKBACK_MODIFIER_ID,
              ACTIVE_DRUM_KNOCKBACK_RESIST,
              AttributeModifier.Operation.ADD_VALUE);
      AttributeOps.replaceTransient(attribute, KNOCKBACK_MODIFIER_ID, modifier);
      TickOps.schedule(
          player.serverLevel(), () -> removeDrumKnockback(player), ACTIVE_DRUM_DURATION_TICKS);
    }
    player
        .level()
        .playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.ARMOR_EQUIP_LEATHER,
            SoundSource.PLAYERS,
            0.8F,
            0.6F + player.getRandom().nextFloat() * 0.2F);
  }

  private void removeDrumKnockback(LivingEntity entity) {
    AttributeInstance attribute = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
    AttributeOps.removeById(attribute, KNOCKBACK_MODIFIER_ID);
  }

  public static void applyRollCounter(LivingEntity entity, int durationTicks, int amplifier) {
    entity.addEffect(
        new MobEffectInstance(
            MobEffects.DAMAGE_RESISTANCE,
            Math.max(1, durationTicks),
            Math.max(0, amplifier),
            false,
            true));
  }

  public static void applyRollSlow(ServerPlayer player, int durationTicks, int amplifier, double radius) {
    if (!(player.level() instanceof ServerLevel serverLevel)) {
      return;
    }
    List<LivingEntity> candidates =
        serverLevel.getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(radius),
            entity -> entity != player && entity.isAlive() && !entity.isSpectator());
    LivingEntity nearest = null;
    double nearestDist = Double.MAX_VALUE;
    for (LivingEntity candidate : candidates) {
      double distance = candidate.distanceToSqr(player);
      if (distance < nearestDist) {
        nearestDist = distance;
        nearest = candidate;
      }
    }
    if (nearest != null) {
      nearest.addEffect(
          new MobEffectInstance(
              MobEffects.MOVEMENT_SLOWDOWN,
              Math.max(1, durationTicks),
              Math.max(0, amplifier),
              false,
              true));
    }
  }

  public static void dealCrashDamage(Player player, Vec3 center, double amount, double radius) {
    if (!(player.level() instanceof ServerLevel serverLevel)) {
      return;
    }
    double clampedRadius = Math.max(0.5D, radius);
    AABB box = new AABB(center, center).inflate(clampedRadius, 1.0D, clampedRadius);
    List<LivingEntity> targets =
        serverLevel.getEntitiesOfClass(
            LivingEntity.class, box, entity -> entity != player && entity.isAlive());
    // 使用自定义真实伤害来源，避免护甲、护盾或防护类效果稀释冲撞惩罚。
    DamageSource source = CCDamageSources.shouPiGuCrash(player);
    for (LivingEntity target : targets) {
      target.hurt(source, (float) amount);
    }
    serverLevel.playSound(
        null,
        center.x,
        center.y,
        center.z,
        SoundEvents.ANVIL_PLACE,
        SoundSource.PLAYERS,
        0.6F,
        1.0F);
  }

  public static void applyStoicSlow(LivingEntity owner) {
    if (!(owner.level() instanceof ServerLevel serverLevel)) {
      return;
    }
    AABB box = owner.getBoundingBox().inflate(STOIC_SLOW_RADIUS);
    List<LivingEntity> targets =
        serverLevel.getEntitiesOfClass(
            LivingEntity.class, box, entity -> entity != owner && entity.isAlive());
    for (LivingEntity target : targets) {
      target.addEffect(
          new MobEffectInstance(
              MobEffects.MOVEMENT_SLOWDOWN, STOIC_SLOW_TICKS, STOIC_SLOW_AMPLIFIER));
    }
  }

  public static void applyShield(LivingEntity victim, double amount) {
    double updated = Math.max(0.0D, victim.getAbsorptionAmount() + amount);
    AbsorptionHelper.applyAbsorption(victim, updated, STOIC_ABSORBTION_ID, false);
  }

  private double attemptSoftReflect(
      DamageSource source,
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      TierParameters params,
      MultiCooldown cooldown,
      long now,
      double mitigated) {
    if (mitigated <= 0.0D) {
      return 0.0D;
    }
    Entity attackerEntity = source == null ? null : source.getEntity();
    if (!(attackerEntity instanceof LivingEntity attacker) || attacker == victim) {
      return 0.0D;
    }
    MultiCooldown.Entry baseEntry = cooldown.entry(KEY_SOFT_READY).withDefault(0L);
    boolean projectile = source.is(DamageTypeTags.IS_PROJECTILE);
    double percent = params.softPercent();
    boolean ready = baseEntry.isReady(now);
    boolean usedProjectileCooldown = false;
    if (projectile && params.projectilePercent() > 0.0D) {
      MultiCooldown.Entry projectileEntry =
          cooldown.entry(KEY_SOFT_PROJECTILE_READY).withDefault(0L);
      if (projectileEntry.isReady(now)) {
        percent = params.projectilePercent();
        ready = true;
        usedProjectileCooldown = true;
        projectileEntry.setReadyAt(now + SOFT_PROJECTILE_COOLDOWN_TICKS);
      }
    }
    if (!ready) {
      return 0.0D;
    }
    // 保持投射物命中与近战共用的互斥窗口：命中投射物时写入 0.6s，
    // 否则沿用按阶梯配置的 0.8s/0.7s 内置冷却。
    long baseReadyTick =
        usedProjectileCooldown
            ? Math.max(baseEntry.getReadyTick(), now + SOFT_PROJECTILE_COOLDOWN_TICKS)
            : now + params.cooldownTicks();
    baseEntry.setReadyAt(baseReadyTick);

    double bonus = state.getLong(KEY_ACTIVE_DRUM_EXPIRE, 0L) > now ? ACTIVE_DRUM_SOFT_BONUS : 0.0D;
    long tempBonusExpire = state.getLong(KEY_SOFT_TEMP_BONUS_EXPIRE, 0L);
    if (tempBonusExpire > now) {
      bonus += Math.max(0.0D, state.getDouble(KEY_SOFT_TEMP_BONUS, 0.0D));
    } else if (tempBonusExpire > 0L || state.getDouble(KEY_SOFT_TEMP_BONUS, 0.0D) > 0.0D) {
      state.setDouble(KEY_SOFT_TEMP_BONUS, 0.0D, value -> Math.max(0.0D, value), 0.0D);
      state.setLong(KEY_SOFT_TEMP_BONUS_EXPIRE, 0L, value -> Math.max(0L, value), 0L);
    }
    double reflected = mitigated * (percent + bonus);
    if (reflected <= 0.0D) {
      return 0.0D;
    }
    applyReflectDamage(victim, attacker, reflected, params, now);
    ReactionTagOps.add(attacker, ReactionTagKeys.THORNS_SOFT, 40);
    state.setBoolean(KEY_THICK_SKIN_READY, false, false);
    registerS1Mark(victim, attacker, now);
    return reflected;
  }

  private void applyReflectDamage(
      LivingEntity victim, LivingEntity attacker, double amount, TierParameters params, long now) {
    if (!(victim.level() instanceof ServerLevel serverLevel)) {
      return;
    }
    DamageSource thorns = victim.damageSources().thorns(victim);
    attacker.hurt(thorns, (float) amount);
    if (params.stage() == Tier.STAGE5) {
      AABB box = attacker.getBoundingBox().inflate(3.0D);
      List<LivingEntity> others =
          serverLevel.getEntitiesOfClass(
              LivingEntity.class, box, entity -> entity != attacker && entity != victim);
      for (LivingEntity other : others) {
        other.hurt(thorns, (float) (amount * 0.6D));
      }
    }
    serverLevel.playSound(
        null,
        victim.getX(),
        victim.getY(),
        victim.getZ(),
        SoundEvents.ARMOR_EQUIP_CHAIN,
        SoundSource.PLAYERS,
        0.8F,
        0.5F + victim.getRandom().nextFloat() * 0.2F);
  }

  private void registerS1Mark(LivingEntity owner, LivingEntity target, long now) {
    if (!(owner instanceof Player player)) {
      return;
    }
    // 将标记存储到目标实体的持久化数据中
    CompoundTag persistentData = target.getPersistentData();
    CompoundTag marks =
        persistentData.contains(MARK_NBT_KEY)
            ? persistentData.getCompound(MARK_NBT_KEY)
            : new CompoundTag();

    // 清理过期的标记
    List<String> keysToRemove = new ArrayList<>();
    for (String key : marks.getAllKeys()) {
      long expireTick = marks.getLong(key);
      if (expireTick <= now) {
        keysToRemove.add(key);
      }
    }
    keysToRemove.forEach(marks::remove);

    // 添加新标记
    marks.putLong(player.getUUID().toString(), now + S1_MARK_DURATION_TICKS);
    persistentData.put(MARK_NBT_KEY, marks);
  }

  public static void bumpSoftPool(
      OrganState state, ChestCavityInstance cc, ItemStack organ, double amount, long now) {
    double current = resolveSoftPool(state, now);
    double updated = current + amount;
    OrganStateOps.setDouble(
        state, cc, organ, KEY_SOFT_POOL_VALUE, updated, value -> Math.max(0.0D, value), 0.0D);
    OrganStateOps.setLong(
        state,
        cc,
        organ,
        KEY_SOFT_POOL_EXPIRE,
        now + SOFT_POOL_WINDOW_TICKS,
        value -> Math.max(0L, value),
        0L);
  }

  public static double resolveSoftPool(OrganState state, long now) {
    long expire = state.getLong(KEY_SOFT_POOL_EXPIRE, 0L);
    if (expire <= now) {
      return 0.0D;
    }
    return Math.max(0.0D, state.getDouble(KEY_SOFT_POOL_VALUE, 0.0D));
  }

  private void updateStoicProgress(
      OrganState state,
      ChestCavityInstance cc,
      ItemStack organ,
      TierParameters params,
      double finalDamage,
      long now) {
    if (params.stage().stage() < 2) {
      return;
    }
    if (state.getLong(KEY_STOIC_LOCK_UNTIL, 0L) > now) {
      return;
    }
    double accum = Math.max(0.0D, state.getDouble(KEY_STOIC_ACCUM, 0.0D) + finalDamage);
    int stacks = Math.max(0, state.getInt(KEY_STOIC_STACKS, 0));
    while (accum >= params.stoicThreshold() && stacks < STOIC_MAX_STACKS) {
      accum -= params.stoicThreshold();
      stacks++;
    }
    boolean ready = state.getBoolean(KEY_STOIC_READY, false);
    if (!ready && stacks >= STOIC_MAX_STACKS) {
      ready = true;
      stacks = STOIC_MAX_STACKS;
    }
    OrganStateOps.setDouble(
        state, cc, organ, KEY_STOIC_ACCUM, accum, value -> Math.max(0.0D, value), 0.0D);
    OrganStateOps.setInt(
        state,
        cc,
        organ,
        KEY_STOIC_STACKS,
        stacks,
        value -> Mth.clamp(value, 0, STOIC_MAX_STACKS),
        0);
    OrganStateOps.setBoolean(state, cc, organ, KEY_STOIC_READY, ready, false);
  }

  public static MultiCooldown cooldown(ChestCavityInstance cc, ItemStack organ, OrganState state) {
    MultiCooldown.Builder builder = MultiCooldown.builder(state).withSync(cc, organ);
    return builder.build();
  }

  public static void ensureStage(OrganState state, ChestCavityInstance cc, ItemStack organ) {
    int stage = state.getInt(KEY_STAGE, 0);
    if (stage <= 0) {
      OrganStateOps.setInt(state, cc, organ, KEY_STAGE, 1, value -> Mth.clamp(value, 1, 5), 1);
    }
  }

  public static TierParameters tierParameters(OrganState state) {
    int stage = Mth.clamp(state.getInt(KEY_STAGE, 1), 1, 5);
    return TIER_PARAMS.getOrDefault(Tier.fromStage(stage), TIER_PARAMS.get(Tier.STAGE1));
  }

  public static ItemStack findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (matchesOrgan(stack, ORGAN_ID)) {
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }

  public static boolean hasOrgan(ChestCavityInstance cc, ResourceLocation organId) {
    if (cc == null || cc.inventory == null) {
      return false;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      if (matchesOrgan(cc.inventory.getItem(i), organId)) {
        return true;
      }
    }
    return false;
  }

  public static boolean matchesOrgan(ItemStack stack, ResourceLocation organId) {
    return INSTANCE.matchesOrgan(stack, organId);
  }

  public static OrganState resolveState(ItemStack organ) {
    return INSTANCE.organState(organ, STATE_ROOT);
  }

  public enum Tier {
    STAGE1(1),
    STAGE2(2),
    STAGE3(3),
    STAGE4(4),
    STAGE5(5);

    private final int stage;

    Tier(int stage) {
      this.stage = stage;
    }

    public int stage() {
      return stage;
    }

    public static Tier fromStage(int stage) {
      return switch (stage) {
        case 1 -> STAGE1;
        case 2 -> STAGE2;
        case 3 -> STAGE3;
        case 4 -> STAGE4;
        default -> STAGE5;
      };
    }
  }

  public static record TierParameters(
      Tier stage,
      double softPercent,
      long cooldownTicks,
      double stoicMitigation,
      int stoicShield,
      double stoicThreshold,
      double projectilePercent,
      long lockTicks) {}
}
