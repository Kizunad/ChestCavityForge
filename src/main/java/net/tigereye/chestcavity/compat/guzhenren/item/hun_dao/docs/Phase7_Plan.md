# Hun Dao Phase 7 实施计划 — Modern UI 魂道面板

## 制定时间
- 2025-11-18

## 阶段目标
在 Phase 0–6 完成架构、runtime、HUD/通知等基础后，Phase 7 专注于"现代化信息面板"：
1. 基于 Modern UI（或现有 GUI 框架）新增"魂道面板"，作为未来多页扩展的承载容器；
2. 第一版实现单个核心 Tab（Soul Overview），显示魂魄核心信息（状态、属性、等级、稀有度、上限等）；
3. 保持可扩展的 Tab/Section 结构，预留两个 Reserved Tab，后续可按需追加更多面板；
4. 确保仅在玩家具备魂道器官时显示有效数据，否则回退为占位符（No crash, no missing data）；
5. 交付完整的文档、自检与验收记录。

## UI 设计规范

### 面板结构
```
┌─────────────────────────────────────────────────────────────────┐
│           Hun Dao Modern Panel (Phase 7)                        │
├─────────────────┬─────────────────┬─────────────────────────────┤
│ Soul Overview   │   (Reserved)    │        (Reserved)           │
├─────────────────┴─────────────────┴─────────────────────────────┤
│                                                                  │
│  Soul State: Active / Rest / Unknown                            │
│  Soul Level: --                                                 │
│  Soul Rarity: Unidentified                                      │
│  Soul Max: --                                                   │
│                                                                  │
│  Attributes:                                                    │
│    - Attribute 1: --                                            │
│    - Attribute 2: --                                            │
│    - Attribute 3: --                                            │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│  If Soul System is inactive                                     │
│    → Display fallback placeholder                               │
│    → No crash, no missing data                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Tab 架构
1. **Soul Overview** (Phase 7 实现)
   - 魂魄状态、等级、稀有度、上限
   - 魂魄属性列表（可扩展）
   - fallback 占位符支持

2. **Reserved Tab 1** (Phase 8+ 预留)
   - 预留位置，待未来扩展

3. **Reserved Tab 2** (Phase 8+ 预留)
   - 预留位置，待未来扩展

## 功能范围

### 主入口
- Modern UI Panel 顶部 Tabs（3 个 Tab 布局，当前仅激活 "Soul Overview"）
- Tab 切换逻辑（Reserved Tabs 可选择性禁用或显示 "Coming Soon"）

### 核心内容（Soul Overview Tab）

#### 1. 魂魄状态 (Soul State)
- **数据源**：`HunDaoClientState#getSoulState()`
- **可选值**：
  - `Active`（活跃）
  - `Rest`（休眠）
  - `Unknown`（未知）
- **fallback**：若无数据，显示 "Unknown"

#### 2. 魂魄等级 (Soul Level)
- **数据源**：`HunDaoClientState#getSoulLevel()`
- **显示格式**：整数或 "--"
- **fallback**：无数据时显示 "--"

#### 3. 魂魄稀有度 (Soul Rarity)
- **数据源**：`HunDaoClientState#getSoulRarity()`
- **可选值**：
  - `Common`（普通）
  - `Rare`（稀有）
  - `Epic`（史诗）
  - `Legendary`（传说）
  - `Unidentified`（未鉴定）
- **fallback**：无数据时显示 "Unidentified"

#### 4. 魂魄上限 (Soul Max)
- **数据源**：`HunDaoClientState#getHunPoMax()`
- **显示格式**：整数或 "--"
- **fallback**：若 ≤ 0 或无数据，显示 "--"

#### 5. 属性列表 (Attributes)
- **数据源**：`HunDaoClientState#getSoulAttributes()`（返回 Map<String, Object> 或 List<Attribute>）
- **显示格式**：
  ```
  Attributes:
    - Attribute 1: <value>
    - Attribute 2: <value>
    - Attribute 3: <value>
  ```
- **fallback**：无数据时每项显示 "--"
- **扩展性**：支持动态数量的属性（不限于 3 个）

### Fallback 策略
- **魂魄系统未激活**：
  - 显示占位符文本："Soul System is Inactive"
  - 所有字段显示 fallback 值（"--" 或 "Unknown"）
  - **关键要求**：No crash, no missing data

## 任务清单

