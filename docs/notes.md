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

- Runtime manager：`linkage/ActiveLinkageContext` & `GuzhenrenLinkageManager` 会在 `ChestCavityUtil` 的 slow tick（20t）阶段最先执行，先跑 Policy（Decay/Clamp/Saturation），再广播 `TriggerType.SLOW_TICK`。
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


"item.guzhenren.dianliugu": "电流蛊",
- 受到 雷道 LEI_DAO_INCREASE_EFFECT 效率增益
- 变量:
  - NBTs: 
    - MAXCHARGE = 10
    - Damage = 5 * (1+LEI_DAO_INCREASE_EFFECT)
    - DEBUFF_TICK = 3 * 20 * (1+LEI_DAO_INCREASE_EFFECT) / (1+targetHealth)
      - DEBUFF:
        - 
          target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DEBUFF_TICK, 10, false, true, true));
          target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DEBUFF_TICK, 0, false, true, true));
  - OnHit 
    当玩家造成伤害时触发
    触发后会释放“电流”效果，对攻击者或范围内实体造成附加打击。
    消耗 1 点 Charge（上限 MAXCHARGE = 10）。
    粒子效果：
      - ParticleTypes.ELECTRIC_SPARK
    音效：
      - SoundEvents.REDSTONE_TORCH_BURNOUT
    













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


镰刀蛊（器官设定）

类型：器官（ATTACK ABILITIES 主动技能）

触发方式：玩家按下快捷键释放

动作表现：

玩家原地蓄力 3 tick（~0.15s），此时刀光开始闪现（用粒子 & 音效提示）。

蓄力结束后，向前方释放一道“刀光波动”，穿过路径中的敌人，延迟判定伤害。

命中区域敌人被击退并短暂“击倒”（效果类似 击退 + 矿车/雪傀儡的短暂停顿）。

若前方存在 3×3 方块区域（如草、木、土），会被直接切断破坏（模拟刀光破坏力）。

数值设定：

消耗：大量真元 + 精力

伤害：30 × (1+剑道EFFECT INCREASE) × (1+金道EFFECT INCREASE)

冷却：约 8–12 秒（避免滥用）

✨ 视觉特效（粒子）

利用 原版粒子系统 来模拟刀光：

蓄力阶段

end_rod 粒子在玩家身体周围螺旋环绕（白色光点，类似聚气感）。

sweep_attack 粒子缓缓出现，提示即将斩击。

刀光释放

sweep_attack 大量刷屏，拉出一道白色/银色的弧形轨迹（原版剑挥击特效）。

搭配 crit 与 crit_magic 粒子，表现出锋锐与灵气溢散。

若命中方块 → 在破坏位置生成 block_crack 粒子（对应方块材质碎裂效果）。

余韵特效

刀光经过区域残留 smoke 或 poof，像空气被撕裂。

偶尔 flash 粒子爆闪（类似闪电残光）。

🔊 音效设计

原版音效组合即可打造震撼感：

蓄力时

item.trident.return（低沉嗡鸣）

block.beacon.activate（聚气感）

斩击瞬间

entity.player.attack.sweep（原版横斩音效）

叠加 entity.lightning_bolt.thunder 的低音部分（削弱音量）

block.anvil.break（短促金属破碎声，强调锋锐）

命中反馈

entity.generic.explode 的弱版本（命中范围爆裂感）

方块破坏时，自动触发该方块的 block.break 音效。

💡 扩展创意

斩击残影：刀光方向短暂生成一个“虚影”实体（仅客户端渲染，不交互），看起来像残留的斩痕。

🕒 时间轴流程
Tick 0（按键触发瞬间）

扣除真元 & 精力（立刻支付消耗）。

播放 蓄力起手音效：

item.trident.return（低沉能量声）

block.beacon.activate（聚气感）

在玩家周身生成环绕粒子：

end_rod → 细碎白光，围绕身体旋转。

crit_magic → 随机星点。

Tick 2（蓄力完成 → 刀光显现阶段）

播放刀光“出鞘”特效：

大量 sweep_attack 粒子朝前方拉伸，形成一条虚影斩击线。

flash 粒子一瞬间爆闪（刀光闪现）。

播放音效：

entity.player.attack.sweep（横斩声）

叠加小音量的 entity.blaze.shoot（急促能量声）。

此时还不结算伤害/破坏，只是展示！

Tick 5（延迟爆发 → 真正命中）

在刀光路径上判定敌人：

造成伤害 = 30 × (1+剑道EFFECT INCREASE) × (1+金道EFFECT INCREASE)

附带 slowness II (1s) + weakness I (1s)（模拟被“击倒”）。

检查前方 3×3 区域的方块：

若是草、土、木等软块 → 直接破坏并掉落。

若是石头/矿石 → 不掉落，仅生成 block_crack 粒子 + 方块 break 音效。

播放音效：

block.anvil.break（金属碎裂感）

entity.generic.explode（弱爆炸声，增强冲击感）。

粒子效果：

block_crack（根据破坏方块材质生成碎片）。

poof / smoke（空气被撕开）。

crit 粒子沿刀光残影随机闪烁。

⚡ 演出总结

Tick 0 → 玩家起手 → 聚气音效 + 光点环绕。

Tick 2 → 刀光显影 → 播放横斩特效（但无伤害）。

Tick 5 → 真正伤害 + 方块破坏 → 粒子爆裂 + 金属爆音。

核心实现逻辑                                                                                                       
                                                                                                                   
- 变量载体：使用玩家与部分实体上挂载的附件数据 GuzhenrenModVariables.PlayerVariables，字段包含奴道相关状态。       
    - 示例字段：                                                                                                   
        - 玩家侧：nudaolanwei_1..10（每个“栏位”的魂魄占用/成本）、nudaolanwei_1_1..10_1（每栏位绑定的物种索引）、  
nudaoshuliang（当前奴道数量）、zuida_hunpo（魂魄上限）、nudaolanwei（当前选择栏位）、以及 GUI 选择标记、升级等。   
            - 参见 decompile/9_9decompile/cfr/src/net/guzhenren/network/GuzhenrenModVariables.java:1113 和同文件附 
近字段读写、克隆、同步。                                                                                           
        - 被奴役实体侧：nudaozhuren（记录主人的名字字符串），以及可选的nudaoxingji等给养/加成参数。                
            - 参见 decompile/9_9decompile/cfr/src/net/guzhenren/network/GuzhenrenModVariables.java:1154            
- 驾驭/招安入口：                                                                                                  
    - 多个“命中实体”流程在 Procedure 中汇聚到 NuDaoXProcedure（X=1..10），它们根据目标实体类型设置玩家的对应栏位“绑定物种索引”和“成本”（魂魄占用），并同步变量：                                                                              - 例：decompile/9_9decompile/cfr/src/net/guzhenren/procedures/NuDao1Procedure.java                             - 具体招安判定逻辑（检查数量上限与魂魄余量、设置主人并驯服、占用栏位、调用 NuDaoXProcedure）：                         - decompile/9_9decompile/cfr/src/net/guzhenren/procedures/HuYan4DangDanSheWuJiZhongShiTiShiProcedure.java: 
taming 前置检查 -> 写 entity 的 nudaozhuren -> 若目标是 TamableAnimal 则 tame(owner) -> 在玩家未占用的栏位中写入   
nudaolanwei = slotIndex 并调用对应 NuDaoXProcedure.execute 将 nudaolanwei_slot 与 nudaolanwei_slot_1 写入。        
    - 这些流程多由技能/投掷物命中时触发，或由 GUI 的按钮消息触发：                                                 
        - GUI 容器与按钮消息路由：decompile/9_9decompile/cfr/src/net/guzhenren/world/inventory/NuDaoGuiMenu.java、 
decompile/9_9decompile/cfr/src/net/guzhenren/network/NuDaoGuiButtonMessage.java                                    
- 实际“控制/追随”的落地：                                                                                          
    - 目标实体如果本身是 TamableAnimal，就直接走原生的主人相关 AI 目标（FollowOwnerGoal 等）。你能在多种自定义实体 
类里看到这些 goal：                                                                                                
        - 例如 decompile/9_9decompile/cfr/src/net/guzhenren/entity/YuanyuelinEntity.java:181、183 使用             
