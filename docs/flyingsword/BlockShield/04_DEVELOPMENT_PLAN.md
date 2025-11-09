# 护幕系统 - 分阶段开发计划

---

## 📋 总体时间表

| 阶段 | 名称 | 文件数 | 估算工时 | 状态 |
|------|------|--------|---------|------|
| **A** | 基础设施与接口定义 | 8 | 4h | ⏳ 待开始 |
| **B** | 规划算法实现 | 2 | 6h | ⏳ 待开始 |
| **C** | 最小可玩原型 | 3 | 8h | ⏳ 待开始 |
| **D** | 反弹与高级特性 | 4 | 6h | ⏳ 待开始 |
| **E** | 配置与热加载 | 3 | 3h | ⏳ 待开始 |
| **F** | 测试与打磨 | - | 4h | ⏳ 待开始 |
| **总计** |  | 20+ | 31h | ⏳ 待开始 |

---

## 🔧 A. 基础设施与接口定义

### 目标
建立完整的类结构骨架，确保编译通过并能被其他模块引用。

### 任务

#### A.1: 创建 ward 包结构
**文件**: 目录创建
**工具**: Bash + Serena
```bash
# 创建目录结构
mkdir -p src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/integration/ward
mkdir -p src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/ai/ward
```

**检查点**: 目录存在且为空

---

#### A.2: 实现 WardState 枚举
**文件**: `WardState.java`
**位置**: `integration/ward/`
**工具**: Serena (insert_before_symbol)

**实现要点**:
- [x] 定义 4 个枚举值 (ORBIT, INTERCEPT, COUNTER, RETURN)
- [x] 实现 `fromId(String)` 静态方法
- [x] Javadoc 注释完整

**检查点**: 编译通过，能被 FlyingSwordEntity 导入

---

#### A.3: 实现数据模型 (Threat & Query)
**文件**: `IncomingThreat.java`, `InterceptQuery.java`
**位置**: `ai/ward/`
**工具**: Serena (write)

**实现要点**:
- [x] IncomingThreat record 包含 6 个字段
- [x] InterceptQuery record 包含 3 个字段
- [x] 添加 `isProjectile()`, `isMelee()` 辅助方法
- [x] Javadoc 完整

**检查点**: 编译通过

---

#### A.4: 实现 WardTuning 接口
**文件**: `WardTuning.java`
**位置**: `integration/ward/`
**工具**: Serena (write)

**实现要点**:
- [x] 定义 12 个方法签名（数量、参数、伤害、耐久等）
- [x] 每个方法都有详细 Javadoc（包括公式）
- [x] 无实现体，仅接口

**检查点**: 编译通过

---

#### A.5: 实现 WardSwordService 接口
**文件**: `WardSwordService.java`
**位置**: `integration/ward/`
**工具**: Serena (write)

**实现要点**:
- [x] 定义 6 个核心方法 (ensureWardSwords, disposeWardSwords, onIncomingThreat, tick, getWardSwords, etc.)
- [x] Javadoc 包含详细的流程描述
- [x] 方法签名与 01_INTERFACE_DESIGN.md 完全一致

**检查点**: 编译通过

---

#### A.6: 实现 WardConfig 常量类
**文件**: `WardConfig.java`
**位置**: `integration/ward/`
**工具**: Serena (write)

**实现要点**:
- [x] 定义 14 个常量字段
- [x] 值与 01_INTERFACE_DESIGN.md 一致
- [x] 私有构造函数

**检查点**: 编译通过

---

#### A.7: 扩展 FlyingSwordEntity 字段与访问器
**文件**: `FlyingSwordEntity.java`
**位置**: `/`（根目录）
**工具**: Serena (find_symbol + insert_after_symbol)

**实现要点**:
- [x] 新增 6 个私有字段 (wardSword, wardDurability, wardState, orbitSlot, currentQuery, interceptStartTime)
- [x] 实现 11 个访问器方法
- [x] 仅签名（`tickWardBehavior`, `steerTo` 返回 void 或布尔）
- [x] 导入必要的类

**具体步骤**:

