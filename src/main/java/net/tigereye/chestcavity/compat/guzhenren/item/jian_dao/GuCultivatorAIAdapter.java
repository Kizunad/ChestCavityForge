package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * 蛊修分身AI适配器。
 *
 * <p>负责管理分身的蛊虫释放AI逻辑，包括：
 * <ul>
 *   <li>初始化AI所需的 PersistentData 键
 *   <li>控制蛊虫冷却时间
 *   <li>执行蛊虫释放逻辑（放入主手 → swing → 清空）
 * </ul>
 *
 * <p><strong>设计说明：</strong>
 * <ul>
 *   <li>MVP版本：仅实现基础蛊虫释放，不包含杀招系统
 *   <li>性能优化：每3 tick执行一次（由调用方控制）
 *   <li>异常防护：所有方法都进行try-catch包裹，防止AI异常导致崩溃
 * </ul>
 *
 * @see net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.PersistentGuCultivatorClone#baseTick()
 */
public final class GuCultivatorAIAdapter {

    /**
     * 私有构造函数，防止实例化（工具类）
     */
    private GuCultivatorAIAdapter() {
        throw new AssertionError("GuCultivatorAIAdapter is a utility class and should not be instantiated");
    }

    // ============ AI数据键常量 ============
    private static final String KEY_MELEE_MODE = "近战";
    private static final String KEY_GU_GLOBAL_CD = "蛊虫CD";
    private static final String KEY_GU_CD_PREFIX = "蛊虫";
    private static final String KEY_GU_CD_SUFFIX = "CD";
    private static final String KEY_KILLMOVE_CD = "杀招CD";

    // ============ 冷却时间常量（单位：tick） ============
    private static final double GU_GLOBAL_CD_TICKS = 20.0;      // 全局冷却：1秒
    private static final double GU_INDIVIDUAL_CD_TICKS = 100.0; // 单个蛊虫冷却：5秒
    private static final double KILLMOVE_INITIAL_CD_TICKS = 200.0; // 杀招初始冷却：10秒（MVP阶段不实现）

    /**
     * 初始化AI所需的 PersistentData 键。
     *
     * <p><strong>调用时机：</strong>分身首次生成时（在 {@code baseTick()} 中检测 {@code ai_initialized} 标记）
     *
     * <p><strong>初始化的数据：</strong>
     * <ul>
     *   <li>{@code 近战}: false（默认不使用近战模式）
     *   <li>{@code 蛊虫CD}: 0.0（全局冷却，初始无冷却）
     *   <li>{@code 蛊虫1CD} ~ {@code 蛊虫5CD}: 0.0（单个蛊虫冷却）
     *   <li>{@code 杀招CD}: 200.0（初始延迟10秒，MVP阶段不实现杀招）
     * </ul>
     *
     * @param entity 分身实体
     */
    public static void initializeAIData(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();

        // 近战模式标记
        tag.putBoolean(KEY_MELEE_MODE, false);

        // 蛊虫冷却 (全部初始化为0)
        tag.putDouble(KEY_GU_GLOBAL_CD, 0.0);
        for (int i = 1; i <= 5; i++) {
            tag.putDouble(KEY_GU_CD_PREFIX + i + KEY_GU_CD_SUFFIX, 0.0);
        }

        // 杀招冷却 (初始延迟10秒，MVP阶段不实现杀招)
        tag.putDouble(KEY_KILLMOVE_CD, KILLMOVE_INITIAL_CD_TICKS);
    }