OwnerHurtByTargetGoal/FollowOwnerGoal                                                                              
        - decompile/9_9decompile/cfr/src/net/guzhenren/entity/WangYiEntity.java:183 添加 FollowOwnerGoal           
    - 因为写入了 entity 侧 nudaozhuren，即便实体不是 TamableAnimal，也可被其它流程读这个字段作自定义加成或行为判断 
（该模式在多处 Procedure 对 entity.getData(PlayerVariables) 的访问中可见）。                                       
                                                                                                                   
在模组外能否读取/更改？如何加增益？                                                                                
                                                                                                                   
- 读取/写入位置（服务端/客户端皆可，注意同步）：                                                                   
    - 任何一侧都可通过附件 API 访问：vars = (GuzhenrenModVariables.PlayerVariables)                                
entity.getData(GuzhenrenModVariables.PLAYER_VARIABLES);                                                            
        - 玩家：读写 nudaolanwei_*、nudaolanwei_*_1、nudaoshuliang、zuida_hunpo 等。                               
        - 被奴役生物：读写 nudaozhuren、nudaoxingji 等。                                                           
    - 写入后用 vars.syncPlayerVariables(entity) 进行同步（该方法在同文件已实现序列化 + 网络同步）。                
- 监听变动（客户端高性能方式）：                                                                                   
    - guzhenren 会在“玩家变量同步”时发送 payload（player_variables_sync 之类；你现有的桥接层已在监听）。最佳实践是 
在客户端监听该 payload，然后把影响（如联动通道、HUD）派发到主线程执行，避免网络线程直接改游戏状态。                
    - 我们已在 ChestCavityForge 侧实现了 GuzhenrenNetworkBridge 和 Dao痕监听（onPlayerVariablesSynced），你可以复用同样的桥接监听来读取奴道字段并驱动外部增益。                                                                       - 写增益/状态：                                                                                                    
    - 外部 mod 读取到 entity 侧 nudaozhuren == 某玩家名 或者检查 entity instanceof TamableAnimal &&                
((TamableAnimal)entity).isTame() 且 owner 匹配，即可施加额外属性/药水/AI 逻辑。                                    
    - 若需要改变“绑定物种”和“魂魄成本”，可直接写玩家的 nudaolanwei_slot_1 和 nudaolanwei_slot，并相应调整          
nudaoshuliang；但这会绕过原 mod 的流程，建议遵循其 Procedure 的入口（例如模拟调用对应 NuDaoXProcedure），或按其判定规则更新并调用 syncPlayerVariables。
    - 若需要“释放”/“清空”某栏位，参考 GUI 的清除与繁殖按钮对应的 Procedure（decompile/…/                           
NuDaoQingChuProcedure.java、NuDaoFanZhiProcedure.java）来对齐其内部字段。                                          
                                                                                                                   
可供直接复用的参考点                                                                                               
                                                                                                                   
- 玩家/实体变量结构与同步：decompile/9_9decompile/cfr/src/net/guzhenren/network/GuzhenrenModVariables.java:1000+ 起
- 招安流程（写 owner、tame、占用栏位）：decompile/9_9decompile/cfr/src/net/guzhenren/procedures/                   HuYan4DangDanSheWuJiZhongShiTiShiProcedure.java                                                                    
- 各栏位“物种索引/成本”写入：decompile/9_9decompile/cfr/src/net/guzhenren/procedures/NuDao1Procedure.java 等       
- GUI 按钮到 Procedure 派发：decompile/9_9decompile/cfr/src/net/guzhenren/network/NuDaoGuiButtonMessage.java:86    
                                                                                                                   
注意事项                                                                                                           
                                                                                                                   
- 同步线程安全：任何由网络监听回调触发的写操作需要切回客户端主线程执行（Minecraft.getInstance().execute），避免在  
Netty 线程直接改联动通道/世界状态。                                                                                
- 兼容性：不是所有目标都实现 TamableAnimal；此类目标仅依赖 nudaozhuren 的读写与自定义 AI/加成，需在外部逻辑自行    
处理。

剑影蛊 (item.guzhenren.jian_ying_gu) 
BASE_DAMAGE 100
COST_ZHENYUAN 2000
JINGLI 50
被动效果
当玩家使用近战攻击命中时，会额外生成一道“剑影”虚拟攻击。(消耗 10% * COST_ZHENYUAN)
剑影造成 40% 基础DAMAGE，并继承剑道 INCREASE_EFFECT 的加成。
剑影会无视护甲
若短时间内（≤2s）连续攻击，剑影伤害逐渐衰减到最低 15% 基础 DAMAGE , 并继承剑道 INCREASE_EFFECT 的加成。
影随步:
若是 玩家 攻击 产生暴击时 有 10%几率产生 "残影"
残影为玩家皮肤，并且半透明，偏暗色
残影会在 1s 内模仿一次基础斩击，对附近敌人造成微弱的溅射伤害。
主动技能（快捷键释放）
剑影分身：[寻路逻辑，先寻找最近的敌对entity，然后若是没有则执行守护指令] [快捷键使用 CCKeybindings.ATTACK_ABILITY_LIST]
消耗：200% ZHENYUAN + 100% JINGLI
效果：召唤 2–3 个“剑影分身”，持续 5s。
分身会自动模仿玩家的近战攻击，但造成 25% DAMAGE。
分身命中敌人时，附带 1s 缓慢III效果。
冷却：20s
风险代价
长时间战斗会迅速消耗真元与体力。      


ChestCavityForge/src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/gu_dao
名称
"item.guzhenren.rou_bai_gu": "肉白骨"
器官（骨道 + 嘴型蛊虫，攻击类辅助）
变量:
HEAL
COST_ZHENYUAN
COST_JINGLI
COST_HUNGER
被动效果
回血强化：
  - 玩家生命恢复速度提高 +50%（类似于再生效果，但不显示药水图标）。
  - 当玩家处于非战斗状态 5s 以上时，回血效率进一步翻倍。
原生器官修复:
  - 逐步修复玩家胸腔中“缺少的原版器官”（例如心脏、肺、肾脏等）。
  - 修复逻辑：
    - 每 180s，随机选择一个缺少的原版器官（非蛊虫器官）[当前标准玩家器官ChestCavityForge/src/main/resources/data/chestcavity/types/humanoids/player.json]
    - 若有了选择 则每 2 * 60s 消耗 100% COST_HUNGER 20% COST_ZHENYUAN  20% COST_JINGLI，恢复 10 点点数
    - 若点数 大于等于 100 则恢复器官
    - 若 2 * 60s 到了，但是玩家 HUNGER < 100% COST_HUNGER，则会随机变异成一个 原版非玩家器官 [ChestCavityForge/src/main/resources/data/chestcavity/organs]
视觉与表现
粒子：细小的 红色肉丝粒子 缠绕胸腔，并逐渐消散。
音效：轻微的“骨骼咔咔声 + 血液流动声”。
在修复器官时，会短暂播放 绿色十字粒子（类似治疗药水效果）。
撕咬攻击:
  - 触发条件: 
    - 玩家近战攻击敌方，或每隔一定冷却（如 5s）有概率 (10%) 发动。
    - 敌人与玩家的距离 ≤ 10 格。
    - 敌人不是无实体/虚体单位（避免对盔甲架、幽灵类无效对象触发）。
  - 判定逻辑:
    - 闪避机制：敌方有概率（如 25%）闪避撕咬。
    - 盔甲判定：
        - 取敌方护甲值 vs 玩家本体 + SUM(INCREASE_EFFECT)：
          - 如果 敌方护甲值 < 玩家 * SUM(INCREASE_EFFECT) → 撕咬生效。
          - 如果 敌方护甲值 ≥ 玩家 * SUM(INCREASE_EFFECT) → 嘴巴不展开，撕咬不生效。
  - 效果（撕咬成功时）:
    - 撕咬造成吸血效果，回复 敌方总血量的 1%（真实伤害吸收，不受护甲影响，若超过玩家血量上限则转化为3分钟的饱和生命值）。
    - 撕咬伴随小范围血液粒子与“肉块撕裂声”。
    - 撕咬不会额外消耗真元，但 每次触发会让饱食度 -0.5（吞食消化血肉代价）。
撕咬成功时：
粒子：红色液体飞溅 + 微小肉块（类似 SLIME 变色 红色粒子）
音效：僵尸咬合声 (entity.zombie.attack_iron_door) 混合骨裂声 (entity.skeleton.hurt)
撕咬失败时：
嘴型蛊虫发出“咔哒”声，但不展开。



