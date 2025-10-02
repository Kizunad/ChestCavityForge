# ChestCavity 器官分数效果一览

下表汇总了题目中列出的每个 `chestcavity` 器官分数在当前 ChestCavityForge 代码库中的实际作用或缺省状态。

## 呼吸与生命维持

- **`chestcavity:breath_capacity`**：调整在水下或缺氧环境中空气值衰减/恢复的速度，更高数值让肺一次损失的空气量更少。【F:src/main/java/net/tigereye/chestcavity/util/ChestCavityUtil.java†L79-L125】
- **`chestcavity:breath_recovery`**：在陆地补氧时减少空气损失，并在潮湿环境下配合鳃类（`water_breath`）提供额外恢复。【F:src/main/java/net/tigereye/chestcavity/util/ChestCavityUtil.java†L127-L156】
- **`chestcavity:water_breath`**：直接抵消或减缓水下窒息，同时在陆地恢复空气时提供额外补偿。【F:src/main/java/net/tigereye/chestcavity/util/ChestCavityUtil.java†L86-L154】
- **`chestcavity:health`**：提升或降低最大生命值；若总分为零且种族默认值不为零会触发心脏失血按周期造成伤害。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganUpdateListeners.java†L30-L44】【F:src/main/java/net/tigereye/chestcavity/listeners/OrganTickListeners.java†L122-L140】
- **`chestcavity:endurance`**：目前仅在数据中分配，除注册常量外没有运行时代码引用，尚无游戏内效果。【F:src/main/java/net/tigereye/chestcavity/registration/CCOrganScores.java†L8-L69】【5f69e5†L1-L64】
- **`chestcavity:nutrition`**：影响进食时获得的饱和度，多数情况下按原版值乘以分数/4 结算，负值会造成饥饿。【F:src/main/java/net/tigereye/chestcavity/util/ChestCavityUtil.java†L226-L239】
- **`chestcavity:digestion`**：影响获得的饥饿值，分数越高越容易吃饱；负值则造成反胃并清空饥饿收益。【F:src/main/java/net/tigereye/chestcavity/util/ChestCavityUtil.java†L188-L201】
- **`chestcavity:metabolism`**：调整饥饿倒计时，正值让饥饿条下降更慢，负值加快饥饿。【F:src/main/java/net/tigereye/chestcavity/util/ChestCavityUtil.java†L242-L270】

## 消化与食性

- **`chestcavity:carnivorous_digestion`**、**`chestcavity:carnivorous_nutrition`**：当食物带有肉类/肉食标签时，分别增加饥饿与饱和度修正。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganFoodListeners.java†L20-L33】
- **`chestcavity:herbivorous_digestion`**、**`chestcavity:herbivorous_nutrition`**：对非肉类食物生效，提供同类加成。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganFoodListeners.java†L20-L33】
- **`chestcavity:rot_digestion`**、**`chestcavity:rotgut`**：食用腐烂类食物时额外提高饥饿与饱和度收益。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganFoodListeners.java†L35-L44】
- **`chestcavity:furnace_powered`**：主动能力可消耗手持燃料赋予“熔炉之力”增益并叠加持续时间/等级；专用食物 `furnace_power` 会根据现有增益叠加营养值。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganActivationListeners.java†L117-L144】【F:src/main/java/net/tigereye/chestcavity/listeners/OrganFoodListeners.java†L35-L44】
- **`chestcavity:grazing`**：主动触发时可将脚下草方块/菌岩吃掉并施加“反刍”增益，持续时间与器官分数及既有增益累加相关。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganActivationListeners.java†L206-L234】
- **`chestcavity:crystalsynthesis`**：周期性绑定最近的末影水晶，维系光束时按分数回复饥饿、饱和或生命；若水晶被摧毁会反噬伤害。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganTickListeners.java†L36-L86】
- **`chestcavity:photosynthesis`**：在明亮环境下累积进度，满值后优先恢复饥饿、再补饱和，最终为玩家或生物回血。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganTickListeners.java†L88-L119】

