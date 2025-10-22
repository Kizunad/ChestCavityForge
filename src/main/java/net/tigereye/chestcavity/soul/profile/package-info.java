/**
 * 快照模型： - 背包：{@code InventorySnapshot} 保存 36+4+1 原版槽位； - 属性：{@code PlayerStatsSnapshot}
 * 保存经验、饥饿三件套、生命/吸收与可同步属性 BaseValue； - 效果：{@code PlayerEffectsSnapshot} 保存并重放药水效果； - 位置：{@code
 * PlayerPositionSnapshot} 保存维度/坐标/朝向，带安全回退坐标； - 存档：{@code SoulProfile} 聚合上述快照并提供捕获/恢复/NBT 序列化能力。
 */
package net.tigereye.chestcavity.soul.profile;
