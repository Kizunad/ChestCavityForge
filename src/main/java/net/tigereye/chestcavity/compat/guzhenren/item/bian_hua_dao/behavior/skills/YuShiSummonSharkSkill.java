package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.skills;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.entity.summon.OwnedSharkEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.YuLinGuBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.NetworkUtil;

/** [主动C] 饵祭召鲨：消耗对应蛊材召唤鲨鱼同伴，最多同时存在五条。 */
public final class YuShiSummonSharkSkill {

  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "yu_shi_summon");

  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "yu_lin_gu");

  private static final String COOLDOWN_KEY = "YuShiSummonReadyAt";
  private static final int COOLDOWN_TICKS = 20 * 15;
  private static final double ZHENYUAN_COST = 900.0;
  private static final double JINGLI_COST = 10.0;
  private static final int HUNGER_COST = 1;
  private static final int TTL_TICKS = 20 * 120;

  private static final TagKey<Item> MATERIALS_TAG =
      TagKey.create(
          BuiltInRegistries.ITEM.key(),
          ResourceLocation.fromNamespaceAndPath("guzhenren", "shark_materials"));
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

  private static final ResourceLocation[] TIER_ENTITY_IDS = {
    null,
    ResourceLocation.fromNamespaceAndPath("guzhenren", "bing_jiao_sha"),
    ResourceLocation.fromNamespaceAndPath("guzhenren", "bing_lin_sha"),
    ResourceLocation.fromNamespaceAndPath("guzhenren", "han_yuan_bing_sha"),
    ResourceLocation.fromNamespaceAndPath("guzhenren", "han_yuan_bing_sha"),
    ResourceLocation.fromNamespaceAndPath("guzhenren", "bing_po_long_wen_sha")
  };

  // 玩家背包结构中副手对应的槽位索引（Vanilla 固定为 40）。
  private static final int OFFHAND_SLOT = 40;

  private YuShiSummonSharkSkill() {}

  public static void bootstrap() {
    OrganActivationListeners.register(ABILITY_ID, YuShiSummonSharkSkill::activate);
  }

  private static void activate(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || cc == null) {
      return;
    }
    if (!(player.level() instanceof ServerLevel serverLevel)) {
      return;
    }
    ItemStack organ = findOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }

    OrganState state = OrganState.of(organ, "YuLinGu");
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    MultiCooldown.Entry ready = cooldown.entry(COOLDOWN_KEY).withDefault(0L);
    long now = serverLevel.getGameTime();
    if (!ready.isReady(now)) {
      return;
    }

    Inventory inventory = player.getInventory();
    Optional<OfferingSlot> offeringOpt = findBestOffering(inventory);
    if (offeringOpt.isEmpty()) {
      sendFailure(player, "你大口吞下了空气。鱼鳞蛊沉默以对。");
      return;
    }
    OfferingSlot offering = offeringOpt.get();
    ItemStack offeringStack = offering.stack();
    int materialTier = tierOf(offeringStack);

    Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    ResourceHandle handle = handleOpt.get();
    if (ResourceOps.tryConsumeScaledZhenyuan(handle, ZHENYUAN_COST).isEmpty()) {
      sendFailure(player, "真元不足，召鲨仪式失败。");
      return;
    }
    if (ResourceOps.tryAdjustJingli(handle, -JINGLI_COST, true).isEmpty()) {
      sendFailure(player, "精力不足，召鲨仪式失败。");
      return;
    }
    player
        .getFoodData()
        .setFoodLevel(Math.max(0, player.getFoodData().getFoodLevel() - HUNGER_COST));

    consumeOffering(player, inventory, offering);

    YuLinGuBehavior behavior = YuLinGuBehavior.INSTANCE;
    int unlocked = behavior.unlockedSharkTier(organ);
    if (materialTier > unlocked) {
      behavior.unlockSharkTier(organ, materialTier);
      unlocked = materialTier;
    }
    int actualTier = Math.max(materialTier, unlocked);

    manageSummonLimit(behavior, player, actualTier, serverLevel);

    EntityType<?> type = resolveEntityType(actualTier);
    if (type == null) {
      return;
    }
    Vec3 spawnPos =
        player.position().add(player.getLookAngle().normalize().scale(2.0)).add(0, 0.2, 0);
    Entity spawned = type.create(serverLevel);
    if (!(spawned instanceof LivingEntity living)) {
      return;
    }
    living.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), player.getXRot());
    serverLevel.addFreshEntity(living);

    OwnedSharkEntity tracked =
        new OwnedSharkEntity(living.getUUID(), player.getUUID(), actualTier, now, now + TTL_TICKS);
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
    if (existing.size() < 5) {
      return;
    }
    existing.sort(
        Comparator.comparingInt(OwnedSharkEntity::tier)
            .thenComparingLong(OwnedSharkEntity::createdAt));
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
      behavior.removeSummon(owner, victim);
    }
  }

  private static EntityType<?> resolveEntityType(int tier) {
    if (tier <= 0 || tier >= TIER_ENTITY_IDS.length) {
      return null;
    }
    ResourceLocation id = TIER_ENTITY_IDS[tier];
    return BuiltInRegistries.ENTITY_TYPE.get(id);
  }

  private static int tierOf(ItemStack stack) {
    if (stack.is(TIER5_TAG)) {
      return 5;
    }
    if (stack.is(TIER4_TAG)) {
      return 4;
    }
    if (stack.is(TIER3_TAG)) {
      return 3;
    }
    if (stack.is(TIER2_TAG)) {
      return 2;
    }
    if (stack.is(TIER1_TAG)) {
      return 1;
    }
    return 0;
  }

  private static Optional<OfferingSlot> findBestOffering(Inventory inventory) {
    // 按要求遍历玩家完整背包，挑选阶次最高且数量可用的鲨材供品。
    OfferingSlot best = null;
    int selectedSlot = inventory.selected;
    int size = inventory.getContainerSize();
    int bestTier = 0;
    for (int slot = 0; slot < size; slot++) {
      ItemStack candidate = inventory.getItem(slot);
      if (candidate.isEmpty() || !candidate.is(MATERIALS_TAG) || candidate.getCount() <= 0) {
        continue;
      }
      int tier = tierOf(candidate);
      if (tier <= 0) {
        continue;
      }
      InteractionHand hand = resolveHand(slot, selectedSlot);
      if (tier > bestTier) {
        bestTier = tier;
        best = new OfferingSlot(slot, candidate.copy(), hand);
      }
    }
    return Optional.ofNullable(best);
  }

  // 将槽位索引映射到可能的手持位置，以便在消耗后执行客户端同步。
  private static InteractionHand resolveHand(int slot, int selectedSlot) {
    if (slot == selectedSlot) {
      return InteractionHand.MAIN_HAND;
    }
    if (slot == OFFHAND_SLOT) {
      return InteractionHand.OFF_HAND;
    }
    return null;
  }

  private static void consumeOffering(
      ServerPlayer player, Inventory inventory, OfferingSlot offering) {
    // 通过 inventory.removeItem 精准扣除供品，并在需要时同步手持物品状态给客户端。
    inventory.removeItem(offering.slot(), 1);
    InteractionHand hand = offering.hand();
    if (hand != null) {
      player.setItemInHand(hand, inventory.getItem(offering.slot()));
    }
  }

  private static ItemStack findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size; i++) {
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

  private static void sendFailure(ServerPlayer player, String message) {
    player.displayClientMessage(net.minecraft.network.chat.Component.literal(message), true);
  }

  private record OfferingSlot(int slot, ItemStack stack, InteractionHand hand) {}
}
