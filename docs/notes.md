为了可玩性，设定 " 三转开始才能稳定替代 原生器官 "

"item.guzhenren.gu_zhu_gu": "骨竹蛊", 骨竹 - 生长 - 强化 - 持续性 - 加快/(或者x转前唯一充能方法?)其他骨道蛊虫充能 - 使用 真元 (被动增长) / 食用骨粉(催化) (主动)


## SoulCultivatorEntity 实施计划书（草案）

### 目标与范围
- 构建可独立存在的“修炼型灵体”实体，复用 SoulPlayer 的资源、LedgerOps、MultiCooldown 等体系。
- 支持自主修炼、战斗与守护模式切换，可由玩家下达命令并在离线时保持状态。
- 集成 GeckoLib 模型/动画，实现站立、修炼、移动、战斗、突破等表现。
- 提供 ModernUI 配置与快捷键联动，后续开放 API 供其他模组扩展修炼事件。

### 阶段划分与预估
1. **阶段 0｜需求冻结（1 天）**  
   - 明确实体定位、交互场景、器官/资源兼容要求。  
   - 产出需求文档与成功判定指标（存活率、修炼收益、动画覆盖度）。
2. **阶段 1｜基础框架（5–7 天）**  
   - 定义 `SoulCultivatorEntity`（或抽象基类）并复用 SoulPlayer 附件。  
   - 搭建修炼/巡逻/守护的子大脑逻辑，落地资源循环与安全持久化。  
   - 接入现有导航/传送体系与命令接口。
3. **阶段 2｜GeckoLib 可视表现（4–6 天）**  
   - 准备模型、材质与动画文件；实现渲染器与状态驱动。  
   - 同步修炼阶段粒子/音效，并通过 DataTracker 保证客户端一致性。
4. **阶段 3｜修炼玩法深化（6–8 天）**  
   - 设计灵气密度、突破/渡劫流程与失败惩罚。  
   - 兼容 Guzhenren 器官与主动技，接入 ModernUI 面板与快捷键。  
   - 引入扩展钩子与配置项。
5. **阶段 4｜测试与优化（4–5 天）**  
   - 编写单元/集成测试（序列化、修炼循环、命令执行、跨维传送）。  
   - 评估性能并优化导航、动画与粒子频率。  
   - 覆盖渡劫、失败回退与异常清理。
6. **阶段 5｜文档与发布（2–3 天）**  
   - 更新 `docs/` 与 `AGENTS.md`，编写 API 指南与使用说明。  
   - 制作演示素材并安排 RC 测试窗口。

### 持续任务与风险
- 配置化：将修炼倍率、守护半径、传送阈值、动画速率初始值纳入 BehaviorConfigAccess，并开放 ModernUI 调整。  
- 扩展钩子：提供注册接口让其他模组注入修炼事件、突破阶段。  
- 所有权与安全：确保仅主人或授权玩家能控制实体，离线存档不损坏。  
- 依赖风险：GeckoLib 版本兼容、SoulPlayer 资源共享的竞态、突破阶段的粒子/音效开销。  
- 验证重点：长时修炼稳定性、跨维传送、资源溢出处理与性能表现。

### 下一步行动
1. 与设计/玩法确认需求细节，冻结阶段 0 输出。  
2. 准备 GeckoLib 资产模板与命名规范。  
3. 启动阶段 1 原型实现，并在完成后同步里程碑至 `AGENTS.md` 与 TODO 列表。


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
              - 调度 DoT：使用 DoTEngine 施加“魂炎”（黑色火焰）→ 每秒 真伤 = maxHunpo * 0.01，持续 5 秒；忽略护甲/抗性
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
      - TrueDamageHelper：封装 DoT 的“真伤”逻辑；DoT 引擎里已有处理，暴露一个公共入口即可。
      - SaturationHelper：温和维持饱食/饱和（上限、不覆盖食物效果），供魂兽状态被动调用。
      - LongevityGuard（可选）：“寿元不自然消耗”若来自外部 Mod，需要在我们侧定期“回填/钳制”，提供兼容层：
  whileSoulBeastClampShouYuan(...)。
  - 测试建议
      - DoTEngine: 到期触发/叠加/目标死亡/卸载分支。
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

### 扩展自定义灵魂实体

2025-10 起，灵魂生成流程统一走 `SoulEntitySpawnRequest → SoulFakePlayerSpawner.spawn(...) → SoulEntitySpawnResult`。

1. **构造请求**：
   ```java
   var request = SoulEntitySpawnRequest.builder(owner, soulId, spawnProfile, "my-mod:reason")
           .profile(container.getOrCreateProfile(soulId))
           .entityType(myEntityType)
           .geckoModel(new ResourceLocation("my_mod", "geo/my_entity.geo.json"))
           .context(SoulEntitySpawnContext.EMPTY)
           .build();
   ```
   - `entityType` 指定要生成的实体类型；
   - `geckoModelId` 供 GeckoLib/客户端渲染引用；
   - `context` 可覆写初始坐标或附加自定义键值。
2. **调用生成器**：`SoulFakePlayerSpawner.spawn(request)` 会执行区块加载、身份校验、位置恢复以及原有的生命周期回调。
3. **处理结果**：`SoulEntitySpawnResult` 提供 `entity()`、`entityType()`、`geckoModelId()` 与 `reusedExisting()`，可据此做日志或后续逻辑。
4. **注册工厂**：通过 `SoulEntityFactories.register(type, factory)` 注入自定义构造逻辑；默认工厂继续创建 `SoulPlayer` 并保持旧行为。

> 提示：便捷场景可以使用 `SoulFakePlayerSpawner.newSpawnRequest(...)` 以现有身份缓存为模板快速创建请求。

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

 niantou_rongliang 是念头的容量上限，用于硬性裁剪当前念头并由流程事件提升


- 提醒弹窗（HUD 层）
- 用原生 Toast，最稳且与所有模组兼容。
- 做法：实现一个 Toast，通过 Minecraft.getInstance().getToasts().addToast(new ReminderToast(...)) 显示 2–5 秒消
  息，支持图标/标题/正文。
- 常驻显示（HUD 层）
- 用 NeoForge 客户端渲染事件在 HUD 顶层绘制一个小部件（右上角/血条下方等）。
- 做法：监听 RenderGuiEvent.Post（1.21.1），在回调内用 GuiGraphics 画底板与文字；如果需要 ModernUI 风格，可调用相
  同配色/圆角与文本阴影参数模拟样式。
- 弹窗（仅限打开 GUI 界面时）
- ModernUI 自带 Popup/ContextMenu 能在“已有屏幕（Screen）里”弹出模块化菜单。
- 做法：在 ModernUI 视图树内，通过 UIManager.showContextMenuForChild(view, x, y) 构建并展示；适合“背包/自定义界
  面”内的右键菜单、提示卡片。
- 注意：NeoForge 分支的 UIManagerForge.openPopup(...) 已标注弃用/不可用，建议用上面的 ContextMenu 方案或在界面布局
  中内嵌一个受控的“浮层”View。

代码骨架

- Toast 提醒
- 新建 ui/ReminderToast.java：
    - 实现 Toast 接口，持有 title、message、icon、durationMs。
    - render(GuiGraphics g, ToastComponent c, long timeMs) 中绘制背景与文本，时间到返回 Visibility.HIDE。
- 显示：Minecraft.getInstance().getToasts().addToast(new ReminderToast(...))。
- HUD 常驻显示
- 客户端注册监听：
    - 在 mod 客户端初始化里 NeoForge.EVENT_BUS.addListener(HudOverlay::onRenderHud)。
- 回调：
    - onRenderHud(RenderGuiEvent.Post e) 取 GuiGraphics g，计算锚点坐标，g.fill(...) 画底板，g.drawString(...) 输
      出文本。
    - 可接状态（魂魄/念头/冷却计时）并缓存数值，降低每帧开销。
- ModernUI 弹窗（仅界面内）
- 在已有 ModernUI Fragment/View 中，准备 ContextMenuBuilder 或自定义 View 作为“卡片”样式。
- 触发时调用 UIManager.showContextMenuForChild(originalView, x, y) 展示；或在布局层级中切换子 View 的可见性做“内嵌
  浮层”。


