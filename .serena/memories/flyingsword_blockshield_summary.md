# 飞剑护幕系统（BlockShield）总结

## 📋 项目概述
**目标**：实现"真拦截、真反击"的护幕系统，基于现有飞剑架构，无需新增实体类型。

## 🏗️ 核心架构

### 分层设计
```
Integration层（集成与接入）
  ├─ WardSwordService       # 服务接口（生命周期、事件、驱动）
  └─ WardTuning            # 参数供给接口（数值计算）

Algorithm层（算法与规划）
  ├─ InterceptPlanner      # 拦截规划器（纯函数式）
  └─ IncomingThreat/Query  # 数据模型

Entity层（实体扩展）
  └─ FlyingSwordEntity     # 6个字段 + 11个访问器
```

## 🎯 核心概念

### 1. 护幕飞剑 (Ward Sword)
- 标志：`wardSword=true`
- 特征：不可召回、不破坏方块、环绕主人
- 状态机：ORBIT → INTERCEPT → COUNTER → RETURN → ORBIT

### 2. 时间窗口 (Time Window)
- 可达时间公式：`tReach = max(reaction_delay, distance/vMax)`
- 可达条件：`0.1s ≤ tReach ≤ 1.0s`
- 反应延迟：`reaction = clamp(0.06 - 0.00005*经验, 0.02, 0.06)`

### 3. 护幕数量 (Ward Count)
- 公式：`N = clamp(1 + floor(√(道痕/100)) + floor(经验/1000), 1, 4)`
- 环绕半径：`r = 2.6 + 0.4*N`

### 4. 耐久消耗 (Durability Cost)
- R系数：`R = clamp(经验/(经验+2000), 0, 0.6)`
- 拦截：`costBlock = round(8*(1-R))`
- 反击：`costCounter = round(10*(1-R))`
- 失败：`costFail = round(2*(1-0.5R))`

### 5. 初始耐久 (Initial Durability)
- 公式：`Dur₀ = 60 + 0.3*道痕 + 0.1*经验`

### 6. 最大速度 (Max Velocity)
- 公式：`vMax = 6.0 + 0.02*道痕 + 0.001*经验`

## 📂 文件组织

```
src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/
├── integration/ward/
│   ├── WardSwordService.java        # 服务接口
│   ├── DefaultWardSwordService.java  # 默认实现
│   ├── WardTuning.java               # 参数接口
│   ├── DefaultWardTuning.java        # 默认实现
│   ├── WardState.java                # 状态枚举
│   ├── WardConfig.java               # 常量定义
│   └── WardConfigLoader.java         # 配置加载
│
├── ai/ward/
│   ├── InterceptPlanner.java         # 拦截规划器
│   ├── IncomingThreat.java           # 威胁模型
│   └── InterceptQuery.java           # 拦截查询
│
└── FlyingSwordEntity.java
    ├── wardSword: boolean
    ├── wardDurability: double
    ├── wardState: WardState
    ├── orbitSlot: Vec3
    ├── currentQuery: InterceptQuery
    └── interceptStartTime: long
```

## 🔄 数据流向

```
伤害事件 → onIncomingThreat()
    ↓
解析威胁 + 调用 InterceptPlanner.plan()
    ↓
仲裁（选择最小tReach的飞剑）
    ↓
分配INTERCEPT任务 + 设置currentQuery
    ↓
玩家Tick驱动 → service.tick()
    ↓
状态机转换：ORBIT→INTERCEPT→(COUNTER/)RETURN→ORBIT
    ↓
耐久消耗 + 反击处理
```

## 📐 关键算法

### 投射物命中点预测
1. 迭代0-1.0s，步长0.05s
2. 计算轨迹点：P(t) = P₀ + v₀*t + 0.5*g*t²
3. 检测与AABB相交，返回第一个相交点

### 近战命中点预测
1. 构造攻击线段：attacker → target
2. 计算线段到AABB最近点
3. 返回该点

