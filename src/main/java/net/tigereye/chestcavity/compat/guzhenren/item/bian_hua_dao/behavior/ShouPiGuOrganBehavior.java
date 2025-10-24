package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
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
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TeleportOps;
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
import org.slf4j.Logger;

/**
 * 兽皮蛊（变化道核心防具）。
 *
 * <p>实现要点：
 *
 * <ul>
 *   <li>软反伤：把本器官造成的减免量按百分比回溅给攻击者，并在 5 阶时溅射周围目标。
 *   <li>皮厚 / 筋膜收束：短时额外减伤与护盾，追踪每次受击并处理冷却。
 *   <li>坚忍计：累计净承伤叠层，满层后触发一次额外减伤与护盾并进入锁定。
 *   <li>主动技：硬皮鼓动（1 阶）、翻滚脱力（2 阶），以及与虎皮蛊/铁骨蛊联动的嵌甲冲撞。
 *   <li>联动：软反伤命中后在 3 秒窗口内为目标挂载“皮虎同纹”标记。
 * </ul>
 */
public final class ShouPiGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganIncomingDamageListener, OrganSlowTickListener, OrganOnHitListener {

  public static final ShouPiGuOrganBehavior INSTANCE = new ShouPiGuOrganBehavior();

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final String MOD_ID = "guzhenren";

  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "shou_pi_gu");
  private static final ResourceLocation HUPI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "hupigu");
  private static final ResourceLocation TIE_GU_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "tie_gu_gu");

  private static final ResourceLocation ACTIVE_DRUM_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "skill/shou_pi_gu_drum");
  private static final ResourceLocation ACTIVE_ROLL_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "skill/shou_pi_gu_roll");
  private static final ResourceLocation SYNERGY_CRASH_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "synergy/qian_jia_chong_zhuang");

  private static final ResourceLocation KNOCKBACK_MODIFIER_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/shou_pi_gu_knockback");
  private static final ResourceLocation STOIC_ABSORBTION_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/shou_pi_gu_stoic");

  private static final double THICK_SKIN_REDUCTION = 0.08D;
  private static final int THICK_SKIN_WINDOW_TICKS = 20;
  private static final int FASCIA_TRIGGER = 5;
  private static final long FASCIA_COOLDOWN_TICKS = 200L; // 10s
  private static final int STOIC_MAX_STACKS = 6;
  private static final long STOIC_DEFAULT_LOCK_TICKS = 8 * 20L;
  private static final long SOFT_POOL_WINDOW_TICKS = 5 * 20L;
  private static final long S1_MARK_DURATION_TICKS = 3 * 20L;
  private static final long ROLL_DAMAGE_WINDOW_TICKS = 12L; // 0.6s
  private static final double ROLL_DAMAGE_REDUCTION = 0.6D;
  private static final double ROLL_DISTANCE = 3.0D;
  private static final double CRASH_DISTANCE = 4.0D;
  private static final long CRASH_IMMUNE_TICKS = 10L;
  private static final double CRASH_SPLASH_RADIUS = 1.5D;
  private static final double STOIC_SLOW_RADIUS = 3.0D;
  private static final int STOIC_SLOW_TICKS = 40;
  private static final int STOIC_SLOW_AMPLIFIER = 0;
  private static final long SOFT_PROJECTILE_COOLDOWN_TICKS = 12L; // 0.6s shared thorns window

  private static final double ACTIVE_DRUM_DEFENSE_BONUS = 0.06D;
  private static final double ACTIVE_DRUM_SOFT_BONUS = 0.10D;
  private static final int ACTIVE_DRUM_DURATION_TICKS = 5 * 20;
  private static final long ACTIVE_DRUM_COOLDOWN_TICKS = 20 * 20L;
  private static final double ACTIVE_DRUM_KNOCKBACK_RESIST = 0.5D;
  private static final double ACTIVE_DRUM_BASE_COST = 40.0D;

  private static final double ACTIVE_ROLL_BASE_COST = 25.0D;
  private static final long ACTIVE_ROLL_COOLDOWN_TICKS = 14 * 20L;

  private static final double SYNERGY_CRASH_BASE_COST = 60.0D;
  private static final long SYNERGY_CRASH_COOLDOWN_TICKS = 18 * 20L;

  private static final String STATE_ROOT = "ShouPiGu";
  private static final String KEY_STAGE = "Stage";
  private static final String KEY_SOFT_READY = "SoftThornsReady";
  private static final String KEY_SOFT_PROJECTILE_READY = "SoftThornsProjectileReady";
  private static final String KEY_LAST_HIT = "LastHitTick";
  private static final String KEY_THICK_SKIN_EXPIRE = "ThickSkinExpire";
  private static final String KEY_THICK_SKIN_READY = "ThickSkinReady";
  private static final String KEY_FASCIA_COOLDOWN = "FasciaCooldown";
  private static final String KEY_FASCIA_COUNT = "FasciaHitCount";
  private static final String KEY_STOIC_ACCUM = "StoicAccum";
  private static final String KEY_STOIC_STACKS = "StoicStacks";
  private static final String KEY_STOIC_READY = "StoicReady";
  private static final String KEY_STOIC_LOCK_UNTIL = "StoicLockUntil";
  private static final String KEY_SOFT_POOL_VALUE = "SoftReflectPool";
  private static final String KEY_SOFT_POOL_EXPIRE = "SoftReflectExpire";
  private static final String KEY_ACTIVE_DRUM_EXPIRE = "DrumExpire";
  private static final String KEY_ACTIVE_DRUM_READY = "DrumReady";
  private static final String KEY_ROLL_READY = "RollReady";
  private static final String KEY_ROLL_EXPIRE = "RollExpire";
  private static final String KEY_CRASH_READY = "CrashReady";
  private static final String KEY_CRASH_IMMUNE = "CrashImmuneExpire";

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
    OrganActivationListeners.register(
        ACTIVE_ROLL_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateRoll(player, cc);
          }
        });
    OrganActivationListeners.register(
        SYNERGY_CRASH_ID,
        (entity, cc) -> {
          if (entity instanceof ServerPlayer player) {
            INSTANCE.activateCrash(player, cc);
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

    long rollExpire = state.getLong(KEY_ROLL_EXPIRE, 0L);
    if (rollExpire > 0L && now >= rollExpire) {
      state.setLong(KEY_ROLL_EXPIRE, 0L, value -> Math.max(0L, value), 0L);
      NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    long immuneExpire = state.getLong(KEY_CRASH_IMMUNE, 0L);
    if (immuneExpire > 0L && now >= immuneExpire) {
      state.setLong(KEY_CRASH_IMMUNE, 0L, value -> Math.max(0L, value), 0L);
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
    collector.record(state.setInt(KEY_FASCIA_COUNT, fasciaCount, value -> Math.max(0, value), 0));

    if (fasciaCount >= FASCIA_TRIGGER) {
      long fasciaReady = state.getLong(KEY_FASCIA_COOLDOWN, 0L);
      if (now >= fasciaReady) {
        double before = workingDamage;
        workingDamage *= 0.88D;
        mitigatedFromOrgan += Math.max(0.0D, before - workingDamage);
        applyShield(victim, 2.0F);
        collector.record(
            state.setLong(
                KEY_FASCIA_COOLDOWN,
                now + FASCIA_COOLDOWN_TICKS,
                value -> Math.max(0L, value),
                0L));
        collector.record(state.setInt(KEY_FASCIA_COUNT, 0, value -> Math.max(0, value), 0));
      }
    }

    boolean stoicReady = state.getBoolean(KEY_STOIC_READY, false);
    long stoicLockUntil = state.getLong(KEY_STOIC_LOCK_UNTIL, 0L);
    if (stoicReady && now >= stoicLockUntil) {
      double beforeStoic = workingDamage;
      workingDamage *= 1.0D - params.stoicMitigation();
      mitigatedFromOrgan += Math.max(0.0D, beforeStoic - workingDamage);
      applyShield(victim, params.stoicShield());
      if (params.stage() == Tier.STAGE5) {
        applyStoicSlow(victim);
      }
      collector.record(state.setBoolean(KEY_STOIC_READY, false, false));
      collector.record(state.setInt(KEY_STOIC_STACKS, 0, value -> Math.max(0, value), 0));
      collector.record(
          state.setDouble(KEY_STOIC_ACCUM, 0.0D, value -> Math.max(0.0D, value), 0.0D));
      collector.record(
          state.setLong(
              KEY_STOIC_LOCK_UNTIL, now + params.lockTicks(), value -> Math.max(0L, value), 0L));
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

  private void activateRoll(ServerPlayer player, ChestCavityInstance cc) {
    if (player == null || cc == null) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    OrganState state = organState(organ, STATE_ROOT);
    int stage = Math.max(1, state.getInt(KEY_STAGE, 1));
    if (stage < 2) {
      return;
    }
    MultiCooldown cooldown = cooldown(cc, organ, state);
    long now = player.level().getGameTime();
    MultiCooldown.Entry entry = cooldown.entry(KEY_ROLL_READY).withDefault(0L);
    if (!entry.isReady(now)) {
      return;
    }
    OptionalDouble consumed = ResourceOps.tryConsumeScaledZhenyuan(player, ACTIVE_ROLL_BASE_COST);
    if (consumed.isEmpty()) {
      return;
    }
    Vec3 look = player.getLookAngle();
    Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
    if (horizontal.lengthSqr() < 1.0E-4D) {
      horizontal = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
    }
    Vec3 offset = horizontal.normalize().scale(ROLL_DISTANCE);
    TeleportOps.blinkOffset(player, offset);
    state.setLong(
        KEY_ROLL_EXPIRE, now + ROLL_DAMAGE_WINDOW_TICKS, value -> Math.max(0L, value), 0L);
    entry.setReadyAt(now + ACTIVE_ROLL_COOLDOWN_TICKS);
    applyRollCounter(player);
    applyRollSlow(player);
    ActiveSkillRegistry.scheduleReadyToast(player, ACTIVE_ROLL_ID, entry.getReadyTick(), now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }

  private void activateCrash(ServerPlayer player, ChestCavityInstance cc) {
    if (player == null || cc == null) {
      return;
    }
    if (!hasOrgan(cc, HUPI_GU_ID) || !hasOrgan(cc, TIE_GU_GU_ID)) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    OrganState state = organState(organ, STATE_ROOT);
    MultiCooldown cooldown = cooldown(cc, organ, state);
    long now = player.level().getGameTime();
    MultiCooldown.Entry entry = cooldown.entry(KEY_CRASH_READY).withDefault(0L);
    if (!entry.isReady(now)) {
      return;
    }
    OptionalDouble consumed = ResourceOps.tryConsumeScaledZhenyuan(player, SYNERGY_CRASH_BASE_COST);
    if (consumed.isEmpty()) {
      return;
    }
    Vec3 look = player.getLookAngle();
    Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
    if (horizontal.lengthSqr() < 1.0E-4D) {
      horizontal = new Vec3(1.0D, 0.0D, 0.0D);
    }
    Vec3 offset = horizontal.normalize().scale(CRASH_DISTANCE);
    Optional<Vec3> destination = TeleportOps.blinkOffset(player, offset);
    Vec3 center = destination.orElse(player.position());

    double pool = resolveSoftPool(state, now);
    double attackValue =
        player.getAttribute(Attributes.ATTACK_DAMAGE) == null
            ? 0.0D
            : player.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
    double capped = Math.min(pool * 0.35D, 8.0D + attackValue * 0.6D);
    if (capped > 0.0D) {
      dealCrashDamage(player, center, capped);
      state.setDouble(KEY_SOFT_POOL_VALUE, 0.0D, value -> Math.max(0.0D, value), 0.0D);
      state.setLong(KEY_SOFT_POOL_EXPIRE, 0L, value -> Math.max(0L, value), 0L);
    }
    state.setLong(KEY_CRASH_IMMUNE, now + CRASH_IMMUNE_TICKS, value -> Math.max(0L, value), 0L);
    entry.setReadyAt(now + SYNERGY_CRASH_COOLDOWN_TICKS);
    ActiveSkillRegistry.scheduleReadyToast(player, SYNERGY_CRASH_ID, entry.getReadyTick(), now);
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

  private void applyRollCounter(LivingEntity entity) {
    entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 15, 0, false, true));
  }

  private void applyRollSlow(ServerPlayer player) {
    if (!(player.level() instanceof ServerLevel serverLevel)) {
      return;
    }
    double radius = 3.0D;
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
      nearest.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 0, false, true));
    }
  }

  private void dealCrashDamage(Player player, Vec3 center, double amount) {
    if (!(player.level() instanceof ServerLevel serverLevel)) {
      return;
    }
    AABB box = new AABB(center, center).inflate(CRASH_SPLASH_RADIUS, 1.0D, CRASH_SPLASH_RADIUS);
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

  private void applyStoicSlow(LivingEntity owner) {
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

  private void applyShield(LivingEntity victim, double amount) {
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

  private void bumpSoftPool(
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

  private double resolveSoftPool(OrganState state, long now) {
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

  private MultiCooldown cooldown(ChestCavityInstance cc, ItemStack organ, OrganState state) {
    MultiCooldown.Builder builder = MultiCooldown.builder(state).withSync(cc, organ);
    return builder.build();
  }

  private void ensureStage(OrganState state, ChestCavityInstance cc, ItemStack organ) {
    int stage = state.getInt(KEY_STAGE, 0);
    if (stage <= 0) {
      OrganStateOps.setInt(state, cc, organ, KEY_STAGE, 1, value -> Mth.clamp(value, 1, 5), 1);
    }
  }

  private TierParameters tierParameters(OrganState state) {
    int stage = Mth.clamp(state.getInt(KEY_STAGE, 1), 1, 5);
    return TIER_PARAMS.getOrDefault(Tier.fromStage(stage), TIER_PARAMS.get(Tier.STAGE1));
  }

  private ItemStack findOrgan(ChestCavityInstance cc) {
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

  private boolean hasOrgan(ChestCavityInstance cc, ResourceLocation organId) {
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

  private enum Tier {
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

  private record TierParameters(
      Tier stage,
      double softPercent,
      long cooldownTicks,
      double stoicMitigation,
      int stoicShield,
      double stoicThreshold,
      double projectilePercent,
      long lockTicks) {}
}
