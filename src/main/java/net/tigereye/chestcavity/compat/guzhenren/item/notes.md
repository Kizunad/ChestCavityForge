为了可玩性，设定 " 三转开始才能稳定替代 原生器官 "

"item.guzhenren.gu_zhu_gu": "骨竹蛊", 骨竹 - 生长 - 强化 - 持续性 - 加快/(或者x转前唯一充能方法?)其他骨道蛊虫充能 - 使用 真元 (被动增长) / 食用骨粉(催化) (主动)


# 🔹 联动触发器系统蓝图

## 1. 核心组件

* **TriggerEndpoint**

  * Passive / Active
  * 定义触发条件（SlowTick, Damage, ItemUse, CustomSignal …）

* **LinkageChannel**

  * 核心：保存一个数值（double/float）
  * 提供 API：`get() / set() / adjust()`
  * 广播更新事件给订阅者（READ / FOLLOW）

* **Policy (可选)**

  * **DecayPolicy**：自动衰减
  * **ClampPolicy**：上下限截断
  * **SaturationPolicy**：超过软上限时写入折减
  ...可拓展
  * 这些只在需要的 Channel 上挂载

* **TriggerRegistry**

  * 全局注册触发器与通道
  * `broadcast(event, context)` 调度逻辑

* **ActiveLinkageContext (运行时)**

  * 存放玩家胸腔实例的 Channel 值、冷却表、缓存
  * 每 tick 处理 decay / 更新 / follow

---

## 2. 执行顺序（每 tick 示例）

1. **Decay 阶段**：先让带有 DecayPolicy 的通道自然回落
2. **Trigger 执行**：

   * Passive → 按条件自动写入
   * Active → 根据玩家操作写入
3. **Policy 修正**：写入时经过 Clamp/Saturation
4. **值更新**：写入 Channel 容器
5. **READ/FOLLOW**：订阅的器官读取当前值，调整属性或触发回调
6. **同步/UI**：显著变化时发送 S2C 包

---

## 3. 数据流图（简化）

```text
[TriggerEndpoint]
     │  (触发事件: SlowTick, ItemUse ...)
     ▼
 [生成 Δdelta]
     │
     ▼
[LinkageChannel]
     │
     ├─> (经过 Policy: Decay, Clamp, Saturation...)
     ▼
[最终数值 v]
     │
     ├─> READ: 器官根据 v 调整属性
     ├─> WRITE: 直接修改 v
     └─> FOLLOW: 数值变化时调用回调
```

---

## 4. 典型应用示例

* **骨竹蛊 (Bone Bamboo Gu)**

  * Passive: 每秒 +5 → `bone_growth`
  * Active: 使用骨粉时 +20（40tick 冷却）
  * Channel: `bone_growth` 挂载 `SaturationPolicy`
  * READ: 读取当前值，指数曲线加速充能
  * FOLLOW: 数值跳变时刷新「骨甲 buff」

* **血水蛊 (Blood Water Gu)**

  * Channel: `blood_loss` 挂 DecayPolicy（自然止血）
  * Passive: 受伤时写入 +X
  * READ: 根据 `blood_loss` 值降低攻击力

---

## 5. 优点

* **轻量核心**：Channel 只管存数和广播
* **策略化扩展**：特殊规则通过 Policy 插件实现
* **灵活性高**：简单 Channel = 纯数值；复杂 Channel = 加策略
* **性能友好**：缓存 + 短路优化 + 最小采样间隔

## 6. 现状记录（2024-12）

- Runtime manager：`compat/guzhenren/linkage/ActiveLinkageContext` & `GuzhenrenLinkageManager` 会在 `ChestCavityUtil` 的 slow tick（20t）阶段最先执行，先跑 Policy（Decay/Clamp/Saturation），再广播 `TriggerType.SLOW_TICK`。
- 水肾蛊（Shuishengu）在充能/减伤时写入 `guzhenren:linkage/shuishengu_charge`（0~1），供后续 FX/叠加器官读取，当 SlowTick 无法监听时可直接读取该通道。
- 木肝蛊（Mugangu）会写入：
  - `guzhenren:linkage/wuhang_completion`：0~1 集齐度；
  - `guzhenren:linkage/mugangu_regen_rate`：当次慢速回复百分比（带 0.05/s 衰减），用于被动联动的权重输入。
- 骨竹蛊（Bone Bamboo Gu）驱动 `guzhenren:linkage/bone_growth`：
  - 每秒 +5 × 堆叠数；
  - 食用骨粉（无冷却）额外 +20 × 堆叠数；
  - 通道挂 `SaturationPolicy(soft=120, falloff=0.5)`，避免无限暴涨，可被其他骨道蛊读取；
  - Linkage 数值写入胸腔存档，下线重登保留累积进度。
- 骨枪蛊（Gu Qiang Gu）
  - 缓存在 `guzhenren:linkage/bone_growth` 的能量并写入 `CustomData[GuQiangCharge]`；额外将层数同步到 `guzhenren:linkage/bone_damage_increase` 供后续联动。
  - 每获得一次阈值能量（默认 60）触发骨裂与能量嗡鸣音效并点亮骨枪模型；最多叠 10 层。
  - 命中后清空充能与增伤通道：
    * 额外附加平滑衰减的物理伤害（base10，封顶 30）。
    * 施加 `guzhenren:lliuxue` 流血效果，最高 10 级并按叠层平滑递增。
- SlowTick 监听修复：即便列表为空，也会在 tick=20n 时触发 linkage 执行，避免“无监听”导致的休眠。

### 性能 & 模块化备忘

