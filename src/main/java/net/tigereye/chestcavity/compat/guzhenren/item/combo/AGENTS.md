# 组合杀招（Combo）规范与最佳实践

目标：为“变化道”及其他家族的组合杀招提供一致的、可测试、可维护的工程结构。所有新建/迁移的 combo 均应遵循本文约定。

目录约定（按技能归档）
- `compat/guzhenren/item/combo/<family>/<skill>/`
  - `behavior/` 行为入口（对接 OrganActivationListeners、资源/冷却、目标选择、FX 调用）
  - `calculator/` 纯函数计算（参数、范围、阈值、联动系数），必须可单元测试
  - `runtime/` 运行时服务（冷却协调、跨 tick 状态机等）
  - `tuning/` 参数表与动态调优（常量、阈值、Icon 声明等）
  - `messages/` 玩家提示与文案（文本拼接集中化，便于本地化）
  - `fx/` 客户端/服务端 FX（粒子、音效、可见化路径）
  - `state/` 附加器/数据结构（例如最近一次配置、撤销窗口、召唤物追踪）

行为约定
- 注册：通过 `ActivationBootstrap.register` 懒加载；`ComboSkillRegistry` 中仅登记技能元数据与初始化器。
- 资源/冷却：统一使用 `ResourceOps` + `MultiCooldown`，冷却挂在“承载器官”的 OrganState 下，配合 `ActiveSkillRegistry.scheduleReadyToast` 显示就绪提示。
- 选择/判定：在 `calculator/` 提供纯函数（如锥形判定、强度参数计算）供行为层调用；行为层不写复杂数学。
- 联动：通过 tooltip 流派或器官清单统计，转换为“可选协同计数/结构”，再交由 `calculator/` 计算最终系数。
- 无掉落：召唤类统一打 `NoDropEvents` 标签，避免战利品/经验污染。

测试约定
- 单元测试位置：`src/test/java/**/combo/**`，优先覆盖 `calculator/` 中的纯函数；不 mock 核心 Minecraft 类型（见 docs/TESTING.md 限制）。

示例：变化道 · 鱼群 / 饵祭召鲨
- `compat/guzhenren/item/combo/bian_hua/yu_qun/behavior/YuQunComboBehavior.java` 行为入口
- `compat/guzhenren/item/combo/bian_hua/yu_qun/calculator/YuQunComboLogic.java` 纯函数
- `compat/guzhenren/item/combo/bian_hua/yu_shi/behavior/YuShiSummonComboBehavior.java` 行为入口
- `compat/guzhenren/item/combo/bian_hua/yu_shi/calculator/YuShiSummonComboLogic.java` 纯函数

迁移策略
1) 先落 `calculator/` + 单测，确保逻辑可验证；
2) 再实现 `behavior/`，接入 `ResourceOps/MultiCooldown`；
3) 需要时补 `fx/messages/runtime/state/tuning`；
4) `ComboSkillRegistry` 使用新包路径的初始化器，避免早期类加载。

命名与日志
- 类名以 `Behavior/Logic/Tuning/Fx/Messages/Runtime` 收尾；
- 日志维持安静（INFO 以下），失败提示走 `messages/` 拼装。

