package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.skills.XueFengJiBiSkill;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.skills.XueShuShouJinSkill;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.skills.XueYongPiShenSkill;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.skills.YiXueFanCiSkill;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageContext;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NetworkUtil;
import org.slf4j.Logger;

/**
 * 血衣蛊 (Xue Yi Gu) - Blood Robe Gu
 *
 * <p>Defense-oriented organ with blood manipulation abilities.
 *
 * <p>4 Active Skills:
 * - 血涌披身 (Blood Aura): Toggle aura, applies bleed DoT in radius
 * - 血束收紧 (Blood Bind): Beam attack with slowness + bleed
 * - 血缝急闭 (Blood Seal): Convert enemy bleed to absorption
 * - 溢血反刺 (Blood Reflect): 3s window to reflect melee damage as bleed
 *
 * <p>6 Passive Skills:
 * - 血衣 (Blood Armor): Gain armor stacks when hit by melee
 * - 渗透 (Penetration): Every 2 melee hits apply bleed DoT
 * - 越染越坚 (Enraged Defense): Below 50% HP gain defense + aura boost
 * - 血偿 (Blood Reward): Kill bleeding enemies to restore zhenyuan
 * - 凝血止创 (Hardened Shield): Take 200 damage to gain absorption
 * - 代价 (Cost): Active skills cost additional 2% current HP
 *
 * <p>5 Synergy Skills (with other organs)
 */