## 防御与减伤

- **`chestcavity:defense`**：按差值成指数衰减接收到的非穿甲伤害，堆叠多根骨头可显著减伤。【F:src/main/java/net/tigereye/chestcavity/util/ChestCavityUtil.java†L74-L77】
- **`chestcavity:arrow_dodging`**：受投射物攻击时尝试瞬移离开并施加冷却，成功后伤害被完全免除。【F:src/main/java/net/tigereye/chestcavity/util/ChestCavityUtil.java†L311-L326】
- **`chestcavity:buff_purging`**：缩短正面状态效果的持续时间，用于对抗增益类药水。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganAddStatusEffectListeners.java†L21-L32】
- **`chestcavity:detoxification`**：当种族默认存在排毒需求时，按与默认值的比例缩短负面状态持续时间。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganAddStatusEffectListeners.java†L34-L45】
- **`chestcavity:filtration`**：缺少肾脏（分数<默认）会周期性中毒，正向差值则在中毒时缩短持续时间。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganTickListeners.java†L150-L171】【F:src/main/java/net/tigereye/chestcavity/listeners/OrganAddStatusEffectListeners.java†L48-L56】
- **`chestcavity:withered`**：被赋予凋零效果时缩短其持续时间。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganAddStatusEffectListeners.java†L62-L70】
- **`chestcavity:buoyant`**：在水中对玩家/生物施加向上推力，数值越高越容易上浮。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganTickListeners.java†L26-L34】
- **`chestcavity:fire_resistant`**：对火焰/灼烧伤害应用指数减免，数值越高减伤越显著。【F:src/main/java/net/tigereye/chestcavity/util/ChestCavityUtil.java†L206-L214】
- **`chestcavity:impact_resistant`**：减少坠落与撞墙伤害。【F:src/main/java/net/tigereye/chestcavity/util/ChestCavityUtil.java†L216-L224】
- **`chestcavity:leaping`**：提升起跳速度并在坠落时按平方量级抵扣摔落伤害。【F:src/main/java/net/tigereye/chestcavity/util/ChestCavityUtil.java†L220-L236】
- **`chestcavity:swim_speed`**：仅在水中开启胸腔时生效，按差值提高游泳速度。【F:src/main/java/net/tigereye/chestcavity/util/ChestCavityUtil.java†L301-L307】
- **`chestcavity:hydroallergenic`**：在雨水或水中造成魔法伤害并施加易水易伤效果，数值越高冷却越短。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganTickListeners.java†L178-L200】
- **`chestcavity:hydrophobia`**：在水或雨中随机瞬移离开，数值决定瞬移距离。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganTickListeners.java†L201-L212】
- **`chestcavity:ease_of_access`**：无视开胸器对敌人生命值的门槛要求，可直接打开胸腔。【F:src/main/java/net/tigereye/chestcavity/chestcavities/types/DefaultChestCavityType.java†L196-L201】

## 属性与战斗能力

- **`chestcavity:luck`**：为实体添加常驻幸运属性修正，按与默认差值线性换算。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganUpdateListeners.java†L22-L33】
- **`chestcavity:strength`**：按差值向攻击力添加乘性修正，提高输出伤害。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganUpdateListeners.java†L45-L55】
- **`chestcavity:speed`**：按差值向移动速度添加乘性修正，提高奔跑速度。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganUpdateListeners.java†L57-L66】
- **`chestcavity:nerves`**：若默认需要神经系统，缺失会禁止移动；额外分数提高攻击速度，同时用于缩短破块所需时间。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganUpdateListeners.java†L68-L89】【F:src/main/java/net/tigereye/chestcavity/util/ChestCavityUtil.java†L218-L242】
- **`chestcavity:launching`**：近战命中时向上推起目标，幅度受击退抗性及器官差值影响。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganOnHitListeners.java†L16-L22】
- **`chestcavity:knockback_resistant`**：为实体增加击退抗性属性值。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganUpdateListeners.java†L91-L105】
- **`chestcavity:attack_range`**：目前仅在 Guzhenren 数据 `guqianggu` 中赋值，主模组未注册该分数，暂无实际效果。【F:src/main/resources/data/chestcavity/organs/guzhenren/human/guqianggu.json†L1-L6】【F:src/main/java/net/tigereye/chestcavity/registration/CCOrganScores.java†L8-L69】

