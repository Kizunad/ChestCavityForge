package net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.AbstractLiDaoOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.synergy.li_dao.YiJingLiGuSynergyBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.synergy.li_dao.YiJingLiGuSynergyBehavior.SynergySnapshot;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import org.jetbrains.annotations.Nullable;

/**
 * 一斤力蛊（力道·肌肉）：
 *
 * <ul>
 *   <li>被动：提供稳定的近战伤害增益，并在静止蓄力后触发首击加成。
 *   <li>被动：削减受到的击退距离，并根据联动情况调整数值波动与饥饿消耗。
 *   <li>被动：每 40 tick 结算一次饥饿消耗，挥击/冲刺时额外提高。
 * </ul>
 */
public final class JingLiGuOrganBehavior extends AbstractLiDaoOrganBehavior
    implements OrganOnHitListener, OrganSlowTickListener {

  public static final JingLiGuOrganBehavior INSTANCE = new JingLiGuOrganBehavior();

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jing_li_gu");

  // --- 状态存储键（供联动/事件共享，保持 package-private 以便同包访问） ---
  static final String STATE_ROOT = "YiJingLiGu";
  static final String KEY_HUNGER_TICK_ACCUM = "HungerTickAccum";
  static final String KEY_LAST_ACTIVE_TICK = "LastActiveTick";
  static final String KEY_LAST_POS_X = "LastPosX";
  static final String KEY_LAST_POS_Y = "LastPosY";
  static final String KEY_LAST_POS_Z = "LastPosZ";
  static final String KEY_PRIME_TICK = "PrimeReadyTick";
  static final String KEY_COOLDOWN_TICK = "CooldownReadyTick";
  static final String KEY_FIRST_STRIKE_READY = "FirstStrikeReady";
  static final String KEY_DAMAGE_VARIANCE = "DamageVariance";
  static final String KEY_HUNGER_REDUCTION = "HungerReduction";
  static final String KEY_KNOCKBACK_REDUCTION = "KnockbackReduction";

  // --- 数值常量，可通过行为配置覆写 ---
  private static final double BASE_DAMAGE_BONUS =
      BehaviorConfigAccess.getFloat(JingLiGuOrganBehavior.class, "BASE_DAMAGE_BONUS", 0.05F);
  private static final double FIRST_STRIKE_BONUS =
      BehaviorConfigAccess.getFloat(JingLiGuOrganBehavior.class, "FIRST_STRIKE_BONUS", 0.10F);
  private static final int FIRST_STRIKE_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(
          JingLiGuOrganBehavior.class, "FIRST_STRIKE_COOLDOWN_TICKS", 5 * 20);
  private static final int STATIONARY_REQUIRED_TICKS =
      BehaviorConfigAccess.getInt(JingLiGuOrganBehavior.class, "STATIONARY_REQUIRED_TICKS", 30);
  private static final double BASE_DAMAGE_VARIANCE =
      BehaviorConfigAccess.getFloat(JingLiGuOrganBehavior.class, "BASE_DAMAGE_VARIANCE", 0.05F);
  private static final double BAI_SHI_VARIANCE =
      BehaviorConfigAccess.getFloat(JingLiGuOrganBehavior.class, "BAI_SHI_VARIANCE", 0.02F);
  private static final double BASE_KNOCKBACK_REDUCTION =
      BehaviorConfigAccess.getFloat(JingLiGuOrganBehavior.class, "BASE_KNOCKBACK_REDUCTION", 0.20F);
  private static final double BING_JI_KNOCKBACK_BONUS =
      BehaviorConfigAccess.getFloat(JingLiGuOrganBehavior.class, "BING_JI_KNOCKBACK_BONUS", 0.10F);
  private static final double MAX_TOTAL_KNOCKBACK_REDUCTION =
      BehaviorConfigAccess.getFloat(
          JingLiGuOrganBehavior.class, "MAX_TOTAL_KNOCKBACK_REDUCTION", 0.30F);
  private static final double MAX_HUNGER_REDUCTION =
      BehaviorConfigAccess.getFloat(JingLiGuOrganBehavior.class, "MAX_HUNGER_REDUCTION", 0.30F);
  private static final int SLOW_TICK_INTERVAL_TICKS = 20;
  private static final int HUNGER_INTERVAL_TICKS = 40;
  private static final float BASE_EXHAUSTION =
      BehaviorConfigAccess.getFloat(JingLiGuOrganBehavior.class, "BASE_EXHAUSTION", 0.12F);
  private static final float ACTIVE_EXHAUSTION =
      BehaviorConfigAccess.getFloat(JingLiGuOrganBehavior.class, "ACTIVE_EXHAUSTION", 0.24F);
  private static final int ACTIVE_WINDOW_TICKS =
      BehaviorConfigAccess.getInt(JingLiGuOrganBehavior.class, "ACTIVE_WINDOW_TICKS", 4 * 20);
  private static final double POSITION_DELTA_EPSILON = 0.01D;

  private JingLiGuOrganBehavior() {}

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
    OrganStateOps.Collector collector = OrganStateOps.collector(cc, organ);

    // --- 1. 联动：根据当前胸腔内容缓存伤害波动、饥饿与击退系数 ---
    SynergySnapshot snapshot = YiJingLiGuSynergyBehavior.INSTANCE.evaluate(cc);
    double variance = snapshot.hasBaiShiGu() ? BAI_SHI_VARIANCE : BASE_DAMAGE_VARIANCE;
    double hungerReduction = Mth.clamp(snapshot.hungerReduction(), 0.0D, MAX_HUNGER_REDUCTION);
    double knockbackReduction =
        Mth.clamp(
            BASE_KNOCKBACK_REDUCTION + (snapshot.hasBingJiGu() ? BING_JI_KNOCKBACK_BONUS : 0.0D),
            0.0D,
            MAX_TOTAL_KNOCKBACK_REDUCTION);

    collector.record(
        OrganStateOps.setDouble(
            state,
            cc,
            organ,
            KEY_DAMAGE_VARIANCE,
            variance,
            value -> Mth.clamp(value, 0.0D, 1.0D),
            BASE_DAMAGE_VARIANCE));
    collector.record(
        OrganStateOps.setDouble(
            state,
            cc,
            organ,
            KEY_HUNGER_REDUCTION,
            hungerReduction,
            value -> Mth.clamp(value, 0.0D, MAX_HUNGER_REDUCTION),
            0.0D));
    collector.record(
        OrganStateOps.setDouble(
            state,
            cc,
            organ,
            KEY_KNOCKBACK_REDUCTION,
            knockbackReduction,
            value -> Mth.clamp(value, 0.0D, MAX_TOTAL_KNOCKBACK_REDUCTION),
            BASE_KNOCKBACK_REDUCTION));

    // --- 2. 静立判定：记录上次位置并在满足条件时开启首击准备 ---
    double x = player.getX();
    double y = player.getY();
    double z = player.getZ();
    double lastX = state.getDouble(KEY_LAST_POS_X, x);
    double lastY = state.getDouble(KEY_LAST_POS_Y, y);
    double lastZ = state.getDouble(KEY_LAST_POS_Z, z);
    double deltaSq = Mth.lengthSquared(x - lastX, y - lastY, z - lastZ);
    boolean moving =
        deltaSq > POSITION_DELTA_EPSILON
            || player.isSprinting()
            || player.isSwimming()
            || player.isFallFlying();

    collector.record(
        OrganStateOps.setDouble(state, cc, organ, KEY_LAST_POS_X, x, value -> value, x));
    collector.record(
        OrganStateOps.setDouble(state, cc, organ, KEY_LAST_POS_Y, y, value -> value, y));
    collector.record(
        OrganStateOps.setDouble(state, cc, organ, KEY_LAST_POS_Z, z, value -> value, z));

    long cooldownReadyAt = Math.max(0L, state.getLong(KEY_COOLDOWN_TICK, 0L));
    long primeTick = Math.max(0L, state.getLong(KEY_PRIME_TICK, 0L));
    boolean ready = state.getBoolean(KEY_FIRST_STRIKE_READY, false);

    if (moving) {
      // 移动后重置蓄力时间，并撤销“已准备”标记。
      collector.record(
          OrganStateOps.setLong(
              state,
              cc,
              organ,
              KEY_PRIME_TICK,
              now + STATIONARY_REQUIRED_TICKS,
              value -> Math.max(0L, value),
              0L));
      if (ready) {
        collector.record(
            OrganStateOps.setBoolean(state, cc, organ, KEY_FIRST_STRIKE_READY, false, false));
      }
    } else {
      if (primeTick == 0L) {
        primeTick = now + STATIONARY_REQUIRED_TICKS;
        collector.record(
            OrganStateOps.setLong(
                state, cc, organ, KEY_PRIME_TICK, primeTick, value -> Math.max(0L, value), 0L));
      }
      if (!ready && now >= primeTick && now >= cooldownReadyAt) {
        collector.record(
            OrganStateOps.setBoolean(state, cc, organ, KEY_FIRST_STRIKE_READY, true, false));
      }
    }

    // --- 3. 饥饿结算：按 40 tick 周期消耗，期间检测最近的挥击/冲刺 ---
    int accumulator = Math.max(0, state.getInt(KEY_HUNGER_TICK_ACCUM, 0));
    accumulator += SLOW_TICK_INTERVAL_TICKS;
    boolean sprintingNow = player.isSprinting() || player.isSwimming();
    if (sprintingNow) {
      collector.record(
          OrganStateOps.setLong(
              state, cc, organ, KEY_LAST_ACTIVE_TICK, now, value -> Math.max(0L, value), 0L));
    }

    if (accumulator >= HUNGER_INTERVAL_TICKS) {
      accumulator -= HUNGER_INTERVAL_TICKS;
      long lastActiveTick = Math.max(0L, state.getLong(KEY_LAST_ACTIVE_TICK, 0L));
      boolean recentlyActive = sprintingNow || now - lastActiveTick <= ACTIVE_WINDOW_TICKS;
      float exhaustion = recentlyActive ? ACTIVE_EXHAUSTION : BASE_EXHAUSTION;
      exhaustion *= (float) (1.0D - hungerReduction);
      if (exhaustion > 0.0F) {
        player.causeFoodExhaustion(exhaustion);
      }
    }

    collector.record(
        OrganStateOps.setInt(
            state, cc, organ, KEY_HUNGER_TICK_ACCUM, accumulator, value -> Math.max(0, value), 0));

    collector.commit();
  }

  @Override
  public float onHit(
      @Nullable DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!(attacker instanceof Player player)
        || attacker.level().isClientSide()
        || cc == null
        || organ == null
        || organ.isEmpty()) {
      return damage;
    }
    if (!matchesOrgan(organ, ORGAN_ID) || !isPrimaryOrgan(cc, organ)) {
      return damage;
    }
    if (damage <= 0.0F
        || target == null
        || !target.isAlive()
        || target == attacker
        || source == null
        || source.getDirectEntity() != attacker
        || source.is(DamageTypeTags.IS_PROJECTILE)) {
      return damage;
    }

    Level level = attacker.level();
    if (!(level instanceof ServerLevel serverLevel)) {
      return damage;
    }
    long now = serverLevel.getGameTime();

    OrganState state = organState(organ, STATE_ROOT);
    OrganStateOps.Collector collector = OrganStateOps.collector(cc, organ);

    double variance =
        Mth.clamp(state.getDouble(KEY_DAMAGE_VARIANCE, BASE_DAMAGE_VARIANCE), 0.0D, 1.0D);
    double knockbackReduction =
        Mth.clamp(
            state.getDouble(KEY_KNOCKBACK_REDUCTION, BASE_KNOCKBACK_REDUCTION),
            0.0D,
            MAX_TOTAL_KNOCKBACK_REDUCTION);
    boolean firstStrikeReady = state.getBoolean(KEY_FIRST_STRIKE_READY, false);
    long cooldownReadyAt = Math.max(0L, state.getLong(KEY_COOLDOWN_TICK, 0L));

    double multiplier = 1.0D + BASE_DAMAGE_BONUS;
    RandomSource random = attacker.getRandom();
    double randomDelta = (random.nextDouble() * 2.0D - 1.0D) * variance;
    multiplier *= 1.0D + randomDelta;

    if (firstStrikeReady && now >= cooldownReadyAt) {
      multiplier *= 1.0D + FIRST_STRIKE_BONUS;
      collector.record(
          OrganStateOps.setBoolean(state, cc, organ, KEY_FIRST_STRIKE_READY, false, false));
      collector.record(
          OrganStateOps.setLong(
              state,
              cc,
              organ,
              KEY_COOLDOWN_TICK,
              now + FIRST_STRIKE_COOLDOWN_TICKS,
              value -> Math.max(0L, value),
              0L));
      collector.record(
          OrganStateOps.setLong(
              state,
              cc,
              organ,
              KEY_PRIME_TICK,
              now + STATIONARY_REQUIRED_TICKS,
              value -> Math.max(0L, value),
              0L));
      spawnImpactParticles(serverLevel, target, knockbackReduction);
    }

    collector.record(
        OrganStateOps.setLong(
            state, cc, organ, KEY_LAST_ACTIVE_TICK, now, value -> Math.max(0L, value), 0L));

    collector.commit();
    double adjusted = Math.max(0.0D, damage * multiplier);
    return (float) adjusted;
  }

  private void spawnImpactParticles(
      ServerLevel server, LivingEntity target, double knockbackReduction) {
    if (server == null || target == null) {
      return;
    }
    if (target.getBbWidth() < 0.75F && target.getBbHeight() < 1.0F) {
      return;
    }
    Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
    double spread = 0.2D + knockbackReduction * 0.4D;
    server.sendParticles(
        ParticleTypes.POOF, center.x, center.y, center.z, 4, spread, 0.1D, spread, 0.02D);
  }

  private boolean isPrimaryOrgan(ChestCavityInstance cc, ItemStack organ) {
    if (cc == null || cc.inventory == null || organ == null || organ.isEmpty()) {
      return false;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack slot = cc.inventory.getItem(i);
      if (slot == null || slot.isEmpty()) {
        continue;
      }
      if (!matchesOrgan(slot, ORGAN_ID)) {
        continue;
      }
      return slot == organ;
    }
    return false;
  }

  static ItemStack findPrimaryOrgan(@Nullable ChestCavityInstance cc) {
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

  @EventBusSubscriber(modid = ChestCavity.MODID)
  public static final class Events {

    private Events() {}

    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
      LivingEntity entity = event.getEntity();
      if (entity == null || entity.level().isClientSide()) {
        return;
      }
      ChestCavityEntity.of(entity)
          .map(ChestCavityEntity::getChestCavityInstance)
          .ifPresent(
              cc -> {
                ItemStack organ = JingLiGuOrganBehavior.findPrimaryOrgan(cc);
                if (organ.isEmpty()) {
                  return;
                }
                OrganState state = OrganState.of(organ, STATE_ROOT);
                double reduction =
                    Mth.clamp(
                        state.getDouble(KEY_KNOCKBACK_REDUCTION, BASE_KNOCKBACK_REDUCTION),
                        0.0D,
                        MAX_TOTAL_KNOCKBACK_REDUCTION);
                if (reduction <= 0.0D) {
                  return;
                }
                float originalStrength = event.getStrength();
                float adjustedStrength = (float) (originalStrength * (1.0D - reduction));
                event.setStrength(Math.max(0.0F, adjustedStrength));
              });
    }
  }
}