阴云蛊   "item.guzhenren.yin_yun_gu": "阴云蛊",
设定基础
部位： 肌肉
属性倾向： 阴性、腐蚀、吸精力
视觉关键词： 烟雾、暗流、寒气、侵蚀
被动机制：
每秒恢复 2 点生命，但吸取周围 2 格内玩家 2 点精力(若没有，则会吸取自身精力)；
每 6 秒积蓄 1 层“阴纹”，阴纹存在时攻击附带微量“蚀伤” DoT 伤害(2/s) 存在10s；
阴纹上限 20 层，每层提供 1% 物理吸血(当伤害不为Projectile)。

一、被动 FX —— “幽气缠身”
🎨 视觉目标
呈现“冷雾逆流”的感觉；
云气不再上升，而是沿肌肉线条下沉回流；
氛围偏阴、带轻微波动，像是寒气从体表蒸发。
⚙️ 粒子实现
粒子类型：smoke / 自定义 dark_mist_small；
颜色：深灰 + 紫（RGB 60, 50, 80，带透明度 0.4~0.8）；
重力 正值（+0.01），即“向下飘”；
速度衰减 0.92，随机偏移 ±0.05；
粒子尺寸 0.05~0.1，寿命 20tick；
音效：轻微“低频风呼声”。
动态逻辑
每层阴纹 → 在角色腿部与肩背生成一条逆流烟丝；
阴纹≥10层 → 烟雾略带红紫闪光，表示“侵蚀气满”；
若处于战斗中，烟丝流动速度加快，模拟能量激荡。
💡 视觉说服力
“下沉流动”是关键。它和白云蛊“向上升腾”的气流方向相反，从潜意识上就传达出“阴气凝聚”的感觉。
此外，烟雾贴体流动并带少许红光，可唤起“毒”、“蚀”、“寒”的直观感受——无需语言解释，玩家自然理解这是阴性能量。

阴云蛊 · 主动技：「雷狱引魂」
🩸 技能概念
“以阴雾为引，撕裂天幕，将敌魂摄至劫云之下。”
这是一个范围控制 + 高能雷击技能，极具视觉冲击力。
技能逻辑分解（程序结构思路）
触发条件
消耗全部「阴纹」；
冷却：20 秒；
作用半径：6 格；
云纹越多，雷击次数越多（每 5 层增加 1 道闪电）；
行为流程（伪代码）
public void activateYinCloudStorm(ServerPlayer player, int stacks) {
    Level level = player.level();

    // 1️⃣ 找出范围内生物
    List<LivingEntity> targets = level.getEntitiesOfClass(
        LivingEntity.class, 
        player.getBoundingBox().inflate(6.0D), 
        e -> e != player && e.isAlive()
    );

    // 2️⃣ 旋转 + 拉扯 + 抬升动画（物理+粒子）
    for (LivingEntity t : targets) {
        Vec3 pull = player.position().subtract(t.position()).normalize().scale(0.5);
        t.setDeltaMovement(pull.x, 0.6, pull.z); // 抬升力
        t.hurtMarked = true;

        // 环绕旋转（轻微偏移）
        double angle = (player.tickCount % 360) * 0.05;
        Vec3 offset = new Vec3(Math.cos(angle), 0, Math.sin(angle)).scale(0.3);
        t.teleportTo(t.getX() + offset.x, t.getY(), t.getZ() + offset.z);

        // 产生漩涡粒子
        spawnVortexParticles(level, t.position());
    }

    // 3️⃣ 延迟 1s 后雷击与伤害
    level.scheduleTick(player.blockPosition(), ModBlocks.YIN_CLOUD_TRIGGER, 20); // tick延迟机制
    for (int i = 0; i < Math.max(1, stacks / 5); i++) {
        BlockPos strikePos = player.blockPosition().above(3 + i);
        level.executeCommand("summon minecraft:lightning_bolt " + 
            strikePos.getX() + " " + strikePos.getY() + " " + strikePos.getZ());
    }

    // 4️⃣ 对目标造成伤害
    for (LivingEntity t : targets) {
        t.hurt(ModDamageSources.YIN_CLOUD_LIGHTNING, stacks * 2f);
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
        t.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0));
    }

    // 5️⃣ 清空阴纹
    clearStacks(player, "yin_cloud_stacks");
}
public void activateYinCloudStorm(ServerPlayer player, int stacks) {
    Level level = player.level();

    // 1️⃣ 找出范围内生物
    List<LivingEntity> targets = level.getEntitiesOfClass(
        LivingEntity.class, 
        player.getBoundingBox().inflate(6.0D), 
        e -> e != player && e.isAlive()
    );

    // 2️⃣ 旋转 + 拉扯 + 抬升动画（物理+粒子）
    for (LivingEntity t : targets) {
        Vec3 pull = player.position().subtract(t.position()).normalize().scale(0.5);
        t.setDeltaMovement(pull.x, 0.6, pull.z); // 抬升力
        t.hurtMarked = true;

        // 环绕旋转（轻微偏移）
        double angle = (player.tickCount % 360) * 0.05;
        Vec3 offset = new Vec3(Math.cos(angle), 0, Math.sin(angle)).scale(0.3);
        t.teleportTo(t.getX() + offset.x, t.getY(), t.getZ() + offset.z);

        // 产生漩涡粒子
        spawnVortexParticles(level, t.position());
    }

    // 3️⃣ 延迟 1s 后雷击与伤害
    level.scheduleTick(player.blockPosition(), ModBlocks.YIN_CLOUD_TRIGGER, 20); // tick延迟机制
    for (int i = 0; i < Math.max(1, stacks / 5); i++) {
        BlockPos strikePos = player.blockPosition().above(3 + i);
        level.executeCommand("summon minecraft:lightning_bolt " + 
            strikePos.getX() + " " + strikePos.getY() + " " + strikePos.getZ());
    }

    // 4️⃣ 对目标造成伤害
    for (LivingEntity t : targets) {
        t.hurt(ModDamageSources.YIN_CLOUD_LIGHTNING, stacks * 2f);
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
        t.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0));
    }

    // 5️⃣ 清空阴纹
    clearStacks(player, "yin_cloud_stacks");
}
视觉说服力讲解

旋转 + 拉扯：

通过反常的“空气倒流”模拟阴气的吸摄力。

玩家居中心，敌人逆流漂浮，立刻传达“灵压中心”的感觉。

视觉逻辑：气场 → 引力 → 雷爆。

抬升 + 雷击：

把目标拉至玩家上方制造“天劫执行”的仪式感；

lightning_bolt 本身是极强视觉符号，与“阴云”主题完美契合；

雷光在暗雾中显得刺眼，强烈阴阳对比。

暗色烟流 + 电弧：

烟是阴气，电是阳气；

二者相撞 → 产生“雷狱”观感。

玩家处于黑雾风眼之中，既冷静又危险。

收尾残雾：

保留短暂能量残迹，让场景不至于瞬间清空；

视觉上强化“余威犹在”的神秘感。


血战蛊  "item.guzhenren.xuezhangu": "血战蛊",
概念
「以血为誓，越战越盛。敌人流的血，便是我的力量。」
血战蛊寄生于血脉，能将失血化作力量。它不怕伤害，反而渴望流血。在生死边缘时，它让宿主化为嗜血狂魔。

被动效果：
嗜血沸腾
每当造成伤害时，积累“战血值”。
每受到一次伤害，战血值提升更快（随受伤比例(计算损失生命值相对于总生命值的比例)提升）。
战血值上限 100，每 10 点提升 3% 攻击力、2% 攻击速度。
战血值在离战状态 (单位时间内没有攻击/受到伤害) 每秒衰减 0.5 点。
战血可以为负数，表现为渴血状态，每20s会 随机扣血[1,5]区间， 5%几率会随机传送，以玩家为中心的 [1,5] r 的圆内(需要空间)，晕眩效果。

当血量低于 30% 时，自动触发“血怒”状态：
获得 +30% 近战伤害、-20% 防御。
攻击附带生命汲取（10% 伤害吸血）,每一次攻击 恢复10点精力。
持续 10 秒，冷却 30 秒。

