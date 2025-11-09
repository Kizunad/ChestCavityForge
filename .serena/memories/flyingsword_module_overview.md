# 飞剑模块完整结构概览

## 📋 模块概述

飞剑模块是 ChestCavityForge 中的核心功能，围绕"纯飞剑逻辑 + 跨系统桥接"的分层架构设计，共包含 **200+ 个 Java 类文件**。

---

## 🏗️ 架构分层

### 1️⃣ **核心层 (Core)** - 纯飞剑逻辑
位置：`/` 根目录

| 核心类 | 职责 | 关键方法 |
|------|------|-------|
| **FlyingSwordEntity** | 飞剑实体主体，扩展 PathfinderMob | `tick()`, `hurt()`, `setAIMode()`, `setTargetEntity()` |
| **FlyingSwordController** | 飞剑控制器，提供操纵接口 | `setAIMode()`, `recall()`, `setTarget()`, `getPlayerSwords()` |
| **FlyingSwordAttributes** | 飞剑属性模型（速度、伤害、耐久等） | `applyModifiers()`, `saveToNBT()`, `loadFromNBT()` |
| **FlyingSwordType** | 飞剑类型枚举和预设 | - |
| **FlyingSwordTypePresets** | 飞剑类型预设配置 | - |

**关键特性：**
- 仅依赖通用 Minecraft API
- 自包含，易于扩展和复用

---

### 2️⃣ **AI 决策层** - `ai/` 目录
负责飞剑的行为决策，生成期望速度或命令

#### 📍 子模块：

