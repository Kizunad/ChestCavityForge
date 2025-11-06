# ChestCavityForge 飞剑模块变更记录

## [Unreleased] – Phase 7 清理与发布候选准备

### Added
- 新增纯逻辑单元测试：KinematicsOpsEdgeCaseTest、KinematicsSnapshotMathTest、SteeringCommandTest。
- 新增阶段文档：`docs/stages/PHASE_8.md`（渲染姿态统一/半圆抬头治理）。

### Changed
- 降低飞剑战斗调试日志级别：将非关键 INFO 调整为 DEBUG，减少默认日志噪音。
- P6 文档同步：标注 Calculator/UpkeepOps 测试已完成，并将渲染姿态计划迁移至 P8。

### Deprecated
- 渲染欧拉路径（Yaw→Z-Pitch）标注为 Legacy（兼容路径），后续以 OrientationOps 取代。

### Fixed
- 修复测试包路径与 API 变更导致的测试编译错误，确保 `./gradlew test` 通过。

### Removed
- 无（功能清理将在 P7 继续推进：未引用的轨迹/意图/模型覆盖资源）。

---

## [2.16.4] – 当前版本基线
- 详见仓库历史记录。