主动技能：血誓爆发
冷却：40秒
消耗：当前生命 20%，真元 400 点
效果：
立刻获得满战血值；
半径 6 格内敌人受到一次爆裂冲击（基础伤害 40 + 每点战血 ×0.5）；
每命中一个敌人，恢复自身 5% 最大生命；
施放结束后 10 秒内，攻击伤害 +25%，生命汲取翻倍，每次攻击OnHit恢复100精力(若玩家，则额外从玩家身上吸取20精力)。


🌫️ 视觉特效（FX 设计）
被动 FX（战血增长）：
玩家身上逐渐出现红色能量脉络（半透明粒子，流动至心口）；
战血满时，心口处有持续的跳动红晕粒子特效。
主动 FX（血誓爆发）：
播放血液冲击波特效：
从身体向外炸出红色环形粒子波（类似爆破环，但为血雾）；
敌人命中后身体表面喷出短暂血花粒子；
自身获得一个心跳形的红色脉冲发光环。
-------------------------------------------------------------

直撞蛊
设计“直撞命中→重置冷却”已经构成了一个循环激励机制（momentum loop），能让玩家越打越勇。
一、核心机制：
  撞击重置冷却:
    基础效果：
    当玩家施放“直撞蛊”并成功以实体碰撞命中敌人时：
      重置技能冷却。
      造成基础冲击伤害（例如 25）。
    判定逻辑建议：
      若撞击命中非友方 LivingEntity → 立即触发冷却重置；
      若撞击命中方块或空气 → 不重置冷却，并且导向 “失误惩罚”。

二、扩展思路：
  【连撞增幅】Momentum Combo
    每次连续成功撞击都会叠加“惯性层数”：
      每层 +10% 撞击伤害与击退；
      层数上限 10；
      未在 2 秒内继续命中则层数清零；
      fx 可通过气流特效或雷光线条强化表现。

  【穿透爆发】Pierce Momentum
    若连续 3 次撞击成功（短时间内），第四次直撞将进入“爆发态”：
      冲刺距离 +50%，伤害 +80%，带小范围冲击波（AABB 2.5m）并且破坏方块。

  【灵魂交互】Spirit Echo 检测如果ChestCavity内部有 流派 = 魂道的 蛊虫 （就使用检测工具）
    每当直撞命中敌人，会唤醒“灵魂回声”：
      一个虚影短暂模仿撞击动作（0.5 秒后延迟造成额外 50% 撞击伤害）；
      特效：黑色魂体拖影；
      若连续命中 3 次 → 回声变为“灵魂连锁”可再次冲刺一次。


  一、总体风格理念：“贯星疾雷·身破风鸣”
  玩家化作一道流光撞击敌人，速度越快，光迹越亮。命中时炸裂风压或电弧，连撞时叠加动能脉冲。
  二、冲刺路径特效（过程表现）
  | 效果               | 表现                         | 技术实现建议                                                                                                                     |
| ---------------- | -------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| **风压尾迹**         | 身后拉出淡青色尾迹带（可分层：主流线 + 涡旋粒子） | 使用 `DustColorTransitionOptions` 淡蓝→白，或 `ParticleTypes.CLOUD` 高速刷新在路径上，每 tick 5~10 粒；尾迹寿命 8 tick。                           |
| **地面气浪**         | 地表出现低平“环形气波”               | `level.addParticle(ParticleTypes.SONIC_BOOM, player.x, player.y, player.z, 0,0,0)`；附加音效 `entity.warden.sonic_boom` 音高 1.3。 |
| **电弧闪动**（若为雷系直撞） | 撞击路径沿途伴随机闪电弧（紫白色电线）        | 自定义粒子或 `DustParticleOptions` (0.6,0.4,1.0)，间距 0.3m，随机旋转角度；命中目标处生成 `LightningBolt` 伪体。                                      |
                                                         |

三、命中特效（撞击瞬间）
| 场景                    | 视觉                      | 音效                                 | 环境影响                 |
| --------------------- | ----------------------- | ---------------------------------- | -------------------- |
| **普通命中**              | 白色冲击波（球形扩散 2m 半径），尘土飞散  | `entity.generic.explode`，混入短促“破风”音 | 被击中实体击退；地面短时风压粒子     |
| **连续命中（Momentum ≥2）** | 冲击波内带“环形电光”，中心闪烁强光      | 叠加“闪电啪声”                           | 附近火把摇晃 / 萤石粒子飘动（仅视觉） |
| **爆发态命中（Momentum≥3）** | 大范围（5m）电磁爆 / 红蓝交替闪      | 撞击瞬间低频轰鸣；附带耳鸣衰减音效                  | 附近树叶飘散粒子，实体浮空 0.2s   |
| **灵魂回声命中**            | 延迟 0.5s 出现半透明“灵体”复制撞击动作 | 混响型“魂鸣”音                           | 对命中目标二次伤害并留下魂迹残影 3s  |

四、连撞动能层表现（Momentum Stack 视觉反馈）
| 层数       | 特效演变              | 表现建议            |
| -------- | ----------------- | --------------- |
| 1 层      | 尾迹淡青              | 轻微光晕            |
| 2 层      | 尾迹加亮，脚下风纹闪动       | 粒子发射频率×1.5      |
| 3 层      | 尾迹发白带电弧，人物身上闪光    | 撞击时伴随机电噼啪       |
| 4 层      | 环体雷环出现，动作带残影      | 加入`SonicBoom`粒子 |
| 5 层（爆发态） | 全身被电光缠绕，短暂无敌 0.2s | 自带“雷爆”音爆特效      |
六、音效搭配节奏（声音节拍感）
| 动作阶段 | 声音参考                                                | 描述        |
| ---- | --------------------------------------------------- | --------- |
| 冲刺启动 | `entity.ender_dragon.flap`（低音处理）                    | 开启时的空气压缩感 |
| 路径经过 | `ambient.wind`（升调叠加）                                | 速度感背景音    |
| 命中瞬间 | `entity.generic.explode` + `block.anvil.land`（短促混合） | 冲击爆裂感     |
| 连撞触发 | `item.trident.thunder` + 残响延音                       | 表现势能积累    |




## 🐢 寿蛊（Shou Gu）总述

> 「借岁月一线，延片刻春秋。」
> ——蛊真人禁术之一。
> 以精血刻纹，以真元续命；寿可延，而债不灭。

---

# 💠 一年寿蛊（I 阶）

itemId:   "item.guzhenren.shou_gu": "寿蛊",
* **阶段名**：`shou_gu_1`
* **寓意**：凡人延寿之术，仅能拖延一息。
* **槽位**：肾脏（Kidney）
* **寿纹上限**：4
* **寿债阈值**：`140 + 15×寿纹层`
* **延寿被动**：见通用被动（下）
* **被动冷却 GraceShift**：55 秒
* **主动技 A1**：伤害转化率 50%，冷却 26 秒
* **主题风格**：适合凡人修士；战中自救，代价较轻，但延续时间有限。

---

# 💠 十年寿蛊（II 阶）

* **阶段名**：`shou_gu_10`
* **寓意**：以真元换十年；知命而不死。
* **槽位**：肾脏（Kidney）
* **寿纹上限**：6
* **寿债阈值**：`160 + 20×寿纹层`
* **被动冷却 GraceShift**：45 秒
* **主动技 A1**：伤害转化率 60%，冷却 22 秒
* **风格**：修真者常用版本，平衡稳定，几乎可以覆盖大多数战斗场景。

---

# 💠 百年寿蛊（III 阶）

itemId   "item.guzhenren.bainianshougu": "百年寿蛊",
* **阶段名**：`shou_gu_100`
* **寓意**：夺天地造化，延百年之命。
* **槽位**：肾脏（Kidney）
* **寿纹上限**：7
* **寿债阈值**：`190 + 20×寿纹层`
* **被动冷却 GraceShift**：40 秒
* **主动技 A1**：伤害转化率 65%，冷却 20 秒
* **风险**：偿还压力极高，一旦出错，反噬几乎等同“寿元归零”。
* **特别机制**：禁疗时，寿债增长速度 ×2。

---

# 🧩 通用被动机制

## 🕰 延寿刻印（P1）

* 战斗内每 **5s** 获得 1 层寿纹（最多见表），战斗外每 10s 也会自然积蓄。
* 每层寿纹提供：

  * +0.5 HP/s 自愈；
  * -2% 延期伤害利息；

