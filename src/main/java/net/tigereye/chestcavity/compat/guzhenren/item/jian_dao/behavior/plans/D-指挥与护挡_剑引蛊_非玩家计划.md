# D 计划 — 指挥与护挡：剑引蛊 非玩家支持

目标：在无 UI 的前提下，让 Mob 具备“守卫/攻击”指挥与被动护挡能力；为非玩家建立“实体-飞剑”映射的最小集合。

—

## 技术基线
- 指挥替代：非玩家进入战斗时，默认标记最近敌对目标集合，向其持有飞剑下达“守卫/攻击”命令；
- 护挡触发：`onIncomingDamage` 中，对 Mob 的守卫飞剑进行“概率格挡”，施加移动减速/虚弱给攻击者；
- 飞剑控制器扩展：为非玩家提供从生物查找飞剑集合的方法。

—

## 任务清单
1) FlyingSwordController 扩展
- 新增 `getOwnerSwords(Level, LivingEntity)`；玩家版保留向后兼容；

2) SwordCommandCenter 替代路径
- 非玩家不打开 UI：新增 `autoSelectAndCommand(owner, now)`，按半径/上限筛选目标并下发命令；

3) 行为类改造
- `onSlowTick`：战斗进入→开启“扫描/指挥”窗口并下发命令；脱战→短冷却后关闭窗口；
- `onIncomingDamage`：对非玩家路径同样执行“守卫格挡”逻辑（复用玩家实现）。

—

## 改动位点
- `src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/entity/flyingsword/FlyingSwordController.java`（新增非玩家映射）
- `src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/behavior/organ/JianYinGuOrganBehavior.java:140, 220`（行为改造）
- `src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/jian_dao/behavior/organ/JianYinGuOrganBehavior.java`（增加 Mob 自动化分支）

—

## 验收标准
- Mob 进战后可使持有飞剑进入守卫/攻击；
- 护挡概率与效果等同玩家；
- 无 UI 和聊天提示；
- 无异常日志与性能问题。

