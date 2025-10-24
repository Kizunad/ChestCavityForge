好的！基于你已经统一到「通用 Reaction Tag 系统」的方向，下面给出**单窍·火炭蛊**的一整套——从运行时标签、DoT 模型到跨道反应与升级路线——全部用通用 tag 与通用注册点来落地，便于后续横向扩展与规则复用。

# 核心思路

* **统一标签驱动**：火炭蛊不再依赖专有名词（如“炭焰标记”），而是使用通用标签组合来表达“点燃→余烬→可被引爆/清理”的过程。
* **反应注册点**：全部通过 `ReactionRegistry.register*Defaults` 下的火系入口注册，内部仅读/写通用标签（`ReactionTagOps`），并接入 `CCConfig.ReactionConfig` 的可调参数。
* **免疫窗口**：一切连锁由 `reaction/*_immune` 控制，避免“火-油-火-油”类无限触发。

---

# 运行时标签（新增/复用）

已复用：

* `reaction/flame_trail`：炎焰余痕（火衣/火道灼痕）
* 其余跨道标签见你的速查表（blood_* / lightning_* / water_veil / heaven_grace / earth_guard / stone_shell / cloud_shroud / soul_scar / sword_scar / …）

为火炭蛊补充的**通用**标签（建议加入 `ReactionTagKeys`）：

* `reaction/flame_mark`：燃痕标记（可叠层，承载可被「引燃/引爆」的状态）
* `reaction/ember_residue`：余烬残留（地面/单位身上都可用，持续性 AoE/触发源）
* `reaction/ignite_window`：点燃窗口（短时：受到火系攻击将被强制点燃/刷新 DoT）
* `reaction/fire_immune`：火系免疫窗（防止爆轰连锁）
* `reaction/ignite_amp`：燃幅（火系对该单位的伤害提升，短时）
* `reaction/char_pressure`：热压（来自「炭心累积」，用于终结技放大；是通用的“热量蓄压”）

> 命名尽量中性：后续任意火道器官都可重用这些标签，不仅限火炭蛊。

---

# 火炭蛊 DoT 模型（全转通用）

**DoT 名称（引擎内）**：`Ignite`（点燃）

* **应用条件**：命中目标或踏入余烬残留时，若目标不在 `reaction/fire_immune` 窗口，则：

  1. 施加 `reaction/flame_mark(n层, 按命中事件增减)`
  2. 进入/刷新 `reaction/ignite_window(d)`（短时对火系更敏感）
  3. 触发/刷新「点燃 DoT」：`dmg_per_sec = base + stacks * k`；时长 `t` 可被火系增益刷新/延长。
* **余烬残留**：`reaction/ember_residue` 在地面单位上都可存在，靠 `SlowTick` 每 10t 检测周围单位，给入侵者追加 1 层 `flame_mark` 或短时点燃。
* **引爆（终结）**：当 `flame_mark` ≥ 阈值或 `char_pressure` ≥ 阈值时，可触发“炭心爆”（小半径瞬伤 + 点燃刷新 + 击退小量）。引爆后在目标上施加 `reaction/fire_immune(d)` 以防二次连锁。

---

# 升级路线（1~4 转）

### 1 转：火炭·初燃

* 装备被动：小幅火系增伤（通用系数）
* 命中：+1 层 `flame_mark`（持续 4s，上限 2），刷新点燃（2s，低额）
* 5% 几率在命中点落下 `ember_residue(3s)`（很小范围）
* 爆阈：当 `flame_mark` 达 2 → 触发微型炭爆（半径 + 点燃刷新），并对目标标记 `fire_immune(4s)`

### 2 转：火炭·恒燃

* `flame_mark` 上限 3，点燃 DoT 系数小幅提升；`ember_residue` 扩到 5s
* 新被动：若目标正处于 `ignite_window`，命中额外 +1 层 `flame_mark`（每 1s 最多一次）
* 余烬链：余烬在寿终前 1s 会生成“回光火星”小 AoE，对范围内单位强制 `ignite_window(2s)`
* 爆阈：3 层；爆后给自身 0.5s 的 `ignite_amp`（下次火伤 +X%）

### 3 转：火炭·炭炉

* `ember_residue` 可堆叠两处，彼此不叠伤，只叠**易燃性**：进入任一余烬时直接+2 层 `flame_mark`
* 新蓄压：每触发一次炭爆，给目标附 `char_pressure(+1)`（10s 内有效，上限 3）
* 终结技（被动引导式）：当 `char_pressure` 达 3，由任一火系命中可触发**过热爆**（较大半径，伤害分段下降），随后施加 `fire_immune(8s)`
* 对护甲强化：点燃期间，火伤对高护甲/抗性单位的衰减减半（通用抗性适配）

