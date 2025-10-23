package net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.AbstractLiDaoOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.util.CombatEntityUtil;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown.Entry;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.NetworkUtil;
import org.slf4j.Logger;

/**
 * 蛮力天牛蛊（力道·肌肉）。
 *
 * <p>要点：
 *
 * <ul>
 *   <li>主动A「蛮牛激发」：消耗真元后获得 10 秒强化窗口，近战命中时尝试额外扣除精力换取真实近战伤害。
 *   <li>主动B「横冲直撞」：短距离冲锋并在首次命中时造成额外伤害、击退与微型震荡波。
 *   <li>被动「槽角蓄力」「蹄踏加乘」：站定蓄力与连续冲刺分别赋予首击增幅与疾跑追击奖励。
 *   <li>基础成长计数（重击/冲刺里程/击退/精英击杀）记录于 {@link OrganState}，后续用于阶段奖励。
 * </ul>
 */
public final class ManLiTianNiuGuOrganBehavior extends AbstractLiDaoOrganBehavior
    implements OrganSlowTickListener, OrganOnHitListener {

  public static final ManLiTianNiuGuOrganBehavior INSTANCE = new ManLiTianNiuGuOrganBehavior();

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "man_li_tian_niu_gu");
  public static final ResourceLocation BOOST_ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "man_li_tian_niu_gu/boost");
  public static final ResourceLocation RUSH_ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "man_li_tian_niu_gu/rush");

  private static final ResourceLocation JING_LI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jing_li_gu");
  private static final ResourceLocation QUAN_LI_YI_FU_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "quan_li_yi_fu_gu");

  private static final String STATE_ROOT = "ManLiTianNiuGu";

  private static final String KEY_STAGE = "Stage";
  private static final String KEY_HIT_PROGRESS = "ProgressHit";
  private static final String KEY_RUN_PROGRESS = "ProgressRun";
  private static final String KEY_KB_PROGRESS = "ProgressKnockback";
  private static final String KEY_ELITE_PROGRESS = "ProgressElite";

  private static final String KEY_BOOST_READY_AT = "BoostReadyAt";
  private static final String KEY_BOOST_ACTIVE_UNTIL = "BoostActiveUntil";
  private static final String KEY_BOOST_LAUNCH_SPENT = "BoostLaunchSpent";
  private static final String KEY_BOOST_COMBO_COUNT = "BoostComboCount";
  private static final String KEY_BOOST_COMBO_READY_AT = "BoostComboReadyAt";

  private static final String KEY_RUSH_READY_AT = "RushReadyAt";
  private static final String KEY_RUSH_ACTIVE_UNTIL = "RushActiveUntil";
  private static final String KEY_RUSH_SEQUENCE = "RushSequence";
  private static final String KEY_RUSH_HIT_OCCURRED = "RushHitOccurred";
  private static final String KEY_RUSH_DIR_X = "RushDirX";
  private static final String KEY_RUSH_DIR_Z = "RushDirZ";

  private static final String KEY_LAST_POS_X = "LastPosX";
  private static final String KEY_LAST_POS_Y = "LastPosY";
  private static final String KEY_LAST_POS_Z = "LastPosZ";
  private static final String KEY_LAST_MOVE_TICK = "LastMoveTick";
  private static final String KEY_STATIONARY_READY = "StationaryReady";
  private static final String KEY_STATIONARY_COOLDOWN = "StationaryCooldown";
  private static final String KEY_STATIONARY_PRIME_AT = "StationaryPrimeAt";

  private static final String KEY_SPRINT_ACCUM = "SprintAccum";
  private static final String KEY_SPRINT_READY = "SprintReady";
  private static final String KEY_SPRINT_EXPIRE = "SprintExpire";

  private static final String KEY_RUN_ACCUM = "RunAccum";

  private static final String KEY_LAST_ELITE_KILL = "LastEliteKill";
  private static final String KEY_LAST_BOSS_KILL = "LastBossKill";

  private static final String KEY_TRIAL_ACTIVE_UNTIL = "TrialActiveUntil";
  private static final String KEY_TRIAL_RUN_PROGRESS = "TrialRunProgress";
  private static final String KEY_TRIAL_RUN_ACCUM = "TrialRunAccum";
  private static final String KEY_TRIAL_ELITE_PROGRESS = "TrialEliteProgress";
  private static final String KEY_TRIAL_COMPLETED = "TrialCompleted";

  private static final double POSITION_EPSILON = 0.003D;
  private static final int TICKS_PER_SECOND = 20;
  private static final double HIT_DEDUP_SECONDS = 5.0D;
  private static final double RUN_SAMPLE_THRESHOLD = 0.12D;

  private static final double BOOST_BASE_ZHENYUAN_COST =
      BehaviorConfigAccess.getFloat(
          ManLiTianNiuGuOrganBehavior.class, "BOOST_BASE_ZHENYUAN_COST", 200.0F);
  private static final double BOOST_HIT_JINGLI_COST =
      BehaviorConfigAccess.getFloat(
          ManLiTianNiuGuOrganBehavior.class, "BOOST_HIT_JINGLI_COST", 6.0F);
  private static final float BOOST_EXTRA_DAMAGE =
      BehaviorConfigAccess.getFloat(ManLiTianNiuGuOrganBehavior.class, "BOOST_EXTRA_DAMAGE", 3.0F);
  private static final int BOOST_BASE_DURATION_TICKS =
      BehaviorConfigAccess.getInt(
          ManLiTianNiuGuOrganBehavior.class, "BOOST_BASE_DURATION_TICKS", 10 * TICKS_PER_SECOND);
  private static final int BOOST_STAGE_TWO_BONUS_TICKS =
      BehaviorConfigAccess.getInt(
          ManLiTianNiuGuOrganBehavior.class, "BOOST_STAGE_TWO_BONUS_TICKS", 2 * TICKS_PER_SECOND);
  private static final int BOOST_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(
          ManLiTianNiuGuOrganBehavior.class, "BOOST_COOLDOWN_TICKS", 25 * TICKS_PER_SECOND);
  private static final double BOOST_LAUNCH_VELOCITY =
      BehaviorConfigAccess.getFloat(
          ManLiTianNiuGuOrganBehavior.class, "BOOST_LAUNCH_VELOCITY", 0.35F);

  private static final double RUSH_BASE_ZHENYUAN_COST =
      BehaviorConfigAccess.getFloat(
          ManLiTianNiuGuOrganBehavior.class, "RUSH_BASE_ZHENYUAN_COST", 80.0F);
  private static final double RUSH_BASE_JINGLI_COST =
      BehaviorConfigAccess.getFloat(
          ManLiTianNiuGuOrganBehavior.class, "RUSH_BASE_JINGLI_COST", 10.0F);
  private static final float RUSH_EXTRA_DAMAGE =
      BehaviorConfigAccess.getFloat(ManLiTianNiuGuOrganBehavior.class, "RUSH_EXTRA_DAMAGE", 2.0F);
  private static final double RUSH_KNOCKBACK =
      BehaviorConfigAccess.getFloat(ManLiTianNiuGuOrganBehavior.class, "RUSH_KNOCKBACK", 1.25F);
  private static final double RUSH_AOE_RADIUS =
      BehaviorConfigAccess.getFloat(ManLiTianNiuGuOrganBehavior.class, "RUSH_AOE_RADIUS", 1.5F);
  private static final int RUSH_AOE_STUN_TICKS =
      BehaviorConfigAccess.getInt(ManLiTianNiuGuOrganBehavior.class, "RUSH_AOE_STUN_TICKS", 4);
  private static final int RUSH_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(
          ManLiTianNiuGuOrganBehavior.class, "RUSH_COOLDOWN_TICKS", 12 * TICKS_PER_SECOND);
  private static final double RUSH_HORIZONTAL_SPEED =
      BehaviorConfigAccess.getFloat(
          ManLiTianNiuGuOrganBehavior.class, "RUSH_HORIZONTAL_SPEED", 0.85F);
  private static final int RUSH_DURATION_TICKS =
      BehaviorConfigAccess.getInt(ManLiTianNiuGuOrganBehavior.class, "RUSH_DURATION_TICKS", 8);

  private static final float PASSIVE1_DAMAGE_BONUS = 0.10F;
  private static final double PASSIVE1_KNOCKBACK = 0.5D;
  private static final int PASSIVE1_STATIONARY_TICKS = Mth.ceil(1.25F * TICKS_PER_SECOND);
  private static final int PASSIVE1_COOLDOWN_TICKS = 5 * TICKS_PER_SECOND;

  private static final float PASSIVE2_BASE_DAMAGE = 2.0F;
  private static final float PASSIVE2_STAGE4_BONUS = 3.0F;
  private static final int PASSIVE2_REQUIRED_TICKS = 3 * TICKS_PER_SECOND;
  private static final int PASSIVE2_WINDOW_TICKS = 6 * TICKS_PER_SECOND;
  private static final int PASSIVE2_SLOW_TICKS = Mth.ceil(0.4F * TICKS_PER_SECOND);
  private static final int PASSIVE2_SLOW_LEVEL = 0;

  private static final int STAGE_ONE = 1;
  private static final int STAGE_TWO = 2;
  private static final int STAGE_THREE = 3;
  private static final int STAGE_FOUR = 4;
  private static final int STAGE_FIVE = 5;

  private static final int STAGE_TWO_HIT_THRESHOLD = 120;
  private static final int STAGE_TWO_RUN_THRESHOLD = 800;
  private static final int STAGE_THREE_KB_THRESHOLD = 60;
  private static final int STAGE_THREE_RUN_THRESHOLD = 2000;
  private static final int STAGE_FOUR_ELITE_THRESHOLD = 10;
  private static final int STAGE_FOUR_HIT_THRESHOLD = 400;

  private static final int ELITE_KILL_COOLDOWN_TICKS = 30 * TICKS_PER_SECOND;
  private static final int ELITE_KILL_VALUE = 3;
  private static final int BOSS_KILL_VALUE = 8;

  private static final int TRIAL_DURATION_TICKS = 10 * 60 * TICKS_PER_SECOND;
  private static final double TRIAL_RUN_TARGET = 1200.0D;
  private static final int TRIAL_ELITE_TARGET = 1;

  private static final int BOOST_COMBO_HIT_REQUIREMENT = 4;
  private static final int BOOST_COMBO_COOLDOWN_TICKS = 2 * TICKS_PER_SECOND;
  private static final float BOOST_COMBO_BONUS_DAMAGE = 4.0F;

  private static final Object2ObjectMap<UUID, Object2LongMap<UUID>> HIT_DEDUP =
      new Object2ObjectOpenHashMap<>();
  private static final Map<UUID, RushSession> ACTIVE_RUSHES = new ConcurrentHashMap<>();

  static {
    OrganActivationListeners.register(BOOST_ABILITY_ID, ManLiTianNiuGuOrganBehavior::activateBoost);
    OrganActivationListeners.register(
        ManLiTianNiuGuOrganBehavior.RUSH_ABILITY_ID, ManLiTianNiuGuOrganBehavior::activateRush);
  }

  private ManLiTianNiuGuOrganBehavior() {}

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof Player player)
        || entity.level().isClientSide()
        || cc == null
        || organ == null
        || organ.isEmpty()) {
      return;
    }
    if (!matchesOrgan(organ, ORGAN_ID) || !isPrimaryOrgan(cc, organ)) {
      return;
    }

    Level level = player.level();
    if (!(level instanceof ServerLevel serverLevel)) {
      return;
    }

    long now = serverLevel.getGameTime();
    OrganState state = organState(organ, STATE_ROOT);
    MultiCooldown cooldown = createCooldown(cc, organ, state);

    tickBoostState(player, state, cooldown, now);
    tickRushState(player, state, cooldown, now);
    sampleMovement(player, state, now);
    sampleSprint(player, state, now);
    decaySprintWindow(player, state, now);
    tickTrialWindow(player, state, now);
    updateStageIfNeeded(player, cc, organ, state, now);
  }

  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
      return damage;
    }
    if (!matchesOrgan(organ, ORGAN_ID) || !isPrimaryOrgan(cc, organ)) {
      return damage;
    }
    if (damage <= 0.0F || target == null || !target.isAlive()) {
      return damage;
    }
    if (!CombatEntityUtil.isMeleeHit(source) || !CombatEntityUtil.areEnemies(attacker, target)) {
      return damage;
    }

    OrganState state = organState(organ, STATE_ROOT);
    MultiCooldown cooldown = createCooldown(cc, organ, state);
    long now = attacker.level().getGameTime();

    float result = damage;
    result = applyBoostStrike(player, target, state, cc, organ, result, now);
    result = applyStationaryPrime(player, target, state, cooldown, organ, result, now);
    result = applySprintBonus(player, target, state, organ, result, now);
    result = applyRushImpact(player, target, state, cooldown, organ, result, now);

    recordHitProgress(player, target, state, now, result);
    return result;
  }

  private void tickBoostState(Player player, OrganState state, MultiCooldown cooldown, long now) {
    long activeUntil = Math.max(0L, state.getLong(KEY_BOOST_ACTIVE_UNTIL, 0L));
    if (activeUntil > 0L && activeUntil <= now) {
      state.setLong(KEY_BOOST_ACTIVE_UNTIL, 0L, value -> Math.max(0L, value), 0L);
      state.setBoolean(KEY_BOOST_LAUNCH_SPENT, false, false);
      state.setInt(KEY_BOOST_COMBO_COUNT, 0, value -> Math.max(0, value), 0);
      state.setLong(KEY_BOOST_COMBO_READY_AT, 0L, value -> Math.max(0L, value), 0L);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("蛮力天牛蛊强筋结束: {}", player.getName().getString());
      }
    }

    Entry readyEntry = cooldown.entry(KEY_BOOST_READY_AT).withDefault(0L);
    if (!readyEntry.isReady(now) && readyEntry.getReadyTick() > 0L) {
      long remaining = readyEntry.getReadyTick() - now;
      if (remaining <= 0L && player instanceof ServerPlayer serverPlayer) {
        ActiveSkillRegistry.scheduleReadyToast(serverPlayer, BOOST_ABILITY_ID, now, now);
        readyEntry.setReadyAt(now);
      }
    }
  }

  private void tickRushState(Player player, OrganState state, MultiCooldown cooldown, long now) {
    long rushUntil = Math.max(0L, state.getLong(KEY_RUSH_ACTIVE_UNTIL, 0L));
    if (rushUntil > 0L && rushUntil <= now) {
      finishRush(player, state, true);
    }

    Entry readyEntry = cooldown.entry(KEY_RUSH_READY_AT).withDefault(0L);
    if (!readyEntry.isReady(now) && readyEntry.getReadyTick() > 0L) {
      long remaining = readyEntry.getReadyTick() - now;
      if (remaining <= 0L && player instanceof ServerPlayer serverPlayer) {
        ActiveSkillRegistry.scheduleReadyToast(serverPlayer, RUSH_ABILITY_ID, now, now);
        readyEntry.setReadyAt(now);
      }
    }
  }

  private void sampleMovement(Player player, OrganState state, long now) {
    double x = player.getX();
    double y = player.getY();
    double z = player.getZ();
    double lastX = state.getDouble(KEY_LAST_POS_X, x);
    double lastY = state.getDouble(KEY_LAST_POS_Y, y);
    double lastZ = state.getDouble(KEY_LAST_POS_Z, z);
    double deltaSq = Mth.lengthSquared(x - lastX, y - lastY, z - lastZ);

    state.setDouble(KEY_LAST_POS_X, x, value -> value, x);
    state.setDouble(KEY_LAST_POS_Y, y, value -> value, y);
    state.setDouble(KEY_LAST_POS_Z, z, value -> value, z);

    boolean moving =
        deltaSq > POSITION_EPSILON
            || player.isSprinting()
            || player.isSwimming()
            || player.isPassenger()
            || player.isFallFlying();

    long lastMoveTick = Math.max(0L, state.getLong(KEY_LAST_MOVE_TICK, now));
    if (moving) {
      state.setLong(KEY_LAST_MOVE_TICK, now, value -> Math.max(0L, value), now);
      state.setBoolean(KEY_STATIONARY_READY, false, false);
      state.setLong(KEY_STATIONARY_PRIME_AT, 0L, value -> Math.max(0L, value), 0L);
    } else {
      long cooldownUntil = Math.max(0L, state.getLong(KEY_STATIONARY_COOLDOWN, 0L));
      long primeAt = Math.max(0L, state.getLong(KEY_STATIONARY_PRIME_AT, 0L));
      if (primeAt == 0L) {
        primeAt = now + Math.max(PASSIVE1_STATIONARY_TICKS - TICKS_PER_SECOND, 0);
        state.setLong(KEY_STATIONARY_PRIME_AT, primeAt, value -> Math.max(0L, value), primeAt);
      }
      if (cooldownUntil <= now && now - lastMoveTick >= PASSIVE1_STATIONARY_TICKS) {
        state.setBoolean(KEY_STATIONARY_READY, true, false);
      }
    }

    if (player.isSprinting()) {
      double horizontal = Math.sqrt((x - lastX) * (x - lastX) + (z - lastZ) * (z - lastZ));
      if (horizontal >= RUN_SAMPLE_THRESHOLD) {
        double accum = state.getDouble(KEY_RUN_ACCUM, 0.0D) + horizontal;
        state.setDouble(KEY_RUN_ACCUM, accum, value -> Math.max(0.0D, value), accum);
        int total = state.getInt(KEY_RUN_PROGRESS, 0);
        int extra = (int) Math.floor(accum);
        if (extra > 0) {
          state.setInt(KEY_RUN_PROGRESS, total + extra, value -> Math.max(0, value), 0);
          state.setDouble(KEY_RUN_ACCUM, accum - extra, value -> Math.max(0.0D, value), 0.0D);
        }
        accumulateTrialRunDistance(player, state, now, horizontal);
      }
    }
  }

  private void sampleSprint(Player player, OrganState state, long now) {
    double accum = state.getDouble(KEY_SPRINT_ACCUM, 0.0D);
    if (player.isSprinting() || player.isSwimming()) {
      accum += TICKS_PER_SECOND;
      accum = Math.min(accum, PASSIVE2_REQUIRED_TICKS);
      state.setDouble(KEY_SPRINT_ACCUM, accum, value -> Math.max(0.0D, value), accum);
      if (accum >= PASSIVE2_REQUIRED_TICKS) {
        state.setBoolean(KEY_SPRINT_READY, true, false);
        state.setLong(
            KEY_SPRINT_EXPIRE,
            now + PASSIVE2_WINDOW_TICKS,
            value -> Math.max(0L, value),
            now + PASSIVE2_WINDOW_TICKS);
      }
    } else {
      if (accum > 0.0D) {
        accum = Math.max(0.0D, accum - TICKS_PER_SECOND);
        state.setDouble(KEY_SPRINT_ACCUM, accum, value -> Math.max(0.0D, value), accum);
      }
    }
  }

  private void decaySprintWindow(Player player, OrganState state, long now) {
    long expire = Math.max(0L, state.getLong(KEY_SPRINT_EXPIRE, 0L));
    if (expire > 0L && expire <= now) {
      state.setBoolean(KEY_SPRINT_READY, false, false);
      state.setLong(KEY_SPRINT_EXPIRE, 0L, value -> Math.max(0L, value), 0L);
      state.setDouble(KEY_SPRINT_ACCUM, 0.0D, value -> Math.max(0.0D, value), 0.0D);
    }
  }

  /** 在四转阶段维持猛牛试炼计时，超时后提醒玩家并立即重启下一轮。 */
  private void tickTrialWindow(Player player, OrganState state, long now) {
    if (state == null) {
      return;
    }
    int stage = Math.max(STAGE_ONE, Math.min(STAGE_FIVE, state.getInt(KEY_STAGE, STAGE_ONE)));
    if (stage < STAGE_FOUR || state.getBoolean(KEY_TRIAL_COMPLETED, false)) {
      if (stage >= STAGE_FIVE) {
        state.setLong(KEY_TRIAL_ACTIVE_UNTIL, 0L, value -> Math.max(0L, value), 0L);
      }
      return;
    }

    long activeUntil = Math.max(0L, state.getLong(KEY_TRIAL_ACTIVE_UNTIL, 0L));
    if (activeUntil == 0L) {
      startTrial(player, state, now, true);
      return;
    }

    if (now >= activeUntil) {
      notifyTrialRestart(player);
      startTrial(player, state, now, true);
    }
  }

  /** 按 RUN 统计规则累计试炼里程，仅在试炼计时有效时采样。 */
  private void accumulateTrialRunDistance(
      Player player, OrganState state, long now, double distance) {
    if (!isTrialActive(state, now) || distance <= 0.0D) {
      return;
    }

    double accum = Math.max(0.0D, state.getDouble(KEY_TRIAL_RUN_ACCUM, 0.0D)) + distance;
    double progress = Math.max(0.0D, state.getDouble(KEY_TRIAL_RUN_PROGRESS, 0.0D));
    int whole = (int) Math.floor(accum);
    if (whole > 0) {
      progress += whole;
      accum -= whole;
      state.setDouble(KEY_TRIAL_RUN_PROGRESS, progress, value -> Math.max(0.0D, value), progress);
      checkTrialCompletion(player, state, now);
    }
    state.setDouble(KEY_TRIAL_RUN_ACCUM, accum, value -> Math.max(0.0D, value), accum);
  }

  /** 检查试炼条件（里程与击杀）是否满足，成功后提示玩家并终止计时。 */
  private void checkTrialCompletion(Player player, OrganState state, long now) {
    if (state.getBoolean(KEY_TRIAL_COMPLETED, false)) {
      return;
    }
    double run = Math.max(0.0D, state.getDouble(KEY_TRIAL_RUN_PROGRESS, 0.0D));
    int kills = Math.max(0, state.getInt(KEY_TRIAL_ELITE_PROGRESS, 0));
    if (run < TRIAL_RUN_TARGET || kills < TRIAL_ELITE_TARGET) {
      return;
    }

    state.setBoolean(KEY_TRIAL_COMPLETED, true, false);
    state.setLong(KEY_TRIAL_ACTIVE_UNTIL, 0L, value -> Math.max(0L, value), 0L);
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.displayClientMessage(Component.literal("猛牛试炼完成，强筋·终觉醒！"), true);
    }
  }

  /** 初始化或重启猛牛试炼窗口，清空累计数据并可选播报提示。 */
  private void startTrial(Player player, OrganState state, long now, boolean announce) {
    if (state.getBoolean(KEY_TRIAL_COMPLETED, false)) {
      return;
    }
    state.setLong(
        KEY_TRIAL_ACTIVE_UNTIL,
        now + TRIAL_DURATION_TICKS,
        value -> Math.max(0L, value),
        now + TRIAL_DURATION_TICKS);
    state.setDouble(KEY_TRIAL_RUN_PROGRESS, 0.0D, value -> Math.max(0.0D, value), 0.0D);
    state.setDouble(KEY_TRIAL_RUN_ACCUM, 0.0D, value -> Math.max(0.0D, value), 0.0D);
    state.setInt(KEY_TRIAL_ELITE_PROGRESS, 0, value -> Math.max(0, value), 0);
    if (announce && player instanceof ServerPlayer serverPlayer) {
      serverPlayer.displayClientMessage(Component.literal("猛牛试炼开始：10分钟内疾跑1200米并猎杀1名精英！"), true);
    }
  }

  /** 试炼超时时的提示。 */
  private void notifyTrialRestart(Player player) {
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.displayClientMessage(Component.literal("猛牛试炼超时，重新开始。"), true);
    }
  }

  /** 试炼是否仍在倒计时中。 */
  private boolean isTrialActive(OrganState state, long now) {
    if (state.getBoolean(KEY_TRIAL_COMPLETED, false)) {
      return false;
    }
    long activeUntil = Math.max(0L, state.getLong(KEY_TRIAL_ACTIVE_UNTIL, 0L));
    return activeUntil > now;
  }

  /** 根据最终伤害推测是否能击杀目标。 */
  private boolean willKillTarget(LivingEntity target, float damage) {
    if (target == null) {
      return false;
    }
    float effectiveHealth = target.getHealth() + target.getAbsorptionAmount();
    return damage >= effectiveHealth - 1.0E-4F;
  }

  /** 判定是否为精英级别生物（非 Boss、突袭者或高血量怪物）。 */
  private boolean isEliteMob(Mob mob) {
    if (mob == null) {
      return false;
    }
    if (isBossEntity(mob)) {
      return false;
    }
    MobCategory category = mob.getType().getCategory();
    if (category != MobCategory.MONSTER && category != MobCategory.MISC) {
      return false;
    }
    if (mob.getType().is(EntityTypeTags.RAIDERS)) {
      return true;
    }
    return mob.getMaxHealth() >= 40.0F;
  }

  /** 判定 Boss：包含原版龙/凋灵与 150HP 以上的重型怪。 */
  private boolean isBossEntity(Mob mob) {
    if (mob == null) {
      return false;
    }
    if (mob instanceof EnderDragon || mob instanceof WitherBoss) {
      return true;
    }
    return mob.getMaxHealth() >= 150.0F;
  }

  private float applyBoostStrike(
      Player player,
      LivingEntity target,
      OrganState state,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage,
      long now) {
    long activeUntil = Math.max(0L, state.getLong(KEY_BOOST_ACTIVE_UNTIL, 0L));
    if (activeUntil <= now) {
      return damage;
    }

    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return damage;
    }
    ResourceHandle handle = handleOpt.get();
    OptionalDouble result = handle.consumeScaledJingli(BOOST_HIT_JINGLI_COST);
    if (result.isEmpty()) {
      return damage;
    }

    spawnImpactParticles(player, target);
    triggerBoostSynergy(player, cc, state, target);
    float boostedDamage = Math.max(0.0F, damage + BOOST_EXTRA_DAMAGE);
    int stage = Math.max(STAGE_ONE, Math.min(STAGE_FIVE, state.getInt(KEY_STAGE, STAGE_ONE)));
    if (stage >= STAGE_FIVE) {
      boostedDamage = applyBoostComboBonus(state, boostedDamage, now);
    }
    return boostedDamage;
  }

  /** 强筋·终：跟踪强筋期间的近战命中数，每第四击若冷却允许则额外追加真实伤害。 */
  private float applyBoostComboBonus(OrganState state, float damage, long now) {
    int count = Math.max(0, state.getInt(KEY_BOOST_COMBO_COUNT, 0));
    count = (count + 1) % BOOST_COMBO_HIT_REQUIREMENT;
    state.setInt(KEY_BOOST_COMBO_COUNT, count, value -> Math.max(0, value), count);
    if (count == 0) {
      long readyAt = Math.max(0L, state.getLong(KEY_BOOST_COMBO_READY_AT, 0L));
      if (readyAt <= now) {
        state.setLong(
            KEY_BOOST_COMBO_READY_AT,
            now + BOOST_COMBO_COOLDOWN_TICKS,
            value -> Math.max(0L, value),
            now + BOOST_COMBO_COOLDOWN_TICKS);
        return Math.max(0.0F, damage + BOOST_COMBO_BONUS_DAMAGE);
      }
    }
    return damage;
  }

  private float applyStationaryPrime(
      Player player,
      LivingEntity target,
      OrganState state,
      MultiCooldown cooldown,
      ItemStack organ,
      float damage,
      long now) {
    if (!state.getBoolean(KEY_STATIONARY_READY, false)) {
      return damage;
    }
    state.setBoolean(KEY_STATIONARY_READY, false, false);
    state.setLong(
        KEY_STATIONARY_COOLDOWN,
        now + PASSIVE1_COOLDOWN_TICKS,
        value -> Math.max(0L, value),
        now + PASSIVE1_COOLDOWN_TICKS);
    state.setLong(KEY_STATIONARY_PRIME_AT, 0L, value -> Math.max(0L, value), 0L);

    double dx = target.getX() - player.getX();
    double dz = target.getZ() - player.getZ();
    target.knockback(PASSIVE1_KNOCKBACK, dx, dz);
    incrementKnockback(state, 1);
    return Math.max(0.0F, damage * (1.0F + PASSIVE1_DAMAGE_BONUS));
  }

  private float applySprintBonus(
      Player player,
      LivingEntity target,
      OrganState state,
      ItemStack organ,
      float damage,
      long now) {
    if (!state.getBoolean(KEY_SPRINT_READY, false)) {
      return damage;
    }
    state.setBoolean(KEY_SPRINT_READY, false, false);
    state.setDouble(KEY_SPRINT_ACCUM, 0.0D, value -> Math.max(0.0D, value), 0.0D);
    state.setLong(KEY_SPRINT_EXPIRE, 0L, value -> Math.max(0L, value), 0L);

    int stage = Math.max(STAGE_ONE, Math.min(STAGE_FIVE, state.getInt(KEY_STAGE, STAGE_ONE)));
    float bonusDamage = PASSIVE2_BASE_DAMAGE;
    if (stage >= STAGE_FOUR) {
      bonusDamage += PASSIVE2_STAGE4_BONUS;
    }
    target.addEffect(
        new MobEffectInstance(
            MobEffects.MOVEMENT_SLOWDOWN,
            PASSIVE2_SLOW_TICKS,
            PASSIVE2_SLOW_LEVEL,
            false,
            true,
            true));
    return Math.max(0.0F, damage + bonusDamage);
  }

  private float applyRushImpact(
      Player player,
      LivingEntity target,
      OrganState state,
      MultiCooldown cooldown,
      ItemStack organ,
      float damage,
      long now) {
    long rushUntil = Math.max(0L, state.getLong(KEY_RUSH_ACTIVE_UNTIL, 0L));
    if (rushUntil <= now || state.getBoolean(KEY_RUSH_HIT_OCCURRED, false)) {
      return damage;
    }

    double dx = target.getX() - player.getX();
    double dz = target.getZ() - player.getZ();
    target.knockback(RUSH_KNOCKBACK, dx, dz);
    state.setBoolean(KEY_RUSH_HIT_OCCURRED, true, false);
    createRushShockwave(player, target);
    finishRush(player, state, false);
    return Math.max(0.0F, damage + RUSH_EXTRA_DAMAGE);
  }

  private void incrementKnockback(OrganState state, int amount) {
    if (amount <= 0) {
      return;
    }
    int total = state.getInt(KEY_KB_PROGRESS, 0) + amount;
    state.setInt(KEY_KB_PROGRESS, total, value -> Math.max(0, value), 0);
  }

  private void spawnImpactParticles(Player player, LivingEntity target) {
    Level level = player.level();
    if (!(level instanceof ServerLevel server)) {
      return;
    }
    Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5F, 0.0);
    server.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 6, 0.1, 0.1, 0.1, 0.01);
  }

  private void triggerBoostSynergy(
      Player player, ChestCavityInstance cc, OrganState state, LivingEntity target) {
    if (state.getBoolean(KEY_BOOST_LAUNCH_SPENT, false)) {
      return;
    }
    if (!hasOrgan(cc, JING_LI_GU_ID)) {
      return;
    }
    Vec3 velocity = target.getDeltaMovement();
    target.setDeltaMovement(velocity.x, velocity.y + BOOST_LAUNCH_VELOCITY, velocity.z);
    state.setBoolean(KEY_BOOST_LAUNCH_SPENT, true, false);
  }

  private void createRushShockwave(Player player, LivingEntity primaryTarget) {
    Level level = player.level();
    if (!(level instanceof ServerLevel server)) {
      return;
    }
    Vec3 origin = primaryTarget.position();
    AABB box = primaryTarget.getBoundingBox().inflate(RUSH_AOE_RADIUS);
    List<LivingEntity> victims =
        server.getEntitiesOfClass(LivingEntity.class, box, entity -> entity != player);
    for (LivingEntity victim : victims) {
      if (victim == primaryTarget) {
        continue;
      }
      if (!CombatEntityUtil.areEnemies(player, victim)) {
        continue;
      }
      Vec3 delta = victim.position().subtract(origin);
      if (delta.lengthSqr() <= 1.0E-4) {
        delta = new Vec3(0.0, 0.0, 1.0);
      }
      victim.knockback(0.3F, delta.x, delta.z);
      victim.addEffect(
          new MobEffectInstance(
              MobEffects.MOVEMENT_SLOWDOWN,
              Math.max(1, RUSH_AOE_STUN_TICKS),
              1,
              false,
              false,
              true));
    }
    server.sendParticles(
        ParticleTypes.POOF, origin.x, origin.y, origin.z, 12, 0.2, 0.05, 0.2, 0.01);
    server.playSound(
        null,
        origin.x,
        origin.y,
        origin.z,
        SoundEvents.ANVIL_PLACE,
        SoundSource.PLAYERS,
        0.7F,
        0.9F + server.random.nextFloat() * 0.1F);
  }

  private void recordHitProgress(
      Player player, LivingEntity target, OrganState state, long now, float damage) {
    Object2LongMap<UUID> inner =
        HIT_DEDUP.computeIfAbsent(player.getUUID(), uuid -> new Object2LongOpenHashMap<>());
    UUID targetId = target.getUUID();
    long nextAllowed = inner.getOrDefault(targetId, 0L);
    if (now < nextAllowed) {
      recordEliteKill(player, target, state, now, damage);
      return;
    }
    inner.put(targetId, now + (long) (HIT_DEDUP_SECONDS * TICKS_PER_SECOND));
    state.setInt(
        KEY_HIT_PROGRESS, state.getInt(KEY_HIT_PROGRESS, 0) + 1, value -> Math.max(0, value), 0);

    EntityType<?> type = target.getType();
    if (type.getCategory() == MobCategory.MONSTER || target.getMaxHealth() >= 60.0F) {
      recordEliteKill(player, target, state, now, damage);
    }
  }

  /** 记录精英/Boss 的击杀贡献：满足致死条件时按照类型附加成长点数，并在试炼期间统计击杀次数。 */
  private void recordEliteKill(
      Player player, LivingEntity target, OrganState state, long now, float damage) {
    if (!willKillTarget(target, damage) || !(target instanceof Mob mob)) {
      return;
    }

    boolean boss = isBossEntity(mob);
    boolean elite = !boss && isEliteMob(mob);
    if (!boss && !elite) {
      return;
    }

    String cooldownKey = boss ? KEY_LAST_BOSS_KILL : KEY_LAST_ELITE_KILL;
    long last = Math.max(0L, state.getLong(cooldownKey, 0L));
    if (now - last < ELITE_KILL_COOLDOWN_TICKS) {
      return;
    }

    state.setLong(cooldownKey, now, value -> Math.max(0L, value), now);
    int increment = boss ? BOSS_KILL_VALUE : ELITE_KILL_VALUE;
    int total = state.getInt(KEY_ELITE_PROGRESS, 0) + increment;
    state.setInt(KEY_ELITE_PROGRESS, total, value -> Math.max(0, value), 0);

    if (!state.getBoolean(KEY_TRIAL_COMPLETED, false) && isTrialActive(state, now)) {
      int trialKills = state.getInt(KEY_TRIAL_ELITE_PROGRESS, 0) + 1;
      state.setInt(KEY_TRIAL_ELITE_PROGRESS, trialKills, value -> Math.max(0, value), 0);
      checkTrialCompletion(player, state, now);
    }
  }

  private void updateStageIfNeeded(
      Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long now) {
    int current = Math.max(STAGE_ONE, Math.min(STAGE_FIVE, state.getInt(KEY_STAGE, STAGE_ONE)));
    int next = current;
    int hits = state.getInt(KEY_HIT_PROGRESS, 0);
    int run = state.getInt(KEY_RUN_PROGRESS, 0);
    int kb = state.getInt(KEY_KB_PROGRESS, 0);
    int elite = state.getInt(KEY_ELITE_PROGRESS, 0);

    if (current == STAGE_ONE && hits >= STAGE_TWO_HIT_THRESHOLD && run >= STAGE_TWO_RUN_THRESHOLD) {
      next = STAGE_TWO;
    } else if (current == STAGE_TWO
        && kb >= STAGE_THREE_KB_THRESHOLD
        && run >= STAGE_THREE_RUN_THRESHOLD) {
      next = STAGE_THREE;
    } else if (current == STAGE_THREE
        && elite >= STAGE_FOUR_ELITE_THRESHOLD
        && hits >= STAGE_FOUR_HIT_THRESHOLD) {
      next = STAGE_FOUR;
    } else if (current == STAGE_FOUR && state.getBoolean(KEY_TRIAL_COMPLETED, false)) {
      next = STAGE_FIVE;
    }

    if (next != current) {
      state.setInt(KEY_STAGE, next, value -> Mth.clamp(value, STAGE_ONE, STAGE_FIVE), STAGE_ONE);
      if (player instanceof ServerPlayer serverPlayer) {
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
        serverPlayer.displayClientMessage(
            net.minecraft.network.chat.Component.literal("蛮力天牛蛊蜕变至" + next + "转"), true);
      }
      if (next == STAGE_FOUR) {
        startTrial(player, state, now, true);
      }
    }
  }

  private static void activateBoost(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof Player player) || player.level().isClientSide()) {
      return;
    }
    if (cc == null) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }

    OrganState state = INSTANCE.organState(organ, STATE_ROOT);
    MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ, state);
    long now = player.level().getGameTime();
    Entry readyEntry = cooldown.entry(KEY_BOOST_READY_AT).withDefault(0L);
    if (!readyEntry.isReady(now)) {
      return;
    }

    OptionalDouble consumed =
        ResourceOps.tryConsumeScaledZhenyuan(player, BOOST_BASE_ZHENYUAN_COST);
    if (consumed.isEmpty()) {
      return;
    }

    int stage = Math.max(STAGE_ONE, Math.min(STAGE_FIVE, state.getInt(KEY_STAGE, STAGE_ONE)));
    int duration = BOOST_BASE_DURATION_TICKS;
    if (stage >= STAGE_TWO) {
      duration += BOOST_STAGE_TWO_BONUS_TICKS;
    }

    state.setLong(
        KEY_BOOST_ACTIVE_UNTIL, now + duration, value -> Math.max(0L, value), now + duration);
    state.setBoolean(KEY_BOOST_LAUNCH_SPENT, false, false);
    state.setInt(KEY_BOOST_COMBO_COUNT, 0, value -> Math.max(0, value), 0);
    state.setLong(KEY_BOOST_COMBO_READY_AT, now, value -> Math.max(0L, value), now);
    readyEntry.setReadyAt(now + BOOST_COOLDOWN_TICKS);

    if (player instanceof ServerPlayer serverPlayer) {
      ActiveSkillRegistry.scheduleReadyToast(
          serverPlayer, BOOST_ABILITY_ID, readyEntry.getReadyTick(), now);
    }
    player
        .level()
        .playSound(
            null,
            player.blockPosition(),
            SoundEvents.IRON_GOLEM_REPAIR,
            SoundSource.PLAYERS,
            0.8F,
            0.9F);
  }

  private static void activateRush(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof Player player) || player.level().isClientSide()) {
      return;
    }
    if (!(player.level() instanceof ServerLevel serverLevel)) {
      return;
    }
    if (cc == null) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }

    OrganState state = INSTANCE.organState(organ, STATE_ROOT);
    MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ, state);
    long now = serverLevel.getGameTime();
    Entry readyEntry = cooldown.entry(KEY_RUSH_READY_AT).withDefault(0L);
    if (!readyEntry.isReady(now)) {
      return;
    }
    long activeUntil = Math.max(0L, state.getLong(KEY_RUSH_ACTIVE_UNTIL, 0L));
    if (activeUntil > now) {
      return;
    }

    var payment = ResourceOps.consumeStrict(player, RUSH_BASE_ZHENYUAN_COST, RUSH_BASE_JINGLI_COST);
    if (!payment.succeeded()) {
      return;
    }

    Vec3 look = player.getLookAngle();
    Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
    if (horizontal.lengthSqr() < 1.0E-4) {
      horizontal = Vec3.directionFromRotation(0.0F, player.getYRot());
      horizontal = new Vec3(horizontal.x, 0.0, horizontal.z);
    }
    horizontal = horizontal.normalize();

    long sequence = state.getLong(KEY_RUSH_SEQUENCE, 0L) + 1L;
    state.setLong(KEY_RUSH_SEQUENCE, sequence, value -> Math.max(0L, value), sequence);
    state.setLong(
        KEY_RUSH_ACTIVE_UNTIL,
        now + RUSH_DURATION_TICKS,
        value -> Math.max(0L, value),
        now + RUSH_DURATION_TICKS);
    state.setBoolean(KEY_RUSH_HIT_OCCURRED, false, false);
    state.setDouble(KEY_RUSH_DIR_X, horizontal.x, value -> value, horizontal.x);
    state.setDouble(KEY_RUSH_DIR_Z, horizontal.z, value -> value, horizontal.z);

    readyEntry.setReadyAt(now + Math.max(1, RUSH_COOLDOWN_TICKS));
    if (player instanceof ServerPlayer serverPlayer) {
      ActiveSkillRegistry.scheduleReadyToast(
          serverPlayer, RUSH_ABILITY_ID, readyEntry.getReadyTick(), now);
    }

    RushSession session =
        new RushSession(player.getUUID(), serverLevel, sequence, horizontal, RUSH_DURATION_TICKS);
    ACTIVE_RUSHES.put(player.getUUID(), session);
    applyRushVelocity(player, horizontal);
    TickOps.schedule(serverLevel, () -> runRushTick(session, 1), 1);
    serverLevel.playSound(
        null, player.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.8F, 1.0F);
  }

  private static void runRushTick(RushSession session, int tick) {
    if (session == null) {
      return;
    }
    Player player = session.resolvePlayer();
    if (player == null) {
      ACTIVE_RUSHES.remove(session.playerId());
      return;
    }
    OrganState state = session.state(INSTANCE);
    if (state == null) {
      ACTIVE_RUSHES.remove(session.playerId());
      return;
    }
    if (ACTIVE_RUSHES.get(session.playerId()) != session) {
      return;
    }

    long now = session.level().getGameTime();
    long activeSequence = Math.max(0L, state.getLong(KEY_RUSH_SEQUENCE, 0L));
    long activeUntil = Math.max(0L, state.getLong(KEY_RUSH_ACTIVE_UNTIL, 0L));
    if (activeSequence != session.sequence() || activeUntil <= now) {
      ACTIVE_RUSHES.remove(session.playerId(), session);
      return;
    }
    if (tick >= session.durationTicks()) {
      INSTANCE.finishRush(player, state, true);
      ACTIVE_RUSHES.remove(session.playerId(), session);
      return;
    }

    applyRushVelocity(player, session.direction());
    TickOps.schedule(session.level(), () -> runRushTick(session, tick + 1), 1);
  }

  private static void applyRushVelocity(Player player, Vec3 direction) {
    Vec3 velocity = direction.scale(RUSH_HORIZONTAL_SPEED);
    player.setDeltaMovement(velocity.x, player.getDeltaMovement().y * 0.5, velocity.z);
    player.hurtMarked = true;
  }

  private void finishRush(Player player, OrganState state, boolean scheduleWave) {
    long sequence = Math.max(0L, state.getLong(KEY_RUSH_SEQUENCE, 0L));
    state.setLong(KEY_RUSH_ACTIVE_UNTIL, 0L, value -> Math.max(0L, value), 0L);
    state.setBoolean(KEY_RUSH_HIT_OCCURRED, false, false);
    Vec3 current = player.getDeltaMovement();
    player.setDeltaMovement(current.x * 0.3, current.y, current.z * 0.3);
    player.hurtMarked = true;
    RushSession session = ACTIVE_RUSHES.get(player.getUUID());
    if (session != null && session.sequence() == sequence) {
      ACTIVE_RUSHES.remove(player.getUUID(), session);
    }
    if (hasOrgan(ccFromPlayer(player), QUAN_LI_YI_FU_GU_ID) && scheduleWave) {
      spawnLandingShockwave(player);
    }
  }

  private void spawnLandingShockwave(Player player) {
    Level level = player.level();
    if (!(level instanceof ServerLevel server)) {
      return;
    }
    Vec3 origin = player.position();
    List<LivingEntity> victims =
        server.getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(RUSH_AOE_RADIUS + 0.5F),
            entity -> entity != player && CombatEntityUtil.areEnemies(player, entity));
    for (LivingEntity victim : victims) {
      victim.hurt(player.damageSources().playerAttack(player), PASSIVE2_BASE_DAMAGE);
      Vec3 delta = victim.position().subtract(origin);
      victim.knockback(0.4F, delta.x, delta.z);
    }
    server.sendParticles(
        ParticleTypes.CLOUD, origin.x, origin.y, origin.z, 20, 0.4, 0.1, 0.4, 0.02);
    server.playSound(
        null, player.blockPosition(), SoundEvents.ANVIL_FALL, SoundSource.PLAYERS, 0.7F, 0.75F);
  }

  private MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ, OrganState state) {
    MultiCooldown.Builder builder =
        MultiCooldown.builder(state).withLongClamp(v -> Math.max(0L, v), 0L);
    if (cc != null) {
      builder.withSync(cc, organ);
    } else {
      builder.withOrgan(organ);
    }
    return builder.build();
  }

  private static ItemStack findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (INSTANCE.matchesOrgan(stack, ORGAN_ID)) {
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }

  private boolean hasOrgan(ChestCavityInstance cc, ResourceLocation id) {
    if (cc == null || cc.inventory == null) {
      return false;
    }
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (matchesOrgan(stack, id)) {
        return true;
      }
    }
    return false;
  }

  private boolean isPrimaryOrgan(ChestCavityInstance cc, ItemStack organ) {
    if (cc == null || cc.inventory == null || organ == null || organ.isEmpty()) {
      return false;
    }
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack == organ) {
        return true;
      }
      if (matchesOrgan(stack, ORGAN_ID)) {
        return stack == organ;
      }
    }
    return false;
  }

  private static ChestCavityInstance ccFromPlayer(Player player) {
    if (player instanceof net.tigereye.chestcavity.interfaces.ChestCavityEntity ccEntity) {
      return ccEntity.getChestCavityInstance();
    }
    return null;
  }

  private static final class RushSession {
    private final UUID playerId;
    private final ServerLevel level;
    private final long sequence;
    private final Vec3 direction;
    private final int durationTicks;

    RushSession(
        UUID playerId, ServerLevel level, long sequence, Vec3 direction, int durationTicks) {
      this.playerId = playerId;
      this.level = level;
      this.sequence = sequence;
      this.direction = direction;
      this.durationTicks = durationTicks;
    }

    UUID playerId() {
      return playerId;
    }

    ServerLevel level() {
      return level;
    }

    long sequence() {
      return sequence;
    }

    Vec3 direction() {
      return direction;
    }

    int durationTicks() {
      return durationTicks;
    }

    Player resolvePlayer() {
      return level.getPlayerByUUID(playerId);
    }

    OrganState state(ManLiTianNiuGuOrganBehavior behavior) {
      Player player = resolvePlayer();
      if (player == null) {
        return null;
      }
      ChestCavityInstance cc = ccFromPlayer(player);
      if (cc == null) {
        return null;
      }
      ItemStack organ = findOrgan(cc);
      if (organ.isEmpty()) {
        return null;
      }
      return behavior.organState(organ, STATE_ROOT);
    }
  }
}
