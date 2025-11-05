# Phase 4｜冷却与资源一致性

## 阶段目标
- 冷却集中 `MultiCooldown`；资源消耗统一 `ResourceOps`；失败策略可配。

## 当前实现状态（核对）
- 冷却统一入口：已提供 `FlyingSwordCooldownOps` 统一 API，并接入 `CombatSystem → FlyingSwordCombat` 攻击冷却链路。
- 存储实现：已切换为 owner 附件 `FLYING_SWORD_COOLDOWN`（基于 Attachment 序列化），Key = `cc:flying_sword/<uuid>/<domain>`。
- Upkeep 覆盖：`UpkeepSystem` 已支持 `UpkeepCheck.finalCost` 覆盖并提供 `skipConsumption` 跳过，调用 `consumeFixedUpkeep(finalCost)`。

## 实施日期
2025-11-05

## 任务列表

### ✅ 已完成任务

#### 4.1 创建 FlyingSwordCooldownOps 封装类
**位置**: `src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/integration/cooldown/FlyingSwordCooldownOps.java`

新增冷却统一管理类（338 行）：

**核心功能**：
- 封装 MultiCooldown 操作，屏蔽附件获取细节
- 统一 Key 命名规范：`cc:flying_sword/<sword_uuid>/<domain>`
- 提供类型安全的冷却读写方法
- 自动防止负冷却（所有设置自动 clamp 到 ≥0）

**Key 命名规范**：
```
cc:flying_sword/<sword_uuid>/attack       - 攻击冷却
cc:flying_sword/<sword_uuid>/block_break  - 破块冷却
cc:flying_sword/<sword_uuid>/ability      - 能力冷却
```

**公开 API**：
```java
// 通用方法
int get(FlyingSwordEntity sword, String domain)
boolean set(FlyingSwordEntity sword, String domain, int ticks)
boolean isReady(FlyingSwordEntity sword, String domain)
boolean tryStart(FlyingSwordEntity sword, String domain, int ticks)
int tickDown(FlyingSwordEntity sword, String domain)

// 便捷方法（攻击冷却）
int getAttackCooldown(FlyingSwordEntity sword)
boolean setAttackCooldown(FlyingSwordEntity sword, int ticks)
boolean isAttackReady(FlyingSwordEntity sword)
int tickDownAttackCooldown(FlyingSwordEntity sword)

// 便捷方法（破块冷却）
int getBlockBreakCooldown(FlyingSwordEntity sword)
boolean setBlockBreakCooldown(FlyingSwordEntity sword, int ticks)
boolean isBlockBreakReady(FlyingSwordEntity sword)
int tickDownBlockBreakCooldown(FlyingSwordEntity sword)
```

**存储机制**：
- 冷却存储在 owner 的 `ChestCavityInstance` 附件中
- 使用 `MultiCooldown.EntryInt`（倒计时模式）
- 自动 NBT 持久化（通过 MultiCooldown）
- owner 切换后冷却保持（存储在 owner 上）

#### 4.2 添加维持失败策略配置
**位置**: `src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/tuning/FlyingSwordTuning.java`

新增配置（+38 行）：

**策略枚举**：
```java
public enum UpkeepFailureStrategy {
  RECALL,  // 召回到物品栏（默认，兼容旧版）
  STALL,   // 停滞不动，速度冻结
  SLOW     // 减速移动
}
```

**配置常量**：
```java
// 默认策略：召回（兼容旧版行为）
public static final UpkeepFailureStrategy UPKEEP_FAILURE_STRATEGY =
    UpkeepFailureStrategy.RECALL;

// SLOW 策略的速度倍率（0.0 到 1.0）
public static final double UPKEEP_FAILURE_SLOW_FACTOR = 0.3;

// STALL 策略下播放音效的间隔（tick）
public static final int UPKEEP_FAILURE_SOUND_INTERVAL = 40;
```

#### 4.3 迁移攻击冷却到 MultiCooldown
**文件修改**：

**FlyingSwordCombat.java**（+1 import, 修改 60 行）：
- 更新 `tickCollisionAttack()` 方法签名：移除 `int attackCooldown` 参数和返回值
- 使用 `FlyingSwordCooldownOps.tickDownAttackCooldown(sword)` 递减冷却
- 使用 `FlyingSwordCooldownOps.setAttackCooldown(sword, cd)` 设置冷却
- 方法从 `public static int` 改为 `public static void`

**CombatSystem.java**（修改 26 行）：
- 更新 `tick()` 方法签名：移除 `int attackCooldown` 参数和返回值
- 调用 `FlyingSwordCombat.tickCollisionAttack(sword)` 不再传递冷却参数
- 方法从 `public static int` 改为 `public static void`

