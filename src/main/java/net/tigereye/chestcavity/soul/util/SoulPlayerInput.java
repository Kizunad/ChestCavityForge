package net.tigereye.chestcavity.soul.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

/**
 * Utility helpers to simulate client left/right mouse operations for SoulPlayer on the server.
 * These methods mirror vanilla item use/attack flows and perform the minimum state updates so that
 * FakePlayer interactions behave like real players.
 */
public final class SoulPlayerInput {

  private SoulPlayerInput() {}

  /**
   * Simulates a right click with the given hand (use item in air). If forceFinish is true, finishes
   * the use immediately (useful for FakePlayer to avoid hold-to-use items).
   */
  public static boolean rightMouseItemUse(
      SoulPlayer player, InteractionHand hand, boolean forceFinish) {
    if (player == null || player.level().isClientSide()) return false;
    ItemStack stack = player.getItemInHand(hand);
    if (stack.isEmpty()) return false;

    InteractionResultHolder<ItemStack> result = stack.use(player.level(), player, hand);
    InteractionResult type = result.getResult();
    boolean consumed = type.consumesAction() || type == InteractionResult.SUCCESS;
    if (!consumed) return false;

    ItemStack after = result.getObject();
    if (after != stack) {
      player.setItemInHand(hand, after);
    }
    if (forceFinish) {
      ItemStack finished = player.getItemInHand(hand).finishUsingItem(player.level(), player);
      player.setItemInHand(hand, finished);
      SoulLog.info(
          "[soul][input] force-finish right use hand={} item={}",
          hand,
          BuiltInRegistries.ITEM.getKey(finished.getItem()));
    } else if (after.getUseDuration(player) > 0 && !player.isUsingItem()) {
      player.startUsingItem(hand);
    }
    return true;
  }

  /**
   * Simulates a right click use on a block with the given hit result. If forceFinish is true,
   * immediately finishes item use to avoid lingering use state.
   */
  public static boolean rightMouseUseOnBlock(
      SoulPlayer player, InteractionHand hand, BlockHitResult hit, boolean forceFinish) {
    if (player == null || player.level().isClientSide()) return false;
    ItemStack stack = player.getItemInHand(hand);
    if (stack.isEmpty()) return false;

    UseOnContext ctx = new UseOnContext(player, hand, hit);
    InteractionResult res = stack.useOn(ctx);
    if (!res.consumesAction() && res != InteractionResult.SUCCESS) return false;

    if (forceFinish) {
      ItemStack finished = player.getItemInHand(hand).finishUsingItem(player.level(), player);
      player.setItemInHand(hand, finished);
      SoulLog.info(
          "[soul][input] force-finish use-on-block hand={} pos={}", hand, hit.getBlockPos());
    }
    return true;
  }

  /**
   * Simulates a left click attack on a living target. Uses vanilla attack pipeline for
   * events/enchants.
   */
  public static boolean leftMouseAttackEntity(SoulPlayer player, LivingEntity target) {
    if (player == null || target == null) return false;
    if (player.level().isClientSide() || !target.isAlive()) return false;
    if (player.isUsingItem()) return false; // do not interrupt eating/drinking
    player.swing(InteractionHand.MAIN_HAND, true);
    player.attack(target);
    return true;
  }