### 4 转：火炭·烈源

* 余烬进化：`ember_residue` 每 1s 尝试向外“跃迁”一次（极低概率），最多跃迁 1 次，避免铺满
* 强蓄压：炭爆对范围内所有受点燃单位也施加 `char_pressure(+1, 6s)`（软扩散）
* 极限技（主动/被动任选其一实现）：

  * 主动：消耗真元与精力，清空区域内**所有**`char_pressure`，按层数计算一次分段炭核风暴（3 段，各 0.4s），并对所有命中的单位施加 `fire_immune(12s)`
  * 被动：当场上**自己制造的**`ember_residue` ≥ 2 且自身处于 `ignite_amp` 时，首次命中触发一次小型链爆（逐个单位扩散，每跳伤害-20%，最多跳 3 次）

> 以上数值全部走 `CCConfig.ReactionConfig.huoTan` 下的可调键，方便服主/包作者微调。

---

# 跨道反应（以通用标签组合表达）

将火炭逻辑合并入你现有的分组注册中（示例仅列新/改部分；提示 i18n 键略）：

### 与血道（registerBloodDefaults 扩展）

* **沸血·燃幅**（`flame_trail`/点燃 × `blood_mark` 或 `hemorrhage`）
  追加小额火瞬伤并施加 `ignite_amp(3s)`；**清除 `blood_mark`**；对目标赋 `fire_immune(2s)`
  目的：一次爆燃-清标-免疫，阻断重复连锁
* **血雾汽化**（`ember_residue` × `blood_residue`）
  触地时将小范围 `blood_residue` 转化为短秒“蒸汽爆”（轻伤+致盲 0.5s），随后地面进入 `fire_immune(4s)`

### 骨道（registerBoneDefaults 扩展）

* **骨煅回火**（点燃 × `bone印/骨棘场`）
  使炭爆对骨系目标额外+穿透（少量）并施加 0.5s 击退；清理骨印，记 `fire_immune`
* **骨灰余温**（`ember_residue` × `stone_shell`）
  余烬接触石壳将被“覆盖”→ 余烬延时 1s 消散，同时为石壳目标施加 1s `ignite_window`（反击窗口）

### 光/魂/剑（registerLightSoulSwordDefaults 扩展）

* **灵焰炽燃**（点燃 × `soul_scar`）
  点燃刷新时对灵痕目标额外附一次微魂伤（不可格挡），随后给目标 `fire_immune(2s)`
* **焰刻剑痕**（点燃 × `sword_scar`）
  刷新点燃时为施法者提供 0.6s 轻微移速（剑势跟进用）

### 雷/木/人（registerLeiMuRenDefaults 扩展）

* **雷炎爆轰（通用版）**（`flame_trail`/点燃 × `lightning_charge`，且未处 `lightning_immune`）
  触发小半径爆轰，清 `lightning_charge`，标记 `lightning_immune(d)`
* **木灵护熄（通用版）**（点燃 × `wood_growth`）
  取消此次火伤并转为治疗，标记 `fire_immune(d)`
* **金刚护心（通用版）**（点燃 × `human_aegis`/`heaven_grace`）
  抵消并微反噬来源（可被护盾吸收），赋 `fire_immune(d)`

### 食/水/天（registerShiShuiTianDefaults 扩展）

* **醉食炎爆（通用版）**（点燃 × `food_madness`）
  本次点燃伤害翻倍结算一次且施加 0.5s 迟缓，然后 `fire_immune(3s)`
* **水幕熄焰（通用版）**（点燃 × `water_veil`）
  移除点燃并转为护盾/治疗，`fire_immune(3s)`

### 土/星/炎/云（registerTuXinYanYunDefaults 扩展）

* **土焰护墙（通用版）**（点燃 × `earth_guard`）
  火伤被泥墙吸收并反馈少量迟缓；`fire_immune(4s)`
* **石火反震（通用版）**（点燃 × `stone_shell`）
  蒸裂反噬来源 1 次小额伤害（非连锁），目标 `fire_immune(3s)`
* **星辉护心（通用版）**（点燃 × `star_glint`）
  抵消并为目标回少量血；`fire_immune(3s)`
