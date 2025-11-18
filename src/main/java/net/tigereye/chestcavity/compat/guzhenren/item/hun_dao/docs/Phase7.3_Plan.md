# Hun Dao Phase 7.3 计划 — Modern UI 国际化与交互完善

## 制定时间
- 2025-11-18

## 背景
Phase 7.2 已实现宽屏布局与数据展示，但仍存在以下短板：
1. **国际化缺失**：所有 UI 文案/枚举字符串硬编码为英文，未接入 `lang` 文件；
2. **enum 显示名**：`SoulState`、`SoulRarity` 通过 `getDisplayName()` 返回英文字符串，难以本地化；
3. **友好提示**：命令按钮、Tab 名称、占位符文本缺少 i18n 支持，无法适配多语言；
4. **交互回执**：面板打开/关闭、Tab 切换、未激活等提示缺少日志或可视反馈，调试困难。

Phase 7.3 需专注于国际化及交互细节，确保面板在中/英环境一致显示，并为未来新增语言奠定基础。

## 目标
1. **全面 i18n**：所有 UI 文案（标题、字段标签、Tab 名、按钮、提示）使用 `Component.translatable(...)` 或 Modern UI 对应 API，`lang/en_us.json` 与 `lang/zh_cn.json` 至少提供翻译。
2. **枚举本地化**：`SoulState`、`SoulRarity` 返回 `TranslationKey`，客户端根据当前语言显示。
3. **提示占位符**：未激活提示、属性占位符、命令说明等均提供多语言文本。
4. **交互反馈**：Tab 切换、面板创建/销毁输出 DEBUG 日志或 UI 提示，便于测试与 QA。

## 范围与任务
### 1. 语言文件
- [ ] `src/main/resources/assets/chestcavity/lang/en_us.json` 增加 Modern UI 相关 key（示例见下）。
- [ ] `src/main/resources/assets/chestcavity/lang/zh_cn.json` 提供中文翻译。
- [ ] 提前预留 Phase 8+ 可能使用的 key（如 `tab.hun_dao.soul_flame` 等）。

**示例 Key：**
```
screen.hun_dao.title = Hun Dao Modern Panel
screen.hun_dao.button.close = Close Panel
tab.hun_dao.soul_overview = Soul Overview
tab.hun_dao.reserved = Reserved
hud.hun_dao.soul_state = Soul State
hud.hun_dao.soul_level = Soul Level
hud.hun_dao.soul_rarity = Soul Rarity
hud.hun_dao.soul_max = Soul Max
hud.hun_dao.attributes = Attributes
hud.hun_dao.system_inactive = Soul system is inactive.
hud.hun_dao.placeholder = --
```
(中文对照在 `zh_cn.json` 中给出)

### 2. 枚举更新
- [ ] `SoulState`/`SoulRarity` 修改为存储 `translationKey`，`getDisplayName()` 返回 `Component.translatable(key)` 或供 UI 调用。
- [ ] 在 README 中记录枚举 key 命名规则。

### 3. UI 代码改造
- [ ] `HunDaoModernPanelFragment`、`SoulOverviewTab`、`ReservedTab` 等位置全部替换硬编码字符串为 `context.getString(R.string.xxx)` 或 `Component.translatable(...)`。
- [ ] 属性列表占位符（Attribute 1/2/3）使用 `hud.hun_dao.attribute.placeholder.{index}` 或统一 key。
- [ ] 未激活提示：展示多语言文本，并在必要时显示“如何激活”的说明。

### 4. 交互反馈
- [ ] `switchTab` 时打印 DEBUG 日志包含 tab id/title。
- [ ] 面板打开/关闭在日志中输出（XX 打开 Hun Dao Panel），便于排查。
- [ ] 可选：使用 Modern UI Toast/提示条告知“数据尚在同步”等信息。

### 5. 文档 & 测试
- [ ] 更新 `client/modernui/README.md`（描述 i18n 方案、key 列表）。
- [ ] `Phase7.3_Report.md` 记录所有 key 及翻译校验。
- [ ] smoke 测试脚本添加“切换语言 -> 打开面板 -> 检查译文”步骤。

## 自检清单
- [ ] `rg -n "\"Hun Dao\"" src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/hun_dao/client/modernui` 仅剩不可翻译文本（例如日志 tag）。
- [ ] `./gradlew compileJava` 通过。
- [ ] 切换 `en_us` 与 `zh_cn` 语言，面板截图对比所有字段翻译。
- [ ] README 与 `Phase7.3_Report.md` 更新完成。
- [ ] `SoulState/SoulRarity` 不再返回硬编码字符串。

## 交付物
- 代码：枚举 + UI 类的 i18n 改造、日志增强。
- 资源：`en_us.json`、`zh_cn.json` 新增条目。
- 文档：`Phase7.3_Plan.md`（本文件）、`Phase7.3_Report.md`、README 更新、smoke 测试步骤。
- 测试资料：双语言截图、命令与切换语言步骤。

## 风险与缓解
1. **翻译遗漏**：可使用脚本 `rg "\"hud.hun_dao"` 检查 key，确保两种语言均存在。
2. **Modern UI 国际化 API 限制**：若无法直接使用 `Component.translatable`，需在 fragment 中转成 plain string；确保不破坏先前布局。
3. **日志噪音**：DEBUG 级别输出，默认 `INFO` 不显示，避免污染日志；文档中说明如何开启。
