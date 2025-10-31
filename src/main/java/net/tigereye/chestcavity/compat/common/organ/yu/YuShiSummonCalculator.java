package net.tigereye.chestcavity.compat.common.organ.yu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.common.tuning.YuLinGuTuning;
import net.tigereye.chestcavity.compat.common.tuning.YuShiSummonTuning;
import net.tigereye.chestcavity.compat.guzhenren.entity.summon.OwnedSharkEntity;
import net.tigereye.chestcavity.compat.guzhenren.event.NoDropEvents;
import net.tigereye.chestcavity.compat.common.organ.yu.YuLinGuOps;

public final class YuShiSummonCalculator {
  private YuShiSummonCalculator() {}

  public static record OfferingSlot(int slot, ItemStack stack, InteractionHand hand) {}

  public static Optional<OfferingSlot> findBestOffering(Inventory inventory) {
    OfferingSlot best = null;
    int selectedSlot = inventory.selected;
    int size = inventory.getContainerSize();
    int bestTier = 0;
    for (int slot = 0; slot < size; slot++) {
      ItemStack candidate = inventory.getItem(slot);
      if (candidate.isEmpty() || candidate.getCount() < YuShiSummonTuning.OFFERING_COST) continue;
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

  public static InteractionHand resolveHand(int slot, int selectedSlot) {
    if (slot == selectedSlot) return InteractionHand.MAIN_HAND;
    if (slot == YuShiSummonTuning.OFFHAND_SLOT) return InteractionHand.OFF_HAND;
    return null;
  }

  public static int tierOf(ItemStack stack) {
    if (stack.is(YuShiSummonTuning.TIER5_TAG)) return 5;
    if (stack.is(YuShiSummonTuning.TIER4_TAG)) return 4;
    if (stack.is(YuShiSummonTuning.TIER3_TAG)) return 3;
    if (stack.is(YuShiSummonTuning.TIER2_TAG)) return 2;
    if (stack.is(YuShiSummonTuning.TIER1_TAG)) return 1;
    ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
    Integer fallback = YuShiSummonTuning.FALLBACK_MATERIAL_TIERS.get(id);
    return fallback == null ? 0 : fallback.intValue();
  }

  public static void consumeOffering(ServerPlayer player, Inventory inventory, OfferingSlot offering, int amount) {
    int slot = offering.slot();
    inventory.removeItem(slot, amount);
    InteractionHand hand = offering.hand();
    if (hand != null) {
      player.setItemInHand(hand, inventory.getItem(slot));
    }
    player.containerMenu.broadcastChanges();
  }

  public static void manageSummonLimit(ServerPlayer owner, int incomingTier, ServerLevel level) {
    List<OwnedSharkEntity> existing = new ArrayList<>(YuLinGuOps.getSummons(owner));
    if (existing.size() < YuLinGuTuning.MAX_SUMMONS) return;
    existing.sort(Comparator.comparingInt(OwnedSharkEntity::tier).thenComparingLong(OwnedSharkEntity::createdAt));
    OwnedSharkEntity victim = null;
    for (OwnedSharkEntity candidate : existing) {
      if (candidate.tier() < incomingTier) { victim = candidate; break; }
    }
    if (victim == null) victim = existing.get(0);
    if (victim != null) {
      victim.discard(level);
      YuLinGuOps.removeSummon(owner, victim);
    }
  }

  public static EntityType<?> resolveEntityType(int tier) {
    if (tier <= 0 || tier >= YuShiSummonTuning.TIER_ENTITY_IDS.length) return null;
    ResourceLocation id = YuShiSummonTuning.TIER_ENTITY_IDS[tier];
    return BuiltInRegistries.ENTITY_TYPE.get(id);
  }

  public static void finalizeSpawned(ServerLevel level, ServerPlayer player, LivingEntity living) {
    living.addTag(NoDropEvents.TAG);
    living.getPersistentData().putBoolean(NoDropEvents.PDC, true);
    if (living instanceof Mob mob) mob.setCanPickUpLoot(false);
  }

  public static Vec3 computeSpawnPos(ServerPlayer player) {
    return player.position().add(player.getLookAngle().normalize().scale(2.0)).add(0, 0.2, 0);
  }
}

