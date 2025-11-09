# 护幕系统 - 接口与数据结构设计

---

## 1️⃣ 实体扩展 (`FlyingSwordEntity.java`)

### 新增字段

```java
public class FlyingSwordEntity extends PathfinderMob implements OwnableEntity {

    // —— 护幕运行期字段 ——

    /** 是否为护幕飞剑 */
    private boolean wardSword = false;

    /** 不破坏方块 */
    private boolean noBlockBreak = true;

    /** 是否可被召回（护幕飞剑固定为 false） */
    private boolean recallable = false;

    /** 护幕当前耐久值 */
    private double wardDurability = 0.0;

    /** 护幕状态机 (ORBIT/INTERCEPT/COUNTER/RETURN) */
    private WardState wardState = WardState.ORBIT;

    /** 环绕槽位（相对主人的相对位置） */
    @Nullable
    private Vec3 orbitSlot = null;

    /** 当前拦截任务 */
    @Nullable
    private InterceptQuery currentQuery = null;

    /** 最后一次进入 INTERCEPT 的时刻 */
    private long interceptStartTime = 0L;
}
```

### 新增访问器

```java
public class FlyingSwordEntity extends PathfinderMob implements OwnableEntity {

    // —— 护幕状态查询 ——

    /**
     * 是否为护幕飞剑
     */
    public boolean isWardSword() {
        return wardSword;
    }

    /**
     * 设置护幕标志（生成时调用）
     */
    public void setWardSword(boolean value) {
        this.wardSword = value;
        if (value) {
            this.setRecallable(false);
            this.noBlockBreak = true;
        }
    }

    /**
     * 获取护幕耐久
     */
    public double getWardDurability() {
        return wardDurability;
    }

    /**
     * 设置护幕耐久
     */
    public void setWardDurability(double durability) {
        this.wardDurability = Math.max(0.0, durability);
    }

    /**
     * 消耗护幕耐久
     */
    public void consumeWardDurability(int amount) {
        wardDurability = Math.max(0.0, wardDurability - amount);
        if (wardDurability <= 0.0 && this.wardSword) {
            // 护幕耐尽 → 消散
            this.discard();
        }
    }

    /**
     * 获取护幕状态
     */
    public WardState getWardState() {
        return wardState;
    }

    /**
     * 设置护幕状态
     */
    public void setWardState(WardState state) {
        if (state == null) state = WardState.ORBIT;
        if (this.wardState == state) return;

        this.wardState = state;

        // 进入 INTERCEPT 时记录时刻
        if (state == WardState.INTERCEPT) {
            this.interceptStartTime = this.level().getGameTime();
        }
    }

    /**
     * 获取环绕槽位（相对主人的位置）
     */
    @Nullable
    public Vec3 getOrbitSlot() {
        return orbitSlot;
    }

    /**
     * 设置环绕槽位
     */
    public void setOrbitSlot(@Nullable Vec3 slot) {
        this.orbitSlot = slot;
    }

    /**
     * 获取当前拦截任务
     */
    @Nullable
    public InterceptQuery getCurrentQuery() {
        return currentQuery;
    }

    /**
     * 设置当前拦截任务
     */
    public void setCurrentQuery(@Nullable InterceptQuery query) {
        this.currentQuery = query;
    }

    // —— 护幕控制钩子（签名仅供示例，不含实现） ——

    /**
     * 护幕行为驱动（每 tick 调用）
     *
     * 流程：
     * 1. 根据 wardState 决定是否继续或转换状态
     * 2. 计算目标位置（ORBIT 环绕槽 / INTERCEPT 拦截点 / RETURN 回环）
     * 3. 调用 steerTo() 驱动位移
     * 4. 检测时间窗或成功判定，触发状态转换
     *
     * @param owner 主人
     * @param tuning 参数供给接口
     */
    public void tickWardBehavior(Player owner, WardTuning tuning) {
        // 仅签名，具体实现在 WardSwordService 或专属系统中
    }

    /**
     * 转向目标点（运动执行）
     *
     * @param target 目标位置
     * @param aMax 最大加速度
     * @param vMax 最大速度
     */
    public void steerTo(Vec3 target, double aMax, double vMax) {
        // 仅签名，具体实现由 MovementSystem 或护幕系统调用
    }

    /**
     * 检测是否已回到环绕位（用于 RETURN 状态）
     *
     * @return 若距环绕槽位 < 0.5m 则返回 true
     */
    public boolean backToOrbitSlot() {
        if (orbitSlot == null) return true; // 无槽位视为已回

        LivingEntity owner = getOwner();
        if (owner == null) return true;

        Vec3 absoluteSlot = owner.position().add(orbitSlot);
        return this.position().distanceTo(absoluteSlot) < 0.5;
    }
}
```

