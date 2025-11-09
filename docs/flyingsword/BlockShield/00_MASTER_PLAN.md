# 剑幕·反击之幕 (BlockShield) - 实现计划

> **项目目标**：基于现有飞剑系统架构，在不新增实体类型前提下，实现**真拦截、真反击**的护幕 AI 与接口。

---

## 📋 设计边界 (Scope)

### ✅ 包含内容

1. **接口与骨架** - 完整的类设计和方法签名
2. **算法与流程** - 规划、仲裁、时间窗判定逻辑
3. **最小实现** - 三态切换 (ORBIT/INTERCEPT/RETURN) 与耐久消耗
4. **事件接入** - 伤害前置回调与玩家 Tick 驱动
5. **配置参数** - 可调整的数值与开发计划

### ❌ 不包含内容（骨架阶段）

- **寻路细节** - 具体的路径查询与避让算法
- **物理模拟** - 速度曲线与精确碰撞响应
- **反弹实装** - 投射物镜面反射的完整代码
- **近战细节** - 攻击线段精确判定

---

## 🏗️ 核心架构设计

### 分层概览

```
┌─────────────────────────────────────────┐
│      Integration 层 (集成与接入)         │
│ WardSwordService (服务接口)             │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│    Algorithm 层 (算法与规划)             │
│ ├─ InterceptPlanner (拦截规划器)       │
│ ├─ WardTuning (参数供给接口)           │
│ └─ Data Classes (IncomingThreat, etc)  │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│     Entity 层 (实体扩展)                │
│ FlyingSwordEntity (字段 + 钩子)         │
│ + WardState 字段与访问器               │
└─────────────────────────────────────────┘
```

---

## 📂 文件组织结构

```
src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/
├── integration/
│   └── ward/  ← 护幕新增模块
│       ├── WardSwordService.java        (服务接口与默认实现)
│       ├── WardTuning.java              (参数供给接口)
│       ├── WardState.java               (状态枚举)
│       ├── WardConfig.java              (配置常量)
│       └── README.md                    (设计文档)
│
├── ai/
│   └── ward/  ← 护幕算法模块
│       ├── InterceptPlanner.java        (拦截规划器)
│       ├── WardThreat.java              (威胁模型，可选)
│       └── README.md                    (算法文档)
│
├── FlyingSwordEntity.java               (字段 + 钩子)
└── ...

docs/flyingsword/BlockShield/
├── 00_MASTER_PLAN.md                    (本文件)
├── 01_INTERFACE_DESIGN.md              (接口与数据结构设计)
├── 02_ALGORITHM_SPEC.md                (算法与公式规范)
├── 03_INTEGRATION_SPEC.md              (集成与事件回调)
├── 04_DEVELOPMENT_PLAN.md              (分阶段开发计划)
├── 05_TEST_CHECKLIST.md                (测试清单)
└── examples/
    └── usage_example.md                 (使用示例)
```

---

## 🔄 数据流向

```
┌─────────────────┐
│  伤害前置事件    │ ← HurtEvent / DamageEvent
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────┐
│ WardSwordService.onIncomingThreat() │
│ ├─ 解析威胁信息                      │
│ ├─ 调用 InterceptPlanner.plan()     │
│ └─ 获得 InterceptQuery              │
└────────┬────────────────────────────┘
         │
         ▼
┌────────────────────────────┐
│  仲裁（同帧唯一令牌）       │
│ ├─ 计算所有护幕的 tReach   │
│ ├─ 筛选窗口内者            │
│ └─ 取最小值 → 中标飞剑     │
└────────┬───────────────────┘
         │
         ▼
┌──────────────────────────────┐
│ FlyingSwordEntity 状态机      │
│ ├─ INTERCEPT (2-20 tick)    │
│ ├─ tryBlock() / tryFail()   │
│ ├─ COUNTER (可选)           │
│ └─ RETURN (回环绕)          │
└──────────────────────────────┘
         │
         ▼
┌──────────────────────────────┐
│  玩家 Tick (每帧驱动)        │
│ WardSwordService.tick()     │
│ ├─ 更新每把飞剑状态         │
│ └─ 执行位移 & 状态转换      │
└──────────────────────────────┘
```

---

## 🎯 核心概念

### 1. 护幕飞剑 (Ward Sword)

- **标志**: `wardSword = true` 的飞剑实例
- **特征**:
  - 不可召回 (`isRecallable = false`)
  - 不破坏方块 (`noBlockBreak = true`)
  - 环绕在主人周围 (固定环绕半径)
  - 响应伤害前置事件

### 2. 威胁 (Incoming Threat)

```
record IncomingThreat(
    Entity attacker,              // 攻击者
    Entity target,                // 目标（玩家）
    @Nullable Vec3 targetHitPoint, // 预期命中点
    @Nullable Vec3 projPos,        // 投射物位置
    @Nullable Vec3 projVel,        // 投射物速度
    long worldTime                // 事件时刻
)
```

### 3. 拦截查询 (Intercept Query)

```
record InterceptQuery(
    Vec3 interceptPoint,  // P* 拦截点（预计命中轨迹上的点）
    double tImpact,       // 投射物到达 P* 的时刻（秒）
    IncomingThreat threat // 原始威胁信息
)
```

### 4. 状态机 (Ward State)