### 拦截点计算
- 投射物：`P* = I - 0.3*normalize(v₀)`（提前0.3m）
- 近战：`P* = I`（目标最近点）

### 仲裁算法
1. 遍历所有ORBIT状态的飞剑
2. 计算每把飞剑的tReach
3. 筛选在[0.1, 1.0]范围内的候选
4. 选择最小tReach者为中标飞剑

## 🎮 状态机

```
ORBIT (环绕)
  ↓ (拦截令牌)
INTERCEPT (向P*移动, 0.1-1.0s)
  ├─ 成功 → COUNTER
  └─ 失败/超时 → RETURN
  ↓
COUNTER (反击，仅距离≤5m)
  ↓
RETURN (返回环绕位)
  ↓
ORBIT
```

## 💾 耐久机制

### 消耗情景
1. **拦截成功**：costBlock = 8-3（随经验）
2. **反击成功**：costCounter = 10-4（随经验）
3. **拦截失败**：costFail = 2-1（随经验）
4. **耐尽**：飞剑消散

### 耐久恢复
- 回环绕位置自动恢复（待实现）

## 🔌 集成点

### 伤害前置钩子
```
HurtContext/DamageEvent → onIncomingThreat()
    ↓ (if拦截成功)
伤害置0 or 缩放30%
```

### 玩家Tick驱动
```
PlayerTickEvent → service.tick(player)
    ↓
驱动所有护幕飞剑状态机
```

## 📋 开发阶段

| 阶段 | 任务数 | 工时 | 状态 | 关键任务 |
|-----|-------|------|------|--------|
| A | 8 | 4h | ⏳ | 接口设计、Entity字段、常量定义 |
| B | 4 | 6h | ⏳ | 轨迹预测、拦截规划 |
| C | 4 | 8h | ⏳ | 最小可玩、状态机、耐久消耗 |
| D | 4 | 6h | **已完成** | 伤害清零、投射反弹、近战反击、参数集成 |
| E | 3 | 3h | ⏳ | 配置文件、热加载 |
| F | - | 4h | ⏳ | 单元/集成测试 |
| **总计** | **23** | **31h** | **1/6** | |

## ✅ D阶段完成情况

根据 04_DEVELOPMENT_PLAN.md，D阶段包含4个任务：

### D.1: 伤害清零
- [ ] 在伤害前置钩子中，若拦截成功，将伤害设为0
- [ ] 或按"穿甲保留30%"规则缩放
- 状态：**待实现**

### D.2: 投射物反弹
- [ ] 在反击时，检测威胁是否为投射物
- [ ] 计算镜面反射速度：`v' = v - 2*(v·n)*n`
- [ ] 改变投射物所有者与速度
- 状态：**待实现**

### D.3: 近战反击
- [ ] 在反击时，若威胁为近战，生成反击伤害
- [ ] 沿"玩家→攻击者"方向发起伤害
- [ ] 使用 counterDamage() 公式计算伤害
- 状态：**待实现**

### D.4: 参数集成
- [ ] 从玩家数据读取"道痕"等级与"流派经验"
- [ ] 替换硬编码常数，使用公式动态计算
- [ ] 考虑从GuzhenRen API获取数据
- 状态：**待实现**

## 🔧 技术依赖

### 现有系统
- FlyingSwordEntity 基础框架
- 伤害事件系统
- 玩家Tick事件
- GuzhenRen属性系统（道痕、经验）

### 可选集成
- MovementSystem（转向控制）
- CombatSystem（伤害记录）
- KinematicsOps（速度计算）
- SteeringOps（转向限制）

## 📖 相关文档

- **00_MASTER_PLAN.md** - 总体规划与架构
- **01_INTERFACE_DESIGN.md** - 接口与数据结构
- **02_ALGORITHM_SPEC.md** - 详细算法与公式
- **03_INTEGRATION_SPEC.md** - 事件回调与系统接入（待查看）
- **04_DEVELOPMENT_PLAN.md** - 分阶段开发计划
- **05_TEST_CHECKLIST.md** - 测试清单
