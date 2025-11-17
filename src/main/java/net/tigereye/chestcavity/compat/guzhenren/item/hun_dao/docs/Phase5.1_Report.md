# Hun Dao Phase 5.1 修复报告

## 修复概述
Phase 5 merge 后审查发现两项阻断性缺陷，现已全部修复。

## 问题描述

### 问题 1: FX Registry 服务器侧未初始化
**症状**：
- `HunDaoFxInit.init()` 仅在 `HunDaoClientAbilities.onClientSetup` 中调用（客户端专用）
- 服务器侧 `HunDaoFxRegistry` 始终为空
- `HunDaoFxRouter.dispatch()` 在服务器上找不到 FX 模板，直接返回 false
- 魂兽/鬼雾/魂魄 FX 实际不会在服务器触发

**根本原因**：
FX 初始化只注册到客户端生命周期钩子，dedicated server/集成服务器的逻辑侧从未调用 `HunDaoFxInit.init()`。

### 问题 2: 客户端事件总线未注册
**症状**：
- `HunDaoClientEvents` 类定义了 `@SubscribeEvent` 方法
- 类本身没有 `@EventBusSubscriber` 注解
- 未通过 `NeoForge.EVENT_BUS.register()` 手动注册
- `onClientTick` 和 `onLevelUnload` 永远不会被调用
- `HunDaoClientState.tick()` 不执行，HUD/通知状态无法衰减
- 维度切换时缓存无法清空

**根本原因**：
事件处理器定义正确但未挂载到 NeoForge 事件总线。

## 修复方案

### 修复 1: 服务器 FX 初始化
**文件**：`src/main/java/net/tigereye/chestcavity/guzhenren/GuzhenrenModule.java`

**修改位置**：`GuzhenrenModule.initialiseCompat()` (line 164-165)

```java
private static void initialiseCompat() {
  if (ChestCavity.LOGGER.isDebugEnabled()) {
    ChestCavity.LOGGER.debug("[compat/guzhenren] installing compatibility hooks");
  }
  OrganRetentionRules.registerNamespace(MOD_ID);
  Abilities.bootstrap();
  GuzhenrenIntegrationModule.bootstrap();
  GuzhenrenOrganScoreEffects.bootstrap();
  GuzhenrenNetworkBridge.bootstrap();
  GuScriptModule.bootstrap();
  // Phase 5.1 fix: Initialize Hun Dao FX templates on both client and server
  net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.fx.HunDaoFxInit.init();
}
```

**效果**：
- `initialiseCompat()` 在客户端和服务器都会执行（通过 `GuzhenrenModule.bootstrap()` 调用）
- `HunDaoFxInit.init()` 内置重复调用保护（`initialized` 标志）
- 服务器和客户端的 `HunDaoFxRegistry` 都会包含完整的 FX 模板集
- `HunDaoFxRouter.dispatch()` 可以正常查找并调度 FX

### 修复 2: 客户端事件注册
**文件**：`src/main/java/net/tigereye/chestcavity/guzhenren/GuzhenrenModule.java`

**修改位置**：`GuzhenrenModule.installForgeListeners()` (lines 146-150)

```java
private static void installForgeListeners(IEventBus forgeBus) {
  forgeBus.addListener(JianYingGuEvents::onServerTick);
  // 领域系统 tick（统一调度）
  forgeBus.addListener(
      net.tigereye.chestcavity.compat.guzhenren.domain.DomainEvents::onServerTick);
  forgeBus.addListener(GuzhenrenResourceEvents::onPlayerLoggedIn);
  forgeBus.addListener(GuzhenrenResourceEvents::onPlayerRespawn);
  forgeBus.addListener(GuzhenrenResourceEvents::onPlayerClone);
  forgeBus.addListener(GuzhenrenResourceEvents::onPlayerChangedDimension);
  if (FMLEnvironment.dist.isClient()) {
    // 通用领域 PNG 渲染（AFTER_PARTICLES 阶段）
    forgeBus.addListener(
        net.tigereye.chestcavity.compat.guzhenren.domain.client.DomainRenderer::render);
    forgeBus.addListener(
        (ClientTickEvent.Post event) -> PlayerSkinSyncClient.onClientTick(event));
    // Hun Dao client events (Phase 5.1 fix: register client tick and level unload handlers)
    forgeBus.addListener(
        net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.HunDaoClientEvents::onClientTick);
    forgeBus.addListener(
        net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.HunDaoClientEvents::onLevelUnload);
  }
}
```

