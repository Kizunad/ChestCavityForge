package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.component.CustomData;
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
     *
     * <p>P1修复: 跨维度清理
     * - 在召唤新分身前,必须先清理旧分身 (防止资源泄漏)
     */
    private InteractionResultHolder<ItemStack> handleSummonOrRecall(
        Level level,
        Player player,
        ItemStack stack
    ) {
        // 1. 检查是否已有分身存在 (可能在不同维度)
        PersistentGuCultivatorClone existingClone = findClone(level, stack);

        if (existingClone != null) {
            // 存在 -> 召回
            recallClone(stack, existingClone, player);
            return InteractionResultHolder.success(stack);
        } else {
            // 不存在或已死亡 -> 准备召唤新分身

            // P1修复: 检查是否有悬挂的UUID (分身已死亡或在不可访问的维度)
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            boolean hadOldClone = !tag.isEmpty() && tag.hasUUID("CloneUUID");

            if (hadOldClone) {
                // 有旧UUID但findClone返回null: 可能在其他维度或已死亡
                // 尝试跨维度查找并强制召回
                PersistentGuCultivatorClone crossDimensionClone = findAndRecallCrossDimensionClone(level, player, stack);

                if (crossDimensionClone != null) {
                    // 成功召回跨维度分身
                    player.displayClientMessage(
                        Component.literal("§e已自动召回其他维度的分身"),
                        false
                    );
                } else {
                    // 实体已死亡或无法访问: 清理UUID
                    cleanupCloneData(stack);
                    player.displayClientMessage(
                        Component.literal("§e旧分身已失联,数据已清理"),
                        false
                    );
                }
            }

            // 召唤新分身
            PersistentGuCultivatorClone newClone = summonClone(level, player, stack);

            if (newClone != null) {
                // 保存UUID到物品NBT
                CustomData.update(DataComponents.CUSTOM_DATA, stack, newTag -> {
                    newTag.putUUID("CloneUUID", newClone.getUUID());
                    newTag.putString("DimensionKey", level.dimension().location().toString());
                });

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
     * - 维度一致性：跨维度时查找原维度的实体
     * - 实体存活性：已死亡时清理UUID
     *
     * @return 存活的分身实体（可能在不同维度），或null
     */
    @Nullable
    private PersistentGuCultivatorClone findClone(Level level, ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        if (tag.isEmpty() || !tag.hasUUID("CloneUUID")) {
            return null;
        }

        UUID cloneUUID = tag.getUUID("CloneUUID");
        ServerLevel targetLevel = null;

        // 检查维度一致性
        if (tag.contains("DimensionKey")) {
            String savedDim = tag.getString("DimensionKey");
            String currentDim = level.dimension().location().toString();

            if (!savedDim.equals(currentDim)) {
                // 跨维度场景: 尝试访问原维度
                if (level instanceof ServerLevel serverLevel) {
                    // 尝试获取保存的维度
                    var dimensionKey = net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        net.minecraft.resources.ResourceLocation.parse(savedDim)
                    );
                    targetLevel = serverLevel.getServer().getLevel(dimensionKey);
                }

                // 如果无法访问原维度,返回null (调用者需要清理)
                if (targetLevel == null) {
                    return null;
                }
            } else {
                // 同维度
                targetLevel = (ServerLevel) level;
            }
        } else {
            // 没有维度记录,假设同维度
            if (!(level instanceof ServerLevel)) {
                return null;
            }
            targetLevel = (ServerLevel) level;
        }

        // 查找实体
        Entity entity = targetLevel.getEntity(cloneUUID);

        if (entity instanceof PersistentGuCultivatorClone clone && clone.isAlive()) {
            return clone;
        }

        // 实体已死亡或不存在
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
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.isEmpty() && tag.contains("CloneData")) {
            CompoundTag cloneData = tag.getCompound("CloneData");
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
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.put("CloneData", cloneData);
        });

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
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove("CloneUUID");
            tag.remove("DimensionKey");
        });

        // 5. 提示玩家
        player.displayClientMessage(
            Component.literal("§6分身已召回"),
            true
        );
    }

    /**
     * 跨维度查找并强制召回分身 (P1修复: 防止资源泄漏)
     *
     * <p>场景: 玩家在维度A召唤分身,切换到维度B后再次召唤
     * <p>行为: 主动召回维度A的分身 (保存数据),而不是留下它继续运行
     *
     * @return 被召回的分身,如果成功召回; null如果无法找到或召回
     */
    @Nullable
    private PersistentGuCultivatorClone findAndRecallCrossDimensionClone(
        Level currentLevel,
        Player player,
        ItemStack stack
    ) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        if (tag.isEmpty() || !tag.hasUUID("CloneUUID")) {
            return null;
        }

        UUID cloneUUID = tag.getUUID("CloneUUID");
        String savedDim = tag.contains("DimensionKey") ? tag.getString("DimensionKey") : null;

        if (savedDim == null) {
            return null;
        }

        // 尝试访问保存的维度
        if (!(currentLevel instanceof ServerLevel serverLevel)) {
            return null;
        }

        try {
            var dimensionKey = net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.parse(savedDim)
            );

            ServerLevel targetLevel = serverLevel.getServer().getLevel(dimensionKey);
            if (targetLevel == null) {
                return null;
            }

            // 查找实体
            Entity entity = targetLevel.getEntity(cloneUUID);
            if (!(entity instanceof PersistentGuCultivatorClone clone) || !clone.isAlive()) {
                return null;
            }

            // 找到了! 强制召回 (保存数据并移除实体)
            CompoundTag cloneData = clone.serializeToItemNBT();
            CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> {
                t.put("CloneData", cloneData);
            });

            // 移除实体
            clone.discard();

            // 清除UUID (即将召唤新分身)
            CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> {
                t.remove("CloneUUID");
                t.remove("DimensionKey");
            });

            return clone;

        } catch (Exception e) {
            // 维度解析失败或其他异常
            return null;
        }
    }

    /**
     * 惰性清理悬挂的分身数据
     *
     * <p>调用时机：
     * - findClone() 返回null时
     * - 玩家再次尝试召唤时
     */
    private void cleanupCloneData(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove("CloneUUID");
            tag.remove("CloneData");
            tag.remove("DimensionKey");
        });
    }

    /**
     * 所有权校验
     *
     * <p>逻辑：
     * - 首次使用：自动绑定当前玩家
     * - 后续使用：检查UUID是否匹配
     */
    private boolean isOwnedBy(ItemStack stack, Player player) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        // 如果物品没有绑定所有者,首次使用时绑定
        if (tag.isEmpty() || !tag.hasUUID("OwnerUUID")) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> {
                t.putUUID("OwnerUUID", player.getUUID());
            });
            return true;
        }

        UUID ownerUUID = tag.getUUID("OwnerUUID");
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
