package net.tigereye.chestcavity.chestcavities.types;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.ChestCavityInventory;

import java.util.ArrayList;
import java.util.List;

public record RandomChestCavityFiller(ResourceLocation id, int minCount, int maxCount, List<Item> items, int chance) {

    public RandomChestCavityFiller {
        items = List.copyOf(items);
        if (chance < 0 || chance > 100) {
            throw new IllegalArgumentException("Chance must be between 0 and 100");
        }
    }

    public void fill(ChestCavityInventory inventory, RandomSource random) {
        if (items.isEmpty() || maxCount <= 0) {
            return;
        }
        
        // 检查 chance 参数，如果设置了且随机数不在范围内，则跳过
        if (chance < 100) {
            int roll = random.nextInt(100);
            if (roll >= chance) {
                return;
            }
        }
        
        int minimum = Math.max(0, minCount);
        int maximum = Math.max(minimum, maxCount);
        if (maximum == 0) {
            return;
        }
        int rolls = random.nextInt(maximum - minimum + 1) + minimum;
        if (rolls <= 0) {
            return;
        }
        List<Integer> emptySlots = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).isEmpty()) {
                emptySlots.add(i);
            }
        }
        if (emptySlots.isEmpty()) {
            return;
        }
        rolls = Math.min(rolls, emptySlots.size());
        for (int i = 0; i < rolls; i++) {
            int slotIndex = random.nextInt(emptySlots.size());
            int slot = emptySlots.remove(slotIndex);
            Item item = items.get(random.nextInt(items.size()));
            inventory.setItem(slot, new ItemStack(item));
        }
    }
}
