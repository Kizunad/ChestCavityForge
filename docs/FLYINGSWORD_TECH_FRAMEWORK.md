# 飞剑模块重构｜技术框架方案（Tech Framework）

> 原则：KISS、轻量化、最少依赖；优先事件驱动与纯函数；避免过度设计。

## 1. 模块边界
- api：稳定对外接口与契约
  - `FlyingSwordController`、事件接口/注册表（`events/*`）
- core：核心对象与持久化镜像
  - `FlyingSwordEntity`、`FlyingSwordAttributes`、`FlyingSwordType`、选择/存储
- systems：可替换的运行时系统（服务端）
  - movement / combat / defense / blockbreak / targeting / progression / lifecycle（upkeep/recall）
- events：上下文与钩子（前置可取消、后置只读），初始化在 mod 构造流程触发
- integration：资源/冷却等外部系统适配（`ResourceOps`、`MultiCooldown`）
- ai：意图/轨迹；默认仅启用基础集合，扩展由开关控制
- client：渲染与 FX；Gecko/覆盖/视觉档为可选
- tuning：常量与配置读取（含特性开关）
- ops/calculator：可测试的纯逻辑与工具

## 2. 关键数据流（Server Tick）
1) `TickContext` → fire OnTick（可标记跳过 Upkeep/AI/BlockBreak）
2) UpkeepSystem → `ResourceOps` 扣减（失败策略：停滞/减速/召回）
3) AI/Intent → SteeringTemplate → MovementSystem 应用速度
4) CombatSystem/BlockBreakSystem 扫描 → OnHitEntity/OnBlockBreakAttempt → PostHit

## 3. 事件模型（扩展与短路）
- 前置（可取消/可改参）：`OnSpawn`、`OnTick`（标志位）、`OnHitEntity`、`OnHurt`、`OnBlockBreakAttempt`、`OnInteract`、`OnDespawn`
- 后置（只读）：`PostHit`、`ModeChange`、`TargetAcquired/Lost`、`UpkeepCheck`、`ExperienceGain/LevelUp`
- 触发顺序：实体/系统 → Registry.fireXxx → 钩子顺序执行（短路）

## 4. 配置与特性开关
- 开关项（默认关闭，服务器可配置）
  - `ENABLE_ADVANCED_TRAJECTORIES`、`ENABLE_EXTRA_INTENTS`、`ENABLE_SWARM`、`ENABLE_TUI`、`ENABLE_GEO_OVERRIDE_PROFILE`
- 建议：NeoForge TOML 读入 + 开机缓存；必要时热重载（下一版）

## 5. 依赖与一致性
- 不新增第三方依赖；沿用现有工具类
- 冷却：统一 `MultiCooldown`（owner 附件），实体仅查询
- 资源：统一 `ResourceOps`，禁止直连底层桥/账本
- 事件：NeoForge 环境下在 mod 构造函数注册初始化（禁止 FMLJavaModLoadingContext）

## 6. 同步与线程
- 仅服务端系统执行业务；客户端只做渲染与最小同步
- Registry 内部无阻塞 I/O；钩子快速返回；错误捕获并日志降噪

## 7. 性能预算（缺省）
- 单把飞剑 tick 平均预算 ≤ X µs（后续以指标收集校准）
- 每 tick 目标搜索 ≤ 1 次（黑板/缓存），避免 N×意图重复扫描
- 网络与日志：默认静音；仅在调试开关开启时输出概要

## 8. 渐进式迁移
- 先启用开关与事件 → 再迁 movement/combat/upkeep → 再补事件 → 冷却/资源收口 → 客户端与指挥降噪

## 9. 兼容性策略
- 对外 `api/` 保持稳定；迁包提供过渡别名期（仅文档标注）
- 事件上下文保证向后兼容：新增字段默认值安全