1. **找到合适位置**:
   ```java
   mcp__serena__find_symbol:
     name_path: "FlyingSwordEntity/cachedTarget"
     relative_path: "src/main/java/.../FlyingSwordEntity.java"
   ```

2. **在现有字段后插入新字段**:
   ```java
   mcp__serena__insert_after_symbol:
     name_path: "FlyingSwordEntity/cachedTarget"
     relative_path: "..."
     body: "
     // —— 护幕运行期字段 ——
     private boolean wardSword = false;
     private double wardDurability = 0.0;
     private WardState wardState = WardState.ORBIT;
     @Nullable private Vec3 orbitSlot = null;
     @Nullable private InterceptQuery currentQuery = null;
     private long interceptStartTime = 0L;
     "
   ```

3. **在类末尾添加访问器方法**（使用 insert_after_symbol 和最后一个方法）

**检查点**: 编译通过，无重复定义

---

#### A.8: 实现 InterceptPlanner 空骨架
**文件**: `InterceptPlanner.java`
**位置**: `ai/ward/`
**工具**: Serena (write)

**实现要点**:
- [x] 定义 3 个静态方法 (plan, timeToReach, 辅助方法)
- [x] plan() 返回 null （骨架）
- [x] timeToReach() 返回 0.0 （骨架）
- [x] Javadoc 包含详细算法描述
- [x] 私有构造函数

**检查点**: 编译通过

---

### 验收标准
- 所有 Java 源文件编译通过 (`./gradlew compileJava`)
- 没有 unused import 警告
- FlyingSwordEntity 能正确导入新增类
- 模块间无循环依赖

---

## 🎯 B. 规划算法实现

### 目标
实现拦截点预测与时间窗判定的核心算法。

### 任务

#### B.1: 实现投射物轨迹预测
**文件**: `InterceptPlanner.java`
**工具**: Serena (replace_symbol_body)

**算法伪代码**:
```
function predictProjectileHitPoint(projPos, projVel, target, gravity):
    // 迭代从 0 到 1.0s，步长 0.05s
    for t in [0, 0.05, 0.10, ..., 1.0]:
        predPos = projPos + projVel * t + gravity * t^2
        if predPos 与 target.getBoundingBox() 相交:
            return predPos
    return null
```

**实现要点**:
- [x] 使用梯形法或线性逼近模拟重力
- [x] 检测与目标 AABB 的相交
- [x] 返回第一个相交点或 null
- [x] 考虑性能（最多 20 个迭代）

**检查点**: 通过单元测试（静止投射物应快速相交）

---

#### B.2: 实现近战线段预测
**文件**: `InterceptPlanner.java`
**工具**: Serena (replace_symbol_body)

**算法伪代码**:
```
function predictMeleeHitPoint(attacker, target):
    // 线性外推：attacker.position → target.position
    // 取与 target.AABB 最近的点
    segment = Line(attacker.getPos, target.getPos)
    closestPoint = segment.closestPointTo(target.getBoundingBox)
    return closestPoint
```

**实现要点**:
- [x] 构造 3D 线段
- [x] 计算线段到 AABB 的最近点
- [x] 返回有效点或 null
- [x] 考虑到达时间（基于攻击者速度）

**检查点**: 通过单元测试（直线冲刺应有有效的最近点）

---

#### B.3: 实现 plan() 方法
**文件**: `InterceptPlanner.java`
**工具**: Serena (replace_symbol_body)

**实现要点**:
- [x] 判断威胁类型（投射 vs 近战）
- [x] 调用相应的预测方法获得命中点 I
- [x] 从命中点推导拦截点 P* (I - 0.3*norm(v))
- [x] 计算 tImpact
- [x] 验证是否在时间窗内
- [x] 返回 InterceptQuery 或 null

**检查点**: 单元测试（投射与近战各 3 个场景）

---

#### B.4: 实现 timeToReach() 方法
**文件**: `InterceptPlanner.java`
**工具**: Serena (replace_symbol_body)

**实现要点**:
- [x] 计算飞剑到 P* 的距离
- [x] 获取 vMax 与 reaction
- [x] 返回 max(reaction, distance/vMax)
- [x] 边界检查（vMax 不为 0）

**检查点**: 公式验证（d=10m, vMax=10m/s, reaction=0.06s → result≈1.06s）

