/**
 * 离线存储： - {@code SoulOfflineStore} 在 Owner 离线时暂存分魂快照，玩家下次登录时归并回容器； - 使用世界存档 {@code SavedData}
 * 进行持久化，键为 Owner UUID → (Soul UUID → NBT)。
 */
package net.tigereye.chestcavity.soul.storage;