"item.guzhenren.bing_ji_gu": "冰肌蛊",
冰肌蛊:(每秒)5生命恢复，1精力，消耗200BASE真元维持
冰道攻击有10％概率额外造成本次攻击所造成伤害的5％伤害（被动）并且对敌人造成 状态效果命令：/effect give @p guzhenren:hhanleng 30 0 此效果存在时将持续为实体添加等同于此效果等级的缓慢，挖掘疲劳。
冰肌玉骨(冰肌蛊和玉骨蛊联动):免疫流血Effect，可以引爆自己(1 slot)部分(匹配:chestcavity:*_muscle)肌肉产生冰爆(主动快捷键AttackAbilities) 若没有则不能执行，
当玩家是北冥冰魄体(需要配置通用接口判断体质)时残血获得短时间无敌，
当胸腔内存在(  "item.guzhenren.bing_bao_gu": "冰爆蛊", 目前暂无效果，但是依旧判断)，
冰爆的伤害提升 - 冰爆受 冰道INCREASE_EFFECT影响
加20点伤害吸收(每1分钟) 冰道INCREASE_EFFECT影响

冰肌蛊
{
  "itemID": "chestcavity:muscle",
  "organScores": [
    {"id":"chestcavity:strength","value": "1"},
    {"id":"chestcavity:speed","value": "1"}
  ]
}

效果表现逻辑
中心爆炸点
爆炸以玩家/实体所在位置为圆心。
半径可配置（例如 5 ~ 10 格）。
伤害与控制
范围内敌对生物：
造成伤害（可考虑随半径衰减，中心伤害最高）。
附加减速效果（冰冻感）。
玩家本身：免疫
地形破坏
类似 TNT 爆炸，但不掉落方块。
仅破坏软方块（泥土、草方块、沙子、石头），避免过度破坏。
地形替换
爆炸范围内随机将方块替换为：
Packed Ice（主要是石头/硬方块）
Snow Block / Powder Snow（主要是草方块/泥土）
表面加上一层 Snow Layer（类似下雪后堆积）。
粒子与音效
爆炸瞬间：
冰晶碎片粒子（蓝白色玻璃破碎感）。
雪雾粒子（类似龙息雾气扩散）。
声音：
glass.break 混合 snow.break 的低沉音效。
爆炸中心传出冰裂声。
⚙ 技术实现思路
伤害/控制：
使用 Level.getEntitiesWithinAABB 获取范围生物 → 施加伤害与减速效果。
地形破坏：
遍历球形范围内的方块。
用 level.setBlock 替换为冰雪混合物。
设置条件避免破坏黑曜石/基岩等不可破坏物。
特效表现：
在爆炸点 level.sendParticles，多种粒子叠加。
播放音效 SoundEvents.GLASS_BREAK + SoundEvents.SNOW_BREAK。

钢筋(  "item.guzhenren.ganjingu": "钢筋蛊",):60伤害吸收(每2分钟)，每秒(OnSlowTick) 恢复 1点精力，近战(Distance < 10方块 判定)攻击有15％概率造成 基础攻击力 8％ 的附加伤害 * (1 + 金道增益)（被动）
配置通用Fx: 
视觉效果（火花特效）
火花粒子
粒子类型：小型橙色/黄色发光粒子，带有轻微的拖尾。
随机性：火花的大小、方向、速度随机，模拟铁器撞击时迸溅的火花。
生命周期：火花生成后快速减弱，寿命 5~15 tick，逐渐熄灭。
重力影响：带轻微重力，火花会向下弧线坠落，末尾渐隐。
冲击点亮度
撞击点瞬间发出一丝亮光（1~3 tick 高亮），类似短暂闪光。
亮度逐渐衰减，给人“铁器摩擦火花”的感觉。
材质/颜色
起始：白黄（RGB: 255, 240, 180）。
中段：橙红（RGB: 220, 140, 70）。
末段：暗红，最终透明消失。
🔊 音效效果（铁跕落地声）
基础音效
选用类似 金属落地（anvil/iron ingot 落地） 的声音。
叠加音
在主声效外，加一点“金属摩擦”细音，衬托出“火花”的感觉。

消耗(OnSlowTick) 1 饱食度维持(若没有饱食度则无上述所有效果) 

铁骨(  "item.guzhenren.tie_gu_gu": "铁骨蛊",):像玉骨一样增幅金道和骨道效率，拥有20点伤害吸收(每1分钟)，消耗骨能
精铁骨蛊(  "item.guzhenren.jingtiegugu": "精铁骨蛊",)像玉骨一样增幅金道和骨道效率，拥有40点伤害吸收(每1分钟)，消耗骨能

钢筋 + (铁骨/精铁骨):
提供抗性1，
当铁骨为精铁骨时获得急迫1，
同时生命值自然恢复停止，
可以使用铁锭修补(恢复10%生命值)，
当胸腔内存在精铁蛊时每秒恢复4点生命值

实现步骤

为钢筋本体建立器官行为类，并挂入CCitems集成表
在 compat/guzhenren 模块里，每个器官都通过 OrganIntegrationSpec 声明要注册的监听器，再由 GuzhenrenIntegrationModule 在启动时统一导入，你可以按 GuzhuguOrganBehavior 的模式新增一个 GangjinguOrganBehavior，并在 GuDaoOrganRegistry 里追加 OrganIntegrationSpec 项目，以便慢速轮询、交互监听等都能绑定到该器官上。
组合判定与增益刷新
在新的行为类的 onSlowTick 中遍历胸腔物品栏，统计钢筋、铁骨与精铁骨的数量（参照骨竹蛊对特定 ResourceLocation 计数的写法），然后针对命中条件的玩家调用 player.addEffect 刷新抗性、急迫等短效状态，持续时间可以保持在 40~60 tick 并在每次慢速 tick 时重置。HuGuguOrganBehavior 演示了如何把效率系数折算成 MobEffectInstance 的 amplifier，配置通用工具函数并且参考这一写法。
拦截自然生命回复
目前 OrganHealingEvents 会在 LivingHealEvent 中断掉“心脏评分为 0”玩家的饱腹自然回复。扩展这里的判定：在拿到 ChestCavityInstance 后，按上面的组合检测逻辑检查钢筋 +（铁骨/精铁骨）是否生效；若是，则同样取消事件，实现“自然恢复停止”。记得跳过再生药水、器官自愈等其它来源（原有守卫逻辑已处理）。
铁锭修补 10% 生命
参照 GuDaoOrganEvents 对骨粉的处理方式注册一个新的交互监听：在玩家右键铁锭(对准空气)且组合有效时，服务端消费 1 个铁锭，计算 healAmount = player.getMaxHealth() * 0.10F，再用 ChestCavityUtil.runWithOrganHeal(() -> player.heal(healAmount)) 执行，避免被“自然回复拦截”误伤。若消耗成功，可像骨粉逻辑那样播放音效、动作并 event.setCanceled(true)。(并且设置正常食用冷却)
精铁蛊在场时每秒 5 点治疗
让钢筋或精铁骨的集成规范再注册一个 OrganHealListener：当检测到胸腔内同时拥有钢筋与精铁蛊时，每 tick 返回 0.5F（20 tick 即 5 HP），否则返回 0。ChestCavityUtil.onTick 会在服务器端累加所有器官的治疗输出并统一调用 heal，借助上一步的 runWithOrganHeal 守卫保证不会误判为自然回复。
状态同步与调试
如果需要给前端或其它器官共享“自然恢复已封禁”或“钢筋增益层数”一类的状态，可以在慢速 tick 中写入一个新的 LinkageChannel，其创建和刷新方式可直接复用骨竹蛊、玉骨蛊等行为里的 LinkageManager.getContext(cc).getOrCreateChannel(...)，以便后续行为读取或做可视化提示。

{
  "itemID": "guzhenren:ganjingu",
  "organScores": [
    {"id":"chestcavity:strength","value": "24"},
    {"id":"chestcavity:speed","value": "8"}
  ]
}

"itemID": "guzhenren.tie_gu_gu",
"organScores": [
  {"id":"chestcavity:defense","value": "2"},
  {"id":"chestcavity:nerves","value": "1"}
]


