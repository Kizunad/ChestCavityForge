# Phase 5｜客户端与网络

## 阶段目标
- 客户端渲染降噪：默认仅保留基础渲染 FlyingSwordRenderer；Gecko/覆盖/视觉档受 ENABLE_GEO_OVERRIDE_PROFILE 控制
- 指挥与提示降级：TUI 关闭时仅保留最小反馈
- 网络同步审计：减少包频率与冗余载荷（优先复用实体同步）

## 当前实现状态（核对）
- 资源加载器控制：`SwordVisualProfileLoader` 和 `SwordModelOverrideLoader` 已在 `ChestCavity.java` 中受 `ENABLE_GEO_OVERRIDE_PROFILE` 控制
- 渲染器路径：`FlyingSwordRenderer` 中的 Gecko 渲染逻辑需要添加开关检查
- TUI 系统：`SwordCommandCenter` 和 `SwordCommandTUI` 需要添加降级逻辑
- 网络同步：`FlyingSwordEntity` 使用了大量 `SynchedEntityData`，需要审计优化

## 实施日期
2025-11-05

## 任务列表

### 5.1 客户端渲染降噪

#### 5.1.1 ✅ Gecko/Visual Profile 资源加载器开关化
**位置**: `ChestCavity.java:228-238`

**状态**: 已在 Phase 0 完成

资源加载器已受 `ENABLE_GEO_OVERRIDE_PROFILE` 控制：
```java
if (FlyingSwordTuning.ENABLE_GEO_OVERRIDE_PROFILE) {
  event.registerReloadListener(new SwordVisualProfileLoader());
  event.registerReloadListener(new SwordModelOverrideLoader());
}
```

#### 5.1.2 FlyingSwordRenderer 渲染路径优化
**位置**: `FlyingSwordRenderer.java`

**问题**：即使资源加载器未注册，渲染器仍会尝试查询注册表并执行 Gecko 渲染逻辑

**优化方案**：
1. 在查询 `SwordModelOverrideRegistry` 和 `SwordVisualProfileRegistry` 前添加开关检查
2. 将 Gecko 渲染路径（Line 118-154）包裹在开关条件中
3. 当开关关闭时，直接使用基础 Item 渲染路径

**代码修改** (+10 行):
```java
// Line 61-62: 添加开关检查
SwordModelOverrideDef def = null;
SwordVisualProfile prof = null;
if (FlyingSwordTuning.ENABLE_GEO_OVERRIDE_PROFILE) {
  def = SwordModelOverrideRegistry.getForSword(entity).orElse(null);
  prof = SwordVisualProfileRegistry.getForSword(entity).orElse(null);
}

// Gecko 渲染逻辑保持不变（已被 def/prof 为 null 自然跳过）
```

**优势**：
- 避免空查询开销
- 清晰表达功能开关语义
- Gecko 渲染路径自然降级到基础渲染

### 5.2 TUI 与指挥系统降级

#### 5.2.1 SwordCommandCenter 添加 TUI 降级逻辑
**位置**: `SwordCommandCenter.java`

**当前问题**：
- 所有指令反馈都通过 `SwordCommandTUI.open()` 输出详细 UI
- 当 `ENABLE_TUI=false` 时，应提供简化的文本反馈

**实现方案**：
1. 在 `SwordCommandCenter` 中添加 `sendMinimalFeedback()` 方法
2. 在需要打开 TUI 的位置添加开关判断
3. TUI 关闭时输出简单文本消息

**代码修改** (+30 行):
```java
// 在 SwordCommandCenter 中添加
private static void sendFeedback(ServerPlayer player, CommandSession session) {
  if (FlyingSwordTuning.ENABLE_TUI) {
    SwordCommandTUI.open(player, session);
  } else {
    sendMinimalFeedback(player, session);
  }
}

private static void sendMinimalFeedback(ServerPlayer player, CommandSession session) {
  int marked = session.markedCount(session.groupId());
  player.sendSystemMessage(
    Component.literal(String.format("[飞剑指挥] 当前标记: %d | 战术: %s",
      marked, session.tactic().displayName().getString())));
}
```

**影响位置**：
- 所有调用 `SwordCommandTUI.open()` 的地方改为调用 `sendFeedback()`

### 5.3 网络同步审计与优化

#### 5.3.1 SynchedEntityData 审计
**位置**: `FlyingSwordEntity.java:50-89`

**当前同步数据**（11 项）：
1. ✅ **OWNER** (UUID) - 必需，决定所有权逻辑
2. ✅ **AI_MODE** (int) - 必需，客户端需要知道模式以播放对应效果
3. ⚠️ **GROUP_ID** (int) - 可选，仅 TUI/Swarm 需要
4. ⚠️ **TARGET** (UUID) - 可选，仅用于客户端渲染朝向
5. ⚠️ **LEVEL** (int) - 可选，仅用于客户端显示
6. ⚠️ **EXPERIENCE** (int) - 可选，仅用于客户端显示
7. ✅ **DURABILITY** (float) - 必需，影响渲染和音效
8. ✅ **SPEED_CURRENT** (float) - 必需，客户端需要同步速度
9. ✅ **DISPLAY_ITEM_STACK** (ItemStack) - 必需，基础渲染需要
10. ⚠️ **MODEL_KEY** (string) - 可选，仅 Gecko 渲染需要
11. ⚠️ **SOUND_PROFILE** (string) - 可选，音效系统需要
12. ⚠️ **SWORD_TYPE** (string) - 可选，用于区分正道/魔道
13. ✅ **IS_RECALLABLE** (boolean) - 必需，影响玩家交互