## 💀 缓死（P2）· GraceShift

* 受到致死伤害时不死，转化为**延期伤害（Deferred Damage, DD）**；生命重设为 `1 + 护盾×0.3`。
* 获得 2 秒完全减伤、3 秒虚弱III/缓慢II；
* 冷却见各阶参数。

## 📜 寿债与利息（P3）

* DD 每秒偿还：`12 + 3×寿纹层`；
* 每消耗一层寿纹：清偿 40 DD，并获得 1s 的额外 8HP/s 回复；
* 利息：每 3s 增长未偿还 DD 的 5%，可被寿纹层数逐层降低 2%；
* 超出寿债阈值：立刻**强制清算** → 余量真实伤害 + 凋零II 8s。

---

# ⚔ 主动技：换命・续命（A1）

* 消耗：150 真元 + 20 精力；
* 清空 1~3 层寿纹，每层提升效果强度；
* 持续 6s：

  * 所受伤害 60%（按阶变化）转为 DD；
  * 每秒 +4 + 4×清空层数 HP；
  * 结束时刷新缓死冷却 -20s；
* 结束清算：期间伤害的 30% 立刻加入 DD（不可寿纹抵消）；
* 冷却见上表。

---

# ☠ 禁疗交互

* 当玩家处于 `effect.guzhenren.jin_liao`（禁疗）状态时：

  * **自愈与寿纹恢复无效**；
  * **延期伤害利息翻倍**；
  * 若正在偿还 DD，则偿还速率减半。

> 💬 提示文案：
> `"text.guzhenren.shou_gu.debuff": "禁疗状态下，寿债膨胀如潮——连岁月都拒你偿命。"`

---

# 🩸 摘除惩罚（OrganRemoval）

* 当寿蛊被摘除时：

  * 立即触发一次强制清算；
  * 当前未偿还 DD ×2（翻倍偿还），并附加 **凋零IV 10s**；
  * 若 DD 超过寿债阈值 50%，直接死亡（`DamageSource.MAGIC`）。

> 💬 提示文案：
> `"text.guzhenren.shou_gu.removal": "寿蛊离体，债无可逃——寿元燃尽。"`


---

# 🧠 逻辑实现补充

### Event Hooks

* **OnIncomingDamageListener**：拦截致死伤害 → 转化 DD
* **SlowTickListener**：每秒偿还 + 寿纹生成 + 禁疗判定
* **OnOrganRemovalListener**：倍偿清算
* **OnStatusEffectChange**：检测 `jin_liao` → 修改利息倍率与自愈速率

### 数据同步字段（capability）

| 字段                   | 类型    | 说明                  |
| -------------------- | ----- | ------------------- |
| `longevityMarks`     | int   | 当前寿纹层数              |
| `deferredDamage`     | float | 当前延期伤害              |
| `graceShiftCooldown` | int   | 缓死剩余CD              |
| `jinliaoMultiplier`  | float | 禁疗修正倍率（正常1.0，禁疗2.0） |


生机叶（肝脏）
基础机制：
每秒恢复1点生命值与100真元，表现为淡绿色粒子从胸口溢出并向心脏汇聚。

扩展创意：
被动共鸣： 当玩家处于濒死（生命值低于25%）时，生机叶会暂时加快恢复速度 ×3，持续5秒，冷却30秒。
灵根反馈： 若同腔内存在“木道”器官，则真元恢复转化率额外+10%。
生命脉动视觉： 每次恢复触发时，会出现淡绿光脉从肝脏扩散至全身，轻微闪烁呼吸节奏。
环境交互： 若玩家站在草方块或森林生物群系中，恢复效率+20%，但在沙漠或下界环境减半。



---

## 🌿 九叶生机草·进阶与养分系统（最终版）

---

### 🧩 一、核心理念

九叶生机草并非纯净的疗愈之物，而是一种以宿主生命力为温床的寄生灵草。
它的“生机”需要持续供养，当饱食度或精力不足时，它会反噬宿主吸血维生；若长期得不到滋养，则会**枯萎退化**。

这套机制让它从“纯辅助器官”变为“可成长但有代价的活体器官”。

---

### 🪴 二、进阶与消耗循环

| 状态            | 名称                  | 条件                          | 效果                         |
| ------------- | ------------------- | --------------------------- | -------------------------- |
| 🌱 I 阶：九叶初成   | 默认                  | 每秒消耗 0.5 饱食度                | 每秒恢复 1 生命 + 3 真元           |
| 🌿 II 阶：灵根稳固  | 修炼境界 ≥「2转」且真元比≥50%  | 每秒消耗 1 饱食度 + 1 精力           | 恢复 3 生命 + 真元效率+10%，主动技强化   |
| 🌳 III 阶：生命母树 | 修炼境界 ≥「3转」且连续成功催生3次 | 每秒消耗 2 饱食度 + 3 精力           | 催生冷却45s，范围+1格，自身获得滋养（真元再生） |
| 🩸 寄生状态       | 饱食/精力任一不足           | 每秒吸取玩家血量1点转化为养分（不触发恢复）      |                            |
| 🪞 枯萎状态       | 连续5秒饱食+精力+真元均不足     | 生机草退化一级，若已为I阶则进入沉眠（失效，特效消失） |                            |

---

### ⚙️ 三、实现逻辑伪代码

```java
public void onOrganTick(Player player, OrganInstance organ, long now) {
    var res = GuzhenrenResourceBridge.open(player);
    var food = player.getFoodData();
    var energy = res.getEnergy();
    var tier = organ.getData().getInt("evolution_tier");

    boolean fed = true;

    // 消耗逻辑
    switch (tier) {
        case 3 -> fed = consume(player, food, energy, 2, 3);
        case 2 -> fed = consume(player, food, energy, 1, 1);
        default -> fed = consume(player, food, energy, 0.5f, 0);
    }

    if (!fed) {
        drainBlood(player, organ);
    }

    // 连续枯萎检测
    if (fed) {
        organ.getData().putInt("wither_timer", 0);
    } else {
        int timer = organ.getData().getInt("wither_timer") + 1;
        organ.getData().putInt("wither_timer", timer);
        if (timer >= 100) { // 5秒
            decayOrgan(player, organ);
        }
    }
}

private boolean consume(Player player, FoodData food, EnergyHandle energy, float hunger, int stamina) {
    boolean ok = true;
    if (food.getFoodLevel() >= hunger) {
        food.setFoodLevel((int) (food.getFoodLevel() - hunger));
    } else ok = false;

    if (energy.getValue() >= stamina) {
        energy.consume(stamina);
    } else ok = false;

    return ok;
}

private void drainBlood(Player player, OrganInstance organ) {
    player.hurt(DamageSource.MAGIC, 1.0F);
    organ.getData().putInt("wither_timer", organ.getData().getInt("wither_timer") + 1);
}

private void decayOrgan(Player player, OrganInstance organ) {
    int tier = organ.getData().getInt("evolution_tier");
    if (tier > 1) {
        organ.getData().putInt("evolution_tier", tier - 1);
        player.displayClientMessage(Component.literal("九叶生机草因养分不足而枯萎！"), true);
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.GRASS_BREAK, SoundSource.PLAYERS, 0.7F, 0.9F);
    } else {
        organ.getData().putBoolean("withered", true);
    }
}
```

---

### 🌈 四、视觉与状态表现

| 状态           | 粒子与音效表现                          |
| ------------ | -------------------------------- |
| **正常（Ⅰ~Ⅲ阶）** | 胸腔中泛出柔和绿光，伴随轻微“呼吸光效”。            |
| **寄生吸血**     | 每秒吸血时播放轻微“滴血”音效，胸口泛红光，绿色粒子夹杂血雾。  |
| **枯萎退化**     | 器官内部光线逐渐暗淡，周围出现凋落叶片粒子，音效为“枯叶飒飒”。 |
| **重新喂养复苏**   | 若饱食与精力恢复，将闪现一次翠光复苏动画。            |

---

### 🧠 五、系统特征总结

