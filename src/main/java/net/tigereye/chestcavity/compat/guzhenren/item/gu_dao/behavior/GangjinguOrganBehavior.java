package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.SteelBoneComboHelper;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/** Behaviour for 钢筋蛊：hunger-gated absorption, jingli restoration and combo-driven buffs. */
public final class GangjinguOrganBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener, OrganOnHitListener {

  public static final GangjinguOrganBehavior INSTANCE = new GangjinguOrganBehavior();

  private static final String STATE_ROOT = "Gangjingu";
  private static final String ABSORPTION_KEY = "LastAbsorptionTick"; // legacy stamp
  private static final String ABSORPTION_READY_AT_KEY = "AbsorptionReadyAt"; // scheduling key
  private static final int ABSORPTION_INTERVAL_TICKS =
      BehaviorConfigAccess.getInt(
          GangjinguOrganBehavior.class, "ABSORPTION_INTERVAL_TICKS", 20 * 30); // 45s
  public static final float ABSORPTION_PER_STACK = 60.0f;
  private static final double JINGLI_PER_SECOND = 1.0;
  private static final double BONUS_DAMAGE_CHANCE = 0.38;
  private static final double BONUS_DAMAGE_RATIO = 0.25;
  private static final int EFFECT_DURATION_TICKS =
      BehaviorConfigAccess.getInt(GangjinguOrganBehavior.class, "EFFECT_DURATION_TICKS", 60);
  private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

  /**
   * Non-player upkeep cost expressed in health per slow tick (~0.1 heart). Keeps mobs from
   * maintaining steel plating for free while avoiding lethal drain.
   */
  private static final float NON_PLAYER_MAINTENANCE_HEALTH_COST =
      BehaviorConfigAccess.getFloat(
          GangjinguOrganBehavior.class, "NON_PLAYER_MAINTENANCE_HEALTH_COST", 0.2f);

  /** Leaves at least one heart to prevent upkeep from finishing off weakened mobs. */
  private static final float NON_PLAYER_MINIMUM_HEALTH_RESERVE =
      BehaviorConfigAccess.getFloat(
          GangjinguOrganBehavior.class, "NON_PLAYER_MINIMUM_HEALTH_RESERVE", 2.0f);

  private static final boolean DEBUG_METAL_BONE =
      Boolean.getBoolean("chestcavity.debugMetalBoneAbsorption");

