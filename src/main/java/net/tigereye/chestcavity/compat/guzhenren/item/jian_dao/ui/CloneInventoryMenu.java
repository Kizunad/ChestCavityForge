package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.ui;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.PersistentGuCultivatorClone;
import net.tigereye.chestcavity.registration.CCContainers;

import javax.annotation.Nonnull;

/**
 * 分身物品栏菜单 (7槽位)
 *
 * <p>槽位布局:
 * <ul>
 *   <li>槽位 0-5: 蛊虫槽位 (仅允许 tag: guzhenren:guchong)
 *   <li>槽位 6: 增益物品槽位 (由 CloneBoostItemRegistry 管理)
 * </ul>
 *
 * @see net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.CloneBoostItemRegistry
 */
public class CloneInventoryMenu extends AbstractContainerMenu {

    private static final int CLONE_INVENTORY_SIZE = 7;
    private static final int GU_SLOTS = 6;
    private static final int BOOST_SLOT = 6;

    private final Container cloneInventory;
    private final PersistentGuCultivatorClone clone;

    /**
     * 客户端构造函数 (用于菜单同步)
     */
    public CloneInventoryMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new net.minecraft.world.SimpleContainer(CLONE_INVENTORY_SIZE), null);
    }

    /**
     * 服务端构造函数 (绑定到实体)
     */
    public CloneInventoryMenu(int syncId, Inventory playerInventory, PersistentGuCultivatorClone clone) {
        this(syncId, playerInventory, new ItemStackHandlerContainer(clone.getInventory(), clone), clone);
    }

    /**
     * 内部构造函数
     */
    private CloneInventoryMenu(int syncId, Inventory playerInventory, Container cloneInventory, PersistentGuCultivatorClone clone) {
        super(CCContainers.CLONE_INVENTORY_MENU.get(), syncId);
        this.cloneInventory = cloneInventory;
        this.clone = clone;

        checkContainerSize(cloneInventory, CLONE_INVENTORY_SIZE);
        cloneInventory.startOpen(playerInventory.player);

        // ============ 分身物品栏槽位 (7格) ============
        // 槽位 0-5: 蛊虫槽位 (第一行，横向排列)
        for (int i = 0; i < GU_SLOTS; i++) {
            this.addSlot(new GuSlot(cloneInventory, i, 8 + i * 18, 18));
        }

        // 槽位 6: 增益槽位 (第二行，左侧)
        this.addSlot(new BoostSlot(cloneInventory, BOOST_SLOT, 8, 54));

        // ============ 玩家背包槽位 ============
        // 玩家背包 (3行 × 9列)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        8 + col * 18,
                        84 + row * 18
                ));
            }
        }

        // 玩家快捷栏 (1行 × 9列)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(
                    playerInventory,
                    col,
                    8 + col * 18,
                    142
            ));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return cloneInventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            // 从分身物品栏移动到玩家背包
            if (index < CLONE_INVENTORY_SIZE) {
                if (!this.moveItemStackTo(stack, CLONE_INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // 从玩家背包移动到分身物品栏
            else {
                // 尝试放入蛊虫槽位 (0-5)
                if (isGuItem(stack)) {
                    if (!this.moveItemStackTo(stack, 0, GU_SLOTS, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // 尝试放入增益槽位 (6)
                else if (isBoostItem(stack)) {
                    if (!this.moveItemStackTo(stack, BOOST_SLOT, BOOST_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // 物品不属于任何分身槽位类型
                else {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }

        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.cloneInventory.stopOpen(player);
    }

    /**
     * 检查物品是否为蛊虫
     */
    private static boolean isGuItem(ItemStack stack) {
        return stack.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("guzhenren", "guchong")));
    }

    /**
     * 检查物品是否为增益物品
     */
    private static boolean isBoostItem(ItemStack stack) {
        return net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.CloneBoostItemRegistry.isBoostItem(stack.getItem());
    }

    /**
     * 蛊虫槽位 (仅允许蛊虫)
     */
    private static class GuSlot extends Slot {
        public GuSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {
            return isGuItem(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1; // 蛊虫不可堆叠
        }
    }

    /**
     * 增益物品槽位 (仅允许 CloneBoostItemRegistry 注册的物品)
     */
    private static class BoostSlot extends Slot {
        public BoostSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {
            return isBoostItem(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1; // 增益物品不可堆叠
        }
    }

    /**
     * 获取分身实体 (仅服务端可用)
     */
    public PersistentGuCultivatorClone getClone() {
        return clone;
    }

    /**
     * ItemStackHandler 到 Container 的适配器
     */
    private static class ItemStackHandlerContainer implements Container {
        private final ItemStackHandler handler;
        private final PersistentGuCultivatorClone clone;

        public ItemStackHandlerContainer(ItemStackHandler handler, PersistentGuCultivatorClone clone) {
            this.handler = handler;
            this.clone = clone;
        }

        @Override
        public int getContainerSize() {
            return handler.getSlots();
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return handler.extractItem(slot, amount, false);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = handler.getStackInSlot(slot);
            handler.setStackInSlot(slot, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            handler.setStackInSlot(slot, stack);
        }

        @Override
        public void setChanged() {
            // 物品栏变化时，触发增益效果更新
            if (clone != null && !clone.level().isClientSide) {
                clone.updateBoostEffect();
            }
        }

        @Override
        public boolean stillValid(Player player) {
            return clone != null && clone.isOwnedBy(player);
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < handler.getSlots(); i++) {
                handler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }
}