* **平衡机制：** 通过饱食度与精力控制恢复效率，防止无限回血滥用；
* **寄生风险：** 真元与体能匮乏时反噬生命；
* **成长性：** 持续修炼可稳态运行高阶形态；
* **代谢生态：** 形成“养 → 生 → 竭 → 枯”的自然循环；
* **实现简洁：** 无需 `Linkage`，完全基于 `ResourceBridge` 与玩家状态API。
好的，下面是对你指定的“进阶与消耗循环”部分的**详细展开版**，我会完整描述每个阶段的触发逻辑、系统行为、视觉表现、内部状态变化，以及它们如何相互过渡（包括寄生与枯萎的条件）。这一部分将直接可转化为行为逻辑脚本或游戏文档说明。

---

## 🌿 九叶生机草：进阶与消耗循环（完整版）

---

### 🌱 一阶·九叶初成（基础阶段）

**触发条件：**

* 器官被成功植入胸腔后即激活。
* 无境界或真元要求。

**养分消耗：**

* 每秒消耗 `0.5` 饱食度（`foodLevel`）。
* 若饱食度耗尽，进入“寄生状态”。

**效果：**

* 每秒恢复 `1 点生命值`；
* 同时恢复 `3 点真元`（通过 `ResourceHandle#addZhenyuan(3)`）；

**视觉表现：**

* 胸口泛出淡绿色光晕；
* 周围空气略有绿色粒子（`DustColorTransitionOptions(0.6F, 0.9F, 0.6F, 0.2F)`）。
* 声效：轻微的“树叶呼吸”音。

**逻辑说明：**
此阶段代表植物刚开始与宿主建立联系，吸收极少量养分即可维生，是稳定的生长阶段。

---

### 🌿 二阶·灵根稳固（中级阶段）

**触发条件：**

* 玩家修炼境界达到「二转」；
* 当前真元比例 ≥ 50%；
* 连续 10 秒内未进入寄生状态（稳定供养期）。

**养分消耗：**

* 每秒消耗 `1 饱食度 + 1 精力（Energy）`。
* 若饱食度或精力任一不足 → 进入寄生状态。

**效果：**

* 每秒恢复 `3 点生命值`；
* 真元恢复效率 +10%；
* 主动技「催生」强化：恢复半径 +2 格，额外为友方添加 **抗性 I（3 秒）**。
* 友方恢复成功时，宿主获得 “滋养” 状态：

  * 每秒回复额外 2 点真元，持续 5 秒。

**视觉表现：**

* 从胸口延伸出翠绿色根须光纹，缓缓向地面扩散；
* 背景出现少量花粉粒子，柔和环绕；
* 启动催生时，会有“九叶同时舒展”的粒子动画。

**逻辑说明：**
此阶段代表宿主与九叶生机草的能量循环已稳定，形成了“精气互通”。宿主若能长期保持修炼供养，便会逐步向生命母树蜕变。

---

### 🌳 三阶·生命母树（高级阶段）

**触发条件：**

* 修炼境界 ≥「三转」；
* 连续成功释放主动技「催生」3次（冷却内不中断）；
* 当前真元储量比 ≥ 70%；
* 在过去 15 秒内未进入寄生状态。

**养分消耗：**

* 每秒消耗 `2 饱食度 + 3 精力`；
* 若真元储量低于 30%，强制降级回二阶；
* 若饱食与精力同时不足，进入寄生状态。

**效果：**

* 主动技「催生」强化为“生命母树形态”：

  * 冷却时间缩短至 45 秒；
  * 恢复范围增加至半径 8 格；
  * 每命中目标生命上限的 5% 以上时，为宿主生成一次性「灵蔓护盾」（吸收50伤害）；
* 自身每秒额外恢复 2 点真元（滋养自身）。

**视觉表现：**

* 胸口浮现九片光叶旋转环绕；
* 叶片顶端连接出一道淡绿灵线至天空，宛如灵树虚影；
* 当护盾生成时，会出现“花开”动画并伴随低频共鸣音。

**逻辑说明：**
此阶段是九叶生机草的共生极境。宿主已完全成为灵草的根系宿主，两者共享生机与灵气。但能量需求极大，若修炼中断即会退化。

---

### 🩸 寄生状态（资源匮乏）

**触发条件：**

* 饱食度 < 0.5 或 精力 < 1；
* 或真元储量 < 10%。

**行为：**

* 每秒吸取宿主血量 `1 点`；
* 恢复 `0.5 饱食度` 与 `1 精力`；
* 若持续 10 秒未恢复充足养分，则触发枯萎判定。

**副作用：**

* 暂停所有回血与真元恢复；
* 造成持续“虚弱 I” 与 “饥饿” 状态（5 秒）。

**视觉表现：**

* 胸口由绿光转为暗红色光晕；
* 有少量血液粒子被吸入体内；
* 声音为“血液流动”低沉声。

**逻辑说明：**
寄生状态模拟九叶生机草“自保”的本能。当宿主不再提供养分时，它会直接吸取生命来维持根系活性。

---

### 🪞 枯萎状态（退化或死亡）

**触发条件：**

* 连续 5 秒饱食度、精力、真元均不足；
* 或寄生状态持续超过 10 秒未中断。

**行为：**

* 九叶生机草退化一级：

  * Ⅲ → Ⅱ → Ⅰ → 枯萎。
* 若当前已为Ⅰ阶，则进入“沉眠”状态（完全失效）。
* 清空其全部“成功施放”与“滋养”计数。

**视觉表现：**

* 胸腔光芒完全熄灭；
* 周围落下凋零叶片粒子，轻微沙沙声；
* 若重新获得养分，则有“复苏”动画：绿光自下而上点亮。

**逻辑说明：**
枯萎并非永久死亡，而是九叶生机草进入保护性沉眠。只要宿主恢复饱食与精力，它会在数秒后自动复苏，并重新回到Ⅰ阶。

---

### 🔁 状态流转图（简述）

```
九叶初成(Ⅰ) 
   ↓ 修炼稳定 + 真元充足
灵根稳固(Ⅱ)
   ↓ 境界提升 + 连续催生3次
生命母树(Ⅲ)
   ↑              ↓ 饱食/精力不足
寄生状态 ←———————→ 枯萎/退化
   ↑
   └ 若重新获得养分 → 复苏回Ⅰ
```

---

### ⚙️ 技术实现要点

1. **数据字段存储：**

   * `tier`：当前阶段（1~3）；
   * `feed_timer`：持续供养时间；
   * `wither_timer`：连续匮乏计时；
   * `successful_casts`：主动技计数；
   * `withered`：是否枯萎。

2. **检测节奏：**
   每20 tick（1秒）调用一次 `onSlowTick` 执行养分检测。

3. **状态转换逻辑：**

   * 当 `feed_timer` ≥ 阶段要求且境界足够 → 提升；
   * 当 `wither_timer` ≥ 100 tick → 降级；
   * 当 `withered=true` 且满足饱食/精力>50% → 恢复Ⅰ阶。

4. **容错机制：**
   若服务器环境关闭真元系统（兼容模式），则以饱食+血量检测替代真元部分。

---

### 🌳 整体体验总结

| 类型       | 体验方向                        |
| -------- | --------------------------- |
| **玩法平衡** | 通过饱食与精力代价，防止无代价回血；          |
| **沉浸表现** | 粒子、音效、渐变光效强化“灵植共生”主题；       |
| **修炼感**  | 结合境界、真元与主动技表现形成成长路径；        |
| **生态循环** | 由“生→盛→竭→枯→生”构成闭环，仿佛器官自身有生命； |

  "item.guzhenren.huo_you_gu": "火油蛊",
火油蛊（胃脏）·反应驱动版 
🧩 一转 ·「燃心」

被动机制：
每次造成火属性攻击（nonProjectile）时，生成 1 层【燃油】，最多 10 层。
每层提升：
攻击 +2%。
真元恢复 + 100 BASE 

击中附加：
若当前拥有 ≥3 层【燃油】，下一次攻击（投射或非投射）命中敌方时，为目标附加：
ReactionStatuses.OIL_COATING.apply(target, duration = 120);
并消耗 3 层燃油。
视觉表现： 胸口出现流动油光粒子；击中目标时喷溅橘红火花。

二转 ·「炼焰」
被动强化：
当玩家生命值 < 50% 时，ReactionStatuses.OIL_COATING 施加时长 +50%。
主动技能「喷焰」
冷却：15 秒
消耗全部【燃油】，向前喷射火焰波（projectile 扇形域）。
命中目标自动尝试触发：