---

## 2️⃣ 枚举与数据类

### WardState.java

```java
package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.ward;

/**
 * 护幕飞剑的状态机
 */
public enum WardState {
    /** 环绕主人（待命态） */
    ORBIT("orbit", "环绕"),

    /** 向拦截点移动（可达窗口内，2-20 tick） */
    INTERCEPT("intercept", "拦截"),

    /** 反击完成（仅当距离 ≤ 5m 时触发） */
    COUNTER("counter", "反击"),

    /** 返回环绕位置 */
    RETURN("return", "返回");

    private final String id;
    private final String displayName;

    WardState(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static WardState fromId(String id) {
        for (WardState s : values()) {
            if (s.id.equals(id)) return s;
        }
        return ORBIT;
    }
}
```

### WardThreat.java (威胁模型)

```java
package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.ward;

/**
 * 完整的威胁描述（近战或投射）
 */
public record IncomingThreat(
        /** 攻击发起者 */
        Entity attacker,

        /** 预期目标（通常为玩家） */
        Entity target,

        /** 预期命中点（用于投射预测或近战线段） */
        @Nullable Vec3 targetHitPoint,

        /** 投射物当前位置（null 表示近战） */
        @Nullable Vec3 projPos,

        /** 投射物速度（null 表示近战或未知） */
        @Nullable Vec3 projVel,

        /** 事件发生的世界时刻（tick） */
        long worldTime
) {
    /**
     * 判定威胁类型
     */
    public boolean isProjectile() {
        return projPos != null && projVel != null;
    }

    public boolean isMelee() {
        return projPos == null && projVel == null;
    }
}
```

### InterceptQuery.java (规划结果)

```java
package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.ward;

/**
 * 拦截规划的结果
 */
public record InterceptQuery(
        /** 预计命中轨迹上的拦截点 P* */
        Vec3 interceptPoint,

        /** 投射物到达 P* 的预计时刻（秒） */
        double tImpact,

        /** 原始威胁信息（用于后续验证） */
        IncomingThreat threat
) {
    /**
     * 当前世界时刻下，拦截点距离（用于计时） */
    public long getCreatedTick(Level level) {
        return level.getGameTime();
    }
}
```

---

## 3️⃣ 参数接口

### WardTuning.java