---

### 验收标准
- `./gradlew compileJava` 通过
- 所有公式与 02_ALGORITHM_SPEC.md 完全一致
- 单元测试覆盖率 ≥ 80%

---

## 🎮 C. 最小可玩原型

### 目标
实现三态切换与基础耐久消耗，达到可演示的阶段。

### 任务

#### C.1: 实现 WardSwordService 默认实现
**文件**: `DefaultWardSwordService.java`
**位置**: `integration/ward/`
**工具**: Serena (write)

**实现要点**:
- [ ] 内存映射存储护幕飞剑 (Map<UUID, List<FlyingSwordEntity>>)
- [ ] ensureWardSwords(): 创建或移除飞剑以达到目标数量
- [ ] disposeWardSwords(): 清空列表
- [ ] onIncomingThreat(): 调用规划器，分配拦截任务
- [ ] tick(): 驱动状态机（ORBIT → INTERCEPT → RETURN → ORBIT）
- [ ] getWardSwords(), getWardCount(), hasWardSwords(): 查询方法

**状态机逻辑伪代码**:
```
for each wardSword in swords:
    switch wardSword.getWardState():
        case ORBIT:
            // 保持在环绕槽位
            target = owner.getPos + orbitSlot
            steerTo(wardSword, target, aMax, vMax)

        case INTERCEPT:
            // 向拦截点移动
            query = wardSword.getCurrentQuery()
            target = query.interceptPoint
            steerTo(wardSword, target, aMax, vMax)

            // 检测超时或成功
            elapsed = (worldTime - interceptStartTime) / 20.0  // 转秒
            if elapsed > 1.0:
                // 超时 → 失败
                consumeWardDurability(costFail)
                setWardState(RETURN)
            elif 碰撞检测成功:
                // 成功拦截 → 反击或返回
                consumeWardDurability(costBlock)
                if dist(attacker, owner) <= 5m:
                    setWardState(COUNTER)
                else:
                    setWardState(RETURN)

        case COUNTER:
            // 执行反击（骨架：仅消耗耐久）
            consumeWardDurability(costCounter)
            setWardState(RETURN)

        case RETURN:
            // 返回环绕位
            target = owner.getPos + orbitSlot
            steerTo(wardSword, target, aMax, vMax)
            if 距离 < 0.5m:
                setWardState(ORBIT)
```

**检查点**: 编译通过，能被导入

---

#### C.2: 实现 WardTuning 默认实现
**文件**: `DefaultWardTuning.java`
**位置**: `integration/ward/`
**工具**: Serena (write)

**实现要点**:
- [ ] 所有方法返回 WardConfig 中的常量（骨架）
- [ ] maxSwords(): 返回 4 (常数)
- [ ] orbitRadius(): 使用公式 2.6 + 0.4*N
- [ ] vMax(), aMax(), etc.: 返回常量或简单公式
- [ ] counterDamage(): 返回固定值 (5.0)
- [ ] costBlock/Counter/Fail(): 返回 WardConfig 常量

**检查点**: 编译通过

---

#### C.3: 扩展 FlyingSwordEntity.tickWardBehavior()
**文件**: `FlyingSwordEntity.java`
**工具**: Serena (find_symbol + replace_symbol_body)

**实现要点**:
- [ ] 获取 WardSwordService 实例（从全局注册或参数传入）
- [ ] 调用 service.tick(owner) 驱动状态机
- [ ] 处理异常不向上抛出

**代码样例**:
```java
public void tickWardBehavior(Player owner, WardTuning tuning) {
    if (!this.wardSword) return;

    try {
        // 从某个全局服务中心获取
        WardSwordService service = WardSwordServiceHolder.getInstance();
        // 让服务驱动这个飞剑的行为
        service.tick(owner);
    } catch (Exception e) {
        LOGGER.warn("Error in wardBehavior for " + this.getName(), e);
    }
}
```

**检查点**: FlyingSwordEntity 编译通过，tickServer() 正确调用

---

#### C.4: 实现 steerTo() 转向方法
**文件**: `FlyingSwordEntity.java`
**工具**: Serena (find_symbol + replace_symbol_body)

