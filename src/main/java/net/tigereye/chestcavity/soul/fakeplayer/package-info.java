/**
 * 灵魂假人（SoulPlayer）运行时： - 由 {@code SoulFakePlayerSpawner} 统一生成/移除/保存，校验 Owner 权限，并广播 PlayerInfo
 * 数据包用于客户端渲染； - {@code SoulPlayer} 基于 {@code FakePlayer} 的最小可用实现，支持受击结算、死亡回调与能力启动位； - {@code
 * SoulFakePlayerEvents} 处理登录/登出/停服时的外化、回写与清理流程。
 */
package net.tigereye.chestcavity.soul.fakeplayer;
