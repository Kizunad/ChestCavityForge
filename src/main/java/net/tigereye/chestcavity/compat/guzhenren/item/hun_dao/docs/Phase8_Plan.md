# Phase 8 计划 —— Hun Dao Checkstyle 零告警

## 1. 目标

- 清空 `net.tigereye.chestcavity.compat.guzhenren.item.hun_dao` 目录下 **全部** Checkstyle 告警（当前 701 条，详见 `Checkstyle_HunDao.md` & `checkstyle_hun_dao.log`）。
- 复用 `jian_dao` 架构规范，确保包名/导入/Javadoc/行长等风格完全一致，可长期维持。
- 验证 `./gradlew checkstyleMain` 在 hun_dao 触达文件上 0 告警，并更新文档/脚本/报告。

## 2. 范围与优先级

| 优先级 | 问题簇 | 说明 |
| --- | --- | --- |
| P1 | Import 顺序 | 323 条；执行 IDE 格式化或 `spotlessApply`（若不可行则手动），并在 PR 前 re-check。|
| P1 | Javadoc 缺失 | 140 (method) + 111 (summary) + 5 (type)，需编写模板+脚本（可 consider snippet reuse）。|
| P2 | 行长/空行/变量距离等 | 31 条；在主功能完成后逐文件清理。|

> 包名告警不在本阶段范围内，重点聚焦 import/Javadoc/行长等可控问题。

## 3. 详细步骤

1. **基线确认**
   - 重跑 `./gradlew checkstyleMain` 并刷新 `docs/checkstyle_hun_dao.log`。
   - 将日志与 `Checkstyle_HunDao.md` 更新到 Phase8 分支。
2. **Import 策略**
   - 制定统一流程：先运行 `IDE optimize imports`，再人工对照 `config/checkstyle/checkstyle.xml` 的 `CustomImportOrder` 要求，确保顺序为 `java.*` → `javax.*` → 第三方 (`org.*`, `com.*`, 其他 mod) → `net.minecraft.*` → `net.tigereye.*`，不同组之间必须空一行。
   - 在每个子包（behavior/soulbeast/…) 批量整理 import，禁止仅插入空行或注释，否则会像合并提交 `08e72ac` 那样引入更多 `CustomImportOrderCheck` 告警。
   - 推荐流程：
     1. `rg -l "CustomImportOrderCheck" docs/checkstyle_hun_dao.log` 找出文件。
     2. 逐个文件执行 IDE 排序 + 手动核查，提交前再跑 `./gradlew checkstyleMain` 确认零新增 warning。
3. **Javadoc 批处理**
   - 制定 `Hun Dao API` Javadoc 模板（单句摘要 + `@param`/`@return`）并记录在 docs。
   - 先覆盖公共接口/抽象基类，再处理行为/FX/客户端 tab。
4. **杂项规则**
   - 按 log 定位超长行/空行/变量作用域问题。
   - 对 `PackageNameCheck` 以外 residual 逐条消灭。
5. **自检**
   - `./gradlew checkstyleMain` & `./gradlew compileJava`。
   - `rg -n "TODO|FIXME"` 确认无新增临时代码。
   - 更新 `smoke_test_script.md`（新增“Checkstyle 零告警”步骤）。
6. **文档与报告**
   - 编写 `Phase8_Report.md` + `Phase8_Acceptance.md`。
   - 如需中途插入 P8.x，沿用 `Phase8.1_Plan.md` 约定。

## 4. 交付物

- `docs/Checkstyle_HunDao.md` + `checkstyle_hun_dao.log`（实时更新）。
- 所有 hun_dao 源文件 & JSON/资源（如包名改动涉及）
- `Phase8_Report.md` / `Phase8_Acceptance.md` / smoke 脚本。

## 5. 验收标准

1. `./gradlew checkstyleMain` 对 hun_dao 范围无告警；日志截屏附在报告。
2. `./gradlew compileJava` 成功；必要时附冒烟截图。
3. 代码审查：Javadoc 内容准确、KISS/DRY/SOLID/YAGNI；无无效 UI/日志。
4. 文档同步更新，包含变更摘要、注意事项、后续维护建议。
