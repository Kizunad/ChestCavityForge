package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.ward;

import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.ward.IncomingThreat;

import java.util.List;

/**
 * 护幕飞剑服务接口
 * <p>
 * 职责：
 * <ol>
 *   <li>生成与维持护幕飞剑数量</li>
 *   <li>监听伤害事件并分配拦截任务</li>
 *   <li>驱动护幕飞剑的状态机（每 tick）</li>
 *   <li>管理耐久消耗与反击逻辑</li>
 * </ol>
 *
 * <h3>实现要求</h3>
 * <ul>
 *   <li>实现应为无状态的工具类或单例</li>
 *   <li>所有方法应线程安全（考虑服务器多线程环境）</li>
 *   <li>避免在 tick 方法中执行耗时操作</li>
 * </ul>
 *
 * <h3>状态机驱动流程</h3>
 * <pre>
 * 每 tick：
 *   for each wardSword:
 *     switch wardState:
 *       ORBIT      → 保持环绕位置
 *       INTERCEPT  → 向拦截点移动，检测超时/成功
 *       COUNTER    → 执行反击，转 RETURN
 *       RETURN     → 返回环绕位，到达后转 ORBIT
 * </pre>
 */
public interface WardSwordService {

    // ====== 生命周期 ======

    /**
     * 确保玩家拥有指定数量的护幕飞剑
     * <p>
     * 流程：
     * <ol>
     *   <li>查询当前护幕飞剑数量</li>
     *   <li>通过 {@link WardTuning#maxSwords(java.util.UUID)} 获取目标数量</li>
     *   <li>若不足，创建新飞剑实例并初始化：
     *     <ul>
     *       <li>设置 {@code wardSword = true}</li>
     *       <li>设置初始耐久（{@link WardTuning#initialWardDurability}）</li>
     *       <li>分配环绕槽位</li>
     *       <li>设置初始状态为 {@link WardState#ORBIT}</li>
     *     </ul>
     *   </li>
     *   <li>若过多，移除多余飞剑（discarded）</li>
     * </ol>
     *
     * 由器官激活逻辑（如剑道阵法）在生成时调用。
     *
     * @param owner 护幕主人（玩家）
     * @return 当前护幕飞剑列表（包含旧有与新建）
     */
    List<FlyingSwordEntity> ensureWardSwords(Player owner);

    /**
     * 清除玩家的所有护幕飞剑
     * <p>
     * 流程：
     * <ol>
     *   <li>查找所有属于该玩家的护幕飞剑</li>
     *   <li>调用 {@link FlyingSwordEntity#discard()} 移除实体</li>
     *   <li>清空内部缓存</li>
     * </ol>
     *
     * 由器官卸载逻辑调用。
     *
     * @param owner 护幕主人（玩家）
     */
    void disposeWardSwords(Player owner);

    // ====== 事件回调 ======

    /**
     * 伤害前置回调：处理来自攻击/投射的威胁
     * <p>
     * 流程：
     * <ol>
     *   <li>解析威胁类型（投射 vs 近战）</li>
     *   <li>调用 {@link net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.ward.InterceptPlanner#plan}
     *       生成拦截查询</li>
     *   <li>遍历所有护幕飞剑，计算到达拦截点的时间 {@code tReach}：
     *     <pre>
     * tReach = max(reaction_delay, distance(S_pos, P*) / vMax)
     *     </pre>
     *   </li>
     *   <li>筛选时间窗内的飞剑（{@code windowMin ≤ tReach ≤ windowMax}）</li>
     *   <li>仲裁确定"拦截令牌"（选择 {@code tReach} 最小的飞剑）</li>
     *   <li>中标飞剑设置：
     *     <ul>
     *       <li>{@code wardState = INTERCEPT}</li>
     *       <li>{@code currentQuery = query}</li>
     *       <li>{@code interceptStartTime = worldTime}</li>
     *     </ul>
     *   </li>
     *   <li>若成功分配拦截任务，伤害置 0（或按穿甲规则缩放）；否则返还伤害</li>
     * </ol>
     *
     * <h3>仲裁规则</h3>
     * 当多个飞剑都在时间窗内时，选择 {@code tReach} 最小的飞剑。
     * 这确保最快到达的飞剑优先拦截，避免资源浪费。
     *
     * <h3>线程安全</h3>
     * 此方法可能在伤害事件线程中调用，实现应确保线程安全。
     *
     * @param threat 威胁信息
     * @return 是否成功分配拦截任务（用于决定是否取消伤害）
     */
    boolean onIncomingThreat(IncomingThreat threat);

