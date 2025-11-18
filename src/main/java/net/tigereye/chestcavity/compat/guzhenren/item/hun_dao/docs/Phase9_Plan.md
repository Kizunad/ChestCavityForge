# Phase 9 计划 —— 魂道道痕/流派接口框架

## 1. 背景与目标

- **痛点**：Hun Dao 仍直接从 `GuzhenrenResourceBridge`/`HunDaoOpsAdapter` 读取临时数值，缺乏与 Jian Dao 一样的“道痕加成”和“流派经验冷却”统一入口，导致后续 buff/冷却设计无法复用。
- **目标**：参考 `jian_dao` 的 `JiandaoDaohenOps`、`SwordOwnerDaohenCache`、`JiandaoCooldownOps`，在 Hun Dao 引入 **接口层+上下文钩子**，但暂不落地具体算法，仅提供可调用的 API、扩展点与文档。
- **约束**：不改动现有行为数值；所有新方法返回“pass-through”或常量。后续 Phase9.x 才填充逻辑。

## 2. 范围

| 模块 | 任务 | 参考 |
| --- | --- | --- |
| calculator/runtime | 定义 `HunDaoDaohenOps`, `HunDaoDaohenCache`, `HunDaoLiuPaiCooldownOps`（命名可微调），含接口、stub 实现、日志骨架 | `jian_dao/calculator/JiandaoDaohenOps.java`, `SwordOwnerDaohenCache.java`, `JiandaoCooldownOps.java` |
| runtime/context | 在 `HunDaoRuntimeContext`, `HunDaoRuntimeOps` 系列新增获取/缓存接口，middleware 注入默认实现 | `jian_dao/runtime/*` |
| tuning/docs | `HunDao_Rearchitecture_Plan.md`, `runtime/README.md`, `calculator/README.md` 记录新接口；`Phase9_Plan.md` 本文；后续生成报告 | - |
| smoke/script | `hun_dao/docs/smoke_test_script.md` 新增“接口编译验证”步骤（编译+反射检查） | - |

不包含：实际读取 Guzhenren 资源、真实系数/曲线、GUI 显示等。

## 3. 工作项

1. **Calculator 层框架**
   - 新建 `hun_dao/calculator/HunDaoDaohenOps.java`：
     - 方法：`effectiveUncached(ServerPlayer player, long now)`、`effectiveCached(...)`、`invalidate(ServerPlayer)`；先返回 `handle.read("daohen_hun_dao").orElse(0.0)` 等占位。
     - 预留 `resolveExpMultiplier(double liupaiExp)` stub，日志打印 WARN 提醒未实现。
   - 新建 `hun_dao/calculator/HunDaoDaohenCache.java`：仿照 `SwordOwnerDaohenCache`，提供 `getEffective(...)` 与 20t TTL（暂写 TODO，可直接 passthrough）。
   - 新建 `hun_dao/calculator/HunDaoCooldownOps.java`：`withHunDaoExp(long baseTicks, int liupaiExp, long minTicks)` 返回 `baseTicks`，但记录参数以便后续实现。

2. **Runtime / Context 接入**
   - 更新 `hun_dao/runtime/HunDaoRuntimeContext`（及相关 ops/impl）：
     - 新接口 `getScarOps()`, `getCooldownOps()`；默认实现返回上述 stub。
     - Middleware 在 `HunDaoRuntimeContextImpl` 构造时注入缓存对象；`HunDaoMiddleware` ensures singletons。
   - 若有 `HunDaoCalcContext`/`HunDaoRuntimeOpsAdapter`，新增 passthrough方法，保持 API 一致。

3. **扩展点/注册**
   - 在 `hun_dao/tuning/HunDaoRuntimeTuning` 或等价文件，添加常量（例如缓存 TTL、最大冷却减免 placeholder）。
   - 在 `HunDao_Rearchitecture_Plan.md` 增补“Phase 9 接口层”章节，说明 Dao 痕 & 流派冷却统一入口。

4. **文档 & 脚本**
   - 本计划文件 + 后续 `Phase9_Report.md` 模板（可先创建标题/章节）。
   - 更新 `runtime/README.md`, `calculator/README.md` 描述新接口用途与当前占位实现，记录 TODO。
   - `smoke_test_script.md` 新增步骤：
     1. `./gradlew compileJava`
     2. 使用 `rg -n "TODO: HunDaoDaohen"` 确认仅剩在 stub 文件中。

## 4. 时间线 & 产出

| 步骤 | 内容 | 产物 |
| --- | --- | --- |
| P9.a | 建立 calculator/runtime stub | `HunDaoDaohenOps.java`, `HunDaoDaohenCache.java`, `HunDaoCooldownOps.java` |
| P9.b | Context 注入 + middleware wiring | `HunDaoRuntimeContextImpl.java`, `HunDaoMiddleware.java` 等变更 |
| P9.c | 文档和脚本 | README 更新、`Phase9_Report.md`、smoke 脚本 |

## 5. 自检 & 验收

- `./gradlew compileJava`、`./gradlew checkstyleMain`（确认仅触及文件零新告警）。
- `rg -n "TODO" hun_dao/calculator`，只允许 stub 处带 `TODO Phase9.x` 标记。
- 代码审查项：
  1. 新接口不读写真实资源，仅透传。
  2. Runtime/context 对新接口具备可替换实现。
  3. 文档明确“当前为占位，后续 Phase9.x 完成算法”。

- 交付：`Phase9_Report.md`（待实现）、更新 README & smoke 脚本、接口代码。