**效果**：
- `HunDaoClientEvents.onClientTick` 每个客户端 tick 执行
- `HunDaoClientEvents.onLevelUnload` 在维度卸载时执行
- `HunDaoClientState.tick()` 正常运行，状态衰减生效
- 维度切换时缓存正确清空
- HUD 和通知状态可以正常更新

## 验证自检

### 编译验证
- ✅ 修改的代码语法正确，无编译错误
- ✅ 使用的 API 与现有代码模式一致（参考其他 DAO 的事件注册）

### 逻辑验证
- ✅ `HunDaoFxInit.init()` 在公共路径调用，客户端和服务器都会执行
- ✅ `HunDaoFxInit.init()` 有重复调用保护，多次调用安全
- ✅ 客户端事件在 `FMLEnvironment.dist.isClient()` 保护下注册
- ✅ 事件方法签名与 NeoForge API 匹配

### 代码一致性
- ✅ 修复方式与现有 Guzhenren 模块的其他事件注册模式一致
- ✅ 注释清晰标注了 Phase 5.1 修复
- ✅ 未引入新的依赖或架构变更

## 预期运行时行为

### 服务器侧
1. 启动时 `GuzhenrenModule.bootstrap()` 执行
2. `initialiseCompat()` 调用 `HunDaoFxInit.init()`
3. 服务器日志显示：`[hun_dao][fx_init] Registered N FX templates`
4. `HunDaoFxRegistry.size() > 0` 返回 true
5. 魂焰/魂兽/鬼雾效果可以在服务器触发，客户端接收网络包播放

### 客户端侧
1. 启动时 `GuzhenrenModule.bootstrap()` 执行
2. `initialiseCompat()` 首次调用 `HunDaoFxInit.init()`
3. `HunDaoClientAbilities.onClientSetup` 再次调用 `HunDaoFxInit.init()`（被重复调用保护拦截）
4. `installForgeListeners()` 注册 `HunDaoClientEvents` 到 NeoForge EVENT_BUS
5. 每个 tick `onClientTick` 被调用，`HunDaoClientState.tick()` 执行
6. 维度切换时 `onLevelUnload` 被调用，缓存清空

## 后续建议

### 测试建议
1. **FX 测试**：装备小魂蛊，攻击实体，确认魂焰 DoT 有粒子效果和声音
2. **魂兽测试**：激活魂兽化，确认变身特效和环境粒子播放
3. **鬼雾测试**：激活鬼雾，确认迷雾粒子持续播放
4. **HUD 测试**：触发魂魄显示，等待几秒确认 HUD 自动消失
5. **维度测试**：切换维度确认客户端状态重置

### 文档更新
- ✅ `P5.1.md` 已记录修复计划
- ✅ `Phase5.1_Report.md` 已记录修复实施与验证
- 建议在 `Phase5_Report.md` 末尾追加 5.1 修复摘要

### 代码清理（可选）
- 考虑在 `HunDaoFxInit` 中添加日志，在重复调用时输出 debug 信息
- 考虑为 `HunDaoClientEvents` 添加 JavaDoc 说明其生命周期管理

## 总结

Phase 5.1 修复已完成，解决了：
1. ✅ FX 模板服务器侧未初始化问题
2. ✅ 客户端事件总线未注册问题

修复方式：
- 在公共初始化路径调用 `HunDaoFxInit.init()`
- 在 NeoForge 客户端事件总线注册 `HunDaoClientEvents` 事件处理器

预期效果：
- 魂道 FX 系统在服务器和客户端都正常工作
- 客户端状态管理（HUD/通知衰减、维度切换清理）正常运行

交付物：
- ✅ 修复代码（`GuzhenrenModule.java`）
- ✅ 修复报告（`Phase5.1_Report.md`）
- ✅ 修复计划（`P5.1.md`）
