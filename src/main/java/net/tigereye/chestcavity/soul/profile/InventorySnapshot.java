package net.tigereye.chestcavity.soul.profile;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 玩家背包快照
 *
 * 说明
 * - 仅保存原版的 41 个槽位：36 主背包 + 4 护甲槽 + 1 副手。
 * - 不涉及外部模组的扩展容器；用于灵魂存档的最小可用还原。
 */
public record InventorySnapshot(NonNullList<ItemStack> items, int selectedHotbar) {

    private static final int TOTAL_SLOTS = 41;

    public static InventorySnapshot capture(Player player) {
        NonNullList<ItemStack> stacks = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            stacks.set(i, player.getInventory().items.get(i).copy());
        }
        for (int i = 0; i < player.getInventory().armor.size(); i++) {
            stacks.set(player.getInventory().items.size() + i, player.getInventory().armor.get(i).copy());
        }
        stacks.set(player.getInventory().items.size() + player.getInventory().armor.size(),
                player.getInventory().offhand.get(0).copy());
        int selected = player.getInventory().selected;
        if (selected < 0 || selected > 8) selected = Math.max(0, Math.min(8, selected));
        return new InventorySnapshot(stacks, selected);
    }

    public void restore(Player player) {
        clearInventory(player);
        for (int i = 0; i < player.getInventory().items.size() && i < items.size(); i++) {
            player.getInventory().items.set(i, items.get(i).copy());
        }
        int base = player.getInventory().items.size();
        for (int i = 0; i < player.getInventory().armor.size() && base + i < items.size(); i++) {
            player.getInventory().armor.set(i, items.get(base + i).copy());
        }
        int offhandIndex = base + player.getInventory().armor.size();
        if (offhandIndex < items.size()) {
            player.getInventory().offhand.set(0, items.get(offhandIndex).copy());
        }
        // Restore selected hotbar slot if valid
        int sel = selectedHotbar;
        if (sel >= 0 && sel <= 8) {
            player.getInventory().selected = sel;
        }
        player.getInventory().setChanged();
    }

    public static InventorySnapshot empty() {
        return new InventorySnapshot(NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY), 0);
    }

    private void clearInventory(Player player) {
        player.getInventory().items.replaceAll(ignored -> ItemStack.EMPTY);
        player.getInventory().armor.replaceAll(ignored -> ItemStack.EMPTY);
        player.getInventory().offhand.replaceAll(ignored -> ItemStack.EMPTY);
        player.getInventory().setChanged();
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                root.put(String.valueOf(i), stack.save(provider, new CompoundTag()));
            }
        }
        root.putInt("size", items.size());
        root.putInt("selected", Math.max(0, Math.min(8, selectedHotbar)));
        return root;
    }

    public static InventorySnapshot load(CompoundTag tag, HolderLookup.Provider provider) {
        int size = Math.max(tag.getInt("size"), TOTAL_SLOTS);
        NonNullList<ItemStack> stacks = NonNullList.withSize(size, ItemStack.EMPTY);
        for (String key : tag.getAllKeys()) {
            if (key.equals("size")) {
                continue;
            }
            try {
                int index = Integer.parseInt(key);
                if (index >= 0 && index < size) {
                    stacks.set(index, ItemStack.parseOptional(provider, tag.getCompound(key)));
                }
            } catch (NumberFormatException ignored) {
            }
        }
        int selected = tag.contains("selected") ? tag.getInt("selected") : 0;
        if (selected < 0 || selected > 8) selected = 0;
        return new InventorySnapshot(stacks, selected);
    }
}