public final class XueYiGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener,
        OrganIncomingDamageListener,
        OrganOnHitListener,
        OrganRemovalListener {

  public static final XueYiGuOrganBehavior INSTANCE = new XueYiGuOrganBehavior();

  private static final Logger LOGGER = LogUtils.getLogger();
  private static final String LOG_PREFIX = "[Xue Yi Gu]";

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "xueyigu");
  private static final String STATE_ROOT = "XueYiGu";

  // State keys for passive skills
  private static final String ARMOR_STACKS_KEY = "ArmorStacks";
  private static final String LAST_DAMAGE_TICK_KEY = "LastDamageTick";
  private static final String HIT_COUNTER_KEY = "HitCounter";
  private static final String PENETRATE_READY_AT_KEY = "PenetrateReadyAt";
  private static final String ENRAGED_UNTIL_KEY = "EnragedUntil";
  private static final String DAMAGE_ACCUMULATOR_KEY = "DamageAccum";
  private static final String ABSORPTION_READY_AT_KEY = "AbsorptionReadyAt";

  // Passive parameters
  private static final int ARMOR_MAX_STACKS = 10;
  private static final float ARMOR_DAMAGE_REDUCTION_PER_STACK = 0.01f; // 1% per stack
  private static final int ARMOR_DECAY_DELAY_TICKS = 200; // 10 seconds
  private static final int PENETRATE_COOLDOWN_TICKS = 40; // 2 seconds
  private static final int ENRAGE_COOLDOWN_TICKS = 400; // 20 seconds
  private static final float ENRAGE_HEALTH_THRESHOLD = 0.5f; // 50% HP
  private static final float ENRAGE_DAMAGE_REDUCTION = 0.1f; // +10% DR
  private static final float ENRAGE_AURA_BOOST = 0.2f; // +20% aura damage
  private static final float HARDENED_SHIELD_DAMAGE_THRESHOLD = 200.0f;
  private static final int HARDENED_SHIELD_COOLDOWN_TICKS = 600; // 30 seconds
  private static final float LIFE_COST_PERCENTAGE = 0.02f; // 2% current HP

  private XueYiGuOrganBehavior() {}

  /**
   * Called when organ is equipped to initialize state.
   */
  public void onEquip(
      ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
    if (cc == null || organ == null || organ.isEmpty()) {
      return;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return;
    }

    // Register removal listener
    registerRemovalHook(cc, organ, this, staleRemovalContexts);

    // Initialize state if needed
    OrganState state = organState(organ, STATE_ROOT);
    if (state.getInt(ARMOR_STACKS_KEY, -1) == -1) {
      state.setInt(ARMOR_STACKS_KEY, 0);
      state.setLong(LAST_DAMAGE_TICK_KEY, 0L);
      state.setInt(HIT_COUNTER_KEY, 0);
      state.setLong(PENETRATE_READY_AT_KEY, 0L);
      state.setLong(ENRAGED_UNTIL_KEY, 0L);
      state.setDouble(DAMAGE_ACCUMULATOR_KEY, 0.0);
      state.setLong(ABSORPTION_READY_AT_KEY, 0L);
    }

    sendSlotUpdate(cc, organ);
  }

  // ============================================================
  // Slow Tick - Active Skills & Passive Maintenance
  // ============================================================

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof ServerPlayer player) || cc == null || organ == null) {
      return;
    }
    if (entity.level().isClientSide()) {
      return;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return;
    }

    // Tick active skills
    XueYongPiShenSkill.tickAura(player, cc, organ);
    YiXueFanCiSkill.tickReflectWindow(player, cc, organ);

    // Tick passive: Armor stack decay
    tickArmorStackDecay(player, organ, cc);

    // Tick passive: Enrage status maintenance
    tickEnrageStatus(player, organ);
  }

  // ============================================================
  // Incoming Damage Listener - Passives & Reflect
  // ============================================================

  @Override
  public float onIncomingDamage(
      DamageSource source,
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!(victim instanceof ServerPlayer player)) {
      return damage;
    }

    if (cc == null) {
      return damage;
    }

    if (!matchesOrgan(organ, ORGAN_ID)) {
      return damage;
    }

    OrganState state = organState(organ, STATE_ROOT);

    float finalDamage = damage;

    // Passive: Blood Armor (reduce damage based on stacks)
    if (isMeleeDamage(source)) {
      int armorStacks = state.getInt(ARMOR_STACKS_KEY, 0);
      if (armorStacks > 0) {
        float reduction = armorStacks * ARMOR_DAMAGE_REDUCTION_PER_STACK;
        finalDamage *= (1.0f - reduction);

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "{} Armor stacks {} reduced damage from {} to {}",
              LOG_PREFIX,
              armorStacks,
              damage,
              finalDamage);
        }
      }

      // Gain armor stack from melee hit
      gainArmorStack(player, organ, state, cc);
    }

    // Passive: Enraged Defense (below 50% HP)
    if (isEnraged(player, state)) {
      finalDamage *= (1.0f - ENRAGE_DAMAGE_REDUCTION);
    }

    // Active: Blood Reflect (if window is active and melee)
    if (isMeleeDamage(source) && source.getEntity() instanceof LivingEntity attacker) {
      YiXueFanCiSkill.handleReflectDamage(player, attacker, finalDamage, cc);
    }

    // Passive: Hardened Blood Shield (accumulate damage)
    accumulateDamageForShield(player, organ, state, cc, finalDamage);

    return finalDamage;
  }

  // ============================================================
  // On Hit Listener - Penetration Passive
  // ============================================================

  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!(attacker instanceof ServerPlayer player)) {
      return damage;
    }
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return damage;
    }

    OrganState state = organState(organ, STATE_ROOT);

    // Passive: Penetration (every 2 hits apply bleed)
    handlePenetration(player, organ, state, cc, target);

    return damage;
  }

  // ============================================================
  // Removal Listener
  // ============================================================

  @Override
  public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!matchesOrgan(organ, ORGAN_ID)) {
      return;
    }

    // Deactivate all active skills
    XueYongPiShenSkill.forceDeactivate(organ);
    YiXueFanCiSkill.forceDeactivate(organ);

    // Clear all passive state
    OrganState state = organState(organ, STATE_ROOT);
    state.setInt(ARMOR_STACKS_KEY, 0);
    state.setInt(HIT_COUNTER_KEY, 0);
    state.setDouble(DAMAGE_ACCUMULATOR_KEY, 0.0);
  }

  // ============================================================
  // Passive Skill Implementations
  // ============================================================

  /**
   * Passive 1: Blood Armor - Gain armor stacks when hit by melee.
   */
  private void gainArmorStack(
      ServerPlayer player, ItemStack organ, OrganState state, ChestCavityInstance cc) {
    int currentStacks = state.getInt(ARMOR_STACKS_KEY, 0);
    if (currentStacks >= ARMOR_MAX_STACKS) {
      return;
    }

    currentStacks++;
    state.setInt(ARMOR_STACKS_KEY, currentStacks);
    state.setLong(LAST_DAMAGE_TICK_KEY, player.level().getGameTime());

    // Play visual effect
    if (player.level() instanceof ServerLevel serverLevel) {
      XueYiGuEffects.playArmorStackGain(serverLevel, player, currentStacks);
    }

    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }

  /**
   * Ticks armor stack decay (lose 1 stack per second after 10s without damage).
   */
  private void tickArmorStackDecay(ServerPlayer player, ItemStack organ, ChestCavityInstance cc) {
    OrganState state = organState(organ, STATE_ROOT);
    int currentStacks = state.getInt(ARMOR_STACKS_KEY, 0);
    if (currentStacks == 0) {
      return;
    }

    long lastDamageTick = state.getLong(LAST_DAMAGE_TICK_KEY, 0L);
    long now = player.level().getGameTime();
    long timeSinceLastDamage = now - lastDamageTick;

    if (timeSinceLastDamage >= ARMOR_DECAY_DELAY_TICKS) {
      // Start decaying: lose 1 stack per second (20 ticks)
      long ticksSinceDecayStart = timeSinceLastDamage - ARMOR_DECAY_DELAY_TICKS;
      int stacksToLose = (int) (ticksSinceDecayStart / 20);

      if (stacksToLose > 0) {
        currentStacks = Math.max(0, currentStacks - stacksToLose);
        state.setInt(ARMOR_STACKS_KEY, currentStacks);
        state.setLong(
            LAST_DAMAGE_TICK_KEY, now - ARMOR_DECAY_DELAY_TICKS - (ticksSinceDecayStart % 20));
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
      }
    }
  }

  /**
   * Passive 2: Penetration - Every 2 melee hits apply bleed DoT.
   */
  private void handlePenetration(
      ServerPlayer player,
      ItemStack organ,
      OrganState state,
      ChestCavityInstance cc,
      LivingEntity target) {
    long now = player.level().getGameTime();
    long readyAt = state.getLong(PENETRATE_READY_AT_KEY, 0L);

    if (now < readyAt) {
      return; // On cooldown
    }

    int hitCount = state.getInt(HIT_COUNTER_KEY, 0);
    hitCount++;

    if (hitCount >= 2) {
      // Apply penetration bleed
      applyPenetrationBleed(player, target);

      // Play effect
      if (player.level() instanceof ServerLevel serverLevel) {
        XueYiGuEffects.playPenetrateEffect(serverLevel, target);
      }

      // Reset counter and set cooldown
      hitCount = 0;
      state.setLong(PENETRATE_READY_AT_KEY, now + PENETRATE_COOLDOWN_TICKS);
    }

    state.setInt(HIT_COUNTER_KEY, hitCount);
  }

  /**
   * Applies penetration bleed to target.
   */
  private void applyPenetrationBleed(ServerPlayer player, LivingEntity target) {
    // TODO: Apply actual bleed DoT (6 damage/sec for 3 seconds)
    // For now, apply instant damage
    target.hurt(player.damageSources().magic(), 6.0f);
  }

  /**
   * Passive 3: Enraged Defense - Checks and activates enrage status.
   */
  private boolean isEnraged(ServerPlayer player, OrganState state) {
    long now = player.level().getGameTime();
    long enragedUntil = state.getLong(ENRAGED_UNTIL_KEY, 0L);

    if (now < enragedUntil) {
      return true; // Already enraged
    }

    // Check if we should trigger enrage
    float healthPercent = player.getHealth() / player.getMaxHealth();
    if (healthPercent <= ENRAGE_HEALTH_THRESHOLD) {
      // Trigger enrage
      long enrageEnd = now + ENRAGE_COOLDOWN_TICKS;
      state.setLong(ENRAGED_UNTIL_KEY, enrageEnd);

      // Play activation effect
      if (player.level() instanceof ServerLevel serverLevel) {
        XueYiGuEffects.playEnrageActivation(serverLevel, player);
      }

      return true;
    }

    return false;
  }

  /**
   * Maintains enrage visual effects.
   */
  private void tickEnrageStatus(ServerPlayer player, ItemStack organ) {
    OrganState state = organState(organ, STATE_ROOT);
    long now = player.level().getGameTime();
    long enragedUntil = state.getLong(ENRAGED_UNTIL_KEY, 0L);

    if (now < enragedUntil && now % 10 == 0) {
      // Play maintain effect every 0.5 seconds
      // XueYiGuEffects.playEnrageMaintain(...) if needed
    }
  }

  /**
   * Passive 5: Hardened Blood Shield - Accumulate damage for absorption.
   */
  private void accumulateDamageForShield(
      ServerPlayer player,
      ItemStack organ,
      OrganState state,
      ChestCavityInstance cc,
      float damage) {
    long now = player.level().getGameTime();
    long readyAt = state.getLong(ABSORPTION_READY_AT_KEY, 0L);

    if (now < readyAt) {
      return; // On cooldown
    }

    double accumulated = state.getDouble(DAMAGE_ACCUMULATOR_KEY, 0.0);
    accumulated += damage;

    if (accumulated >= HARDENED_SHIELD_DAMAGE_THRESHOLD) {
      // Trigger shield
      float absorptionAmount = 30.0f; // Fixed amount
      applyHardenedShield(player, absorptionAmount);

      // Play effect
      if (player.level() instanceof ServerLevel serverLevel) {
        XueYiGuEffects.playHardenedBloodShield(serverLevel, player, absorptionAmount);
      }

      // Reset and set cooldown
      accumulated = 0.0;
      state.setLong(ABSORPTION_READY_AT_KEY, now + HARDENED_SHIELD_COOLDOWN_TICKS);
      NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    state.setDouble(DAMAGE_ACCUMULATOR_KEY, accumulated);
  }

  /**
   * Applies hardened blood shield absorption.
   */
  private void applyHardenedShield(ServerPlayer player, float amount) {
    // Add absorption effect
    player.addEffect(
        new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.ABSORPTION, 200, 0, false, false, true));
    player.setAbsorptionAmount(player.getAbsorptionAmount() + amount);
  }

  /**
   * Passive 6: Cost - Deducts additional HP cost for active skills.
   */
  public static void applyLifeCost(ServerPlayer player, ChestCavityInstance cc) {
    float currentHealth = player.getHealth();
    float cost = currentHealth * LIFE_COST_PERCENTAGE;

    if (currentHealth - cost <= 1.0f) {
      // Don't kill player with cost
      cost = currentHealth - 1.0f;
    }

    if (cost > 0) {
      GuzhenrenResourceCostHelper.drainHealth(player, cost, 1.0f, player.damageSources().generic());

      // Play effect
      if (player.level() instanceof ServerLevel serverLevel) {
        XueYiGuEffects.playLifeCost(serverLevel, player);
      }
    }
  }

  // ============================================================
  // Utility Methods
  // ============================================================

  private boolean isMeleeDamage(DamageSource source) {
    return source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE) == false
        && source.getDirectEntity() instanceof LivingEntity;
  }

  private Optional<ItemStack> findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return Optional.empty();
    }

    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (!stack.isEmpty()) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (ORGAN_ID.equals(itemId)) {
          return Optional.of(stack);
        }
      }
    }

    return Optional.empty();
  }

  // Bootstrap all active skills
  public static void bootstrapSkills() {
    XueYongPiShenSkill.bootstrap();
    XueShuShouJinSkill.bootstrap();
    XueFengJiBiSkill.bootstrap();
    YiXueFanCiSkill.bootstrap();
  }
}