- 慢速监听的费用：每秒仅一个 `WeakHashMap` 查表 + 若干浮点计算，可承受。建议未来「阅读端蛊」在 slow tick 中取 `LinkageChannel` 决定加速度，而不是每 tick 轮询。
- 若某些联动需要指数或线性平滑（如 `LinkageChannel` 值驱动充能加速），可通过 Policy 组合实现：`DecayPolicy` 控制回落，`SaturationPolicy` 限制上限，避免每次都算复杂曲线。
- 主动联动（玩家操作）走 `TriggerEndpoint`，将冷却和广播放在 linkage 层，减少 ItemStack 自己维护定时器的成本。
- 当检测到性能瓶颈时，可把 slow tick 调整为 40t 或以上级别，只要使用方按 `delta` 或 `tickSpan` 做比例缩放即可。

将 骨枪蛊的效果替换为 NBTCharge ，使用 guzhenren:linkage/bone_growth 充能， 到达 X 点 后，  播放音效    { "name": "guzhenren:bone_crack", "volume": 0.8, "pitch": 1.0 },
      { "name": "guzhenren:energy_hum", "volume": 0.5, "pitch": 0.9 } 
      然后Render 骨枪
On hit 
特效占位符
触发  流血
状态效果命令：/effect give @p guzhenren:lliuxue 30 0
此效果将持续造成等同于此效果等级乘以2的魔法伤害
并且 造成 (base = 10) * bone_damage_increasement 点的物理伤害，若有item叠加，按照平滑曲线 
最终效果递减在(base = 30)点伤害(不影响 bone_damage_increasement 增加最终伤害) ，
同时 /effect give @p guzhenren:lliuxue 30 0 最大在10级 叠加item increasement递减(平滑曲线)

虎骨蛊 "item.guzhenren.hugugu": "虎骨蛊",

- 受到 变化道BIAN_HUA_DAO_INCREASE_EFFECT 力道LI_DAO_INCREASE_EFFECT 骨道GU_DAO_INCREASE_EFFECT 效率增益
变量:
- NBT MAX Charge : 20
- Minimal Damage : 10 
- Max Return Damage: 50
- onSlowTick : 如果 未满 0.5 * NBT MAX Charge 附加 玩家 虚弱 缓慢 疲劳 饥饿，并且 回复 0.25点 Charge，并且 尝试消耗 10 精力和 500 BASE 真元 若能够消耗 则 再恢复 0.25点 Charge
  若 > 0.5 则 每次 onSlowTick 回复 0.1 点，无损耗
- onIncomingDamage: 若 伤害 < Minimal Damage 则:return 
  若 伤害 >=  Minimal Damage: 则:
    (以下是可受到增益变量，药水效果int 四舍五入，若没有备注则都是 BASE 乘SUM(1+INCREASE))
    对玩家 effect 20(BASE) 点 饱和生命值
    对玩家 effect 0(BASE+增益INCREASE) 抗性
    对玩家 effect 1(BASE) 点速度 和跳跃提升
    以上药水效果时常 1 Minute (BASE)

    将 IncomingDamage 的 entity 击退，并且 反弹 0.5 * Damage Max Return (Damage*(1+SUM(INCREASE)) )

  
    













> 结论先说：松骨蛊触发后不是直接“秒杀”，而是给范围内生物施加自定义药水效果“松骨”(songgu)，再由该效果每刻结算“固定伤
  害+属性削减”，从而快速致死目标。

  - 触发入口
      - 文件：net/guzhenren/item/SongGuGuItem.java:use
      - 调用：net/guzhenren/procedures/SongGuGuYouJiKongQiShiShiTiDeWeiZhiProcedure.execute
      - 关键行为：
          - 播放音效/粒子，扣耐久/消耗副手素材（若持有守痕护蛊）
          - 对施法者周围半径25格的活物，施加自定义药水“松骨”效果
              - 代码：net/guzhenren/procedures/SongGuGuYouJiKongQiShiShiTiDeWeiZhiProcedure.java:146
              - 效果ID：GuzhenrenModMobEffects.SONGGU（注册名 songgu）
  - 杀伤机制（核心）
      - 药水类：net/guzhenren/potion/SongguMobEffect
          - 每tick都会触发效果逻辑（shouldApplyEffectTickThisTick 返回 true）
          - 调 SongguXiaoGuoChiXuShiMeiKeFaShengProcedure.execute
      - 周期伤害与削弱：net/guzhenren/procedures/SongguXiaoGuoChiXuShiMeiKeFaShengProcedure.java
          - 每tick对目标造成 100 点通用伤害：entity.hurt(DamageTypes.GENERIC, 100f)
          - 施加移动减速：MobEffects.MOVEMENT_SLOWDOWN，持续60tick，放大10
          - 每20tick（1秒）额外结算一次“骨骼松化”效果：
              - 玩家：通过记分板 smz 每秒减少 100 * 放大等级 分
              - 非玩家生物：直接降低其“最大生命”属性 MAX_HEALTH 每秒100点
          - 这意味着——松骨不是一次性“秒杀”，而是在短时间内通过高额DOT与上限削减迅速致死
  - 小结
      - 实现方式：范围施加“自定义药水效果”→ 药水每tick造成高额伤害并附带减速→ 每秒进一步降低生命上限/记分板，最终导
  致目标被“磨死/压死”
      - 非原版“中毒”路线，核心是自定义 MobEffect songgu + 周期 Procedure 的数值伤害与属性操作

  如果你要在游戏里验证：

  - 用松骨蛊对怪物释放后，data get entity <目标> ActiveEffects 可看到 guzhenren:songgu
  - 观察其生命会持续快速掉并出现减速，同时非玩家目标的最大生命值会被持续降低
