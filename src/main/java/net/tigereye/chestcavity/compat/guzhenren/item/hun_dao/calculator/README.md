# Hun Dao Calculator 层设计文档

## 概述

Calculator 层是 Hun Dao（魂道）模块 Phase 4 的核心组件，负责将所有战斗和数值计算从行为层中剥离，形成可测试、可调参的纯计算模块。

## 设计原则

1. **纯函数计算**：所有 Calculator 方法不依赖 Minecraft 对象（如 `Player`、`LivingEntity`），仅根据输入参数返回数值结果
2. **单一数据来源**：所有常量从 `tuning/` 包读取，避免硬编码
3. **依赖倒置**：行为层通过接口或门面调用 Calculator，而非直接实例化
4. **可测试性**：每个计算方法都可以独立进行单元测试

## 目录结构

```
calculator/
├── HunDaoCombatCalculator.java      # 门面（Facade），统一入口
├── HunDaoDaohenOps.java             # Phase9 Dao痕/流派接口
├── HunDaoCooldownOps.java           # Phase9 冷却缩放接口
├── common/                           # 通用工具
│   ├── HunDaoCalcContext.java       # 计算上下文（不可变值对象）
│   └── CalcMath.java                # 数学工具（clamp/softCap/scale）
├── damage/                           # 伤害计算
│   ├── HunDaoDamageCalculator.java  # 真实伤害、护盾
│   └── HunDaoDotCalculator.java     # 魂焰 DoT 计算
├── resource/                         # 资源计算
│   ├── HunPoDrainCalculator.java    # 魂魄泄露
│   └── HunPoRecoveryCalculator.java # 魂魄/精力回复
└── skill/                            # 技能计算
    └── GuiWuCalculator.java         # 鬼雾范围、衰减
```

## 核心组件

### 1. HunDaoCombatCalculator（门面）

统一的计算入口，组合所有子 Calculator。

**使用示例**：

```java
// 创建计算上下文
HunDaoCalcContext context = HunDaoCalcContext.create(
    currentHunpo,  // 当前魂魄
    maxHunpo,      // 最大魂魄
    efficiency,    // 联动增益（1.0 = 无增益）
    stackCount     // 堆叠数
);

// 计算魂焰 DPS
double soulFlameDps = HunDaoCombatCalculator.dot().calculateSoulFlameDps(context);

// 计算真实伤害
float trueDamage = HunDaoCombatCalculator.damage().calculateGuiQiGuTrueDamage(context);

// 计算魂魄泄露
double leakPerSec = HunDaoCombatCalculator.resource().calculateLeakPerSecond();

// 计算鬼雾范围
double guiWuRadius = HunDaoCombatCalculator.skill().calculateGuiWuRadius();
```

## Phase 4 目标检查

- [x] 建立分层结构（common/damage/resource/skill）
- [x] 创建 HunDaoCombatCalculator 门面
- [x] 所有常量从 Tuning 读取
- [x] 计算方法纯函数化（不依赖 MC 对象）
- [ ] 与 HunDaoRuntimeContext 集成
- [ ] 行为层迁移完成
- [ ] 单元测试覆盖
- [ ] Smoke 测试验证

## Phase 9：道痕/流派接口占位

- **HunDaoDaohenOps / HunDaoDaohenCache**  
  - API 与 `jian_dao` 的 `JiandaoDaohenOps` 对齐，提供 `effectiveUncached`/`effectiveCached`/`invalidate`。
  - 当前实现仅透传 `daohen_hun_dao` 与 `liupai_hun_dao` 值，`resolveExpMultiplier` 返回 1.0，并输出 WARN 提醒 Phase9.x 需要补全。
  - `HunDaoDaohenCache` 提供 20 tick TTL 的简易缓存，后续可扩展为记录原始 Dao 痕/流派快照。

- **HunDaoCooldownOps**  
  - 对齐 `JiandaoCooldownOps` 的调用方式，提供 `withHunDaoExp(base, liupai, minTicks)`。
  - Phase 9 仅返回 `baseTicks`，并记录日志，等待 Phase9.x 写入实际冷却缩放逻辑。

这些入口现在已经暴露给 `HunDaoRuntimeContext#getScarOps()` 和 `getCooldownOps()`，行为层可以安全引用 API 而不依赖最终实现。

## 与 FX 系统的交互 (Phase 5)

Calculator 层负责**计算数值**，FX 层负责**视觉/音效表现**。两者通过 Runtime 层协调：

```
Behavior → Calculator.dot().calculateSoulFlameDps(ctx)  // 计算 DPS
         ↓
Behavior → RuntimeContext.fx().applySoulFlame(...)      // 应用 FX
         ↓
       HunDaoFxRouter → FxEngine (客户端粒子/音效)
```

**职责划分**：
- Calculator：返回伤害数值、持续时间、范围等
- FX Router：根据数值调度音效、粒子、动画
- Behavior：编排两者，先计算后表现

**示例**：
```java
// 计算魂焰 DPS 和持续时间
HunDaoCalcContext ctx = HunDaoCalcContext.create(...);
double dps = HunDaoCombatCalculator.dot().calculateSoulFlameDps(ctx);
int duration = HunDaoTuning.SoulFlame.DURATION_SECONDS;

// 应用 DoT（伤害） + FX（表现）
runtimeContext.fx().applySoulFlame(source, target, dps, duration);
```

**参见**：
- `fx/README.md` - FX 系统架构
- `runtime/README.md` - Runtime 上下文与操作接口

## 参考

- Phase 4 计划：`docs/Phase4_Plan.md`
- Phase 5 计划：`docs/Phase5_Plan.md`
- FX 文档：`fx/README.md`
- Tuning 文档：`tuning/README.md`（待创建）
- 测试报告：`docs/Phase4_Report.md`（待创建）
