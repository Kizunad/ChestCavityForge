package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.common.ShadowService;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SingleSwordProjectile;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.SwordShadowClone;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianYingTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JianYingCalculator;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.util.PlayerSkinUtil;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import org.apache.logging.log4j.Logger;
import net.tigereye.chestcavity.compat.common.skillcalc.DamageCalculator;
import net.tigereye.chestcavity.compat.common.skillcalc.DamageKind;
import net.tigereye.chestcavity.compat.common.skillcalc.DamageResult;

/** Behaviour for 剑影蛊. Handles passive shadow strikes, afterimages, and the sword clone ability. */
public enum JianYingGuOrganBehavior implements OrganOnHitListener {
  INSTANCE;

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_ying_gu");
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_ying_fenshen");
  private static final ResourceLocation SKILL_PASSIVE_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_dao/shadow_strike");
  private static final ResourceLocation SKILL_AFTERIMAGE_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_dao/afterimage");

  private static final ResourceLocation JIAN_DAO_INCREASE_EFFECT =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/jian_dao_increase_effect");

  private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

  private static final double PASSIVE_TRIGGER_CHANCE = 0.10;
  private static final String STATE_ROOT = "JianYingGu";
  private static final String ACTIVE_READY_KEY = "ActiveReadyAt";

  private static final double AFTERIMAGE_CHANCE = 0.1;
  private static final double AFTERIMAGE_RADIUS = 3.0;

  private static final Map<UUID, SwordShadowState> SWORD_STATES = new ConcurrentHashMap<>();
  private static final Map<UUID, ArrayDeque<Long>> COOLDOWN_HISTORY = new ConcurrentHashMap<>();
  private static final List<AfterimageTask> AFTERIMAGES =
      Collections.synchronizedList(new ArrayList<>());
  private static final Map<UUID, Long> EXTERNAL_CRITS = new ConcurrentHashMap<>();

  // Guard to avoid recursive re-entry when applying internal true damage which also fires
  // LivingIncomingDamageEvent and would otherwise loop back into onHit.
  private static final ThreadLocal<Boolean> REENTRY_GUARD =
      ThreadLocal.withInitial(() -> Boolean.FALSE);
  private static final Logger LOGGER = ChestCavity.LOGGER;
  private static final String ABILITY_LOG_PATTERN =
      "[compat/guzhenren][jian_dao][ability] {} owner={} reason={} {}";

