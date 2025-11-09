# 剑幕·反击之幕 (BlockShield) - 项目文档总览

## 📚 文档导航

本项目包含以下核心文档，按阅读顺序排列：

| 文档 | 用途 | 读者 |
|------|------|------|
| **00_MASTER_PLAN.md** | 项目总体目标、范围、架构概览 | 所有人 |
| **01_INTERFACE_DESIGN.md** | 完整的类与接口定义 | 架构师、开发者 |
| **02_ALGORITHM_SPEC.md** | 详细的算法与数学公式 | 算法实现者 |
| **03_INTEGRATION_SPEC.md** | 系统集成点与事件回调 | 集成工程师 |
| **04_DEVELOPMENT_PLAN.md** | 分阶段的代码编写任务 | 项目经理、开发者 |
| **05_TEST_CHECKLIST.md** | 测试验收标准 | QA、开发者 |
| **examples/usage_example.md** | 使用示例代码 | 集成开发者 |

---

## 🎯 项目概览

### 目标
在不新增实体类型的前提下，基于现有 `FlyingSwordEntity` 实现**真拦截、真反击**的护幕 AI 与最小接口。

### 核心特性

#### ✅ 实装
- **拦截判定** - 在 0.1~1.0s 时间窗内拦截来袭威胁
- **三态机制** - ORBIT（环绕） → INTERCEPT（拦截） → COUNTER（反击） → RETURN（返回）
- **耐久消耗** - 成功/失败消耗，经验降低消耗成本
- **事件驱动** - 通过伤害前置与玩家 Tick 驱动
- **配置参数** - 所有数值通过接口供给，支持热加载

#### ❌ 骨架阶段不包含
- **寻路细节** - 具体的路径查询与避让
- **物理模拟** - 速度曲线与精确碰撞
- **反弹实装** - 投射物镜面反射的完整代码（仅签名）

---

## 🏗️ 核心架构

```
┌──────────────────────────────────────────────────┐
│         集成入口 (Integration)                    │
│  WardSwordService (主服务)                       │
│  ├─ ensureWardSwords(玩家) → 创建/维持护幕      │
│  ├─ onIncomingThreat(威胁) → 分配拦截任务       │
│  └─ tick(玩家) → 驱动状态机                      │
└──────────────────────────────────────────────────┘
                      │
                      ▼
┌──────────────────────────────────────────────────┐
│       算法层 (Algorithm)                          │
│  InterceptPlanner (规划器)                       │
│  ├─ plan() → 计算拦截点与预计时刻               │
│  └─ timeToReach() → 判定可达性                   │
└──────────────────────────────────────────────────┘
                      │
                      ▼
┌──────────────────────────────────────────────────┐
│     实体扩展 (FlyingSwordEntity)                  │
│  + wardSword 字段与访问器                        │
│  + tickWardBehavior() → 驱动护幕行为             │
│  + steerTo() → 转向与位移                        │
└──────────────────────────────────────────────────┘
```

---

## 📊 关键数值

### 护幕数量公式
```
N = clamp(1 + floor(sqrt(道痕/100)) + floor(经验/1000), 1, 4)
```

### 时间窗口
```
tReach ∈ [0.1, 1.0] 秒 → 可拦截
tReach < 0.1 或 > 1.0 秒 → 无法拦截
```

### 耐久消耗
```
R = clamp(经验 / (经验 + 2000), 0, 0.6)
Block:   cost = round(8  * (1 - R))
Counter: cost = round(10 * (1 - R))
Fail:    cost = round(2  * (1 - 0.5R))
```

---

## 📂 文件结构

```
src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/
├── integration/ward/                 # 集成层
│   ├── WardSwordService.java        # 服务接口
│   ├── WardTuning.java              # 参数接口
│   ├── WardState.java               # 状态枚举
│   ├── WardConfig.java              # 配置常量
│   └── DefaultWardSwordService.java # 默认实现
│
├── ai/ward/                          # 算法层
│   ├── InterceptPlanner.java        # 规划器
│   ├── IncomingThreat.java          # 威胁模型
│   └── InterceptQuery.java          # 规划结果
│
└── FlyingSwordEntity.java           # 扩展实体字段

docs/flyingsword/BlockShield/
├── 00_MASTER_PLAN.md               # 项目规划
├── 01_INTERFACE_DESIGN.md          # 接口设计
├── 02_ALGORITHM_SPEC.md            # 算法规范
├── 03_INTEGRATION_SPEC.md          # 集成规范
├── 04_DEVELOPMENT_PLAN.md          # 开发计划
├── 05_TEST_CHECKLIST.md            # 测试清单
└── examples/
    └── usage_example.md            # 使用示例
```