### 任务 1：Modern UI Shell 架构
- [ ] 创建 `HunDaoModernPanel`（或 `HunDaoModernScreen`）主面板类；
- [ ] 实现顶部 Tab 导航栏，支持 3 个 Tab：
  - `SoulOverviewTab`（激活）
  - `ReservedTab1`（禁用或显示 "Coming Soon"）
  - `ReservedTab2`（禁用或显示 "Coming Soon"）
- [ ] 定义 `IHunDaoPanelTab` 接口：
  ```java
  interface IHunDaoPanelTab {
      String getId();
      String getTitle();
      void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
      boolean isVisible();
      boolean isEnabled();
  }
  ```
- [ ] 集成到现有 UI 启动入口（键位绑定、命令或 Modern UI 菜单）；
- [ ] 确保 Tab 切换高亮显示正确。

### 任务 2：Soul Overview Tab 实现
- [ ] 创建 `SoulOverviewTab` 类，实现 `IHunDaoPanelTab`；
- [ ] 实现数据读取逻辑：
  - 从 `HunDaoClientState` 获取魂魄状态、等级、稀有度、上限；
  - 获取属性列表（Map 或 List）；
- [ ] 实现 UI 渲染：
  - 标题区：显示各字段标签与值；
  - 属性区：循环渲染属性列表；
  - fallback 区：当魂魄系统未激活时显示提示信息；
- [ ] 实现数据格式化：
  ```java
  private String formatSoulState(SoulState state) {
      if (state == null) return "Unknown";
      return state.getDisplayName(); // "Active" / "Rest" / "Unknown"
  }

  private String formatSoulLevel(int level) {
      return level > 0 ? String.valueOf(level) : "--";
  }

  private String formatSoulRarity(SoulRarity rarity) {
      if (rarity == null) return "Unidentified";
      return rarity.getDisplayName();
  }

  private String formatSoulMax(int max) {
      return max > 0 ? String.valueOf(max) : "--";
  }
  ```
- [ ] 排版参考 Modern UI 样式，添加 section 标题与分隔线；
- [ ] 确保布局响应式（支持不同分辨率）。

### 任务 3：数据桥接 & 客户端状态扩展
- [ ] 扩展 `HunDaoClientState` 提供 Panel 所需的 getter：
  ```java
  public Optional<SoulState> getSoulState();
  public int getSoulLevel();
  public Optional<SoulRarity> getSoulRarity();
  public int getHunPoMax();
  public Map<String, Object> getSoulAttributes();
  ```
- [ ] 实现魂魄系统激活检测：
  ```java
  public boolean isSoulSystemActive() {
      // 检查玩家是否具备魂道器官
      return hasHunDaoOrgan();
  }
  ```
- [ ] 确保所有 getter 返回安全的默认值（避免 NPE）；
- [ ] 实现数据同步机制（若需要，通过 Payload 从服务器同步）；
- [ ] 支持惰性更新（避免每 tick 重绘，仅在数据变化时刷新）。

### 任务 4：Fallback & 错误处理
- [ ] 实现魂魄系统未激活时的 fallback UI：
  - 显示提示文本："Soul System is Inactive"
  - 显示 fallback placeholder："→ Display fallback placeholder"
  - 显示安全保证："→ No crash, no missing data"
- [ ] 所有数据字段在无数据时显示占位符（"--" 或 "Unknown" 或 "Unidentified"）；
- [ ] 添加异常捕获，确保 UI 渲染不会因数据问题崩溃；
- [ ] 日志记录：当数据缺失时记录 DEBUG 级别日志。

### 任务 5：Reserved Tabs 占位实现
- [ ] 创建 `ReservedTab` 类（或两个独立类）；
- [ ] 实现占位内容：
  - 显示 "Coming Soon" 或 "Reserved for Future Use"；
  - 可选：显示预期功能的简短描述；
- [ ] Tab 默认禁用状态（点击无响应或显示提示）；
- [ ] 确保架构支持未来轻松替换为实际实现。

### 任务 6：测试与文档
- [ ] 更新 `smoke_test_script.md`：
  - 添加"打开 Modern UI 面板"步骤；
  - 验证 Soul Overview Tab 数据正确显示；
  - 验证 fallback 策略（移除魂道器官后检查）；
  - 验证 Tab 切换功能；
  - 截图：激活状态、未激活状态各一张；
- [ ] 编写 `Phase7_Report.md`：
  - 实施总结；
  - 功能验收结果；
  - 已知问题与未来改进；
  - 截图与日志附录；
