# Hun Dao Phase 6 实施计划

## 制定时间
- 2025-11-18

## 阶段目标
在 Phase 0–5（含 5.1 修复）完成的结构基础上，Phase 6 聚焦“验收与体验”——将 HUD/FX/同步落地到可发布品质，并完成全链路测试与报告：
1. 完成 HUD/通知的实际渲染实现，确保魂魄/魂焰/魂兽状态直观可见；
2. 接入 NeoForge 网络 Payload，保证 FX/HUD 数据在 C/S 间实时同步；
3. 编写并执行冒烟测试 + 自动化测试，输出验收报告；
4. 对 runtime/storage/calculator/fx/client/ui 文档和 smoke 脚本进行终稿更新。

## 任务清单

### 任务 1：HUD & 通知渲染落地
- [ ] 在 `HunDaoSoulHud` 中实现具体绘制逻辑：
  - 魂魄条：位置、渐变、数值显示；
  - 魂兽计时器、Gui Wu 剩余时间；
  - 魂焰堆栈指示（含 Crosshair 目标探测）；
- [ ] 在 `HunDaoNotificationRenderer` 中完成 Toast 样式绘制，支持淡入/淡出、分类配色；
- [ ] 将 HUD / 通知注册到 `RenderGuiEvent.Post` 或 `RegisterGuiOverlaysEvent`，确保渲染顺序正确；
- [ ] 添加最小化配置开关/键位（例如 `render_hun_dao_hud` gamerule 或 client config）。

### 任务 2：客户端同步与网络 Payload
- [ ] 定义 `HunDaoClientSyncPayloads`（或复用 ChestCavity 网络层）并实现序列化：
  - `SoulFlameSyncPayload`、`SoulBeastSyncPayload`、`HunPoSyncPayload`、`GuiWuSyncPayload`；
  - Clear 事件（单实体/全量）；
- [ ] 在服务器触发点（middleware/runtime/schedulers）发送 Payload；
- [ ] 在客户端使用 `HunDaoClientSyncHandlers` 消化 Payload，写入 `HunDaoClientState`；
- [ ] 考虑带宽与触发频率（合并事件、抗抖动）。

### 任务 3：FX/HUD/同步集成测试
- [ ] 更新 `smoke_test_script.md`：加入 HUD/FX/同步检查步骤；
- [ ] 编写可选自动化断言（如低成本 `ClientSimulationTest` 或 `GuiTest`）；
- [ ] 在本地运行 `./gradlew compileJava`、`./gradlew check`（若可行）；
- [ ] 记录游戏内测试：截图或日志说明魂焰、魂兽、Gui Wu、魂魄泄露等场景效果；
- [ ] 将测试结果整合进 `Phase6_Report.md`。

### 任务 4：文档与收尾
- [ ] 更新 `runtime/README.md`、`storage/README.md`、`calculator/README.md`、`fx/README.md`、`client/README.md`、`ui/README.md` 以反映最终架构；
- [ ] 汇总 Phase 0–6 计划/报告、烟测记录，形成 `Phase6_Acceptance.md`；
- [ ] 清理临时 TODO / placeholder 注释（若未实现则标记 Phase7）；
- [ ] 确认 `HunDao_Rearchitecture_Plan.md` 的所有阶段项已勾选或追加 Phase7 说明。

## 关键要点
1. **功能等价 + 体验提升**：HUD/FX 必须与原魂道机制行为一致，同时提升可读性；
2. **性能与带宽**：FX/HUD 更新需考虑限频，避免每 tick 大量同步；
3. **可配置**：提供客户端选项或 gamerule 允许关闭 HUD/FX（低配兼容）；
4. **文档完备**：所有 README/smoke/test 报告需对齐最终实现，便于移交；
5. **自检严谨**：保持 0 容忍，所有 Checkstyle、`rg` 自检项必须通过。

## 自检清单
- [ ] `rg -n "TODO Phase 6" src/main/java/.../hun_dao` → 0 命中（或注明 Phase 7）；
- [ ] `rg -n "send" src/main/java/.../hun_dao/middleware` 确认 payload 发送点覆盖魂焰/魂兽/GW/HunPo；
- [ ] 客户端日志中 `HunDaoClientState.tick()` 每 tick 运行；
- [ ] `./gradlew compileJava`、`./gradlew check` 通过；
- [ ] Smoke 测试：魂焰 DOT、鬼雾、魂兽化、魂魄泄露四个场景的 FX/HUD 截图或日志；
- [ ] `Phase6_Report.md` 中列出所有测试与自检结果。

## 交付物
- `client/`：HUD/通知渲染实现、事件注册；
- `network/`（若新增）：Payload 定义与注册；
- `middleware/runtime`：同步触发点更新；
- `docs/`: `Phase6_Plan.md`、`Phase6_Report.md`、`Phase6_Acceptance.md`、更新后的 READMEs 与 smoke 脚本；
- 测试资产：日志、截图、命令记录。
