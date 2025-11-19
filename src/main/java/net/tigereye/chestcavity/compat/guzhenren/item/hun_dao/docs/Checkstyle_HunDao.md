# Hun Dao Checkstyle 摘要（P8 准备）

> 基于 `./gradlew checkstyleMain` 2025-??-?? 最新输出，仅统计 `net/tigereye/.../hun_dao/` 目录下文件。

## 1. 总览

- 报告生成：`build/reports/checkstyle/main.xml`
- 提取日志：`docs/checkstyle_hun_dao.log`（323 条 warning，逐行包含文件/行列/规则/文案）
- 影响范围：13 个子目录，76 个文件

### 1.1 规则分布（Top 6）

| Check | 计数 | 核心问题 |
| --- | --- | --- |
| `CustomImportOrderCheck` | 171 | import 未分组 & 顺序混乱 |
| `MissingJavadocMethodCheck` | 72 | 公开方法缺少 Javadoc |
| `SummaryJavadocCheck` | 69 | 摘要缺少结尾句点/描述不规范（集中在 calculator/soulbeast）|
| `LineLengthCheck` | 4 | 超过 100 列（soulbeast 日志字符串）|
| `VariableDeclarationUsageDistanceCheck` | 3 | 变量声明/使用间距过长 |
| `AbbreviationAsWordInNameCheck` | 2 | 标识符含连续大写缩写 |

> 其余规则（4 类，4 条）详见 log。

### 1.2 目录热度

| 子目录 | Warning 数 | 主要问题 |
| --- | --- | --- |
| `soulbeast/` | 90 | 状态事件 import 顺序紊乱、RuntimeEvents 缺 Javadoc |
| `calculator/` | 85 | 计算器公式/常量行长与 import 顺序混乱，公共 API 无 Javadoc |
| `client/` | 56 | HUD/面板方法注释缺失，render 调用顺序与导入冲突 |
| `runtime/` | 23 | Runtime Context Javadoc 与长字符串检查未通过 |
| `middleware/` | 17 | 桥接层注释缺失、空行/变量作用域控制不足 |
| `storage/` | 0 ↓ (6) | `BeastSoulRecord`/`ItemBeastSoulStorage` 全部规范化，可作为存储模板 |
| `behavior/` | 0 ↓ (155) | 本批次清空导入/Javadoc/行长告警 |
| `fx/` | 0 ↓ (46) | FX Router/Registry/SoulFlameFx 已在 2025-??-?? 批次清零，可作为基准 |
| 其他（root/combat/ui/events） | 37 | `HunShouHuaSynergyBehavior`、`HunDaoOrganRegistry` 仍有 import/Javadoc 告警 |

### 1.3 `fx/` 目录状态（2025-??-?? 更新）

> 46 条告警全部清零：Javadoc、CustomImportOrder、EmptyLineSeparator、LineLength 均已符合规范。

| 类 | 当前 Warning 数 | 主要动作 | 结果 |
| --- | --- | --- | --- |
| `HunDaoFxInit.java` | 0 | 重排 import（SPECIAL 在 THIRD_PARTY 前），保持初始化流程 Javadoc | ✅ 自检通过 |
| `HunDaoFxRegistry.java` | 0 | import 重排 + 复核 `FxTemplate/Builder` Javadoc 标签 | ✅ 自检通过 |
| `HunDaoFxRouter.java` | 0 | import 重排 + `dispatch` overload 重新排序，确保 OverloadMethodsDeclarationOrderCheck 满足 | ✅ 自检通过 |
| `HunDaoSoulFlameFx.java` | 0 | import 重排 + fallback scheduler 添加空行，移除超长字符串 | ✅ 自检通过 |

> `HunDaoFxDescriptors.java` 与 `fx/tuning/HunDaoFxTuning.java` 继续保持 0 告警，可作为 FX 子系统的格式化模板。

### 1.4 `behavior/` 目录状态（2025-??-?? 更新）

> `HunDaoBehaviorContextHelper`、`DaHunGuBehavior`、`GuiQiGuOrganBehavior`、`TiPoGuOrganBehavior`、`XiaoHunGuBehavior`、`HunDaoSoulBeastBehavior` 均已 0 告警，可作为导入/Javadoc 模板。

| 类 | 当前 Warning 数 | 主要动作 | 结果 |
| --- | --- | --- | --- |
| `HunDaoBehaviorContextHelper` | 0 | 重排 import，Javadoc 英文化并处理 `{@code <unknown>}` 转义 | ✅ 自检通过 |
| `DaHunGuBehavior` | 0 | 导入归组、`ensureAttached/onEquip` 注释模板化，`onSlowTick` 添加继承文档 | ✅ |
| `GuiQiGuOrganBehavior` | 0 | 补充 `ensureAttached/onEquip` 模板、`dispatch` overload 注释 | ✅ |
| `TiPoGuOrganBehavior` | 0 | 导入归组、`onSlowTick/onHit` 添加 `{@inheritDoc}`，单行注释拆分 | ✅ |
| `XiaoHunGuBehavior` | 0 | 新增类摘要、导入归组、`onSlowTick/onRemoved` 注释统一 | ✅ |
| `HunDaoSoulBeastBehavior` | 0 | Javadoc 英文化 + 行长拆分，`ensureActiveState`/`bindOrganState` 补摘要 | ✅ |

### 1.5 `soulbeast/storage` & `soulbeast/damage` 状态（2025-??-?? 更新）

> 9 条存储类 + 2 条 damage 警告全部清零：统一导入分组、补全 Javadoc、拆分行长，并将 `ItemBeastSoulStorage` 的自定义数据写入逻辑抽象为 helper，解决 `VariableDeclarationUsageDistance`。

| 类 | 当前 Warning 数 | 主要动作 | 结果 |
| --- | --- | --- | --- |
| `BeastSoulRecord` | 0 | 英文化摘要，canonical constructor 校验参数并补 doc，导入分组加空行 | ✅ |
| `BeastSoulStorage` | 0 | 重新撰写接口说明，补充 capture/store/peek/consume/clear 注释 | ✅ |
| `ItemBeastSoulStorage` | 0 | Import 分组、构造函数 Javadoc、`createStoragePayload()` 提前使用 state | ✅ |
| `SoulBeastDamageContext` | 0 | 规范 import + record canonical Javadoc | ✅ |

## 2. 核心结论

1. **Javadoc 缺失（~130 条）** —— 主要集中在 `soulbeast/runtime`（事件监听）与 `calculator`，继续沿用行为层模板。
2. **Import 顺序（171 条）** —— 高度集中在 `calculator`、`client`、`HunShouHuaSynergyBehavior`。统一执行 `CustomImportOrder`（Std Java -> SPECIAL -> Third-party -> static）。
3. **超长行（3 条）** —— Soul Beast 运行期日志仍有 >100 列字符串，后续处理 RuntimeEvents 时拆分。
4. **行为/FX/存储模板** —— 行为层、FX 层、魂兽存储层均 0 告警，可直接复用为 import/Javadoc/变量范围的基准。

## 3. 交付物

- `docs/checkstyle_hun_dao.log`：详细清单。
- 本摘要：为 P8 计划准备的输入。
- 后续 P8 计划将给出修复优先级、批次、自检项（`./gradlew checkstyleMain` + `rg`）。
