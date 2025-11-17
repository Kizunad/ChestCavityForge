# Hun Dao Phase 3 实施计划

## 制定时间
- 2025-11-18

## 阶段目标
在完成 Phase 0–2（含 2.1 修复）后的基础上，Phase 3 聚焦于行为模块化与客户端/事件层的解耦，目标是让 `hun_dao` 架构与 `jian_dao` 完整对齐：
1. 按功能拆分行为层，形成清晰的 `active/passive/skill/common` 分层；
2. 将运行时上下文引入所有魂道行为，彻底消除直接依赖；
3. 清理事件与 UI/客户端代码结构，预留 Phase 4+ 的扩展点。

## 任务清单

### 任务 1：行为层重构
- [ ] 为行为目录建立分层结构：
  - `behavior/common`：共享工具、上下文适配器
  - `behavior/passive`：被动类（XiaoHunGu、DaHunGu 等）
  - `behavior/active`：主动技能类（GuiQiGu 能力等）
  - `behavior/skill`：主动技能入口（命令/流程控制）
- [ ] 将现有行为类迁移到对应子目录并更新包名。
- [ ] 为共享逻辑（日志、上下文拉取、校验）提取 `HunDaoBehaviorContextHelper`。

### 任务 2：运行时上下文全面接入
- [ ] `XiaoHunGuBehavior`：改为 `HunDaoRuntimeContext` + `HunDaoSoulState`。
- [ ] `DaHunGuBehavior`：通过状态机判断魂兽状态，移除手工 `SoulBeastStateManager` 调用。
- [ ] `GuiQiGuOrganBehavior`/`GuiQiGuEvents`：统一使用 `HunDaoRuntimeContext` 操作资源与特效。
- [ ] `TiPoGuOrganBehavior`：所有资源与状态调用走上下文，必要时将护盾逻辑抽到 `HunDaoSoulState`。
- [ ] 自检：`rg -n "HunDaoOpsAdapter.INSTANCE" behavior/` 应仅剩通用 helper 内部引用。

### 任务 3：事件与客户端解耦
- [ ] 在 `hun_dao/events/` 下建立独立事件处理器（如 `GuiQiGuEvents`、`SoulBeastEvents`）。
- [ ] 客户端 FX/能力脚本移动到 `client/`，编写 README 指明 Phase 4 计划。
- [ ] 为事件/客户端包添加 `package-info.java` 或 README，记录依赖关系。

### 任务 4：调试与文档
- [ ] 更新 `runtime/README.md`，加入 Phase 3 行为分层示例。
- [ ] 更新 `storage/README.md`，说明新增的统计字段（如护盾刷新、魂魄泄露时间戳）。
- [ ] 编写 `Phase3_Report.md`，记录目录调整、上下文迁移、事件拆分。

## 关键要点
1. **保持功能等价**：重构只改结构与依赖，不能改变现有数值/行为。
2. **上下文为单一入口**：任何资源/状态访问必须经过 `HunDaoRuntimeContext`，禁止在行为层直接引用 `ResourceOps`、`GuzhenrenResourceBridge`。
3. **包名一致性**：迁移文件后务必更新包路径与 `META-INF` 资源（如需要）。
4. **日志与调试**：给新的 helper/事件类添加统一前缀，便于排查。

## 自检清单
- [ ] `rg -n "HunDaoOpsAdapter.INSTANCE" src/main/java/.../hun_dao/behavior` → 0 命中。
- [ ] `rg -n "GuzhenrenResourceBridge" src/main/java/.../hun_dao/behavior` → 0 命中。
- [ ] `rg -n "HunDaoRuntimeContext.get" src/main/java/.../hun_dao/behavior` → 显示所有行为类。
- [ ] `./gradlew compileJava`（本地）确保结构调整无编译错误。
- [ ] 冒烟测试：装备 XiaoHunGu、DaHunGu、TiPoGu、GuiQiGu，并验证魂魄恢复、护盾、技能与魂焰一切正常。

## 交付物
- `behavior/` 子目录的代码迁移与新的 helper 类。
- 更新后的 `events/`、`client/` README。
- `Phase3_Report.md`。
- 自检与冒烟测试记录附在报告末尾。
