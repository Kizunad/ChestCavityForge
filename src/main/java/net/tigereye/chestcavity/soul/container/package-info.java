/**
 * 玩家灵魂容器：
 * - 提供多槽位的 {@link net.tigereye.chestcavity.soul.profile.SoulProfile} 管理（增删改查、激活状态）。
 * - 负责将容器内容序列化/反序列化到玩家附件（Capabilities/Attachments）。
 * - 避免在客户端产生副作用，包含快照捕获仅在服务端进行。
 */
package net.tigereye.chestcavity.soul.container;

