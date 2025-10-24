package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.engine.reaction.ResidueManager;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowControllerManager;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgram;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgramRegistry;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.registration.CCItems;
import net.tigereye.chestcavity.util.AbsorptionHelper;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import org.slf4j.Logger;

/** Behaviour implementation for 冰肌蛊 (Bing Ji Gu). */
public final class BingJiGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener, OrganOnHitListener, OrganRemovalListener {

  public static final BingJiGuOrganBehavior INSTANCE = new BingJiGuOrganBehavior();

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "bing_ji_gu");
  private static final ResourceLocation JADE_BONE_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yu_gu_gu");
  private static final ResourceLocation ICE_BURST_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "bing_bao_gu");
  private static final ResourceLocation BING_XUE_INCREASE_EFFECT =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/bing_xue_dao_increase_effect");
  private static final ResourceLocation BLEED_EFFECT_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "lliuxue");
  private static final ResourceLocation ICE_COLD_EFFECT_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "hhanleng");

  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "bing_ji_gu_iceburst");
  private static final ResourceLocation ICE_BURST_FLOW_ID =
      ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "bing_xue_burst");
  private static final ResourceLocation ABSORPTION_MODIFIER_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/bing_ji_gu_absorption");

  private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

  private static final String STATE_ROOT = "BingJiGu";
  private static final String ABSORPTION_TIMER_KEY =
      "AbsorptionTimer"; // legacy int (unused for scheduling)
  private static final String ABSORPTION_READY_AT_KEY =
      "AbsorptionReadyAt"; // timestamp-style scheduling
  private static final String INVULN_COOLDOWN_KEY = "InvulnCooldown"; // legacy int key (unused)
  private static final String INVULN_READY_AT_KEY = "InvulnReadyAt"; // timestamp-style readyAt
  private static final boolean DEBUG = false;

  private static final CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig DEFAULTS =
      new CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig();

  private static CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig cfg() {
    CCConfig root = ChestCavity.config;
    if (root != null) {
      CCConfig.GuzhenrenBingXueDaoConfig group = root.GUZHENREN_BING_XUE_DAO;
      if (group != null && group.BING_JI_GU != null) {
        return group.BING_JI_GU;
      }
    }
    return DEFAULTS;
  }

  static {
    OrganActivationListeners.register(ABILITY_ID, BingJiGuOrganBehavior::activateAbility);
  }

  private BingJiGuOrganBehavior() {}

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof Player player) || entity.level().isClientSide() || cc == null) {
      return;
    }
    if (!matchesOrgan(organ, ORGAN_ID) && !organ.is(CCItems.GUZHENREN_BING_JI_GU)) {
      return;
    }

    int stackCount = Math.max(1, organ.getCount());
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt =
        GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      if (DEBUG) {
        LOGGER.info(
            "[compat/guzhenren][ice_skin] no resource handle, skip slow tick for {}",
            player.getGameProfile().getName());
      }
      return;
    }
    GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
    CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig config = cfg();

    ActiveLinkageContext context = LinkageManager.getContext(cc);
    double efficiency = 1.0 + lookupIncreaseEffect(context);

    boolean paid =
        ResourceOps.tryConsumeScaledZhenyuan(handle, config.zhenyuanBaseCost * stackCount)
            .isPresent();
    if (DEBUG) {
      LOGGER.info(
          "[compat/guzhenren][ice_skin] slow tick: stacks={}, eff={}, paidZhenyuan={} (cost={})",
          stackCount,
          String.format(java.util.Locale.ROOT, "%.3f", efficiency),
          paid,
          String.format(java.util.Locale.ROOT, "%.1f", config.zhenyuanBaseCost * stackCount));
    }
    MultiCooldown cooldown = createCooldown(cc, organ);
    MultiCooldown.Entry absorptionReady = cooldown.entry(ABSORPTION_READY_AT_KEY);
    MultiCooldown.Entry invulnReady = cooldown.entry(INVULN_READY_AT_KEY);

    if (paid) {
      ResourceOps.adjustJingli(player, config.jingliPerTick * stackCount);
      float healAmount = config.healPerTick * stackCount;
      if (healAmount > 0.0f) {
        ChestCavityUtil.runWithOrganHeal(() -> player.heal(healAmount));
        if (DEBUG) {
          LOGGER.info(
              "[compat/guzhenren][ice_skin] healed +{} and added jingli +{}",
              healAmount,
              config.jingliPerTick * stackCount);
        }
      }
      scheduleAbsorptionIfNeeded(
          player, cc, organ, absorptionReady, stackCount, efficiency, config);
      if (hasJadeBone(cc)) {
        clearBleed(player);
        if (DEBUG) {
          LOGGER.info("[compat/guzhenren][ice_skin] jade bone present: cleared bleed if any");
        }
      }
    }

    if (player.level() instanceof ServerLevel serverLevel) {
      tickInvulnerability(player, invulnReady, cc, serverLevel, config);
    }
  }

  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (attacker == null || target == null || attacker.level().isClientSide()) {
      return damage;
    }
    if (!matchesOrgan(organ, ORGAN_ID) && !organ.is(CCItems.GUZHENREN_BING_JI_GU)) {
      return damage;
    }
    CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig config = cfg();
    if (damage <= 0.0f || attacker.getRandom().nextDouble() >= config.bonusTriggerChance) {
      return damage;
    }

    double efficiency = 1.0 + lookupIncreaseEffect(LinkageManager.getContext(cc));
    float bonus = (float) (damage * config.bonusDamageFraction * Math.max(0.0, efficiency));
    if (bonus > 0.0f) {
      applyColdEffect(target, config);
      if (DEBUG) {
        LOGGER.info(
            "[compat/guzhenren][ice_skin] onHit bonus: baseDamage={} bonus={} eff={}",
            String.format(java.util.Locale.ROOT, "%.2f", damage),
            String.format(java.util.Locale.ROOT, "%.2f", bonus),
            String.format(java.util.Locale.ROOT, "%.3f", efficiency));
      }
      return damage + bonus;
    }
    applyColdEffect(target, config);
    return damage;
  }

  @Override
  public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!matchesOrgan(organ, ORGAN_ID) && !organ.is(CCItems.GUZHENREN_BING_JI_GU)) {
      return;
    }
    AbsorptionHelper.clearAbsorptionCapacity(entity, ABSORPTION_MODIFIER_ID);
  }

  public void onEquip(
      ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
    if (cc == null || organ == null || organ.isEmpty()) {
      return;
    }
    if (!matchesOrgan(organ, ORGAN_ID) && !organ.is(CCItems.GUZHENREN_BING_JI_GU)) {
      return;
    }
    RemovalRegistration registration = registerRemovalHook(cc, organ, this, staleRemovalContexts);
    if (!registration.alreadyRegistered()) {
      MultiCooldown cooldown = createCooldown(cc, organ);
      cooldown.entryInt(ABSORPTION_TIMER_KEY).clear();
      cooldown.entry(INVULN_READY_AT_KEY).setReadyAt(0L);
    }
  }

  public void ensureAttached(ChestCavityInstance cc) {
    if (cc == null) {
      return;
    }
    ActiveLinkageContext context = LinkageManager.getContext(cc);
    if (context == null) {
      return;
    }
    ensureIncreaseChannel(context, BING_XUE_INCREASE_EFFECT);
  }

  private static void ensureIncreaseChannel(ActiveLinkageContext context, ResourceLocation id) {
    if (context == null || id == null) {
      return;
    }
    context.getOrCreateChannel(id).addPolicy(NON_NEGATIVE);
  }

  private static double lookupIncreaseEffect(ActiveLinkageContext context) {
    if (context == null) {
      return 0.0;
    }
    return context.lookupChannel(BING_XUE_INCREASE_EFFECT).map(LinkageChannel::get).orElse(0.0);
  }

  private static void scheduleAbsorptionIfNeeded(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      MultiCooldown.Entry ready,
      int stacks,
      double efficiency,
      CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig config) {
    if (player == null || player.level().isClientSide() || ready == null) return;
    if (!(player.level() instanceof ServerLevel server)) return;
    long now = server.getGameTime();
    if (ready.getReadyTick() <= now) {
      ready.setReadyAt(now + config.slowTickIntervalsPerMinute);
    }
    ready.onReady(
        server,
        now,
        () -> {
          try {
            double eff = efficiency; // recompute live
            ActiveLinkageContext context = LinkageManager.getContext(cc);
            if (context != null) {
              eff = 1.0 + lookupIncreaseEffect(context);
            }
            int sc = Math.max(1, organ == null ? stacks : organ.getCount());
            float gain =
                (float) (config.absorptionPerTrigger * Math.max(1, sc) * Math.max(0.0, eff));
            float before = player.getAbsorptionAmount();
            double cap = config.absorptionCap;
            double target = Math.min(cap, before + gain);
            AbsorptionHelper.applyAbsorption(player, (float) target, ABSORPTION_MODIFIER_ID, false);
            if (DEBUG) {
              LOGGER.info(
                  "[compat/guzhenren][ice_skin] absorption tick: +{} (eff={}, stacks={}) {} -> {}",
                  String.format(java.util.Locale.ROOT, "%.1f", gain),
                  String.format(java.util.Locale.ROOT, "%.3f", eff),
                  sc,
                  String.format(java.util.Locale.ROOT, "%.1f", before),
                  String.format(java.util.Locale.ROOT, "%.1f", player.getAbsorptionAmount()));
            }
            long nextAt = server.getGameTime() + config.slowTickIntervalsPerMinute;
            MultiCooldown.Entry e = createCooldown(cc, organ).entry(ABSORPTION_READY_AT_KEY);
            e.setReadyAt(nextAt);
            e.onReady(server, server.getGameTime(), () -> {});
          } catch (Throwable ignored) {
          }
        });
  }

  private static boolean tickInvulnerability(
      Player player,
      MultiCooldown.Entry invulnReady,
      ChestCavityInstance cc,
      ServerLevel server,
      CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig config) {
    if (player == null || invulnReady == null || server == null) {
      return false;
    }
    long now = server.getGameTime();
    long readyAt = Math.max(0L, invulnReady.getReadyTick());
    if (now < readyAt) {
      return false;
    }
    if (hasJadeBone(cc)
        && isBeimingConstitution(player)
        && player.getHealth() <= player.getMaxHealth() * config.lowHealthThreshold) {
      player.invulnerableTime =
          Math.max(player.invulnerableTime, config.invulnerabilityDurationTicks);
      player.addEffect(
          new MobEffectInstance(
              MobEffects.DAMAGE_RESISTANCE,
              config.invulnerabilityDurationTicks,
              4,
              false,
              true,
              true));
      invulnReady.setReadyAt(now + config.invulnerabilityCooldownTicks);
      if (DEBUG) {
        LOGGER.info(
            "[compat/guzhenren][ice_skin] invulnerability granted: duration={}t, cooldown={}t",
            config.invulnerabilityDurationTicks,
            config.invulnerabilityCooldownTicks);
      }
      return true;
    }
    return false;
  }

  private static MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ) {
    MultiCooldown.Builder builder =
        MultiCooldown.builder(OrganState.of(organ, STATE_ROOT))
            .withIntClamp(value -> Math.max(0, value), 0);
    if (cc != null) {
      builder.withSync(cc, organ);
    } else {
      builder.withOrgan(organ);
    }
    return builder.build();
  }

  private static void clearBleed(LivingEntity entity) {
    if (entity == null || entity.level().isClientSide()) {
      return;
    }
    Optional<Holder.Reference<MobEffect>> holder =
        BuiltInRegistries.MOB_EFFECT.getHolder(BLEED_EFFECT_ID);
    holder.ifPresent(entity::removeEffect);
  }

  private static void applyColdEffect(
      LivingEntity target, CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig config) {
    if (target == null || target.level().isClientSide()) {
      return;
    }
    Optional<Holder.Reference<MobEffect>> holder =
        BuiltInRegistries.MOB_EFFECT.getHolder(ICE_COLD_EFFECT_ID);
    holder.ifPresent(
        effect ->
            target.addEffect(
                new MobEffectInstance(
                    effect, config.iceEffectDurationTicks, 0, false, true, true)));
    target.addEffect(
        new MobEffectInstance(
            MobEffects.MOVEMENT_SLOWDOWN, config.iceEffectDurationTicks, 0, false, true, true));
    target.addEffect(
        new MobEffectInstance(
            MobEffects.DIG_SLOWDOWN, config.iceEffectDurationTicks, 0, false, true, true));
    // 挂霜痕标记，便于“霜痕碎裂/蒸汽灼烫”反应统一触发
    int frostMarkTicks =
        ChestCavity.config != null
            ? Math.max(20, ChestCavity.config.REACTION.frostMarkDurationTicks)
            : 120;
    ReactionTagOps.add(target, ReactionTagKeys.FROST_MARK, frostMarkTicks);
  }

  private static boolean hasJadeBone(ChestCavityInstance cc) {
    return hasOrgan(cc, JADE_BONE_ID);
  }

  private static boolean hasBingBao(ChestCavityInstance cc) {
    return hasOrgan(cc, ICE_BURST_ID);
  }

  private static boolean hasOrgan(ChestCavityInstance cc, ResourceLocation id) {
    if (cc == null || id == null || cc.inventory == null) {
      return false;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack == null || stack.isEmpty()) {
        continue;
      }
      ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (Objects.equals(stackId, id)) {
        return true;
      }
    }
    return false;
  }

  private static ItemStack findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack == null || stack.isEmpty()) {
        continue;
      }
      if (stack.is(CCItems.GUZHENREN_BING_JI_GU)) {
        if (DEBUG) {
          LOGGER.info("[compat/guzhenren][ice_skin] found organ via item constant at slot {}", i);
        }
        return stack;
      }
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (Objects.equals(id, ORGAN_ID)) {
        if (DEBUG) {
          LOGGER.info("[compat/guzhenren][ice_skin] found organ via id {} at slot {}", id, i);
        }
        return stack;
      }
    }
    return ItemStack.EMPTY;
  }

  private static boolean consumeMuscle(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return false;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack == null || stack.isEmpty()) {
        continue;
      }
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (isMuscleId(id)) {
        cc.inventory.removeItem(i, 1);
        if (DEBUG) {
          LOGGER.info("[compat/guzhenren][ice_skin] consumed muscle {} at slot {}", id, i);
        }
        return true;
      }
    }
    if (DEBUG) {
      LOGGER.info("[compat/guzhenren][ice_skin] no muscle found to consume");
    }
    return false;
  }

  private static boolean isMuscleId(ResourceLocation id) {
    if (id == null) {
      return false;
    }
    if (!"chestcavity".equals(id.getNamespace())) {
      return false;
    }
    String path = id.getPath();
    // Count as muscle if it's the base human muscle ("muscle") or any suffixed variant ("*_muscle")
    return "muscle".equals(path) || path.endsWith("_muscle");
  }

  private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    if (entity == null || cc == null || entity.level().isClientSide()) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty() || !hasJadeBone(cc)) {
      if (DEBUG) {
        LOGGER.info(
            "[compat/guzhenren][ice_skin] activate rejected: organEmpty={} hasJadeBone={}",
            organ.isEmpty(),
            hasJadeBone(cc));
      }
      return;
    }
    if (!consumeMuscle(cc)) {
      if (DEBUG) {
        LOGGER.info("[compat/guzhenren][ice_skin] activate rejected: no consumable muscle found");
      }
      return;
    }
    Level level = entity.level();
    if (!(level instanceof ServerLevel server)) {
      return;
    }
    CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig config = cfg();
    int stacks = Math.max(1, organ.getCount());
    double efficiency = 1.0 + lookupIncreaseEffect(LinkageManager.getContext(cc));
    double baseDamage =
        config.iceBurstBaseDamage * Math.pow(config.iceBurstStackDamageScale, stacks - 1);
    baseDamage *= Math.max(0.0, efficiency);
    if (hasBingBao(cc)) {
      baseDamage *= 1.0 + config.iceBurstBingBaoMultiplier;
    }
    double radius = config.iceBurstRadius + Math.max(0, stacks - 1) * config.iceBurstRadiusPerStack;
    if (DEBUG) {
      LOGGER.info(
          "[compat/guzhenren][ice_skin] activating burst: stacks={}, eff={}, damageBase={}, radius={}",
          stacks,
          String.format(java.util.Locale.ROOT, "%.3f", efficiency),
          String.format(java.util.Locale.ROOT, "%.2f", baseDamage),
          String.format(java.util.Locale.ROOT, "%.2f", radius));
    }

    // Play explosion sound FX at the performer
    server.playSound(
        null,
        entity.blockPosition(),
        SoundEvents.GENERIC_EXPLODE.value(),
        SoundSource.PLAYERS,
        1.0F,
        1.0F);

    Vec3 origin = entity.position();
    List<LivingEntity> victims = gatherTargets(entity, server, radius);
    int slowDuration = Math.max(0, config.iceBurstSlowDurationTicks);
    int slowAmplifier = Mth.clamp((int) Math.round(config.iceBurstSlowAmplifier), 0, 255);
    for (LivingEntity target : victims) {
      double distance = Math.sqrt(target.distanceToSqr(origin));
      double falloff = Math.max(0.0, 1.0 - (distance / radius));
      float damage = (float) (baseDamage * falloff);
      if (damage > 0.0f) {
        DamageSource source =
            entity instanceof Player player
                ? player.damageSources().playerAttack(player)
                : server.damageSources().mobAttack(entity);
        target.hurt(source, damage);
        target.addEffect(
            new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, slowDuration, slowAmplifier, false, true, true));
        target.addEffect(
            new MobEffectInstance(
                MobEffects.DIG_SLOWDOWN, slowDuration, slowAmplifier, false, true, true));
        applyColdEffect(target, config);
        if (DEBUG) {
          LOGGER.info(
              "[compat/guzhenren][ice_skin] hit {} for {} (falloff={})",
              target.getName().getString(),
              String.format(java.util.Locale.ROOT, "%.2f", damage),
              String.format(java.util.Locale.ROOT, "%.2f", falloff));
        }
      }
    }

    triggerBurstFlow(entity, radius, victims.size(), config);

    // 在爆心留下短暂的“霜雾残留域”，用于区域控场（轻量粒子与减速均由引擎处理）
    if (server != null) {
      float residueRadius = (float) Math.max(0.5F, radius * 0.6F);
      int residueDuration = Math.max(40, (int) (config.iceEffectDurationTicks * 0.8));
      int slowAmp = Math.max(0, (int) Math.round(config.iceBurstSlowAmplifier));
      ResidueManager.spawnOrRefreshFrost(
          server, origin.x, origin.y, origin.z, residueRadius, residueDuration, slowAmp);
      // 少量雪花粒子点缀
      server.sendParticles(
          ParticleTypes.SNOWFLAKE, origin.x, origin.y + 0.2, origin.z, 12, 0.4, 0.2, 0.4, 0.02);
      if (entity instanceof Player p && !p.level().isClientSide()) {
        p.sendSystemMessage(
            net.minecraft.network.chat.Component.translatable(
                "message.chestcavity.bingxue.iceburst_residue"));
      }
    }

    // Deduct 1% of player's max health as activation cost
    if (entity instanceof Player player) {
      float max = player.getMaxHealth();
      float cost = (float) (max * 0.01f);
      if (cost > 0.0f) {
        float before = player.getHealth();
        float after = Math.max(0.0f, before - cost);
        player.setHealth(after);
        if (DEBUG) {
          LOGGER.info(
              "[compat/guzhenren][ice_skin] health cost applied: {}% ({} -> {})",
              1,
              String.format(java.util.Locale.ROOT, "%.2f", before),
              String.format(java.util.Locale.ROOT, "%.2f", after));
        }
      }
    }
  }

  private static List<LivingEntity> gatherTargets(
      LivingEntity user, ServerLevel level, double radius) {
    AABB area = user.getBoundingBox().inflate(radius);
    return level.getEntitiesOfClass(
        LivingEntity.class,
        area,
        target -> target != user && target.isAlive() && !target.isAlliedTo(user));
  }

  private static void triggerBurstFlow(
      LivingEntity entity,
      double radius,
      int victims,
      CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig config) {
    if (!(entity instanceof ServerPlayer player)) {
      return;
    }
    Optional<FlowProgram> programOpt = FlowProgramRegistry.get(ICE_BURST_FLOW_ID);
    if (programOpt.isEmpty()) {
      if (DEBUG) {
        LOGGER.info("[compat/guzhenren][ice_skin] burst flow {} not found", ICE_BURST_FLOW_ID);
      }
      return;
    }
    ServerLevel level = player.serverLevel();
    FlowController controller = FlowControllerManager.get(player);
    Map<String, String> params = new HashMap<>();
    params.put("burst.radius", formatDouble(Math.max(0.0D, radius)));
    double victimContribution = Math.max(0, victims) * 0.25D;
    double radiusContribution = Math.max(0.0D, radius - config.iceBurstRadius) * 0.1D;
    double scale = Math.max(1.0D, Math.min(6.0D, 1.0D + victimContribution + radiusContribution));
    params.put("burst.scale", formatDouble(scale));
    FlowProgram program = programOpt.get();
    controller.start(program, player, 1.0D, params, level.getGameTime(), "bing_ji_gu.iceburst");
    if (DEBUG) {
      LOGGER.info(
          "[compat/guzhenren][ice_skin] burst flow started: radius={} victims={} scale={}",
          String.format(java.util.Locale.ROOT, "%.2f", radius),
          victims,
          String.format(java.util.Locale.ROOT, "%.2f", scale));
    }
  }

  private static String formatDouble(double value) {
    return String.format(Locale.ROOT, "%.4f", value);
  }

  private static boolean isBeimingConstitution(Player player) {
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt =
        GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return false;
    }
    return handleOpt.get().hasConstitution("北冥冰魄体");
  }
}
