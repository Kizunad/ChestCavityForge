package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.PersistentGuCultivatorClone;

/**
 * 多重剑影蛊物品类
 *
 * <p>功能：
 * <ul>
 *   <li>右键：召唤/召回分身
 *   <li>Shift+右键：打开分身界面
 *   <li>所有权绑定：首次使用自动绑定玩家
 *   <li>跨维度支持：惰性清理机制
 * </ul>
 *
 * <p>NBT结构：
 * <ul>
 *   <li>OwnerUUID: 物品所有者UUID
 *   <li>CloneUUID: 当前分身实体UUID（临时）
 *   <li>DimensionKey: 分身所在维度
 *   <li>CloneData: 分身序列化数据（持久）
 * </ul>
 */
public class DuochongjianyingGuItem extends Item {

    public DuochongjianyingGuItem() {
        super(new Properties()
            .stacksTo(1)  // 不可堆叠
            .rarity(Rarity.RARE));
    }

    /**
     * 核心交互方法
     *
     * <p>交互优先级（修正v1.0交互不一致问题）：
     * 1. 所有权校验
     * 2. Shift+右键 -> 打开分身界面
     * 3. 右键 -> 召唤/召回分身
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 客户端不执行逻辑
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        // 1. 所有权校验
        if (!isOwnedBy(stack, player)) {
            player.displayClientMessage(Component.literal("§c这不是你的蛊虫!"), true);
            return InteractionResultHolder.fail(stack);
        }

        // 2. 优先处理 Shift + 右键 (打开界面)
        if (player.isShiftKeyDown()) {
            return handleOpenInventory(level, player, stack);
        }

        // 3. 处理召唤/召回
        return handleSummonOrRecall(level, player, stack);
    }

    /**
     * 处理打开分身界面
     *
     * <p>验证：
     * - 分身必须存在且存活
     * - 距离不超过10格（可选）
     */
    private InteractionResultHolder<ItemStack> handleOpenInventory(
        Level level,
        Player player,
        ItemStack stack
    ) {
        // 1. 检查分身是否存在
        PersistentGuCultivatorClone clone = findClone(level, stack);

        if (clone == null) {
            player.displayClientMessage(
                Component.literal("§c分身未召唤或已死亡!"),
                true
            );
            return InteractionResultHolder.fail(stack);
        }

        // 2. 检查距离 (最大10格)
        if (player.distanceToSqr(clone) > 100.0) {
            player.displayClientMessage(
                Component.literal("§c分身距离太远!"),
                true
            );
            return InteractionResultHolder.fail(stack);
        }

        // 3. 打开界面
        clone.openInventoryMenu((ServerPlayer) player);

        return InteractionResultHolder.success(stack);
    }

    /**
     * 处理召唤或召回分身
     *
     * <p>逻辑：
     * - 分身存在 -> 召回（保存数据到物品NBT）
     * - 分身不存在 -> 召唤（从物品NBT恢复数据）
     */
    private InteractionResultHolder<ItemStack> handleSummonOrRecall(
        Level level,
        Player player,
        ItemStack stack
    ) {
        // 1. 检查是否已有分身存在
        PersistentGuCultivatorClone existingClone = findClone(level, stack);

        if (existingClone != null) {
            // 存在 -> 召回
            recallClone(stack, existingClone, player);
            return InteractionResultHolder.success(stack);
        } else {
            // 不存在 -> 召唤
            PersistentGuCultivatorClone newClone = summonClone(level, player, stack);

            if (newClone != null) {
                // 保存UUID到物品NBT
                stack.getOrCreateTag().putUUID("CloneUUID", newClone.getUUID());
                stack.getTag().putString("DimensionKey", level.dimension().location().toString());

                player.displayClientMessage(
                    Component.literal("§6分身已召唤"),
                    true
                );
                return InteractionResultHolder.success(stack);
            } else {
                player.displayClientMessage(
                    Component.literal("§c召唤失败!"),
                    true
                );
                return InteractionResultHolder.fail(stack);
            }
        }
    }

    /**
     * 查找分身实体
     *
     * <p>边界检查：
     * - 维度一致性：跨维度返回null（惰性清理）
     * - 实体存活性：已死亡返回null（惰性清理）
     *
     * @return 存活的分身实体，或null
     */
    @Nullable
    private PersistentGuCultivatorClone findClone(Level level, ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().hasUUID("CloneUUID")) {
            return null;
        }

