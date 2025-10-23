package net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.AbstractLiDaoOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.util.CombatEntityUtil;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.AttributeOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper.ConsumptionResult;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 熊豪蛊（力道·肌肉位）核心行为。
 *
 * <p>本实现按照《熊豪蛊·计划文档》要求，实现下列能力：
 *
 * <ul>
 *   <li>三个主动技：豪力爆发、破城重锤、威压怒吼。
 *   <li>被动：熊脊稳固、血热激昂、豪力惯性。
 *   <li>阶段进阶（HXP）及提示。
 * </ul>
 *
 * <p>代码力求保持注释齐备，便于后续扩展联动或继续强化阶段逻辑。
 */
public final class XiongHaoGuOrganBehavior extends AbstractLiDaoOrganBehavior
    implements OrganOnHitListener, OrganSlowTickListener, OrganIncomingDamageListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(XiongHaoGuOrganBehavior.class);

  public static final XiongHaoGuOrganBehavior INSTANCE = new XiongHaoGuOrganBehavior();

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "xiong_hao_gu");

  public static final ResourceLocation BURST_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "xiong_hao_burst");
  public static final ResourceLocation SLAM_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "xiong_hao_slam");
  public static final ResourceLocation ROAR_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "xiong_hao_roar");

  private static final String STATE_ROOT = "XiongHaoGu";
  private static final String KEY_BURST_ACTIVE = "BurstActive";
  private static final String KEY_BURST_EXPIRE = "BurstExpire";
  private static final String KEY_BURST_READY = "BurstReady";
  private static final String KEY_BURST_STACK = "BurstStacks";
  private static final String KEY_SLAM_READY = "SlamReady";
  private static final String KEY_ROAR_READY = "RoarReady";
  private static final String KEY_STAGE = "Stage";
  private static final String KEY_STAGE_XP = "StageXp";
  private static final String KEY_STAGE_TOAST = "StageToast";
  private static final String KEY_SPRINT_SINCE = "SprintSince";
  private static final String KEY_INERTIA_READY = "InertiaReady";
  private static final String KEY_HOT_BLOOD_UNTIL = "HotBloodUntil";
  private static final String KEY_HOT_BLOOD_CD = "HotBloodCooldown";
  private static final String KEY_FAIL_COOLDOWN = "FailCooldown";

  private static final ResourceLocation HOT_BLOOD_SPEED_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "xiong_hao_hot_blood_speed");
  private static final ResourceLocation HOT_BLOOD_DAMAGE_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "xiong_hao_hot_blood_damage");
  private static final ResourceLocation KNOCKBACK_RESIST_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "xiong_hao_knockback");

  private static final int BURST_DURATION_TICKS =
      BehaviorConfigAccess.getInt(XiongHaoGuOrganBehavior.class, "BURST_DURATION_TICKS", 10 * 20);
  private static final int BURST_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(XiongHaoGuOrganBehavior.class, "BURST_COOLDOWN_TICKS", 25 * 20);
  private static final double BURST_ZHENYUAN_BASE =
      BehaviorConfigAccess.getFloat(XiongHaoGuOrganBehavior.class, "BURST_ZHENYUAN_BASE", 300.0f);
  private static final double BURST_PER_ATTACK_JINGLI =
      BehaviorConfigAccess.getFloat(XiongHaoGuOrganBehavior.class, "BURST_PER_ATTACK_JINGLI", 6.0f);
  private static final float BURST_EXTRA_DAMAGE =
      BehaviorConfigAccess.getFloat(XiongHaoGuOrganBehavior.class, "BURST_EXTRA_DAMAGE", 10.0f);
  private static final float BURST_EXTRA_DAMAGE_STAGE4 = 12.0f;

  private static final double SLAM_BASE_DAMAGE = 10.0;
  private static final double SLAM_EDGE_RATIO = 0.6;
  private static final double SLAM_ZHENYUAN_BASE = 120.0;
  private static final double SLAM_JINGLI = 8.0;
  private static final double SLAM_BAOSHI = 2.0;
  private static final int SLAM_COOLDOWN_TICKS = 14 * 20;
  private static final double SLAM_KNOCKBACK_BASE = 0.6;
  private static final double SLAM_KNOCKBACK_BONUS = 0.3;
  private static final double SLAM_DAMAGE_PER_KNOCKBACK = 0.1;
  private static final double SLAM_DAMAGE_BONUS_CAP = 0.3;
  private static final int SLAM_REFUND_COUNT = 3;
  private static final double SLAM_REFUND_JINGLI = 4.0;

  private static final double ROAR_ZHENYUAN_BASE = 80.0;
  private static final double ROAR_NIANTOU = 5.0;
  private static final double ROAR_HUNPO = 2.0;
  private static final int ROAR_COOLDOWN_TICKS = 20 * 20;
  private static final int ROAR_DURATION_TICKS = 4 * 20;
  private static final double ROAR_MOVE_SLOW = 0.2;
  private static final double ROAR_HATE_SHIFT = 0.15;
  private static final int ROAR_RANGE = 5;

  private static final double HOT_BLOOD_SPEED = 0.05;
  private static final double HOT_BLOOD_DAMAGE = 2.0;
  private static final int HOT_BLOOD_DURATION = 6 * 20;
  private static final int HOT_BLOOD_DURATION_ELITE = 10 * 20;
  private static final int HOT_BLOOD_COOLDOWN = 30 * 20;

  private static final double INERTIA_DAMAGE_MULT = 0.15;
  private static final double INERTIA_KNOCKBACK_BONUS = 0.1;
  private static final int INERTIA_CHARGE_TICKS = 24; // 1.2 秒
  private static final int INERTIA_COOLDOWN_TICKS = 8 * 20;

  private static final double KNOCKBACK_RESIST_BONUS = 0.2;
  private static final double FALL_REDUCTION = 0.1;

  private static final int FAIL_COOLDOWN_TICKS = 12;

  private static final EnumMap<Stage, Integer> STAGE_THRESHOLDS = new EnumMap<>(Stage.class);
  private static boolean eventsRegistered;

  static {
    STAGE_THRESHOLDS.put(Stage.STAGE_ONE, 120);
    STAGE_THRESHOLDS.put(Stage.STAGE_TWO, 160);
    STAGE_THRESHOLDS.put(Stage.STAGE_THREE, 180);
    STAGE_THRESHOLDS.put(Stage.STAGE_FOUR, 200);
    OrganActivationListeners.register(
        BURST_ID,
        (entity, cc) -> {
          if (!(entity instanceof Player player) || cc == null) {
            return;
          }
          ItemStack organ = findOrgan(cc);
          if (!organ.isEmpty()) {
            INSTANCE.activateBurst(player, cc, organ);
          }
        });
    OrganActivationListeners.register(
        SLAM_ID,
        (entity, cc) -> {
          if (!(entity instanceof Player player) || cc == null) {
            return;
          }
          ItemStack organ = findOrgan(cc);
          if (!organ.isEmpty()) {
            INSTANCE.activateSlam(player, cc, organ);
          }
        });
    OrganActivationListeners.register(
        ROAR_ID,
        (entity, cc) -> {
          if (!(entity instanceof Player player) || cc == null) {
            return;
          }
          ItemStack organ = findOrgan(cc);
          if (!organ.isEmpty()) {
            INSTANCE.activateRoar(player, cc, organ);
          }
        });
  }

  private XiongHaoGuOrganBehavior() {
    registerEvents();
  }

  /** 表示熊豪蛊的阶段。 */
  public enum Stage {
    STAGE_ONE(1),
    STAGE_TWO(2),
    STAGE_THREE(3),
    STAGE_FOUR(4);

    private final int value;

    Stage(int value) {
      this.value = value;
    }

    public int value() {
      return value;
    }

    public static Stage fromValue(int value) {
      for (Stage stage : values()) {
        if (stage.value == value) {
          return stage;
        }
      }
      return STAGE_ONE;
    }
  }

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof Player player)
        || player.level().isClientSide()
        || cc == null
        || organ == null
        || organ.isEmpty()) {
      return;
    }
    if (!matchesOrgan(organ, ORGAN_ID) || !isPrimaryOrgan(cc, organ)) {
      return;
    }

    OrganState state = organState(organ, STATE_ROOT);
    MultiCooldown cooldown = createCooldown(cc, organ);
    long now = player.level().getGameTime();

    tickBurst(player, cc, organ, state, cooldown, now);
    tickHotBlood(player, cc, organ, state, now);
    maintainKnockback(player);
    tickStage(player, cc, organ, state, now);
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
    if (damage <= 0.0f
        || target == null
        || !target.isAlive()
        || target == attacker
        || target.isAlliedTo(attacker)) {
      return damage;
    }
    if (source == null
        || source.getDirectEntity() != attacker
        || source.is(DamageTypeTags.IS_PROJECTILE)) {
      return damage;
    }

    OrganState state = organState(organ, STATE_ROOT);
    MultiCooldown cooldown = createCooldown(cc, organ);
    long now = attacker.level().getGameTime();

    float extra = applyBurstBonus(player, cc, organ, state, cooldown, now);
    extra += applyInertiaBonus(player, cc, organ, state, cooldown, now, target);
    grantHitXp(player, state, cooldown, now);
    return Math.max(0.0f, damage + extra);
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
    if (!matchesOrgan(organ, ORGAN_ID) || !isPrimaryOrgan(cc, organ)) {
      return damage;
    }
    if (source != null && source.is(DamageTypeTags.IS_FALL)) {
      return damage * (1.0f - (float) FALL_REDUCTION);
    }
    return damage;
  }

  private void activateBurst(Player player, ChestCavityInstance cc, ItemStack organ) {
    OrganState state = organState(organ, STATE_ROOT);
    MultiCooldown cooldown = createCooldown(cc, organ);
    long now = player.level().getGameTime();
    MultiCooldown.Entry failGate = cooldown.entry(KEY_FAIL_COOLDOWN);
    if (failGate.getReadyTick() > now) {
      return;
    }
    MultiCooldown.Entry readyEntry = cooldown.entry(KEY_BURST_READY);
    if (readyEntry.getReadyTick() > now) {
      return;
    }
    if (ResourceOps.tryConsumeScaledZhenyuan(player, BURST_ZHENYUAN_BASE).isEmpty()) {
      failGate.setReadyAt(now + FAIL_COOLDOWN_TICKS);
      return;
    }

    OrganStateOps.setBoolean(state, cc, organ, KEY_BURST_ACTIVE, true, false);
    OrganStateOps.setLong(
        state, cc, organ, KEY_BURST_EXPIRE, now + BURST_DURATION_TICKS, v -> Math.max(0L, v), 0L);
    OrganStateOps.setInt(state, cc, organ, KEY_BURST_STACK, 0, v -> Math.max(0, v), 0);
    long readyAt = now + BURST_COOLDOWN_TICKS;
    readyEntry.setReadyAt(readyAt);

    if (player instanceof ServerPlayer sp) {
      ActiveSkillRegistry.scheduleReadyToast(sp, BURST_ID, readyAt, now);
    }
    player
        .level()
        .playSound(
            null,
            player.blockPosition(),
            SoundEvents.PLAYER_ATTACK_STRONG,
            player.getSoundSource(),
            0.9f,
            0.9f);
  }

  private void activateSlam(Player player, ChestCavityInstance cc, ItemStack organ) {
    if (!(player instanceof ServerPlayer sp)) {
      return;
    }
    OrganState state = organState(organ, STATE_ROOT);
    MultiCooldown cooldown = createCooldown(cc, organ);
    long now = player.level().getGameTime();
    MultiCooldown.Entry failGate = cooldown.entry(KEY_FAIL_COOLDOWN);
    if (failGate.getReadyTick() > now) {
      return;
    }
    MultiCooldown.Entry readyEntry = cooldown.entry(KEY_SLAM_READY);
    if (readyEntry.getReadyTick() > now) {
      return;
    }
    ConsumptionResult payment = ResourceOps.consumeStrict(player, SLAM_ZHENYUAN_BASE, SLAM_JINGLI);
    if (!payment.succeeded()) {
      failGate.setReadyAt(now + FAIL_COOLDOWN_TICKS);
      return;
    }
    player.causeFoodExhaustion((float) SLAM_BAOSHI);

    Level level = player.level();
    Vec3 look = player.getLookAngle();
    Vec3 origin = player.position().add(0.0, player.getEyeHeight() * 0.3, 0.0);
    Vec3 center = origin.add(look.scale(2.0));
    AABB box = new AABB(center.add(-2.0, -1.5, -2.0), center.add(2.0, 1.5, 2.0));

    List<LivingEntity> candidates =
        level.getEntitiesOfClass(
            LivingEntity.class,
            box,
            target -> CombatEntityUtil.areEnemies(player, target) && target.isAlive());
    int hitCount = 0;
    for (LivingEntity target : candidates) {
      double distance = player.distanceToSqr(target);
      double ratio = distance >= 9.0 ? SLAM_EDGE_RATIO : 1.0;
      double knockback = SLAM_KNOCKBACK_BASE + SLAM_KNOCKBACK_BONUS * (1.0 - ratio);
      double bonusPercent = Math.min(SLAM_DAMAGE_PER_KNOCKBACK * knockback, SLAM_DAMAGE_BONUS_CAP);
      float damage = (float) (SLAM_BASE_DAMAGE * (1.0 + bonusPercent));
      if (target.hurt(player.damageSources().playerAttack(player), damage)) {
        target.push(look.x * knockback, 0.3, look.z * knockback);
        target.hurtMarked = true;
        hitCount++;
      }
    }

    if (hitCount >= SLAM_REFUND_COUNT) {
      ResourceOps.tryAdjustJingli(player, SLAM_REFUND_JINGLI, true);
    }

    long readyAt = now + SLAM_COOLDOWN_TICKS;
    readyEntry.setReadyAt(readyAt);
    if (sp != null) {
      ActiveSkillRegistry.scheduleReadyToast(sp, SLAM_ID, readyAt, now);
    }
    level.playSound(
        null, player.blockPosition(), SoundEvents.ANVIL_LAND, player.getSoundSource(), 0.8f, 0.7f);
  }

  private void activateRoar(Player player, ChestCavityInstance cc, ItemStack organ) {
    if (!(player instanceof ServerPlayer sp)) {
      return;
    }
    OrganState state = organState(organ, STATE_ROOT);
    MultiCooldown cooldown = createCooldown(cc, organ);
    long now = player.level().getGameTime();
    MultiCooldown.Entry failGate = cooldown.entry(KEY_FAIL_COOLDOWN);
    if (failGate.getReadyTick() > now) {
      return;
    }
    MultiCooldown.Entry readyEntry = cooldown.entry(KEY_ROAR_READY);
    if (readyEntry.getReadyTick() > now) {
      return;
    }
    ConsumptionResult payment = ResourceOps.consumeStrict(player, ROAR_ZHENYUAN_BASE, 0.0);
    if (!payment.succeeded()) {
      failGate.setReadyAt(now + FAIL_COOLDOWN_TICKS);
      return;
    }

    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      ResourceOps.refund(player, payment);
      failGate.setReadyAt(now + FAIL_COOLDOWN_TICKS);
      return;
    }
    ResourceHandle handle = handleOpt.get();
    if (handle.adjustHunpo(-ROAR_HUNPO, true).isEmpty()
        || handle.adjustNiantou(-ROAR_NIANTOU, true).isEmpty()) {
      ResourceOps.refund(player, payment);
      failGate.setReadyAt(now + FAIL_COOLDOWN_TICKS);
      return;
    }

    Level level = player.level();
    List<LivingEntity> targets =
        level.getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(ROAR_RANGE),
            other -> CombatEntityUtil.areEnemies(player, other) && other.isAlive());
    for (LivingEntity target : targets) {
      boolean elite = target instanceof Mob mob && isElite(mob);
      int duration = elite ? ROAR_DURATION_TICKS / 2 : ROAR_DURATION_TICKS;
      target.addEffect(
          new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 0, false, true));
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true));
      if (target instanceof Mob mob && !elite) {
        mob.setTarget(player);
      }
    }

    long readyAt = now + ROAR_COOLDOWN_TICKS;
    readyEntry.setReadyAt(readyAt);
    ActiveSkillRegistry.scheduleReadyToast(sp, ROAR_ID, readyAt, now);
    level.playSound(
        null,
        player.blockPosition(),
        SoundEvents.PLAYER_ATTACK_SWEEP,
        player.getSoundSource(),
        0.8f,
        0.6f);
  }

  private void tickBurst(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      long now) {
    if (!state.getBoolean(KEY_BURST_ACTIVE, false)) {
      return;
    }
    long expire = cooldown.entry(KEY_BURST_EXPIRE).getReadyTick();
    if (expire <= now) {
      boolean changed =
          OrganStateOps.setBoolean(state, cc, organ, KEY_BURST_ACTIVE, false, false).changed();
      cooldown.entry(KEY_BURST_EXPIRE).setReadyAt(0L);
      OrganStateOps.setInt(state, cc, organ, KEY_BURST_STACK, 0, v -> Math.max(0, v), 0);
      if (changed) {
        sendSlotUpdate(cc, organ);
      }
    }
  }

  private void tickHotBlood(
      Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long now) {
    long until = state.getLong(KEY_HOT_BLOOD_UNTIL, 0L);
    if (until <= now) {
      removeHotBlood(player);
      return;
    }
    applyHotBlood(player);
  }

  private void maintainKnockback(Player player) {
    AttributeInstance attribute = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
    if (attribute == null) {
      return;
    }
    AttributeOps.replaceTransient(
        attribute,
        KNOCKBACK_RESIST_ID,
        new AttributeModifier(
            KNOCKBACK_RESIST_ID, KNOCKBACK_RESIST_BONUS, AttributeModifier.Operation.ADD_VALUE));
  }

  private void tickStage(
      Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, long now) {
    Stage stage = Stage.fromValue(Math.max(1, state.getInt(KEY_STAGE, 1)));
    int xp = Math.max(0, state.getInt(KEY_STAGE_XP, 0));
    int threshold = STAGE_THRESHOLDS.getOrDefault(stage, Integer.MAX_VALUE);
    if (stage == Stage.STAGE_FOUR || xp < threshold) {
      return;
    }
    Stage next = Stage.fromValue(Math.min(stage.value() + 1, Stage.STAGE_FOUR.value()));
    OrganStateOps.setInt(state, cc, organ, KEY_STAGE, next.value(), v -> Mth.clamp(v, 1, 4), 1);
    OrganStateOps.setInt(state, cc, organ, KEY_STAGE_XP, 0, v -> Math.max(0, v), 0);
    long toastGate = state.getLong(KEY_STAGE_TOAST, 0L);
    if (player instanceof ServerPlayer sp && toastGate <= now) {
      Component title = Component.literal("熊豪蛊").withStyle(ChatFormatting.GOLD);
      Component body =
          Component.literal("进阶至" + next.value() + "转").withStyle(ChatFormatting.YELLOW);
      sp.displayClientMessage(title.copy().append(Component.literal(" ")).append(body), true);
      OrganStateOps.setLong(state, cc, organ, KEY_STAGE_TOAST, now + 40, v -> Math.max(0L, v), 0L);
    }
  }

  private float applyBurstBonus(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      long now) {
    if (!state.getBoolean(KEY_BURST_ACTIVE, false)) {
      return 0.0f;
    }
    if (cooldown.entry(KEY_BURST_EXPIRE).getReadyTick() <= now) {
      OrganStateOps.setBoolean(state, cc, organ, KEY_BURST_ACTIVE, false, false);
      return 0.0f;
    }
    ConsumptionResult payment = ResourceOps.consumeStrict(player, 0.0, BURST_PER_ATTACK_JINGLI);
    if (!payment.succeeded()) {
      OrganStateOps.setBoolean(state, cc, organ, KEY_BURST_ACTIVE, false, false);
      return 0.0f;
    }
    Stage stage = Stage.fromValue(Math.max(1, state.getInt(KEY_STAGE, 1)));
    float base = stage == Stage.STAGE_FOUR ? BURST_EXTRA_DAMAGE_STAGE4 : BURST_EXTRA_DAMAGE;
    int stacks = state.getInt(KEY_BURST_STACK, 0) + 1;
    OrganStateOps.setInt(state, cc, organ, KEY_BURST_STACK, stacks, v -> Math.max(0, v), 0);
    if (stage == Stage.STAGE_THREE && stacks % 3 == 0) {
      base += 6.0f;
    }
    return base;
  }

  private float applyInertiaBonus(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      long now,
      LivingEntity target) {
    long sprintSince = state.getLong(KEY_SPRINT_SINCE, 0L);
    if (sprintSince <= 0L || now - sprintSince < INERTIA_CHARGE_TICKS) {
      return 0.0f;
    }
    MultiCooldown.Entry gate = cooldown.entry(KEY_INERTIA_READY);
    if (gate.getReadyTick() > now) {
      return 0.0f;
    }
    gate.setReadyAt(now + INERTIA_COOLDOWN_TICKS);
    target.push(
        player.getLookAngle().x * INERTIA_KNOCKBACK_BONUS,
        0.1,
        player.getLookAngle().z * INERTIA_KNOCKBACK_BONUS);
    target.hurtMarked = true;
    AttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
    double attackValue = attackAttr == null ? 1.0 : attackAttr.getValue();
    return (float) (INERTIA_DAMAGE_MULT * attackValue);
  }

  private void grantHitXp(Player player, OrganState state, MultiCooldown cooldown, long now) {
    MultiCooldown.Entry gate = cooldown.entry("xiong_hao_hit_xp");
    if (gate.getReadyTick() > now) {
      return;
    }
    addStageXp(state, 1);
    gate.setReadyAt(now + 10);
    if (player.isSprinting()) {
      MultiCooldown.Entry sprintGate = cooldown.entry("xiong_hao_sprint_xp");
      if (sprintGate.getReadyTick() <= now) {
        addStageXp(state, 2);
        sprintGate.setReadyAt(now + 20);
      }
    }
  }

  private static void addStageXp(OrganState state, int amount) {
    int xp = Math.max(0, state.getInt(KEY_STAGE_XP, 0));
    state.setInt(KEY_STAGE_XP, Math.max(0, Math.min(9999, xp + amount)));
  }

  private void applyHotBlood(Player player) {
    AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
    if (speed != null) {
      AttributeOps.replaceTransient(
          speed,
          HOT_BLOOD_SPEED_ID,
          new AttributeModifier(
              HOT_BLOOD_SPEED_ID,
              HOT_BLOOD_SPEED,
              AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }
    AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (attack != null) {
      AttributeOps.replaceTransient(
          attack,
          HOT_BLOOD_DAMAGE_ID,
          new AttributeModifier(
              HOT_BLOOD_DAMAGE_ID, HOT_BLOOD_DAMAGE, AttributeModifier.Operation.ADD_VALUE));
    }
  }

  private void removeHotBlood(Player player) {
    AttributeOps.removeById(player.getAttribute(Attributes.MOVEMENT_SPEED), HOT_BLOOD_SPEED_ID);
    AttributeOps.removeById(player.getAttribute(Attributes.ATTACK_DAMAGE), HOT_BLOOD_DAMAGE_ID);
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

  private static ItemStack findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack != null && !stack.isEmpty()) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (ORGAN_ID.equals(id)) {
          return stack;
        }
      }
    }
    return ItemStack.EMPTY;
  }

  private boolean isPrimaryOrgan(ChestCavityInstance cc, ItemStack organ) {
    if (cc == null || cc.inventory == null || organ == null || organ.isEmpty()) {
      return false;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack slotStack = cc.inventory.getItem(i);
      if (slotStack == null || slotStack.isEmpty()) {
        continue;
      }
      if (!matchesOrgan(slotStack, ORGAN_ID)) {
        continue;
      }
      return slotStack == organ;
    }
    return false;
  }

  private static void registerEvents() {
    if (eventsRegistered) {
      return;
    }
    eventsRegistered = true;
    NeoForge.EVENT_BUS.addListener(XiongHaoGuOrganBehavior::onPlayerTick);
    NeoForge.EVENT_BUS.addListener(XiongHaoGuOrganBehavior::onLivingDeath);
    NeoForge.EVENT_BUS.addListener(XiongHaoGuOrganBehavior::onKnockback);
    NeoForge.EVENT_BUS.addListener(XiongHaoGuOrganBehavior::onAttack);
  }

  private static void onPlayerTick(PlayerTickEvent.Post event) {
    Player player = event.getEntity();
    if (player.level().isClientSide()) {
      return;
    }
    ChestCavityEntity.of(player)
        .map(ChestCavityEntity::getChestCavityInstance)
        .ifPresent(
            cc -> {
              ItemStack organ = findOrgan(cc);
              if (organ.isEmpty()) {
                return;
              }
              OrganState state = OrganState.of(organ, STATE_ROOT);
              long now = player.level().getGameTime();
              if (player.isSprinting()) {
                long since = state.getLong(KEY_SPRINT_SINCE, 0L);
                if (since <= 0L) {
                  OrganStateOps.setLong(
                      state, cc, organ, KEY_SPRINT_SINCE, now, v -> Math.max(0L, v), 0L);
                }
              } else if (state.getLong(KEY_SPRINT_SINCE, 0L) > 0L) {
                OrganStateOps.setLong(
                    state, cc, organ, KEY_SPRINT_SINCE, 0L, v -> Math.max(0L, v), 0L);
              }
            });
  }

  private static void onLivingDeath(LivingDeathEvent event) {
    LivingEntity victim = event.getEntity();
    Entity killer = event.getSource().getEntity();
    if (!(killer instanceof Player player) || player.level().isClientSide()) {
      return;
    }
    ChestCavityEntity.of(player)
        .map(ChestCavityEntity::getChestCavityInstance)
        .ifPresent(
            cc -> {
              ItemStack organ = findOrgan(cc);
              if (organ.isEmpty()) {
                return;
              }
              OrganState state = OrganState.of(organ, STATE_ROOT);
              MultiCooldown cooldown =
                  MultiCooldown.builder(state)
                      .withLongClamp(v -> Math.max(0L, v), 0L)
                      .withOrgan(organ)
                      .build();
              long now = player.level().getGameTime();
              grantHotBlood(player, cc, organ, state, cooldown, now, victim);
              grantKillXp(
                  state,
                  cooldown,
                  now,
                  victim instanceof Mob mob && isElite(mob),
                  victim instanceof Mob mob && isBoss(mob));
            });
  }

  private static void grantHotBlood(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      long now,
      LivingEntity victim) {
    MultiCooldown.Entry gate = cooldown.entry(KEY_HOT_BLOOD_CD);
    if (gate.getReadyTick() > now) {
      return;
    }
    boolean elite = victim instanceof Mob mob && isElite(mob);
    int duration = elite ? HOT_BLOOD_DURATION_ELITE : HOT_BLOOD_DURATION;
    OrganStateOps.setLong(
        state, cc, organ, KEY_HOT_BLOOD_UNTIL, now + duration, v -> Math.max(0L, v), 0L);
    gate.setReadyAt(now + HOT_BLOOD_COOLDOWN);
  }

  private static void grantKillXp(
      OrganState state, MultiCooldown cooldown, long now, boolean elite, boolean boss) {
    MultiCooldown.Entry gate;
    int amount;
    if (boss) {
      gate = cooldown.entry("xiong_hao_boss_xp");
      amount = 12;
    } else if (elite) {
      gate = cooldown.entry("xiong_hao_elite_xp");
      amount = 6;
    } else {
      gate = cooldown.entry("xiong_hao_kill_xp");
      amount = 3;
    }
    if (gate.getReadyTick() > now) {
      return;
    }
    gate.setReadyAt(now + 30 * 20);
    addStageXp(state, amount);
  }

  private static void onKnockback(LivingKnockBackEvent event) {
    LivingEntity entity = event.getEntity();
    if (!(entity instanceof Player player) || player.level().isClientSide()) {
      return;
    }
    ChestCavityEntity.of(player)
        .map(ChestCavityEntity::getChestCavityInstance)
        .ifPresent(
            cc -> {
              ItemStack organ = findOrgan(cc);
              if (organ.isEmpty()) {
                return;
              }
              OrganState state = OrganState.of(organ, STATE_ROOT);
              long now = player.level().getGameTime();
              long until = state.getLong(KEY_HOT_BLOOD_UNTIL, 0L);
              if (until > now) {
                event.setCanceled(true);
              }
            });
  }

  private static void onAttack(AttackEntityEvent event) {
    Player player = event.getEntity();
    if (player.level().isClientSide()) {
      return;
    }
    if (!(event.getTarget() instanceof LivingEntity)) {
      return;
    }
    ChestCavityEntity.of(player)
        .map(ChestCavityEntity::getChestCavityInstance)
        .ifPresent(
            cc -> {
              ItemStack organ = findOrgan(cc);
              if (organ.isEmpty()) {
                return;
              }
              OrganState state = OrganState.of(organ, STATE_ROOT);
              long now = player.level().getGameTime();
              MultiCooldown cooldown =
                  MultiCooldown.builder(state)
                      .withLongClamp(v -> Math.max(0L, v), 0L)
                      .withOrgan(organ)
                      .build();
              MultiCooldown.Entry gate = cooldown.entry("xiong_hao_counter_xp");
              if (player.hurtTime > 0 && gate.getReadyTick() <= now) {
                addStageXp(state, 3);
                gate.setReadyAt(now + 200);
              }
            });
  }

  private static boolean isElite(Mob mob) {
    if (mob == null) {
      return false;
    }
    return mob.getMaxHealth() >= 40.0f && !isBoss(mob);
  }

  private static boolean isBoss(Mob mob) {
    if (mob == null) {
      return false;
    }
    if (mob instanceof EnderDragon || mob instanceof WitherBoss) {
      return true;
    }
    return mob.getMaxHealth() >= 150.0f;
  }
}
