# Agent Kickstart

**状态**: ChestCavityForge 目标 NeoForge 1.21.1；当前重点保持古真人器官逻辑、灵魂系统和联动账本在迁移后的一致性。

---

## 必须了解

- 你拥有完整的文件系统访问权限。在 `/home/kiz/Code/java/ChestCavityForge` 内工作；在完成前始终运行 `./gradlew compileJava`，在重要更改后运行 `./gradlew test`。
- 上游：`origin` = 个人分支，`upstream` = BoonelDanForever/ChestCavityForge。
- NeoForge 事件：mod 总线监听器通过 mod 构造函数注册（无 `FMLJavaModLoadingContext`）。
- 使用 `rg`、`git diff`、`git status -sb` 进行导航；除非明确请求，避免破坏性命令。

## 沟通规范（重要）
- 与用户沟通一律使用中文（含总结/说明/答复）。
- 仅在思考、代码实现与代码注释需要时，可使用英文；但对用户可见的交互文案仍保持中文。
- 输出保持简洁、明确、可执行；必要时给出下一步建议或验证方式。

## 工作流程提醒
- 到达时完整阅读此文件，保持笔记简短但精确。
- 如果任务需要多个直接操作，打开计划（`update_plan` 工具）并保持更新。
- 优先使用现有助手（`LedgerOps`、`MultiCooldown`、`NBTCharge`、`OrganStateOps`、`ResourceOps`）而非临时代码。
- 如果决策或待办事项影响未来工作，在此记录；否则保持低噪音。

---

## 核心系统规范

### DoT Reaction 规范（必须遵守）
- 所有 DoT 必须携带 `typeId`。仅使用带 `typeId` 的 `DoTManager.schedulePerSecond(...)` 重载，`typeId==null` 将抛异常。
- 统一在 `DoTTypes` 中声明/复用类型标识（例：`YAN_DAO_HUO_YI_AURA`、`HUN_DAO_SOUL_FLAME`、`SHUANG_XI_FROSTBITE`、`YIN_YUN_CORROSION`）。
- 反应系统：`util/reaction/ReactionRegistry` + `ReactionStatuses`。默认规则为火衣光环 + 油涂层 => 爆炸并移除油；当前不设置连锁屏蔽（窗口=0）。
- 在 DoT 伤害执行前会调用 `ReactionRegistry.preApplyDoT(...)`，可取消当次伤害；新增反应用 `ReactionRegistry.register(...)`。

### Attack Ability 注册规范（统一做法）
- 注册模式（强制）：
  - 行为类使用 `enum` 单例（例如 `JianYingGuOrganBehavior`、`LiandaoGuOrganBehavior`），在 `static { ... }` 中调用 `OrganActivationListeners.register(ABILITY_ID, <Behavior>::activateAbility)` 完成注册。
  - 禁止在构造函数里做注册或重逻辑，避免客户端早期类加载触发崩溃。
- 客户端热键列表：
  - 在各 `*ClientAbilities.onClientSetup(FMLClientSetupEvent)` 中仅以字面 `ResourceLocation` 加入 `CCKeybindings.ATTACK_ABILITY_LIST`；
  - 不要引用行为类常量（避免 classloading）；跳过占位 `chestcavity:attack_abilities`。
- 服务端激活链路：
  - 网络包 `ChestCavityHotkeyPayload.handle` 调用 `OrganActivationListeners.activate(id, cc)`；代码保持静音（INFO 以下）。
  - `OrganActivationListeners` 可保留"懒注册"兜底（按需加载行为类并重试），但默认静默失败，不打日志。

### Guzhenren Ops 迁移（四步）
1. **盘点**：用 `rg` 搜索 `LinkageManager.getContext|getOrCreateChannel|GuzhenrenResourceBridge.open|NBTCharge`，登记仍未走 `LedgerOps/ResourceOps/MultiCooldown/AbsorptionHelper` 的行为类。
2. **迁移**：按家族（如 炎/力/水）分批替换至对应 Ops，删除重复的钳制/计时/属性清理代码。仅只读查询可暂保留低层 API。
3. **一致性**：确认无直接 `LinkageChannel.adjust`/`ledger.remove` 遗留，冷却集中在 `MultiCooldown`，护盾统一 `AbsorptionHelper`，资源统一 `ResourceOps`。
4. **验证**：`./gradlew compileJava`，进游戏做装备/卸下/触发/护盾刷新实测；若发现 Ledger 重建或负冷却日志，回归对应行为修正并记录到本文件。