        UUID cloneUUID = stack.getTag().getUUID("CloneUUID");

        // 检查维度一致性
        if (stack.getTag().contains("DimensionKey")) {
            String savedDim = stack.getTag().getString("DimensionKey");
            String currentDim = level.dimension().location().toString();

            if (!savedDim.equals(currentDim)) {
                // 跨维度场景: 惰性清理 (不立即清理,等待用户下次操作)
                return null;
            }
        }

        // 查找实体
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }

        Entity entity = serverLevel.getEntity(cloneUUID);

        if (entity instanceof PersistentGuCultivatorClone clone && clone.isAlive()) {
            return clone;
        }

        // 实体已死亡或不存在: 惰性清理
        return null;
    }

    /**
     * 召唤新分身
     *
     * <p>流程：
     * 1. 计算生成位置（玩家前方2格）
     * 2. 调用 PersistentGuCultivatorClone.spawn()
     * 3. 从物品NBT恢复数据（如果有）
     * 4. 播放召唤特效
     */
    @Nullable
    private PersistentGuCultivatorClone summonClone(
        Level level,
        Player player,
        ItemStack stack
    ) {
        // 1. 计算生成位置 (玩家前方2格)
        Vec3 spawnPos = player.position().add(player.getLookAngle().scale(2.0));

        // 2. 创建新分身 (使用静态工厂方法)
        PersistentGuCultivatorClone clone = PersistentGuCultivatorClone.spawn(
            (ServerLevel) level,
            player,
            spawnPos
        );

        if (clone == null) {
            return null;
        }

        // 3. 如果物品NBT中有存储的数据,恢复它
        if (stack.hasTag() && stack.getTag().contains("CloneData")) {
            CompoundTag cloneData = stack.getTag().getCompound("CloneData");
            clone.deserializeFromItemNBT(cloneData);
        }

        // 4. 播放召唤特效
        level.playSound(
            null,
            clone.blockPosition(),
            SoundEvents.ENDERMAN_TELEPORT,
            SoundSource.PLAYERS,
            1.0f,
            0.8f
        );

        return clone;
    }

    /**
     * 召回分身
     *
     * <p>流程：
     * 1. 保存分身数据到物品NBT
     * 2. 播放召回特效
     * 3. 移除分身实体
     * 4. 清除临时UUID
     */
    private void recallClone(
        ItemStack stack,
        PersistentGuCultivatorClone clone,
        Player player
    ) {
        // 1. 保存分身数据到物品NBT (使用专用方法)
        CompoundTag cloneData = clone.serializeToItemNBT();
        stack.getOrCreateTag().put("CloneData", cloneData);

        // 2. 播放召回特效
        clone.level().playSound(
            null,
            clone.blockPosition(),
            SoundEvents.ENDERMAN_TELEPORT,
            SoundSource.PLAYERS,
            1.0f,
            1.2f
        );

        // 3. 移除分身实体
        clone.discard();

        // 4. 清除UUID (因为实体已不存在)
        stack.getTag().remove("CloneUUID");
        stack.getTag().remove("DimensionKey");

        // 5. 提示玩家
        player.displayClientMessage(
            Component.literal("§6分身已召回"),
            true
        );
    }

    /**
     * 惰性清理悬挂的分身数据
     *
     * <p>调用时机：
     * - findClone() 返回null时
     * - 玩家再次尝试召唤时
     */
    private void cleanupCloneData(ItemStack stack) {
        if (stack.hasTag()) {
            stack.getTag().remove("CloneUUID");
            stack.getTag().remove("CloneData");
            stack.getTag().remove("DimensionKey");
        }
    }

    /**
     * 所有权校验
     *
     * <p>逻辑：
     * - 首次使用：自动绑定当前玩家
     * - 后续使用：检查UUID是否匹配
     */
    private boolean isOwnedBy(ItemStack stack, Player player) {
        // 如果物品没有绑定所有者,首次使用时绑定
        if (!stack.hasTag() || !stack.getTag().hasUUID("OwnerUUID")) {
            stack.getOrCreateTag().putUUID("OwnerUUID", player.getUUID());
            return true;
        }

        UUID ownerUUID = stack.getTag().getUUID("OwnerUUID");
        return ownerUUID.equals(player.getUUID());
    }

    /**
     * 防止在方块上使用时触发误操作
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        // 所有交互都在 use() 中处理,useOn() 不做特殊处理
        return InteractionResult.PASS;
    }
}