**实现要点**:
- [ ] 接收目标位置、加速度、最大速度
- [ ] 计算向目标的方向向量
- [ ] 调用 MovementSystem.applySteeringVelocity() 或直接设置速度
- [ ] 处理速度限制与加速度约束

**简化实现**:
```java
public void steerTo(Vec3 target, double aMax, double vMax) {
    Vec3 toTarget = target.subtract(this.position());
    double dist = toTarget.length();
    if (dist < 0.5) return; // 已到达

    Vec3 dir = toTarget.normalize();
    Vec3 currentVel = this.getDeltaMovement();
    double currentSpeed = currentVel.length();

    // 限制加速度
    double maxAccelThisTick = aMax / 20.0;  // tick 转秒
    double targetSpeed = Math.min(currentSpeed + maxAccelThisTick, vMax);

    Vec3 newVel = dir.scale(targetSpeed);
    this.setDeltaMovement(newVel);
}
```

**检查点**: 飞剑能向目标平稳运动

---

### 验收标准
- `./gradlew compileJava` 通过
- 创建玩家并激活护幕，应有 1-4 个飞剑在周围环绕
- 受到伤害时，飞剑能切换到 INTERCEPT 并向拦截点移动
- 拦截失败时，耐久正确消耗
- 飞剑能返回环绕并回到 ORBIT 状态

---

## 🔄 D. 反弹与高级特性

### 目标
增强反击与投射反弹，提升游戏体验。

### 任务

#### D.1: 实现成功拦截的伤害清零
**文件**: 伤害事件回调（待创建或扩展现有）
**工具**: Serena

**实现要点**:
- [ ] 在伤害前置钩子中，若拦截成功，将伤害设为 0
- [ ] 或按"穿甲保留 30%" 规则缩放
- [ ] 触发 PostHit 事件

**检查点**: 拦截成功时玩家不受伤

---

#### D.2: 实现投射物反弹
**文件**: `DefaultWardSwordService.java` 或独立模块
**工具**: Serena (replace_symbol_body)

**实现要点**:
- [ ] 在反击时，检测威胁是否为投射物
- [ ] 计算镜面反射速度：v' = v - 2*(v·n)*n
- [ ] 改变投射物的所有者为玩家（如可能）
- [ ] 改变投射物速度

**检查点**: 箭矢能被反弹回射手

---

#### D.3: 实现近战反击
**文件**: `DefaultWardSwordService.java`
**工具**: Serena (replace_symbol_body)

**实现要点**:
- [ ] 在反击时，若威胁为近战，生成一次"剑气突刺"粒子或伤害事件
- [ ] 沿"玩家→攻击者"方向发起伤害
- [ ] 使用 counterDamage() 公式计算伤害

**检查点**: 近战反击能对攻击者造成伤害

---

#### D.4: 集成道痕与流派经验参数
**文件**: `DefaultWardTuning.java`
**工具**: Serena (replace_symbol_body)

**实现要点**:
- [ ] 从玩家数据中读取"道痕"等级和"流派经验"值
- [ ] 替换硬编码的常数，使用公式计算
- [ ] 考虑如何获取这些数据（GuzhenRen API）

**例子**:
```java
@Override
public double vMax(UUID owner) {
    Player player = Minecraft.getInstance().level.getPlayerByUUID(owner);
    if (player == null) return WardConfig.SPEED_BASE;

    int trail = getTrailLevel(player);
    int exp = getSectExp(player);

    return 6.0 + 0.02 * trail + 0.001 * exp;
}
```

**检查点**: 参数动态变化，与玩家经验挂钩

---

### 验收标准
- 反击能对投射物进行反弹
- 近战反击能伤害攻击者
- 道痕提升会降低耐久消耗

---

## ⚙️ E. 配置与热加载

### 目标
使护幕系统可配置，支持运行时修改。

### 任务

#### E.1: 创建配置文件格式
**文件**: `config/ward.toml` (或 JSON)
**位置**: `config/` 目录
**工具**: 文本编辑

**示例内容**:
```toml
[ward]
window_min = 0.1
window_max = 1.0
counter_range = 5.0
speed_base = 6.0
accel_base = 40.0

[costs]
block = 8
counter = 10
fail = 2
```