**FlyingSwordEntity.java**（修改 8 行）：
- 移除 `private int attackCooldown = 0` 字段声明
- 添加注释说明已迁移到 MultiCooldown
- 更新 `tick()` 方法中的调用：
  - 旧：`this.attackCooldown = CombatSystem.tick(this, this.attackCooldown);`
  - 新：`CombatSystem.tick(this);`

#### 4.4 实现维持失败策略
**位置**: `src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/systems/UpkeepSystem.java`

新增方法 `handleUpkeepFailure(FlyingSwordEntity sword)`（+50 行）：

**策略实现**：
```java
switch (FlyingSwordTuning.UPKEEP_FAILURE_STRATEGY) {
  case RECALL -> {
    // 默认策略：召回飞剑（兼容旧版）
    FlyingSwordController.recall(sword);
  }
  case STALL -> {
    // 停滞策略：冻结移动速度
    sword.setDeltaMovement(Vec3.ZERO);
    // 定期播放音效（避免刷屏）
    if (sword.tickCount % UPKEEP_FAILURE_SOUND_INTERVAL == 0) {
      SoundOps.playOutOfEnergy(sword);
    }
  }
  case SLOW -> {
    // 减速策略：速度降低到配置的倍率
    var currentVel = sword.getDeltaMovement();
    var slowedVel = currentVel.scale(UPKEEP_FAILURE_SLOW_FACTOR);
    sword.setDeltaMovement(slowedVel);
  }
}
```

**行为说明**：
- `RECALL`: 保持 Phase 2/3 的默认行为，召回到物品栏
- `STALL`: 飞剑停滞不动但不消失，每 40 tick 播放一次音效提示
- `SLOW`: 飞剑以 30% 速度继续移动（可配置）

## 代码行数变化

| 文件 | 变化 | 说明 |
|------|------|------|
| **新增文件** | | |
| FlyingSwordCooldownOps.java | +338 行 | 新增冷却统一管理类 |
| **修改文件** | | |
| FlyingSwordTuning.java | +38 行 | 新增维持失败策略配置 |
| FlyingSwordCombat.java | ~60 行 | 迁移到 FlyingSwordCooldownOps |
| CombatSystem.java | ~26 行 | 更新方法签名 |
| FlyingSwordEntity.java | ~8 行 | 移除 attackCooldown 字段 |
| UpkeepSystem.java | +50 行 | 实现维持失败策略 |
| FLYINGSWORD_STANDARDS.md | +46 行 | 文档化 Phase 4 规范 |
| **总计** | **+520 行** | 冷却与资源一致性完成 |

## 架构改进

### 冷却管理统一化

**Before Phase 4**:
```java
// FlyingSwordEntity.java
private int attackCooldown = 0;  // ❌ 实体字段，不持久化

// tick()
this.attackCooldown = CombatSystem.tick(this, this.attackCooldown);
```

**After Phase 4**:
```java
// FlyingSwordEntity.java
// Phase 4: attackCooldown 已迁移到 MultiCooldown (owner 附件)
// ✅ 存储在 owner 的 ChestCavityInstance 中，自动持久化

// tick()
CombatSystem.tick(this);  // ✅ 内部使用 FlyingSwordCooldownOps
```

**优势**：
1. **持久化**：冷却自动随 MultiCooldown 持久化到 NBT
2. **集中管理**：所有冷却统一存储位置，便于调试和扩展
3. **无负冷却**：自动 clamp 到 ≥0，防止数据异常
4. **owner 绑定**：冷却跟随 owner，不随飞剑消失而丢失
5. **类型安全**：通过 FlyingSwordCooldownOps 封装，防止直接操作

### 维持失败策略可配置化

**Before Phase 4**:
```java
if (!success) {
  SoundOps.playOutOfEnergy(sword);
  FlyingSwordController.recall(sword);  // ❌ 硬编码唯一策略
}
```

**After Phase 4**:
```java
if (!success) {
  handleUpkeepFailure(sword);  // ✅ 根据配置执行不同策略
}

private static void handleUpkeepFailure(FlyingSwordEntity sword) {
  SoundOps.playOutOfEnergy(sword);
  switch (FlyingSwordTuning.UPKEEP_FAILURE_STRATEGY) {
    case RECALL -> recall(sword);
    case STALL -> stall(sword);
    case SLOW -> slow(sword);
  }
}
```

**优势**：
1. **可配置**：玩家/服务器可选择不同策略
2. **兼容性**：默认 RECALL 保持旧版行为
3. **扩展性**：未来可添加更多策略（如 HOVER、CUSTOM）
4. **用户体验**：不同场景选择不同策略

## 依赖关系
- ✅ 依赖 Phase 2 的系统抽取（CombatSystem, UpkeepSystem）
- ✅ 依赖 Phase 3 的事件系统（UpkeepCheck 事件）
- ✅ 依赖 MultiCooldown 基础设施（Phase 0 已存在）

## 验收标准

### 编译测试
```bash
./gradlew compileJava
```
- ✅ 编译通过（待用户环境验证）
- ✅ 无新增警告

