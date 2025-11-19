# 魂道（hun_dao）重构指南（门面优先）

遵循统一分层，抽离计算与数值，保持可测试与可维护。

## 分层/注册
- behavior→注册/参数；calculator→计算；runtime→副作用；fx/messages→表现；tuning→常量。
- 主动 enum + static；被动 `PassiveBus`/Organ；快照 `ActivationHookRegistry`。

## 依赖
- `MultiCooldown`、`ResourceOps`、`AbsorptionHelper`、`LedgerOps`；统一 `*Ops` 门面。

## 测试
- 仅测 calculator；运行 `./gradlew test`。

## Checkstyle（Hun Dao 模块）

> 2025-Phase8：`net/tigereye/.../hun_dao/` 目录下所有 Checkstyle 告警已清零，可直接作为风格模板使用。

### 导入顺序（必须遵守）

按 4 组分隔，组间空一行：

1. `java.*` / `javax.*`
2. SPECIAL：`net.minecraft.*`、`net.neoforged.*`、`net.tigereye.chestcavity.*`
3. 第三方库：如 `icyllis.modernui.*`、`com.mojang.logging.LogUtils`、`org.slf4j.Logger`
4. `static` import（最后）

> 写新代码时直接参考：  
> `calculator/*`、`behavior/*`、`runtime/*`、`soulbeast/*`、`client/modernui/*`、`combat/HunDaoDamageUtil.java` 的当前导入分组。

### Javadoc 规范（hun_dao 专用约定）

- 所有 public/protected 类与方法必须有 Javadoc。
- Summary（第一句）必须以句号 `.` 或 `。` 结尾。
- render / runtime / state / behavior / storage 方法，摘要一句话必须点明职责。
- 标签顺序：`@param` → `@return` → `@throws` → `@deprecated`。

可直接照抄的模板：
- 状态/存储：`soulbeast/state/*`、`soulbeast/storage/*`。
- 行为：`behavior/*`。
- Runtime/FX：`runtime/*`、`fx/*`。
- UI：`client/modernui/*`。

### 命名与抑制

- Checkstyle 的命名规则默认不允许连续大写缩写。对外 API/接口命名确需保留缩写时，统一使用：
  - 在类型上添加 `@SuppressWarnings("checkstyle:AbbreviationAsWordInName")`。
  - 当前已采用：`IHunDaoPanelTab`、`SoulBeastAPI`。
- 非对外 API（内部实现类/局部变量）应尽量避免新引入大写缩写。

### 变量距离与行长

- 行长上限 100 列，日志字符串或长公式需要：
  - 拆分为多行字符串拼接；或
  - 提取为常量（tuning/ 或私有 static final 字段）。
- 避免「声明很早、使用很晚」：
  - 尽量在第一次使用前一两行声明变量。
  - 必要时改为内联/局部 `final`，示例参见：
    - `ItemBeastSoulStorage#createStoragePayload`
    - `HunDaoModernPanelFragment#onCreateView` 中关闭按钮的 `Minecraft.getInstance()` 调用。

### hun_dao Checkstyle 自检流程（推荐）

1. 修改完 hun_dao 代码后运行：
   - `./gradlew -x compileJava checkstyleMain`
2. 使用日志过滤（如需复查）：
   - `build/reports/checkstyle/main.xml` 中查找 `hun_dao`。
3. 若需要重新生成 hun_dao 专用 log，可复用 Phase8 中的脚本逻辑：
   - 仅筛选 `name` 含 `/hun_dao/` 的 `<file>`/`<error>`，写入 `docs/checkstyle_hun_dao.log`。

## TODO
- [ ] 常量迁出；
- [ ] 计算迁出；
- [ ] FX/提示归口；
- [ ] combo（若有）分离至 combo 目录。
