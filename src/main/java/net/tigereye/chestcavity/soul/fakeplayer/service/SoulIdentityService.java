package net.tigereye.chestcavity.soul.fakeplayer.service;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 提供灵魂身份（GameProfile）缓存的统一入口。
 *
 * <p>该服务负责：
 * <ul>
 *     <li>创建或复用 Soul 专属的 GameProfile；</li>
 *     <li>更新/查询显示名与皮肤属性；</li>
 *     <li>在不同系统间共享缓存并维持线程安全。</li>
 * </ul>
 */
public interface SoulIdentityService {

    /**
     * 确保 soul 拥有 identity；若尚未存在则基于来源 profile 初始化。
     *
     * @param soulId        目标 soul UUID（profile UUID）
     * @param sourceProfile 默认参考的 GameProfile
     * @param forceDerivedId 是否强制派生独立实体 ID
     * @return identity GameProfile（缓存内实例）
     */
    GameProfile ensureIdentity(UUID soulId, GameProfile sourceProfile, boolean forceDerivedId);

    /**
     * 根据新的显示名更新缓存。若缓存缺失则保持为空。
     */
    void updateIdentityName(UUID soulId, String newName);

    /**
     * 预先写入 identity 名称（不存在则创建，存在则覆盖名字）。
     */
    void seedIdentityName(UUID soulId, String newName);

    /**
     * 检查名称是否已被任意 identity 使用（忽略大小写）。
     */
    boolean isIdentityNameInUse(String candidate);

    /**
     * 判断 soul 是否已有专属 identity。
     */
    boolean hasIdentity(UUID soulId);

    /**
     * 返回缓存中的 identity；若不存在则返回 {@code fallback}。
     */
    GameProfile getIdentityOrDefault(UUID soulId, GameProfile fallback);

    /**
     * 返回缓存中的 identity；若不存在则返回 {@code null}。
     */
    @Nullable
    GameProfile getIdentity(UUID soulId);

    /**
     * 将 identity 存入缓存；传入 {@code null} 时视为移除。
     */
    void putIdentity(UUID soulId, @Nullable GameProfile profile);

    /**
     * 移除指定 soul 的 identity。
     */
    void removeIdentity(UUID soulId);

    /**
     * 遍历当前缓存，用于序列化/诊断。
     */
    void forEachIdentity(BiConsumer<UUID, GameProfile> consumer);

    /**
     * 清空全部缓存（用于服务器停止或测试环境）。
     */
    void clear();

    /**
     * 克隆给定 profile（复制属性）。
     */
    GameProfile cloneProfile(GameProfile source);

    /**
     * 将 {@code from} 的属性复制到 {@code to}。
     */
    void copyProperties(GameProfile from, GameProfile to);
}
