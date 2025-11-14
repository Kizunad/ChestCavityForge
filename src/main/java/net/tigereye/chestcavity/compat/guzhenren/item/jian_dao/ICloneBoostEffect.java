package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.PersistentGuCultivatorClone;

/**
 * 分身增益物品效果接口
 *
 * <p>定义增益物品对分身的影响。增益效果在物品放入增益槽位时应用，移除时清除。
 *
 * <p><strong>实现指南:</strong>
 * <ul>
 *   <li>{@link #apply(PersistentGuCultivatorClone, ItemStack)} - 当物品放入增益槽位时调用
 *   <li>{@link #remove(PersistentGuCultivatorClone, ItemStack)} - 当物品从增益槽位移除时调用
 *   <li>{@link #getDescription(ItemStack)} - 提供效果描述（可选，用于UI显示）
 * </ul>
 *
 * <p><strong>使用示例:</strong>
 * <pre>{@code
 * ICloneBoostEffect diamondBoost = new ICloneBoostEffect() {
 *     @Override
 *     public void apply(PersistentGuCultivatorClone clone, ItemStack stack) {
 *         // 增加20%攻击力
 *         var attr = clone.getAttribute(Attributes.ATTACK_DAMAGE);
 *         if (attr != null) {
 *             attr.addPermanentModifier(new AttributeModifier(
 *                 UUID.fromString("..."), "diamond_boost", 0.2, Operation.MULTIPLY_BASE));
 *         }
 *     }
 *
 *     @Override
 *     public void remove(PersistentGuCultivatorClone clone, ItemStack stack) {
 *         // 移除增益
 *         var attr = clone.getAttribute(Attributes.ATTACK_DAMAGE);
 *         if (attr != null) {
 *             attr.removeModifier(UUID.fromString("..."));
 *         }
 *     }
 * };
 * }</pre>
 *
 * @see CloneBoostItemRegistry#register(net.minecraft.world.item.Item, ICloneBoostEffect)
 */
@FunctionalInterface
public interface ICloneBoostEffect {

    /**
     * 应用增益效果到分身
     *
     * <p>当物品放入增益槽位时调用。可以：
     * <ul>
     *   <li>修改分身属性（攻击力、防御、速度等）
     *   <li>添加状态效果（力量、抗性提升等）
     *   <li>修改分身行为（AI模式等）
     * </ul>
     *
     * <p><strong>注意:</strong>
     * <ul>
     *   <li>应使用唯一的UUID标识属性修改器，以便在 {@link #remove} 中正确移除
     *   <li>避免持久化修改（不要修改PersistentData中的境界等核心数据）
     *   <li>确保 {@link #apply} 和 {@link #remove} 操作对称
     * </ul>
     *
     * @param clone 要增益的分身实体
     * @param stack 增益物品堆（可能包含NBT数据）
     */
    void apply(PersistentGuCultivatorClone clone, ItemStack stack);

    /**
     * 从分身移除增益效果
     *
     * <p>当物品从增益槽位移除时调用。应撤销 {@link #apply} 中所做的所有修改。
     *
     * <p><strong>默认实现:</strong> 空操作（适用于一次性效果）
     *
     * @param clone 要移除增益的分身实体
     * @param stack 被移除的增益物品堆
     */
    default void remove(PersistentGuCultivatorClone clone, ItemStack stack) {
        // 默认不执行任何操作（适用于一次性效果或不可逆效果）
    }

    /**
     * 获取增益效果的描述文本
     *
     * <p>用于在界面或物品提示中显示增益效果说明。
     *
     * <p><strong>默认实现:</strong> 返回空描述
     *
     * @param stack 增益物品堆
     * @return 效果描述组件（可包含格式化文本）
     */
    default Component getDescription(ItemStack stack) {
        return Component.empty();
    }
}
