# Hun Dao Phase 7 计划 — Modern UI 面板

## 制定时间
- 2025-11-18

## 阶段目标
在 Phase 0–6 完成架构、runtime、HUD/通知等基础后，Phase 7 专注于“现代化信息面板”：
1. 基于 Modern UI（或现有 GUI 框架）新增“魂道面板”，作为未来多页扩展的承载容器；
2. 第一版仅实现单个基础 Tab，显示核心魂魄信息（状态、属性、等级、稀有度、上限等）；
3. 保持可扩展的 Tab/Section 结构，后续可按需追加更多面板；
4. 确保仅在玩家具备魂道器官时显示有效数据，否则回退为占位符；
5. 交付完整的文档、自检与验收记录。

## 功能范围
- **主入口**：Modern UI Panel 顶部 Tabs（至少包含“魂道”默认页，保留扩展槽位）。
- **核心内容**（基础面板）：
  - 魂魄状态（占位文本：例如“活跃/休眠/未知”）；
  - 魂魄属性条目（可先列出占位键值对）；
  - 魂魄等级（数值/占位“--”）；
  - 魂魄稀有度（Common/Rare/Epic 或占位）；
  - 魂魄上限（数值；若无数据显示 “无 / -”）。
- **框架要求**：
  - 面板数据读取 `HunDaoClientState`（若无数据则 fallback）。
  - Tab 架构使用接口/枚举定义，方便未来添加更多子面板。
  - 面板 UI 与游戏风格一致，遵守 KISS/SOLID。

## 任务拆解
1. **Modern UI Shell**
   - [ ] 设计 `HunDaoModernPanel`（或 `HunDaoModernScreen`），顶端渲染 Tabs。
   - [ ] 提供 `IHunDaoPanelTab` 接口（id、标题、renderContent()、isVisible()）。
   - [ ] 默认注册 `SoulOverviewTab`，其余 Tab 作为空实现占位。
   - [ ] 集成到现有 UI 启动入口（键位/命令或 `ModernUI` 菜单）。

2. **基础面板实现**
   - [ ] `SoulOverviewTab` 读取客户端状态：hun po 当前/上限、魂兽/鬼雾状态等；
   - [ ] 将缺失数据渲染为 “无 / -”；
   - [ ] 输出字段：
        | Label    | 数据源/占位 |
        | 状态     | `HunDaoClientState#getSoulState()` (占位 -> “未知”) |
        | 属性     | 预留 Map/List（可展示示例键值，例如“魂焰抗性: -”） |
        | 等级     | 占位 `--` |
        | 稀有度   | 占位 “未鉴定” |
        | 上限     | `hunPoMax`，若 <=0 显示 “无 / -” |
   - [ ] 排版参考 Modern UI 样式，加入 section 标题与分隔线。

3. **数据桥接 & 占位策略**
   - [ ] 扩展 `HunDaoClientState`（若需要）提供 Panel 所需的 getter（返回 Optional/默认值）；
   - [ ] 若玩家无魂道器官，整面板显示“魂魄系统未激活”提示；
   - [ ] Tab 切换与数据刷新需支持惰性更新（避免每 tick 重绘）。

4. **测试与文档**
   - [ ] 更新 `smoke_test_script.md`：加入“Modern UI 面板打开/数据展示”步骤；
   - [ ] `Phase7_Plan.md` 对应实施完后需编写 `Phase7_Report.md`；
   - [ ] README：在 `client/README.md` 与 `ui/README.md` 增加 Modern UI 架构说明；
   - [ ] 自检：`./gradlew compileJava`、`./gradlew check`（触达文件 lint 干净）；截图面板效果。

## 风险与约束
1. **Modern UI 依赖**：确认项目内已有可用的 Modern UI Hook；如需新依赖，需评估体积/兼容性。
2. **扩展性**：Tab 实现必须遵循开放封闭原则，避免未来新增面板需要重写结构。
3. **性能**：面板仅在打开时拉取/渲染数据，禁止后台常驻开销。
4. **本地化**：当前文案可先使用中文/英文混合占位，最终需支持国际化（TODO Phase7?7+?  maybe future; ensure note)

## 自检清单（完成时勾选）
- [ ] Modern UI Panel 可通过指定入口打开，Tab 高亮切换正常；
- [ ] `SoulOverviewTab` 在有/无魂道数据时显示不同占位；
- [ ] `HunDaoClientState` API 提供默认值，避免 NPE；
- [ ] `rg -n "TODO Phase 7"` 仅包含明确留给 Phase7.x 的任务；
- [ ] 更新 smoke script + README；
- [ ] `./gradlew compileJava` & `./gradlew check`（触及文件）通过；
- [ ] 截图/日志附在 `Phase7_Report.md`。

## 交付物
- 代码：`client/modernui/*`, `ui/modern_panel/*`, `HunDaoClientState` 扩展；
- 文档：`Phase7_Plan.md`（当前）、`Phase7_Report.md`（后续）、README、smoke 测试更新；
- 测试资料：面板截图 + 打开流程记录。
