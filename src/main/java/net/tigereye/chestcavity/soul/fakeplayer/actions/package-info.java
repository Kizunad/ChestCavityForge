/**
 * FakePlayer Actions 集中地
 * <p>
 * 本包用于承载与 Soul 假人（FakePlayer）相关的“动作/行为”实现，
 * 例如：战斗指令、交互脚本、姿态切换、临时状态切换等。其目的：
 * </p>
 * <ul>
 *   <li>给 FakePlayer 的可执行动作提供一个清晰的归属位置；</li>
 *   <li>避免分散在各模块造成的耦合与类加载顺序问题；</li>
 *   <li>便于后续为动作统一接入调度、冷却与网络同步（参考 DelayedTaskScheduler / MultiCooldown）；</li>
 *   <li>与 {@link net.tigereye.chestcavity.soul.fakeplayer.SoulFakePlayerSpawner}、
 *       {@code SoulPlayer} 同模块，侧重服务端执行；客户端仅负责可视化与输入转发。</li>
 * </ul>
 *
 * 约定：
 * <ul>
 *   <li>仅放置 FakePlayer 专用的动作类与其辅助类；</li>
 *   <li>命名以 Action 结尾（如 MoveToAction、AttackTargetAction），
 *       后续若需要统一接口，可在本包内新增最小依赖的动作接口；</li>
 *   <li>如需网络可视化/提示，请复用既有 NetworkHandler 的 payload，或在 compat 层新增轻量载荷。</li>
 * </ul>
 */
package net.tigereye.chestcavity.soul.fakeplayer.actions;