if (target.hasStatus(OIL_COATING) && player.hasStatus(FIRE_COAT)) {
    ReactionOps.triggerExplosion(target, scaleByFuelLayers());
    target.removeStatus(OIL_COATING);
    player.addStatus(FIRE_COAT_DISABLED, 60); // 屏蔽火衣反应短暂冷却
}
若无油层，仅造成常规火属性反应。

三转 ·「油尽灯明」
被动：
每当有敌方单位因“火衣 + 油涂层”爆炸死亡，恢复玩家 5% 真元，并重新获得 1 层【燃油】。
死亡触发：
若自身死亡时仍有【燃油】≥10，自动生成一次 ReactionOps.explode(player, power=medium)，并移除所有燃油层。
环境反馈：
Nether 环境下，燃油积累效率 ×1.5。
雨天 / 水下环境下，OIL_COATING 施加概率减半。

四转 ·「烈源」
联动机制：
与「火心蛊」共鸣 → 火衣反应爆炸半径 +1 格。

高阶特性：燃场余烬
【烈源态】结束时，自动生成一个燃场残余域（3 格范围，持续 4 秒）：
区域内敌方实体每秒受到火焰反应伤害（由 ReactionOps 统一计算）。
若区域内仍存在未触发的 OIL_COATING 状态，会被点燃引爆一次弱爆（非连锁）。

体验文本（突破提示）
升阶触发：「烈焰之心在胸中翻涌，火油开始自炼！」
突破成功：「真元灼心，油焰凝形，火油蛊晋入新阶！」
失败衰减：「火意不稳，燃炼未成。」

1. 燃炼积蓄（FireRefine Points, FRP）
火油蛊内部存在一个隐性资源值：fire_refine_points。
以下事件会累积 FRP：
| 触发条件             | 增加FRP      |
| ---------------- | ---------- |
| 触发「火衣 + 油涂层」反应爆炸 | +25        |
| 主动技能“喷焰”命中≥3个目标  | +15        |
| 在烈源态下持续完整5秒      | +10        |
| 身处地狱或高温环境每分钟     | +2         |
| 死亡时油量满层（自爆）      | +30（一次性事件） |
FRP 有上限（随当前转数提升而提高），达到上限后触发晋阶判定。

{
  "itemID": "guzhenren:huo_you_gu",
  "organScores": [
    {"id":"chestcavity:heat_resistance","value":"1.5"},
    {"id":"chestcavity:digestion","value": ".75"}
  ],  
  "defaultCompatibility": 0.85
}


# 单窍·火炭蛊 — 创意设计

简短概念
单窍火炭蛊是一种集中「燃烧·引爆·残火」三合一的小型器官。它把火属性浓缩成“炭心”，能够在命中、被击或环境触发时点燃目标并留下可被连锁的余烬效果。机制强调短时爆发、残留可交互（比如与油、火衣等反应）以及“造势型”玩法：把它当作引信来编排连招，而不是只做纯粹的持续伤害。

---

## 核心机制（通用）

1. 装备时提供微量火抗穿透与少量火系伤害加成（被动）。
2. 命中敌人会叠加**炭焰标记**（`CharcoalMark`），每层持续短时间并在层数触达阈值时引发一次**炭心爆裂**（短半径爆炸、造成火系瞬伤 + 点燃）。
3. 被点燃的实体会携带一个可交互的状态（例如 `ReactionStatuses.OIL_COATING`、`火衣`）——若与这些状态满足特定组合，则触发额外反应（如爆炸、持续高伤害或移动控制）。
4. 在胸腔内存在“火心蛊/烈源/其他火系器官”时提供共鸣加成（爆裂半径、标记上限或触发概率提升）。
5. 为防止连锁滥触，爆裂后会对同一目标产生连锁爆炸，直到没有任何 火衣 OIL_COATING等 炎道状态

---

## 数值与玩法细分（按转数）

### 一转 — 炭心雏形

* 被动：+2 火系伤害加成（乘算到器官体系），+1 真元/秒（小）。
* 命中：对敌方施加 `CharcoalMark`（持续4s，叠加上限2层）。当叠到2层时立刻触发**小型炭心爆裂**：半径1.5格，造成 6—10 火瞬伤 + 3s 点燃（每秒 2 火伤）。触发后该目标获得 6s 的防爆屏蔽。
* 主动（可选，冷却20s）：消耗 150 真元，立即引爆携带 `CharcoalMark` 的敌人并在自身周围制造燃烧圈（持续3s，每0.5s伤害并推动敌人短向外）。

### 二转 — 炭炉·恒燃

* 被动升级：`CharcoalMark` 持续延长至6s，上限3层；小型爆裂伤害提升（10—16），点燃改为 4s（每秒3火伤）。
* 新机制：命中时有 20% 概率在命中点残留一块**余烬**（持续6s），任何踩到余烬的敌人会被标记并被点燃。
* 被动交互：若目标同时带有 `ReactionStatuses.OIL_COATING`，爆裂变为**油炭爆**：半径+0.5格，爆裂伤害额外+30%，且爆裂后移除油涂层并对周围目标造成短暂眩晕（0.5s）。（触发后目标短暂无油涂层，防止重复连锁）
* 主动：消耗 250 真元，制造一个持续2s的大爆裂点（半径3格），造成 24—40 火伤并施加2s 燃烧与0.8s 眩晕。冷却35s。

### 三转 — 炭炉巅峰（战术/控场）

* 被动升级：`CharcoalMark` 上限4层；每层提升最终爆裂暴击率小幅（例如每层+4% 爆伤概率）并增加穿甲效果（对护甲/护盾伤害提高）。
* 余烬系统强化：余烬会「扩散」——在余烬存续期内每秒有低概率向外扩散额外余烬，扩大控场区域。
* 新增蓄能：当你对同一目标连续造成 `CharcoalMark` 爆裂（3次不同爆裂）时，会积攒一层**焦炭印记**（最多2层），使用主动技会消耗层数并将爆裂的伤害/半径乘以1.25/1.15。
* 主动：消耗 400 真元 + 5 精力，投掷一枚“燃炭弹”在目标处停留3s，期间每0.6s爆发小范围火焰（每次造成 8—12 火伤），若目标被点燃则爆燃（造成额外一次大爆裂）。冷却45s。

### 四转 — 烈源·炭魂（终极设定）

* 被动：每次爆裂都有一定几率留下“炭魂”——持续存在并在你下次爆裂时附加爆裂伤害（像弹药一样叠加，最多3层）。炭魂会根据你胸腔内其他火系器官提供额外加成（例如火心蛊提高炭魂伤害系数）。
* 交互强化：与“火衣”配合 → 爆裂不再触发短时免疫，而是转为**连锁震荡**（半径扩散 +1格，但每连锁目标伤害依次递减，防止无限扩散）。与“油涂层”配合 → 产生一次致命“油炭核爆”，移除所有油涂层并在大半径内造成高比例燃烧伤害（保留防链机制：对同一区域每60s限触发一次）。
* 主动（终极）：消耗 800 真元 + 12 精力，立即消耗所有炭魂（若有）并在自身位置释放一次范围极大、分段爆炸的炭心风暴（分3次爆发，每次间隔0.4s），总伤害极高并附带大范围点燃与击退。冷却120s。


* **资源桥接**：主动技能消耗`真元`（GuzhenrenResourceBridge）与精力（若项目有精力系统），使用 `ResourceHandle#consumeScaledZhenyuan()` 风格的调用。
* **Linkage 通道**：建议创建 `linkage/channel("huotan:residue")` 存储余烬计数与炭魂层数，便于器官间共享（例如与火心蛊共鸣读取该通道值）。
* **状态互操作**：使用已有的 `ReactionStatuses.OIL_COATING` 与火衣判定路径来编写特殊反应（并实现短时免疫标记避免连锁滥用）。
* **事件钩子**：用 `OrganOnHitListener` 处理命中叠层；`OrganSlowTickListener` 处理余烬扩散与炭魂成长；`OrganOnExplodeListener` 管理爆裂结果与交互效果。

---

## 视觉与特效（建议）

