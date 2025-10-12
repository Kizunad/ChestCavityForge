/**
 * 灵魂系统（Soul System）总览。
 *
 * <p>本模块提供基于玩家快照、FakePlayer 与灵魂运行时调度的“灵魂分身”能力，并将整套功能拆分为若干子域：</p>
 * <ul>
 *     <li><strong>持久化：</strong>{@code container} 维护每位玩家的多个 {@link net.tigereye.chestcavity.soul.profile.SoulProfile}
 *     槽位，负责脏标记、自动保存与附着到玩家实体的生命周期。</li>
 *     <li><strong>快照：</strong>{@code profile} 捕获与恢复背包、原版属性、药水效果、位置及扩展能力，并支持与
 *     {@code capability} 管线协同工作。</li>
 *     <li><strong>运行时：</strong>{@code fakeplayer} 负责在服务端生成/移除 SoulPlayer，转发
 *     PlayerInfo 数据包并封装所有权校验；{@code runtime} 则提供心跳调度与被动技能处理。</li>
 *     <li><strong>行为：</strong>{@code ai} 与 {@code navigation} 封装灵魂的战斗与移动指令，{@code command}
 *     暴露临时调试命令，{@code engine} 提供危险功能的开关。</li>
 *     <li><strong>数据管理：</strong>{@code storage} 负责离线快照归并，{@code util} 聚合日志、消息广播、
 *     Profile 操作等跨域工具。</li>
 * </ul>
 *
 * <p>关键约束：</p>
 * <ul>
 *     <li>仅在服务端执行生成、传送、保存等具有副作用的操作；客户端仅维持最小可视状态。</li>
 *     <li>属性回写仅修改 BaseValue，不直接同步修饰符（Modifier），以避免跨模组状态不一致。</li>
 *     <li>Owner 与 Soul 的关联、权限校验、假人合法性追踪统一由
 *     {@link net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner} 负责。</li>
 *     <li>所有新增功能须记录详细日志（Info/Debug 分级），确保线上排障可追溯。</li>
 * </ul>
 */
package net.tigereye.chestcavity.soul;