```java
package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.ward;

import net.minecraft.world.entity.player.Player;
import java.util.UUID;

/**
 * 护幕数值供给接口
 *
 * 实现应通过以下来源动态计算：
 * - 道痕等级 (Sword Trail Level)
 * - 流派经验 (Sect Experience)
 * - 玩家当前 debuff 与 buff
 * - 全局配置参数
 */
public interface WardTuning {

    // —— 护幕数量与配置 ——

    /**
     * 最大护幕飞剑数
     * 公式: N = clamp(1 + floor(sqrt(道痕/100)) + floor(经验/1000), 1, max)
     */
    int maxSwords(UUID owner);

    /**
     * 护幕环绕半径
     * 公式: r = 2.6 + 0.4 * N
     */
    double orbitRadius(UUID owner, int currentSwordCount);

    // —— 运动性能 ——

    /**
     * 最大速度 (m/s)
     * 公式: vMax = 6.0 + 0.02 * 道痕 + 0.001 * 经验
     */
    double vMax(UUID owner);

    /**
     * 最大加速度 (m/s²)
     * 建议: aMax = 40.0 (常数或可调)
     */
    double aMax(UUID owner);

    /**
     * 反应延迟 (秒)
     * 公式: reaction = clamp(0.06 - 0.00005 * 经验, 0.02, 0.06)
     */
    double reactionDelay(UUID owner);

    // —— 反击条件 ——

    /**
     * 触发反击的最大距离 (米)
     * 默认: 5.0 m
     */
    double counterRange();

    // —— 时间窗口 ——

    /**
     * 最小可达时间窗 (秒)
     * 默认: 0.1 s
     */
    double windowMin();

    /**
     * 最大可达时间窗 (秒)
     * 默认: 1.0 s
     */
    double windowMax();

    // —— 耐久消耗系数 ——

    /**
     * 成功拦截的耐久消耗
     * 公式: costBlock = round(8 * (1 - R)), R = exp / (exp + 2000)
     */
    int costBlock(UUID owner);

    /**
     * 成功反击的耐久消耗
     * 公式: costCounter = round(10 * (1 - R))
     */
    int costCounter(UUID owner);

    /**
     * 失败尝试的耐久消耗
     * 公式: costFail = round(2 * (1 - 0.5*R))
     */
    int costFail(UUID owner);

    // —— 反击伤害 ——

    /**
     * 反击伤害基线
     * 公式: D_counter = base(5) + 0.05 * 道痕 + 0.01 * 经验
     */
    double counterDamage(UUID owner);

    // —— 初始耐久 ——

    /**
     * 护幕飞剑的初始耐久
     * 公式: Dur0 = 60 + 0.3 * 道痕 + 0.1 * 经验
     */
    double initialWardDurability(UUID owner);
}
```

---

## 4️⃣ 服务接口

### WardSwordService.java

```java
package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.ward;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import java.util.List;

/**
 * 护幕飞剑服务接口
 *
 * 职责：
 * 1. 生成与维持护幕飞剑数量
 * 2. 监听伤害事件并分配拦截任务
 * 3. 驱动护幕飞剑的状态机（每 tick）
 * 4. 管理耐久消耗与反击逻辑
 *
 * 实现应为无状态的工具类或单例。
 */
public interface WardSwordService {

    // —— 生命周期 ——

    /**
     * 确保玩家拥有指定数量的护幕飞剑
     *
     * 若不足，则创建新实例；若过多，则移除。
     * 由器官激活逻辑（如剑道阵法）在生成时调用。
     *
     * @param owner 护幕主人
     * @return 当前护幕飞剑列表（包含旧有与新建）
     */
    List<FlyingSwordEntity> ensureWardSwords(Player owner);

    /**
     * 清除玩家的所有护幕飞剑
     *
     * 由器官卸载逻辑调用。
     *
     * @param owner 护幕主人
     */
    void disposeWardSwords(Player owner);

    // —— 事件回调 ——

    /**
     * 伤害前置回调：处理来自攻击/投射的威胁
     *
     * 流程：
     * 1. 解析威胁类型（投射/近战）
     * 2. 调用 InterceptPlanner.plan() 生成拦截查询
     * 3. 对所有护幕飞剑计算可达时间 tReach
     * 4. 筛选时间窗内的飞剑，以仲裁确定"拦截令牌"
     * 5. 中标飞剑设置 wardState = INTERCEPT + currentQuery
     * 6. 若成功拦截，伤害置 0；否则返还伤害
     *
     * @param threat 威胁信息
     */
    void onIncomingThreat(IncomingThreat threat);

    // —— 驱动循环 ——

    /**
     * 玩家 Tick 驱动（每 tick 调用）
     *
     * 流程：
     * 1. 遍历所有护幕飞剑
     * 2. 根据 wardState 调用相应的行为：
     *    - ORBIT: 保持环绕位置
     *    - INTERCEPT: 向 P* 移动，检测超时或成功
     *    - COUNTER: 执行反击（可选），随后转 RETURN
     *    - RETURN: 向环绕位返航，到达后转 ORBIT
     * 3. 检测护幕耐尽，移除实体
     *
     * @param owner 护幕主人
     */
    void tick(Player owner);

    // —— 工具方法 ——

    /**
     * 获取玩家的所有护幕飞剑
     */
    List<FlyingSwordEntity> getWardSwords(Player owner);

    /**
     * 统计玩家当前护幕数量
     */
    int getWardCount(Player owner);

    /**
     * 检查玩家是否激活护幕
     */
    boolean hasWardSwords(Player owner);
}
```