"itemID": "guzhenren.jingtiegugu",
"organScores": [
  {"id":"chestcavity:defense","value": "4"},
  {"id":"chestcavity:nerves","value": "1"}
]

清热蛊（  "item.guzhenren.qing_re_gu": "清热蛊"）:每秒恢复3点生命值和1点精力，当胸腔内存在玉骨时，获得［清热解毒］，有10％概率(OnSlowTick)免疫中毒，受到的着火时伤害降低3％ * (1+冰雪道 INCREASE EFFECT)，
消耗100BASE真元维持

{
  "itemID": "guzhenren:qing_re_gu",
  "organScores": [
    {"id":"chestcavity:filtration","value": "3"}
  ]
}


肋骨盾蛊（  "item.guzhenren.le_gu_dun_gu": "肋骨盾蛊",）:
每秒提供60点骨能(Bone_growth)，骨道INCREASE EFFECT + 8％，
且拥有［风骨］效果，该效果每秒提供一点［不屈］，当［不屈］达到10点，
可催发(ATTACKABILITY)［骨御］获得2秒无敌，消耗200BASE真元维持/s，胸腔内只能生效一个肋骨盾蛊

{
  "itemID": "guzhenren:le_gu_dun_gu",
  "organScores": [
    {"id":"chestcavity:defense","value": "8"},
    {"id":"chestcavity:nerves","value": "2"}
  ]
}

竭泽蛊（  "item.guzhenren.jiezegu": "竭泽蛊",）:
OnHit
攻击有18％概率触发［回流］
，［回流］会基于本次攻击造成的 伤害，
额外造成8％伤害（音效:河水流动声），
［回流］有8％概率触发［断流］，
［断流］会给予 敌方 时长 4秒 的 碎甲(/effect give @p guzhenren:suijia 30 X) X为水道INCREASE EFFECT，消耗500BASE真元维持/s，同时每秒额外消耗25点生命值

{
  "itemID": "guzhenren:jiezegu",
  "organScores": [
    {"id":"chestcavity:detoxification","value": "3"}
  ]
}



泉涌命蛊（  "item.guzhenren.quan_yong_ming_gu": "泉涌命蛊", 心脏）:
水道INCREASE EFFECT提升 30% ，每秒恢复1％生命值和5点精力，
当胸腔内存在 水体蛊(  "item.guzhenren.shui_ti_gu": "水体蛊",) 
会获得［纯水］效果，
该效果提供10点常驻伤害(OnSlowTick)吸收，
消耗800BASE真元维持/s，
胸腔内只能生效一个泉涌命蛊

{
  "itemID": "guzhenren:quan_yong_ming_gu",
  "organScores": [
    {"id":"chestcavity:health","value": "2"}
  ]
}


犬:
  chestcavity:health 1
  chestcavity:strength 1
  chestcavity:speed 1
虎:
  chestcavity:health 1
  chestcavity:strength 2
  chestcavity:speed 1
  chestcavity:impact_resistant 1 
熊:
  chestcavity:health 2
  chestcavity:strength 2
  chestcavity:defense 2
  chestcavity:impact_resistant 1
狼:
  chestcavity:strength  1
  chestcavity:arrow_dodging 1
  chestcavity:speed 2
羚:
  chestcavity:health 2
  chestcavity:filtration 1
  chestcavity:defense 2


百兽王:
  guzhenren:zuida_zhenyuan 1
  guzhenren:zuida_jingli 1
千兽王:
  guzhenren:zuida_zhenyuan 4
  guzhenren:zuida_jingli 4
  guzhenren:niantou_zuida 1
万兽王:
  guzhenren:zuida_zhenyuan 8
  guzhenren:zuida_jingli 8
  guzhenren:niantou_zuida 4
  guzhenren:zuida_hunpo 1
兽皇:
  guzhenren:zuida_zhenyuan 16
  guzhenren:zuida_jingli 16
  guzhenren:niantou_zuida 8
  guzhenren:zuida_hunpo 4
  guzhenren:shouyuan 1

魂兽状态:血条将会消失替换成魂魄条，攻击视为魂道攻击，每次攻击都会附加［魂炎］（黑色火焰），［魂炎］每秒对敌人造成等同于当前魂魄值上限1％的真伤，持续5秒，每次攻击都会消耗18点魂魄值，魂魄值每秒溢散3点，魂兽寿元不会自然消耗，获得饱和效果

小魂蛊（心脏）:每秒恢复1点魂魄，提供［魂基］效果，该效果将额外提升20％魂魄恢复效率

大魂蛊（  "item.guzhenren.dahungu": "大魂蛊",）:
每秒恢复2点魂魄值和1点念头，
胸腔内存在小魂蛊时获得［魂心］效果，该效果会将人同步转化为->魂兽，魂兽状态不可解除；
当角色为魂兽状态，提供［魂慑］效果，该效果可根据当前魂魄量，震慑生命值小于魂魄量的敌对生物
{
  "itemID": "guzhenren:dahungu",
  "organScores": [
    { "id": "guzhenren:zuida_hunpo", "value": "50" },
    {"id":"chestcavity:health","value": "2"}
  ]
}


  "item.guzhenren.lingyangyan": "蛊材_羚羊眼",
  "item.guzhenren.quanyan": "蛊材_土犬眼",
  "item.guzhenren.quanrou": "蛊材_土犬肉",
  "item.guzhenren.langrou": "蛊材_狼肉",
  "item.guzhenren.lingrou": "蛊材_羚肉",
  "item.guzhenren.quanpi": "蛊材_土犬皮",
  "item.guzhenren.lingyaopi": "蛊材_羚羊皮",
  "item.guzhenren.xiongpi": "蛊材_熊皮",
  "item.guzhenren.llangpi": "蛊材_狼皮",

- `item.guzhenren.baishouwanghuya`: 蛊材_百兽王虎牙
- `item.guzhenren.qianshouwanghuya`: 蛊材_千兽王虎牙
- `item.guzhenren.wanshouwanghuya`: 蛊材_万兽王虎牙
- `item.guzhenren.shouhanghuya`: 蛊材_兽皇虎牙

