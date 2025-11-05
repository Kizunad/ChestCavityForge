# Phase 0｜接线与开关

## 阶段目标
- 初始化事件系统；添加特性开关并接入（不改默认行为）。
- 为后续裁剪工作做准备，确保功能开关可以控制可选功能的加载。

## 当前实现状态

### ✅ 已完成
- `FlyingSwordEventInit.java` 已存在并实现了 init() 方法
- 事件系统框架已就绪（FlyingSwordEventRegistry）
- 默认钩子已定义（DefaultEventHooks、SwordClashHook）

### ❌ 待完成
- FlyingSwordEventInit.init() **未在模组初始化流程中调用**
- FlyingSwordTuning 中**缺少功能开关**定义
- 客户端资源加载器（SwordVisualProfileLoader、SwordModelOverrideLoader）**未受开关控制**

## 任务清单

### 任务 0.1：在 FlyingSwordTuning.java 添加功能开关

**文件位置**：`src/main/java/net/tigereye/chestcavity/compat/guzhenren/flyingsword/tuning/FlyingSwordTuning.java`

**添加内容**：
```java
// ==================== 功能开关 ====================
// Phase 0: 添加功能开关，为后续裁剪做准备

/** 启用高级轨迹（保留：Orbit, PredictiveLine, CurvedIntercept） */
public static final boolean ENABLE_ADVANCED_TRAJECTORIES = false;

/** 启用额外意图（每个模式最多保留 2 条基础意图） */
public static final boolean ENABLE_EXTRA_INTENTS = false;

/** 启用青莲剑群系统（QingLianSwordSwarm） */
public static final boolean ENABLE_SWARM = false;

/** 启用剑引蛊 TUI（SwordCommandTUI） */
public static final boolean ENABLE_TUI = false;

/** 启用 Gecko 模型覆盖与视觉档案 */
public static final boolean ENABLE_GEO_OVERRIDE_PROFILE = false;
```

**插入位置**：在 `NON_PLAYER_DEFAULT_SWORD_PATH_EXP` 定义之后（文件末尾之前）

**预期结果**：编译通过，新增 5 个布尔常量

---

### 任务 0.2：在模组初始化流程调用 FlyingSwordEventInit.init()

**文件位置**：`src/main/java/net/tigereye/chestcavity/ChestCavity.java`

**当前状态**：
- line 179-184 的 `setup(FMLCommonSetupEvent event)` 方法中已有 RiftSystemInitializer 初始化
- **缺少** FlyingSwordEventInit.init() 调用

**修改方案**：
在 `setup()` 方法中，**RiftSystemInitializer.initialize() 之前**添加：

```java
public void setup(FMLCommonSetupEvent event) {
  // 初始化飞剑事件系统（Phase 0）
  if (ModList.get().isLoaded("guzhenren")) {
    net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.FlyingSwordEventInit.init();
  }

  // 初始化裂剑蛊系统（飞剑事件钩子、剑气技能注册）
  if (ModList.get().isLoaded("guzhenren")) {
    net.tigereye.chestcavity.compat.guzhenren.rift.RiftSystemInitializer.initialize();
  }
}
```

**原因**：事件系统应该在其他系统之前初始化，以便其他系统可以注册事件钩子。

**预期结果**：
- 服务端启动时日志输出：`[FlyingSwordEvent] Initializing event system...`
- 日志输出：`[FlyingSwordEvent] Event system initialized with 2 hooks`

---

### 任务 0.3：使客户端资源加载器受功能开关控制

**文件位置**：`src/main/java/net/tigereye/chestcavity/ChestCavity.java`

**当前状态**：
- line 218-231 的 `registerClientReloadListeners()` 方法中
- 无条件注册了 `SwordVisualProfileLoader` 和 `SwordModelOverrideLoader`

**修改方案**：
将这两个加载器的注册用 `ENABLE_GEO_OVERRIDE_PROFILE` 包裹：

```java
private void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
  event.registerReloadListener(new GuScriptLeafLoader());
  event.registerReloadListener(new GuScriptRuleLoader());
  event.registerReloadListener(new GeckoFxDefinitionLoader());
  event.registerReloadListener(new FxDefinitionLoader());

  // Gecko 模型与视觉档案系统（受功能开关控制）
  if (net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning.FlyingSwordTuning.ENABLE_GEO_OVERRIDE_PROFILE) {
    event.registerReloadListener(
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.client
            .profile.SwordVisualProfileLoader());
    event.registerReloadListener(
        new net.tigereye.chestcavity.compat.guzhenren.flyingsword.client
            .override.SwordModelOverrideLoader());
  }
}
```

**预期结果**：
- 当 `ENABLE_GEO_OVERRIDE_PROFILE = false` 时，Gecko 相关资源加载器不会被注册
- 编译通过，客户端启动正常

---

## 实现顺序

1. **先完成任务 0.1**（添加开关定义）
2. **再完成任务 0.2**（事件系统初始化）
3. **最后完成任务 0.3**（客户端加载器控制）

顺序原因：任务 0.3 依赖任务 0.1 中定义的常量。

---

## 依赖关系
- 无外部依赖（起点阶段）
- 任务 0.3 依赖任务 0.1

---

## 验收标准

### 编译测试
```bash
./gradlew compileJava
```
- ✅ 编译通过，无错误
- ✅ 无警告（或仅有已知警告）

### 功能测试
1. **事件系统初始化**：
   - 启动服务端，检查日志
   - 应包含：`[FlyingSwordEvent] Initializing event system...`
   - 应包含：`[FlyingSwordEvent] Event system initialized with 2 hooks`

2. **功能开关生效**：
   - 默认所有开关为 `false`
   - 游戏可正常启动（服务端 + 客户端）
   - 飞剑基础功能可用（召唤、环绕、护卫）

3. **客户端资源加载**：
   - 开关关闭时，Gecko 相关加载器不注册
   - 日志中不应出现 SwordVisualProfileLoader/SwordModelOverrideLoader 的加载信息

### 代码审查
- ✅ 开关命名符合规范（ENABLE_XXX）
- ✅ 注释清晰标注 Phase 0
- ✅ 导入语句完整（如需要）

---

## 风险与回退

### 风险等级：低

**潜在问题**：
1. 事件系统初始化顺序不当，导致其他系统找不到事件钩子
   - **缓解**：在 RiftSystemInitializer 之前初始化

2. 功能开关默认关闭，可能影响依赖这些功能的代码
   - **缓解**：本阶段仅添加开关，暂不使用（除了客户端加载器）
   - **验证**：手动测试基础功能

**回退方案**：
- 如果事件初始化失败：临时注释掉 `FlyingSwordEventInit.init()` 调用
- 如果客户端加载器控制失败：临时将 `ENABLE_GEO_OVERRIDE_PROFILE` 设为 `true`
- 所有更改均可通过 git revert 快速回退

---

## 后续阶段衔接

Phase 0 完成后，为 Phase 1 做好准备：
- ✅ 功能开关已定义
- ✅ 事件系统已初始化
- ➡️ Phase 1 可以开始使用开关来裁剪轨迹和意图