## 主动攻击与投射物

- **`chestcavity:creepy`**：触发自爆时若没有冷却便按 `explosive` 分数规模引爆并摧毁相关器官，爆炸后施加冷却。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganActivationListeners.java†L58-L70】
- **`chestcavity:explosive`**：被 `creepy` 消耗来决定爆炸强度；爆炸逻辑调用公共工具类生成实体伤害与效果云。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganActivationListeners.java†L65-L68】【F:src/main/java/net/tigereye/chestcavity/util/CommonOrganUtil.java†L42-L49】
- **`chestcavity:dragon_breath`**：喷吐龙息会消耗饱食度、施加冷却并排队生成龙息弹幕。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganActivationListeners.java†L73-L88】
- **`chestcavity:dragon_bombs`**：排队发射火球并根据数量造成饱食度消耗与冷却效果。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganActivationListeners.java†L91-L101】【F:src/main/java/net/tigereye/chestcavity/util/CommonOrganUtil.java†L93-L101】
- **`chestcavity:forceful_spit`**：按分数排队喷吐羊驼口水弹，附带饱食度消耗与冷却。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganActivationListeners.java†L104-L115】【F:src/main/java/net/tigereye/chestcavity/util/CommonOrganUtil.java†L103-L111】
- **`chestcavity:ghastly`**：触发时排队释放恶魂火球，附带饱食度消耗与冷却。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganActivationListeners.java†L193-L203】【F:src/main/java/net/tigereye/chestcavity/util/CommonOrganUtil.java†L113-L121】
- **`chestcavity:pyromancy`**：排队生成小型火球并施加冷却，数量由分数决定。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganActivationListeners.java†L237-L247】【F:src/main/java/net/tigereye/chestcavity/util/CommonOrganUtil.java†L123-L131】
- **`chestcavity:shulker_bullets`**：寻找附近目标并生成潜影弹，触发后进入冷却。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganActivationListeners.java†L250-L260】【F:src/main/java/net/tigereye/chestcavity/util/CommonOrganUtil.java†L133-L140】
- **`chestcavity:silk`**：在无冷却时可生成蛛网/羊毛或发射丝束，消耗饥饿值并进入冷却。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganActivationListeners.java†L263-L272】【F:src/main/java/net/tigereye/chestcavity/util/CommonOrganUtil.java†L316-L352】
- **`chestcavity:venomous`**：装备毒腺时近战命中可给目标附加自定义或默认的剧毒效果，同时给施法者套上冷却并消耗少量体力。【F:src/main/java/net/tigereye/chestcavity/items/VenomGland.java†L23-L64】

## 其他特殊效果

- **`chestcavity:buff_purging`**、**`detoxification`**、**`filtration`**、**`withered`**：见“防御与减伤”段落中的说明，这些能力都通过状态效果监听器即时调整药水持续时间。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganAddStatusEffectListeners.java†L21-L70】
- **`chestcavity:iron_repair`**：检测到持有铁质材料时可自我治疗并进入冷却，治疗量按最大生命值百分比计算。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganActivationListeners.java†L162-L189】
- **`chestcavity:glowing`**：持续让实体获得发光效果，以便被远距离看见。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganTickListeners.java†L232-L240】
- **`chestcavity:hydrophobia`**、**`hydroallergenic`**：参见“防御与减伤”段落——分别用于瞬移躲水与因水致伤。【F:src/main/java/net/tigereye/chestcavity/listeners/OrganTickListeners.java†L178-L212】

以上内容基于当前仓库中的 Java 逻辑与数据文件，若未来版本新增实现，请在对应分数的监听器或工具类中补充说明。
