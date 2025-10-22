package net.tigereye.chestcavity.util;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.network.NetworkHandler;
import net.tigereye.chestcavity.network.packets.ChestCavityOrganSlotUpdatePayload;
import net.tigereye.chestcavity.network.packets.ChestCavityUpdatePayload;

public class NetworkUtil {
  // S2C = SERVER TO CLIENT //I think

  public static boolean SendS2CChestCavityUpdatePacket(ChestCavityInstance cc) {
    cc.updateInstantiated = true;
    if ((!cc.owner.level().isClientSide()) && cc.owner instanceof ServerPlayer player) {
      if (player.connection == null) {
        return false;
      }
      Map<net.minecraft.resources.ResourceLocation, Float> organScores =
          new HashMap<>(cc.getOrganScores());
      NetworkHandler.sendChestCavityUpdate(
          player, new ChestCavityUpdatePayload(cc.opened, organScores));
      return true;
    }
    return false;
  }

  /**
   * Synchronises a single inventory slot from the owner's chest cavity to the client. The stack
   * reference must come directly from the chest cavity inventory so the helper can resolve the slot
   * index.
   */
  public static boolean sendOrganSlotUpdate(ChestCavityInstance cc, ItemStack stack) {
    if (cc == null || stack == null || stack.isEmpty()) {
      return false;
    }
    if (cc.owner.level().isClientSide() || !(cc.owner instanceof ServerPlayer player)) {
      return false;
    }
    int slot = findSlot(cc, stack);
    if (slot < 0) {
      return false;
    }
    if (player.connection == null) {
      return false;
    }
    NetworkHandler.sendOrganSlotUpdate(
        player, new ChestCavityOrganSlotUpdatePayload(slot, stack.copy()));
    return true;
  }

  private static int findSlot(ChestCavityInstance cc, ItemStack target) {
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack slotStack = cc.inventory.getItem(i);
      if (slotStack == target) {
        return i;
      }
    }
    if (target.isEmpty()) {
      return -1;
    }
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack slotStack = cc.inventory.getItem(i);
      if (ItemStack.isSameItemSameComponents(slotStack, target)
          && slotStack.getCount() == target.getCount()) {
        return i;
      }
    }
    return -1;
  }
}