- [ ] 更新 README：
  - `client/README.md`：添加 Modern UI 架构说明；
  - `ui/README.md`：添加面板使用指南；
- [ ] 自检：
  - `./gradlew compileJava` 通过；
  - `./gradlew check` 通过（触及文件 lint 干净）；
  - 手动测试：面板打开、数据显示、Tab 切换、fallback 策略。

## 关键要点
1. **可扩展性优先**：Tab 架构必须支持未来轻松添加新面板，遵循开放封闭原则；
2. **健壮性保证**：No crash, no missing data — 所有数据缺失场景必须有 fallback；
3. **数据驱动**：UI 显示内容完全由 `HunDaoClientState` 驱动，禁止硬编码数据；
4. **性能优化**：面板仅在打开时拉取/渲染数据，禁止后台常驻开销；
5. **用户体验**：布局清晰、信息易读、操作流畅，符合 Modern UI 风格；
6. **本地化支持**：当前文案可使用中文/英文混合占位，最终需支持国际化（Phase 7.1 或 8）。

## 技术约束
1. **Modern UI 依赖**：
   - 确认项目内已有可用的 Modern UI 库；
   - 如需新依赖，评估体积/兼容性/许可证；
   - 可选方案：使用原生 Minecraft GUI 框架（降级方案）。

2. **客户端专用**：
   - 面板仅在客户端渲染，不涉及服务器逻辑；
   - 数据通过 `HunDaoClientState` 同步，已在 Phase 6 实现。

3. **兼容性**：
   - 支持 NeoForge 客户端事件；
   - 与现有 HUD/通知系统共存，互不干扰。

## 风险与缓解
| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Modern UI 库不可用 | 无法使用现代化框架 | 降级使用原生 GUI，调整 UI 样式 |
| 数据同步延迟 | 面板显示过期数据 | 添加刷新按钮 + 自动刷新机制 |
| 属性列表动态数量过多 | UI 溢出/卡顿 | 添加滚动条 + 分页 |
| 国际化未实现 | 仅支持单一语言 | Phase 7.1 追加，当前使用占位符 |

## 自检清单（完成时勾选）
- [ ] Modern UI Panel 可通过指定入口（键位/命令）打开；
- [ ] 三个 Tab 正确显示，Soul Overview 为默认激活 Tab；
- [ ] Reserved Tabs 显示占位内容，点击无副作用；
- [ ] Soul Overview Tab 在魂魄系统激活时正确显示数据；
- [ ] Soul Overview Tab 在魂魄系统未激活时显示 fallback placeholder；
- [ ] 所有字段在无数据时显示占位符（"--" / "Unknown" / "Unidentified"）；
- [ ] 属性列表支持动态数量（至少支持 3 个）；
- [ ] `HunDaoClientState` API 提供安全的默认值，无 NPE；
- [ ] `rg -n "TODO Phase 7"` 仅包含明确留给 Phase 7.1+ 的任务；
- [ ] 更新 `smoke_test_script.md` + `client/README.md` + `ui/README.md`；
- [ ] `./gradlew compileJava` 通过；
- [ ] `./gradlew check`（触及文件）通过；
- [ ] 截图附在 `Phase7_Report.md`（激活/未激活各一张）；
- [ ] 手动测试：数据正确、fallback 正确、Tab 切换流畅、无崩溃。

## 交付物
- **代码**：
  - `client/modernui/HunDaoModernPanel.java`
  - `client/modernui/IHunDaoPanelTab.java`
  - `client/modernui/tabs/SoulOverviewTab.java`
  - `client/modernui/tabs/ReservedTab.java`（或 `ReservedTab1/2.java`）
  - `HunDaoClientState.java`（扩展 getter）

- **文档**：
  - `Phase7_Plan.md`（当前文档）
  - `Phase7_Report.md`（实施后编写）
  - `client/README.md`（更新）
  - `ui/README.md`（更新）
  - `smoke_test_script.md`（更新）

- **测试资料**：
  - 面板截图（激活状态）
  - 面板截图（未激活/fallback 状态）
  - 打开流程记录
  - 自检日志

## Phase 7.1+ 展望
- **国际化支持**：i18n 文本资源文件；
- **更多 Tabs**：魂焰管理、魂兽状态、Gui Wu 详情等；
- **交互功能**：点击属性查看详情、刷新按钮、设置选项；
- **美化优化**：图标、颜色主题、动画效果；
- **数据导出**：面板数据导出为 JSON/CSV（调试用）。