| 子模块 | 文件数 | 职责 |
|------|------|------|
| **intent/** | 15+ | 意图系统 - 定义飞剑的战术意图 |
| **trajectory/** | 15+ | 轨迹系统 - 生成空间轨迹路径 |
| **behavior/** | 7+ | 行为规则 - 基础行为组件（狩猎、守卫、轨道等） |
| **command/** | 3 | 指挥棒系统 - 高阶命令中枢 |
| **swarm/** | 2 | 群体行为 - 多剑协同控制 |

**核心枚举：**
- **AIMode** - AI模式枚举（IDLE, GUARD, HUNT, etc.）

**关键接口：**
- **Intent** - 战术意图基类
- **Trajectory** - 轨迹基类

---

### 3️⃣ **运动执行层** - `motion/` 目录
统一处理转向和速度限制

| 类 | 职责 |
|---|---|
| **KinematicsOps** | 运动学计算（加速、速度限制） |
| **SteeringOps** | 转向操作和转向优先级 |
| **SteeringCommand** | 转向命令结构 |
| **KinematicsSnapshot** | 运动学快照（缓存计算结果） |
| **LegacySteeringAdapter** | 旧系统兼容适配器 |
| **SteeringTemplate** | 转向模板基类 |

---

### 4️⃣ **飞剑操作层** - `ops/` 目录
飞剑自身的可复用操作，不触碰外部系统

| 类 | 职责 |
|---|---|
| **BlockBreakOps** | 破坏方块逻辑 |
| **RepairOps** | 修复耐久逻辑 |
| **SoundOps** | 音效播放 |

---

### 5️⃣ **外部集成层** - `integration/` 目录
与其他系统（道痕、领域、冷却等）的桥接

#### 📍 子模块：

| 子模块 | 文件 | 职责 |
|------|------|------|
| **domain/** | 2 | 领域系统集成（速度修饰符、伤害钩子） |
| **resource/** | 1 | 资源系统（真元扣减、维持消耗） |
| **cooldown/** | 1 | 冷却系统集成 |

**关键类：**
- **SwordSpeedModifiers** - 读取领域状态，调整飞剑速度
- **SwordDomainDamageHook** - 领域伤害钩子
- **UpkeepOps** - 真元资源管理

---

### 6️⃣ **事件系统** - `events/` 目录
飞剑专属事件钩子，支持外部模块监听和扩展

#### 📍 核心组件：

| 类 | 职责 |
|---|---|
| **FlyingSwordEventRegistry** | 事件注册中心 |
| **FlyingSwordEventHook** | 事件钩子基类 |
| **DefaultEventHooks** | 默认事件实现 |

#### 📍 事件上下文（`context/`）：
支持以下生命周期事件：
- **SpawnContext** - 生成时
- **TickContext** - 每 tick
- **HurtContext** - 受伤时
- **HitEntityContext** - 击中实体时
- **PostHitContext** - 击中后处理
- **TargetAcquiredContext** - 获取目标时
- **TargetLostContext** - 丧失目标时
- **ModeChangeContext** - AI 模式改变时
- **LevelUpContext** - 升级时
- **ExperienceGainContext** - 获得经验时
- **DespawnContext** - 消失时
- **BlockBreakAttemptContext** - 破块尝试
- **BlockBreakContext** - 破块成功
- **InteractContext** - 交互时
- **UpkeepCheckContext** - 维持检查

#### 📍 专属钩子（`hooks/`）：
- **SwordClashHook** - 飞剑碰撞钩子
- **JianYingGuShadowHook** - 剑影蛊影子钩子
- **JianSuoGuSwordAutoDashHook** - 剑锁蛊自动冲刺钩子

---

### 7️⃣ **系统执行层** - `systems/` 目录
飞剑的主要系统循环

| 类 | 职责 | 执行时机 |
|---|---|---|
| **MovementSystem** | 运动更新 | 每 tick（服务器） |
| **CombatSystem** | 战斗逻辑 | 击中时 |
| **UpkeepSystem** | 维持消耗检查 | 定时检查 |
| **RiderControlSystem** | 骑士控制逻辑 | 乘坐时 |

---

### 8️⃣ **计算和配置层**

| 目录 | 职责 |
|------|------|
| **calculator/** | 伤害计算、属性计算、数值公式 |
| **tuning/** | 所有可配置参数（速度、伤害、冷却等） |

**关键类：**
- **FlyingSwordCalculator** - 伤害计算器
- **FlyingSwordTuning** - 核心调参
- **FlyingSwordAITuning** - AI 调参
- **FlyingSwordCombatTuning** - 战斗调参

---

### 9️⃣ **客户端层** - `client/` 目录
渲染、模型、特效等客户端逻辑

#### 📍 子模块：

| 子模块 | 职责 |
|------|------|
| **profile/** | 飞剑视觉配置（模型、纹理） |
| **fx/** | 特效和粒子系统 |
| **gecko/** | Geckolib 3D 模型集成 |
| **orientation/** | 飞剑姿态和朝向计算 |
| **override/** | 模型覆写机制 |
| **pipeline/** | 渲染管道优化 |

**关键类：**
- **FlyingSwordRenderer** - 主渲染器
- **SwordVisualProfile** - 视觉配置

---

### 🔟 **UI 层** - `ui/` 目录
飞剑管理的终端界面（TUI）

| 类 | 职责 |
|---|---|
| **FlyingSwordTUI** | 主 TUI 界面 |
| **FlyingSwordTUIOps** | TUI 操作集合 |
| **TUISessionManager** | TUI 会话管理 |
| **TUICommandGuard** | 命令守卫 |
| **CharWidthCalculator** | 字符宽度计算 |

---

### 1️⃣1️⃣ **初始化层** - `init/` 目录
飞剑模块的启动和注册

| 类 | 职责 |
|---|---|
| **FlyingSwordInit** | 主初始化器 |
| **FlyingSwordInitSpec** | 初始化规范 |

---

### 1️⃣2️⃣ **工具和其他**

| 目录 | 文件 | 职责 |
|------|------|------|
| **util/** | 3 | 物品亲和度、身份识别、耐久度工具 |
| **state/** | 2 | 飞剑选择、冷却数据附件 |
| **FlyingSwordCommand** | - | 命令处理 |
| **FlyingSwordSpawner** | - | 飞剑生成工厂 |
| **FlyingSwordStorage** | - | 持久化存储 |
| **FlyingSwordEventHandler** | - | 事件处理主类 |

---

## 📊 文件统计

```
总计：~200+ Java 文件
├── 核心类：5 个（Entity, Controller, Attributes, Type, etc.）
├── AI 系统：30+ 个（Intent, Trajectory, Behavior）
├── 运动系统：6 个（Kinematics, Steering）
├── 集成系统：6 个（Domain, Resource, Cooldown）
├── 事件系统：20+ 个（Hooks, Context）
├── 系统执行：4 个（Movement, Combat, Upkeep, Rider）
├── 计算调参：15+ 个（Calculator, Tuning）
├── 客户端：25+ 个（Renderer, FX, Gecko）
├── UI 系统：6+ 个（TUI, Session）
└── 其他：10+ 个（Util, State, Init, etc.）
```

---

## 🔄 数据流向

```
玩家操作
  ↓
FlyingSwordController (控制入口)
  ↓
FlyingSwordEntity (实体状态)
  ↓
AI 决策 (Intent → Trajectory)
  ↓
Motion 层 (KinematicsOps, SteeringOps)
  ↓
Integration 层 (领域、资源、冷却等外部系统)
  ↓
Systems 执行 (MovementSystem, CombatSystem, etc.)
  ↓
Events 触发 (事件钩子监听)
  ↓
Client 渲染 (FlyingSwordRenderer, FX)
```

---

## 🛠️ 编辑指引

1. **纯飞剑逻辑** → 核心层或 `ai/` `motion/` 目录
2. **外部系统交互** → `integration/` 子模块
3. **扩展行为** → `ai/intent/types/` 或 `ai/behavior/`
4. **新轨迹类型** → `ai/trajectory/impl/`
5. **事件监听** → 通过 `FlyingSwordEventRegistry` 注册钩子
6. **配置参数** → `tuning/` 目录
7. **UI 操作** → `ui/` 或通过 `FlyingSwordController` 接口

---

## 🔗 关键依赖关系

```
FlyingSwordEntity
  ├── FlyingSwordController (通过 get/set 方法控制)
  ├── FlyingSwordAttributes (属性管理)
  ├── AIMode (AI 模式)
  ├── Intent + Trajectory (AI 决策)
  ├── KinematicsOps + SteeringOps (运动执行)
  ├── Integration 系统 (外部系统接口)
  ├── Systems (执行循环)
  └── Events (事件发布)

FlyingSwordController
  └── FlyingSwordEntity (批量操纵)
```

---

## ✅ 验证架构完整性

修改后执行：
```bash
./gradlew compileJava
```

