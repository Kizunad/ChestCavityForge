package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.storage;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * 魂道器官“兽魂”存取的抽象接口。
 * <p>
 * 具体实现负责：
 * - 判断能否捕获；
 * - 序列化并写入到器官物品的自定义数据；
 * - 读取、消费、清空已存储的兽魂。
 */
public interface BeastSoulStorage {

    /**
     * 判断器官是否已有存储的兽魂。
     * @return 若已存在存储负载返回 {@code true}
     */
    boolean hasStoredSoul(ItemStack organ);

    /**
     * 捕获前的快速校验：默认排除玩家、空实体、已存在存储等情况。
     * 实现可进一步限制（如排除 Boss）。
     */
    default boolean canStore(ItemStack organ, LivingEntity entity) {
        return organ != null && !organ.isEmpty() && entity != null && !(entity instanceof net.minecraft.world.entity.player.Player) && !hasStoredSoul(organ);
    }

    /**
     * 序列化目标实体并保存到器官物品。
     * @param organ 承载存储负载的器官物品
     * @param entity 需要被捕获“兽魂”的实体
     * @param storedGameTime 捕获时的游戏时间（tick）
     * @return 若成功存储，返回快照记录
     */
    Optional<BeastSoulRecord> store(ItemStack organ, LivingEntity entity, long storedGameTime);

    /** 只读地返回已存储的兽魂负载，不修改器官物品。 */
    Optional<BeastSoulRecord> peek(ItemStack organ);

    /**
     * 移除并返回存储负载，调用方可据此在其他位置重塑实体。
     */
    Optional<BeastSoulRecord> consume(ItemStack organ);

    /** 无返回地清空存储负载。 */
    void clear(ItemStack organ);
}