---

## 5️⃣ 规划器

### InterceptPlanner.java

```java
package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.ward;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 拦截规划算法（纯函数式，无状态）
 *
 * 职责：
 * - 从威胁信息推导出拦截点 P* 与预计命中时刻
 * - 验证是否在时间窗口 [windowMin, windowMax] 内
 * - 不包含具体的寻路或避让逻辑（那部分由运动系统处理）
 */
public final class InterceptPlanner {

    private InterceptPlanner() {}

    // —— 主规划方法 ——

    /**
     * 生成拦截查询
     *
     * 算法：
     *
     * 1. 若威胁为投射物：
     *    a. 根据 projPos, projVel, gravity 预测与玩家 AABB 的相交点 I
     *    b. 计算相交时刻 tImpact
     *    c. P* = I - 0.3 * normalize(projVel) （提前 0.3m）
     *
     * 2. 若威胁为近战：
     *    a. 构造攻击线段：从 attacker.position → target.position
     *    b. 取命中前最近点 I，计算线性外推时刻 tImpact
     *    c. P* = I 或 I + 0.3m（向玩家方向）
     *
     * 3. 若 tImpact > windowMax() 或算法无解 → 返回 null
     *
     * @param threat 威胁信息
     * @param owner 护幕主人（玩家）
     * @param tuning 参数接口
     * @return 拦截查询，或 null 若无法在窗口内拦截
     */
    public static @Nullable InterceptQuery plan(
        IncomingThreat threat,
        Player owner,
        WardTuning tuning
    ) {
        // 仅签名，具体实现为骨架阶段的任务
        return null;
    }

    // —— 辅助方法 ——

    /**
     * 计算飞剑到达拦截点所需的时间
     *
     * 公式：
     * tReach = max(reaction_delay, distance / vMax)
     *
     * @param sword 飞剑实体
     * @param pStar 拦截点
     * @param tuning 参数接口
     * @return 所需时间（秒）
     */
    public static double timeToReach(
        FlyingSwordEntity sword,
        Vec3 pStar,
        WardTuning tuning
    ) {
        double distance = sword.position().distanceTo(pStar);
        double vMax = tuning.vMax(sword.getOwner().getUUID());
        double reaction = tuning.reactionDelay(sword.getOwner().getUUID());

        double tByDistance = distance / Math.max(vMax, 0.1);
        return Math.max(reaction, tByDistance);
    }

    /**
     * 预测投射物与目标 AABB 的相交（简化）
     *
     * 仅签名，具体实现为后续开发任务
     */
    private static @Nullable Vec3 predictProjectileHitPoint(
        Vec3 projPos,
        Vec3 projVel,
        Player target,
        double gravity
    ) {
        // 简化伪代码：
        // 1. 迭代预测 0.1-1.0s 内的位置
        // 2. 检测与 target.getBoundingBox() 的相交
        // 3. 返回第一个相交点或 null
        return null;
    }

    /**
     * 预测近战攻击线段与目标的相交
     *
     * 仅签名，具体实现为后续开发任务
     */
    private static @Nullable Vec3 predictMeleeHitPoint(
        Entity attacker,
        Player target,
        double reach
    ) {
        // 简化伪代码：
        // 1. 构造线段：attacker.getEyePosition() → target.position()
        // 2. 距离 target AABB 最近的点为 I
        // 3. 返回 I
        return null;
    }
}
```

