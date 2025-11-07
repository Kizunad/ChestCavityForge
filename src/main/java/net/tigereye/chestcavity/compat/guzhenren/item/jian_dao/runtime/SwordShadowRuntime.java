package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.runtime;

import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.skillcalc.DamageCalculator;
import net.tigereye.chestcavity.compat.common.skillcalc.DamageKind;
import net.tigereye.chestcavity.compat.common.skillcalc.DamageResult;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.common.ShadowService;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JianYingCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SingleSwordProjectile;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SwordShadowClone;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianYingTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.util.PlayerSkinUtil;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.skill.effects.SkillEffectBus;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import org.apache.logging.log4j.Logger;

/**
 * 剑影蛊执行层（Runtime）：不包含数学公式，专注于 Minecraft 世界的副作用与逻辑调度。
 *
 * <p>依赖 ShadowService、ResourceOps、LedgerOps、MultiCooldown、JianYingCalculator。
 */
public final class SwordShadowRuntime {

  private static final ResourceLocation JIAN_DAO_INCREASE_EFFECT =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "linkage/jian_dao_increase_effect");
  private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

  private static final ResourceLocation SKILL_PASSIVE_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "jian_dao/shadow_strike");
  private static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "jian_ying_fenshen");

  private static final String STATE_ROOT = "JianYingGu";
  private static final String ACTIVE_READY_KEY = "ActiveReadyAt";

  private static final Map<UUID, SwordShadowState> SWORD_STATES = new ConcurrentHashMap<>();
  private static final Map<UUID, ArrayDeque<Long>> COOLDOWN_HISTORY = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> EXTERNAL_CRITS = new ConcurrentHashMap<>();

  // 伤害重入保护
  private static final ThreadLocal<Boolean> REENTRY_GUARD =
      ThreadLocal.withInitial(() -> Boolean.FALSE);

  private static final Logger LOGGER = ChestCavity.LOGGER;
  private static final String ABILITY_LOG_PATTERN =
      "[compat/guzhenren][jian_dao][ability] {} owner={} reason={} {}";

  private SwordShadowRuntime() {}

  /**
   * 尝试触发被动影袭。
   *
   * @param player 攻击者
   * @param target 目标
   * @param efficiency 家族增伤效率
   * @return 实际造成的伤害值（0 表示未触发）
   */
  public static double attemptPassiveStrike(Player player, LivingEntity target, double efficiency) {
    if (player.getRandom().nextDouble() >= JianYingTuning.PASSIVE_TRIGGER_CHANCE) {
      return 0.0;
    }
    OptionalDouble consumed =
        ResourceOps.tryConsumeTieredZhenyuan(
            player,
            JianYingTuning.DESIGN_ZHUANSHU,
            JianYingTuning.DESIGN_JIEDUAN,
            JianYingTuning.PASSIVE_ZHENYUAN_TIER);
    if (consumed.isEmpty()) {
      return 0.0;
    }

    long now = player.level().getGameTime();
    SwordShadowState state =
        SWORD_STATES.computeIfAbsent(player.getUUID(), unused -> new SwordShadowState());
    float multiplier =
        JianYingCalculator.passiveMultiplier(state.lastTriggerTick, now, state.lastMultiplier);
    state.lastTriggerTick = now;
    state.lastMultiplier = multiplier;

    PlayerSkinUtil.SkinSnapshot tint =
        ShadowService.captureTint(player, ShadowService.JIAN_DAO_CLONE);
    ItemStack display = ShadowService.resolveDisplayStack(player);
    Vec3 anchor = ShadowService.swordAnchor(player);
    Vec3 tip = ShadowService.swordTip(player, anchor);
    SingleSwordProjectile.spawn(player.level(), player, anchor, tip, tint, display);

    return JianYingTuning.BASE_DAMAGE * multiplier * efficiency;
  }

  /**
   * 尝试生成残影（暴击时有几率触发）。
   *
   * @param player 玩家
   * @param target 目标
   * @param source 伤害来源
   * @return 是否成功生成残影
   */
  public static boolean trySpawnAfterimage(
      Player player, LivingEntity target, DamageSource source) {
    if (player == null || target == null) {
      return false;
    }
    if (!isCritical(player, source)) {
      return false;
    }
    if (player.getRandom().nextDouble() >= JianYingTuning.AFTERIMAGE_CHANCE) {
      return false;
    }
    Level level = player.level();
    if (level.isClientSide()) {
      return false;
    }
    Vec3 origin = target.position();
    PlayerSkinUtil.SkinSnapshot tinted =
        ShadowService.captureTint(player, ShadowService.JIAN_DAO_AFTERIMAGE);
    ItemStack display = ShadowService.resolveDisplayStack(player);
    Vec3 anchor = ShadowService.swordAnchor(player);
    Vec3 tip = ShadowService.swordTip(player, anchor);
    SingleSwordProjectile.spawn(level, player, anchor, tip, tinted, display);
    if (level instanceof ServerLevel server) {
      Vec3 spawnPos = player.position().add(player.getLookAngle().scale(0.3)).add(0.0, 0.1, 0.0);
      ShadowService.spawn(
          server,
          player,
          spawnPos,
          ShadowService.JIAN_DAO_AFTERIMAGE,
          0.0f,
          JianYingTuning.AFTERIMAGE_DURATION_TICKS);
    }
    AfterimageScheduler.queueAfterimage(
        player.getUUID(),
        level.dimension(),
        level.getGameTime() + JianYingTuning.AFTERIMAGE_DELAY_TICKS,
        origin);
    return true;
  }

  /**
   * 激活主动技能：召唤剑影分身。
   *
   * @param entity 实体
   * @param cc 胸腔实例
   * @param organStack 器官物品栈（用于冷却状态存储）
   * @param organCount 器官数量
   */
  public static void activateClone(
      LivingEntity entity, ChestCavityInstance cc, ItemStack organStack, int organCount) {
    if (!(entity instanceof Player player)) {
      logAbility(null, "EXIT", "non_player", "entity_type=" + entity.getType());
      return;
    }
    if (entity.level().isClientSide()) {
      logAbility(player, "EXIT", "client_side", "ignored client invocation");
      return;
    }
    logAbility(
        player,
        "ENTER",
        "attempt",
        String.format(Locale.ROOT, "tick=%d", entity.level().getGameTime()));

    long now = entity.level().getGameTime();
    if (organCount <= 0) {
      logAbility(player, "EXIT", "missing_organ", "count=0 AFTER verification");
      return;
    }

    ArrayDeque<Long> history =
        COOLDOWN_HISTORY.computeIfAbsent(player.getUUID(), key -> new ArrayDeque<>());
    boolean allowed =
        windowAcceptAndRecord(history, now, Math.max(1, organCount), JianYingTuning.CLONE_COOLDOWN_TICKS);
    if (!allowed) {
      long head = history.isEmpty() ? now : history.peekFirst();
      long elapsed = now - head;
      long remaining = Math.max(0, JianYingTuning.CLONE_COOLDOWN_TICKS - elapsed);
      logAbility(
          player,
          "EXIT",
          "cooldown",
          String.format(
              Locale.ROOT,
              "remaining=%d in_use=%d limit=%d",
              remaining,
              history.size(),
              organCount));
      return;
    }

    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt =
        GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      logAbility(player, "EXIT", "resource_handle_missing", "bridge_closed=true");
      return;
    }
    GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();

    OptionalDouble jingliBeforeOpt = handle.getJingli();
    OptionalDouble zhenBeforeOpt = handle.getZhenyuan();
    if (jingliBeforeOpt.isEmpty()) {
      logAbility(player, "EXIT", "jingli_unavailable", "attachment_missing=true");
      return;
    }
    double jingliBefore = jingliBeforeOpt.getAsDouble();
    if (jingliBefore < JianYingTuning.ACTIVE_JINGLI_COST) {
      logAbility(
          player,
          "EXIT",
          "jingli_insufficient",
          String.format(
              Locale.ROOT, "have=%.1f required=%.1f", jingliBefore, JianYingTuning.ACTIVE_JINGLI_COST));
      return;
    }

    OptionalDouble jingliAfterOpt =
        ResourceOps.tryAdjustJingli(handle, -JianYingTuning.ACTIVE_JINGLI_COST, true);
    if (jingliAfterOpt.isEmpty()) {
      logAbility(
          player,
          "EXIT",
          "jingli_adjust_failed",
          String.format(Locale.ROOT, "start=%.1f", jingliBefore));
      return;
    }
    OptionalDouble zhenAfterOpt =
        ResourceOps.tryConsumeTieredZhenyuan(
            handle,
            JianYingTuning.DESIGN_ZHUANSHU,
            JianYingTuning.DESIGN_JIEDUAN,
            JianYingTuning.ACTIVE_ZHENYUAN_TIER);
    if (zhenAfterOpt.isEmpty()) {
      ResourceOps.trySetJingli(handle, jingliBefore);
      String detail;
      if (zhenBeforeOpt.isPresent()) {
        detail =
            String.format(
                Locale.ROOT,
                "have=%.1f required=%.1f",
                zhenBeforeOpt.getAsDouble(),
                -1.0);
      } else {
        detail = "zhenyuan_unreadable";
      }
      logAbility(player, "EXIT", "zhenyuan_insufficient", detail);
      return;
    }

    int clones = 2 + player.getRandom().nextInt(2);
    double efficiency = 1.0;
    if (cc != null) {
      efficiency +=
          LedgerOps.ensureChannel(cc, JIAN_DAO_INCREASE_EFFECT, NON_NEGATIVE).get();
    }

    Level level = player.level();
    if (!(level instanceof ServerLevel server)) {
      logAbility(player, "EXIT", "non_server_level", level.getClass().getSimpleName());
      return;
    }

    // 参数快照优先：在 ActivationHookRegistry 中已注册 jiandao:* 字段的快照
    if (player instanceof ServerPlayer sp) {
      double snapDaoHen =
          SkillEffectBus.consumeMetadata(sp, ABILITY_ID, "jiandao:daohen_jiandao", Double.NaN);
      double snapLiupai =
          SkillEffectBus.consumeMetadata(sp, ABILITY_ID, "jiandao:liupai_jiandao", Double.NaN);
      // 当前数值未用于计算，仅完成“优先读取快照”的约定，便于后续数值迭代
      if (Double.isFinite(snapDaoHen) || Double.isFinite(snapLiupai)) {
        // no-op
      }
    }

    PlayerSkinUtil.SkinSnapshot tint =
        PlayerSkinUtil.withTint(PlayerSkinUtil.capture(player), 0.05f, 0.05f, 0.1f, 0.55f);
    float cloneDamage = JianYingCalculator.cloneDamage(efficiency);
    RandomSource random = player.getRandom();
    int spawned = 0;
    for (int i = 0; i < clones; i++) {
      Vec3 offset = randomOffset(random);
      Vec3 spawnPos = player.position().add(offset);
      SwordShadowClone clone = SwordShadowClone.spawn(server, player, spawnPos, tint, cloneDamage);
      if (clone != null) {
        clone.setLifetime(JianYingTuning.CLONE_DURATION_TICKS);
        spawned++;
      }
    }

    // 历史记录已在 windowAcceptAndRecord 中更新

    level.playSound(
        null,
        player.getX(),
        player.getY(),
        player.getZ(),
        SoundEvents.ILLUSIONER_PREPARE_MIRROR,
        SoundSource.PLAYERS,
        0.8f,
        0.6f);
    server.sendParticles(
        ParticleTypes.PORTAL,
        player.getX(),
        player.getY(0.5),
        player.getZ(),
        30,
        0.4,
        0.6,
        0.4,
        0.2);
    server.sendParticles(
        ParticleTypes.LARGE_SMOKE,
        player.getX(),
        player.getY(0.4),
        player.getZ(),
        20,
        0.35,
        0.35,
        0.35,
        0.01);

    double jingliAfter = jingliAfterOpt.getAsDouble();
    double jingliSpent = jingliBefore - jingliAfter;
    double zhenSpent =
        zhenBeforeOpt.isPresent() && zhenAfterOpt.isPresent()
            ? Math.max(0.0, zhenBeforeOpt.getAsDouble() - zhenAfterOpt.getAsDouble())
            : 0.0;
    logAbility(
        player,
        "EXIT",
        "success",
        String.format(
            Locale.ROOT,
            "spawned=%d damage=%.1f eff=%.3f jingli_spent=%.1f zhenyuan_spent=%.1f",
            spawned,
            cloneDamage,
            efficiency,
            jingliSpent,
            zhenSpent));

    // 使用 MultiCooldown 记录冷却
    if (player instanceof ServerPlayer sp) {
      MultiCooldown cooldown =
          MultiCooldown.builder(OrganState.of(organStack, STATE_ROOT))
              .withSync(cc, organStack)
              .build();
      long nowTick = server.getGameTime();
      MultiCooldown.Entry ready = cooldown.entry(ACTIVE_READY_KEY);
      long readyAt = nowTick + JianYingTuning.CLONE_COOLDOWN_TICKS;
      ready.setReadyAt(readyAt);
      ActiveSkillRegistry.scheduleReadyToast(sp, ABILITY_ID, readyAt, nowTick);
    }
  }

  /**
   * 纯函数：滑动窗口限并发判定。按容量与冷却窗口裁剪历史，并在允许时记录本次时间戳。
   *
   * @param history 时间戳队列（升序，队首为最早一次）
   * @param now 当前tick
   * @param capacity 同时占用的最大数量（通常=器官数，最小为1）
   * @param cooldownTicks 冷却窗口（每个占用之间的最小间隔）
   * @return true 表示通过并已记录；false 表示拒绝，不修改记录
   */
  public static boolean windowAcceptAndRecord(
      ArrayDeque<Long> history, long now, int capacity, int cooldownTicks) {
    if (history == null) {
      return true;
    }
    if (capacity < 1) capacity = 1;
    if (cooldownTicks < 0) cooldownTicks = 0;

    // 时间回退保护：若 now 小于队首，视为时间偏斜，清空历史
    if (!history.isEmpty() && now < history.peekFirst()) {
      history.clear();
    }

    // 清理过期记录（超过冷却窗口）
    while (!history.isEmpty()) {
      long head = history.peekFirst();
      long delta = now - head;
      if (delta >= cooldownTicks) {
        history.pollFirst();
      } else {
        break;
      }
    }

    // 若当前占用已达容量，则拒绝（仍保持历史不变）
    if (history.size() >= capacity) {
      return false;
    }

    // 记录本次占用
    history.addLast(now);
    // 防御性裁剪（一般不会触发）
    while (history.size() > capacity) {
      history.pollFirst();
    }
    return true;
  }

  /**
   * 指挥分身攻击目标。
   *
   * @param player 玩家
   * @param target 目标
   */
  public static void commandClones(Player player, LivingEntity target) {
    if (!(player.level() instanceof ServerLevel server)) {
      return;
    }
    ShadowService.commandOwnedClones(server, player, target);
  }

  /**
   * 应用准真实伤害（默认为被动近战斩击）。
   *
   * @param player 攻击者
   * @param target 目标
   * @param amount 伤害量
   */
  public static void applyTrueDamage(Player player, LivingEntity target, float amount) {
    applyTrueDamage(
        player,
        target,
        amount,
        SKILL_PASSIVE_ID,
        java.util.Set.of(DamageKind.MELEE, DamageKind.TRUE_DAMAGE));
  }

  /**
   * 应用准真实伤害。
   *
   * @param player 攻击者
   * @param target 目标
   * @param baseAmount 基础伤害
   * @param skillId 技能 ID
   * @param kinds 伤害类型集合
   */
  public static void applyTrueDamage(
      Player player, LivingEntity target, float baseAmount, ResourceLocation skillId, Set<DamageKind> kinds) {
    if (target == null || baseAmount <= 0.0f) {
      return;
    }
    double scaled = baseAmount;
    try {
      long castId = player == null ? 0L : player.level().getGameTime();
      DamageResult result =
          DamageCalculator.compute(
              player == null ? target : player,
              target,
              baseAmount,
              skillId == null ? SKILL_PASSIVE_ID : skillId,
              castId,
              kinds == null ? java.util.Set.of() : kinds);
      scaled = Math.max(0.0, result.scaled());
    } catch (Throwable ignored) {}
    doApplyTrueDamage(player, target, (float) scaled);
  }

  private static void doApplyTrueDamage(Player player, LivingEntity target, float amount) {
    if (Boolean.TRUE.equals(REENTRY_GUARD.get())) {
      return;
    }
    REENTRY_GUARD.set(true);
    try {
      float startHealth = target.getHealth();
      float startAbsorption = target.getAbsorptionAmount();
      target.invulnerableTime = 0;
      DamageSource source =
          player instanceof ServerPlayer serverPlayer
              ? target.damageSources().playerAttack(serverPlayer)
              : target.damageSources().magic();
      target.hurt(source, amount);
      target.invulnerableTime = 0;

      float remaining = amount;
      float absorbed = Math.min(startAbsorption, remaining);
      remaining -= absorbed;
      target.setAbsorptionAmount(Math.max(0.0f, startAbsorption - absorbed));

      if (!target.isDeadOrDying() && remaining > 0.0f) {
        float expected = Math.max(0.0f, startHealth - remaining);
        if (target.getHealth() > expected) {
          target.setHealth(expected);
        }
      }
      target.hurtTime = 0;
      if (player != null && !target.level().isClientSide()) {
        ChestCavityEntity.of(player)
            .map(ChestCavityEntity::getChestCavityInstance)
            .ifPresent(
                ccIgnored ->
                    ReactionTagOps.add(
                        target,
                        ReactionTagKeys.SWORD_SCAR,
                        JianYingTuning.SWORD_SCAR_DURATION_TICKS));
      }
    } finally {
      REENTRY_GUARD.remove();
    }
  }

  private static Vec3 randomOffset(RandomSource random) {
    double radius = 1.2 + random.nextDouble() * 0.4;
    double angle = random.nextDouble() * Math.PI * 2.0;
    return new Vec3(Math.cos(angle) * radius, 0.2, Math.sin(angle) * radius);
  }

  private static boolean isCritical(Player player, DamageSource source) {
    if (player == null) {
      return false;
    }
    Long stamp = EXTERNAL_CRITS.remove(player.getUUID());
    if (stamp != null) {
      long now = player.level().getGameTime();
      if (now - stamp <= 2L) {
        return true;
      }
    }
    if (source != null && source.is(DamageTypeTags.BYPASSES_ARMOR)) {
      return false;
    }
    boolean airborne = !player.onGround() && !player.onClimbable() && !player.isInWaterOrBubble();
    boolean strongSwing = player.getAttackStrengthScale(0.5f) > 0.9f;
    boolean descending = player.getDeltaMovement().y < 0.0;
    return airborne && strongSwing && descending && !player.isSprinting();
  }

  public static void markExternalCrit(Player player) {
    if (player == null) {
      return;
    }
    EXTERNAL_CRITS.put(player.getUUID(), player.level().getGameTime());
  }

  private static void logAbility(
      @Nullable Player player, String phase, String reason, String details) {
    String owner = player != null ? player.getScoreboardName() : "?";
    String info = details == null ? "-" : details;
    if ("WARN".equalsIgnoreCase(phase)) {
      LOGGER.warn(ABILITY_LOG_PATTERN, phase, owner, reason, info);
      return;
    }
    LOGGER.debug(ABILITY_LOG_PATTERN, phase, owner, reason, info);
  }

  private static final class SwordShadowState {
    private long lastTriggerTick;
    private float lastMultiplier = JianYingTuning.PASSIVE_INITIAL_MULTIPLIER;
  }
}