    /**
     * 每3 tick执行一次的蛊虫释放逻辑。
     *
     * <p><strong>执行流程：</strong>
     * <ol>
     *   <li>递减所有冷却时间（因为每3 tick执行一次，所以减少3.0）
     *   <li>检查全局冷却是否完成
     *   <li>遍历6个蛊虫槽位，找到第一个冷却完毕且有物品的槽位
     *   <li>释放该蛊虫并设置冷却
     * </ol>
     *
     * <p><strong>冷却机制：</strong>
     * <ul>
     *   <li><strong>全局冷却：</strong>控制两次蛊虫释放的最小间隔（1秒）
     *   <li><strong>单个冷却：</strong>控制同一槽位的蛊虫再次释放的间隔（5秒）
     * </ul>
     *
     * <p><strong>性能优化：</strong>
     * <ul>
     *   <li>每3 tick执行一次（由调用方 {@code PersistentGuCultivatorClone.baseTick()} 控制）
     *   <li>异常防护：所有操作都在try-catch中，防止AI逻辑异常导致游戏崩溃
     * </ul>
     *
     * @param entity 分身实体
     * @param handler 物品栏处理器（7格：0-5为蛊虫，6为增益）
     */
    public static void tickGuUsage(LivingEntity entity, IItemHandlerModifiable handler) {
        if (!(entity instanceof Mob mob)) {
            return; // 仅处理 Mob 实体
        }

        LivingEntity target = mob.getTarget();
        if (target == null) {
            return; // 没有攻击目标，不释放蛊虫
        }

        CompoundTag tag = entity.getPersistentData();

        // ========== 冷却递减 ==========
        // 因为每3 tick执行一次，所以减少3.0
        tag.putDouble(KEY_GU_GLOBAL_CD, tag.getDouble(KEY_GU_GLOBAL_CD) - 3.0);
        for (int i = 1; i <= 5; i++) {
            String cdKey = KEY_GU_CD_PREFIX + i + KEY_GU_CD_SUFFIX;
            tag.putDouble(cdKey, tag.getDouble(cdKey) - 3.0);
        }

        // ========== 尝试释放蛊虫 (全局CD控制) ==========
        if (tag.getDouble(KEY_GU_GLOBAL_CD) <= 0.0) {
            // 遍历6个槽位，找到第一个冷却完毕的蛊虫
            for (int slot = 0; slot < 6; slot++) {
                // 槽位0使用全局CD键，槽位1-5使用专用CD键
                String cdKey = (slot == 0) ? KEY_GU_GLOBAL_CD : (KEY_GU_CD_PREFIX + slot + KEY_GU_CD_SUFFIX);

                if (tag.getDouble(cdKey) <= 0.0) {
                    ItemStack guStack = handler.getStackInSlot(slot);

                    if (!guStack.isEmpty()) {
                        // 执行释放逻辑
                        releaseGu(entity, guStack);

                        // 设置冷却
                        tag.putDouble(cdKey, GU_INDIVIDUAL_CD_TICKS);
                        tag.putDouble(KEY_GU_GLOBAL_CD, GU_GLOBAL_CD_TICKS);
                        break; // 每次只释放一个
                    }
                }
            }
        }
    }

    /**
     * 释放蛊虫（放入主手 → swing → 清空）。
     *
     * <p><strong>实现逻辑：</strong>
     * <ol>
     *   <li>复制蛊虫物品（设置数量为1）
     *   <li>放入主手
     *   <li>调用 {@code swing()} 触发物品使用逻辑
     *   <li>清空主手（避免持久占用）
     * </ol>
     *
     * <p><strong>注意事项：</strong>
     * <ul>
     *   <li>不直接从物品栏中移除蛊虫（保留在分身物品栏中）
     *   <li>使用物品副本，避免影响原物品
     *   <li>异常防护：释放失败也不会导致崩溃
     * </ul>
     *
     * @param entity 分身实体
     * @param guStack 蛊虫物品（来自物品栏槽位）
     */
    private static void releaseGu(LivingEntity entity, ItemStack guStack) {
        try {
            // 1. 放入主手（使用副本）
            ItemStack copy = guStack.copy();
            copy.setCount(1);
            entity.setItemInHand(InteractionHand.MAIN_HAND, copy);

            // 2. 挥动手臂 (触发物品使用逻辑)
            entity.swing(InteractionHand.MAIN_HAND, true);

            // 3. 清空主手 (避免持久占用)
            entity.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        } catch (Exception e) {
            // 释放失败也不要崩溃，静默处理
            // 日志已在 PersistentGuCultivatorClone.baseTick() 中记录
        }
    }
}
