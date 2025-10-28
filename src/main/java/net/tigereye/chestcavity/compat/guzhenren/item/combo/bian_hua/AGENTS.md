本目录用于“变化道（bian_hua）”组合杀招的实现。子目录按技能名分组，并采用以下子结构：

- `<skill>/behavior/` 行为入口，对接 OrganActivationListeners；
- `<skill>/calculator/` 纯逻辑函数，必须有单测；
- `<skill>/runtime/` 运行时状态；
- `<skill>/tuning/` 参数常量/调优；
- `<skill>/fx/` 粒子与音效；
- `<skill>/messages/` 玩家提示；
- `<skill>/state/` 持久化状态。

现有示例：
- `yu_qun/behavior/YuQunComboBehavior.java`、`yu_qun/calculator/YuQunComboLogic.java`
- `yu_shi/behavior/YuShiSummonComboBehavior.java`、`yu_shi/calculator/YuShiSummonComboLogic.java`

新增 combo 请遵循 `compat/guzhenren/item/combo/AGENTS.md` 总规范。

