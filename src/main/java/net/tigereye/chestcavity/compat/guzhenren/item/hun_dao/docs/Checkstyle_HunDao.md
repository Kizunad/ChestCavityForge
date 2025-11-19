# Hun Dao Checkstyle 摘要（P8 准备）

> 基于 `./gradlew checkstyleMain` 2025-??-?? 最新输出，仅统计 `net/tigereye/.../hun_dao/` 目录下文件。

## 1. 总览

- 报告生成：`build/reports/checkstyle/main.xml`
- 提取日志：`docs/checkstyle_hun_dao.log`（516 条 warning，逐行包含文件/行列/规则/文案）
- 影响范围：13 个子目录，76 个文件

### 1.1 规则分布（Top 6）

| Check | 计数 | 核心问题 |
| --- | --- | --- |
| `CustomImportOrderCheck` | 323 | import 未分组 & 顺序混乱 |
| `MissingJavadocMethodCheck` | 140 | 公开方法缺少 Javadoc |
| `SummaryJavadocCheck` | 111 | 摘要缺少结尾句点/描述不规范 |
| `MissingJavadocTypeCheck` | 5 | 类/接口缺少 Javadoc |
| `LineLengthCheck` | 4 | 超过 100 列 |

> 其余规则（7 类，13 条）详见 log。

### 1.2 目录热度

| 子目录 | Warning 数 | 主要问题 |
| --- | --- | --- |
| `behavior/` | 155 | 行为上下文 import 分组缺失 + 公共方法无 Javadoc |
| `soulbeast/` | 137 | 状态/存储类缺少摘要，常量行长未拆分 |
| `calculator/` | 85 | 计算器公式/常量行长与 import 顺序混乱 |
| `client/` | 56 | HUD/面板方法注释缺失，render 调用顺序与导入冲突 |
| `fx/` | 0 ↓ (46) | FX Router/Registry/SoulFlameFx 已在 2025-??-?? 批次清零，可作为 import/Javadoc/行长基准 |
| `runtime/` | 30 | Runtime Context Javadoc 与长字符串检查未通过 |
| `middleware/` | 25 | 桥接层注释缺失、空行/变量作用域控制不足 |
| `network/` | 10 | Packet Builder 导入顺序 + 事件 Javadoc 未补全 |
| `ui/` | 9 | 小组件摘要/渲染方法未写 Javadoc |
| 其他（root/storage/events/combat） | 31 | 剩余零散 import/Javadoc/行长告警 |

### 1.3 `fx/` 目录状态（2025-??-?? 更新）

> 46 条告警全部清零：Javadoc、CustomImportOrder、EmptyLineSeparator、LineLength 均已符合规范。

| 类 | 当前 Warning 数 | 主要动作 | 结果 |
| --- | --- | --- | --- |
| `HunDaoFxInit.java` | 0 | 重排 import（SPECIAL 在 THIRD_PARTY 前），保持初始化流程 Javadoc | ✅ 自检通过 |
| `HunDaoFxRegistry.java` | 0 | import 重排 + 复核 `FxTemplate/Builder` Javadoc 标签 | ✅ 自检通过 |
| `HunDaoFxRouter.java` | 0 | import 重排 + `dispatch` overload 重新排序，确保 OverloadMethodsDeclarationOrderCheck 满足 | ✅ 自检通过 |
| `HunDaoSoulFlameFx.java` | 0 | import 重排 + fallback scheduler 添加空行，移除超长字符串 | ✅ 自检通过 |

> `HunDaoFxDescriptors.java` 与 `fx/tuning/HunDaoFxTuning.java` 继续保持 0 告警，可作为 FX 子系统的格式化模板。

## 2. 核心结论

1. **Javadoc 缺失（>250 条）** —— 既包括类型也包括方法。必须制定模板（简介 + 参数/返回/异常）后批量补齐，避免过度堆砌，控制在必要对外 API。
2. **Import 顺序（324 条）** —— 统一执行 `CustomImportOrder`（Std Java -> 3rd-party -> 项目 -> 空行）。可以通过 `spotless` 模块或 IDE optimize imports 但需匹配 Checkstyle 配置。
3. **超长行（32 条）** —— 主要集中在行为/Runtime 说明性字符串，可拆分 builder 或使用 `+` 分行。
4. **日志文件** —— `checkstyle_hun_dao.log` 将作为 P8 期间的工作基线，每修复一批即重新生成。

## 3. 交付物

- `docs/checkstyle_hun_dao.log`：详细清单。
- 本摘要：为 P8 计划准备的输入。
- 后续 P8 计划将给出修复优先级、批次、自检项（`./gradlew checkstyleMain` + `rg`）。