- `item.guzhenren.lingyangjiao`: 蛊材_羚羊角
- `item.guzhenren.bbaishoulingyangjiao`: 蛊材_百兽王羚角
- `item.guzhenren.qianshouwanglingjiao`: 蛊材_千兽王羚角
- `item.guzhenren.wanshouwanglingyangjiao`: 蛊材_万兽王羚角
- `item.guzhenren.shouhuangjiao`: 蛊材_兽皇羚角


  建议新增/完善的通用模块与工具

  - 魂兽状态（服务端权威 + 客户端显示）
      - 类/工具
          - SoulBeastState 附着与管理器: compat/guzhenren/item/hun_dao/state/SoulBeastState.java,
  SoulBeastStateManager.java
              - 字段: active 是否魂兽、permanent 是否不可解除、lastTick 最近同步、source 来源（小/大魂蛊）。
              - API: isActive(entity), isPermanent(entity), setActive(entity, flag), setPermanent(entity, flag),
  syncToClient(player).
          - 事件钩子: 在玩家登录、换维、死亡/重生时同步魂兽标记；在心跳时保持快照一致（必要时）。
      - 网络同步
          - 复用现有 GuzhenrenNetworkBridge 风格，定义 C2S/S2C: soul_beast_sync，客户端接收后刷新 HUD 与本地缓存。
      - 标签与提示
          - 在 HUD 附近加“魂兽状态”小图标或文本；调试开关打印当前 hunpo/max。
  - 伤害标记与命中附加（“魂道攻击”与“魂炎”）
      - 伤害类型/标记
          - Damage 标记工具: compat/guzhenren/combat/HunDaoDamageUtil.java
              - markHunDaoAttack(attacker), isHunDao(source): 将近战伤害源打上 HUN_DAO 标记（可用自定义 DamageType 或
  DamageSource 上的 tag）。
      - 命中监听与 DoT 调用
          - 统一从一个 OnHit 中间件出发：若 SoulBeastState.active，则每次近战命中：
              - 消耗 18 魂魄（玩家：用 GuzhenrenResourceCostHelper.consumeStrict(player, 18, 0)；非玩家：默认跳过或按需
  走生命折算）
              - 调度 DoT：使用 DoTManager 施加“魂炎”（黑色火焰）→ 每秒 真伤 = maxHunpo * 0.01，持续 5 秒；忽略护甲/抗性
          - 这部分 HunDaoSoulBeastBehavior 已有雏形：继续复用/充实它，或将计算与扣资下沉到 HunDaoMiddleware。
  - 资源统一调度（溢散与恢复）
      - 每秒溢散（魂兽）
          - 在 HunDaoMiddleware 中增加 leakHunpoPerSecond(Player, amount=3.0)；由 HunDaoSoulBeastBehavior.onSlowTick
  调用。
      - 每秒恢复（小魂/大魂）
          - 小魂蛊（已实现）: XiaoHunGuBehavior.handlerPlayer 每秒 +1 并通过联动 HUN_DAO_INCREASE_EFFECT 提供 +20% 效率。
          - 大魂蛊（新增行为）: DaHunGuBehavior 每秒 +2 魂魄 +1 念头；若胸腔内存在小魂蛊则施加“魂心”并将
  SoulBeastState.setPermanent(true)。
              - 需要一个“器官存在性”工具：OrganPresenceUtil.has(cc, itemOrId)；已有模式可参考
  ShuangXiGuOrganBehavior.hasBingJiGu 并抽到共享 util。
      - 资源桥
          - 继续复用 GuzhenrenResourceBridge 的 ResourceHandle.read/adjustDouble/clampToMax 接口，所有读写都经它走。
  - 魂慑（范围震慑）
      - 中心工具: HunDaoAuraHelper.java
          - applyDeterAura(Player source, double radius): 扫描半径 R（可配置），对于“生命值 < 当前魂魄量”的敌对生物，施
  加“震慑”状态
          - 实现手段：若无自定义效果，可用组合：短时 Weakness + Slowness + brief Stun-like（若无“眩晕”可用
  MovementSlowdown + 攻速极低 + 短免疫窗口模拟）
          - 调用时机：当 SoulBeastState.active（尤其 permanent）时，每 1s 执行一次 aura；强度随 hunpo 变化（阈值判断由
  hunpo 读数得出）
  - 行为注册（Registry 绑定）
      - HunDaoOrganRegistry
          - 目前仅注册了 XiaoHunGuBehavior。需新增：
              - 大魂蛊 DaHunGuBehavior（SlowTick + Removal + ensureAttached + onEquip）
              - 魂兽 HunDaoSoulBeastBehavior（SlowTick + OnHit + Removal + ensureAttached + onEquip）
          - 确保与物品 id 对应（guzhenren:dahungu、小魂蛊 id、以及魂兽行为的触发来源）
  - HUD/FX/音效
      - 黑色火焰 FX：在 DoT 施加与每秒 tick 时发送粒子与音效（需注册自定义音效 id；参考之前 break_air 的注册缺失问题，必
  须用 DeferredRegister 注册 SoundEvent）。
      - GUI 资源：Hunpo 条材质/颜色主题。
  - 配置与常量集中
      - HunDaoBalance.java 或 config 节点
          - SOUL_BEAST_HUNPO_LEAK_PER_SEC = 3.0
          - SOUL_BEAST_ON_HIT_COST = 18.0
          - SOUL_FLAME_DPS_FACTOR = 0.01, SOUL_FLAME_DURATION = 5s
          - XIAO_HUN_RECOVER = 1.0, XIAO_HUN_RECOVER_BONUS = 0.2
          - DA_HUN_RECOVER = 2.0, DA_HUN_NIANTOU = 1.0
          - DETER_RADIUS, HUD_TOGGLE 等
      - 统一从一处读取，便于平衡与服主配置。
  - 工具/辅助
      - OrganPresenceUtil：快速查胸腔是否含某器官/ID（抽取自多处 contains 循环）。
      - CombatEntityUtil：判断近战/抛射来源、敌我阵营（已有部分逻辑，整合一下）。
      - TrueDamageHelper：封装 DoT 的“真伤”逻辑；DoTManager 里应已有处理，暴露一个公共入口即可。
      - SaturationHelper：温和维持饱食/饱和（上限、不覆盖食物效果），供魂兽状态被动调用。
      - LongevityGuard（可选）：“寿元不自然消耗”若来自外部 Mod，需要在我们侧定期“回填/钳制”，提供兼容层：
  whileSoulBeastClampShouYuan(...)。
  - 测试建议
      - DoTManager: 到期触发/叠加/目标死亡/卸载分支。
      - 资源消耗: 18 魂魄不足时拒绝施加；refund/回滚路径（若未来用事务）。
      - HUD: 客户端仅在 SoulBeastState.active 时显示；切换维度/重登保持一致。
      - Aura: 阈值判断正确（生命值 < hunpo 时被震慑）。

魂道杀招 自定义
魂道，魂魄，野兽，改造

鬼气蛊（肌肉）:每秒恢复3点魂魄值和1点精力值
被动:魂道攻击额外造成等同于当前魂魄值上限1％的真伤，
当角色处于魂兽状态获得［噬魂］，击杀生物有12％概率提升所击杀生物0.1％生命值的魂魄值上限，同时损伤5％魂魄稳定度
主动技［鬼雾］:会在对方眼前生成一团黑雾，使敌人迷失方向

魂道分魂实现计划：
1. 分魂先实现以下几种AI逻辑 1. 修炼 2.照看蛊材农田 3. 守护玩家 4.狩猎 5.逃跑 (可以通过 NeoForge 的 AI Goal 系统)
2. 能否实现伪玩家逻辑？即可以被服务器判定为玩家 类型 (继承 FakePlayer, FakePlayer 的行为在服务器端完全等价于玩家，实体类型是 LivingEntity + Player 混合体)
3. 玩家/分魂沟通渠道-ingame /message 直接实现？ 注册 /soul <id> 命令 --(扩展 最后再实现LLM接入 ) 命令
 有 chat control command 
 chat就是和分魂对话，先实现简单的提取关键词回复
 control 实现分魂切换
 command 实现切换AI逻辑

4. 玩家能在分魂之间切换 (PlayerData 快照与恢复
灵魂注册与切换
可接入 AI 分魂控制（FakePlayer）)


---

## 🧩 一、核心目标

> 实现一个“换魂 / 分魂”系统：
> 玩家拥有多份独立的“魂数据”（SoulProfile），可在其中切换，
> 每个魂都有独立的属性、装备、修炼状态、蛊真人资源（真元、境界、阶段等），
> 并能被 AI 控制或玩家附身。

系统需要兼容原版机制、NeoForge 事件体系，以及外部模组（尤其是蛊真人）。

---

## 🧠 二、核心结构：多魂容器与魂快照

每个玩家都有一个**魂容器**（SoulContainer），负责管理多个“魂档案”（SoulProfile）。

* **SoulContainer**：挂在玩家身上的全局数据容器

  * 记录所有已拥有的魂（UUID → SoulProfile）
  * 记录当前激活的魂（ActiveSoul）

* **SoulProfile**：单个魂的数据快照

  * 物品栏、装备、经验、生命值、药水效果
  * 玩家属性（力量、敏捷等）
  * 外部模组能力快照（例如 Curios 插槽、ChestCavity 器官）
  * 蛊真人特有资源状态（真元、境界、空窍、修炼进度等）

---

## ⚙️ 三、换魂的基本机制

魂切换由三步组成：

1. **捕获当前魂状态**

   * 保存玩家的原版数据（背包、属性、经验、药水）
   * 保存所有模组能力（通过 Adapter 系统）
   * 保存蛊真人字段（通过 `GuzhenrenResourceBridge`）

2. **应用目标魂状态**

   * 清空并写入目标魂的数据
   * 恢复对应能力与模组扩展数据
   * 恢复蛊真人核心资源（真元、境界等）

3. **同步到客户端**

   * 重发状态或调用模组同步接口
   * 让客户端 UI、属性、修炼界面即时刷新

换魂完成后，玩家表现为“同一身体，不同灵魂”，
所有属性、修炼线、模组数值完全切换。

---

## 🔧 四、跨模组兼容设计：SoulAdapter 机制

不同模组的数据结构不同，因此系统设计一层 **适配器（SoulAdapter）** 来处理模组特定数据。

* 每个适配器只负责一个模组；
* 提供三个动作：检测、捕获、应用；
* 例如：

  * CuriosAdapter：保存饰品栏状态；
  * ChestCavityAdapter：保存器官配置；
  * GuzhenrenAdapter：保存蛊真人资源字段。

