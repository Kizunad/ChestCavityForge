package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_shi.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.agent.Agents;
import net.tigereye.chestcavity.compat.common.organ.yu.YuLinGuOps;
import net.tigereye.chestcavity.compat.common.tuning.YuLinGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.entity.summon.OwnedSharkEntity;
import net.tigereye.chestcavity.compat.guzhenren.event.NoDropEvents;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_shi.calculator.YuShiSummonComboLogic;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_shi.fx.YuShiFx;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_shi.messages.YuShiMessages;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_shi.tuning.YuShiTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.GuzhenrenFlowTooltipResolver;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.skill.ComboSkillRegistry;
import net.tigereye.chestcavity.skill.ComboSkillRegistry.ComboSkillEntry;
import net.tigereye.chestcavity.skill.effects.SkillEffectBus;
import net.tigereye.chestcavity.util.NetworkUtil;

/** 饵祭召鲨（组合）运行时入口。 */
public final class YuShiRuntime {
  public static final ResourceLocation ABILITY_ID =
      id("guzhenren", "yu_shi_summon_combo");

  private static final double ZHENYUAN_COST = 900.0;
  private static final double JINGLI_COST = 10.0;
  private static final int HUNGER_COST = 1;
  private static final int TTL_TICKS = 20 * 120;
  private static final int OFFERING_COST = 64;

  private static final TagKey<Item> TIER1_TAG = tag("guzhenren", "shark_materials_tiered/tier1");
  private static final TagKey<Item> TIER2_TAG = tag("guzhenren", "shark_materials_tiered/tier2");
  private static final TagKey<Item> TIER3_TAG = tag("guzhenren", "shark_materials_tiered/tier3");
  private static final TagKey<Item> TIER4_TAG = tag("guzhenren", "shark_materials_tiered/tier4");
  private static final TagKey<Item> TIER5_TAG = tag("guzhenren", "shark_materials_tiered/tier5");

  private static final Map<ResourceLocation, Integer> FALLBACK_MATERIAL_TIERS =
      Map.ofEntries(
          Map.entry(id("guzhenren", "sha_yu_ya_chi"), 1),
          Map.entry(id("guzhenren", "sha_yu_yu_chi"), 1),
          Map.entry(id("guzhenren", "sha_yu_ya_chi_1"), 2),
          Map.entry(id("guzhenren", "sha_yu_yu_chi_1"), 2),
          Map.entry(id("guzhenren", "sha_yu_ya_chi_2"), 3),
          Map.entry(id("guzhenren", "sha_yu_yu_chi_2"), 3),
          Map.entry(id("guzhenren", "sha_yu_ya_chi_3"), 4),
          Map.entry(id("guzhenren", "sha_yu_yu_chi_3"), 4),
          Map.entry(id("guzhenren", "sha_yu_ya_chi_4"), 5),
          Map.entry(id("guzhenren", "sha_yu_yu_chi_4"), 5));

  private static final ResourceLocation[] TIER_ENTITY_IDS = {
    null,
    id("guzhenren", "bing_jiao_sha"),
    id("guzhenren", "bing_lin_sha"),
    id("guzhenren", "xuan_shuang_bing_sha"),
    id("guzhenren", "han_yuan_bing_sha"),
    id("guzhenren", "bing_po_long_wen_sha")
  };

  private static final ResourceLocation HEALTH_MODIFIER_ID = id("guzhenren", "combo/yu_shi_health");
  private static final ResourceLocation SPEED_MODIFIER_ID = id("guzhenren", "combo/yu_shi_speed");
  private static final int OFFHAND_SLOT = 40;

  private YuShiRuntime() {}

