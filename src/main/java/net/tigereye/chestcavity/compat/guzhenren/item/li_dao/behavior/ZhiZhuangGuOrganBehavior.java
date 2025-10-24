package net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.common.ShadowService;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.AbstractLiDaoOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.util.CombatEntityUtil;
import net.tigereye.chestcavity.compat.guzhenren.util.GuzhenrenFlowTooltipResolver;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/** 直撞蛊（力道·肌肉）：短距冲锋命中可刷新冷却、叠加动能并触发魂道回声。 */
public final class ZhiZhuangGuOrganBehavior extends AbstractLiDaoOrganBehavior
    implements OrganSlowTickListener {

  public static final ZhiZhuangGuOrganBehavior INSTANCE = new ZhiZhuangGuOrganBehavior();

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final String LOG_PREFIX = "[compat/guzhenren][li_dao][zhi_zhuang_gu]";

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "zhi_zhuang_gu");
  public static final ResourceLocation ABILITY_ID = ORGAN_ID;

  private static final String STATE_ROOT = "ZhiZhuangGu";
  private static final String KEY_COOLDOWN_READY_AT = "CooldownReadyAt";
  private static final String KEY_DASH_ACTIVE_UNTIL = "DashActiveUntil";
  private static final String KEY_SEQUENCE = "DashSequence";
  private static final String KEY_MOMENTUM_STACK = "MomentumStack";
  private static final String KEY_MOMENTUM_EXPIRE = "MomentumExpireTick";
  private static final String KEY_COMBO_COUNT = "ComboCount";
  private static final String KEY_PENDING_BURST = "PendingBurst";
  private static final String KEY_LAST_HIT_TICK = "LastHitTick";

  // 基础伤害参数：初始冲击、动能层数与肌肉条加成、击退与爆发倍率
  private static final double BASE_IMPACT_DAMAGE =
      BehaviorConfigAccess.getFloat(
          ZhiZhuangGuOrganBehavior.class, "BASE_IMPACT_DAMAGE", 25.0F); // 基础直撞伤害
  private static final double DAMAGE_PER_STACK =
      BehaviorConfigAccess.getFloat(
          ZhiZhuangGuOrganBehavior.class, "DAMAGE_PER_STACK", 0.10F); // 每层惯性提供的额外伤害比例
  private static final double MUSCLE_DAMAGE_BONUS =
      BehaviorConfigAccess.getFloat(
          ZhiZhuangGuOrganBehavior.class, "MUSCLE_DAMAGE_BONUS", 0.03F); // 每条力道肌肉提升的额外伤害
  private static final double BASE_KNOCKBACK =
      BehaviorConfigAccess.getFloat(
          ZhiZhuangGuOrganBehavior.class, "BASE_KNOCKBACK", 0.9F); // 基础击退强度
  private static final double KNOCKBACK_PER_STACK =
      BehaviorConfigAccess.getFloat(
          ZhiZhuangGuOrganBehavior.class, "KNOCKBACK_PER_STACK", 0.08F); // 每层惯性提升击退
  private static final int MOMENTUM_MAX_STACKS =
      BehaviorConfigAccess.getInt(
          ZhiZhuangGuOrganBehavior.class, "MOMENTUM_MAX_STACKS", 10); // 惯性层数上限
  private static final int MOMENTUM_DECAY_TICKS =
      BehaviorConfigAccess.getInt(
          ZhiZhuangGuOrganBehavior.class, "MOMENTUM_DECAY_TICKS", 40); // 惯性层在无命中时的衰减时间
  private static final int BURST_THRESHOLD =
      BehaviorConfigAccess.getInt(
          ZhiZhuangGuOrganBehavior.class, "BURST_THRESHOLD", 3); // 爆发态触发所需连撞次数
  private static final double BURST_DAMAGE_MULTIPLIER =
      BehaviorConfigAccess.getFloat(
          ZhiZhuangGuOrganBehavior.class, "BURST_DAMAGE_MULTIPLIER", 1.8F); // 爆发态伤害倍率
  private static final double BURST_KNOCKBACK_MULTIPLIER =
      BehaviorConfigAccess.getFloat(
          ZhiZhuangGuOrganBehavior.class, "BURST_KNOCKBACK_MULTIPLIER", 1.6F); // 爆发态击退倍率
  private static final double BURST_AOE_RADIUS =
      BehaviorConfigAccess.getFloat(
          ZhiZhuangGuOrganBehavior.class, "BURST_AOE_RADIUS", 2.5F); // 爆发态冲击波半径

  // 冷却与位移：基础冷却、失手冷却、冲刺持续与速度参数
  private static final int BASE_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(
          ZhiZhuangGuOrganBehavior.class, "BASE_COOLDOWN_TICKS", 40); // 命中后的默认冷却
  private static final int FAIL_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(
          ZhiZhuangGuOrganBehavior.class, "FAIL_COOLDOWN_TICKS", 60); // 未命中时的惩罚冷却
  private static final int DASH_DURATION_TICKS =
      BehaviorConfigAccess.getInt(
          ZhiZhuangGuOrganBehavior.class, "DASH_DURATION_TICKS", 12); // 冲刺持续时间
  private static final double DASH_SPEED =
      BehaviorConfigAccess.getFloat(ZhiZhuangGuOrganBehavior.class, "DASH_SPEED", 1.1F); // 基础冲刺速度
  private static final double DASH_SPEED_PER_STACK =
      BehaviorConfigAccess.getFloat(
          ZhiZhuangGuOrganBehavior.class, "DASH_SPEED_PER_STACK", 0.05F); // 每层惯性增加的冲刺速度
  private static final double BURST_DISTANCE_MULTIPLIER =
      BehaviorConfigAccess.getFloat(
          ZhiZhuangGuOrganBehavior.class, "BURST_DISTANCE_MULTIPLIER", 1.5F); // 爆发态冲刺距离倍率
  private static final double DASH_VERTICAL_DAMP =
      BehaviorConfigAccess.getFloat(
          ZhiZhuangGuOrganBehavior.class, "DASH_VERTICAL_DAMP", 0.15F); // 冲刺时竖直速度衰减
  private static final double HIT_RADIUS =
      BehaviorConfigAccess.getFloat(
          ZhiZhuangGuOrganBehavior.class, "HIT_RADIUS", 1.25F); // 命中判定水平半径
  private static final double HIT_VERTICAL =
      BehaviorConfigAccess.getFloat(
          ZhiZhuangGuOrganBehavior.class, "HIT_VERTICAL", 1.6F); // 命中判定垂直范围

  // 消耗：激活直撞所需真元、精力
  private static final double ACTIVE_ZHENYUAN_COST =
      BehaviorConfigAccess.getFloat(
          ZhiZhuangGuOrganBehavior.class, "ACTIVE_ZHENYUAN_COST", 120000.0F); // 激活消耗真元
  private static final double ACTIVE_JINGLI_COST =
      BehaviorConfigAccess.getFloat(
          ZhiZhuangGuOrganBehavior.class, "ACTIVE_JINGLI_COST", 80.0F); // 激活消耗精力

  // 灵魂回声：延迟时间、补刀伤害比例与击退力度
  private static final int SPIRIT_ECHO_DELAY_TICKS =
      BehaviorConfigAccess.getInt(
          ZhiZhuangGuOrganBehavior.class, "SPIRIT_ECHO_DELAY_TICKS", 20); // 魂回声延迟
  private static final double SPIRIT_ECHO_DAMAGE_MULTIPLIER =
      BehaviorConfigAccess.getFloat(
          ZhiZhuangGuOrganBehavior.class, "SPIRIT_ECHO_DAMAGE_MULTIPLIER", 0.5F); // 魂回声伤害倍率
  private static final double SPIRIT_ECHO_KNOCKBACK =
      BehaviorConfigAccess.getFloat(
          ZhiZhuangGuOrganBehavior.class, "SPIRIT_ECHO_KNOCKBACK", 0.45F); // 魂回声击退力度

  private static final int SHADOW_TRAVEL_TICKS =
      BehaviorConfigAccess.getInt(
          ZhiZhuangGuOrganBehavior.class, "SHADOW_TRAVEL_TICKS", 8); // 影道残像沿冲刺路径行进的插值帧数

  private static final ParticleOptions TRAIL_PARTICLE = ParticleTypes.CLOUD;
  private static final ParticleOptions IMPACT_PARTICLE = ParticleTypes.SONIC_BOOM;

  private static final String FLOW_KEY_YING_DAO = "影道";

  private static final Map<UUID, DashSession> ACTIVE_DASHES = new ConcurrentHashMap<>();

  static {
    OrganActivationListeners.register(ABILITY_ID, ZhiZhuangGuOrganBehavior::activateAbility);
  }

  private ZhiZhuangGuOrganBehavior() {}

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof Player player) || entity.level().isClientSide()) {
      return;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return;
    }
    Level level = entity.level();
    if (!(level instanceof ServerLevel serverLevel)) {
      return;
    }
    OrganState state = organState(organ, STATE_ROOT);
    MultiCooldown cooldown = createCooldown(cc, organ, state);
    long now = serverLevel.getGameTime();
    MultiCooldown.Entry momentumExpireEntry = cooldown.entry(KEY_MOMENTUM_EXPIRE);
    long expireAt = momentumExpireEntry.getReadyTick();
    if (expireAt > 0L && expireAt <= now) {
      int momentum = state.getInt(KEY_MOMENTUM_STACK, 0);
      int combo = state.getInt(KEY_COMBO_COUNT, 0);
      boolean pendingBurst = state.getBoolean(KEY_PENDING_BURST, false);
      boolean dirty = false;
      if (momentum > 0) {
        dirty |=
            OrganStateOps.setInt(
                    state,
                    cc,
                    organ,
                    KEY_MOMENTUM_STACK,
                    0,
                    value -> Mth.clamp(value, 0, MOMENTUM_MAX_STACKS),
                    0)
                .changed();
      }
      if (combo > 0) {
        dirty |=
            OrganStateOps.setInt(
                    state,
                    cc,
                    organ,
                    KEY_COMBO_COUNT,
                    0,
                    value -> Mth.clamp(value, 0, MOMENTUM_MAX_STACKS),
                    0)
                .changed();
      }
      if (pendingBurst) {
        dirty |=
            OrganStateOps.setBoolean(state, cc, organ, KEY_PENDING_BURST, false, false).changed();
      }
      if (dirty && LOGGER.isDebugEnabled()) {
        LOGGER.debug("{} momentum decayed for {}", LOG_PREFIX, describeStack(organ));
      }
      momentumExpireEntry.setReadyAt(0L);
    }

    MultiCooldown.Entry dashEntry = cooldown.entry(KEY_DASH_ACTIVE_UNTIL);
    if (dashEntry.getReadyTick() > 0 && dashEntry.getReadyTick() <= now) {
      dashEntry.setReadyAt(0L);
    }
  }

  private static void activateAbility(LivingEntity living, ChestCavityInstance cc) {
    if (!(living instanceof ServerPlayer player)) {
      return;
    }
    ServerLevel level = player.serverLevel();
    if (level == null) {
      return;
    }
    if (cc == null || cc.inventory == null) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }

    OrganState state = INSTANCE.organState(organ, STATE_ROOT);
    MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ, state);
    long now = level.getGameTime();
    MultiCooldown.Entry dashEntry = cooldown.entry(KEY_DASH_ACTIVE_UNTIL);
    if (dashEntry.getReadyTick() > now) {
      return;
    }
    MultiCooldown.Entry readyEntry = cooldown.entry(KEY_COOLDOWN_READY_AT);
    if (readyEntry.getReadyTick() > now) {
      return;
    }

    ResourceOps.consumeStrict(player, ACTIVE_ZHENYUAN_COST, ACTIVE_JINGLI_COST);

    int momentum = state.getInt(KEY_MOMENTUM_STACK, 0);
    long momentumExpire = cooldown.entry(KEY_MOMENTUM_EXPIRE).getReadyTick();
    if (momentumExpire <= now) {
      OrganStateOps.setInt(
          state,
          cc,
          organ,
          KEY_MOMENTUM_STACK,
          0,
          value -> Mth.clamp(value, 0, MOMENTUM_MAX_STACKS),
          0);
      OrganStateOps.setInt(
          state,
          cc,
          organ,
          KEY_COMBO_COUNT,
          0,
          value -> Mth.clamp(value, 0, MOMENTUM_MAX_STACKS),
          0);
      OrganStateOps.setBoolean(state, cc, organ, KEY_PENDING_BURST, false, false);
      momentum = 0;
    }

    boolean burst = state.getBoolean(KEY_PENDING_BURST, false);
    if (burst) {
      OrganStateOps.setBoolean(state, cc, organ, KEY_PENDING_BURST, false, false);
      OrganStateOps.setInt(
          state,
          cc,
          organ,
          KEY_COMBO_COUNT,
          0,
          value -> Mth.clamp(value, 0, MOMENTUM_MAX_STACKS),
          0);
    }

    long sequence = state.getLong(KEY_SEQUENCE, 0L) + 1L;
    OrganStateOps.setLong(
        state, cc, organ, KEY_SEQUENCE, sequence, value -> Math.max(0L, value), 0L);

    int dashTicks =
        burst ? Mth.ceil(DASH_DURATION_TICKS * BURST_DISTANCE_MULTIPLIER) : DASH_DURATION_TICKS;
    dashEntry.setReadyAt(now + dashTicks);
    readyEntry.setReadyAt(now + BASE_COOLDOWN_TICKS);
    ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, readyEntry.getReadyTick(), now);

    Vec3 direction = resolveDashDirection(player);
    if (direction.lengthSqr() < 1.0E-4) {
      return;
    }

    boolean hasSoulFlow = hasSoulFlow(cc, level);
    int organSlot = ChestCavityUtil.findOrganSlot(cc, organ);
    Vec3 startPosition = player.position();
    DashSession session =
        new DashSession(
            player.getUUID(),
            level,
            organSlot,
            organ,
            sequence,
            direction,
            dashTicks,
            computeDashSpeed(momentum, burst),
            burst,
            hasSoulFlow,
            now,
            startPosition);
    ACTIVE_DASHES.put(player.getUUID(), session);

    spawnDashStartFx(level, player);
    applyDashVelocity(player, session);
    scheduleDashTick(session, 1);
  }

  private static Vec3 resolveDashDirection(ServerPlayer player) {
    Vec3 look = player.getLookAngle();
    Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
    if (horizontal.lengthSqr() < 1.0E-4) {
      float yaw = player.getYRot();
      horizontal = Vec3.directionFromRotation(0.0F, yaw);
      horizontal = new Vec3(horizontal.x, 0.0, horizontal.z);
    }
    return horizontal.normalize();
  }

  private static double computeDashSpeed(int momentum, boolean burst) {
    double speed = DASH_SPEED + Math.max(0, momentum) * DASH_SPEED_PER_STACK;
    if (burst) {
      speed *= BURST_DISTANCE_MULTIPLIER;
    }
    return speed;
  }

  private static void scheduleDashTick(DashSession session, int tick) {
    ServerLevel level = session.level;
    TickOps.schedule(level, () -> runDashTick(session, tick), 1);
  }

  private static void runDashTick(DashSession session, int tick) {
    ServerLevel level = session.level;
    ServerPlayer player = level.getServer().getPlayerList().getPlayer(session.playerId);
    if (player == null || player.isRemoved()) {
      ACTIVE_DASHES.remove(session.playerId, session);
      return;
    }
    ChestCavityInstance cc =
        ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
    if (cc == null) {
      ACTIVE_DASHES.remove(session.playerId, session);
      return;
    }
    ItemStack organ = session.resolveOrgan(cc);
    if (organ.isEmpty() || !INSTANCE.matchesOrgan(organ, ORGAN_ID)) {
      ACTIVE_DASHES.remove(session.playerId, session);
      return;
    }

    OrganState state = INSTANCE.organState(organ, STATE_ROOT);
    MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ, state);
    long now = level.getGameTime();
    if (state.getLong(KEY_SEQUENCE, 0L) != session.sequence) {
      ACTIVE_DASHES.remove(session.playerId, session);
      return;
    }
    if (tick > session.durationTicks) {
      endDash(session, player, cc, organ, state, cooldown, now, false);
      return;
    }

    applyDashVelocity(player, session);
    spawnDashTrail(level, player);
    boolean hit = applyDashHits(player, cc, organ, state, cooldown, session, now);
    if (hit) {
      session.hadImpact = true;
      session.lastImpactTick = now;
    }

    if (tick == session.durationTicks) {
      endDash(session, player, cc, organ, state, cooldown, now, false);
    } else {
      scheduleDashTick(session, tick + 1);
    }
  }

  private static void applyDashVelocity(ServerPlayer player, DashSession session) {
    Vec3 dir = session.direction;
    Vec3 current = player.getDeltaMovement();
    Vec3 motion =
        new Vec3(
            dir.x * session.speed, current.y * (1.0 - DASH_VERTICAL_DAMP), dir.z * session.speed);
    player.setDeltaMovement(motion);
    player.hurtMarked = true;
    player.fallDistance = 0.0F;
    player.hasImpulse = true;
  }

  private static boolean applyDashHits(
      ServerPlayer player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      DashSession session,
      long now) {
    ServerLevel level = session.level;
    Vec3 dir = session.direction;
    Vec3 center = player.position().add(dir.scale(0.5));
    AABB box =
        player
            .getBoundingBox()
            .expandTowards(dir.scale(0.75))
            .inflate(HIT_RADIUS, HIT_VERTICAL, HIT_RADIUS);
    List<LivingEntity> candidates =
        level.getEntitiesOfClass(
            LivingEntity.class,
            box,
            entity ->
                CombatEntityUtil.areEnemies(player, entity)
                    && session.hitEntities.add(entity.getId()));

    boolean anyImpact = false;
    for (LivingEntity target : candidates) {
      if (!target.isAlive() || target.level().isClientSide()) {
        continue;
      }
      if (performImpact(player, cc, organ, state, cooldown, session, target, now)) {
        anyImpact = true;
      }
    }
    if (anyImpact) {
      spawnImpactFx(level, center);
    }
    return anyImpact;
  }

  private static boolean performImpact(
      ServerPlayer player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      DashSession session,
      LivingEntity target,
      long now) {
    if (!target.hurt(
        player.damageSources().playerAttack(player),
        (float) calculateImpactDamage(cc, state, session))) {
      return false;
    }

    applyKnockback(player, target, state, session);
    MultiCooldown.Entry cooldownEntry = cooldown.entry(KEY_COOLDOWN_READY_AT);
    cooldownEntry.setReadyAt(now);
    ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, now, now);

    MultiCooldown.Entry momentumExpire = cooldown.entry(KEY_MOMENTUM_EXPIRE);
    momentumExpire.setReadyAt(now + MOMENTUM_DECAY_TICKS);

    int momentum = state.getInt(KEY_MOMENTUM_STACK, 0);
    momentum = Mth.clamp(momentum + 1, 0, MOMENTUM_MAX_STACKS);
    OrganStateOps.setInt(
        state,
        cc,
        organ,
        KEY_MOMENTUM_STACK,
        momentum,
        value -> Mth.clamp(value, 0, MOMENTUM_MAX_STACKS),
        0);
    OrganStateOps.setLong(
        state, cc, organ, KEY_LAST_HIT_TICK, now, value -> Math.max(0L, value), 0L);

    int combo = Mth.clamp(state.getInt(KEY_COMBO_COUNT, 0) + 1, 0, MOMENTUM_MAX_STACKS);
    OrganStateOps.setInt(
        state,
        cc,
        organ,
        KEY_COMBO_COUNT,
        combo,
        value -> Mth.clamp(value, 0, MOMENTUM_MAX_STACKS),
        0);
    if (combo >= BURST_THRESHOLD && !session.burstConsumed) {
      OrganStateOps.setBoolean(state, cc, organ, KEY_PENDING_BURST, true, false);
    }

    spawnTargetFx(session.level, target, combo);

    if (session.hasSoulEcho) {
      spawnShadowTraverse(session, player, target);
      boolean chain = combo >= BURST_THRESHOLD;
      scheduleSpiritEcho(
          session.level,
          player,
          cc,
          organ,
          target,
          session.direction,
          calculateImpactDamage(cc, state, session) * SPIRIT_ECHO_DAMAGE_MULTIPLIER,
          chain);
    }

    if (session.burst) {
      applyBurstShockwave(session, player, cc, organ, state, target);
      session.burstConsumed = true;
    }
    return true;
  }

  private static double calculateImpactDamage(
      ChestCavityInstance cc, OrganState state, DashSession session) {
    double damage = BASE_IMPACT_DAMAGE;
    int momentum = Math.max(0, state.getInt(KEY_MOMENTUM_STACK, 0));
    damage *= (1.0 + momentum * DAMAGE_PER_STACK);
    damage *= (1.0 + Math.max(0.0, INSTANCE.liDaoIncrease(cc)));
    damage *= (1.0 + Math.max(0, INSTANCE.countMuscles(cc)) * MUSCLE_DAMAGE_BONUS);
    if (session.burst) {
      damage *= BURST_DAMAGE_MULTIPLIER;
    }
    return damage;
  }

  private static void applyKnockback(
      ServerPlayer player, LivingEntity target, OrganState state, DashSession session) {
    Vec3 dir = session.direction;
    int momentum = Math.max(0, state.getInt(KEY_MOMENTUM_STACK, 0));
    double knockback = BASE_KNOCKBACK + momentum * KNOCKBACK_PER_STACK;
    if (session.burst) {
      knockback *= BURST_KNOCKBACK_MULTIPLIER;
    }
    target.push(dir.x * knockback, 0.15 + (session.burst ? 0.25 : 0.08), dir.z * knockback);
    target.hurtMarked = true;
  }

  private static void applyBurstShockwave(
      DashSession session,
      ServerPlayer player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      LivingEntity primaryTarget) {
    ServerLevel level = session.level;
    AABB aoe = primaryTarget.getBoundingBox().inflate(BURST_AOE_RADIUS);
    List<LivingEntity> others =
        level.getEntitiesOfClass(
            LivingEntity.class,
            aoe,
            entity ->
                entity != player
                    && entity != primaryTarget
                    && CombatEntityUtil.areEnemies(player, entity));
    double damage = calculateImpactDamage(cc, state, session);
    DamageSource source = player.damageSources().playerAttack(player);
    for (LivingEntity target : others) {
      target.hurt(source, (float) (damage * 0.5));
      Vec3 push = target.position().subtract(player.position()).normalize();
      if (push.lengthSqr() > 1.0E-4) {
        target.push(push.x * 0.55, 0.25, push.z * 0.55);
        target.hurtMarked = true;
      }
    }
    level.playSound(
        null,
        primaryTarget.blockPosition(),
        SoundEvents.LIGHTNING_BOLT_THUNDER,
        SoundSource.PLAYERS,
        0.6F,
        1.35F);
    level.sendParticles(
        ParticleTypes.EXPLOSION,
        primaryTarget.getX(),
        primaryTarget.getY(),
        primaryTarget.getZ(),
        4,
        0.4,
        0.2,
        0.4,
        0.01);
  }

  private static void scheduleSpiritEcho(
      ServerLevel level,
      ServerPlayer player,
      ChestCavityInstance cc,
      ItemStack organ,
      LivingEntity target,
      Vec3 direction,
      double damage,
      boolean chain) {
    UUID playerId = player.getUUID();
    int targetId = target.getId();
    int organSlot = ChestCavityUtil.findOrganSlot(cc, organ);
    TickOps.schedule(
        level,
        () -> applySpiritEcho(level, playerId, targetId, organSlot, damage, direction, chain),
        SPIRIT_ECHO_DELAY_TICKS);
  }

  private static void applySpiritEcho(
      ServerLevel level,
      UUID playerId,
      int targetId,
      int organSlot,
      double damage,
      Vec3 direction,
      boolean chain) {
    ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
    if (player == null) {
      return;
    }
    ChestCavityInstance cc =
        ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
    if (cc == null) {
      return;
    }
    ItemStack organ = resolveOrgan(cc, organSlot);
    if (organ.isEmpty() || !INSTANCE.matchesOrgan(organ, ORGAN_ID)) {
      return;
    }
    OrganState state = INSTANCE.organState(organ, STATE_ROOT);
    MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ, state);

    Entity entity = level.getEntity(targetId);
    if (!(entity instanceof LivingEntity living)
        || !living.isAlive()
        || !CombatEntityUtil.areEnemies(player, living)) {
      return;
    }

    living.hurt(player.damageSources().playerAttack(player), (float) damage);
    living.push(direction.x * SPIRIT_ECHO_KNOCKBACK, 0.2, direction.z * SPIRIT_ECHO_KNOCKBACK);
    living.hurtMarked = true;
    level.playSound(
        null,
        living.blockPosition(),
        SoundEvents.SOUL_ESCAPE.value(),
        SoundSource.PLAYERS,
        0.5F,
        1.2F);
    level.sendParticles(
        ParticleTypes.SOUL,
        living.getX(),
        living.getEyeY(),
        living.getZ(),
        10,
        0.2,
        0.2,
        0.2,
        0.02);

    if (chain) {
      MultiCooldown.Entry cooldownEntry = cooldown.entry(KEY_COOLDOWN_READY_AT);
      long now = level.getGameTime();
      cooldownEntry.setReadyAt(now);
      ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, now, now);
      MultiCooldown.Entry momentumExpire = cooldown.entry(KEY_MOMENTUM_EXPIRE);
      momentumExpire.setReadyAt(now + MOMENTUM_DECAY_TICKS);
      int momentum = Mth.clamp(state.getInt(KEY_MOMENTUM_STACK, 0) + 1, 0, MOMENTUM_MAX_STACKS);
      OrganStateOps.setInt(
          state,
          cc,
          organ,
          KEY_MOMENTUM_STACK,
          momentum,
          value -> Mth.clamp(value, 0, MOMENTUM_MAX_STACKS),
          0);
    }
  }

  private static void endDash(
      DashSession session,
      ServerPlayer player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      long now,
      boolean aborted) {
    ACTIVE_DASHES.remove(session.playerId, session);
    cooldown.entry(KEY_DASH_ACTIVE_UNTIL).setReadyAt(0L);

    if (!session.hadImpact || aborted) {
      cooldown.entry(KEY_COOLDOWN_READY_AT).setReadyAt(now + FAIL_COOLDOWN_TICKS);
      ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, now + FAIL_COOLDOWN_TICKS, now);
      OrganStateOps.setInt(
          state,
          cc,
          organ,
          KEY_MOMENTUM_STACK,
          0,
          value -> Mth.clamp(value, 0, MOMENTUM_MAX_STACKS),
          0);
      OrganStateOps.setInt(
          state,
          cc,
          organ,
          KEY_COMBO_COUNT,
          0,
          value -> Mth.clamp(value, 0, MOMENTUM_MAX_STACKS),
          0);
      OrganStateOps.setBoolean(state, cc, organ, KEY_PENDING_BURST, false, false);
    }
  }

  private static void spawnDashStartFx(ServerLevel level, Player player) {
    level.playSound(
        null,
        player.blockPosition(),
        SoundEvents.ENDER_DRAGON_FLAP,
        SoundSource.PLAYERS,
        0.65F,
        0.9F);
    level.sendParticles(
        ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 8, 0.2, 0.1, 0.2, 0.05);
  }

  private static void spawnDashTrail(ServerLevel level, Player player) {
    level.sendParticles(
        TRAIL_PARTICLE,
        player.getX(),
        player.getY() + 0.2,
        player.getZ(),
        4,
        0.05,
        0.05,
        0.05,
        0.0);
  }

  private static void spawnImpactFx(ServerLevel level, Vec3 center) {
    level.playSound(
        null,
        center.x,
        center.y,
        center.z,
        SoundEvents.GENERIC_EXPLODE,
        SoundSource.PLAYERS,
        0.45F,
        1.1F);
    level.sendParticles(IMPACT_PARTICLE, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
  }

  private static void spawnTargetFx(ServerLevel level, LivingEntity target, int combo) {
    int count = Math.max(5, combo * 3);
    level.sendParticles(
        ParticleTypes.ELECTRIC_SPARK,
        target.getX(),
        target.getEyeY(),
        target.getZ(),
        count,
        0.25,
        0.4,
        0.25,
        0.05);
  }

  private static boolean hasSoulFlow(ChestCavityInstance cc, ServerLevel level) {
    if (cc == null || cc.inventory == null) {
      return false;
    }
    TooltipContext context = TooltipContext.of(level);
    TooltipFlag flag = TooltipFlag.NORMAL;
    for (int i = 0, size = cc.inventory.getContainerSize(); i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }
      GuzhenrenFlowTooltipResolver.FlowInfo info =
          GuzhenrenFlowTooltipResolver.inspect(stack, context, flag, null);
      if (!info.hasFlow()) {
        continue;
      }
      for (String flow : info.flows()) {
        if (flow != null && flow.toLowerCase(Locale.ROOT).contains(FLOW_KEY_YING_DAO)) {
          return true;
        }
      }
    }
    return false;
  }

  private static void spawnShadowTraverse(
      DashSession session, ServerPlayer player, LivingEntity target) {
    if (SHADOW_TRAVEL_TICKS <= 0) {
      return;
    }
    Vec3 start = session.startPosition == null ? player.position() : session.startPosition;
    Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
    int steps = Math.max(1, SHADOW_TRAVEL_TICKS);
    ShadowService.spawn(
            session.level,
            player,
            start,
            ShadowService.HEI_ZHU_CLONE,
            0.0f,
            steps + 6,
            clone -> {
              clone.setNoAi(true);
              clone.getNavigation().stop();
              clone.setDeltaMovement(Vec3.ZERO);
              clone.setYRot(player.getYRot());
              clone.setXRot(player.getXRot());
            })
        .ifPresent(
            clone -> {
              for (int i = 0; i <= steps; i++) {
                final int step = i;
                TickOps.schedule(
                    session.level,
                    () -> {
                      if (clone.isRemoved()) {
                        return;
                      }
                      double progress = step / (double) steps;
                      Vec3 pos = start.lerp(end, progress);
                      clone.setPos(pos.x, pos.y, pos.z);
                      clone.setDeltaMovement(Vec3.ZERO);
                      clone.setYRot(player.getYRot());
                      clone.setXRot(player.getXRot());
                      clone.yHeadRot = player.getYRot();
                      if (step == steps) {
                        clone.discard();
                      }
                    },
                    step);
              }
            });
  }

  private MultiCooldown createCooldown(
      @Nullable ChestCavityInstance cc, @Nullable ItemStack organ, OrganState state) {
    return MultiCooldown.builder(state)
        .withSync(cc, organ)
        .withLongClamp(value -> Math.max(0L, value), 0L)
        .build();
  }

  private static ItemStack findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (INSTANCE.matchesOrgan(stack, ORGAN_ID)) {
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }

  private static ItemStack resolveOrgan(ChestCavityInstance cc, int slot) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    if (slot >= 0 && slot < cc.inventory.getContainerSize()) {
      ItemStack stack = cc.inventory.getItem(slot);
      if (!stack.isEmpty() && INSTANCE.matchesOrgan(stack, ORGAN_ID)) {
        return stack;
      }
    }
    return findOrgan(cc);
  }

  private MultiCooldown createCooldown(
      @Nullable ChestCavityInstance cc, @Nullable ItemStack organ) {
    OrganState state = organState(organ, STATE_ROOT);
    return createCooldown(cc, organ, state);
  }

  private static final class DashSession {
    final UUID playerId;
    final ServerLevel level;
    final int organSlot;
    final ItemStack organRef;
    final long sequence;
    final Vec3 direction;
    final int durationTicks;
    final double speed;
    final boolean burst;
    final boolean hasSoulEcho;
    final long startTick;
    final Vec3 startPosition;

    boolean hadImpact;
    boolean burstConsumed;
    long lastImpactTick;
    final IntSet hitEntities = new IntOpenHashSet();

    DashSession(
        UUID playerId,
        ServerLevel level,
        int organSlot,
        ItemStack organRef,
        long sequence,
        Vec3 direction,
        int durationTicks,
        double speed,
        boolean burst,
        boolean hasSoulEcho,
        long startTick,
        Vec3 startPosition) {
      this.playerId = Objects.requireNonNull(playerId, "playerId");
      this.level = Objects.requireNonNull(level, "level");
      this.organSlot = organSlot;
      this.organRef = organRef;
      this.sequence = sequence;
      this.direction = direction.normalize();
      this.durationTicks = durationTicks;
      this.speed = speed;
      this.burst = burst;
      this.hasSoulEcho = hasSoulEcho;
      this.startTick = startTick;
      this.startPosition = startPosition == null ? Vec3.ZERO : startPosition;
    }

    ItemStack resolveOrgan(ChestCavityInstance cc) {
      ItemStack stack = ZhiZhuangGuOrganBehavior.resolveOrgan(cc, organSlot);
      if (stack.isEmpty() && organRef != null && INSTANCE.matchesOrgan(organRef, ORGAN_ID)) {
        return organRef;
      }
      return stack;
    }
  }
}