**检查点**: TOML 语法正确，文件存在

---

#### E.2: 实现配置加载器
**文件**: `WardConfigLoader.java`
**位置**: `integration/ward/`
**工具**: Serena (write)

**实现要点**:
- [ ] 使用 nightconfig 或 gson 读取配置文件
- [ ] 缓存配置，支持热重载（可选）
- [ ] 验证数值范围
- [ ] 回退到默认值

**检查点**: 能从文件加载配置

---

#### E.3: 扩展 DefaultWardTuning 使用配置
**文件**: `DefaultWardTuning.java`
**工具**: Serena (replace_symbol_body)

**实现要点**:
- [ ] 注入 WardConfigLoader
- [ ] 返回加载的配置值而非常量

**检查点**: 修改配置文件后，游戏内数值更新（需重启或热重载命令）

---

### 验收标准
- 配置文件存在且可读
- 加载器能正确解析配置
- 修改配置后，效果生效（至少需重启）

---

## 🧪 F. 测试与打磨

### 目标
验证系统完整性与性能，修复缺陷。

### 任务

#### F.1: 单元测试
**文件**: `InterceptPlannerTest.java`, etc.
**位置**: `src/test/java/...`
**工具**: JUnit 5

**测试覆盖**:
- [ ] InterceptPlanner.plan() 投射场景
- [ ] InterceptPlanner.plan() 近战场景
- [ ] InterceptPlanner.timeToReach() 边界情况
- [ ] WardConfig 常量值
- [ ] 耐久消耗公式

**检查点**: `./gradlew test` 通过，覆盖率 ≥ 80%

---

#### F.2: 集成测试
**文件**: `BlockShieldIntegrationTest.java`
**位置**: `src/test/java/...`
**工具**: JUnit 5 + Minecraft Test Framework

**测试场景**:
- [ ] 激活护幕，验证飞剑数量
- [ ] 伤害前置，验证拦截分配
- [ ] 完整交互（受伤 → 拦截 → 反击）
- [ ] 耐久耗尽 → 消散

**检查点**: 所有场景通过

---

#### F.3: 手动测试清单
**文件**: `05_TEST_CHECKLIST.md` (待创建)
**工具**: 文档编写

**内容**:
- [ ] 玩家接近怪物，护幕能否及时拦截
- [ ] 箭矢能否成功反弹
- [ ] 近战反击能否触发
- [ ] 多个护幕同时拦截时的仲裁
- [ ] 性能监控（无明显延迟）

**检查点**: 所有项目通过

---

#### F.4: 性能优化
**工具**: Java 分析工具（JProfiler/YourKit）

**优化点**:
- [ ] 避免每 tick 进行大量 GC
- [ ] 规划器的缓存机制（可选）
- [ ] 检测到达的提前退出

**检查点**: 护幕系统不超过全局 CPU 1%

---

### 验收标准
- 所有测试通过
- 无明显性能下降
- 文档完整

---

## 🗺️ 任务依赖关系

```
A.1 (目录创建)
  ├─ A.2 (WardState) ──┐
  ├─ A.3 (Data)       ├─ B.1/B.2 (预测算法)
  ├─ A.4 (Tuning)     ├─ B.3 (plan)
  ├─ A.5 (Service)    ├─ B.4 (timeToReach)
  ├─ A.6 (Config)     └─ C.1 (Service 实现)
  ├─ A.7 (Entity)         ├─ C.2 (Tuning 实现)
  └─ A.8 (Planner)        ├─ C.3 (tickWardBehavior)
                           ├─ C.4 (steerTo)
                           ├─ D.* (高级特性)
                           ├─ E.* (配置)
                           └─ F.* (测试)
```

---

## 📊 完成度跟踪

使用以下模板跟踪每个任务的完成度：

```markdown
### [x] A.1 - 创建目录结构
- [x] 目录存在
- [x] 权限正确
- [x] 检查点通过

### [x] A.2 - WardState 枚举
- [x] 编码完成
- [x] Javadoc 完整
- [x] 编译通过
```

---

**计划版本**: v1.0
**最后更新**: 2025年
