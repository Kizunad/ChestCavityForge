# Hun Dao Phase 9 行为迁移勾选清单

> 下述条目描述了 Phase 9 之后计划迁移到 `HunDaoRuntimeContext` Scar/Cooldown 接口的行为效果。  
> - `[x]` = 已实现并进入代码路径  
> - `[ ]` = 待确认方案，一旦勾选即可着手实现

## 被动类 Organ

- [x] **小魂蛊 (Xiao Hun Gu)**  
  - 魂魄回复 `base = RECOVER * (1 + ledger_bonus)`  
  - 新增：`base * (1 + scar)` 的 Dao 痕倍率，来源 `HunDaoRuntimeContext.getScarOps()`。

- [x] **大魂蛊 (Da Hun Gu)**  
  - 魂魄回复 `RECOVER * (1 + soul_intent)` 并同步念头  
  - 新增：输出值整体乘以 `(1 + scar)`，保持魂意与 DAO 痕可叠乘。

- [x] **体魄蛊 (Ti Po Gu)**  
  - **方案**：  
    1. 将被动魂魄/精力回复、魂兽护盾刷新改用 `(1 + scar)` 乘区。  
    2. 血盾/反击倍率受 `scarMultiplier` 影响，使防御与 Dao 痕成长挂钩。

- [x] **魂兽化被动 (HunDaoSoulBeastBehavior)**  
  - **方案**：  
    1. 根据 Dao 痕决定魂兽形态持续时间上限或泄露速度。  
    2. 当 `scar` 较高时降低 `HunPoDrainScheduler` 的泄露速率。
    3. 受击根据 scar 减免伤害：当前实现为 `(1 - clamp(scar / 2000) * 60%)` 乘区，仅在魂兽态且不跳过 invulnerable 伤害时生效，确保仍有剩余伤害。

## 主动技能 / 其他系统

- [x] **鬼气蛊 (Gui Qi Gu)**  
  - **方案**：  
    1. 技能冷却调用 `HunDaoCooldownOps.withHunDaoExp(...)`，由流派经验削减冷却。  
    2. 技能造成/持续（Gui Wu 范围、魂焰附加）依照 Dao 痕乘区放大。

- [x] **体魄蛊·魂兽攻击**  
  - **方案**：  
    1. `TiPoGuOrganBehavior` 中的 `attackHunpoCostReduction` 改为：基础减免 × `(1 + scar)`。  
    2. 将魂兽命中触发的效果（实时 `HunDaoDamageUtil`）接入 Dao 痕倍率。

- [x] **Hun Po Drain / 维护链路**  
  - **方案**：  
    1. `HunPoDrainScheduler` 读取 Dao 痕并按 `(scar / 2000)` 线性插值，在 `[+50%, -50%]` 区间调节泄露倍率。  
    2. 监听 Dao 痕突变 (`Δscar > 1e-3`) 时主动调用 `HunDaoDaohenOps.invalidate`，下一 tick 即可刷新缓存。

请在决定下一步实现范围后勾选对应条目并反馈。