    // ====== 驱动循环 ======

    /**
     * 玩家 Tick 驱动（每 tick 调用）
     * <p>
     * 流程：
     * <ol>
     *   <li>获取所有护幕飞剑</li>
     *   <li>遍历每个飞剑，根据 {@link WardState} 执行对应行为：
     *     <ul>
     *       <li>{@link WardState#ORBIT}:
     *         <ul>
     *           <li>计算环绕目标位置：{@code owner.position + orbitSlot}</li>
     *           <li>调用 {@link FlyingSwordEntity#steerTo} 转向目标</li>
     *         </ul>
     *       </li>
     *       <li>{@link WardState#INTERCEPT}:
     *         <ul>
     *           <li>向拦截点 {@code P*} 移动</li>
     *           <li>检测超时（{@code elapsed > 1.0s}）→ 失败，消耗耐久，转 {@link WardState#RETURN}</li>
     *           <li>检测成功（距离拦截点 < 0.5m）→ 成功拦截：
     *             <ul>
     *               <li>消耗拦截耐久（{@link WardTuning#costBlock}）</li>
     *               <li>若攻击者距离 ≤ {@link WardTuning#counterRange}，转 {@link WardState#COUNTER}</li>
     *               <li>否则转 {@link WardState#RETURN}</li>
     *             </ul>
     *           </li>
     *         </ul>
     *       </li>
     *       <li>{@link WardState#COUNTER}:
     *         <ul>
     *           <li>执行反击（投射反弹或近战伤害）</li>
     *           <li>消耗反击耐久（{@link WardTuning#costCounter}）</li>
     *           <li>转 {@link WardState#RETURN}</li>
     *         </ul>
     *       </li>
     *       <li>{@link WardState#RETURN}:
     *         <ul>
     *           <li>向环绕槽位返航</li>
     *           <li>若距离 < 0.5m，转 {@link WardState#ORBIT}</li>
     *         </ul>
     *       </li>
     *     </ul>
     *   </li>
     *   <li>检测护幕耐久耗尽，移除实体</li>
     * </ol>
     *
     * @param owner 护幕主人（玩家）
     */
    void tick(Player owner);

    /**
     * 单个护幕飞剑 Tick 驱动（每 tick 调用）
     * <p>
     * 由 {@link FlyingSwordEntity#tickWardBehavior} 调用，处理单个飞剑的状态机逻辑。
     * <p>
     * 流程：
     * <ol>
     *   <li>根据 {@link WardState} 执行对应行为（ORBIT/INTERCEPT/COUNTER/RETURN）</li>
     *   <li>检测护幕耐久耗尽，移除实体</li>
     * </ol>
     *
     * @param sword 护幕飞剑实体
     * @param owner 护幕主人（玩家）
     */
    void tickWardSword(FlyingSwordEntity sword, Player owner);

    // ====== 工具方法 ======

    /**
     * 获取玩家的所有护幕飞剑
     * <p>
     * 此方法应过滤出所有满足以下条件的飞剑：
     * <ul>
     *   <li>{@link FlyingSwordEntity#isWardSword()} == true</li>
     *   <li>{@link FlyingSwordEntity#getOwner()} == owner</li>
     *   <li>实体未被移除（{@code !isRemoved()}）</li>
     * </ul>
     *
     * @param owner 护幕主人（玩家）
     * @return 护幕飞剑列表（可能为空）
     */
    List<FlyingSwordEntity> getWardSwords(Player owner);

    /**
     * 统计玩家当前护幕数量
     *
     * @param owner 护幕主人（玩家）
     * @return 护幕数量
     */
    int getWardCount(Player owner);

    /**
     * 检查玩家是否激活护幕
     *
     * @param owner 护幕主人（玩家）
     * @return 如果有至少一个护幕飞剑返回 true
     */
    boolean hasWardSwords(Player owner);
}