* **云火蒸腾（通用版）**（`flame_trail`/`ember_residue` × `cloud_shroud`）
  触发一次蒸汽冲击（轻 AoE + 视野蒙灰 0.6s），清双方标记，区域进入 `fire_immune(5s)`

> 以上全部是“**通用火系 × 其他标签**”式规则，火衣、火炭蛊、其他火道器官都能吃到；不会绑定专用名字。

---

# 代码落点（伪代码）

**标签常量**（`ReactionTagKeys`）

```java
public static final TagKey FLAME_MARK = key("reaction/flame_mark");
public static final TagKey EMBER_RESIDUE = key("reaction/ember_residue");
public static final TagKey IGNITE_WINDOW = key("reaction/ignite_window");
public static final TagKey FIRE_IMMUNE = key("reaction/fire_immune");
public static final TagKey IGNITE_AMP = key("reaction/ignite_amp");
public static final TagKey CHAR_PRESSURE = key("reaction/char_pressure");
```

**注册火炭通用规则**（挂到现有各 register*Defaults 中，或新增 `registerFireGenericDefaults` 并在主入口调度）

```java
ReactionRegistry.register((ctx) -> {
    if(ctx.has(FIRE_IMMUNE)) return; // 全局短免
    if(ctx.event().isEnterResidue(EMBER_RESIDUE)) {
        ReactionTagOps.add(ctx.target(), FLAME_MARK, 1, 80); // 4s
        Ignite.applyOrRefresh(ctx.target(), cfg.igniteBase, cfg.igniteK, cfg.igniteDur);
    }
    if(ctx.event().isOnHitFireLike()) {
        int add = ctx.target().has(IGNITE_WINDOW) ? 2 : 1;
        ReactionTagOps.add(ctx.target(), FLAME_MARK, add, 80);
        Ignite.applyOrRefresh(ctx.target(), ...);
        // 爆阈
        if(ReactionTagOps.count(ctx.target(), FLAME_MARK) >= cfg.burstThreshold) {
            FireBursts.charcoalPop(ctx.area(), ctx.source(), cfg);
            ReactionTagOps.clear(ctx.target(), FLAME_MARK);
            ReactionTagOps.add(ctx.target(), CHAR_PRESSURE, 1, cfg.pressureTTL);
            ReactionTagOps.add(ctx.target(), FIRE_IMMUNE, 1, cfg.fireImmuneDur);
            ReactionTagOps.add(ctx.source(), IGNITE_AMP, 1, 10); // 0.5s
        }
    }
});
```

**器官行为（简）**

* `OnHitListener`：按转数把 `EMBER_RESIDUE` 与 `FLAME_MARK`、`IGNITE_WINDOW` 投放出去。
* `SlowTickListener`：驱动余烬检测、3/4 转的“跃迁/蓄压软扩散”。
* 主动技（若有）：读取并清空 `CHAR_PRESSURE` 生成分段风暴，广播免窗。

---

# 数据注册模板（示例）

```json
{
  "itemID": "guzhenren:single_charcoal_core",
  "organScores": {
    "chestcavity:fire_power": 2,
    "chestcavity:stability": 1
  },
  "tiers": {
    "1": {"burstThreshold": 2, "emberSeconds": 3, "igniteBase": 1.0, "igniteK": 0.3},
    "2": {"burstThreshold": 3, "emberSeconds": 5, "igniteBase": 1.2, "igniteK": 0.35},
    "3": {"burstThreshold": 3, "emberSeconds": 6, "igniteBase": 1.5, "igniteK": 0.4, "pressureTTL": 200},
    "4": {"burstThreshold": 3, "emberSeconds": 6, "igniteBase": 1.8, "igniteK": 0.45, "pressureTTL": 200, "chainMax": 3}
  }
}
```

---

# 验证清单

* 命中→`flame_mark` 叠层→达阈触发炭爆→清层 + 标记 `fire_immune`
* 余烬落地→靠近单位自动上火标记、刷新点燃
* 与 `blood_mark/hemorrhage` 同时存在→“沸血·燃幅”触发并清血印
* `cloud_shroud` 与余烬/灼痕相遇→蒸汽冲击并进入区域火免
* 3/4 转：`char_pressure` 行为确实累积与被主动清空结算

---

要不要我把这些**落进具体的 Java 伪实现骨架**（Listener + Registry + TagOps 用例），或者直接给一份**可复制的 `ReactionRegistry` 扩展类**和对应的 `CCConfig.ReactionConfig.huoTan` 键位？
