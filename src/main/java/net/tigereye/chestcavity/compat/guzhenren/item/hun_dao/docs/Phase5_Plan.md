# Hun Dao Phase 5 实施计划

## 制定时间
- 2025-11-18

## 阶段目标
Phase 5 关注 FX / Client / UI 与服务端中间件的彻底解耦，使魂道客户端表现与 `jian_dao` 架构对齐：
1. 建立独立的 FX 子系统（路由、调参、数据驱动），实现数据化的特效/音效调度；
2. 搭建客户端能力注册与事件处理层，清理 `HunDaoClientAbilities` 中的冗余逻辑；
3. 提供基础 UI 组件（魂魄 HUD / 状态提示）与客户端状态同步机制；
4. 将 `HunDaoMiddleware` 内所有 FX/客户端提示拆出，仅保留服务端逻辑，并完成文档+自检。

## 任务清单

### 任务 1：FX 子系统落地
- [ ] 在 `hun_dao/fx/` 下创建：
  - `HunDaoFxRouter`：集中派发音效、粒子、动画；
  - `HunDaoFxRegistry` / `HunDaoFxDescriptors`：声明可用 FX ID、粒子模板；
  - `fx/tuning/HunDaoFxTuning`（或整合到 `tuning/`）存放持续时间、颜色、音量等参数；
- [ ] 将魂焰、魂兽化、Gui Wu 相关 FX 统一注册在 Router，通过数据对象（POJO）驱动；
- [ ] 提供服务器调用入口（如 `HunDaoFxOps`）→ Router → 客户端的事件链；
- [ ] 更新 `calculator/README.md` / `fx/README.md` 说明调用顺序和扩展方法。

### 任务 2：客户端能力与事件架构
- [ ] 在 `client/` 内新增/扩展：`HunDaoClientRegistries`、`HunDaoClientEvents`、`HunDaoClientSyncHandlers`；
- [ ] 拆解 `HunDaoClientAbilities`：仅保留能力注册，具体逻辑分发到 FX/UI 层；
- [ ] 通过 NeoForge 客户端事件（`ClientTickEvent`, `RenderGuiEvent` 等）监听魂道状态，调用 Router；
- [ ] 设计 `client/runtime/HunDaoClientState`，缓存魂焰 DOT、魂兽形态等客户端所需状态；
- [ ] 更新 `client/README.md`，记录 Phase 5 架构与 Phase 6 展望。

### 任务 3：UI / HUD 与提示
- [ ] 在 `ui/` 包下实现 `HunDaoSoulHud`（或 `HudOverlay`），展示魂魄、魂焰堆栈、魂兽计时；
- [ ] 增加 `ui/notifications/HunDaoNotificationRenderer`，响应服务器推送的提示事件；
- [ ] 通过 `HunDaoRuntimeContext` + 新的 `HunDaoClientSyncPayload` 同步必要指标；
- [ ] 复用 `HunDaoBehaviorContextHelper` 中的日志/格式化逻辑，确保 UI 层信息来源一致；
- [ ] 将原本在 `HunDaoMiddleware` 或行为类内的客户端提示/聊天信息迁移到 UI 层。

### 任务 4：中间件清理与文档/测试
- [ ] 审核 `HunDaoMiddleware`、`HunDaoClientAbilities`、`HunDaoDamageUtil` 等文件，移除残留的客户端/FX 代码；
- [ ] 补充 `runtime/README.md`、`client/README.md`、`fx/README.md`、`ui/README.md` 与 `Phase5_Report.md`；
- [ ] 更新 `smoke_test_script.md`：新增“FX 触发”、“HUD 同步”检查步骤；
- [ ] 若联机/客户端测试无法自动化，记录手动步骤（含截图建议位置）；
- [ ] 确认 Checkstyle 通过并执行 `./gradlew :ChestCavityForge:compileJava`；如条件允许，运行 `./gradlew check`。

## 关键要点
1. **服务端-客户端职责分离**：服务端只负责发出 FX/状态事件，所有渲染逻辑放在 `client/` & `fx/`。
2. **数据驱动**：FX/提示参数统一来自 `tuning/` 或 `fx/tuning/`，禁止散落常量。
3. **兼容性**：保持与 Phase 0 冒烟脚本功能等价，任何可见特效需手动验证表现一致或更好。
4. **依赖倒置**：客户端也依赖接口/数据对象，而不是直接访问服务端实现（如 `HunDaoMiddleware`）。
5. **KISS/YAGNI**：仅实现现有魂道功能所需 FX/UI，不预先研发 Phase 6+ 的拓展。

## 自检清单
- [ ] `rg -n "playSound" src/main/java/.../hun_dao/middleware` → 0 命中（或仅保留服务端日志）；
- [ ] `rg -n "spawnParticles" src/main/java/.../hun_dao/middleware` → 0 命中；
- [ ] `rg -n "HunDaoFxRouter"` 显示所有调用点集中在 client/fx/combat hooks；
- [ ] `./gradlew :ChestCavityForge:compileJava` 成功（必要时 `./gradlew check`）；
- [ ] Smoke：魂焰命中、魂兽化、Gui Wu、魂魄泄露四个场景，均产生预期 FX/HUD 更新并记录截图/日志；
- [ ] 手动验证 HUD 与服务器状态同步（断线/重连后状态正确）。

## 交付物
- `fx/`、`client/`、`ui/` 新增/更新的实现（Router、Registry、HUD 等）；
- `tuning/` 或 `fx/tuning/` 中的 FX 配置；
- 更新后的 `HunDaoMiddleware`、`HunDaoClientAbilities`、`smoke_test_script.md`；
- 文档：`calculator/README.md`（如引用）、`fx/README.md`、`client/README.md`、`ui/README.md`、`Phase5_Report.md`；
- 自检与烟测记录。
