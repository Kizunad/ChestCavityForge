# Hun Dao Phase 9 实施报告 —— 道痕/流派接口框架

## 1. 阶段目标
- 提供与 `jian_dao` 同级别的 Dao 痕/流派入口，统一后续 buff、冷却逻辑的调用点。
- 不改变现有数值，只输出占位实现 + 文档 + 自检脚本，等待 Phase 9.x 填充算法。

## 2. 范围与交付

### Calculator & Tuning
- `HunDaoDaohenOps` / `HunDaoDaohenCache`: 读取 `daohen_hun_dao` + `liupai_hun_dao`，提供 `effectiveUncached`、`effectiveCached`、`invalidate`。
- `HunDaoCooldownOps`: `withHunDaoExp(baseTicks, liupaiExp, minTicks)` 占位实现，仅记录日志。
- `HunDaoRuntimeTuning`: 记录缓存 TTL、冷却最小值等常量。

### Runtime
- `HunDaoRuntimeContext` 新增 `getScarOps()` 与 `getCooldownOps()`，默认注入上述 stub。
- 行为层可以立即获取占位接口，便于 Phase 9.x 引入真实逻辑。

### 文档 & 脚本
- 更新 `calculator/README.md`, `runtime/README.md`, `HunDao_Rearchitecture_Plan.md` 描述 Phase 9 接口。
- `smoke_test_script.md` 增补“接口编译验证”步骤。
- 本报告文件作为 Phase 9 交付记录。

## 3. 验证
- `./gradlew compileJava` —— 确认新接口可编译。
- `rg -n "TODO: HunDao"` —— 仅允许在 stub 文件中出现。

## 4. 风险与后续
- Phase 9.x 需要实现真实的流派系数、冷却缩放，并将 TODO 替换为正式逻辑。
- 任何行为层调用必须继续假设当前实现只是 passthrough，避免依赖警告日志。