  private static final ResourceLocation GU_DAO_CHANNEL =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "linkage/gu_dao_increase_effect");
  private static final ResourceLocation JIN_DAO_CHANNEL =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "linkage/jin_dao_increase_effect");
  private static final ResourceLocation BONE_GROWTH_CHANNEL =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "linkage/bone_growth");

  private GangjinguOrganBehavior() {}

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (entity == null || entity.level().isClientSide()) {
      return;
    }
    if (!SteelBoneComboHelper.isPrimarySteelOrgan(cc, organ)) {
      ChestCavity.LOGGER.debug(
          "[compat/guzhenren][steel_bone] skip slow tick – not primary stack for {}",
          describeStack(organ));
      return;
    }

    SteelBoneComboHelper.ComboState comboState = SteelBoneComboHelper.analyse(cc);
    if (!SteelBoneComboHelper.hasSteel(comboState)) {
      ChestCavity.LOGGER.debug(
          "[compat/guzhenren][steel_bone] skip slow tick – analyse() returned no steel (state={})",
          comboState);
      return;
    }

    if (entity instanceof net.minecraft.world.entity.player.Player player) {
      handlePlayerSlowTick(player, cc, organ, comboState);
    } else {
      handleNonPlayerSlowTick(entity, cc, organ, comboState);
    }
  }

  private void handlePlayerSlowTick(
      net.minecraft.world.entity.player.Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      SteelBoneComboHelper.ComboState comboState) {
    if (!SteelBoneComboHelper.consumeMaintenanceHunger(player)) {
      if (DEBUG_METAL_BONE) {
        ChestCavity.LOGGER.info(
            "[compat/guzhenren][steel_bone] hunger gate blocked absorption for {}",
            describeStack(organ));
      }
      return;
    }

    int steelStacks = Math.max(1, comboState.steel());
    performSteelBoneMaintenance(player, cc, organ, comboState, steelStacks);
    SteelBoneComboHelper.restoreJingli(player, JINGLI_PER_SECOND * steelStacks);
  }

  private void handleNonPlayerSlowTick(
      LivingEntity entity,
      ChestCavityInstance cc,
      ItemStack organ,
      SteelBoneComboHelper.ComboState comboState) {
    if (!payNonPlayerMaintenance(entity, organ)) {
      return;
    }

    int steelStacks = Math.max(1, comboState.steel());
    performSteelBoneMaintenance(entity, cc, organ, comboState, steelStacks);
  }

  private void performSteelBoneMaintenance(
      LivingEntity entity,
      ChestCavityInstance cc,
      ItemStack organ,
      SteelBoneComboHelper.ComboState comboState,
      int steelStacks) {
    SteelBoneComboHelper.ensureAbsorptionCapacity(entity, cc);
    if (DEBUG_METAL_BONE) {
      var maxAttr =
          entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION);
      double maxAbsorption = maxAttr != null ? maxAttr.getBaseValue() : Double.NaN;
      ChestCavity.LOGGER.info(
          "[compat/guzhenren][steel_bone] post-capacity: maxAbsorption={} currentAbsorption={} stacks={}",
          Double.isNaN(maxAbsorption)
              ? "<missing>"
              : String.format(java.util.Locale.ROOT, "%.1f", maxAbsorption),
          String.format(java.util.Locale.ROOT, "%.1f", entity.getAbsorptionAmount()),
          comboState.steel());
    }

    scheduleSteelAbsorptionIfNeeded(entity, cc, organ, steelStacks);

    if (SteelBoneComboHelper.hasActiveCombo(comboState)) {
      applyResistance(entity, cc);
    }
    if (SteelBoneComboHelper.hasRefinedCombo(comboState)) {
      applyHaste(entity, cc);
    }
  }

  private boolean payNonPlayerMaintenance(LivingEntity entity, ItemStack organ) {
    if (NON_PLAYER_MAINTENANCE_HEALTH_COST <= 0.0f) {
      return true;
    }
    boolean drained =
        GuzhenrenResourceCostHelper.drainHealth(
            entity,
            NON_PLAYER_MAINTENANCE_HEALTH_COST,
            NON_PLAYER_MINIMUM_HEALTH_RESERVE,
            entity.damageSources().starve());
    if (!drained && DEBUG_METAL_BONE) {
      ChestCavity.LOGGER.info(
          "[compat/guzhenren][steel_bone] non-player maintenance blocked – insufficient health for {}",
          describeStack(organ));
    }
    return drained;
  }

  private void scheduleSteelAbsorptionIfNeeded(
      LivingEntity entity, ChestCavityInstance cc, ItemStack organ, int steelStacks) {
    if (!(entity.level() instanceof ServerLevel server)) return;
    OrganState state = organState(organ, STATE_ROOT);
    MultiCooldown mc = MultiCooldown.builder(state).withSync(cc, organ).build();
    MultiCooldown.Entry ready = mc.entry(ABSORPTION_READY_AT_KEY);
    long now = server.getGameTime();
    if (ready.getReadyTick() <= 0L || now >= ready.getReadyTick()) {
      ready.setReadyAt(now + Math.max(1, ABSORPTION_INTERVAL_TICKS));
    }
    ready.onReady(
        server,
        now,
        () -> {
          try {
            float required = Math.max(0.0f, ABSORPTION_PER_STACK * Math.max(1, steelStacks));
            if (required > 0.0f) {
              float current = entity.getAbsorptionAmount();
              if (current + 1.0E-3f < required) {
                entity.setAbsorptionAmount(Math.max(current, required));
                if (DEBUG_METAL_BONE) {
                  ChestCavity.LOGGER.info(
                      "[compat/guzhenren][steel_bone] apply absorption -> {} (stacks={})",
                      String.format(java.util.Locale.ROOT, "%.1f", required),
                      steelStacks);
                }
              }
            }
          } finally {
            long next = server.getGameTime() + Math.max(1, ABSORPTION_INTERVAL_TICKS);
            MultiCooldown.Entry e =
                MultiCooldown.builder(state)
                    .withSync(cc, organ)
                    .build()
                    .entry(ABSORPTION_READY_AT_KEY);
            e.setReadyAt(next);
            e.onReady(server, server.getGameTime(), () -> {});
            OrganStateOps.setLong(
                state, cc, organ, ABSORPTION_KEY, server.getGameTime(), v -> v, Long.MIN_VALUE);
          }
        });
  }

  private void applyResistance(LivingEntity entity, ChestCavityInstance cc) {
    double increase = Math.max(0.0, SteelBoneComboHelper.guDaoIncrease(cc));
    int amplifier = Math.max(0, (int) Math.round(increase));
    entity.addEffect(
        new MobEffectInstance(
            MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION_TICKS, amplifier, true, true));
  }

  private void applyHaste(LivingEntity entity, ChestCavityInstance cc) {
    double jinIncrease = Math.max(0.0, SteelBoneComboHelper.jinDaoIncrease(cc));
    int amplifier = Math.max(0, (int) Math.round(jinIncrease));
    entity.addEffect(
        new MobEffectInstance(MobEffects.DIG_SPEED, EFFECT_DURATION_TICKS, amplifier, true, true));
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
    if (!SteelBoneComboHelper.isPrimarySteelOrgan(cc, organ)) {
      return damage;
    }
    if (target == null || source.getDirectEntity() != attacker) {
      return damage;
    }
    if (attacker.distanceToSqr(target) > 100.0) {
      return damage;
    }

    RandomSource random = attacker.getRandom();
    if (random.nextDouble() >= BONUS_DAMAGE_CHANCE) {
      return damage;
    }

    double jinIncrease = Math.max(0.0, SteelBoneComboHelper.jinDaoIncrease(cc));
    double bonus = damage * BONUS_DAMAGE_RATIO * (1.0 + jinIncrease);
    if (bonus <= 0.0) {
      return damage;
    }

    float result = (float) (damage + bonus);
    ChestCavity.LOGGER.debug(
        "[compat/guzhenren] Gangjingu bonus damage +{}/{} (jinIncrease={})",
        String.format(java.util.Locale.ROOT, "%.2f", bonus),
        String.format(java.util.Locale.ROOT, "%.2f", result),
        String.format(java.util.Locale.ROOT, "%.3f", jinIncrease));
    if (attacker.level() instanceof ServerLevel server) {
      Vec3 impact = target.position().add(0.0, target.getBbHeight() * 0.4, 0.0);
      SteelBoneComboHelper.spawnImpactFx(server, target, impact);
    }
    return result;
  }

  public void ensureAttached(ChestCavityInstance cc) {
    if (cc == null) {
      return;
    }
    ActiveLinkageContext context = LinkageManager.getContext(cc);
    ensureChannel(context, GU_DAO_CHANNEL).addPolicy(NON_NEGATIVE);
    ensureChannel(context, JIN_DAO_CHANNEL).addPolicy(NON_NEGATIVE);
    ensureChannel(context, BONE_GROWTH_CHANNEL).addPolicy(NON_NEGATIVE);
  }
}
