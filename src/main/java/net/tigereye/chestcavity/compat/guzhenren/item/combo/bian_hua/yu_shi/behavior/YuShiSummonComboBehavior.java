package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_shi.behavior;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.entity.summon.OwnedSharkEntity;
import net.tigereye.chestcavity.compat.guzhenren.event.NoDropEvents;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.YuLinGuBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.GuzhenrenFlowTooltipResolver;
import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yu_shi.calculator.YuShiSummonComboLogic;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.NetworkUtil;

/** 组合杀招：饵祭召鲨（组合） — 按材料阶数召唤协战鲨鱼，遵循数量上限与无掉落规则。 */
public final class YuShiSummonComboBehavior {
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "yu_shi_summon_combo");

  // 成本与时序（承接器官主动的数值）
  private static final int COOLDOWN_TICKS = 20 * 15;
  private static final double ZHENYUAN_COST = 900.0;
  private static final double JINGLI_COST = 10.0;
  private static final int HUNGER_COST = 1;
  private static final int TTL_TICKS = 20 * 120;
  private static final int OFFERING_COST = 64;

  // 材料标签（优先按 tier1~5 匹配，失败再兜底硬编码表）
  private static final TagKey<Item> TIER1_TAG =
      TagKey.create(
          BuiltInRegistries.ITEM.key(),
          ResourceLocation.fromNamespaceAndPath("guzhenren", "shark_materials_tiered/tier1"));
  private static final TagKey<Item> TIER2_TAG =
      TagKey.create(
          BuiltInRegistries.ITEM.key(),
          ResourceLocation.fromNamespaceAndPath("guzhenren", "shark_materials_tiered/tier2"));
  private static final TagKey<Item> TIER3_TAG =
      TagKey.create(
          BuiltInRegistries.ITEM.key(),
          ResourceLocation.fromNamespaceAndPath("guzhenren", "shark_materials_tiered/tier3"));
  private static final TagKey<Item> TIER4_TAG =
      TagKey.create(
          BuiltInRegistries.ITEM.key(),
          ResourceLocation.fromNamespaceAndPath("guzhenren", "shark_materials_tiered/tier4"));
  private static final TagKey<Item> TIER5_TAG =
      TagKey.create(
          BuiltInRegistries.ITEM.key(),
          ResourceLocation.fromNamespaceAndPath("guzhenren", "shark_materials_tiered/tier5"));

  private static final Map<ResourceLocation, Integer> FALLBACK_MATERIAL_TIERS =
      Map.ofEntries(
          Map.entry(ResourceLocation.fromNamespaceAndPath("guzhenren", "sha_yu_ya_chi"), 1),
          Map.entry(ResourceLocation.fromNamespaceAndPath("guzhenren", "sha_yu_yu_chi"), 1),
          Map.entry(ResourceLocation.fromNamespaceAndPath("guzhenren", "sha_yu_ya_chi_1"), 2),
          Map.entry(ResourceLocation.fromNamespaceAndPath("guzhenren", "sha_yu_yu_chi_1"), 2),
          Map.entry(ResourceLocation.fromNamespaceAndPath("guzhenren", "sha_yu_ya_chi_2"), 3),
          Map.entry(ResourceLocation.fromNamespaceAndPath("guzhenren", "sha_yu_yu_chi_2"), 3),
          Map.entry(ResourceLocation.fromNamespaceAndPath("guzhenren", "sha_yu_ya_chi_3"), 4),
          Map.entry(ResourceLocation.fromNamespaceAndPath("guzhenren", "sha_yu_yu_chi_3"), 4),
          Map.entry(ResourceLocation.fromNamespaceAndPath("guzhenren", "sha_yu_ya_chi_4"), 5),
          Map.entry(ResourceLocation.fromNamespaceAndPath("guzhenren", "sha_yu_yu_chi_4"), 5));

  // 实体映射（按阶）
  private static final ResourceLocation[] TIER_ENTITY_IDS = {
    null,
    ResourceLocation.fromNamespaceAndPath("guzhenren", "bing_jiao_sha"),
    ResourceLocation.fromNamespaceAndPath("guzhenren", "bing_lin_sha"),
    ResourceLocation.fromNamespaceAndPath("guzhenren", "xuan_shuang_bing_sha"),
    ResourceLocation.fromNamespaceAndPath("guzhenren", "han_yuan_bing_sha"),
    ResourceLocation.fromNamespaceAndPath("guzhenren", "bing_po_long_wen_sha")
  };

  private static final int OFFHAND_SLOT = 40; // Vanilla 副手槽位

  private static final ResourceLocation HEALTH_MODIFIER_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "combo/yu_shi_health");
  private static final ResourceLocation SPEED_MODIFIER_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "combo/yu_shi_speed");

  static {
    OrganActivationListeners.register(ABILITY_ID, YuShiSummonComboBehavior::activate);
  }

  private YuShiSummonComboBehavior() {}

  private static void activate(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || cc == null) return;
    if (!(player.level() instanceof ServerLevel serverLevel)) return;

    // 查找鱼鳞蛊作为冷却与状态锚点
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      player.displayClientMessage(
          Component.literal("缺少鱼鳞蛊，无法施展饵祭召鲨（组合）"), true);
      return;
    }

    OrganState state = OrganState.of(organ, "YuLinGu");
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    MultiCooldown.Entry ready = cooldown.entry("YuShiSummonReadyAt").withDefault(0L);
    long now = serverLevel.getGameTime();
    if (!ready.isReady(now)) return;

    // 选取供品（≥64 且阶数最高）
    Inventory inventory = player.getInventory();
    Optional<OfferingSlot> offeringOpt = findBestOffering(inventory);
    if (offeringOpt.isEmpty()) {
      player.displayClientMessage(
          Component.literal("需要整组鲨材作为供品（牙齿/鱼鳍 ×64）"), true);
      return;
    }
    OfferingSlot offering = offeringOpt.get();
    ItemStack offeringStack = offering.stack();
    int materialTier = tierOf(offeringStack);
    if (materialTier <= 0) {
      player.displayClientMessage(Component.literal("供品无效，召鲨仪式失败"), true);
      return;
    }

    // 扣资源
    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) return;
    ResourceHandle handle = handleOpt.get();
    if (ResourceOps.tryConsumeScaledZhenyuan(handle, ZHENYUAN_COST).isEmpty()) {
      player.displayClientMessage(Component.literal("真元不足，召鲨仪式失败。"), true);
      return;
    }
    if (ResourceOps.tryAdjustJingli(handle, -JINGLI_COST, true).isEmpty()) {
      player.displayClientMessage(Component.literal("精力不足，召鲨仪式失败。"), true);
      return;
    }
    player.getFoodData().setFoodLevel(Math.max(0, player.getFoodData().getFoodLevel() - HUNGER_COST));
    consumeOffering(player, inventory, offering, OFFERING_COST);

    // 阶梯解锁与数量上限
    YuLinGuBehavior behavior = YuLinGuBehavior.INSTANCE;
    int unlocked = behavior.unlockedSharkTier(organ);
    if (materialTier > unlocked) {
      behavior.unlockSharkTier(organ, materialTier);
      unlocked = materialTier;
    }
    int actualTier = Math.max(materialTier, unlocked);
    manageSummonLimit(behavior, player, actualTier, serverLevel);

    YuShiSummonComboLogic.FlowStats flowStats = evaluateFlowStats(cc, serverLevel);
    YuShiSummonComboLogic.SummonModifiers modifiers =
        YuShiSummonComboLogic.computeModifiers(flowStats);

    // 生成实体并登记追踪
    EntityType<?> type = resolveEntityType(actualTier);
    if (type == null) return;
    Vec3 spawnPos = player.position().add(player.getLookAngle().normalize().scale(2.0)).add(0, 0.2, 0);
    Entity spawned = type.create(serverLevel);
    if (!(spawned instanceof LivingEntity living)) return;
    living.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), player.getXRot());
    serverLevel.addFreshEntity(living);
    living.addTag(NoDropEvents.TAG);
    living.getPersistentData().putBoolean(NoDropEvents.PDC, true);
    if (living instanceof Mob mob) mob.setCanPickUpLoot(false);

    applyModifiers(living, modifiers);

    OwnedSharkEntity tracked =
        new OwnedSharkEntity(
            living.getUUID(),
            player.getUUID(),
            actualTier,
            now,
            now + TTL_TICKS + modifiers.ttlBonusTicks());
    behavior.addSummon(player, tracked);
    behavior.recordWetContact(player, organ);

    long readyAt = now + COOLDOWN_TICKS;
    ready.setReadyAt(readyAt);
    ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, readyAt, now);
    NetworkUtil.sendOrganSlotUpdate(cc, organ);
    serverLevel.playSound(
        null, player.blockPosition(), SoundEvents.DOLPHIN_SPLASH, SoundSource.PLAYERS, 1.0f, 0.9f);
  }

  private static void manageSummonLimit(
      YuLinGuBehavior behavior, ServerPlayer owner, int incomingTier, ServerLevel level) {
    List<OwnedSharkEntity> existing = new ArrayList<>(behavior.getSummons(owner));
    if (existing.size() < 5) return;
    existing.sort(
        Comparator.comparingInt(OwnedSharkEntity::tier).thenComparingLong(OwnedSharkEntity::createdAt));
    OwnedSharkEntity victim = null;
    for (OwnedSharkEntity candidate : existing) {
      if (candidate.tier() < incomingTier) {
        victim = candidate;
        break;
      }
    }
    if (victim == null) victim = existing.get(0);
    if (victim != null) {
      victim.discard(level);
      behavior.removeSummon(owner, victim);
    }
  }

  private static EntityType<?> resolveEntityType(int tier) {
    if (tier <= 0 || tier >= TIER_ENTITY_IDS.length) return null;
    ResourceLocation id = TIER_ENTITY_IDS[tier];
    return BuiltInRegistries.ENTITY_TYPE.get(id);
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

  private static YuShiSummonComboLogic.FlowStats evaluateFlowStats(
      ChestCavityInstance cc, ServerLevel level) {
    if (cc == null || cc.inventory == null) {
      return new YuShiSummonComboLogic.FlowStats(0, 0);
    }
    TooltipContext context = TooltipContext.of(level);
    TooltipFlag flag = TooltipFlag.NORMAL;
    List<List<String>> flows = new ArrayList<>();
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }
      var info = GuzhenrenFlowTooltipResolver.inspect(stack, context, flag, null);
      if (info.hasFlow()) {
        flows.add(info.flows());
      }
    }
    return YuShiSummonComboLogic.computeFlowStats(flows);
  }

  private static void applyModifiers(
      LivingEntity living, YuShiSummonComboLogic.SummonModifiers modifiers) {
    AttributeInstance maxHealth = living.getAttribute(Attributes.MAX_HEALTH);
    if (maxHealth != null) {
      AttributeModifier existing = maxHealth.getModifier(HEALTH_MODIFIER_ID);
      if (existing != null) {
        maxHealth.removeModifier(existing);
      }
      double mult = modifiers.healthMultiplier();
      if (Math.abs(mult - 1.0) > 1e-4) {
        maxHealth.addTransientModifier(
            new AttributeModifier(
                HEALTH_MODIFIER_ID,
                mult - 1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        living.setHealth((float) maxHealth.getValue());
      }
    }

    AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
    if (speed != null) {
      AttributeModifier existing = speed.getModifier(SPEED_MODIFIER_ID);
      if (existing != null) {
        speed.removeModifier(existing);
      }
      double mult = modifiers.speedMultiplier();
      if (Math.abs(mult - 1.0) > 1e-4) {
        speed.addTransientModifier(
            new AttributeModifier(
                SPEED_MODIFIER_ID,
                mult - 1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
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

  private static ItemStack findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) return ItemStack.EMPTY;
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack s = cc.inventory.getItem(i);
      if (s.isEmpty()) continue;
      var id = BuiltInRegistries.ITEM.getKey(s.getItem());
      if (id.getNamespace().equals("guzhenren") && id.getPath().equals("yu_lin_gu")) return s;
    }
    return ItemStack.EMPTY;
  }

  private record OfferingSlot(int slot, ItemStack stack, InteractionHand hand) {}
}