```
enum WardState {
    ORBIT,      // 环绕主人
    INTERCEPT,  // 向 P* 移动（可达时间窗 0.1-1.0s）
    COUNTER,    // 完成拦截 & 条件满足(dist ≤ 5m) → 反击
    RETURN      // 返回环绕位
}
```

---

## 📊 时间与可达性

### 时间窗口

```
tReach = max(reaction_delay, distance(S_pos, P*) / vMax)
         ↓
判定条件: 0.1s ≤ tReach ≤ 1.0s → ✅ 可拦截
         tReach < 0.1s 或 > 1.0s → ❌ 不可拦截
```

### 反应延迟 (Reaction Delay)

```
reaction = clamp(0.06 - 0.00005 * 流派经验, 0.02, 0.06) (秒)
```

### 速度约束

```
vMax = 6.0 + 0.02 * 道痕 + 0.001 * 经验 (m/s)
```

---

## 💾 耐久消耗公式

```
R = clamp(经验 / (经验 + 2000), 0, 0.6)

costBlock   = round(8  * (1 - R))    // 成功拦截
costCounter = round(10 * (1 - R))    // 成功反击
costFail    = round(2  * (1 - 0.5R)) // 失败尝试
```

### 示例

- **经验 = 0**: R = 0
  - Block: 8, Counter: 10, Fail: 2
- **经验 = 2000**: R = 0.5
  - Block: 4, Counter: 5, Fail: 1
- **经验 = 10000**: R = 0.6 (上限)
  - Block: 3, Counter: 4, Fail: 1

---

## 🔌 与现有系统的集成点

### 1. 事件系统 (`events/`)

- **伤害前置** - 拦截 `HurtContext` 或独立钩子
- **玩家 Tick** - 在 `FlyingSwordEventRegistry` 中注册定时驱动

### 2. 系统层 (`systems/`)

- **MovementSystem** - 护幕飞剑向 P* 移动
- **CombatSystem** - (可选) 记录反击伤害

### 3. 运动层 (`motion/`)

- **KinematicsOps** - 计算向目标点的速度
- **SteeringOps** - 应用转向限制

### 4. 集成层 (`integration/`)

- **道痕、流派经验** - 通过 `WardTuning` 接口获取
- **资源消耗** - 通过 `UpkeepOps` 或独立接口扣减耐久

---

## 📝 关键接口预览

### WardSwordService (服务接口)

```java
public interface WardSwordService {
    // 生命周期
    List<FlyingSwordEntity> ensureWardSwords(Player owner);
    void disposeWardSwords(Player owner);

    // 事件驱动
    void onIncomingThreat(IncomingThreat threat);
    void tick(Player owner);
}
```

### WardTuning (参数供给)

```java
public interface WardTuning {
    int maxSwords(UUID owner);
    double orbitRadius(UUID owner, int swordCount);
    double vMax(UUID owner);
    double aMax(UUID owner);
    double reactionDelay(UUID owner);
    // ... 耐久消耗系数
}
```

### InterceptPlanner (规划器)

```java
public final class InterceptPlanner {
    public static @Nullable InterceptQuery plan(
        IncomingThreat threat,
        Player owner,
        WardTuning tuning
    );

    public static double timeToReach(
        FlyingSwordEntity s,
        Vec3 pStar,
        WardTuning tuning
    );
}
```

---

## 🛠️ 开发里程碑

| 阶段 | 任务 | 文件 | 状态 |
|------|------|------|------|
| **A** | 接口设计 & 基础字段 | Entity, Service, Tuning | 📋 待规划 |
| **B** | 规划算法实现 | InterceptPlanner | 📋 待规划 |
| **C** | 最小可玩 | 3 态 + 耐久消耗 | 📋 待规划 |
| **D** | 反弹与细化 | 反击、资源整合 | 📋 待规划 |
| **E** | 配置热加载 | Config, Tuning 实现 | 📋 待规划 |
| **F** | 测试与打磨 | Unit Tests, Manual Tests | 📋 待规划 |

---

## 📖 关联文档

- **01_INTERFACE_DESIGN.md** - 完整的类与接口定义
- **02_ALGORITHM_SPEC.md** - 详细的算法规范与公式
- **03_INTEGRATION_SPEC.md** - 事件回调与系统接入
- **04_DEVELOPMENT_PLAN.md** - 分阶段代码编写任务
- **05_TEST_CHECKLIST.md** - 测试验收清单
- **examples/usage_example.md** - 使用示例代码

---

## ⚡ 快速参考

| 概念 | 说明 |
|------|------|
| **护幕飞剑数** | N = 1 + floor(√(道痕/100)) + floor(经验/1000), max=4 |
| **环绕半径** | r = 2.6 + 0.4*N |
| **可达窗口** | 0.1 ~ 1.0 秒 |
| **反击距离** | ≤ 5.0 m |
| **初始耐久** | 60 + 0.3*道痕 + 0.1*经验 |
| **最大速度** | 6.0 + 0.02*道痕 + 0.001*经验 (m/s) |

---

## 📌 设计原则

1. **仅接口/骨架** - 此阶段不落地具体寻路/物理
2. **无新实体类型** - 基于 FlyingSwordEntity 扩展
3. **事件驱动** - 通过回调与 Tick 驱动
4. **松耦合** - 独立模块，易于维护与扩展
5. **可配置** - 所有数值通过接口供给，支持热加载

---

**计划版本**: v1.0
**最后更新**: 2025年
**作者**: AI Assistant

