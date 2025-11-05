# Phase 4｜冷却与资源一致性

## 阶段目标
- 冷却集中 `MultiCooldown`；资源消耗统一 `ResourceOps`；失败策略可配。

## 任务列表
- 将实体内攻击/破块等冷却迁至 `MultiCooldown`（owner 附件）。
- UpkeepSystem：唯一入口扣减与失败策略（停滞/减速/召回）。
- 在 `FlyingSwordTuning` 暴露策略配置。

## 依赖关系
- 依赖 Phase 2/3。

## 验收标准
- 无负冷却/残留；资源扣减与策略生效且可观测。

## 风险与回退
- 中；临时保留旧字段镜像，便于回退与对比。

---

## 不能做什么（红线约束）
- 不得继续在实体类中维护新的“裸”整型冷却字段（如 `attackCooldown2` 等）；冷却状态统一由 `MultiCooldown` 管理。
- 不得绕过 `ResourceOps` 直接消耗/恢复主人资源（禁止 `LinkageManager`/`LinkageChannel.adjust`/`ledger.remove`）。
- 不得在冷却和资源路径中引入阻塞 I/O、昂贵的日志输出或客户端类依赖。
- 不得修改既有数值体验（攻击节奏、资源成本）的大幅基准，仅做“存储位置与一致性”迁移。
- 不得在同一逻辑上保留两套并行冷却实现（避免“双写”与漂移）。

## 要做什么（详细清单）
- 冷却统一到 MultiCooldown（owner 附件）
  - 设计并固定 Key 命名：
    - `cc:flying_sword/<sword_uuid>/attack`
    - `cc:flying_sword/<sword_uuid>/blockbreak`
    - `cc:flying_sword/global/recall_window`（可选）
  - 提供轻薄封装方法：`CooldownOps.get/set/hasCooldown(sword,key)`，屏蔽附件细节
  - 在 `CombatSystem.tick` 替换实体字段 `attackCooldown` 的读取/写回为 MultiCooldown 操作
  - 在 BlockBreakOps 破块节流点接入对应冷却 Key（如每 tick 最多 N 方块）
  - 兼容期：保留实体字段作为镜像，仅从 MultiCooldown 同步回写，验收后移除字段

- 资源消耗一致性（Upkeep）
  - 已完成：UpkeepCheck 事件与 `consumeFixedUpkeep` 覆盖路径（P3）
  - 扩展策略：在 `FlyingSwordTuning` 暴露 `ON_UPKEEP_FAILURE_STRATEGY={STALL|SLOW|RECALL}` 与参数（如减速倍率/持续秒数）
  - 在 `UpkeepSystem.tick` 中按策略执行：
    - STALL：冻结速度，保留姿态；
    - SLOW：按倍率降低 `deltaMovement`；
    - RECALL：调用 `FlyingSwordController.recall`（保持现状默认）
  - 统一音效/提示：仅在进入失败策略时播放一次提示，避免刷屏

- 观察与验证
  - 增加可选 DEBUG 指标：冷却命中次数、资源失败次数、策略命中次数（默认关闭）
  - 手动测试：长时间运行后无“负冷却/残留计时器”，召回/重载后冷却与资源状态一致

- 文档与迁移说明
  - 更新 `FLYINGSWORD_STANDARDS.md` 冷却与资源章节，规定统一入口
  - 在 `PHASE_4.md` 保留“不能做/要做”对照作为上线前检查清单

