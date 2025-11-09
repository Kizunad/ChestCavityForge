# 护幕系统 (BlockShield) - 完整实现计划

## 📌 项目概览

**目标**: 基于 FlyingSwordEntity，实现真拦截、真反击的护幕 AI 与最小接口。
**范围**: 仅接口骨架 & 算法规范，不含寻路/物理细节实现。
**工作量**: 约 31 小时（6 个阶段）

---

## 📂 文档位置

所有文档已创建在 `/home/kiz/Code/java/ChestCavityForge/docs/flyingsword/BlockShield/`:

### 核心文档
- **00_MASTER_PLAN.md** (10.8 KB) - 项目总体规划
- **01_INTERFACE_DESIGN.md** (19.1 KB) - 完整类设计与接口
- **02_ALGORITHM_SPEC.md** (11.7 KB) - 算法与数学公式
- **04_DEVELOPMENT_PLAN.md** (17.7 KB) - 分阶段开发任务
- **README.md** (8.3 KB) - 文档导航与快速开始

### 待创建文档
- **03_INTEGRATION_SPEC.md** - 事件集成与系统接入
- **05_TEST_CHECKLIST.md** - 测试验收标准
- **examples/usage_example.md** - 使用示例代码

---

## 🏗️ 核心设计

### 文件结构
```
src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/
├── integration/ward/           # 集成层
│   ├── WardSwordService.java  # 服务接口
│   ├── WardTuning.java        # 参数接口
│   ├── WardState.java         # 状态枚举
│   ├── WardConfig.java        # 配置常量
│   └── DefaultWardSwordService.java # 默认实现
├── ai/ward/                    # 算法层
│   ├── InterceptPlanner.java  # 规划器
│   ├── IncomingThreat.java    # 威胁模型
│   └── InterceptQuery.java    # 规划结果
└── FlyingSwordEntity.java     # 扩展字段
```

### 数据流向
```
伤害事件 → onIncomingThreat() → InterceptPlanner.plan()
  ↓
InterceptQuery (拦截点 & 时刻)
  ↓
仲裁 (同帧唯一令牌)
  ↓
FlyingSwordEntity 状态机 (ORBIT → INTERCEPT → COUNTER/RETURN → ORBIT)
  ↓
玩家 Tick 驱动 (service.tick())
```

---

## 📊 关键数值

### 护幕数量
```
N = clamp(1 + floor(√(Trail/100)) + floor(Exp/1000), 1, 4)
```

### 时间窗口
```
tReach ∈ [0.1, 1.0]s → 可拦截
```

### 耐久消耗
```
R = clamp(Exp/(Exp+2000), 0, 0.6)
costBlock = round(8*(1-R))
costCounter = round(10*(1-R))
costFail = round(2*(1-0.5R))
```

---

## 🎯 开发阶段

| 阶段 | 名称 | 文件数 | 工时 | 关键任务 |
|------|------|--------|------|---------|
| **A** | 基础设施 | 8 | 4h | 创建接口、数据模型、实体字段 |
| **B** | 规划算法 | 2 | 6h | 投射预测、近战预测、plan()、timeToReach() |
| **C** | 最小可玩 | 3 | 8h | 状态机实现、位移驱动、耐久消耗 |
| **D** | 高级特性 | 4 | 6h | 反弹、近战反击、道痕参数集成 |
| **E** | 配置 | 3 | 3h | 配置文件、热加载、参数化 |
| **F** | 测试 | - | 4h | 单元测试、集成测试、性能验证 |

---

## 🔌 集成关键点

### 事件系统
- **伤害前置** - 拦截 HurtContext，改写伤害
- **Tick 驱动** - FlyingSwordEventRegistry 注册事件

### 系统层
- **MovementSystem** - 护幕位移
- **CombatSystem** - 反击伤害（可选）

### 运动层
- **KinematicsOps** - 速度限制
- **SteeringOps** - 转向控制

### 集成层
- **WardTuning** - 动态获取道痕/经验
- **UpkeepOps** - 耐久扣减

---

## 📋 快速检查清单

### 第一步：理解设计 (1.5h)
- [ ] 阅读 00_MASTER_PLAN.md
- [ ] 理解 01_INTERFACE_DESIGN.md 中的 5 个关键类
- [ ] 学习 02_ALGORITHM_SPEC.md 中的公式