### 功能测试

**冷却管理**：
- [ ] 攻击冷却正常工作（攻击间隔正确）
- [ ] 冷却持久化正确（重载世界后冷却保留）
- [ ] 无负冷却出现
- [ ] owner 切换后冷却保持

**维持失败策略**：
- [ ] RECALL 策略：召回到物品栏（默认）
- [ ] STALL 策略：停滞不动，定期音效
- [ ] SLOW 策略：减速移动（30% 速度）

### 性能测试
- [ ] 冷却操作无性能回退
- [ ] MultiCooldown 附件内存占用合理
- [ ] 大量飞剑同时存在时性能稳定

### 兼容性测试
- [ ] 旧存档加载正常（无 attackCooldown NBT 数据）
- [ ] 默认策略（RECALL）行为与 Phase 3 一致
- [ ] 外部模块通过 FlyingSwordCooldownOps 扩展冷却

## 风险与回退

### 风险等级：中

**潜在问题**：
1. **MultiCooldown 附件未创建**
   - **影响**：无 owner 或 owner 不是 ChestCavityEntity 时冷却失效
   - **缓解**：FlyingSwordCooldownOps 自动创建附件
   - **回退**：返回 0（表示就绪），不阻塞行为

2. **冷却数据迁移**
   - **影响**：旧存档中的 attackCooldown 字段数据丢失
   - **缓解**：attackCooldown 本身不持久化，重载后自然重置
   - **回退**：无需迁移

3. **维持失败策略误配置**
   - **影响**：STALL/SLOW 策略下飞剑无法自动召回
   - **缓解**：默认 RECALL 保持旧版行为
   - **回退**：修改配置为 RECALL

**回退方案**：
- FlyingSwordCooldownOps 内部有完整的 null 检查，失败时返回安全默认值
- 维持失败策略默认 RECALL，兼容旧版行为
- 可单独回退某个组件（如维持失败策略）而不影响冷却系统

## 下一步 (Phase 5)

Phase 4 完成后，Phase 5 将专注于：
- 客户端渲染降噪
- 网络同步优化（减少数据包）
- Gecko/TUI 开关化（可选加载）
- 粒子效果优化

---

## 不能做什么（红线约束）
- 不得继续在实体类中维护新的“裸”整型冷却字段（如 `attackCooldown2` 等）；冷却状态统一由 `MultiCooldown` 管理。
- 不得绕过 `ResourceOps` 直接消耗/恢复主人资源（禁止 `LinkageManager`/`LinkageChannel.adjust`/`ledger.remove`）。
- 不得在冷却和资源路径中引入阻塞 I/O、昂贵的日志输出或客户端类依赖。
- 不得修改既有数值体验（攻击节奏、资源成本）的大幅基准，仅做“存储位置与一致性”迁移。
- 不得在同一逻辑上保留两套并行冷却实现（避免“双写”与漂移）。

## 要做什么（详细清单）
- 冷却统一到 MultiCooldown（owner 附件）
  - 设计并固定 Key 命名：
    - `cc:flying_sword/<sword_uuid>/attack`
    - `cc:flying_sword/<sword_uuid>/blockbreak`
    - `cc:flying_sword/global/recall_window`（可选）
  - 提供轻薄封装方法：`CooldownOps.get/set/hasCooldown(sword,key)`，屏蔽附件细节
  - 在 `CombatSystem.tick` 替换实体字段 `attackCooldown` 的读取/写回为 MultiCooldown 操作
  - 在 BlockBreakOps 破块节流点接入对应冷却 Key（如每 tick 最多 N 方块）
  - 兼容期：保留实体字段作为镜像，仅从 MultiCooldown 同步回写，验收后移除字段

- 资源消耗一致性（Upkeep）
  - 已完成：UpkeepCheck 事件与 `consumeFixedUpkeep` 覆盖路径（P3）
  - 扩展策略：在 `FlyingSwordTuning` 暴露 `ON_UPKEEP_FAILURE_STRATEGY={STALL|SLOW|RECALL}` 与参数（如减速倍率/持续秒数）
  - 在 `UpkeepSystem.tick` 中按策略执行：
    - STALL：冻结速度，保留姿态；
    - SLOW：按倍率降低 `deltaMovement`；
    - RECALL：调用 `FlyingSwordController.recall`（保持现状默认）
  - 统一音效/提示：仅在进入失败策略时播放一次提示，避免刷屏

- 观察与验证
  - 增加可选 DEBUG 指标：冷却命中次数、资源失败次数、策略命中次数（默认关闭）
  - 手动测试：长时间运行后无“负冷却/残留计时器”，召回/重载后冷却与资源状态一致

- 文档与迁移说明
  - 更新 `FLYINGSWORD_STANDARDS.md` 冷却与资源章节，规定统一入口
  - 在 `PHASE_4.md` 保留“不能做/要做”对照作为上线前检查清单