* 命中叠层时短暂生成暗红→橙黄色渐变的“炭粉云”，被标记目标胸口出现小型燃炭符印。
* 余烬残留表现为地面上暗红拱形碎片，触碰时暴发小火花并拉伸燃烧特效。
* 高转数爆裂可加入烟雾残留、黑灰飘散以及微量摄像机震动（客户端表现，避免影响服务器）。

---

## 平衡要点（设计注意）

1. **防止链式滥用**：每次爆裂后对同一区域/目标短时设免疫或转换成低伤害连锁，避免创造无限连锁。
2. **交互代价**：与油或火衣造成的大范围爆炸必须有资源/冷却代价（真元、高冷却、消耗油涂层），避免零代价全图爆炸。
3. **可玩性**：二转到三转应当把控场与单体爆发两条路线都保持竞争力；四转给出明确的“全部燃尽”终极技，但高真元与长冷却限制使用频率。

**小光蛊**做成可直接落地的数据与机制方案：有清晰的 tier（转数）、点数升级路线、以及完全对齐现有 Organscores 的 JSON。

---

# 小光蛊 · 幻光系（眼类器官）

**定位**：灵巧与欺敌。利用光折射制造幻象、误导仇恨，PVP 友好。
**物品本地化**：`"item.guzhenren.xiaoguanggu": "小光蛊"`

## 机制总览（随转数进阶）

* **被动·折影**：闪避成功时在原位生成“残影”标记（`reaction/mirror_image`），3秒内被远程锁定概率 -15%（PVP/PVE均有效，服务端命中判定偏置）。
总体思路
当玩家触发“折影”（闪避）后，给其添加一个3 秒的模糊态（Blur）标记。期间：
AI 选敌降权：远程单位在将其设为目标时，有 15% 概率放弃锁定。
弹道脱靶：投射物命中该玩家的那一刻，15% 概率改判为未命中（取消/偏转）。
兜底减伤：若仍进入结算，按投射物伤害类型 15% 概率取消伤害（Config可选开关，避免太强）。
注意：Minecraft 没有“官方锁定”概念，我们通过选敌/弹道/伤害三个切面叠加，得到接近“锁定概率下降”的体感。

* **主动技·幻映术**：投射“分身幻影”（实时复制玩家皮肤并附带淡蓝发光），**2秒自主演出 AI 后自动消散**；可被攻击，受击即提前消散（仅特效，不反击，不掉落；PVP可用）。
* **三转解锁**：分身在存活期间可**模仿一次轻攻击**（伤害系数 30%）。受击或消散时触发小型「幻光爆裂」（`reaction/illusion_burst`）。
* **四转解锁**：**光遁步**：在闪避后 0.5 秒内再次闪避→向前位移 5 格，获得 1 秒无敌与隐身（CD 10s）。与风/雷器官有共鸣增益。

> 默认主动技参数：CD 18s；消耗：`200 真元 + 5 精力`。
> 共鸣增强（已有ChestCavityForge/src/main/java/net/tigereye/chestcavity/compat/guzhenren/util/GuzhenrenFlowTooltipResolver.java辅助实现）：
>
> * 与**雷道蛊虫**：分身消散时附带 1 次短链雷击。
> * 与**风道蛊虫**：光遁步距离 +2 格、隐身 +0.5 秒。

---

## 点数升级路线（数据驱动 + 轻实现）

* **点数名**：`光灵点（LuminaPoints）`（建议以玩家 NBT / scoreboard 计数，键名 `guzhenren:lumina_points`）。
* **获取示例**（任选实现）：白昼击杀+1；分身被敌方命中+1（冷却 5s）；成功触发光遁步+1（冷却 30s）。
* **消耗规则**：

  * 二转 → 三转：消耗 **40 光灵点**；要求：`guzhenren:hunpo_stability ≥ 0.3`、`guzhenren:max_zhenyuan ≥ 0.5`
  * 三转 → 四转：消耗 **120 光灵点**；要求：`hunpo_stability ≥ 0.6`、`max_zhenyuan ≥ 1.0`

到达点数自动升级并且清空点数

---

## 器官 Organscores（与现有格式完全一致）

> 放置路径（建议）：
> `src/main/resources/data/chestcavity/organs/guzhenren/human/guang_dao/`
`xiaoguanggu.json`

