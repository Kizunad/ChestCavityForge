# 飞剑 AI 子系统速览

该目录实现了“Intent + Trajectory”两层结构：

- `intent/`：感知态的“意图”判定，仅输出 `IntentResult { target, priority, trajectoryType, params }`，不直接修改速度。
  - 基础意图：`Guard`（护主）、`Assassin`（猎首）、`Duel`（缠斗）、`Breaker`（断阵）、`Patrol`（巡域）、`Recall`（收锋）。
  - 战术意图：`Intercept`、`Kiting`、`Shepherd`、`Decoy`、`Sweep`、`Suppress`、`Hold`、`FocusFire`、`Pivot`、`SweepSearch`。
  - 规划入口：`intent/planner/IntentPlanner.java` 会根据 `AIMode` 汇集候选意图并筛选最高优先级。
- `trajectory/`：路径规划器，接收 `IntentResult` 并返回“期望速度”，交由实体的转向系统执行。
  - 已实现轨迹：`Orbit`、`PredictiveLine`、`BezierS`、`Corkscrew`、`Boomerang`、`Serpentine`、`CurvedIntercept`、`VortexOrbit`、`Sawtooth`、`PetalScan`、`WallGlide`、`ShadowStep`、`DomainEdgePatrol`、`Ricochet`、`HelixPair`、`PierceGate`。
  - 注册表：`trajectory/Trajectories.java` 统一管理所有轨迹解析器。
- `behavior/TargetFinder.java`：集中封装敌对目标与场景信息的检索逻辑（召唤物识别、集群中心、拦截预测等），供各意图复用。

## 指挥棒（JianYinGu Command Baton）
- 激活剑引蛊可开启“标记→配置→执行”流程：沿视线扫描敌对目标，使用聊天 TUI 选择战术（集中火力、截击、压制等）后下发给飞剑。
- 指挥执行期间会在 `SwordCommandCenter` 中生成高优先级的 `IntentResult`，覆盖默认意图并保持现有轨迹体系。
- 再次激活或发送取消指令可立即撤销标记；守护态飞剑在格挡时会获得速度增益并为敌方施加强制减速。
- 战术效果速记（2025-02-14）：
  - 集中火力（Focus Fire）：优先锁定血量最低的目标，提升直刺速度并增加提前量。
  - 截击突防（Intercept）：选取移动速度最高的敌人，增强曲线拦截的速度与曲率。
  - 压制扫荡（Suppress）：优先标记集群目标，增大蛇形拉扯半径并放缓速度以控制火线。
  - 驱离牵制（Shepherd）：挑选最接近玩家的敌对者，通过涡旋轨迹逼退并保持高机动。
  - 缠斗破防（Duel）：针对高血量/精英单位，螺旋半径收缩并提高缠斗频率以持续施压。
- 指挥候选扩展：实体若带有记分板标签 `cc_command_target` 或被加入数据标签
  `chestcavity:jianyin_command_targets` 即可被视作可指挥目标（适用于试炼假人等模组实体）。
- 分组指挥：指挥棒支持“全部 / 组1 / 组2 / 组三 / 青莲”目标过滤。可用 `/jianyin command group <id>`
  或 TUI 切换目标组；`/flyingsword group_*` 指令与 TUI 行为栏可为飞剑分配分组，青莲剑群固定为“青莲”组。
  扫描时生成全局“待分派”标记，可在执行前自由切换目标分组；按下执行后将标记赋给当前分组并与其他已执行分组并行运行。当前分组的清除仅清空未执行的待分派列表，不影响其他分组的执行指令。

> 温馨提示：Intent 只做判定逻辑，所有位移/速度修改必须通过 Trajectory → `FlyingSwordEntity.applySteeringVelocity(...)` 流程完成，保持 AI 结果可被轨迹“风格化”。