通过注册适配器列表，换魂时会自动执行所有匹配模组的捕获与恢复逻辑。

---

## 🔮 五、GuzhenrenResourceBridge 的作用

`GuzhenrenResourceBridge` 是整个系统的关键桥梁。
它通过反射访问蛊真人模组的 `PlayerVariables` 类，实现安全读写和同步：

* 能读取与写入所有蛊真人的核心字段（真元、最大真元、转数、境界、修炼进度等）；
* 提供 `syncPlayerVariables` 方法，主动触发蛊真人自己的网络同步逻辑；
* 允许系统在不直接依赖蛊真人 API 的情况下与其交互；
* 可以在每个魂切换时独立保存与恢复修炼状态，实现“多魂多修炼路线”。

换魂时：

1. 捕获当前魂的 Guzhenren 字段；
2. 写回目标魂的 Guzhenren 字段；
3. 调用同步方法，让客户端 UI 立即刷新。

这就保证了蛊真人模组的数据与“魂系统”完全联动。

---

## 🧬 六、系统数据流总结

```
玩家（Player）
 │
 │ 挂载 SoulContainer (能力)
 │
 ├─ 当前激活魂 SoulProfileA
 │    ├─ 原版属性/装备/经验
 │    ├─ Forge 能力数据
 │    ├─ 模组扩展 (Curios, ChestCavity)
 │    └─ Guzhenren 字段映射
 │
 └─ 备用魂 SoulProfileB, SoulProfileC...
```

当执行 `/soul switch`：

1. 保存 SoulProfileA；
2. 应用 SoulProfileB；
3. 更新蛊真人资源；
4. 触发同步与事件广播；
5. 客户端即时刷新。

---

## ⚡ 七、体外化身与伪玩家控制（扩展）

系统还支持生成“分魂化身”（FakePlayer）：

* 绑定到本体玩家；
* 继承对应 SoulProfile 的所有属性与能力；
* 可由 AI 或玩家输入控制；
* 支持视角切换（附身控制）；
* 能单独修炼或守护本体。

在换魂逻辑上，“体外化身”只是另一个 **载体实体**，
其内部依然存放一个完整 SoulProfile。

---

## 🧠 八、魂系统与 NBT 同步的整合原则

* 所有原版与模组字段，最终都能序列化为 NBT；
* 每个魂都有一份完整的 NBT 快照；
* 只要调用 `saveWithoutId()` / `load()`，就能恢复；
* Guzhenren 资源用 `ResourceBridge` 精准同步；
* 其他模组通过 Adapter 精准同步；
* 不破坏网络连接与核心玩家对象；
* 可以无缝在多人服务器运行。

---

## 🪶 九、整体换魂流程总结（文字版流程图）

1️⃣ 玩家输入 `/soul switch next`
2️⃣ 系统获取当前魂容器
3️⃣ 捕获当前魂状态 → 存档
4️⃣ 找到目标魂 → 读取其数据
5️⃣ 覆盖玩家背包、属性、能力
6️⃣ 覆盖 Guzhenren 字段（真元、境界等）
7️⃣ 触发 Guzhenren 同步方法（客户端刷新）
8️⃣ 发送客户端同步包（更新HUD/UI）
9️⃣ 更新当前魂ID并保存
🔟 广播事件：`SoulSwitchedEvent`（供AI/特效使用）

---

## 🧩 十、系统特性总结

| 模块                          | 作用        | 特点                 |
| --------------------------- | --------- | ------------------ |
| **SoulContainer**           | 管理所有魂数据   | 绑定在玩家 Capability 上 |
| **SoulProfile**             | 单个魂的快照    | 可持久化与序列化           |
| **SoulAdapter**             | 模组数据适配    | 可注册多种模组            |
| **GuzhenrenResourceBridge** | 反射访问蛊真人字段 | 实现修炼体系切换           |
| **SoulSwitchEngine**        | 执行切换逻辑    | 统一同步、事件触发          |
| **FakeSoulEntity**          | 分魂化身      | 可被AI控制或附身          |
| **LLM/AI层（未来）**             | 自动修炼/决策   | 可独立驱动分魂行为          |

---

## ✅ 十一、最终效果（设计目标）

* 每个魂是一个完整修炼线，独立真元与境界。
* 玩家可以在不同魂之间切换，实现“多修炼体系”。
* 分魂化身可独立行动、修炼、守护、战斗。
* Guzhenren 模组的所有真元/境界/阶段字段都能精确同步。
* 不需要直接依赖蛊真人或其他模组的源码。
* 与 NeoForge 架构完全兼容。
* 未来可扩展到 LLM 自主AI魂。

---

**一句话总结整个系统：**

> “SoulProfile 负责保存灵魂的数据，SoulContainer 负责管理它们，SoulSwitchEngine 负责切换，GuzhenrenResourceBridge 负责与修炼系统桥接。
> 这样就能在同一玩家内实现真正意义上的‘多魂共修’与‘灵魂互换’。”

Player（主魂实体）
│
├── Container (Capability)
│     ├── 当前操控魂 ID
│     ├── 所有 SoulProfile
│     │     ├── 物品存储 (InventorySnapshot)
│     │     ├── 能力存储 (CapabilitySnapshot) ） (预留接口) 
│     │     ├── 器官信息 (ChestCavitySnapshot) ）(预留接口)
│     │     ├── 蛊真人数据 (GuzhenrenSnapshot) ）(预留接口)
│     │     └── 其他模组附着数据                 (预留注册接口)
│     │
│     └── SoulPlayer 实例引用（活跃或分魂实体）
│
└── SoulPlayer (FakePlayer subclass)
      ├── InventoryHandler（独立背包）
      ├── CapabilityHandler（Curios(预留接口) / ChestCavity / Guzhenren）
      ├── Owner UUID 引用
      ├── Tick 行为（修炼/守护/狩猎）(预留接口)
      └── VisualEntity 链接（客户端表现）


火人蛊（      "item.guzhenren.huorengu": "火人蛊",）:每秒恢复0.5％生命值，
每秒恢复4点精力，
获得飞行能力（长按SPACE键飞行,类似于喷气背包？获得持续向上的推力）和火焰抗性，
胸腔内只能生效一个火人蛊
{
  "itemID": "guzhenren:huoxingu",
  "organScores": [
    { "id": "chestcavity:fire_resistant", "value": "2" },
    {"id":"chestcavity:health","value": "4"}
  ]
}

联动效果:
火心孕灵:由火心蛊&火人蛊组成，炎道效率提升26％，
获得［火灵］效果，获得急迫1，
攻击会为对方施加持续10分钟的火焰，获得一个很帅的火焰特效(粒子特效)


大魂蛊（心脏）:每秒恢复2点魂魄值和1点念头；
当胸腔内存在小魂蛊将获得以下特殊增益，
若角色并非魂兽将获得［魂意］，胸腔内每有一只魂道蛊虫[HunDaoOrganRegistry实现一个List注册]，魂魄恢复效率提升1％，最高提升20％；
如果角色处于魂兽状态将获得［威灵］，攻击消耗的魂魄值降低10点，同时会对所有当前生命值小于角色魂魄值的敌对生物产生威慑

鬼气蛊（"item.guzhenren.guiqigu": "鬼气蛊",）:每秒恢复3点魂魄值和1点精力值  
被动:
攻击会造成等同于当前魂魄值上限1％的真伤magic，
当角色处于魂兽状态获得［噬魂］，
击杀生命值>40的生物有12％概率提升所击杀生物0.1％生命值的魂魄值上限，
同时损伤5％魂魄稳定度  
主动技:释放技能［鬼雾］会在敌人眼前生成一团黑雾(黑色粒子效果)，使敌人迷失方向(缓慢4，失明)

{
  "itemID": "guzhenren:xiao_hun_gu",
  "organScores": [
    { "id": "guzhenren:zuida_hunpo", "value": "66" },
    {"id":"chestcavity:health","value": "4"}
  ]
}

三转全力以赴（      "item.guzhenren.quan_li_yi_fu_gu": "全力以赴蛊", ）：
每15秒消耗500真元恢复5点精力，
当胸腔内的肌肉类越多(判断json是否提供strength)，
精力回复越快，
每个肌肉类蛊虫提供0.5，最高恢复上限15。不可叠加。