```json
{
  "itemID": "guzhenren:xiaoguanggu",
  "organScores": [
    { "id": "chestcavity:nerves",          "value": "0.40" },
    { "id": "chestcavity:speed",           "value": "0.15" },
    { "id": "chestcavity:luck",            "value": "0.02" },
    { "id": "chestcavity:metabolism",      "value": "-0.05" },
    { "id": "guzhenren:hunpo_stability",   "value": "0.20" },
    { "id": "guzhenren:max_jingli",        "value": "0.40" },
    { "id": "guzhenren:jingli",            "value": "0.20" },
    { "id": "guzhenren:max_zhenyuan",      "value": "0.20" },
    { "id": "guzhenren:zhenyuan",          "value": "0.20" }
  ]
}

> 说明：
>
> * 数值刻意沿用现有风格（小数、正负混合，含 `chestcavity:*` 与 `guzhenren:*`），避免引入新 Score Key。
> * `incompatibility` 与 `metabolism` 给出少量代价，平衡灵巧与续航。
> * 三/四转额外的机制（分身模拟攻击、光遁步）通过行为挂钩实现，不需要在 JSON 内声明。



---

## 行为落地要点（与现有架构对齐）

* **触发入口**：`compat/guzhenren/item/guang_dao/behavior/IllusionSkill`（主动技）
* **被动挂钩**：闪避事件监听（客户端判定 + 服务端校验）→ 残影标记 & 三/四转分支
* **分身实体**：`IllusionDecoy`（你前面已看过骨架），生命周期 40 tick；PVP 命中提前消散
* **光遁步**（四转）：条件触发 + 位移 + 无敌帧，CD 以玩家 NBT/冷却管理器驱动
* **资源消耗**：`GuzhenrenResourceBridge.open(player)` → `consumeScaledZhenyuan()` / 精力同步

--------------------------------------

月道
# 月光蛊（Moonlight）— 完整落地计划 "item.guzhenren.yue_guang_gu": "月光蛊",

## 1) 设计目标
* 低消耗: 每SlowTick只需消耗 baseCost=96 的真元
* 夜间 + 月相驱动的全局增益器官，定位为**耐久与位移的通用核心**。
* 特征：按月相浮动的属性强化 + 可再生“月辉护盾” + 4转解锁的“盈亏计数/涌潮”防爆。
* 升级：**单线 1→5 转**，**自动生效**，**不提供重置与分支**。

---

## 2) 玩家可感知的核心机制

### 2.1 触发与缩放

* **触发**：夜间，并且**头顶见天**（天空可见，环境亮度≤7）→ 全额生效；不见天 → 按室内比例生效。
* **PVP 缩放**：最终数值统一 × **0.75**。
* **默认室内比例**：**50%**（L1 升级后变为 60%，见 4.1）。

### 2.2 月相表（`level.getMoonPhase(): 0..7`）

| 相位    | 说明     |         最大生命上限 | 月辉护盾（上限） | 伤害减免（DR） |     移动速度 | 跳跃          |
| ----- | ------ | -------------: | -------: | -------: | -------: | ----------- |
| 0     | 满月（最强） |       **+30%** |    **8** |  **12%** | **+10%** | Jump **II** |
| 1 / 7 | 凸月     |           +22% |        6 |       9% |      +7% | Jump I      |
| 2 / 6 | 上/下弦   |           +14% |        4 |       6% |      +5% | Jump I      |
| 3 / 5 | 娥眉     |            +8% |        2 |       4% |      +3% | 轻度          |
| 4     | 朔月（最弱） | **-5%**（4转后取消） |        0 |       0% |       0% | 无           |

> 说明：最大生命为百分比；护盾为**可再生**吸收池；DR 为伤害乘法减免（吃完护盾后再结算）；速度为乘法；跳跃用 Jump 效果。

### 2.3 月辉护盾（Lunar Ward）

* **容量**：随月相（见表），**L1/L2 升级可 +2**（见 4.1）。
* **再生**：离战 **2s** 后，每 **0.5s** 恢复 **1 点**，直至上限。破盾后 **1.5s** 不再生。
* **结算顺序**：先扣护盾 → 再按 DR 百分比缩减 → 再伤害进生命。
* **可读通道**：`Linkage.channel("guzhenren:lunar_ward")`（当前值/上限/冷却）。

### 2.4 盈亏计数 & 涌潮（4转解锁）

* **叠层**：夜间每 **8s** +1，或玩家命中 +1；上限 **6 层**。
* **触发**：满层后**首次受击**触发“涌潮”：当次伤害**额外 -8%**，并**立刻回盾 +2**，随后**清空层数**，**8s** 内无法再叠满。
* UI 显示：月盘周围小刻度（0~6）。

### 2.5 5转强化（望极）

* **满月增幅**：该蛊**所有正向数值 ×1.30**（即在原 20% 的基础上，总体 30% 增幅）。
* **清辉涌动**（被动，冷却 **20s**）：夜间获得“可触发态”；**第一次承伤**时，先吃**临时护盾 +4**（先于本体护盾扣除），并对 4 格内敌人**减速 20% 持续 2s**。触发后进入冷却。

---

## 3) 数值计算顺序（稳定结算）

1. 按月相表得到基础值：`HP% / Ward / DR% / SPD% / Jump`。
2. 叠加**单线升级**（第 4 节）。
3. 乘**室内比例**（见天=1，室内=比例）与**PVP 系数**（0.75）。
4. DR 做软封顶：**≤18%**（与其它系统合并时仍不超）。
5. 4转若满层 → 本次受击生效“涌潮”；5转若可触发 → 先吃“清辉涌动”的临时护盾。

---

## 4) 单线路升级（1→5转，自动生效；无重置/无分支）

### 4.1 L1（1转）— 入月

* **室内比例 +10%**：默认 50% → **60%**。
* **护盾上限 +1**。
* **非朔月最低 Jump I**（若月相给更高，则取更高）。

### 4.2 L2（2转）— 盈辉

* **护盾上限再 +1**（累计 +2）。
* **护盾再生速度 +20%**（离战 2s 触发不变）。

### 4.3 L3（3转）— 常照

* **全系 DR +3%**（参与 18% 封顶）。
* **移动速度 +3%**（乘法，加到月相速度上）。

### 4.4 L4（4转）— 涨落

* **取消朔月惩罚**（不再 -5% 最大生命）。
* **解锁“盈亏计数/涌潮”**（见 2.4）。

### 4.5 L5（5转）— 望极

* **满月增幅 ×1.30**（对本器官所有正向数值乘法叠加）。
* **“清辉涌动”冷却 20s**（首次承伤吃临时护盾 +4，并群体减速 20%/2s）。

---

## 5) 系统对接与实现清单

### 5.1 事件挂点

* **慢速 Tick**（每 20 tick）：

  * 判定夜间/见天、当前月相、PVP、L 级别。
  * 计算最终 `HP%/Ward/DR/Speed/Jump`，写入属性与效果；同步护盾再生。
  * 4转：处理“盈亏计数”的自然叠层（每 8s +1）。
* **受击事件（LivingHurt / IncomingDamage）**：

  1. 若 5转“清辉涌动”为可触发态 → 先扣临时护盾 +4 → 标记冷却。
  2. 扣**月辉护盾**（从 `wardNow` 扣至 0）。
  3. 对剩余伤害乘 **DR 最终值**。
  4. 4转若处于满层未触发 → **本次受击**套用“涌潮”减伤与回盾并清空层数 + 设置 8s 锁定。
* **命中事件（OnHit）**：夜间命中 +1 层盈亏计数（受上限与锁定约束）。

### 5.2 属性与效果应用

* **最大生命/速度**：AttributeModifier（稳定 UUID）。
* **跳跃**：MobEffect（Jump），每 40～60 tick 刷一次以防闪烁。
* **DR**：在 `IncomingDamage` 阶段乘法缩放（避免与抗性药水叠爆）。
* **护盾**：本地 Capability / Linkage 存 `wardCap/wardNow/lastHitTs`，离战再生。

### 5.3 数据存储（仅必要键）

* **玩家端映射**：`LunarState`（当前月相、是否见天、室内比例、最终 DR/速度/Jump 等，用于 UI）。
* **器官 NBT**：`GZR_Lunar.tier`（0~5，仅反映**转数映射**；安装即≥1）。
* **计数状态**：`lunar_tide_stacks`、`tide_lockout_until`、`surge_cd_until`、`temp_ward_until`（5转）。

### 5.4 客户端与 UI

* **月盘挂件**：显示月相、护盾环（当前/上限）、盈亏刻度（0~6）与 5转被动是否就绪。
* **器官条目**：只读展示“L1~L5 单线进度”，无按钮、无重置。
* **战斗提示**：护盾破裂音效 + 5转触发时的短促音与银辉脉冲粒子。

---

## 6) 配置开关（可选，默认值）

* `pvpMultiplier = 0.75`（统一缩放）
* `indoorsBaseMultiplier = 0.50`（L1 生效后 0.60）
* `drSoftCap = 0.18`
* `wardRegenTick = 0.5s`，`wardRegenDelay = 2s`，`wardBreakNoRegen = 1.5s`
* `tideMaxStacks = 6`，`tideGainSec = 8`，`tideLockout = 8s`
* `surgeExtraWard = 4`，`surgeCd = 20s`（仅 5转）

> 若你希望**极简**，以上配置也可全部**写死**在常量中。

---

## 7) 验证用例（快速自测）

1. **相位/室内**：白天无效 → 夜间见天全额 → 洞穴 60%（L1 后）。
2. **护盾流**：离战再生速率与上限随 L1/L2 变化；破盾 1.5s 验证。
3. **DR 封顶**：L3 后在满月叠一切，观察 ≤18%。
4. **计数/涌潮**：4转后叠到 6 层，首次受击触发减伤与回盾，锁定 8s。
5. **5转被动**：首次承伤先吃 +4 临时护盾并群体减速；20s 冷却自复位。
6. **PVP**：切 PVP 服，观察统一 ×0.75 缩放后仍稳定。

---

## 8) 兼容与风控

* **与雷盾/光系**：雷盾结算在**月辉护盾之后**；光系跳跃效果不覆盖本蛊的最低档。
* **速度上限**：建议全局加速软上限 **+30%**，超过部分由其它系统消化，避免窜速。
* **药水/装备**：DR 不与“抗性药水”叠爆（采用**乘法**与**封顶**）；Jump 以更高者为准。
* **存档安全**：移除器官时即时移除所有修饰与护盾，计数清零，避免残留。

---

## 9) 开发步骤（建议顺序，半天内可跑通雏形）

1. **定义常量**：月相表、PVP/室内系数、封顶等。
2. **护盾 Capability/Linkage**：读写、再生、受击消耗。
3. **SlowTick 合成器**：按“计算顺序”写入属性/效果与 UI 状态。
4. **受击/命中监听**：护盾→DR→涌潮/清辉涌动；命中叠层。
5. **UI 包与粒子/音效**：月盘、护盾环、触发特效。
6. **QA 自测**：按第 7 节流程打点看数值。
7. **平衡微调**：根据服内反馈仅改常量，不动流程。

"organScores": {
  "chestcavity:luck": 0.5,
  "chestcavity:breath": 0.2
}

并且同步更新  ChestCavityForge/src/main/resources/assets/guzhenren/docs/human/yue_dao 的文档
--------------------------------------

baseCost / (2^(jieduan + zhuanshu*4) * zhuanshu * 3 / 96

[复制粘贴用 - 更新文档]
整理 ChestCavityForge/src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/guang_dao  的主动/被动等逻辑，写入文档json中 ChestCavityForge/src/main/resources/data/guzhenren/docs/human/guang_dao , 要求，以系统语气撰写以下器官说明：
1. 用简洁、正式的语气描述其作用（如游戏系统提示）。
2. 若有数值，明确标出增益/减益效果与冷却。
3. 避免花哨形容词，强调“功能”、“触发条件”、“持续时间”。
4. 每条说明 1~3 句，读感类似“系统面板条目”。
5. 若存在主动技能，请用[主动]；被动效果用[被动]开头。无需编译验证，只需检测json格式是否正确即可


