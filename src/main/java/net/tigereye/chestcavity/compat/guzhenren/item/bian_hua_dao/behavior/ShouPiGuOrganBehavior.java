package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.WeakHashMap;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntUnaryOperator;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.AbsorptionHelper;
import org.jetbrains.annotations.Nullable;

/**
 * 兽皮蛊核心行为实现。
 *
 * <p>功能涵盖：
 *
 * <ul>
 *   <li>软反伤：根据被减免量回溅真实伤害，支持内置冷却、投射物反弹与 5 转溅射。
 *   <li>坚忍计：记录净承伤叠层并提供额外减伤+护盾，含锁定窗口。
 *   <li>被动链：皮厚 / 筋膜收束等在连续受击时提供额外防护。
 *   <li>主动技能：硬皮鼓动、翻滚脱力；联动技“嵌甲冲撞”。
 *   <li>联动：与虎皮蛊、铁骨蛊交互。
 *   <li>升级：HXP 计分、自动进阶 1→5 转。
 * </ul>
 *
 * <p>实现尽可能复用 MultiCooldown / ResourceOps 等公共工具，并在关键状态变更时同步 OrganState，确保客户端提示及时刷新。
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class ShouPiGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganIncomingDamageListener,
        OrganSlowTickListener,
        OrganOnHitListener,
        OrganRemovalListener {

  public static final ShouPiGuOrganBehavior INSTANCE = new ShouPiGuOrganBehavior();

  private static final String MOD_ID = "guzhenren";

  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "shou_pi_gu");
  private static final ResourceLocation HU_PI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "hupigu");
  private static final ResourceLocation TIE_GU_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "tie_gu_gu");
  public static final ResourceLocation ABILITY_HARDEN =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "shou_pi_gu/hard_skin");
  public static final ResourceLocation ABILITY_ROLL =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "shou_pi_gu/roll");
  public static final ResourceLocation ABILITY_SYNERGY_CHARGE =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "shou_pi_gu/qian_jia_chong_zhuang");

  private static final String STATE_ROOT = "ShouPiGu";
  private static final String KEY_TIER = "Tier";
  private static final String KEY_HXP = "Hxp";
  private static final String KEY_NET_DAMAGE = "NetDamage";
  private static final String KEY_STACK_PROGRESS = "StalwartProgress";
  private static final String KEY_STACKS = "StalwartStacks";
  private static final String KEY_LOCK_UNTIL = "StalwartLock";
  private static final String KEY_HARDEN_EXPIRE = "HardenExpire";
  private static final String KEY_HARDEN_READY = "HardenReady";
  private static final String KEY_ROLL_READY = "RollReady";
  private static final String KEY_SYNERGY_READY = "SynergyReady";
  private static final String KEY_PI_HOU_WINDOW = "PiHouWindow";
  private static final String KEY_FASCIA_COUNT = "FasciaCount";
  private static final String KEY_FASCIA_READY = "FasciaReady";
  private static final String KEY_REFLECT_READY = "SoftReflectReady";
  private static final String KEY_REFLECT_PROJECTILE_READY = "SoftReflectProjectileReady";
  private static final String KEY_REFLECT_ACCUM = "SoftReflectAccum";
  private static final String KEY_REFLECT_ACCUM_EXPIRE = "SoftReflectAccumExpire";
  private static final String KEY_REFLECT_HXP_READY = "SoftReflectHxpReady";
  private static final String KEY_KILL_HXP_READY = "KillHxpReady";
  private static final String KEY_PROJECTILE_HXP_READY = "ProjectileHxpReady";
  private static final String KEY_LAST_TARGET = "LastReflectTarget";
  private static final String KEY_LAST_TARGET_EXP = "LastReflectTargetExpire";

  private static final ResourceLocation ABSORPTION_MODIFIER_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "shou_pi_gu/absorption");

  private static final float[] REFLECT_VALUES = {0.0f, 0.25f, 0.30f, 0.35f, 0.40f, 0.45f};
  private static final float[] REFLECT_PROJECTILE_VALUES = {0.0f, 0.0f, 0.0f, 0.0f, 0.40f, 0.45f};
  private static final int[] REFLECT_ICD_TICKS = {0, 20, 18, 16, 12, 14};
  private static final double HARDEN_DEFENSE_BONUS = 0.06D;
  private static final double HARDEN_REFLECT_BONUS = 0.10D;
  private static final int HARDEN_DURATION_TICKS = 5 * 20;
  private static final int HARDEN_COOLDOWN_TICKS = 20 * 20;
  private static final int HARDEN_SOUND_COOLDOWN_TICKS = 10;
  private static final int ROLL_COOLDOWN_TICKS = 14 * 20;
  private static final double ROLL_DISTANCE = 3.0D;
  private static final int ROLL_IFRAME_TICKS = 12;

  private static final int STACK_MAX = 6;
  private static final int STACK_LOCK_TICKS = 8 * 20;
  private static final int STACK_THRESHOLD_TIER2 = 100;
  private static final int STACK_THRESHOLD_TIER5 = 200;

  private static final double[] STACK_DAMAGE_REDUCTION = {0.0D, 0.0D, 0.15D, 0.18D, 0.18D, 0.22D};
  private static final float[] STACK_SHIELD_VALUES = {0f, 0f, 3f, 4f, 4f, 5f};

  private static final float FASCIA_SHIELD_VALUE = 2f;
  private static final double FASCIA_REDUCTION = 0.12D;
  private static final int FASCIA_COOLDOWN_TICKS = 10 * 20;
  private static final int FASCIA_TRIGGER_COUNT = 5;

  private static final double PI_HOU_REDUCTION = 0.08D;
  private static final int PI_HOU_WINDOW_TICKS = 20;

  private static final double REFLECT_SPLASH_RATIO = 0.60D;
  private static final double REFLECT_SPLASH_RADIUS = 3.0D;

  private static final int REFLECT_ACCUM_WINDOW_TICKS = 5 * 20;
  private static final int REFLECT_HXP_COOLDOWN_TICKS = 5 * 20;
  private static final int KILL_HXP_COOLDOWN_TICKS = 30 * 20;
  private static final int PROJECTILE_HXP_COOLDOWN_TICKS = 5 * 20;

  private static final int[] HXP_REQUIREMENTS = {0, 120, 200, 320, 480, Integer.MAX_VALUE};

  private static final Map<LivingEntity, Float> ORIGINAL_DAMAGE = new WeakHashMap<>();
  private static final Map<LivingEntity, Integer> PENDING_IFRAMES = new WeakHashMap<>();

  private enum HxpSource {
    NET_DAMAGE,
    REFLECT,
    PROJECTILE,
    KILL
  }

  private static final Map<HxpSource, Integer> HXP_VALUES = new EnumMap<>(HxpSource.class);

  static {
    HXP_VALUES.put(HxpSource.NET_DAMAGE, 1);
    HXP_VALUES.put(HxpSource.REFLECT, 1);
    HXP_VALUES.put(HxpSource.PROJECTILE, 2);
    HXP_VALUES.put(HxpSource.KILL, 2);
    OrganActivationListeners.register(ABILITY_HARDEN, ShouPiGuOrganBehavior::activateHarden);
    OrganActivationListeners.register(ABILITY_ROLL, ShouPiGuOrganBehavior::activateRoll);
    OrganActivationListeners.register(
        ABILITY_SYNERGY_CHARGE, ShouPiGuOrganBehavior::activateSynergyCharge);
  }

  private ShouPiGuOrganBehavior() {}

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (entity == null || entity.level().isClientSide()) {
      return;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return;
    }
    OrganState state = organState(organ, STATE_ROOT);
    MultiCooldown cooldown = cooldown(cc, organ, state);
    long now = entity.level().getGameTime();

    cleanupReflectAccumulator(state, now, cc, organ);
    cleanupLastTarget(state, now);
    tickPendingIframes(entity);

    maybeLevelUp(entity, cc, organ, state);

    sendSlotUpdate(cc, organ);
  }

  @Override
  public float onIncomingDamage(
      DamageSource source,
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (victim == null || victim.level().isClientSide() || damage <= 0.0f) {
      return damage;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return damage;
    }
    OrganState state = organState(organ, STATE_ROOT);
    MultiCooldown cooldown = cooldown(cc, organ, state);
    long now = victim.level().getGameTime();

    int tier = resolveTier(state);
    float original = ORIGINAL_DAMAGE.getOrDefault(victim, damage);
    float adjustedDamage = damage;

    boolean bypassArmor = source != null && source.is(DamageTypeTags.BYPASSES_ARMOR);
    adjustedDamage =
        applyPassiveReductions(
            source, victim, cc, organ, state, cooldown, adjustedDamage, bypassArmor, now);

    float prevented = Math.max(0.0f, original - adjustedDamage);
    boolean projectile = isProjectileDamage(source);

    float reflected =
        tryReflectDamage(
            victim,
            cc,
            organ,
            state,
            cooldown,
            source,
            adjustedDamage,
            prevented,
            projectile,
            now,
            tier);
    if (reflected > 0.0f) {
      registerHxp(cc, organ, state, HxpSource.REFLECT, now);
    }

    if (adjustedDamage > 0.0f) {
      float applied = adjustedDamage;
      registerNetDamage(cc, organ, state, applied, now, tier);
    }

    return adjustedDamage;
  }

  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance chestCavity,
      ItemStack organ,
      float damage) {
    if (attacker == null || attacker.level().isClientSide() || damage <= 0.0f) {
      return damage;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return damage;
    }
    OrganState state = organState(organ, STATE_ROOT);
    long now = attacker.level().getGameTime();
    return maybeConsumeS1Mark(chestCavity, attacker, target, state, now, damage);
  }

  private static float maybeConsumeS1Mark(
      ChestCavityInstance cc,
      LivingEntity attacker,
      LivingEntity target,
      OrganState state,
      long now,
      float baseDamage) {
    if (!(attacker instanceof Player) || cc == null || !INSTANCE.hasHuPiGu(cc)) {
      return baseDamage;
    }
    long expire = Math.max(0L, state.getLong(KEY_LAST_TARGET_EXP, 0L));
    if (expire <= 0L || now > expire) {
      return baseDamage;
    }
    int id = state.getInt(KEY_LAST_TARGET, -1);
    if (id < 0 || target == null || target.getId() != id) {
      return baseDamage;
    }
    state.setLong(KEY_LAST_TARGET_EXP, 0L, LONG_NON_NEGATIVE, 0L);
    state.setInt(KEY_LAST_TARGET, -1, value -> value, -1);
    float result = baseDamage;
    if (baseDamage > 0.0f) {
      result = (float) (baseDamage * (1.0D + 0.10D));
    }
    if (target.isAlive()) {
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 16, 0, false, true));
    }
    return result;
  }

  private static boolean isProjectileDamage(@Nullable DamageSource source) {
    return source != null && source.is(DamageTypeTags.IS_PROJECTILE);
  }

  private float applyPassiveReductions(
      DamageSource source,
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      float damage,
      boolean bypassArmor,
      long now) {
    float adjusted = damage;

    if (!bypassArmor) {
      long window = Math.max(0L, state.getLong(KEY_PI_HOU_WINDOW, 0L));
      if (window > 0L && now <= window) {
        adjusted = (float) Math.max(0.0D, adjusted * (1.0D - PI_HOU_REDUCTION));
      }
      state.setLong(KEY_PI_HOU_WINDOW, now + PI_HOU_WINDOW_TICKS, LONG_NON_NEGATIVE, 0L);
    }

    int tier = resolveTier(state);
    if (tier >= 2) {
      int count = Math.max(0, state.getInt(KEY_FASCIA_COUNT, 0)) + 1;
      state.setInt(KEY_FASCIA_COUNT, count, INT_NON_NEGATIVE, 0);
      if (count >= FASCIA_TRIGGER_COUNT) {
        MultiCooldown.Entry fasciaCd = cooldown.entry(KEY_FASCIA_READY);
        if (fasciaCd.isReady(now)) {
          fasciaCd.setReadyAt(now + FASCIA_COOLDOWN_TICKS);
          state.setInt(KEY_FASCIA_COUNT, 0, INT_NON_NEGATIVE, 0);
          adjusted = (float) Math.max(0.0D, adjusted * (1.0D - FASCIA_REDUCTION));
          AbsorptionHelper.applyAbsorption(
              victim, FASCIA_SHIELD_VALUE, ABSORPTION_MODIFIER_ID, false);
          sendSlotUpdate(cc, organ);
        }
      }
    }

    if (state.getLong(KEY_HARDEN_EXPIRE, 0L) > now) {
      adjusted = (float) Math.max(0.0D, adjusted * (1.0D - HARDEN_DEFENSE_BONUS));
    }

    if (triggerStalwartStacks(state, now)) {
      double reduction = STACK_DAMAGE_REDUCTION[Math.min(tier, STACK_DAMAGE_REDUCTION.length - 1)];
      float shield = STACK_SHIELD_VALUES[Math.min(tier, STACK_SHIELD_VALUES.length - 1)];
      adjusted = (float) Math.max(0.0D, adjusted * (1.0D - reduction));
      if (shield > 0f) {
        AbsorptionHelper.applyAbsorption(victim, shield, ABSORPTION_MODIFIER_ID, false);
      }
      sendSlotUpdate(cc, organ);
    }

    return adjusted;
  }

  private float tryReflectDamage(
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      DamageSource source,
      float adjustedDamage,
      float prevented,
      boolean projectile,
      long now,
      int tier) {
    boolean projectileBranch = projectile && tier >= 4;
    MultiCooldown.Entry entry;
    int cooldownTicks;
    float ratio = 0.0f;

    if (projectileBranch) {
      entry = cooldown.entry(KEY_REFLECT_PROJECTILE_READY);
      cooldownTicks = REFLECT_ICD_TICKS[Math.min(tier, REFLECT_ICD_TICKS.length - 1)];
      ratio = REFLECT_PROJECTILE_VALUES[Math.min(tier, REFLECT_PROJECTILE_VALUES.length - 1)];
      if (!entry.tryStart(now, cooldownTicks)) {
        projectileBranch = false;
      }
    }

    if (!projectileBranch) {
      entry = cooldown.entry(KEY_REFLECT_READY);
      cooldownTicks = REFLECT_ICD_TICKS[Math.min(tier, REFLECT_ICD_TICKS.length - 1)];
      ratio = REFLECT_VALUES[Math.min(tier, REFLECT_VALUES.length - 1)];
      if (!entry.tryStart(now, cooldownTicks)) {
        return 0.0f;
      }
    }

    if (state.getLong(KEY_HARDEN_EXPIRE, 0L) > now) {
      ratio += HARDEN_REFLECT_BONUS;
    }
    ratio = (float) Math.min(1.0D, Math.max(0.0D, ratio));

    float reflected = prevented * ratio;
    if (reflected <= 0.0f) {
      return 0.0f;
    }

    LivingEntity attacker = resolveAttacker(source);
    if (attacker == null || !attacker.isAlive()) {
      return 0.0f;
    }

    float applied = applyReflectDamage(victim, attacker, reflected, source);
    if (applied <= 0.0f) {
      return 0.0f;
    }

    accumulateReflect(state, applied, now, cc, organ);
    if (cc != null && hasHuPiGu(cc)) {
      markS1Target(state, attacker, now);
    }

    if (projectileBranch) {
      redirectProjectile(source, attacker, victim);
      registerHxp(cc, organ, state, HxpSource.PROJECTILE, now);
    }

    if (tier >= 5) {
      splashReflect(victim, attacker, applied, ratio);
    }

    spawnReflectFeedback(victim);
    return applied;
  }

  private static LivingEntity resolveAttacker(@Nullable DamageSource source) {
    if (source == null) {
      return null;
    }
    Entity direct = source.getEntity();
    if (direct instanceof LivingEntity living) {
      return living;
    }
    direct = source.getDirectEntity();
    return direct instanceof LivingEntity living ? living : null;
  }

  private void redirectProjectile(
      @Nullable DamageSource source, LivingEntity attacker, LivingEntity victim) {
    if (source == null || attacker == null || victim == null) {
      return;
    }
    Entity direct = source.getDirectEntity();
    if (!(direct instanceof Projectile projectile) || !projectile.isAlive()) {
      return;
    }
    Vec3 victimCenter = victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D);
    Vec3 attackerCenter = attacker.position().add(0.0D, attacker.getBbHeight() * 0.5D, 0.0D);
    Vec3 direction = attackerCenter.subtract(victimCenter);
    if (direction.lengthSqr() < 1.0E-6D) {
      direction = attacker.position().subtract(projectile.position());
    }
    if (direction.lengthSqr() < 1.0E-6D) {
      return;
    }
    Vec3 motion =
        direction.normalize().scale(Math.max(0.3D, projectile.getDeltaMovement().length()));
    projectile.setOwner(victim);
    projectile.setPos(victimCenter.x, victimCenter.y, victimCenter.z);
    projectile.setDeltaMovement(motion);
    projectile.hasImpulse = true;
  }

  private float applyReflectDamage(
      LivingEntity victim, LivingEntity attacker, float damage, DamageSource original) {
    DamageSource thorns = victim.damageSources().thorns(victim);
    attacker.hurt(thorns, damage);
    return damage;
  }

  private void splashReflect(
      LivingEntity victim, LivingEntity primary, float baseDamage, float ratio) {
    Level level = victim.level();
    if (!(level instanceof ServerLevel server)) {
      return;
    }
    double radius = REFLECT_SPLASH_RADIUS;
    for (LivingEntity entity :
        server.getEntitiesOfClass(
            LivingEntity.class, primary.getBoundingBox().inflate(radius), e -> e != victim)) {
      if (!entity.isAlive() || entity == primary) {
        continue;
      }
      double distance = entity.distanceTo(primary);
      if (distance > radius) {
        continue;
      }
      float splash = (float) (baseDamage * REFLECT_SPLASH_RATIO);
      entity.hurt(victim.damageSources().thorns(victim), splash);
    }
  }

  private void accumulateReflect(
      OrganState state, float applied, long now, ChestCavityInstance cc, ItemStack organ) {
    double previous = Math.max(0.0D, state.getDouble(KEY_REFLECT_ACCUM, 0.0D));
    double updated = previous + applied;
    state.setDouble(KEY_REFLECT_ACCUM, updated, DOUBLE_NON_NEGATIVE, 0.0D);
    state.setLong(
        KEY_REFLECT_ACCUM_EXPIRE, now + REFLECT_ACCUM_WINDOW_TICKS, LONG_NON_NEGATIVE, 0L);
    sendSlotUpdate(cc, organ);
  }

  private void cleanupReflectAccumulator(
      OrganState state, long now, ChestCavityInstance cc, ItemStack organ) {
    long expire = Math.max(0L, state.getLong(KEY_REFLECT_ACCUM_EXPIRE, 0L));
    if (expire > 0L && now > expire) {
      OrganState.Change<Double> change = state.setDouble(KEY_REFLECT_ACCUM, 0.0D);
      if (change.changed()) {
        sendSlotUpdate(cc, organ);
      }
      state.setLong(KEY_REFLECT_ACCUM_EXPIRE, 0L, LONG_NON_NEGATIVE, 0L);
    }
  }

  private void markS1Target(OrganState state, LivingEntity attacker, long now) {
    if (attacker == null) {
      return;
    }
    state.setInt(KEY_LAST_TARGET, attacker.getId(), value -> value, -1);
    state.setLong(KEY_LAST_TARGET_EXP, now + 60, LONG_NON_NEGATIVE, 0L);
  }

  private void cleanupLastTarget(OrganState state, long now) {
    long expire = Math.max(0L, state.getLong(KEY_LAST_TARGET_EXP, 0L));
    if (expire > 0L && now > expire) {
      state.setLong(KEY_LAST_TARGET_EXP, 0L, LONG_NON_NEGATIVE, 0L);
      state.setInt(KEY_LAST_TARGET, -1, value -> value, -1);
    }
  }

  @Override
  public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (organ == null || organ.isEmpty()) {
      return;
    }
    OrganState state = organState(organ, STATE_ROOT);
    MultiCooldown cooldown = cooldown(cc, organ, state);
    cooldown.clearAll();
    state.setInt(KEY_STACKS, 0, INT_NON_NEGATIVE, 0);
    state.setInt(KEY_FASCIA_COUNT, 0, INT_NON_NEGATIVE, 0);
    state.setLong(KEY_LOCK_UNTIL, 0L, LONG_NON_NEGATIVE, 0L);
    state.setLong(KEY_HARDEN_EXPIRE, 0L, LONG_NON_NEGATIVE, 0L);
    state.setLong(KEY_PI_HOU_WINDOW, 0L, LONG_NON_NEGATIVE, 0L);
    state.setLong(KEY_LAST_TARGET_EXP, 0L, LONG_NON_NEGATIVE, 0L);
    state.setInt(KEY_LAST_TARGET, -1, value -> value, -1);
    state.setDouble(KEY_NET_DAMAGE, 0.0D, DOUBLE_NON_NEGATIVE, 0.0D);
    state.setDouble(KEY_STACK_PROGRESS, 0.0D, DOUBLE_NON_NEGATIVE, 0.0D);
    state.setDouble(KEY_REFLECT_ACCUM, 0.0D, DOUBLE_NON_NEGATIVE, 0.0D);
    state.setLong(KEY_REFLECT_ACCUM_EXPIRE, 0L, LONG_NON_NEGATIVE, 0L);
    sendSlotUpdate(cc, organ);
    if (entity != null) {
      PENDING_IFRAMES.remove(entity);
    }
  }

  private void registerNetDamage(
      ChestCavityInstance cc, ItemStack organ, OrganState state, float damage, long now, int tier) {
    double xpAccumulated = Math.max(0.0D, state.getDouble(KEY_NET_DAMAGE, 0.0D)) + damage;
    int hxpGrants = (int) (xpAccumulated / 20.0D);
    xpAccumulated -= hxpGrants * 20.0D;
    state.setDouble(KEY_NET_DAMAGE, xpAccumulated, DOUBLE_NON_NEGATIVE, 0.0D);
    for (int i = 0; i < hxpGrants; i++) {
      registerHxp(cc, organ, state, HxpSource.NET_DAMAGE, now);
    }

    if (tier < 2) {
      return;
    }

    double stackAccum = Math.max(0.0D, state.getDouble(KEY_STACK_PROGRESS, 0.0D)) + damage;
    int threshold = tier >= 5 ? STACK_THRESHOLD_TIER5 : STACK_THRESHOLD_TIER2;
    boolean stacksChanged = false;
    while (stackAccum >= threshold) {
      stackAccum -= threshold;
      stacksChanged |= gainStack(state, now);
    }
    state.setDouble(KEY_STACK_PROGRESS, stackAccum, DOUBLE_NON_NEGATIVE, 0.0D);
    if (stacksChanged) {
      sendSlotUpdate(cc, organ);
    }
  }

  private boolean gainStack(OrganState state, long now) {
    int stacks = Math.max(0, state.getInt(KEY_STACKS, 0));
    long lock = Math.max(0L, state.getLong(KEY_LOCK_UNTIL, 0L));
    if (lock > now || stacks >= STACK_MAX) {
      return false;
    }
    state.setInt(KEY_STACKS, stacks + 1, INT_NON_NEGATIVE, 0);
    return true;
  }

  private boolean triggerStalwartStacks(OrganState state, long now) {
    int stacks = Math.max(0, state.getInt(KEY_STACKS, 0));
    long lock = Math.max(0L, state.getLong(KEY_LOCK_UNTIL, 0L));
    if (stacks < STACK_MAX || lock > now) {
      return false;
    }
    state.setInt(KEY_STACKS, 0, INT_NON_NEGATIVE, 0);
    state.setLong(KEY_LOCK_UNTIL, now + STACK_LOCK_TICKS, LONG_NON_NEGATIVE, 0L);
    return true;
  }

  private void registerHxp(
      ChestCavityInstance cc, ItemStack organ, OrganState state, HxpSource source, long now) {
    int value = HXP_VALUES.getOrDefault(source, 0);
    if (value <= 0) {
      return;
    }
    String key;
    int cooldownTicks;
    switch (source) {
      case REFLECT -> {
        key = KEY_REFLECT_HXP_READY;
        cooldownTicks = REFLECT_HXP_COOLDOWN_TICKS;
      }
      case PROJECTILE -> {
        key = KEY_PROJECTILE_HXP_READY;
        cooldownTicks = PROJECTILE_HXP_COOLDOWN_TICKS;
      }
      case KILL -> {
        key = KEY_KILL_HXP_READY;
        cooldownTicks = KILL_HXP_COOLDOWN_TICKS;
      }
      default -> {
        key = null;
        cooldownTicks = 0;
      }
    }

    if (key != null) {
      long ready = Math.max(0L, state.getLong(key, 0L));
      if (ready > now) {
        return;
      }
      state.setLong(key, now + cooldownTicks, LONG_NON_NEGATIVE, 0L);
    }

    int previous = Math.max(0, state.getInt(KEY_HXP, 0));
    int updated = previous + value;
    state.setInt(KEY_HXP, updated, INT_NON_NEGATIVE, 0);
    sendSlotUpdate(cc, organ);
  }

  private void maybeLevelUp(
      LivingEntity entity, ChestCavityInstance cc, ItemStack organ, OrganState state) {
    int tier = resolveTier(state);
    int xp = Math.max(0, state.getInt(KEY_HXP, 0));
    boolean upgraded = false;
    while (tier < 5) {
      int requirement = HXP_REQUIREMENTS[Math.min(tier, HXP_REQUIREMENTS.length - 1)];
      if (requirement <= 0 || xp < requirement) {
        break;
      }
      xp -= requirement;
      tier++;
      state.setInt(KEY_TIER, tier, INT_NON_NEGATIVE, 1);
      announceTierUpgrade(entity, tier);
      upgraded = true;
    }
    state.setInt(KEY_HXP, xp, INT_NON_NEGATIVE, 0);
    if (upgraded) {
      sendSlotUpdate(cc, organ);
    }
  }

  private static void announceTierUpgrade(LivingEntity entity, int tier) {
    if (!(entity instanceof ServerPlayer player)) {
      return;
    }
    player.displayClientMessage(
        net.minecraft.network.chat.Component.literal(String.format(Locale.ROOT, "兽皮蛊晋入第%d转", tier)),
        true);
  }

  private static int resolveTier(OrganState state) {
    return Mth.clamp(state.getInt(KEY_TIER, 1), 1, 5);
  }

  private void tickPendingIframes(LivingEntity entity) {
    if (entity == null) {
      return;
    }
    Integer remaining = PENDING_IFRAMES.get(entity);
    if (remaining == null) {
      return;
    }
    if (remaining <= 0) {
      PENDING_IFRAMES.remove(entity);
      return;
    }
    entity.invulnerableTime = Math.max(entity.invulnerableTime, remaining);
    PENDING_IFRAMES.put(entity, remaining - 1);
  }

  private MultiCooldown cooldown(ChestCavityInstance cc, ItemStack organ, OrganState state) {
    MultiCooldown.Builder builder =
        MultiCooldown.builder(state)
            .withLongClamp(LONG_NON_NEGATIVE, 0L)
            .withIntClamp(INT_NON_NEGATIVE, 0);
    if (cc != null) {
      builder.withSync(cc, organ);
    } else if (organ != null && !organ.isEmpty()) {
      builder.withOrgan(organ);
    }
    return builder.build();
  }

  private static final DoubleUnaryOperator DOUBLE_NON_NEGATIVE = value -> Math.max(0.0D, value);
  private static final IntUnaryOperator INT_NON_NEGATIVE = value -> Math.max(0, value);
  private static final java.util.function.LongUnaryOperator LONG_NON_NEGATIVE =
      value -> Math.max(0L, value);

  private static void spawnReflectFeedback(LivingEntity entity) {
    Level level = entity.level();
    if (!(level instanceof ServerLevel server)) {
      return;
    }
    server.sendParticles(
        ParticleTypes.CRIT,
        entity.getX(),
        entity.getY(0.5D),
        entity.getZ(),
        10,
        0.2D,
        0.4D,
        0.2D,
        0.1D);
    server.playSound(
        null,
        entity.blockPosition(),
        SoundEvents.ARMOR_EQUIP_LEATHER.value(),
        SoundSource.PLAYERS,
        0.6f,
        0.8f);
  }

  private static void activateHarden(LivingEntity user, ChestCavityInstance cc) {
    if (!(user instanceof ServerPlayer player) || cc == null) {
      return;
    }
    ItemStack organ = locateOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    OrganState state = INSTANCE.organState(organ, STATE_ROOT);
    int tier = resolveTier(state);
    if (tier < 1) {
      return;
    }
    long now = player.level().getGameTime();
    MultiCooldown cooldown = INSTANCE.cooldown(cc, organ, state);
    MultiCooldown.Entry entry = cooldown.entry(KEY_HARDEN_READY);
    if (!entry.isReady(now)) {
      return;
    }

    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    OptionalDouble consume = handle.consumeScaledZhenyuan(40.0D);
    if (consume.isEmpty()) {
      return;
    }

    state.setLong(KEY_HARDEN_EXPIRE, now + HARDEN_DURATION_TICKS, LONG_NON_NEGATIVE, 0L);
    entry.setReadyAt(now + HARDEN_COOLDOWN_TICKS);
    ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_HARDEN, entry.getReadyTick(), now);
  }

  private static void activateRoll(LivingEntity user, ChestCavityInstance cc) {
    if (!(user instanceof ServerPlayer player) || cc == null) {
      return;
    }
    ItemStack organ = locateOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    OrganState state = INSTANCE.organState(organ, STATE_ROOT);
    int tier = resolveTier(state);
    if (tier < 2) {
      return;
    }
    long now = player.level().getGameTime();
    MultiCooldown cooldown = INSTANCE.cooldown(cc, organ, state);
    MultiCooldown.Entry entry = cooldown.entry(KEY_ROLL_READY);
    if (!entry.isReady(now)) {
      return;
    }

    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    OptionalDouble cost = handle.consumeScaledZhenyuan(25.0D);
    if (cost.isEmpty()) {
      return;
    }

    Vec3 look = user.getLookAngle().normalize();
    Vec3 motion = new Vec3(look.x, 0.0D, look.z).normalize().scale(ROLL_DISTANCE);
    user.setDeltaMovement(motion);
    PENDING_IFRAMES.put(user, ROLL_IFRAME_TICKS);
    entry.setReadyAt(now + ROLL_COOLDOWN_TICKS);
    ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ROLL, entry.getReadyTick(), now);
  }

  private static void activateSynergyCharge(LivingEntity user, ChestCavityInstance cc) {
    if (!(user instanceof ServerPlayer player) || cc == null) {
      return;
    }
    ItemStack organ = locateOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    OrganState state = INSTANCE.organState(organ, STATE_ROOT);
    if (!INSTANCE.hasTieGuGu(cc)) {
      player.displayClientMessage(net.minecraft.network.chat.Component.literal("需要铁骨蛊联动"), true);
      return;
    }
    long now = player.level().getGameTime();
    MultiCooldown cooldown = INSTANCE.cooldown(cc, organ, state);
    MultiCooldown.Entry entry = cooldown.entry(KEY_SYNERGY_READY);
    if (!entry.isReady(now)) {
      return;
    }
    double accumulated = Math.max(0.0D, state.getDouble(KEY_REFLECT_ACCUM, 0.0D));
    if (accumulated <= 0.0D) {
      player.displayClientMessage(net.minecraft.network.chat.Component.literal("未储存软反伤"), true);
      return;
    }
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    OptionalDouble cost = handle.consumeScaledZhenyuan(60.0D);
    if (cost.isEmpty()) {
      return;
    }
    state.setDouble(KEY_REFLECT_ACCUM, 0.0D, DOUBLE_NON_NEGATIVE, 0.0D);
    state.setLong(KEY_REFLECT_ACCUM_EXPIRE, 0L, LONG_NON_NEGATIVE, 0L);
    entry.setReadyAt(now + 18 * 20);
    performSynergyCharge(player, accumulated);
    ActiveSkillRegistry.scheduleReadyToast(
        player, ABILITY_SYNERGY_CHARGE, entry.getReadyTick(), now);
  }

  private static void performSynergyCharge(ServerPlayer player, double accumulated) {
    ServerLevel server = player.serverLevel();
    Vec3 look = player.getLookAngle().normalize();
    Vec3 dash = look.scale(4.0D);
    player.setDeltaMovement(dash);
    player.hurtMarked = true;
    double attackValue = 0.0D;
    net.minecraft.world.entity.ai.attributes.AttributeInstance attackAttribute =
        player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (attackAttribute != null) {
      attackValue = attackAttribute.getValue();
    }
    float damage = (float) Math.min(8.0D + attackValue * 0.6D, accumulated * 0.35D);
    if (damage > 0.0f) {
      AABB sweep = player.getBoundingBox().expandTowards(dash).inflate(1.0D);
      for (LivingEntity target :
          server.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != player)) {
        if (!target.isAlive()) {
          continue;
        }
        target.hurt(player.damageSources().playerAttack(player), damage);
      }
    }
    PENDING_IFRAMES.put(player, Math.max(PENDING_IFRAMES.getOrDefault(player, 0), 10));
    player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), 20);
    server.playSound(
        null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 0.7f);
  }

  private boolean hasTieGuGu(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return false;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (matchesOrgan(stack, TIE_GU_GU_ID)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasHuPiGu(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return false;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (matchesOrgan(stack, HU_PI_GU_ID)) {
        return true;
      }
    }
    return false;
  }

  private static ItemStack locateOrgan(ChestCavityInstance cc) {
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

  @SubscribeEvent(priority = EventPriority.HIGHEST)
  public static void captureIncomingDamage(LivingIncomingDamageEvent event) {
    LivingEntity entity = event.getEntity();
    if (entity != null) {
      ORIGINAL_DAMAGE.put(entity, event.getAmount());
    }
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void clearIncomingDamage(LivingIncomingDamageEvent event) {
    LivingEntity entity = event.getEntity();
    if (entity != null) {
      ORIGINAL_DAMAGE.remove(entity);
    }
  }

  @SubscribeEvent
  public static void onLivingDeath(LivingDeathEvent event) {
    LivingEntity victim = event.getEntity();
    DamageSource source = event.getSource();
    Entity attacker = source.getEntity();
    if (!(attacker instanceof Player player)) {
      return;
    }
    Optional<ChestCavityEntity> optional = ChestCavityEntity.of(player);
    if (optional.isEmpty()) {
      return;
    }
    ChestCavityInstance cc = optional.get().getChestCavityInstance();
    if (cc == null) {
      return;
    }
    ItemStack organ = locateOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }
    OrganState state = INSTANCE.organState(organ, STATE_ROOT);
    if (victim.getType().getCategory() == MobCategory.CREATURE) {
      long now = player.level().getGameTime();
      INSTANCE.registerHxp(cc, organ, state, HxpSource.KILL, now);
    }
  }

  @SubscribeEvent
  public static void onServerTick(ServerTickEvent.Post event) {
    if (PENDING_IFRAMES.isEmpty()) {
      return;
    }
    PENDING_IFRAMES
        .entrySet()
        .removeIf(
            entry -> {
              LivingEntity entity = entry.getKey();
              if (entity == null) {
                return true;
              }
              int remaining = Math.max(0, entry.getValue() - 1);
              entity.invulnerableTime = Math.max(entity.invulnerableTime, remaining);
              entry.setValue(remaining);
              return remaining <= 0;
            });
  }
}