---

## 6️⃣ 配置常量

### WardConfig.java

```java
package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.ward;

/**
 * 护幕系统的全局常量与默认配置
 */
public final class WardConfig {

    private WardConfig() {}

    // —— 时间窗口 ——
    public static final double WINDOW_MIN = 0.1;  // 秒
    public static final double WINDOW_MAX = 1.0;  // 秒

    // —— 反击条件 ——
    public static final double COUNTER_RANGE = 5.0;  // 米

    // —— 运动默认值 ——
    public static final double SPEED_BASE = 6.0;     // m/s
    public static final double ACCEL_BASE = 40.0;    // m/s²

    // —— 耐久默认值 ——
    public static final int DURABILITY_BLOCK = 8;      // 拦截消耗
    public static final int DURABILITY_COUNTER = 10;   // 反击消耗
    public static final int DURABILITY_FAIL = 2;       // 失败消耗

    // —— 经验衰减参数 ——
    public static final double EXP_DECAY_BASE = 2000.0;  // R = exp / (exp + 2000)
    public static final double EXP_DECAY_MAX = 0.6;      // R 上限

    // —— 初始耐久参数 ——
    public static final double INITIAL_DUR_BASE = 60.0;
    public static final double INITIAL_DUR_TRAIL = 0.3;      // 每点道痕
    public static final double INITIAL_DUR_EXP = 0.1;        // 每点经验

    // —— 环绕参数 ——
    public static final double ORBIT_RADIUS_BASE = 2.6;
    public static final double ORBIT_RADIUS_PER_SWORD = 0.4;

    // —— 反应延迟参数 ——
    public static final double REACTION_BASE = 0.06;        // 秒
    public static final double REACTION_EXP_COEF = 0.00005;  // 每点经验
    public static final double REACTION_MIN = 0.02;
    public static final double REACTION_MAX = 0.06;

    // —— 最大护幕数 ——
    public static final int MAX_WARDS = 4;
}
```

---

## 7️⃣ 集成点标记

### 在 `FlyingSwordEntity.tickServer()` 中

```java
private void tickServer() {
    LivingEntity owner = getOwner();
    if (owner == null || !owner.isAlive()) {
        this.discard();
        return;
    }

    // ... 现有逻辑 ...

    // —— 新增：护幕驱动（若为护幕飞剑） ——
    if (this.wardSword && owner instanceof Player player) {
        WardSwordService service = /* 获取服务实例 */;
        WardTuning tuning = /* 获取参数接口 */;
        this.tickWardBehavior(player, tuning);
    }

    // ... 继续现有逻辑 ...
}
```

### 在伤害处理中（待集成）

```java
// 伤害前置钩子（由外部系统转发）
void onIncomingThreat(IncomingThreat threat) {
    if (/* 主人有护幕飞剑 */) {
        WardSwordService service = /* 获取服务 */;
        service.onIncomingThreat(threat);

        if (/* 拦截成功 */) {
            threat.setHandled(true);
            threat.setDamage(0);  // 或按穿甲规则缩放
        }
    }
}
```

---

## 📝 设计检查清单

- [x] WardState 枚举定义完整
- [x] IncomingThreat record 包含必要字段
- [x] InterceptQuery record 包含结果数据
- [x] WardTuning 接口涵盖所有参数
- [x] WardSwordService 接口方法签名完整
- [x] FlyingSwordEntity 新增字段与访问器正确
- [x] InterceptPlanner 方法签名正确
- [x] WardConfig 常量值合理

---

**设计版本**: v1.0
**最后更新**: 2025年