---

## 🚀 快速开始

### 第一步：阅读设计
1. 从 **00_MASTER_PLAN.md** 了解项目目标与架构
2. 查看 **01_INTERFACE_DESIGN.md** 了解类设计
3. 参考 **02_ALGORITHM_SPEC.md** 理解算法

### 第二步：执行开发
按 **04_DEVELOPMENT_PLAN.md** 的分阶段任务编写代码：
- **A 阶段** (4h) - 创建基础设施
- **B 阶段** (6h) - 实现规划算法
- **C 阶段** (8h) - 最小可玩原型
- **D 阶段** (6h) - 反弹与高级特性
- **E 阶段** (3h) - 配置与热加载
- **F 阶段** (4h) - 测试与打磨

### 第三步：验收测试
按 **05_TEST_CHECKLIST.md** 进行验收测试。

---

## 🔗 与现有系统的集成

### 事件系统
- **伤害前置** - 拦截 `HurtContext` 并改写伤害
- **Tick 驱动** - 通过 `FlyingSwordEventRegistry` 注册定时事件

### 系统层
- **MovementSystem** - 护幕飞剑的位移计算
- **CombatSystem** - (可选) 记录反击伤害

### 运动层
- **KinematicsOps** - 速度限制
- **SteeringOps** - 转向控制

### 集成层
- **道痕/流派经验** - 通过 `WardTuning` 获取
- **资源消耗** - 通过 UpkeepOps 扣减耐久

---

## 💡 设计原则

1. **无新实体** - 基于现有 FlyingSwordEntity 扩展
2. **仅接口骨架** - 本阶段不落地寻路/物理细节
3. **事件驱动** - 通过回调与 Tick 驱动，避免耦合
4. **松耦合设计** - 独立模块，易于维护与扩展
5. **完全可配置** - 所有数值通过接口供给

---

## 📋 任务检查清单

### 必读文档
- [ ] 00_MASTER_PLAN.md (20 min)
- [ ] 01_INTERFACE_DESIGN.md (30 min)
- [ ] 04_DEVELOPMENT_PLAN.md (30 min)

### 开发阶段
- [x] A. 基础设施与接口定义 (4h)
- [x] B. 规划算法实现 (6h)
- [ ] C. 最小可玩原型 (8h)
- [ ] D. 反弹与高级特性 (6h)
- [ ] E. 配置与热加载 (3h)
- [ ] F. 测试与打磨 (4h)

### 验收
- [ ] 所有测试通过
- [ ] 性能达标
- [ ] 文档完整

---

## ❓ 常见问题

### Q: 为什么不新增实体类型？
**A**: 基于现有系统可快速迭代，降低维护成本。护幕只是飞剑的一种模式。

### Q: 拦截点 P* 是如何计算的？
**A**: 见 **02_ALGORITHM_SPEC.md** § 3.3 规划算法。投射物通过线性预测，近战通过线段最近点。

### Q: 反击伤害如何计算？
**A**: 见 **02_ALGORITHM_SPEC.md** § 3.6 反击。基础伤害 5 点 + 0.05*道痕 + 0.01*经验。

### Q: 系统如何获取道痕等数据？
**A**: 通过 `WardTuning` 接口。实现应集成 GuzhenRen API 读取玩家数据。

### Q: 性能如何？
**A**: 见 **04_DEVELOPMENT_PLAN.md** § F.4 性能优化。目标：不超过全局 CPU 1%。

---

## 🔄 版本信息

| 版本 | 日期 | 备注 |
|------|------|------|
| v1.0 | 2025年 | 初稿：骨架阶段完整设计 |

---

## 📞 联系信息

- **项目经理**: AI Assistant
- **技术文档**: 本目录
- **相关讨论**: 项目 Wiki

---

## 📖 相关资源

- [飞剑模块总览](../flyingsword_module_overview.md)
- [飞剑系统设计](../FLYINGSWORD_TECH_FRAMEWORK.md)
- [事件系统文档](../events/README.md)
- [运动系统文档](../systems/README.md)

---

**最后更新**: 2025年
**文档版本**: v1.0
**维护者**: AI Assistant