  public static void activate(ServerPlayer player, ChestCavityInstance cc) {
    if (player == null || cc == null) {
      return;
    }
    ServerLevel serverLevel = player.serverLevel();
    ComboSkillEntry entry = ComboSkillRegistry.get(ABILITY_ID).orElse(null);
    if (entry == null) {
      return;
    }

    ItemStack organ = YuLinGuOps.findOrgan(player);
    if (organ.isEmpty()) {
      YuShiMessages.missingOrgan(player);
      return;
    }

    OrganState state = OrganState.of(organ, YuLinGuOps.stateRoot());
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    MultiCooldown.Entry ready = cooldown.entry("YuShiSummonReadyAt").withDefault(0L);
    long now = serverLevel.getGameTime();
    if (!ready.isReady(now)) {
      return;
    }

    Inventory inventory = player.getInventory();
    Optional<OfferingSlot> offeringOpt = findBestOffering(inventory);
    if (offeringOpt.isEmpty()) {
      YuShiMessages.needOffering(player);
      return;
    }
    OfferingSlot offering = offeringOpt.get();
    ItemStack offeringStack = offering.stack();
    int materialTier = tierOf(offeringStack);
    if (materialTier <= 0) {
      YuShiMessages.invalidOffering(player);
      return;
    }

    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    if (ResourceOps.tryConsumeScaledZhenyuan(handle, ZHENYUAN_COST).isEmpty()) {
      YuShiMessages.insufficientZhenyuan(player);
      return;
    }
    if (ResourceOps.tryAdjustJingli(handle, -JINGLI_COST, true).isEmpty()) {
      YuShiMessages.insufficientJingli(player);
      return;
    }
    player.getFoodData().setFoodLevel(Math.max(0, player.getFoodData().getFoodLevel() - HUNGER_COST));
    consumeOffering(player, inventory, offering, OFFERING_COST);
    YuShiFx.playCast(player);

    int unlocked = YuLinGuOps.unlockedSharkTier(organ);
    if (materialTier > unlocked) {
      YuLinGuOps.unlockSharkTier(organ, materialTier);
      unlocked = materialTier;
    }
    int actualTier = Math.max(materialTier, unlocked);
    manageSummonLimit(player, actualTier, serverLevel);

    YuShiSummonComboLogic.FlowStats flowStats = evaluateFlowStats(player);
    double waterFlowExp =
        SkillEffectBus.consumeMetadata(player, ABILITY_ID, "yu_shi:liupai_shuidao", 0.0D);
    double changeFlowExp =
        SkillEffectBus.consumeMetadata(player, ABILITY_ID, "yu_shi:liupai_bianhuadao", 0.0D);
    double totalExp = Math.max(0.0D, waterFlowExp + changeFlowExp);

    YuShiSummonComboLogic.SummonModifiers baseModifiers =
        YuShiSummonComboLogic.computeModifiers(flowStats);

    double waterDaoHen =
        SkillEffectBus.consumeMetadata(player, ABILITY_ID, "yu_shi:daohen_shuidao", 0.0D);
    double changeDaoHen =
        SkillEffectBus.consumeMetadata(player, ABILITY_ID, "yu_shi:daohen_bianhuadao", 0.0D);
    double fireDaoHen =
        SkillEffectBus.consumeMetadata(player, ABILITY_ID, "yu_shi:daohen_yandao", 0.0D);

    double daoBonus =
        (Math.max(0.0D, waterDaoHen) + Math.max(0.0D, changeDaoHen)) / 1000.0D;
    double firePenalty = Math.max(0.0D, 1.0D - Math.max(0.0D, fireDaoHen) / 1000.0D);
    double healthMultiplier =
        baseModifiers.healthMultiplier() * Math.max(0.0D, 1.0D + daoBonus) * firePenalty;
    double speedMultiplier =
        baseModifiers.speedMultiplier() * Math.max(0.0D, 1.0D + daoBonus) * firePenalty;

    YuShiSummonComboLogic.SummonModifiers modifiers =
        new YuShiSummonComboLogic.SummonModifiers(
            healthMultiplier,
            speedMultiplier,
            baseModifiers.regenDurationTicks(),
            baseModifiers.regenAmplifier(),
            baseModifiers.resistanceDurationTicks(),
            baseModifiers.resistanceAmplifier(),
            baseModifiers.ttlBonusTicks());

    EntityType<?> type = resolveEntityType(actualTier);
    if (type == null) {
      return;
    }
    Vec3 spawnPos = player.position().add(player.getLookAngle().normalize().scale(2.0)).add(0, 0.2, 0);
    Entity spawned = type.create(serverLevel);
    if (!(spawned instanceof LivingEntity living)) {
      return;
    }
    living.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), player.getXRot());
    serverLevel.addFreshEntity(living);
    YuShiFx.playSummoned(serverLevel, living);
    living.addTag(NoDropEvents.TAG);
    living.getPersistentData().putBoolean(NoDropEvents.PDC, true);
    if (living instanceof Mob mob) {
      mob.setCanPickUpLoot(false);
    }

    applyModifiers(living, modifiers);

    OwnedSharkEntity tracked =
        new OwnedSharkEntity(
            living.getUUID(),
            player.getUUID(),
            actualTier,
            now,
            now + TTL_TICKS + modifiers.ttlBonusTicks());
    YuLinGuOps.addSummon(player, tracked);
    YuLinGuOps.recordWetContact(player, organ);

    int cooldownTicks = YuShiTuning.computeCooldownTicks(totalExp);
    long readyAt = now + cooldownTicks;
    ready.setReadyAt(readyAt);
    ComboSkillRegistry.scheduleReadyToast(player, ABILITY_ID, readyAt, now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
  }

  private static void applyModifiers(
      LivingEntity living, YuShiSummonComboLogic.SummonModifiers modifiers) {
    double healthMult = modifiers.healthMultiplier();
    if (Math.abs(healthMult - 1.0D) > 1.0E-4D) {
      Agents.applyTransientAttribute(
          living,
          Attributes.MAX_HEALTH,
          HEALTH_MODIFIER_ID,
          healthMult - 1.0D,
          AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      living.setHealth((float) living.getAttributeValue(Attributes.MAX_HEALTH));
    }
    double speedMult = modifiers.speedMultiplier();
    if (Math.abs(speedMult - 1.0D) > 1.0E-4D) {
      Agents.applyTransientAttribute(
          living,
          Attributes.MOVEMENT_SPEED,
          SPEED_MODIFIER_ID,
          speedMult - 1.0D,
          AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
    if (modifiers.regenDurationTicks() > 0) {
      living.addEffect(
          new MobEffectInstance(
              MobEffects.REGENERATION,
              modifiers.regenDurationTicks(),
              modifiers.regenAmplifier()));
    }
    if (modifiers.resistanceDurationTicks() > 0) {
      living.addEffect(
          new MobEffectInstance(
              MobEffects.DAMAGE_RESISTANCE,
              modifiers.resistanceDurationTicks(),
              modifiers.resistanceAmplifier()));
    }
  }

  private static void manageSummonLimit(ServerPlayer owner, int incomingTier, ServerLevel level) {
    List<OwnedSharkEntity> existing = new ArrayList<>(YuLinGuOps.getSummons(owner));
    if (existing.size() < YuLinGuTuning.MAX_SUMMONS) {
      return;
    }
    existing.sort(
        Comparator.comparingInt(OwnedSharkEntity::tier).thenComparingLong(OwnedSharkEntity::createdAt));
    OwnedSharkEntity victim = null;
    for (OwnedSharkEntity candidate : existing) {
      if (candidate.tier() < incomingTier) {
        victim = candidate;
        break;
      }
    }
    if (victim == null) {
      victim = existing.get(0);
    }
    if (victim != null) {
      victim.discard(level);
      YuLinGuOps.removeSummon(owner, victim);
    }
  }

  private static YuShiSummonComboLogic.FlowStats evaluateFlowStats(ServerPlayer player) {
    List<GuzhenrenFlowTooltipResolver.FlowInfo> infos = Agents.collectInventoryFlows(player);
    if (infos.isEmpty()) {
      return new YuShiSummonComboLogic.FlowStats(0, 0);
    }
    List<List<String>> flows = new ArrayList<>();
    for (GuzhenrenFlowTooltipResolver.FlowInfo info : infos) {
      if (info != null && info.hasFlow()) {
        flows.add(info.flows());
      }
    }
    return flows.isEmpty()
        ? new YuShiSummonComboLogic.FlowStats(0, 0)
        : YuShiSummonComboLogic.computeFlowStats(flows);
  }

  private static Optional<OfferingSlot> findBestOffering(Inventory inventory) {
    OfferingSlot best = null;
    int selectedSlot = inventory.selected;
    int size = inventory.getContainerSize();
    int bestTier = 0;
    for (int slot = 0; slot < size; slot++) {
      ItemStack candidate = inventory.getItem(slot);
      if (candidate.isEmpty() || candidate.getCount() < OFFERING_COST) continue;
      int tier = tierOf(candidate);
      if (tier <= 0) continue;
      InteractionHand hand = resolveHand(slot, selectedSlot);
      if (tier > bestTier) {
        bestTier = tier;
        best = new OfferingSlot(slot, candidate.copy(), hand);
      }
    }
    return Optional.ofNullable(best);
  }

  private static InteractionHand resolveHand(int slot, int selectedSlot) {
    if (slot == selectedSlot) return InteractionHand.MAIN_HAND;
    if (slot == OFFHAND_SLOT) return InteractionHand.OFF_HAND;
    return null;
  }

  private static void consumeOffering(
      ServerPlayer player, Inventory inventory, OfferingSlot offering, int amount) {
    int slot = offering.slot();
    inventory.removeItem(slot, amount);
    InteractionHand hand = offering.hand();
    if (hand != null) {
      player.setItemInHand(hand, inventory.getItem(slot));
    }
    player.containerMenu.broadcastChanges();
  }

  private static int tierOf(ItemStack stack) {
    if (stack.is(TIER5_TAG)) return 5;
    if (stack.is(TIER4_TAG)) return 4;
    if (stack.is(TIER3_TAG)) return 3;
    if (stack.is(TIER2_TAG)) return 2;
    if (stack.is(TIER1_TAG)) return 1;
    Integer fallback = FALLBACK_MATERIAL_TIERS.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    return fallback == null ? 0 : fallback.intValue();
  }

  private static EntityType<?> resolveEntityType(int tier) {
    if (tier <= 0 || tier >= TIER_ENTITY_IDS.length) {
      return null;
    }
    ResourceLocation id = TIER_ENTITY_IDS[tier];
    return BuiltInRegistries.ENTITY_TYPE.get(id);
  }

  private record OfferingSlot(int slot, ItemStack stack, InteractionHand hand) {}

  private static ResourceLocation id(String namespace, String path) {
    return ResourceLocation.fromNamespaceAndPath(namespace, path);
  }

  private static TagKey<Item> tag(String namespace, String path) {
    return TagKey.create(BuiltInRegistries.ITEM.key(), id(namespace, path));
  }
}
