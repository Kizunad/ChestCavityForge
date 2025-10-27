package net.tigereye.chestcavity.util.mock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.mockito.Mockito;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 模拟 Minecraft 环境的核心工具类
 * 用于单元测试中快速构建 Player、Entity、World 等对象
 */
public class MockMinecraftEnvironment {

    private static final AtomicLong GAME_TIME = new AtomicLong(0);

    /**
     * 创建一个模拟的玩家
     * @param name 玩家名称
     * @return 模拟的 Player 对象
     */
    public static Player createMockPlayer(String name) {
        Player player = mock(Player.class);
        UUID uuid = UUID.nameUUIDFromBytes(name.getBytes());

        // 基础属性
        when(player.getUUID()).thenReturn(uuid);
        when(player.getName()).thenReturn(net.minecraft.network.chat.Component.literal(name));
        when(player.getStringUUID()).thenReturn(uuid.toString());

        // 游戏模式
        when(player.isCreative()).thenReturn(false);
        when(player.isSpectator()).thenReturn(false);

        // 生命值
        when(player.getMaxHealth()).thenReturn(20.0f);
        when(player.getHealth()).thenReturn(20.0f);

        // NBT 数据（关键：用于存储胸腔数据）
        CompoundTag persistentData = new CompoundTag();
        when(player.getPersistentData()).thenReturn(persistentData);

        // Level/World
        ServerLevel level = createMockLevel();
        when(player.level()).thenReturn(level);

        // EntityType (使用 doReturn 避免泛型问题)
        doReturn(EntityType.PLAYER).when(player).getType();

        return player;
    }

    /**
     * 创建模拟的生物实体
     */
    public static LivingEntity createMockLivingEntity(EntityType<?> type) {
        LivingEntity entity = mock(LivingEntity.class);
        UUID uuid = UUID.randomUUID();

        when(entity.getUUID()).thenReturn(uuid);
        doReturn(type).when(entity).getType();
        when(entity.getMaxHealth()).thenReturn(20.0f);
        when(entity.getHealth()).thenReturn(20.0f);

        CompoundTag persistentData = new CompoundTag();
        when(entity.getPersistentData()).thenReturn(persistentData);

        ServerLevel level = createMockLevel();
        when(entity.level()).thenReturn(level);

        return entity;
    }

    /**
     * 创建模拟的世界
     */
    public static ServerLevel createMockLevel() {
        ServerLevel level = mock(ServerLevel.class);

        // 游戏时间（可以手动推进）
        when(level.getGameTime()).thenAnswer(inv -> GAME_TIME.get());

        // 维度
        when(level.dimension()).thenReturn(Level.OVERWORLD);

        return level;
    }

    /**
     * 推进游戏时间（用于测试冷却等时间相关逻辑）
     */
    public static void advanceGameTime(long ticks) {
        GAME_TIME.addAndGet(ticks);
    }

    /**
     * 重置游戏时间
     */
    public static void resetGameTime() {
        GAME_TIME.set(0);
    }

    /**
     * 创建模拟的伤害源
     */
    public static DamageSource createMockDamageSource(ResourceLocation typeId) {
        DamageSource source = mock(DamageSource.class);
        // 简化实现，仅返回 mock 对象
        return source;
    }

    /**
     * 模拟实体受伤
     * @param entity 被攻击的实体
     * @param amount 伤害量
     * @param source 伤害源
     * @return 是否成功受伤
     */
    public static boolean simulateHurt(LivingEntity entity, float amount, DamageSource source) {
        float currentHealth = entity.getHealth();
        float newHealth = Math.max(0, currentHealth - amount);

        // 模拟 hurt 方法被调用
        when(entity.getHealth()).thenReturn(newHealth);
        when(entity.isDeadOrDying()).thenReturn(newHealth <= 0);

        return true;
    }

    /**
     * 模拟物品栈（器官）
     * 注意：简化实现，仅创建基本的 mock 对象
     */
    public static ItemStack createMockOrganStack(ResourceLocation organId, int count) {
        ItemStack stack = mock(ItemStack.class);

        when(stack.getCount()).thenReturn(count);
        when(stack.isEmpty()).thenReturn(false);

        return stack;
    }

    /**
     * 清理所有 Mock 对象（测试结束后调用）
     */
    public static void cleanup() {
        resetGameTime();
        Mockito.framework().clearInlineMocks();
    }
}
