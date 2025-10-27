package net.tigereye.chestcavity.util.mock;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.chestcavities.ChestCavityInventory;

import static org.mockito.Mockito.*;

/**
 * ChestCavity 相关的 Mock 助手
 * 用于创建和操作模拟的胸腔实例
 */
public class MockChestCavityHelper {

    /**
     * 创建一个模拟的胸腔实例
     * @param owner 拥有者
     * @return 模拟的 ChestCavityInstance
     */
    public static ChestCavityInstance createMockChestCavity(LivingEntity owner) {
        ChestCavityInstance cc = mock(ChestCavityInstance.class);

        // 创建实际的 inventory（用于存储器官）
        ChestCavityInventory inventory = new ChestCavityInventory();

        // 模拟字段访问
        cc.owner = owner;
        cc.opened = true;
        cc.inventory = inventory;

        return cc;
    }

    /**
     * 向胸腔添加器官
     */
    public static void addOrgan(ChestCavityInstance cc, ItemStack organ) {
        cc.inventory.addItem(organ);
    }

    /**
     * 从胸腔移除器官
     */
    public static void removeOrgan(ChestCavityInstance cc, ItemStack organ) {
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            if (cc.inventory.getItem(i) == organ) {
                cc.inventory.removeItemNoUpdate(i);
                break;
            }
        }
    }

    /**
     * 检查胸腔是否包含特定器官
     * 注意：简化实现，仅检查非空 ItemStack
     */
    public static boolean hasOrgan(ChestCavityInstance cc, ResourceLocation organId) {
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (!stack.isEmpty()) {
                // 简化实现：假设非空即为目标器官
                // 实际测试时可根据需要扩展
                return true;
            }
        }
        return false;
    }

    /**
     * 获取特定器官的数量
     * 注意：简化实现，返回所有非空ItemStack数量
     */
    public static int getOrganCount(ChestCavityInstance cc, ResourceLocation organId) {
        int count = 0;
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 清空胸腔
     */
    public static void clearOrgans(ChestCavityInstance cc) {
        cc.inventory.clearContent();
    }

    /**
     * 获取持久化数据（存储在owner的NBT中）
     */
    public static CompoundTag getPersistentData(ChestCavityInstance cc) {
        return cc.owner.getPersistentData();
    }
}
