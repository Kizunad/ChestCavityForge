# Hun Dao Checkstyle 摘要（P8 准备）

> 基于 `./gradlew checkstyleMain` 2025-??-?? 最新输出，仅统计 `net/tigereye/.../hun_dao/` 目录下文件。

## 1. 总览

- 报告生成：`build/reports/checkstyle/main.xml`
- 提取日志：`docs/checkstyle_hun_dao.log`（641 条 warning，逐行包含文件/行列/规则/文案）
- 影响范围：13 个子目录，76 个文件

### 1.1 规则分布（Top 6）

| Check | 计数 | 核心问题 |
| --- | --- | --- |
| `CustomImportOrderCheck` | 340 | import 未分组 & 顺序混乱 |
| `MissingJavadocMethodCheck` | 140 | 公开方法缺少 Javadoc |
| `SummaryJavadocCheck` | 111 | 摘要缺少结尾句点/描述不规范 |
| `LineLengthCheck` | 32 | 超过 100 列 |
| `MissingJavadocTypeCheck` | 5 | 类/接口缺少 Javadoc |

> 其余规则（7 类，13 条）详见 log。

### 1.2 目录热度

| 子目录 | Warning 数 |
| --- | --- |
| `behavior/` | 171 |
| `soulbeast/` | 137 |
| `calculator/` | 86 |
| `client/` | 62 |
| `fx/` | 54 |
| `runtime/` | 39 |
| `middleware/` | 25 |
| `network/` | 10 |
| `ui/` | 9 |
| 其他（root/storage/events/combat） | 49 |

## 2. 核心结论

1. **Javadoc 缺失（>250 条）** —— 既包括类型也包括方法。必须制定模板（简介 + 参数/返回/异常）后批量补齐，避免过度堆砌，控制在必要对外 API。
2. **Import 顺序（324 条）** —— 统一执行 `CustomImportOrder`（Std Java -> 3rd-party -> 项目 -> 空行）。可以通过 `spotless` 模块或 IDE optimize imports 但需匹配 Checkstyle 配置。
3. **超长行（32 条）** —— 主要集中在行为/Runtime 说明性字符串，可拆分 builder 或使用 `+` 分行。
4. **日志文件** —— `checkstyle_hun_dao.log` 将作为 P8 期间的工作基线，每修复一批即重新生成。

## 3. 交付物

- `docs/checkstyle_hun_dao.log`：详细清单。
- 本摘要：为 P8 计划准备的输入。
- 后续 P8 计划将给出修复优先级、批次、自检项（`./gradlew checkstyleMain` + `rg`）。
