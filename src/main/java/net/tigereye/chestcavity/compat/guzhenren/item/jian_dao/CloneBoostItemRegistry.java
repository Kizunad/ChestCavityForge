package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.Set;

/**
 * 分身增益物品注册表
 *
 * <p>管理可放入分身增益槽位的物品及其效果。
 *
 * <p><strong>设计说明:</strong>
 * <ul>
 *   <li>阶段4将实现完整的增益效果系统 (ICloneBoostEffect接口)
 *   <li>当前版本仅提供基础的物品注册和过滤功能
 *   <li>示例增益物品暂未注册，待阶段4完整实现
 * </ul>
 *
 * @see net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.ui.CloneInventoryMenu.BoostSlot
 */
public final class CloneBoostItemRegistry {

    /**
     * 已注册的增益物品集合
     */
    private static final Set<Item> BOOST_ITEMS = new HashSet<>();

    private CloneBoostItemRegistry() {
        // 工具类，禁止实例化
    }

    /**
     * 注册增益物品
     *
     * @param item 要注册的物品
     */
    public static void register(Item item) {
        BOOST_ITEMS.add(item);
    }

    /**
     * 检查物品是否为增益物品
     *
     * @param item 要检查的物品
     * @return 如果该物品可放入增益槽位则返回 true
     */
    public static boolean isBoostItem(Item item) {
        return BOOST_ITEMS.contains(item);
    }

    /**
     * 获取所有已注册的增益物品
     *
     * @return 增益物品集合（不可修改）
     */
    public static Set<Item> getRegisteredItems() {
        return Set.copyOf(BOOST_ITEMS);
    }

    /**
     * 清空所有注册的增益物品
     * <p><strong>警告:</strong> 仅用于测试或重新加载，生产环境不应调用
     */
    public static void clear() {
        BOOST_ITEMS.clear();
    }

    /**
     * 注册默认增益物品
     * <p>当前为空实现，待阶段4添加示例物品（钻石、凋零骷髅头等）
     */
    public static void registerDefaults() {
        // TODO: 阶段4实现
        // 示例：
        // register(Items.DIAMOND);
        // register(Items.WITHER_SKELETON_SKULL);
        // register(Items.NETHER_STAR);
    }
}