{
  "itemID": "guzhenren:quan_li_yi_fu_gu",
  "organScores": [
    {"id":"chestcavity:defense","value": "1"},
    {"id":"chestcavity:nerves","value": "1"},
    {"id":"chestcavity:strength","value": "32"}
  ]
}


三转自力更生（      "item.guzhenren.zi_li_geng_sheng_gu_3": "自力更生蛊", 肾脏）：
每10秒消耗500真元恢复2点生命。
释放主动技后，将消耗胸腔内的肌肉器官（正则判断是否为chestcavity:*muscle即可），
获得30秒的生命恢复，结束后获得30秒的虚弱，消耗肌肉时播放进食声音。
不可叠加。

{
  "itemID": "guzhenren:zi_li_geng_sheng_gu_3",
  "organScores": [
    {"id":"chestcavity:filtration","value": "1"},
    {"id":"chestcavity:nerves","value": "1"},
    {"id":"chestcavity:strength","value": "32"}
  ]
}

体魄蛊（肌肉）:每秒恢复3点魂魄值和1点精力
被动:若为魂兽，攻击 额外造成等同于 当前魂魄值上限(1+魂道INCREASE EFFECT)％的真伤 消耗0.1%的魂魄，
如果角色并非魂兽，获得［滋魂哺身］效果，魂道INCREASE EFFECT提升10％，
同时根据当前魂魄值上限的(0.5*(1+魂道INCREASE EFFECT))％获得伤害吸收(每10s刷新一次)，
［滋魂哺身］不可叠加

{
  "itemID": "guzhenren:xiao_hun_gu",
  "organScores": [
    { "id": "guzhenren:zuida_hunpo", "value": "77" },
    {"id":"chestcavity:strength","value": "32"}
  ]
}

Bing_xue_dao
冰布蛊（  "item.guzhenren.bing_bu_gu": "冰布蛊",）：
当背包内存在冰块或是雪球时，
冰布蛊将产生「进食」，
进食冰块儿时将获得生命恢复效果 II；
进食雪球时将获得饱和效果，两者同时存在于背包内时
，以雪球为优先级。不可叠加。

{
  "itemID": "guzhenren:bing_bu_gu",
  "organScores": [
    {"id":"chestcavity:nutrition","value": "2"},
    {"id":"guzhenren:daohen_bingxuedao","value":"1"}
  ]
}



自然恢复的基础节奏在原版 KE1Procedure 中写死：每秒（每 tick 加/20）按条件累加到 niantou_zhida，随后统一夹到
  niantou_rongliang。增量来源有三档：

  - 体质=8 时每秒 +1.0
  - 种族=0（人族）时额外 +0.2
  - 非种族=1（即除魔族外的其余族群）时额外 +0.01

  假设玩家同时满足这些条件，满额自然回复约为 1.21 念头/秒，且总数不会突破 niantou_rongliang。 Louise 等
  Buff（NIANTOUHUIFU 状态）会叠加额外恢复量，数值由 NianTouHuiFu 效果强度和部分道痕加成公式 (0.05 + amplifier/100) * (1
  + dahen_zhidao * 0.001) 决定，再受 WEI_LI_FAN_BEI 倍增。


非常好👌 那我用你指定的格式来重排前面六个蛊虫的定义（以 Markdown 呈现，结构清晰、便于直接粘贴到设计文档或注册表中）。

---

### 🟥 火衣蛊（肌肉）

**itemID:** `guzhenren:huo_gu`

**被动:**
每秒对radius: 10格内 Entity 造成(0.5) * (1+炎道INCREASE_EFFECT)点燃烧伤害，持续5秒后消失，15秒后可再次释放，不可叠加。

**主动 (键位默认为 ATTACKABILITY):**
释放后Entity每秒受到持续燃烧伤害并附加缓慢Ⅰ，持续10秒，冷却15秒。

**organScores:**

```json
{
  "organScores": [
    {"id": "chestcavity:strength", "value": "3"},
    {"id": "chestcavity:speed", "value": "-1"},
    {"id": "guzhenren:daohen_yandao", "value": "1"}
  ]
}
```

---

### 🟥 火龙蛊（脊椎）

**itemID:** `guzhenren:huolonggu`

**被动:**
［龙旺火运］：气运恢复效率提升10％。

**主动 (键位默认为 ATTACKABILITY):**
10秒内可释放两次。首次释放时发射火龙造成150基础伤害并爆炸（爆炸伤害为生命上限10%）。命中后自动触发第二段，从高空俯冲造成300基础伤害并爆炸，扣除自身30%生命值；每扣15滴血伤害提升5%，上限40%。冷却20秒。

**organScores:**

```json
{
  "organScores": [
    {"id": "guzhenren:zuida_qiyun", "value": "20"},
    {"id": "guzhenren:daohen_yandao", "value": "3"},
    {"id": "chestcavity:strength", "value": "2"}
  ]
}
```

---

### 🟥 龙丸蛐蛐蛊（肋骨）

**itemID:** `guzhenren:long_wan_qu_qu_gu`

**被动:**
不可叠加。

**主动 (键位默认为 ATTACKABILITY):**
消耗50精力与200真元，获得10秒自动闪避（最多触发三次），冷却30秒。

**organScores:**

```json
{
  "organScores": [
    {"id": "guzhenren:max_jingli", "value": "5"},
    {"id": "guzhenren:max_zhenyuan", "value": "10"},
    {"id": "chestcavity:nerves", "value": "1"}
  ]
}
```

---

### 🟥 黑豕蛊（肌肉）

**itemID:** `guzhenren:hei_shi_gu`

**被动:**
每3秒恢复1点精力。当玩家攻击时，有6%的概率召唤出[力影]，复制玩家动作造成10%伤害。[力影]冷却20秒。不可叠加。

**主动 (键位默认为 ATTACKABILITY):**
无。

**organScores:**

```json
{
  "organScores": [
    {"id": "guzhenren:jingli", "value": "1"},
    {"id": "guzhenren:max_jingli", "value": "3"},
    {"id": "chestcavity:strength", "value": "2"}
  ]
}
```

---

### 🟥 白豕蛊（肌肉）

**itemID:** `guzhenren:bai_shi_gu`

**被动:**
每3秒恢复1点精力。当玩家攻击时，有6%的概率召唤出[力影]，复制玩家动作造成10%伤害。[力影]冷却20秒。不可叠加。

**主动 (键位默认为 ATTACKABILITY):**
无。

**organScores:**

```json
{
  "organScores": [
    {"id": "guzhenren:jingli", "value": "1"},
    {"id": "guzhenren:max_jingli", "value": "3"},
    {"id": "chestcavity:strength", "value": "2"}
  ]
}
```

---

### 🟥 焚身蛊（肾脏）

**itemID:** `guzhenren:fen_shen_gu`

**被动:**
着火时每秒恢复3点精力，20%概率免疫中毒，获得［灵火庇护］固定抵御15点伤害（最多叠加2层）。当胸腔内存在火心蛊和火人蛊时，不再需要着火，同时获得火焰免疫。

**主动 (键位默认为 ATTACKABILITY):**
无。

**organScores:**

```json
{
  "organScores": [
    {"id": "chestcavity:filtration", "value": "2"},
    {"id": "chestcavity:fire_resistant", "value": "2"},
    {"id": "guzhenren:max_jingli", "value": "5"},
    {"id": "guzhenren:daohen_yandao", "value": "2"}
  ]
}
```

灵光一闪（脊柱）：

每秒回复3点念头，
装备了该器官的生物自身念头上限增加2000点，
自然念头恢复提升300点。
若装备该器官的生物体质为逍遥智心体[tizhi == 8]，
则每秒回复的念头恢复效率提升10%(也就是0.12/s)。不可叠加

ChestCavityForge/src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/zhi_dao/...
灵光一闪（  "item.guzhenren.ling_guang_yi_shan_gu": "灵光一闪蛊", ）：
消耗1000真元，每秒回复3点念头(若是超过自然上限"念头容量"则不添加)，

若装备该器官的 Player 体质为逍遥智心体[tizhi == 8]，
则每秒回复的念头恢复效率提升10%(也就是0.12/s)。(此效果不可叠加)

JSON 
{
  "itemID": "guzhenren:ling_guang_yi_shan_gu",
  "organScores": [
    {"id":"guzhenren:niantou_zhida","value": "2000"},
    {"id":"guzhenren:niantou_rongliang","value": "300"},
    {"id":"chestcavity:nerves","value": "1"}
  ]
}