### 常用助手
- `GuzhenrenResourceBridge` 用于真元/精力/魂魄读取和调整。
- `LedgerOps.adjust/set/remove` 用于通道 + 账本更新。
- `SteelBoneComboHelper.ensureAbsorptionCapacity` 恢复 `MAX_ABSORPTION`。
- `SoulFakePlayerSpawner` + `SoulBeastAPI` 用于灵魂/灵魂兽转换。

---

## 灵魂系统 (Soul & Soulbeast)

### 核心组件
- **灵魂 API** (`SoulBeastAPI`, `SoulBeastStateManager`)：
  - `toSoulBeast(entity, permanent, source)` / `clearSoulBeast` / `isSoulBeast`。
  - 门限 + 永久标志决定活跃状态；状态同步集中处理。
  - 运行时回调（命中、tick）必须早期返回如果实体不是活跃灵魂兽。
- **灵魂假人** (`SoulFakePlayerSpawner`)：
  - 强制执行所有权；在生成或切换时使用提供的命令/util API。
  - 通过附件快照进行状态持久化；记得在突变后脏化所有者的附件。

### Soul Brain（统一"行动大脑"）
- **目标**：引入统一控制器（BrainController）作为"行动大脑"，在每个 SoulPlayer 上根据模式选择子大脑并驱动 Actions。
- **结构**：
  - `soul/fakeplayer/brain/BrainController.java` — 统一大脑，`SoulRuntimeHandler` 实现，`onTickEnd` 调度子大脑。
  - `soul/fakeplayer/brain/Brain.java` — 子大脑接口。
  - `soul/fakeplayer/brain/BrainMode.java` — 模式：`AUTO/COMBAT/SURVIVAL/IDLE`（现实现 `AUTO/COMBAT/IDLE`）。
- **工作流**：
  - `AUTO`：根据 `SoulAIOrders`（`FORCE_FIGHT`/`GUARD`）切入 `COMBAT`，否则 `IDLE`。
  - `COMBAT`：确保 `action/force_fight` 启动；若血量低，确保 `action/heal` 并发运行。
  - `IDLE`：空实现（占位）。

### FakePlayer Actions
- 新建集中目录：`ChestCavityForge/src/main/java/net/tigereye/chestcavity/soul/fakeplayer/actions`。
- 用于承载 Soul 假人可执行的动作实现（战斗/交互/姿态等），与 `SoulFakePlayerSpawner` 同模块，默认服务端执行。
- 命名规范建议以 `Action` 结尾；需要冷却/延时请复用 `MultiCooldown` 与 `DelayedTaskScheduler`；网络提示复用 `NetworkHandler` 现有载荷或按需新增。
- 已迁移（Actions API）：`GuardAction`、`ForceFightAction`、`HealingAction`（并发允许，周期尝试使用治疗物品）。

---

## 当前重点任务

### 待验证项目
- **TODO**: 验证最近迁移的吸收提供者（冰肌蛊、全勇明蛊、土墙蛊）与游戏内堆叠/移除测试，并确认 AbsorptionHelper 修饰符保持清洁。
- **TODO**: 将古真人行为逐步迁移到 `util.behavior` Ops（LedgerOps/ResourceOps/AbsorptionHelper/MultiCooldown）。从雷/食/丑/酒系开始，其中 LinkageManager/charge 计时器仍是手动的，并在每个桶转换时记录进度。

