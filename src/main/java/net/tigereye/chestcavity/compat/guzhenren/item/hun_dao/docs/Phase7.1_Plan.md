# Hun Dao Phase 7.1 计划 — Modern UI 严苛整改

## 制定时间
- 2025-11-18

## 背景
Phase 7 引入的 Modern UI 面板虽完成框架与文档，但核心体验严重背离需求：
- 面板内容区仅显示固定占位文本，未渲染魂魄状态/属性/等级/稀有度/上限。
- Tab 切换逻辑未真正驱动内容刷新，`IHunDaoPanelTab.renderContent` 完全未被调用。
- ASCII 设计稿要求的布局/占位符/未激活提示均缺失，易误导玩家。

Phase 7.1 必须以“零容忍”态度补完 UI 数据渲染与交互，确保可发布质量。

## 目标
1. **精准对齐 ASCII 设计**：顶部 Tabs + 内容面板按图展示魂魄字段、属性列表与 FallBack 提示。
2. **数据实时绑定**：内容区基于 `HunDaoClientState` 渲染当前玩家数据，未激活时展示“魂魄系统未激活”。
3. **Tab 渲染落地**：`IHunDaoPanelTab.renderContent()` 必须被调用，切换 Tabs 立刻刷新内容。
4. **严格自检**：包含 UI 截图、命令操作说明、`rg` 检查、`./gradlew compileJava` 验证。

## 范围与要求
### 1. Fragment 结构
- 替换 `TextView("[Tab content renders here]")` 为可绘制容器：
  - 方案 A：自定义 `CanvasView`，在 `onDraw` 中调用 active tab 的 `renderContent(Canvas, ...)`。
  - 方案 B：为内容区构建 `LinearLayout`/`TextView` 栈，由 tab 返回 `View` 实例（任选其一，但必须可实际显示字段）。
- `switchTab()` 调用内容刷新（例如 `contentView.invalidate()` 或替换子 View），禁止 TODO 占位。

### 2. SoulOverviewTab 内容
- 渲染以下字段（带标签、值、占位符）：
  1. 魂魄状态
  2. 魂魄等级
  3. 魂魄稀有度
  4. 魂魄上限
  5. 魂魄属性（至少 3 条，缺失填 `--`）
- 当 `isSoulSystemActive=false`：
  - 顶部显示「魂魄系统未激活」提示区块。
  - 其余字段全部输出与 ASCII 相符的占位文本（`Unknown`、`--`、`无 / -`等）。
- 字段排版遵循单列列表（或表格）风格，配色/间距统一。

### 3. Tab 架构
- `IHunDaoPanelTab` 接口若仍使用 `renderContent(Canvas...)`，必须存在具体 Canvas 实现。
- 若改为返回 `View`，需同步更新现有实现与文档。
- `ReservedTab` 渲染“Coming Soon”/禁用提示，阻止用户误点击。

### 4. 命令/入口
- `/hundaopanel` 打开面板时必须看到真实内容。
- 如需新增热键提示或 HUD 入口，需记录在文档中（可选）。

## 自检清单
1. `./gradlew compileJava` 通过（记录日志路径）。
2. `rg -n "TODO Phase 7" src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/hun_dao/client/modernui` → 0 命中。
3. 面板截图（含魂魄激活、未激活两种状态）附在 `Phase7.1_Report.md`。
4. 交互测试步骤（命令/热键）写入 `smoke_test_script.md`。
5. README 更新：`client/modernui/README.md`、`ui/README.md` 描述最新实现。
6. 从日志确认 `switchTab()` 被调用且 `renderContent()` 执行（可使用 DEBUG log）。

## 交付物
- 代码：`HunDaoModernPanelFragment` 内容区域、Tab 刷新逻辑、`IHunDaoPanelTab`、`SoulOverviewTab` 渲染实现。
- 文档：`Phase7.1_Plan.md`（本文件）、`Phase7.1_Report.md`、README、smoke 测试。
- 测试资产：现代面板截图、命令演示步骤。

## 风险与缓解
1. **Modern UI 渲染兼容性**：若 Canvas 渲染不稳定，可退回到 `LinearLayout + TextView` 方案，但必须符合 ASCII 布局。
2. **数据缺失**：`HunDaoClientState` 若暂无真实数据，仍需通过占位符展示；同时在 Report 中说明需要服务器同步支持。
3. **扩展性**：保持 Tab 架构可扩展，避免硬编码字段，后续可移植到 Phase 8+。