**优化建议**：
- **不优化**：保持当前同步频率，因为：
  1. 实体同步已经由 Minecraft 优化（默认每 3 tick 同步一次）
  2. 大部分数据变化频率很低（owner/mode/target 等）
  3. SynchedEntityData 自动处理脏标记，只同步变化的数据
  4. 当前没有自定义网络包，所有同步都通过实体同步完成（已经是最优方案）

**结论**：
- ✅ 网络同步已经通过实体同步机制优化
- ✅ 没有额外的自定义数据包
- ✅ 符合 Phase 5 "优先复用实体同步" 的要求
- ⚠️ 未来可考虑将 MODEL_KEY/SOUND_PROFILE 设为客户端本地查询（但需要数据驱动支持）

#### 5.3.2 事件副作用审计
**审计范围**：确保事件副作用通过实体同步实现，避免额外数据包

**结论**：
- ✅ 飞剑系统没有自定义网络消息
- ✅ 所有状态变化通过 `SynchedEntityData` 同步
- ✅ 客户端效果（粒子/音效）通过 `ClientLevel` 判断本地播放

### 5.4 实施总结

**代码变更**：
- `FlyingSwordRenderer.java`: +10 行（添加渲染开关检查）
- `SwordCommandCenter.java`: +30 行（添加 TUI 降级逻辑）

**行为变更**：
- 当 `ENABLE_GEO_OVERRIDE_PROFILE=false` 时：
  - 资源加载器不注册
  - 渲染器跳过 Gecko 查询，直接使用基础渲染
- 当 `ENABLE_TUI=false` 时：
  - 指挥中心输出简化文本消息
  - 不显示复杂的点击式 UI

## 依赖关系
- ✅ 依赖 Phase 0 的功能开关（`ENABLE_GEO_OVERRIDE_PROFILE`, `ENABLE_TUI`）
- ✅ 依赖既有渲染路径（Phase 0/1）

## 验收标准

### 编译测试
```bash
./gradlew compileJava
```
- ✅ 编译通过（待验证）
- ✅ 无新增警告

### 功能测试

**渲染降噪**：
- [ ] `ENABLE_GEO_OVERRIDE_PROFILE=false` 时，飞剑使用基础 Item 渲染
- [ ] `ENABLE_GEO_OVERRIDE_PROFILE=true` 时，Gecko 渲染正常工作
- [ ] 渲染器不会尝试查询未注册的资源加载器

**TUI 降级**：
- [ ] `ENABLE_TUI=false` 时，指令输出简化文本
- [ ] `ENABLE_TUI=true` 时，完整 TUI 正常显示
- [ ] 核心指令功能（标记/执行/切换战术）不受开关影响

**网络同步**：
- [ ] 没有自定义网络包（仅使用实体同步）
- [ ] 多人环境下同步正常，无延迟
- [ ] 服务器日志无网络警告

### 性能测试
- [ ] 默认开关关闭时，客户端渲染帧率无下降
- [ ] 多个飞剑同时存在时，网络流量正常
- [ ] 服务器 TPS 无影响

## 风险与回退

### 风险等级：低

**潜在问题**：
1. **渲染降级后视觉效果变化**
   - **影响**：Gecko 模型用户可能不满意基础渲染
   - **缓解**：开关默认关闭，需要高级渲染的用户可手动开启
   - **回退**：设置 `ENABLE_GEO_OVERRIDE_PROFILE=true`

2. **TUI 降级后操作不便**
   - **影响**：习惯点击式 UI 的用户需要手动输入命令
   - **缓解**：核心功能（标记/执行）不受影响，仍可通过命令使用
   - **回退**：设置 `ENABLE_TUI=true`

**回退方案**：
- 所有改动都是条件编译，可通过开关立即回退
- 默认渲染路径保持不变，兼容性高
- TUI 降级逻辑独立，不影响核心指令功能

## 下一步 (Phase 6)

Phase 5 完成后，Phase 6 将专注于：
- 补充系统文档（systems/README.md, AGENTS.md 等）
- 添加单元测试（Calculator, UpkeepOps, SteeringOps 等）
- 测试覆盖率 ≥ 70%
- 编写扩展示例

---

## 不能做什么（红线约束）
- 不得移除基础渲染路径（Item 渲染必须始终可用）
- 不得在渲染路径中引入阻塞操作或昂贵的计算
- 不得破坏现有的网络同步机制（必须保持与原版实体同步兼容）
- 不得在 TUI 降级时移除核心指令功能（标记/执行/战术切换）

## 要做什么（详细清单）
- ✅ 在 `FlyingSwordRenderer` 中添加 `ENABLE_GEO_OVERRIDE_PROFILE` 开关检查
  - 查询 `SwordModelOverrideRegistry` 和 `SwordVisualProfileRegistry` 前检查开关
  - 确保 Gecko 渲染路径被正确跳过
  - 保持基础 Item 渲染始终可用

- ✅ 在 `SwordCommandCenter` 中实现 TUI 降级逻辑
  - 添加 `sendFeedback(player, session)` 统一反馈入口
  - 添加 `sendMinimalFeedback(player, session)` 简化反馈
  - 将所有 `SwordCommandTUI.open()` 调用替换为 `sendFeedback()`
  - 确保核心指令（标记/执行/战术切换）在任何模式下都能工作

- ✅ 审计网络同步机制
  - 确认没有自定义网络包
  - 确认所有状态变化通过 `SynchedEntityData` 同步
  - 确认事件副作用通过实体同步触发客户端逻辑