  static {
    OrganActivationListeners.register(ABILITY_ID, JianYingGuOrganBehavior::activateAbility);
  }

  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (Boolean.TRUE.equals(REENTRY_GUARD.get())) {
      return damage;
    }
    if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
      return damage;
    }
    if (source == null || source.is(DamageTypeTags.IS_PROJECTILE)) {
      return damage;
    }
    if (target == null || !target.isAlive()) {
      return damage;
    }
    if (!isMeleeAttack(source)) {
      return damage;
    }

    double efficiency =
        1.0 + LedgerOps.ensureChannel(cc, JIAN_DAO_INCREASE_EFFECT, NON_NEGATIVE).get();

    double passiveDamage = triggerSwordShadow(player, target, efficiency);
    if (passiveDamage > 0.0) {
      applyTrueDamage(
          player,
          target,
          (float) passiveDamage,
          SKILL_PASSIVE_ID,
          java.util.Set.of(DamageKind.MELEE, DamageKind.TRUE_DAMAGE));
    }

    commandClones(player, target);

    trySpawnAfterimage(player, target, source);

    return damage;
  }

  public void ensureAttached(ChestCavityInstance cc) {
    LedgerOps.ensureChannel(cc, JIAN_DAO_INCREASE_EFFECT, NON_NEGATIVE);
  }

  public void tickLevel(ServerLevel level) {
    long gameTime = level.getGameTime();
    List<AfterimageTask> pending;
    synchronized (AFTERIMAGES) {
      pending = new ArrayList<>(AFTERIMAGES);
    }
    for (AfterimageTask task : pending) {
      if (!task.level().equals(level.dimension())) {
        continue;
      }
      if (task.executeTick() > gameTime) {
        continue;
      }
      if (executeAfterimage(level, task)) {
        AFTERIMAGES.remove(task);
      }
    }
  }

  private static boolean executeAfterimage(ServerLevel level, AfterimageTask task) {
    Player player = level.getPlayerByUUID(task.playerId());
    if (player == null) {
      return true;
    }

    ChestCavityInstance cc =
        ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
    double efficiency = 1.0;
    if (cc != null) {
      LinkageChannel channel =
          LedgerOps.ensureChannel(cc, JIAN_DAO_INCREASE_EFFECT, NON_NEGATIVE);
      efficiency += channel.get();
    }

    Vec3 centre = task.origin();
    AABB area = new AABB(centre, centre).inflate(AFTERIMAGE_RADIUS);
    List<LivingEntity> victims =
        level.getEntitiesOfClass(
            LivingEntity.class,
            area,
            entity -> entity.isAlive() && entity != player && !entity.isAlliedTo(player));
    if (victims.isEmpty()) {
      return true;
    }

    float damage = JianYingCalculator.afterimageDamage(efficiency);
    for (LivingEntity victim : victims) {
      applyTrueDamage(
          player,
          victim,
          damage,
          SKILL_AFTERIMAGE_ID,
          java.util.Set.of(DamageKind.MELEE, DamageKind.TRUE_DAMAGE));
      level.sendParticles(
          ParticleTypes.SWEEP_ATTACK,
          victim.getX(),
          victim.getY(0.5),
          victim.getZ(),
          2,
          0.1,
          0.1,
          0.1,
          0.01);
    }
    level.playSound(
        null,
        centre.x,
        centre.y,
        centre.z,
        SoundEvents.PLAYER_ATTACK_SWEEP,
        SoundSource.PLAYERS,
        0.7f,
        0.7f);

    return true;
  }

  private static void trySpawnAfterimage(Player player, LivingEntity target, DamageSource source) {
    if (player == null || target == null) {
      return;
    }
    if (!isCritical(player, source)) {
      return;
    }
    if (player.getRandom().nextDouble() >= AFTERIMAGE_CHANCE) {
      return;
    }
    Level level = player.level();
    if (level.isClientSide()) {
      return;
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
    AFTERIMAGES.add(
        new AfterimageTask(
            player.getUUID(),
            level.dimension(),
            level.getGameTime() + JianYingTuning.AFTERIMAGE_DELAY_TICKS,
            origin));
  }

  private static double triggerSwordShadow(Player player, LivingEntity target, double efficiency) {
    if (player.getRandom().nextDouble() >= PASSIVE_TRIGGER_CHANCE) {
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

  private static void commandClones(Player player, LivingEntity target) {
    if (!(player.level() instanceof ServerLevel server)) {
      return;
    }
    ShadowService.commandOwnedClones(server, player, target);
  }

  private static Vec3 randomOffset(RandomSource random) {
    double radius = 1.2 + random.nextDouble() * 0.4;
    double angle = random.nextDouble() * Math.PI * 2.0;
    return new Vec3(Math.cos(angle) * radius, 0.2, Math.sin(angle) * radius);
  }

  private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
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
    if (!hasOrgan(cc)) {
      boolean opened = cc != null && cc.opened;
      logAbility(player, "EXIT", "missing_organ", "chest_cavity_opened=" + opened);
      return;
    }
    long now = entity.level().getGameTime();
    int organCount = countOrgans(cc);
    if (organCount <= 0) {
      logAbility(player, "EXIT", "missing_organ", "count=0 AFTER verification");
      return;
    }

    ArrayDeque<Long> history =
        COOLDOWN_HISTORY.computeIfAbsent(player.getUUID(), key -> new ArrayDeque<>());
    if (!history.isEmpty()) {
      long head = history.peekFirst();
      if (now < head) {
        logAbility(
            player, "WARN", "time_skew", String.format(Locale.ROOT, "head=%d now=%d", head, now));
        history.clear();
      } else {
        while (!history.isEmpty()) {
          head = history.peekFirst();
          long delta = now - head;
          if (delta >= JianYingTuning.CLONE_COOLDOWN_TICKS) {
            history.pollFirst();
          } else {
            break;
          }
        }
      }
    }

    while (history.size() > organCount) {
      history.pollFirst();
    }

    if (history.size() >= organCount && !history.isEmpty()) {
      long head = history.peekFirst();
      long elapsed = now - head;
      if (elapsed < JianYingTuning.CLONE_COOLDOWN_TICKS) {
        long remaining = JianYingTuning.CLONE_COOLDOWN_TICKS - elapsed;
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

    history.addLast(now);
    while (history.size() > organCount) {
      history.pollFirst();
    }

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

    // 使用 MultiCooldown 记录冷却，同时借助 ActiveSkillRegistry 统一调度就绪提示
    if (player instanceof ServerPlayer sp) {
      ItemStack organIcon = findOrgan(cc);
      ItemStack stateStack =
          organIcon.isEmpty()
              ? new ItemStack(net.tigereye.chestcavity.registration.CCItems.GUZHENREN_JIAN_YING_GU)
              : organIcon;
      MultiCooldown cooldown =
          MultiCooldown.builder(OrganState.of(stateStack, STATE_ROOT))
              .withSync(cc, stateStack)
              .build();
      long nowTick = server.getGameTime();
      MultiCooldown.Entry ready = cooldown.entry(ACTIVE_READY_KEY);
      long readyAt = nowTick + JianYingTuning.CLONE_COOLDOWN_TICKS;
      ready.setReadyAt(readyAt);
      ActiveSkillRegistry.scheduleReadyToast(sp, ABILITY_ID, readyAt, nowTick);
    }
  }

  private static boolean hasOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return false;
    }
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (ORGAN_ID.equals(id)) {
        return true;
      }
    }
    return false;
  }

  private static int countOrgans(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return 0;
    }
    int total = 0;
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (ORGAN_ID.equals(id)) {
        total += Math.max(1, stack.getCount());
      }
    }
    return total;
  }

  private static ItemStack findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (ORGAN_ID.equals(id)) {
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }

  // Expose cooldown head for external watchers (server-side toast on countdown end)
  public static Long peekCooldownHead(UUID playerId) {
    var q = COOLDOWN_HISTORY.get(playerId);
    if (q == null || q.isEmpty()) return null;
    return q.peekFirst();
  }

  public static int getCloneCooldownTicks() {
    return JianYingTuning.CLONE_COOLDOWN_TICKS;
  }

  private static boolean isMeleeAttack(DamageSource source) {
    return !source.is(DamageTypeTags.IS_PROJECTILE);
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

  public static void applyTrueDamage(Player player, LivingEntity target, float amount) {
    // 默认视为被动近战斩击
    applyTrueDamage(
        player,
        target,
        amount,
        SKILL_PASSIVE_ID,
        java.util.Set.of(DamageKind.MELEE, DamageKind.TRUE_DAMAGE));
  }

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
    // 按“准真实伤害”方式应用修正后的伤害
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
            .filter(JianYingGuOrganBehavior::hasOrgan)
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

  private static final class SwordShadowState {
    private long lastTriggerTick;
    private float lastMultiplier = JianYingTuning.PASSIVE_INITIAL_MULTIPLIER;
  }

  private record AfterimageTask(
      UUID playerId, ResourceKey<Level> level, long executeTick, Vec3 origin) {}

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
}