### 迁移步骤
1. **库存盘点**：列出每个仍接触 `LinkageManager`、原始 `GuzhenrenResourceBridge` 或特定冷却/吸收代码的行为；在跟踪表中捕获目标助手（LedgerOps / ResourceOps / MultiCooldown / AbsorptionHelper）。
2. **迁移通道**：一次重构一个桶（雷/食/丑/酒优先）以调用映射的助手，删除重复的钳制、账本重建和属性逻辑；保持每个器官家族的差异集中。
3. **一致性通道**：在每个桶之后，重新运行账本/冷却健全性检查（无手动 `LinkageChannel.adjust` 遗留，计时器集中）并更新跟踪表 + `AGENTS.md` 以反映完成。
4. **验证通道**：执行 `./gradlew compileJava` 加上针对迁移器官的定向手动测试（装备/卸下、能力触发、护盾刷新）；如果出现回归，在跟踪表旁边记录重现 + 修复计划。

### 注意事项
- 装备/卸下不应留下账本残留或负冷却。
- 计时器状态应在物品移除（通过重建）和区块重载后存活。
- 日志噪音：保持调试切换（例如 `DEBUG_ABSORPTION`）默认关闭，除非主动诊断。

---

## 数据驱动架构

### 器官数据定义
ChestCavityForge 使用数据驱动的方式定义古真人器官，所有器官配置位于资源目录中：

#### 人类器官 (`data/chestcavity/organs/guzhenren/human/`)
- **路径**: `src/main/resources/data/chestcavity/organs/guzhenren/human/`
- **结构**: 按流派组织（冰雪道、力道、魂道等）
- **内容**: 定义人类可用的古真人器官及其属性
- **示例文件**: `bing_ji_gu.json`（冰肌蛊）
  ```json
  {
    "itemID": "guzhenren:bing_ji_gu",
    "organScores": {
      "chestcavity:defense": 1.0,
      "chestcavity:health": 1.0,
      "chestcavity:nerves": 1.0,
      "chestcavity:strength": 1.0,
      "chestcavity:speed": 1.0
    },
    "defaultCompatibility": {
      "chestcavity:human": true,
      "chestcavity:golem": false
    }
  }
  ```

#### 动物器官 (`data/chestcavity/organs/guzhenren/animal/`)
- **路径**: `src/main/resources/data/chestcavity/organs/guzhenren/animal/`
- **结构**: 按器官类型组织（骨骼、眼睛、肌肉、皮肤、羊角、虎牙等）
- **内容**: 定义动物相关的古真人器官
- **示例文件**: `quanyan_eye.json`（犬眼）
  ```json
  {
    "itemID": "guzhenren:quanyan_eye",
    "organScores": {
      "chestcavity:sight": 1.0,
      "chestcavity:nerves": 1.0,
      "chestcavity:defense": 1.0,
      "chestcavity:health": 1.0
    },
    "defaultCompatibility": {
      "chestcavity:human": true,
      "chestcavity:golem": false,
      "chestcavity:animal": true
    }
  }
  ```

### 兼容性类型定义 (`data/chestcavity/types/compatibility/guzhenren/`)
- **路径**: `src/main/resources/data/chestcavity/types/compatibility/guzhenren/`
- **结构**: 按实体类型组织（蚂蚁、熊、标、修炼者、猎犬、僵尸、蟒蛇、羊、蛇、虎、狼等）
- **内容**: 定义不同实体类型的胸腔兼容性
- **示例文件**: `nan_gu_xiu.json`（男古修）
  ```json
  {
    "order": 0,
    "name": "nan_gu_xiu",
    "types": ["chestcavity:human"],
    "incompatibleTypes": ["chestcavity:animal"],
    "maxDifference": 1,
    "mustBeFullSet": false,
    "boneOrgan": true,
    "muscleOrgan": true,
    "fleshOrgan": true,
    "skinOrgan": true,
    "nerveOrgan": true,
    "sightOrgan": true,
    "breathOrgan": true,
    "thoughtOrgan": true,
    "rottenOrgan": false,
    "carnivorous": false,
    "herbivorous": false
  }
  ```