### 第二步：A 阶段 (4h) - 基础设施
- [ ] A.1 创建包结构 (Bash)
- [ ] A.2-A.6 实现 6 个接口与常量类 (Serena: write)
- [ ] A.7 扩展 FlyingSwordEntity 字段 (Serena: insert_after_symbol)
- [ ] A.8 创建 InterceptPlanner 空骨架 (Serena: write)
- [ ] 验证编译通过: `./gradlew compileJava`

### 第三步：B 阶段 (6h) - 规划算法
- [ ] B.1 投射物预测 (Serena: replace_symbol_body)
- [ ] B.2 近战预测 (Serena: replace_symbol_body)
- [ ] B.3 plan() 方法 (Serena: replace_symbol_body)
- [ ] B.4 timeToReach() 方法 (Serena: replace_symbol_body)
- [ ] 编写单元测试

### 第四步：C 阶段 (8h) - 最小可玩
- [ ] C.1 DefaultWardSwordService 实现 (Serena: write)
- [ ] C.2 DefaultWardTuning 实现 (Serena: write)
- [ ] C.3 tickWardBehavior() 实现 (Serena: replace_symbol_body)
- [ ] C.4 steerTo() 实现 (Serena: replace_symbol_body)
- [ ] 手动测试：护幕能否正确环绕与拦截

### 第五步：D+E+F (13h)
- [ ] D 阶段：反弹与高级特性
- [ ] E 阶段：配置与热加载
- [ ] F 阶段：测试与性能优化

---

## 🛠️ 推荐使用的 Serena 工具

### 创建新文件
```
mcp__serena__write()
  file: "src/main/java/.../ClassName.java"
  content: "完整的 Java 代码"
```

### 查找符号
```
mcp__serena__find_symbol()
  name_path: "ClassName"
  relative_path: "src/main/java/.../ClassName.java"
  depth: 1
```

### 在符号后插入
```
mcp__serena__insert_after_symbol()
  name_path: "ClassName/lastField"
  relative_path: "..."
  body: "新增的字段或方法"
```

### 替换方法体
```
mcp__serena__replace_symbol_body()
  name_path: "ClassName/methodName"
  relative_path: "..."
  body: "新的方法实现"
```

---

## 🔗 关键接口签名

### WardSwordService (服务)
```java
List<FlyingSwordEntity> ensureWardSwords(Player owner);
void onIncomingThreat(IncomingThreat threat);
void tick(Player owner);
```

### WardTuning (参数)
```java
int maxSwords(UUID owner);
double vMax(UUID owner);
double reactionDelay(UUID owner);
int costBlock(UUID owner);
// ... 共 12 个方法
```

### InterceptPlanner (规划器)
```java
@Nullable InterceptQuery plan(IncomingThreat, Player, WardTuning);
double timeToReach(FlyingSwordEntity, Vec3, WardTuning);
```

### FlyingSwordEntity (扩展)
```java
void setWardSword(boolean);
void tickWardBehavior(Player, WardTuning);
void steerTo(Vec3, double, double);
// ... 共 11 个访问器
```

---

## ✅ 验收标准

### 编译
- `./gradlew compileJava` 通过
- 无 unused import 警告

### 功能
- 护幕能按数量计算正确生成
- 受伤时能拦截并消耗耐久
- 时间窗内判定正确
- 反击距离 ≤ 5m

### 性能
- 护幕系统不超过 CPU 1%
- 无明显卡顿或内存泄漏

### 测试
- 单元测试覆盖率 ≥ 80%
- 所有验收测试通过

---

## 📞 常见问题

**Q: 拦截点 P* 如何计算？**
A: 见 02_ALGORITHM_SPEC.md § 3.2-3.3。投射物使用二次轨迹预测，近战使用线段最近点。

**Q: 如何确保同帧唯一拦截？**
A: 通过仲裁算法选择最小 tReach 的飞剑，使用原子操作更新。

**Q: 反击伤害基线是多少？**
A: 5 + 0.05*Trail + 0.01*Exp

**Q: 耐久消耗如何计算？**
A: 见 02_ALGORITHM_SPEC.md § 8.2。经验越多，消耗越低。

---

## 📚 相关文档

- [飞剑模块总览](../../flyingsword_module_overview.md)
- [飞剑技术框架](../../FLYINGSWORD_TECH_FRAMEWORK.md)
- [系统层 README](../../systems/README.md)
- [事件系统 README](../../events/README.md)

---

**计划版本**: v1.0
**总工时**: 31h
**状态**: 📋 待开发
**最后更新**: 2025年

