package net.tigereye.chestcavity.ui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.ChestCavityInventory;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.registration.CCContainers;
import net.tigereye.chestcavity.util.ScoreboardUpgradeManager;

/**
 * The screen handler for the chest cavity GUI.
 */
public class ChestCavityScreenHandler extends AbstractContainerMenu {

  private final ChestCavityInventory inventory;
  private final int size;
  private final int rows;

  public int getRows() {
    return rows;
  }

  private static ChestCavityInventory getOrCreateChestCavityInventory(Inventory playerInventory) {
    Player player = playerInventory.player;
    ChestCavityInstance playerCc = CCAttachments.getExistingChestCavity(player).orElse(null);
    if (playerCc != null && playerCc.ccBeingOpened != null) {
      return playerCc.ccBeingOpened.inventory;
    }
    return new ChestCavityInventory();
  }

  /**
   * Creates a new ChestCavityScreenHandler.
   *
   * @param syncId The sync ID.
   * @param playerInventory The player inventory.
   */
  public ChestCavityScreenHandler(int syncId, Inventory playerInventory) {
    this(syncId, playerInventory, getOrCreateChestCavityInventory(playerInventory));
  }

  /**
   * Creates a new ChestCavityScreenHandler.
   *
   * @param syncId The sync ID.
   * @param playerInventory The player inventory.
   * @param inventory The chest cavity inventory.
   */
  public ChestCavityScreenHandler(
      int syncId, Inventory playerInventory, ChestCavityInventory inventory) {
    super(CCContainers.CHEST_CAVITY_SCREEN_HANDLER.get(), syncId);
    this.size = inventory.getContainerSize();
    this.inventory = inventory;
    this.rows = (size - 1) / 9 + 1;
    inventory.startOpen(playerInventory.player);
    int i = (rows - 4) * 18;

    int n;
    int m;
    for (n = 0; n < this.rows; ++n) {
      for (m = 0; m < 9 && (n * 9) + m < size; ++m) {
        final int slotIndex = m + n * 9;
        this.addSlot(
            new Slot(inventory, slotIndex, 8 + m * 18, 18 + n * 18) {
              @Override
              public boolean mayPickup(Player player) {
                ChestCavityInstance instance = inventory.getInstance();
                if (instance != null
                    && ScoreboardUpgradeManager.isSlotLocked(instance, slotIndex)) {
                  return false;
                }
                return super.mayPickup(player);
              }

              @Override
              public boolean mayPlace(ItemStack stack) {
                ChestCavityInstance instance = inventory.getInstance();
                if (instance != null
                    && ScoreboardUpgradeManager.isSlotLocked(instance, slotIndex)) {
                  return false;
                }
                return super.mayPlace(stack);
              }
            });
      }
    }

    for (n = 0; n < 3; ++n) {
      for (m = 0; m < 9; ++m) {
        this.addSlot(
            new Slot(
                playerInventory,
                m + n * 9 + 9,
                8 + m * 18,
                102 + n * 18 + i)); // 103 + n * 18 + i));
      }
    }

    for (n = 0; n < 9; ++n) {
      this.addSlot(new Slot(playerInventory, n, 8 + n * 18, 160 + i)); // 161 + i));
    }
  }

  @Override
  public ItemStack quickMoveStack(Player player, int invSlot) {
    ItemStack newStack = ItemStack.EMPTY;
    Slot slot = this.slots.get(invSlot);
    if (slot != null && slot.hasItem()) {
      if (slot.container == this.inventory) {
        ChestCavityInstance instance = inventory.getInstance();
        if (instance != null
            && ScoreboardUpgradeManager.isSlotLocked(instance, slot.getSlotIndex())) {
          return ItemStack.EMPTY;
        }
      }
      ItemStack originalStack = slot.getItem();
      newStack = originalStack.copy();
      if (invSlot < this.inventory.getContainerSize()) {
        /*if(inventory.getInstance().type.isSlotForbidden(invSlot)){
            return ItemStack.EMPTY;
        }*/
        if (!this.moveItemStackTo(
            originalStack, this.inventory.getContainerSize(), this.slots.size(), true)) {
          return ItemStack.EMPTY;
        }
      } else if (!this.moveItemStackTo(
          originalStack, 0, this.inventory.getContainerSize(), false)) {
        return ItemStack.EMPTY;
      }

      if (originalStack.isEmpty()) {
        slot.set(ItemStack.EMPTY);
      } else {
        slot.setChanged();
      }
    }

    return newStack;
  }

  @Override
  public boolean stillValid(Player player) {
    return this.inventory.stillValid(player);
  }
}