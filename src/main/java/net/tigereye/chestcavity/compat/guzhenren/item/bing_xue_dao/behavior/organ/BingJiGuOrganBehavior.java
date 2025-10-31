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
import net.tigereye.chestcavity.skill.effects.SkillEffectBus;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.chat.Component;
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
// removed deprecated linkage imports
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.registration.CCDamageSources;
import net.tigereye.chestcavity.registration.CCItems;
import net.tigereye.chestcavity.util.AbsorptionHelper;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.tuning.BingJiTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.state.BingJiStateKeys;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator.AoEFalloff;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.fx.BingJiFx;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.messages.BingXueMessages;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import org.slf4j.Logger;

/** Behavior implementation for 冰肌蛊 (Bing Ji Gu). */
public final class BingJiGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener,
        OrganOnHitListener,
        OrganRemovalListener,
        OrganIncomingDamageListener {

  public static final BingJiGuOrganBehavior INSTANCE = new BingJiGuOrganBehavior();

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "bing_ji_gu");
  private static final ResourceLocation JADE_BONE_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yu_gu_gu");
  private static final ResourceLocation ICE_BURST_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "bing_bao_gu");
  private static final ResourceLocation BLEED_EFFECT_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "lliuxue");
  private static final ResourceLocation ICE_COLD_EFFECT_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "hhanleng");

  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "bing_ji_gu_iceburst");
  private static final ResourceLocation ICE_BURST_FLOW_ID =
      BingJiTuning.ICE_BURST_FLOW_ID;
  private static final ResourceLocation ABSORPTION_MODIFIER_ID =
      BingJiTuning.ABSORPTION_MODIFIER_ID;

  private static final String STATE_ROOT = "BingJiGu";
  private static final boolean DEBUG = false;

  private static CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig cfg() {
    return BingJiTuning.cfg();
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

    boolean paid =
        ResourceOps.tryConsumeScaledZhenyuan(handle, config.zhenyuanBaseCost * stackCount)
            .isPresent();
    if (DEBUG) {
      LOGGER.info(
          "[compat/guzhenren][ice_skin] slow tick: stacks={}, paidZhenyuan={} (cost={})",
          stackCount,
          paid,
          String.format(java.util.Locale.ROOT, "%.1f", config.zhenyuanBaseCost * stackCount));
    }
    MultiCooldown cooldown = createCooldown(cc, organ);
    MultiCooldown.Entry absorptionReady = BingJiStateKeys.absorptionEntry(cooldown);

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
      if (absorptionReady.isReady(player.level().getGameTime())) {
        tickAbsorption(player, cc, organ, stackCount, config);
        absorptionReady.setReadyAt(
            player.level().getGameTime() + (long) config.slowTickIntervalsPerMinute * 80);
      }
      if (hasJadeBone(cc)) {
        net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.runtime.ColdEffectOps.clearBleed(player, BLEED_EFFECT_ID);
        if (DEBUG) {
          LOGGER.info("[compat/guzhenren][ice_skin] jade bone present: cleared bleed if any");
        }
      }
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

    float bonus = (float) (damage * config.bonusDamageFraction);
    if (bonus > 0.0f) {
      applyColdEffect(target, config);
      if (DEBUG) {
        LOGGER.info(
            "[compat/guzhenren][ice_skin] onHit bonus: baseDamage={} bonus={}",
            String.format(java.util.Locale.ROOT, "%.2f", damage),
            String.format(java.util.Locale.ROOT, "%.2f", bonus));
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
      cooldown.entryInt(BingJiStateKeys.ABSORPTION_TIMER_KEY).clear();
      BingJiStateKeys.invulnEntry(cooldown).setReadyAt(0L);
    }
  }


  private static void tickAbsorption(
      Player player,
      ChestCavityInstance cc,
      ItemStack organ,
      int stacks,
      CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig config) {
    if (player == null || player.level().isClientSide()) {
      return;
    }
    try {
      int sc = Math.max(1, organ == null ? stacks : organ.getCount());
      float gain = (float) (config.absorptionPerTrigger * Math.max(1, sc));
      float before = player.getAbsorptionAmount();
      double cap = config.absorptionCap;
      double target = Math.min(cap, before + gain);
      AbsorptionHelper.applyAbsorption(player, (float) target, ABSORPTION_MODIFIER_ID, false);
      if (DEBUG) {
        LOGGER.info(
            "[compat/guzhenren][ice_skin] absorption tick: +{} (stacks={}) {} -> {}",
            String.format(java.util.Locale.ROOT, "%.1f", gain),
            sc,
            String.format(java.util.Locale.ROOT, "%.1f", before),
            String.format(java.util.Locale.ROOT, "%.1f", player.getAbsorptionAmount()));
      }
    } catch (Throwable ignored) {
    }
  }

  @Override
  public float onIncomingDamage(
      DamageSource source,
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    if (!(victim instanceof Player player)
        || victim.level().isClientSide()
        || cc == null
        || damage <= 0) {
      return damage;
    }
    if (!matchesOrgan(organ, ORGAN_ID) && !organ.is(CCItems.GUZHENREN_BING_JI_GU)) {
      return damage;
    }

    CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig config = cfg();
    MultiCooldown cooldown = createCooldown(cc, organ);
    MultiCooldown.Entry invulnReady = net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.state.BingJiStateKeys.invulnEntry(cooldown);
    long now = player.level().getGameTime();

    if (invulnReady.isReady(now)) {
      if (hasJadeBone(cc)
          && isBeimingConstitution(player)
          && (player.getHealth() - damage) <= player.getMaxHealth() * config.lowHealthThreshold) {

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

        if (player.level() instanceof ServerLevel serverLevel) {
          serverLevel.sendParticles(
              ParticleTypes.SNOWFLAKE,
              player.getX(),
              player.getY() + player.getBbHeight() / 2,
              player.getZ(),
              50,
              player.getBbWidth() / 2,
              player.getBbHeight() / 4,
              player.getBbWidth() / 2,
              0.1);
          serverLevel.playSound(
              null,
              player.blockPosition(),
              SoundEvents.GLASS_BREAK,
              SoundSource.PLAYERS,
              1.0F,
              0.5F);
        }

        if (DEBUG) {
          LOGGER.info(
              "[compat/guzhenren][ice_skin] invulnerability granted by onIncomingDamage: duration={}t, cooldown={}t",
              config.invulnerabilityDurationTicks,
              config.invulnerabilityCooldownTicks);
        }
        // Negate the triggering damage
        return 0;
      }
    }

    return damage;
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

  public static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    if (entity == null || cc == null || entity.level().isClientSide() || !(entity instanceof ServerPlayer)) {
        return;
    }
    ServerPlayer player = (ServerPlayer) entity;
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
        net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.messages.FailNotifier.notifyThrottled(player, Component.translatable(
            net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.messages.BingXueMessages.FAIL_NEED_BING_JI_GU));
        if (DEBUG) {
            LOGGER.info("[compat/guzhenren][ice_skin] activate rejected: organ missing");
        }
        return;
    }
    if (!hasJadeBone(cc)) {
        net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.messages.FailNotifier.notifyThrottled(player, Component.translatable(
            net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.messages.BingXueMessages.FAIL_NEED_JADE_BONE));
        if (DEBUG) {
            LOGGER.info("[compat/guzhenren][ice_skin] activate rejected: missing jade bone");
        }
        return;
    }
    if (!consumeMuscle(cc)) {
        net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.messages.FailNotifier.notifyThrottled(player, Component.translatable(
            net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.messages.BingXueMessages.FAIL_NO_MUSCLE));
        if (DEBUG) {
            LOGGER.info("[compat/guzhenren][ice_skin] activate rejected: no consumable muscle found");
        }
        return;
    }
    Level level = entity.level();
    if (!(level instanceof ServerLevel server)) {
        return;
    }

    // 快照+计算参数
    double daohen = SkillEffectBus.consumeMetadata(player, ABILITY_ID, ComputedBingXueDaohenEffect.DAO_HEN_KEY, 0.0);
    CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig config = cfg();
    int stacks = Math.max(1, organ.getCount());
    var burst = net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator.BurstParamOps.compute(config, stacks, daohen, hasBingBao(cc));

    if (DEBUG) {
        LOGGER.info(
            "[compat/guzhenren][ice_skin] activating burst: stacks={}, daohen={}, damageBase={}, radius={}",
            stacks,
            String.format(java.util.Locale.ROOT, "%.3f", daohen),
            String.format(java.util.Locale.ROOT, "%.2f", burst.baseDamage()),
            String.format(java.util.Locale.ROOT, "%.2f", burst.radius()));
    }

    // 执行冰爆
    net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.runtime.IceBurstRuntime.execute(
        player, cc, config, ICE_COLD_EFFECT_ID, ICE_BURST_FLOW_ID, burst);

    // Deduct 1% of player's max health as activation cost
    float max = player.getMaxHealth();
    float cost = (float) (max * 0.01f);
    if (cost > 0.0f) {
        float before = player.getHealth();
        DamageSource source = CCDamageSources.organCost(player);
        player.hurt(source, cost);
        if (DEBUG) {
            LOGGER.info(
                "[compat/guzhenren][ice_skin] health cost applied: {}% ({} -> {})"
                    .formatted(
                        1,
                        String.format(java.util.Locale.ROOT, "%.2f", before),
                        String.format(java.util.Locale.ROOT, "%.2f", player.getHealth())));
        }
    }
  }

  // 失败提示与流/目标选择已下沉到 messages/ 与 runtime/

  private static boolean isBeimingConstitution(Player player) {
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt =
        GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return false;
    }
    return handleOpt.get().hasConstitution("北冥冰魄体");
  }
}
