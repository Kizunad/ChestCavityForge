package net.tigereye.chestcavity.soul.util;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * 通用掉落物过滤工具。
 */
public final class ItemFilterOps {

    private ItemFilterOps() {
    }

    /**
     * 判断物品是否为方块掉落物。
     */
    public static boolean isBlockDrop(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.getItem() instanceof BlockItem;
    }

    /**
     * 判断掉落物是否可视为“实用”——默认排除空气或空栈。
     */
    public static boolean isUseful(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            return block.defaultDestroyTime() >= 0.0f;
        }
        return true;
    }
}