ChestCavityForge/src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/yan_dao/...
火衣蛊（肌肉）：
主动效果(为 ATTACKABILITY )：消耗50真元，5饱食度 生成一个特殊的范围燃烧效果，
使用期间会每秒受到 (5*(1+炎道INCREASE_EFFECT)) 持续燃烧DoT伤害 会附加缓慢(1*(1+炎道INCREASE_EFFECT))，持续10s。(冷却11s) 使用自定义音效chestcavity:fire_huo_yi
被动效果：对Radius: 10格内的目标进行，每秒(0.5*(1+炎道INCREASE_EFFECT))的持续DoT伤害5秒后消失，
5秒后会再次释放。

JSON 
{
  "itemID": "guzhenren:huo_gu",
  "organScores": [
    {"id":"guzhenren:speed","value": "0.1"},
    {"id":"guzhenren:strength","value": "16"},
    {"id":"chestcavity:fire_resistant","value": "1"}
  ]
}

- 示例：在服务器侧、拿到 ServerPlayer performer 后调用：

AbilityFxDispatcher.play(
    performer,
    net.minecraft.resources.ResourceLocation.parse("chestcavity:fire_huo_yi"),
    net.minecraft.world.phys.Vec3.ZERO, // 原点偏移；可换成 new Vec3(0,1,0) 等
    1.0F                                // 强度（>0 即可）
);

火龙蛊（脊椎）：
气运上限提升至20，消耗真元
被动：［龙旺火运］气运恢复效率提升10％
主动技：这只蛊可以在10秒内释放两次，冷却20秒
首次释放时向前发射一条火龙，同时玩家强制飞行停滞半空，
（火龙基础伤害150），命中目标后会发生爆炸（爆炸伤害是当前生命值上限10％的伤害），
同时命中目标后会自主触发二次释放，由高空中化身火龙从高空俯冲前方目标命中后爆炸，
触发爆炸时会扣除当前生命值30%血量（爆炸伤害血量越低伤害越高，每扣15滴血伤害提升5%，
上限40％，基础伤害300），不可叠加

ChestCavityForge/src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/li_dao/...
龙丸蛐蛐蛊（肋骨）：
使用主动技(为 ATTACKABILITY )动闪避，闪避最多触发三次。冷却30秒。不可叠加。
自动闪避效果可用: performShortDodge(但是具体无敌效果还是需要你去实现,例如：在 dodge 开始时登记一个短暂的“无敌窗口”，在 LivingIncomingDamageEvent（或 LivingHurtEvent) 中检查该标记，命中时直接
     event.setCanceled(true) 或把伤害降为 0；
{
  "itemID": "guzhenren:long_wan_qu_qu_gu",
  "organScores": [
    {"id": "guzhenren:max_jingli", "value": "5"},
    {"id": "guzhenren:max_zhenyuan", "value": "10"},
    {"id": "chestcavity:defense", "value": "4"},
    {"id": "chestcavity:speed", "value": "0.1"},
  ]
}

ChestCavityForge/src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/li_dao/...
[力影] 通用工具库
src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/common/ShadowService.java
[力影] 逻辑： 生成 力影， 攻击 玩家攻击的 entity (punch音效：SoundEvents.createVariableRangeEvent(new ResourceLocation("chestcavity","custom.fight.punch")) 播放此音效。)， 消失 (玻璃破碎音效)

黑豕蛊（肌肉）：每3秒恢复1点精力，当玩家攻击时，有6%的概率召唤出[力影]，
「力影」将重复玩家的当前动作并造成相同伤害的(10*(1+力道INCREASE_EFFECT))%。
[力影]冷却20秒。不可叠加。
{
  "itemID": "guzhenren:long_wan_qu_qu_gu",
  "organScores": [
    {"id": "guzhenren:max_jingli", "value": "5"},
    {"id": "chestcavity:strength", "value": "8"},
    {"id": "chestcavity:speed", "value": "0.05"}
  ]
}

白豕蛊（肌肉）：每3秒恢复1点精力，当玩家攻击时，有6%的概率召唤出[力影]，
「力影」将重复玩家的当前动作并造成相同伤害的(10*(1+力道INCREASE_EFFECT))%。
[力影]冷却20秒。不可叠加。
{
  "itemID": "guzhenren:long_wan_qu_qu_gu",
  "organScores": [
    {"id": "guzhenren:max_jingli", "value": "5"},
    {"id": "chestcavity:strength", "value": "4"},
    {"id": "chestcavity:speed", "value": "0.1"}
  ]
}

ChestCavityForge/src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/yan_dao/...
焚身蛊（肾脏）：

处于着火状态时，每秒恢复3点精力值，
有每秒 20％概率 去除中毒debuff(如果有)，获得［灵火庇护］固定减免12点伤害（灵火庇护最多叠加n个）；n=2
当胸腔内存在火心蛊和火人蛊时，自身将被赋予永久着火状态，同时获得火焰免疫。

  "detoxification": 0.2,
  "fire_resistant": 2,


{
  "itemID": "guzhenren:fen_shen_gu",
  "organScores": [
    {"id": "guzhenren:max_jingli", "value": "20"},
    {"id": "chestcavity:fire_resistant", "value": "2"},
    {"id": "chestcavity:detoxification", "value": "1"}
  ]
}

ChestCavityForge/src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/li_dao

蓄力蛊（肋骨）：
攻击时有12%概率打出「蓄力一击」，本次攻击将造成1.4*(1+力道INCREASE_EFFECT)倍伤害。
胸腔内的肌肉越多（只判定肌肉器官(chestcavity:*muscle(包括muscle))，判定已有通用工具实现），
概率越高，每组（16个）肌肉器官提高0.5%概率，最高不超过23%。不可叠加。
---
提醒 · 待验证（Soul 模块）

- 副手临时交换（SoulPlayerInput.useWithOffhandSwapIfReady）
  - 当前已加“安全归还”与冲突兜底（优先槽非空则尝试 add，剩余丢地；先清空副手再归还；恢复原副手快照）。
  - 仍需集成测试覆盖极端场景（使用期间槽位被其他逻辑写入、冷却/失败路径等）。
  - 待办：落地测试用例/服内脚本验证；必要时增加 DEBUG 日志记录归还路径。

- 日志降噪已生效
  - 启用详细日志：`-Dchestcavity.debugSoul=true`（INFO 级别），导航详细：`-Dchestcavity.debugNav=true`。
  - 关闭后仅保留必要 WARN/ERROR。
  - 消息开关（玩家聊天/系统提示）：
    - 关闭所有分魂消息：`-Dchestcavity.soul.msgEnabled=false`
    - 仅关闭逃跑消息：`-Dchestcavity.soul.fleeMsgEnabled=false`
    - 调整消息冷却：`-Dchestcavity.soul.msgCooldownTicks=200`（单位 tick）

- 可调开关（JVM 参数）
  - 自愈：
    - `-Dchestcavity.soul.selfHealCooldown`（默认 20tick）
    - `-Dchestcavity.soul.selfHealHealthFrac`（默认 0.60）
    - `-Dchestcavity.soul.selfHealMinMissing`（默认 4.0）
  - 台阶上抬冷却：`-Dchestcavity.soul.stepAssistCooldown`（默认 8tick）

下一步（计划）
- P1：
  - 为“安全归还”路径加 DEBUG 日志（仅 `debugSoul=true` 时）。
  - 将背景快照/切换细节 INFO 切换为 DEBUG（受开关控制）。
- P2：
  - 路径平滑（节点前瞻/直射跳点）。
  - 两栖偏好（长水域优先上岸绕行）。
  - 上抬前快速空域/前向 AABB 探测以减少误判。


花豕蛊（肌肉）：
每5秒消耗200真元恢复3点精力，
释放主动技后，将消耗300真元获得10秒的力量。

—

2025-10-08 Soul 每秒回调挂载
- 已在 SoulRuntimeHandlers 引导时注册 `GuzhenrenZhuanshuSecondHandler`，每秒读取 `zhuanshu` 字段；当 `zhuanshu != 0.0` 时触发占位 handler（仅 DEBUG 日志）。
- 后续可在该 handler 内扩展实际逻辑（如状态门控/FX/增益）。