  /**
   * Convenience: ray-traces up to reach distance and tries right use on the first block hit; falls
   * back to air use.
   */
  public static boolean rightClickAuto(
      SoulPlayer player, InteractionHand hand, double reach, boolean forceFinish) {
    if (player == null || player.level().isClientSide()) return false;
    Vec3 from = player.getEyePosition();
    Vec3 look = player.getLookAngle();
    Vec3 to = from.add(look.scale(Math.max(1.0, reach)));
    var hit =
        player
            .level()
            .clip(
                new ClipContext(
                    from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    if (hit.getType() == HitResult.Type.BLOCK) {
      BlockHitResult bhr = (BlockHitResult) hit;
      return rightMouseUseOnBlock(player, hand, bhr, forceFinish);
    }
    return rightMouseItemUse(player, hand, forceFinish);
  }

  /**
   * Uses the specified item from offhand if present and not on cooldown. Returns true on successful
   * consumption/use.
   */
  public static boolean useOffhandIfReady(SoulPlayer player, Item item, boolean forceFinish) {
    if (player == null || player.level().isClientSide()) return false;
    if (player.getCooldowns().isOnCooldown(item)) return false;
    ItemStack off = player.getOffhandItem();
    if (off.isEmpty() || off.getItem() != item) return false;
    return rightMouseItemUse(player, InteractionHand.OFF_HAND, forceFinish);
  }

  /**
   * If the offhand does not hold the requested item, temporarily swaps the first matching hotbar
   * stack into the offhand, uses it once, then restores previous slots. Skips when on cooldown.
   */
  public static boolean useWithOffhandSwapIfReady(
      SoulPlayer player, Item item, boolean forceFinish) {
    if (player == null || player.level().isClientSide()) return false;
    if (player.getCooldowns().isOnCooldown(item)) return false;

    // If already in offhand, use directly
    if (useOffhandIfReady(player, item, forceFinish)) return true;

    // Prefer hotbar, then the rest of the inventory
    int slot = -1;
    int size = player.getInventory().getContainerSize();
    // hotbar 0..8
    for (int i = 0; i < Math.min(9, size); i++) {
      ItemStack s = player.getInventory().getItem(i);
      if (!s.isEmpty() && s.getItem() == item) {
        slot = i;
        break;
      }
    }
    // main inventory 9..size-1
    if (slot == -1) {
      for (int i = 9; i < size; i++) {
        ItemStack s = player.getInventory().getItem(i);
        if (!s.isEmpty() && s.getItem() == item) {
          slot = i;
          break;
        }
      }
    }
    if (slot == -1) return false;

    ItemStack prevOff = player.getOffhandItem().copy();
    ItemStack hot = player.getInventory().getItem(slot);
    // Move whole stack into offhand (preserve reference for mutation on use)
    player.setItemInHand(InteractionHand.OFF_HAND, hot);
    player.getInventory().setItem(slot, ItemStack.EMPTY);
    boolean ok = false;
    try {
      ok = rightMouseItemUse(player, InteractionHand.OFF_HAND, forceFinish);
    } finally {
      // Move back whatever remains to inventory safely, then restore previous offhand
      ItemStack remain = player.getOffhandItem();
      // Clear offhand first to avoid duplicating reference when adding to inventory
      player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      safeReturnToInventory(player, slot, remain);
      // Restore original offhand snapshot
      player.setItemInHand(InteractionHand.OFF_HAND, prevOff);
    }
    return ok;
  }

  /** Uses the specified item from mainhand if present and not on cooldown. */
  public static boolean useMainhandIfReady(SoulPlayer player, Item item, boolean forceFinish) {
    if (player == null || player.level().isClientSide()) return false;
    if (player.getCooldowns().isOnCooldown(item)) return false;
    ItemStack main = player.getMainHandItem();
    if (main.isEmpty() || main.getItem() != item) return false;
    return rightMouseItemUse(player, InteractionHand.MAIN_HAND, forceFinish);
  }

  /** Swap a matching stack into mainhand, use once, then restore. */
  public static boolean useWithMainhandSwapIfReady(
      SoulPlayer player, Item item, boolean forceFinish) {
    if (player == null || player.level().isClientSide()) return false;
    if (player.getCooldowns().isOnCooldown(item)) return false;
    if (useMainhandIfReady(player, item, forceFinish)) return true;
    int slot = -1;
    int size = player.getInventory().getContainerSize();
    for (int i = 0; i < Math.min(9, size); i++) {
      ItemStack s = player.getInventory().getItem(i);
      if (!s.isEmpty() && s.getItem() == item) {
        slot = i;
        break;
      }
    }
    if (slot == -1) {
      for (int i = 9; i < size; i++) {
        ItemStack s = player.getInventory().getItem(i);
        if (!s.isEmpty() && s.getItem() == item) {
          slot = i;
          break;
        }
      }
    }
    if (slot == -1) return false;

    ItemStack prevMain = player.getMainHandItem().copy();
    ItemStack hot = player.getInventory().getItem(slot);
    player.setItemInHand(InteractionHand.MAIN_HAND, hot);
    player.getInventory().setItem(slot, ItemStack.EMPTY);
    boolean ok = false;
    try {
      ok = rightMouseItemUse(player, InteractionHand.MAIN_HAND, forceFinish);
    } finally {
      ItemStack remain = player.getMainHandItem();
      player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      safeReturnToInventory(player, slot, remain);
      player.setItemInHand(InteractionHand.MAIN_HAND, prevMain);
    }
    return ok;
  }

  private static void safeReturnToInventory(SoulPlayer player, int preferredSlot, ItemStack stack) {
    if (stack == null || stack.isEmpty()) return;
    var inv = player.getInventory();
    ItemStack cur = inv.getItem(preferredSlot);
    if (cur.isEmpty()) {
      inv.setItem(preferredSlot, stack);
      return;
    }
    // Try to merge into inventory; if not fully added, drop the rest
    ItemStack copy = stack.copy();
    boolean added = inv.add(copy);
    if (!added || !copy.isEmpty()) {
      // drop remaining safely at feet (no random throw)
      player.drop(copy, false);
    }
  }

  // --- Matching helpers for stacks with NBT (e.g., specific potions) ---

  /**
   * Try to use an offhand stack that matches the given predicate; otherwise swap a matching
   * inventory stack into the offhand and use it once.
   */
  public static boolean useAnyMatchingWithOffhandFirst(
      SoulPlayer player, java.util.function.Predicate<ItemStack> matcher, boolean forceFinish) {
    if (player == null || player.level().isClientSide()) return false;
    // Offhand direct
    ItemStack off = player.getOffhandItem();
    if (!off.isEmpty() && matcher.test(off)) {
      return rightMouseItemUse(player, InteractionHand.OFF_HAND, forceFinish);
    }
    // Mainhand direct
    ItemStack main = player.getMainHandItem();
    if (!main.isEmpty() && matcher.test(main)) {
      return rightMouseItemUse(player, InteractionHand.MAIN_HAND, forceFinish);
    }
    // Find a matching stack in inventory
    int slot = -1;
    int size = player.getInventory().getContainerSize();
    for (int i = 0; i < size; i++) {
      ItemStack s = player.getInventory().getItem(i);
      if (!s.isEmpty() && matcher.test(s)) {
        slot = i;
        break;
      }
    }
    if (slot == -1) return false;

    // Swap into offhand, use, then restore
    ItemStack prevOff = off.copy();
    ItemStack hot = player.getInventory().getItem(slot);
    player.setItemInHand(InteractionHand.OFF_HAND, hot);
    player.getInventory().setItem(slot, ItemStack.EMPTY);
    boolean ok = false;
    try {
      ok = rightMouseItemUse(player, InteractionHand.OFF_HAND, forceFinish);
    } finally {
      ItemStack remain = player.getOffhandItem();
      player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      safeReturnToInventory(player, slot, remain);
      player.setItemInHand(InteractionHand.OFF_HAND, prevOff);
    }
    return ok;
  }
}
