/**
 * 灵魂系统（Soul System）总览。
 *
 * <p>本模块提供基于玩家快照与 FakePlayer 的“灵魂分身”能力：</p>
 * - 持久化：{@code container} 维护每位玩家的多个 {@code SoulProfile} 槽位，并通过 NBT 序列化保存到玩家附件。
 * - 运行时：{@code fakeplayer} 负责在服务端生成/移除 SoulPlayer 实体，广播 PlayerInfo 数据包以保证客户端渲染。
 * - 快照：{@code profile} 捕获/恢复背包、原版属性、药水效果与位置，按需回写到玩家或 SoulPlayer。
 * - 协调：{@code command} 暴露临时调试指令，{@code engine} 提供危险功能的开关，{@code storage} 处理离线快照归并。
 * - 工具：{@code util} 聚合日志与容器标脏/离线写入等共用逻辑。
 *
 * <p>关键约束：</p>
 * - 仅在服务端执行生成/传送/保存等具有副作用的操作；客户端侧仅保持最小可用状态。
 * - 属性回写仅修改 BaseValue，不同步修饰符（Modifiers），避免跨模组状态不一致。
 * - Owner 与 Soul 的关联与权限校验统一由 {@code SoulFakePlayerSpawner} 负责，指令与外部调用均应经由该协调器。</p>
 */
package net.tigereye.chestcavity.soul;