### 实体分配 (`data/chestcavity/entity_assignment/guzhenren/`)
- **路径**: `src/main/resources/data/chestcavity/entity_assignment/guzhenren/`
- **结构**: 与兼容性类型目录相同的组织结构
- **内容**: 将古真人实体类型映射到胸腔类型
- **示例文件**: `nan_gu_xiu.json`
  ```json
  {
    "entity_type": "guzhenren:nan_gu_xiu",
    "chestcavity_type": {
      "type": "chestcavity:guzhenren/nan_gu_xiu",
      "source": "chestcavity:types/compatibility/guzhenren/cultivators/nan_gu_xiu.json"
    }
  }
  ```

### BingXue 道（冰雪系）小结（2025-10-18）
- 霜息蛊（ShuangXiGu）
  - 命中挂 `reaction/frost_mark`（已接入）并在命中点生成小型霜雾残留（ReactionEngine.queueFrostResidue）。
  - 轻量粒子与命中音效保持克制，不刷屏。
- 冰肌蛊（BingJiGu）
  - `applyColdEffect` 统一挂 `reaction/frost_mark`，从而冰爆/普攻路径也能触发“蒸汽灼烫/霜痕碎裂”。
  - 冰爆后在爆心留下短时霜雾残留，并向玩家提示“冰爆余寒，地面凝霜”。
- 清热蛊（QingReGu）
  - 成功供能后短时 `reaction/frost_immune` 自护，清理 `fire_mark/fire_residue`；首次授予轻提示与雪花粒子。
- 冰布蛊（BingBuGu）
  - 成功给予饱和/再生后短时 `reaction/frost_immune`，清理 `fire_mark/fire_residue`；玩家显示轻提示与雪花粒子。

验证建议
- 霜息/冰肌命中与冰爆均可触发蒸汽/碎裂；
- 自护免疫仅首次授予提示，避免刷屏；
- 残留域通过 ReactionEngine 排队，具有限流与合并策略。

---

## 快速参考

### 项目结构
- **项目根目录**: `/home/kiz/Code/java/ChestCavityForge`
- **有用目录**:
  - Java 源文件: `src/main/java`
  - 资源/数据: `src/main/resources`
  - 测试: `src/test/java`
  - 文档/设计: `docs/`
- **数据目录**:
  - 器官定义: `src/main/resources/data/chestcavity/organs/guzhenren/`
  - 兼容性类型: `src/main/resources/data/chestcavity/types/compatibility/guzhenren/`
  - 实体分配: `src/main/resources/data/chestcavity/entity_assignment/guzhenren/`
- **构建/测试**:
  - `./gradlew compileJava`
  - `./gradlew test`
  - `./gradlew runClient` / `runServer` 用于手动检查（NeoForge 配置已设置为使用项目映射）。

### 常用工具
- `rg "<term>" src/main/java`
- `git status -sb`
- `git diff --stat`, `git diff <file>`

### 关键文件
- **古真人兼容模块**: `src/main/java/net/tigereye/chestcavity/compat/guzhenren/`
- **灵魂系统**: `src/main/java/net/tigereye/chestcavity/soul/`
- **工具类**: `src/main/java/net/tigereye/chestcavity/compat/guzhenren/util/behavior/`
- **数据定义**: `src/main/resources/data/chestcavity/`

---

## 近期修复亮点（保持上下文简短）

- 将多个器官计时器合并到 `MultiCooldown`（冰肌蛊、力影家族、龙丸蛐蛐蛊、火衣蛊、体魄蛊、自力更生蛊）以防止负时间戳并简化同步。
- 体魄蛊现在注册为增加贡献者，使用 `LedgerOps.set/remove`，并支持账本重建，因此 `hun_dao_increase_effect` 在装备时保持在 +0.10 并在移除时重置。
- 自力更生蛊在激活时授予 30 秒再生，在完成/移除时移除效果，并继续应用预期的后 buff 虚弱。
- 钢骨组合助手确保 `MAX_ABSORPTION` 属性已注册，并且护盾刷新计时器尊重全局上限。
- 账本重建工具（`IncreaseEffectLedger`）在计数漂移时警告；如果联动总数意外重置，搜索警告。

---

**最后更新**: 2025-10-18  
**维护者**: ChestCavityForge 开发团队
