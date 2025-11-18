# Hun Dao Phase 7.2 计划 — Modern UI 宽度/布局整改

## 制定时间
- 2025-11-18

## 背景
Phase 7.1 聚焦内容渲染与数据绑定，但面板仍沿用“狭长条”布局，与 ASCII 设计示例的宽屏面板严重不符：
- 顶部标题与 Tab 过于紧凑，缺少左右留白。
- 内容区垂向堆叠，无法形成 ASCII 图所示的矩形面板比例（宽 > 高）。
- 视觉层级缺少分隔线 / 分区卡片，信息密集度与可读性不足。

Phase 7.2 需将 UI 结构重塑为接近 ASCII 图示的宽屏面板，同时保留 Phase 7.1 的数据功能。

## 目标
1. **面板尺寸**：固定宽度约 420–480 dp、内容区域高度约 280–320 dp，形成“横向较宽”的主框。
2. **布局对齐 ASCII**：
   - 顶部：标题 + Tab 行占据整宽，Tab 按钮均匀分布。
   - 中央：信息内容在方形/矩形框内展示（魂魄字段 + 属性列表）。
   - 底部：未激活提示/帮助信息独立区域。
3. **视觉层级**：使用卡片、分隔线或背景色块区分各 Section；保持 KISS/KISS。
4. **响应式**：保持最小缩放/最大宽度约束，避免超大屏幕时过度扩张或小屏幕时溢出。

## 任务拆解
### 1. 容器调整
- [ ] 将 `HunDaoModernPanelFragment` 根布局改为固定宽度的 `FrameLayout` + `LinearLayout` 组合，限制 `maxWidth`，并在中心居中渲染。
- [ ] 内容区域改为 `RoundedRectangle` 背景卡片，内部采用 `LinearLayout` 或自定义 ViewGroup 以实现表格效果。
- [ ] Tab 栏位改用 `Weighted` 布局或 `ModernUI` FlexBox，使三个 Tab 平分宽度，保持与 ASCII 对齐。

### 2. 内容排版
- [ ] 魂魄字段区：使用两列布局（Label/Value），或等宽文本行，形成整齐列表。
- [ ] 属性列表区：采用网格或列表，但需控制宽度，避免挤压。
- [ ] 未激活提示区：独立卡片（浅色背景 + 描述），放在内容区下方。

### 3. 组件复用
- [ ] 若 Phase 7.1 引入 Canvas 渲染，可新增 `SoulOverviewView`，内部定义 `measure/layout/draw`，确保宽度受控。
- [ ] Tab 切换保持现有逻辑，只需更新 `renderContent()` 实际输出的 View/Canvas。

### 4. 美术要求
- 颜色/阴影与现代 UI 一致（深色背景 + 高亮描边）。
- 参考 ASCII 图在内容区加入细分线或标题条（例如 “Attributes” 分隔线）。
- 预留空间用于未来插入图标/图示。

## 自检清单
- [ ] `/hundaopanel` 打开后面板宽度固定，截图对比 ASCII（附两张：默认/未激活）。
- [ ] 检查 1440×900 / 1920×1080 / 2560×1440 下布局保持比例。
- [ ] `./gradlew compileJava` 通过。
- [ ] `rg -n "TODO Phase 7.2"` → 0 未完成项（若需留 TODO，必须标注 Phase 8）。
- [ ] README（`client/modernui/README.md`）更新新的布局描述及截图摘要。
- [ ] `Phase7.2_Report.md` 记录改动、截图、测试步骤。

## 交付物
- 代码：`HunDaoModernPanelFragment` 布局/样式、Tab 组件、`SoulOverviewTab` 渲染视图等。
- 文档：`Phase7.2_Plan.md`（本文件）、`Phase7.2_Report.md`、README、smoke 脚本补丁。
- 测试：截图（含分辨率信息）、命令操作记录。

## 风险与缓解
1. **Modern UI 尺寸限制**：若框架无法固定 dp 宽度，可使用 `LayoutParams` + `setMinimumWidth` + `setMaximumWidth` 组合，或自定义 `ConstraintLayout`。
2. **小屏幕兼容**：在小窗口/低分辨率下自动缩放字体或启用滚动；必要时添加“紧凑模式”切换。
3. **未来 Tab 扩展**：保证 Tab 栏适应更多 Tab（可滚动或折叠），避免锁死 3 个按钮布局。
