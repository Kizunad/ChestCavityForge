# Agent Kickstart

**Current status**: ChestCavityForge is migrated to Minecraft 1.21.1 on NeoForge.

## Important memory
- Full system access is available; act responsibly and avoid unnecessary destructive commands.
- Any tool installation or environment-variable changes that could pollute the system must be expressed through Nix (e.g., via `flake.nix` devShell updates).
- Upstreams: `origin` = personal fork, `upstream` = BoonelDanForever/ChestCavityForge.
- In NeoForge 21.x the mod event bus is injected via the mod constructor (`public ChestCavity(IEventBus bus)`); `FMLJavaModLoadingContext` is gone.

## First actions when you arrive
- Run `ls -a` in `/home/kiz/Code/java` to confirm you're in the repo root and discover key files.
- Read this `AGENTS.md` top to bottom so you know the context and workflow.
- Enter the dev environment with `nix develop`; the prompt should switch to `[Modding]:`.
- Inspect the active NeoForge module(s) and verify `build.gradle`, `gradle.properties`, and `mods.toml` align with NeoForge 1.21.1.

## Working protocol
- Use the planning tool whenever the task needs more than one straightforward action; keep the plan updated as you progress.
- Keep changes scoped and well-justified. Add focused comments only when they clarify non-obvious logic.
- Prefer `rg` and other read-only commands to explore; avoid destructive operations unless explicitly required.
- Document any new decisions, assumptions, or TODOs back into this repo (update this file or add notes) so the next agent inherits the context.
- Before yielding or completing a task, run `./gradlew compileJava` to validate the current changeset.

### ModernUI HUD/Toast 统一样式（当前状态）
- 范围：仅实现 HUD 常驻与 Toast 提醒；ModernUI 界面内弹窗暂缓。
- 命令：
  - `/testmodernUI toast` 显示 PNG 图标的 Toast 提醒。
  - `/testmodernUI hui true|false` 开关 HUD 常驻卡片。
- 代码：
  - 样式工具：`ChestCavityForge/src/main/java/net/tigereye/chestcavity/client/ui/HudUiPaint.java`
    - `drawCard(...)` 统一阴影+底板配色；`drawIcon24(...)` 统一图标绘制；`TEXT_TITLE/TEXT_SUB` 文本色常量。
  - Toast：`client/ui/ReminderToast.java`（改用 HudUiPaint）。
  - HUD：`client/hud/TestHudOverlay.java`（改用 HudUiPaint）。
- 注意：圆角目前为占位（方形卡片），后续可用 9-slice/mesh 实现真圆角与阴影模糊。

### AttackAbility 冷却统计与 Toast 嵌入计划（分阶段）

目标
- 主动技能（AttackAbility）冷却结束时，客户端弹出“技能就绪”Toast（物品图标或 PNG）。

现状分类（按冷却存储与触发方式）
- 组A：倒计时型（EntryInt，显式 tickDown）
  - 代表：冰肌蛊 BingJiGu 的无敌冷却 `INVULN_COOLDOWN_KEY`（MultiCooldown.EntryInt）等。
  - 嵌入点：EntryInt.withOnChange(prev>0 && curr==0) 服务端发包。
  - 进度：已在 BingJiGu 接入（onChange→CooldownReadyToastPayload）。

- 组B：时间戳型（Entry/或自定义 NBT long，存“readyAt = gameTime + 冷却”）
  - 代表：
    - 火衣蛊 HuoYiGu：`cooldownUntilEntry`（MultiCooldown.Entry long）ACTIVE_COOLDOWN_TICKS=220。
    - 镰刀蛊 LiandaoGu：`LiandaoGuCooldown`（自管 NBT long），160–240t。
    - 剑影蛊 JianYingGu：内部 `COOLDOWN_HISTORY`（仅内存映射，不落 NBT）。
  - 嵌入点：时间自然流逝不会触发 onChange，需“到时回调”。
  - 计划：实现服务端轻量调度器 CooldownToastScheduler
    - API：`scheduleReadyToast(ServerPlayer, long readyTick, ItemStack|textureId, titleKey, sub)`
    - 机制：`ServerTickEvent.Post` 每秒扫描最近 64 条；到时发 `CooldownReadyToastPayload`，并从队列移除。
    - 接入：在设置 `setReadyAt(gameTime + cooldown)` 的同一处调用 `scheduleReadyToast(...)`。

- 组C：状态效果型（利用 CCStatusEffects 作为冷却）
  - 代表（OrganActivationListeners 内置）：
    - `ARROW_DODGE_COOLDOWN`, `DRAGON_BOMB_COOLDOWN`, `DRAGON_BREATH_COOLDOWN`, `EXPLOSION_COOLDOWN`,
      `FORCEFUL_SPIT_COOLDOWN`, `GHASTLY_COOLDOWN`, `IRON_REPAIR_COOLDOWN`, `PYROMANCY_COOLDOWN`,
      `SHULKER_BULLET_COOLDOWN`, `SILK_COOLDOWN` 等（对应 CCConfig 中秒表）。
  - 嵌入点（无网络改造版本）：客户端每 tick 观察本地玩家的效果有无→由“有→无”转变时弹 Toast（每个效果保持 1 个最近状态，避免抖动）。

数据与位置速记
- 客户端能力登记（热键触发名单）：
  - `compat/guzhenren/item/xue_dao/XueDaoClientAbilities.java`（血道：血肺蛊、血滴蛊）
  - `compat/guzhenren/item/mu_dao/MuDaoClientAbilities.java`（木道：镰刀蛊）
  - `compat/guzhenren/item/tu_dao/TuDaoClientAbilities.java`（土道：土墙蛊）
  - `compat/guzhenren/item/yan_dao/YanDaoClientAbilities.java`（炎道：火衣蛊）
- 代表性冷却键值：
  - 冰肌蛊：`INVULN_COOLDOWN_KEY`（EntryInt，已接入 Toast）
  - 火衣蛊：`cooldownUntilEntry`（Entry long）
  - 镰刀蛊：`LiandaoGuCooldown`（自定义 NBT long）
  - 剑影蛊：`CLONE_COOLDOWN_TICKS` + `COOLDOWN_HISTORY`（内存）
  - 血肺蛊：`STATE_KEY/COOLDOWN_KEY`（Cooldown long）

阶段性落地（建议顺序）
1) 完成组A（EntryInt）批量接入：
   - 模式：`entryInt(...).withOnChange((prev,curr) -> if(prev>0 && curr==0) sendCooldownToast)`
   - 图标：优先 `organ.getItem()`，副标题用 `organ.getHoverName()`。
2) 引入 CooldownToastScheduler（服务端）：
   - 在火衣蛊与镰刀蛊设置冷却的同一处 `scheduleReadyToast(player, readyTick, icon, titleKey, sub)`。
   - 剑影蛊可在成功激活时 `scheduleReadyToast(now + CLONE_COOLDOWN_TICKS, ...)`（从 `COOLDOWN_HISTORY` 推导）。
3) 组C（状态效果）客户端观察器：
   - 新建 `client/hud/CooldownEffectWatcher`：维护一组目标效果 `Holder<MobEffect>` 的上帧存在标记。
   - 在 `ClientTickEvent.Post` 检测“从有到无”，调用 `ReminderToast.show(...)`。

客户端展示协议（已实现）
- Payload：`network/packets/CooldownReadyToastPayload`
  - 服务器端构造：`useItemIcon` + `iconId`（物品或 PNG）+ `title` + `subtitle`
  - 客户端处理：优先用物品图标，其次用 PNG；调用 `ReminderToast`（统一 HUD 样式）。

验收清单
- 冰肌蛊：无敌冷却就绪 → 弹物品图标 Toast。
- 火衣蛊/镰刀蛊：冷却就绪（不提前/不重复）→ 弹 Toast。
- CC 基础能力（火球/龙息等）：效果消失即 Toast（不刷屏）。
- 文案：后续改为翻译键（title/subkey），当前可先用“技能就绪 + 器官名”。

### Guzhenren Ops 迁移（四步）
1) 盘点：用 `rg` 搜索 `LinkageManager.getContext|getOrCreateChannel|GuzhenrenResourceBridge.open|NBTCharge`，登记仍未走 `LedgerOps/ResourceOps/MultiCooldown/AbsorptionHelper` 的行为类。
2) 迁移：按家族（如 炎/力/水）分批替换至对应 Ops，删除重复的钳制/计时/属性清理代码。仅只读查询可暂保留低层 API。
3) 一致性：确认无直接 `LinkageChannel.adjust`/`ledger.remove` 遗留，冷却集中在 `MultiCooldown`，护盾统一 `AbsorptionHelper`，资源统一 `ResourceOps`。
4) 验证：`./gradlew compileJava`，进游戏做装备/卸下/触发/护盾刷新实测；若发现 Ledger 重建或负冷却日志，回归对应行为修正并记录到本文件。

### Attack Ability 注册规范（统一做法）
- 注册模式（强制）：
  - 行为类使用 `enum` 单例（例如 `JianYingGuOrganBehavior`、`LiandaoGuOrganBehavior`），在 `static { ... }` 中调用
    `OrganActivationListeners.register(ABILITY_ID, <Behavior>::activateAbility)` 完成注册。
  - 禁止在构造函数里做注册或重逻辑，避免客户端早期类加载触发崩溃。
- 客户端热键列表：
  - 在各 `*ClientAbilities.onClientSetup(FMLClientSetupEvent)` 中仅以字面 `ResourceLocation` 加入 `CCKeybindings.ATTACK_ABILITY_LIST`；
  - 不要引用行为类常量（避免 classloading）；跳过占位 `chestcavity:attack_abilities`。
- 服务端激活链路：
  - 网络包 `ChestCavityHotkeyPayload.handle` 调用 `OrganActivationListeners.activate(id, cc)`；代码保持静音（INFO 以下）。
  - `OrganActivationListeners` 可保留“懒注册”兜底（按需加载行为类并重试），但默认静默失败，不打日志。
- Soul 攻击处理器：
  - `GuzhenrenAttackAbilityHandler.ABILITY_IDS` 使用字符串 `ResourceLocation` 列出可尝试的主动技 id，避免静态引用行为类触发不安全的 `<clinit>`。
- 日志约定：
  - 激活早退/成功默认使用 `DEBUG`，不使用 `INFO`。
  - 若需临时诊断，可临时提升单类日志级别，修复后恢复静音。

## Web Codex 快速上手（Guzhenren 新器官）

- 进度跟踪：`docs/guzhenren_behavior_migration.md` 维护蛊真人行为迁移表。本次合并（codex/migrate-guzhenren-behaviours-to-util.behavior）已将雷道/食道/臭道（DianLiugu/JiuChong/ChouPiGu）迁移到 `util.behavior` 工具链：
  - 使用 `LedgerOps.ensureChannel/adjust` 统一通道与 Ledger 校验
  - 使用 `MultiCooldown` 替换 `NBTCharge`/散落 Map 计时
  - 按槽位/主器官抽象整理状态读写（必要时结合 `OrganStateOps`）

 余下候选（建议后续批量迁移或确认维持现状）
  - 血道：XieFeigu 仅读取 `xue_dao_increase_effect`（不改写），目前保留 `LinkageManager.lookupChannel` 可接受；其余血道器官（Xiedigu/Tiexuegu）已使用 `LedgerOps` 管理增效，继续关注卸下残留日志。
  - 炎道：HuoYiGu 多处引用 `GuzhenrenResourceCostHelper`，建议统一走 `ResourceOps.consumeStrict/WithFallback`，并确认冷却是否已集中到 `MultiCooldown`。
  - 力道：ZiLiGengShengGu 已接入 `MultiCooldown` 与 `ResourceOps`；`AbstractLiDaoOrganBehavior`/`LiDaoHelper` 中对 `LinkageManager` 的只读路径可保留，若新增通道写入请改走 `LedgerOps`。
  - 水道：LingXiangu/Jiezegu/ShuishenguShield 已部分迁移至 `ResourceOps`/`LedgerOps`（见 57d67f8：退款/扣血改走 `ResourceOps.refund/drainHealth`，充能显示通道改 `LedgerOps.ensureChannel`）。剩余仅保留护盾核心在 `ShuishenguShield`。
  - 冰雪道：BingJiGu 已接 `AbsorptionHelper`，ShuangXiGu 使用 `LedgerOps`；检查 `QingReGu` 是否仍直接建通道，必要时改为 `LedgerOps.ensureChannel`。
  - 空窍：DaoHenSeedHandler 基于种子播种通道，维持低层操作可接受；若仅需“确保存在”，可替换为 `LedgerOps.ensureChannel`。
  - 模块：`GuzhenrenOrganHandlers`、`GuzhenrenOrganScoreEffects` 侧重全局挂载与资源桥分发，保持当前结构。

- **需求拆解**：收到用户类似 `水体蛊` / `血气蛊` 的 JSON 模板时，先把要做的事情分成数据注册（`organScores` 等静态内容）与行为实现（OnEquip/SlowTick/资源消耗）。模板里的 `itemID`、`organScores` 直接映射到数据驱动文件，行为描述留给 Java 侧的监听器处理。
- **数据注册路径**：在 `ChestCavityForge/src/main/resources/data/chestcavity/organs/guzhenren/` 下新增或调整对应 `*.json`（人类器官通常位于 `.../human/` 子目录）。对照现有文件（例如 `guzhenren:gucaikongqiao`）复制 `itemID`、`organScores`、`defaultCompatibility` 等字段，必要时同步更新 `assets/chestcavity/lang/zh_cn.json` 里的条目。
- **资源桥入口**：所有 Guzhenren 真元/精力/念头 等附件都通过 `ChestCavityForge/src/main/java/net/tigereye/chestcavity/guzhenren/resource/GuzhenrenResourceBridge.java` 访问。打开后使用 `GuzhenrenResourceBridge.open(player)` 拿到 `ResourceHandle`。默认真元扣减统一调用 `ResourceHandle#consumeScaledZhenyuan(baseCost)`（按境界缩放），不要直接走 `adjustZhenyuan`，否则会绕过缩放公式。
- **监听器选择**：行为逻辑放在 `compat/guzhenren` 对应包，并在 `ChestCavityForge/src/main/java/net/tigereye/chestcavity/compat/guzhenren/GuzhenrenOrganHandlers.java` 里挂载。常用监听点包含 `listeners/OrganSlowTickListener`（持续回血/充能）、`listeners/OrganOnHitListener` / `OrganIncomingDamageListener`（命中/受击触发）、`listeners/OrganRemovalListener`（OnEquip/OnRemove 增益）。参考同文件现有蛊类（如水肾、血眼蛊）了解注册方式，确保缓存/Linkage 与移除逻辑配套。
- **Linkage 通道**：共享增益走 `LinkageChannel`。阅读 `ChestCavityForge/src/main/java/net/tigereye/chestcavity/linkage/LinkageChannel.java` 了解 API，通过 `LinkageManager.getContext(cc).channel(id)` 获得通道，再调用 `adjust()`/`set()`。新的 `INCREASE EFFECT` 数值在 `guzhenren:linkage/*` 命名空间统一管理。
- **公式/辅助工具**：回复/扣血等公共逻辑优先复用 `util` 与 `compat/guzhenren/resource` 下的 helper。例如持续回血可结合 `GuzhenrenResourceCostHelper` 或 `NBTCharge`，避免重复造轮子。新增 `util/AbsorptionHelper` 封装吸收护盾的容量提升与清理流程，记得给护盾时优先复用它。
- **交付检查**：完成后记得 `/reload` 验证 datapack、`./gradlew compileJava` 编译，必要时在 `AGENTS.md` 记录新的监听或通道 ID，便于下一位 Web Codex 继续扩展。

## 2025-09-30 GuScript UX & Runtime Additions (usage + notes)

What was added/changed
- Simulate Compile (UI)
  - Button label: “模拟编译”。位置：与分页按钮同一行，位于其左侧（在 UI 左边距内，如窗口过窄会贴近左边界）。
  - 点击后服务器生成“GuScript 编译日志”纸张（Lore 承载），内容包括：
    1) 编译步骤（每步：序号) 规则ID: 叶A + 叶B -> 产物）
    2) 最终根清单（kind + 名称）
    3) 消耗的叶（含标签与重复计数）
    4) 剩余的叶（含标签与重复计数）
  - 作用：用于核对“应用次数 vs 最终根数”、确认哪些叶被消耗、为何只触发一次杀招等。

- 规则优先与抑制
  - 杀招规则统一 priority=100，普通组合低于之，废能保持最低（负值）。
  - 基础组合“远程伤害”已添加抑制："inhibitors": ["杀招", "念头"]，避免降级杀招产物。
  - “念头远程伤害”规则收紧 required 为 {"远程伤害","念头","智道"}，并添加 "inhibitors": ["杀招"]，防止将既有杀招再次作为原料合成。

- Flow 资源守卫（不足直接取消，不扣不放）
  - 念头远程伤害（thoughts_remote_burst）：charged→releasing 跳转加守卫
    - 守卫：念头 ≥ 180 → releasing；念头 < 180 → cancel（不会扣念头与伤害）。
  - 醉元鼓（zui_yuan_gu）flow：只为压制缺状态告警，核心逻辑改至“根执行期”，不再依赖 flow 释放。

- 新“辅助杀招”：醉元鼓（真元+精力+酒道+辅助 → 杀招 醉元鼓）
  - 规则：`data/chestcavity/guscript/rules/zui_yuan_gu.json`
    - arity=4，priority=100
    - result.actions（根执行期）使用资源守卫 `if.resource`：
      - 若真元≥100 且 精力≥20：
        - `guzhenren.adjust zhenyuan -100`，`guzhenren.adjust jingli -20`
        - `linkage.adjust guzhenren:linkage/li_dao_increase_effect +0.1`
        - `linkage.adjust guzhenren:linkage/xue_dao_increase_effect +0.1`
        - `linkage.adjust guzhenren:linkage/shi_dao_increase_effect +0.1`
        - `emit.fx chestcavity:mind_thoughts_pulse`
      - 否则不做任何扣减与加成。
    - 目的：把加成放在“同一次按键会话”中导出，使后续同页杀招（如念头远程伤害）得到加成。

- 新动作（通用工具）
  - `if.resource`（根/动作期资源守卫）
    - JSON：`{ "id": "if.resource", "identifier": "zhenyuan", "minimum": 100.0, "actions": [ ... ] }`
    - 仅在 performer（玩家）资源满足时，执行嵌套 actions。支持 identifier: `zhenyuan`/`jingli` 等。
  - `linkage.adjust`（通道调整）
    - JSON：`{ "id": "linkage.adjust", "channel": "guzhenren:linkage/li_dao_increase_effect", "amount": 0.1 }`
    - 直接调整 linkage 通道数值（可正可负）。用于同页叠加加成或全局通道微调。

注意事项 / 差异提示
- “同页会话”叠加只对“根执行阶段导出”的 multiplier/flat 有效；flow 内的导出不跨会话合并。
  - time.accelerate 例外：执行器会在启动 flow 时将 page/会话导出的 timeScale 合并进 flow；但 multiplier/flat 不会自动跨 flow 合并。
  - 因此醉元鼓的加成已改为“根执行期导出”，保证同页伤害类杀招吃到增益。
- 想一次按键触发多个杀招：
  - 需要准备“互不争用”的完整素材（或分布到多个 KEYBIND 页）。
  - 否则第一次反应会消耗关键叶，第二条不成立或会被基础组合/废能兜底消耗。

文件索引（本次改动）
- UI 按钮与网络
  - 按钮：`src/main/java/net/tigereye/chestcavity/guscript/ui/GuScriptScreen.java`（左移一按钮宽度，和分页行对齐）
  - 模拟编译包：`src/main/java/net/tigereye/chestcavity/guscript/network/packets/GuScriptSimulateCompilePayload.java`
  - 注册：`src/main/java/net/tigereye/chestcavity/network/NetworkHandler.java`
- 规则/流
  - 念头远程伤害规则：`data/chestcavity/guscript/rules/niantou_yuancheng_shanghai.json`
  - 远程伤害规则：`data/chestcavity/guscript/rules/yuancheng_shanghai.json`
  - 念头远程伤害 flow：`data/chestcavity/guscript/flows/thoughts_remote_burst.json`
  - 醉元鼓规则/flow：
    - 规则：`data/chestcavity/guscript/rules/zui_yuan_gu.json`（根期守卫+扣资源+通道加成+FX）
    - 流：`data/chestcavity/guscript/flows/zui_yuan_gu.json`（最小状态集，非核心）
- 通用动作
  - 资源守卫：`guscript/actions/IfResourceAction.java` （id: `if.resource`）
  - 通道调整：`guscript/actions/AdjustLinkageChannelAction.java`（id: `linkage.adjust`）
  - 注册：`guscript/runtime/action/ActionRegistry.java`

验证方法
- 重载数据：`/reload`
- 编译：`./gradlew compileJava`
- 模拟日志：点击“模拟编译”获取纸张，确认“最终根/消耗叶/剩余叶”。
- 按键测试：
  - 看聚合日志是否包含“醉元鼓”在“念头远程伤害”之前；若是，后者应获得通道加成；
  - 资源不足时，相关 flow/守卫会引导至 cancel，不会扣资源也不会释放伤害。

常见问题
- “编译步骤出现两次杀招但只触发一次”：编译步骤记录“规则应用次数”，不是最终根数。后续应用可能复用前一次产物；请参看“最终根”与“消耗叶/剩余叶”判定。
- “念头远程伤害没有 multiply”：若没有其它根期导出乘区，multiplier=0 正常。醉元鼓改为根期导出后，需确保它在会话内先于杀招执行。


## 2025-09-29 YuanLaoGu 元石 & Blood-Bone Bomb Flow (analysis + TODOs)

What we verified (decompile, storage format)
- 元老蛊物品 `item.guzhenren.yuan_lao_gu_1` 持久化“元石”在物品自定义数据（DataComponents.CUSTOM_DATA）里：
  - 当前数量键：`"元老蛊内元石数量"`
  - 上限键：`"元老蛊内元石数量上限"`（通常 10000）
  - 参考：decompile/9_9decompile/cfr/src/net/guzhenren/item/YuanLaoGu1Item.java、…/YuanLaoGu1WuPinZaiWuPinLanShiMeiKeFaShengProcedure.java、…/YuanLaoGu1YouJiKongQiShiShiTiDeWeiZhiProcedure.java
- SHIFT+使用 时按 64 粒为单位吐出 `guzhenren:gucaiyuanshi`，并回写上述数量键。

Actionable TODOs (assign to web Codex worker)
- New helper in our compat bridge to read/write YuanLaoGu 元石数量
  - Path: `ChestCavityForge/src/main/java/net/tigereye/chestcavity/guzhenren/resource/YuanlaoguStoneHelper.java`
  - API (static methods):
    - `OptionalInt readCount(ItemStack stack)` → 读取 `"元老蛊内元石数量"`（四舍五入/向下取整）
    - `OptionalInt readMax(ItemStack stack)` → 读取 `"元老蛊内元石数量上限"`，无则默认 10000
    - `boolean setCount(ItemStack stack, int value, boolean clamp)` → 写入并可选 clamp 到 [0,max]
    - `int adjustCount(ItemStack stack, int delta, boolean clamp)` → 返回新值
  - Notes:
    - 优先使用 `util/NBTWriter` 提供的 Shared NBT helpers，避免直接操作 CustomData。
    - 仅对 `guzhenren:yuan_lao_gu_*`/`guzhenren:wei_lian_hua_yuan_lao_gu_*` 类物品生效（通过 `Item` 判定），否则返回 false。
    - 可选：提供 `syncName(ItemStack)`，按原版逻辑刷新 `CUSTOM_NAME`（显示数量）。

- Unit test for helper
  - Path: `ChestCavityForge/src/test/java/net/tigereye/chestcavity/guzhenren/resource/YuanlaoguStoneHelperTest.java`
  - Cases: 默认 0→加 64→到上限→溢出 clamp；上限不存在时 fallback=10000。

Blood-bone bomb flow didn’t “fire” (root cause analysis)
- Current logs show flows start: `Root 血骨爆弹#… started flow chestcavity:demo_charge_release`，但没有后续发射日志。
- Flow runtime is ticked server-side via `GuScriptFlowEvents.onServerTick` (registered in `ChestCavity.java`).
- Demo flow `chestcavity:demo_charge_release` 设计：`idle -(start)-> charging(40t) -> charged(160t) -> releasing(enter_actions: emit.projectile) -> cooldown`（总计 ~10s，随 `time.accelerate` 加速）。
- Most probable reasons:
  1) Start guards fail silently: `idle.start` 需要 `cooldown_ready` + `has_resource("zhenyuan", >=10)`。若真元不足→停留 `idle`，下一 tick 即结束实例（FlowInstance 在 `IDLE` 且 `ticksInState>0` 会 `finished=true`），因此每次触发都“开始”但不前进。
  2) 未等待到释放：需要 40+160t（默认 10s）；若期望“立刻发射”，需改设计或临时降低时间。
  3) 客户端期望的“禁足/扣血/扣精力/扣真元”还未实现在 flow 中（当前仅 FX + 发射），外观反馈不足易误判为未执行。

What to instrument (low-risk diagnostics)
- Add INFO logs when a state is entered（program id, state, ticksInState=0, timeScale），或在 `FlowSyncDispatcher.syncState` 旁打印一次；便于确认 40t/160t 是否达到。
- In `DefaultGuScriptExecutionBridge.emitProjectile` 已有 `[GuScript] Projectile #… emitted` 日志；排查时先搜索该日志。

Flow design tasks (align with “统一走 flow”的诉求)
- Implement per-second resource costs during CHARGING
  - In `demo_charge_release.json` → state `charging`:
    - `update_period: 20`
    - `update_actions`: `consume_health(2)`, `consume_resource("zhenyuan", 20)`, `consume_resource("jingli", 10)`，以及粒子/音效 `emit_fx` 心跳/收缩。
  - Failure path：在 `charging` 增加自动跳转 Guard：`health_below(>2)` 或 `resource_below("zhenyuan", 20)` / `resource_below("jingli", 10)` → target `cancel`；
    - `cancel.enter_actions`: `true_damage(50)`, `explode(power≈3-4)` + 失败音效/FX。

- Lock movement for 10s
  - Option A（简单）：`apply_effect(id=minecraft:slowness, amplifier=255, duration=200)`；
  - Option B（属性）：`apply_attribute(id=minecraft:generic.movement_speed, modifier=chestcavity:charge_lock, operation=ADD_MULTIPLIED_TOTAL, amount=-1.0)`，在 `cooldown.enter_actions` 或 `cancel.enter_actions` 里 `remove_attribute`；

- Keep “total cost = 10s baseline” under time.accelerate
  - Approach A（周期缩放）：`update_period = round(20 / time.accelerate)`（需要 flow loader支持基于变量的周期；当前不支持）
  - Approach B（量缩放）：在 `idle.start.actions` 里 `set_variable_from_param(param="time.accelerate", name="flow.time_scale", default=1)`，新增 `consume_*_scaled(amount, scale_variable)` 动作，按 `amount * time_scale` 结算；
  - Implementation note：当前 `FlowActions.consume_resource/consume_health` 仅定值参数，建议新动作或扩展现有动作支持 `amount_variable`。

- Release feedback and projectile
  - `releasing.enter_actions`: 已有 `emit.projectile("chestcavity:bone_gun_projectile", damage=12)`；可叠加 `emit_fx(chestcavity:burst)` 作强反馈。
  - 提醒：骨枪实体已在 `CCEntities` 注册，客户端渲染器在 `BloodBoneBombClient`；日志关键字：`[GuScript][Damage] BloodBoneBomb hit`（命中时）。

JSON compile test
- 已存在：`GuScriptJsonLoadTest` 会加载/编译 leaves/rules/flows，当前通过。
- 后续可加：flow 参数注入/时间加速解析的断言（验证 `parseTimeScale` 与流内变量同步）。

“thoughts_cycle 缺少 CHARGED/RELEASING” 警告（噪声清理）
- 在 `data/chestcavity/guscript/flows/thoughts_cycle.json` 添加最小 stub 状态：
  - `charged` 与 `releasing`，各自 `transitions: [{"trigger":"auto","target":"cooldown","min_ticks":1}]`；

How to quickly validate in-game
- 触发脚本后等 10s（或使用 `time.accelerate`），观察：
  1) 服务端日志是否出现 `Projectile #… emitted … id=chestcavity:bone_gun_projectile`；
  2) 命中时是否出现 `[GuScript][Damage] BloodBoneBomb hit …`；
  3) 若无，先确认真元≥10（`GuzhenrenResourceBridge.open(player).read("zhenyuan")`）。

Notes
- Flow 事件钩子（ServerTickEvent/Post、PlayerLoggedOut）已在 `ChestCavity.java` 以 `NeoForge.EVENT_BUS.addListener` 方式注册，无需 `@EventBusSubscriber`。
- 目前 flow 没有 movement-lock 与周期扣费：这是设计预期的缺口，需要上面的 JSON 和/或动作扩展去补齐。


### 2025-09-28 FX loading change (client-only)
- Decision: Load GuScript FX definitions on the client only to reduce server overhead and avoid client/server resource pack mismatch.
- What changed:
  - Removed server reload registration for `FxDefinitionLoader` in `ChestCavity.java:registerReloadListeners`.
  - Kept client registration via `RegisterClientReloadListenersEvent`.
  - Relocated FX JSONs from `data/chestcavity/guscript/fx/*.json` to `assets/chestcavity/guscript/fx/*.json` so client reload can find them.
- File references:
  - ChestCavityForge/src/main/java/net/tigereye/chestcavity/ChestCavity.java
  - ChestCavityForge/src/main/resources/assets/chestcavity/guscript/fx/*.json
- Guidance:
  - Add new FX under `assets/chestcavity/guscript/fx/`. Do not place FX JSONs under `data/` unless we add a server→client sync pipeline.
  - If we later need server datapack‑driven FX, implement an S2C sync payload and remove the client loader for FX to avoid double sources.

### Next branches and priorities (handoff)
1) `feature/guscript-flow-modules` (P1)
   - Implement Flow core MVP (Idle/Charging/Charged/Releasing/Cooldown/Cancel), loader for `data/chestcavity/guscript/flows/*.json`, server tick controller + light S2C mirror.
   - Acceptance: demo charge→release works; cooldown enforced; logs present.

## 2025-10-01 Jian Dao Sword-Slash FX/Flow Plan

Goal
- Implement a configurable “sword light” slash akin to Jian Dao, with adjustable size (length/width/lifespan), fired as a Projectile, that can break fragile blocks in flight. Provide general-purpose FX and Flow modules to drive visuals and behavior.

Scope & Constraints
- NeoForge 1.21.1, reuse existing GuScript infra for data-driven trigger where possible.
- Block breaking limited to a curated, tag-based whitelist; respect gamerules (mobGriefing) and server-only mutation.
  - Client visuals purely via FX JSON + renderer; do not require server resource JSON beyond tags.

## 2025-10-06 Guzhenren 属性桥接 PLAN（给 web codex 执行）

目标
- 将蛊真人玩家变量（真元/精力/魂魄/念头/道痕/境界/阶段等）规范化为统一的“属性桥接层”，供 ChestCavity 行为与器官逻辑读取/修改；必要时把关键能力映射到原版 Attribute（示例：MAX_ABSORPTION）。
- 读写一致、可缓存、可订阅变更，避免客户端权威；当兼容模组缺失时优雅降级（读 0/只读）。

必须先读的文件
- ChestCavityForge/src/main/java/net/tigereye/chestcavity/guzhenren/resource/GuzhenrenResourceBridge.java

范围与非目标
- 范围：桥接“玩家变量”到统一 API；把少量与战斗/生存紧密相关的能力映射为 Attribute（如吸收上限）；提供用于日志与命令的快照视图。
- 非目标：不改动蛊真人自身的数据定义/同步包格式；不引入新的网络协议（沿用现有 GuzhenrenNetworkBridge 事件）。

API 设计（服务端优先）
- 核心门面（AttributeFacade）
  - `double read(String key)` / `boolean write(String key, double v)` / `boolean adjust(String key, double dv)`；key 支持中/英别名与文档别名（沿用 GuzhenrenResourceBridge 解析）。
  - `double clampToMax(String key)`：当桥接层有“最大值”变量时进行封顶（例如 真元≤最大真元）。
  - `boolean present()`：是否存在兼容附件（缺失时只读 0）。
  - 事件：`onSynced(UUID player)` 回调刷新缓存（见下“事件与缓存”）。
- 属性枚举（GuzhenrenAttribute）
  - 维护标准化键集合与别名：`ZHENYUAN, JINGLI, HUNPO, NIAN_TOU, DAO_HEN_* ...`，并标注读/写/最大值键、是否会被 clamp。
  - 兼容文档里的歧义键（云道/运道等）→ 采用已存在的后缀区分（_CLOUD/_FORBIDDEN）。
- Attribute 注入（可选）
  - 复用 MAX_ABSORPTION 注入做法：在 `EntityAttributeModificationEvent` 中仅为玩家注册需要的 Attribute；
  - 只为「需要与原版数值系统交互」的能力注入 Attribute（如上限/抗性），其余变量通过桥接层读写即可。

实现步骤
1) 梳理桥接字段
  - 逐项核对 GuzhenrenResourceBridge 提供的枚举/字符串别名与 `read/writeDouble/adjustDouble/clampToMax`；
  - 输出一份“标准键→附件真实键”的映射表（Java 枚举+注释），并标注只读/可写/有上限。
2) 新建门面层
  - 包：`chestcavity/guzhenren/attribute/`
  - 类：`GuzhenrenAttribute`（枚举）、`GuzhenrenAttributeFacade`（实现上述 API，内部委托 GuzhenrenResourceBridge）
  - 类：`GuzhenrenAttributeCache`（简单缓存：`Map<String, Double>` + 脏标记/有效期，默认 1s）
3) 事件与缓存
  - 订阅 `GuzhenrenPlayerVariablesSyncedEvent`（已由 GuzhenrenNetworkBridge 发布），命中后刷新 Facade 缓存；
  - 保留 1s 客户端轮询仅用于日志（已有 DaoHenBehavior），服务端只走事件+主动写入后的本地刷新。
4) Attribute 注入（按需）
  - 在 `GuzhenrenModule.bootstrap(bus, EVENT_BUS)` 里追加 `EntityAttributeModificationEvent` 监听，参考 MAX_ABSORPTION；
  - 仅注入必要项（例如“最大念头上限”若会用于原版机制判断），否则留在桥接层。
5) 行为接入点
  - 统一在器官/监听代码里调用 Facade，而非直接操作附件；
  - 示例：某器官消耗真元 → `facade.adjust("真元", -cost)`；某被动状态受“禁道”影响 → `read("禁道_FORBIDDEN")` 参与计算。
6) 命令与调试
  - `/guz attr dump`：输出标准键与当前值、最大值（若有）；
  - `/guz attr set <key> <value>`：仅限 OP，用于临时校验读写路径。
7) 测试
  - 单测：枚举别名解析与 clamp 逻辑；
  - 集成：模拟 `player_variables_sync` 事件 → 校验缓存/读值；写入型用 FakeAttachment（当兼容模组缺席时只读）。
8) 日志
  - 统一前缀 `[compat/guzhenren][attr]`；关键节点打印：解析别名失败、写入被拒（只读/无附件）、clamp 生效、事件刷新命中/跳过。

文件变更（建议）
- 新增
  - `src/main/java/net/tigereye/chestcavity/guzhenren/attribute/GuzhenrenAttribute.java`
  - `src/main/java/net/tigereye/chestcavity/guzhenren/attribute/GuzhenrenAttributeFacade.java`
  - `src/main/java/net/tigereye/chestcavity/guzhenren/attribute/GuzhenrenAttributeCache.java`
  - `src/main/java/net/tigereye/chestcavity/guzhenren/command/GuzAttrCommands.java`
- 修改
  - `src/main/java/net/tigereye/chestcavity/guzhenren/GuzhenrenModule.java`：注册事件/命令
  - 器官/监听调用处：由直接桥接改为 Facade 读写（逐步替换）

判定标准（验收）
- 兼容模组在场：
  - 读写 `read/adjust` 能反映到蛊真人 UI/行为（或通过命令 dump 对比事件后的缓存）
  - `player_variables_sync` 到来后，Facade 缓存值变化一致、日志显示命中
  - 注入的 Attribute（若启用）在玩家身上可见，数值与桥接层一致
- 兼容模组缺席：
  - Facade `present()` 为 false；`read→0`、`write/adjust→false`，无 NPE
- 性能：常态读从缓存返回，事件驱动刷新；无 1tick 高频日志；无主线程阻塞

风险与回退
- 文档别名歧义：已通过 `_CLOUD/_FORBIDDEN` 后缀区分；仍歧义时以默认主键为准并记录 WARN。
- 注入 Attribute 的兼容性：仅在玩家上注入，避免影响生物；出问题可在配置/模块开关禁用注入路径。

执行顺序（给 web codex）
1) 研读 `ChestCavityForge/src/main/java/net/tigereye/chestcavity/guzhenren/resource/GuzhenrenResourceBridge.java`，列出可用键与别名；
2) 实现三件套（枚举/门面/缓存），先打通只读；
3) 接事件，验证缓存；
4) 打通写入（`adjust`→`write`），加 clamp；
5) 接入 1~2 个器官/监听作为样板；
6) 可选注入 Attribute（若本期需要）；
7) 加命令 dump/set；
8) 补文档与小测，提交 PR。


High-level Tasks
1) Projectile entity
   - New `SwordSlashProjectile` extending `ThrowableItemProjectile` or lightweight `Projectile` with custom width/length and lifespan ticks.
   - Params: `length`, `thickness` (width), `lifespan`, `baseDamage`, `breakPower`.
   - Movement: forward ray step per tick; sweep AABB along facing over `length` for hit detection; pierce limited entities (configurable `maxPierce`).
   - Registration: `CCEntities#SWORD_SLASH` server + client renderer registration.

2) Block breaking (fragile-only)
   - Define tag `#chestcavity:breakable_by_sword_slash` listing grass/leaves/glass/ice/snow/flowers/crops/sand/dirt/weak planks, etc. Keep conservative defaults.
   - Server tick: for each step segment, iterate intersecting block positions within swept AABB; if block matches tag and `!level.isClientSide`, destroy with `level.destroyBlock(pos, true /* drops */)`; cap per-tick break count for perf.
   - Respect `GameRules.RULE_MOBGRIEFING` and a mod config toggle `enableSwordSlashBlockBreaking`.

3) Entity hit logic
   - Deal damage once per entity per projectile (track UUID set) with knockback along slash direction; apply crit multiplier if originating attack was crit (optional: consult `JianYingGu` markers).
   - Optional status: short Bleeding/Weakness for flavor via `MobEffectInstance`.

4) Client visuals (FX + renderer)
   - Renderer: billboarding quad or thin extruded mesh aligned to velocity; additive glow; color/alpha fades over lifespan.
   - FX JSON under `assets/chestcavity/guscript/fx/`:
     - `sword_slash_spawn.json`: initial whoosh + spark particles + swing sound.
     - `sword_slash_trail.json`: short-lived trail particles following projectile.
     - `sword_slash_impact.json`: small shard burst on entity/block hit.

5) GuScript integration (flow/operator)
   - New operator "杀招·剑光" rule producing a root that fires the slash.
   - Flow `data/chestcavity/guscript/flows/sword_slash.json`:
     - ENTER: optional charge effects; emit.fx `sword_slash_spawn`.
     - RELEASING: `emit.projectile id=chestcavity:sword_slash length/thickness/lifespan from flow_params`; loop emit.fx `sword_slash_trail`.
     - EXIT: emit.fx `sword_slash_impact` when projectile ends.
   - Bridge: ensure `GuScriptExecutionBridge.emitProjectile` can pass custom NBT for size/damage params to the projectile on spawn.

6) Config and balancing
   - Add server config entries: default length, thickness, lifespan, damage, blockBreakCapPerTick, allowBlockBreaking, maxPierce.
   - Expose minimal client config for brightness/color.

7) Tags/Data
   - Add `data/chestcavity/tags/blocks/breakable_by_sword_slash.json` initial list; allow datapack overrides by users.

8) Tests & validation
   - Unit: math helpers for sweep and per-tick step segmentation.
   - Headless server test: spawn projectile in a small test level slice with leaf/glass blocks, assert those in tag are removed, others intact.
   - Runtime log guards: INFO on spawn with parameters; DEBUG for per-tick break count capped.

Implementation Notes
- Use stable `AttributeModifier` UUIDs only if adding temporary speed/attack-speed buffs via flows; not required for core projectile.
- For performance, compute discrete steps: `steps = ceil(length / stepSize)` with `stepSize≈0.5-0.75`; early-exit if lifespan expired.
- Ensure client/server separation: renderer + FX on client, all block/entity interaction on server.

File touchpoints (anticipated)
- `ChestCavityForge/src/main/java/net/tigereye/chestcavity/entity/SwordSlashProjectile.java`
- `ChestCavityForge/src/main/java/net/tigereye/chestcavity/registration/CCEntities.java`
- `ChestCavityForge/src/main/java/net/tigereye/chestcavity/client/render/SwordSlashRenderer.java`
- `ChestCavityForge/src/main/java/net/tigereye/chestcavity/guscript/flows/sword_slash.json` (datapack)
- `ChestCavityForge/src/main/resources/assets/chestcavity/guscript/fx/{sword_slash_spawn,trail,impact}.json`
- `ChestCavityForge/src/main/resources/data/chestcavity/tags/blocks/breakable_by_sword_slash.json`
- `ChestCavityForge/src/main/java/net/tigereye/chestcavity/config/CCConfig.java` (toggles)

Risks / Open Questions
- Multiplayer fairness: block breaking by player-fired slash should attribute to the shooter for permissions/claims; may need integration in protected servers (out of scope now).
- Glass panes/thin blocks collision requires careful AABB checks; start with full-cube checks, iterate if needed.


## 2025-09-30 Non‑Player Handlers: parallel branches plan

Goal
- 为尚未实现非玩家逻辑的器官增加 handlerNonPlayer 支持；当实体不是玩家时，以生命值作为真元/精力的替代支付路径（已有通用工具）。

Shared helper
- 使用 `GuzhenrenResourceCostHelper`（路径：`ChestCavityForge/src/main/java/net/tigereye/chestcavity/compat/guzhenren/resource/GuzhenrenResourceCostHelper.java`）统一扣费规则：
  - 玩家：优先扣真元/精力。
  - 非玩家：自动回退为生命值消耗（比例按 helper 内策略）。

Already covered (mob support present)
- 血眼蛊（Xieyangu）：onHit/slow‑tick 支持非玩家（server 端）。
- 血肺蛊（XieFeigu）：slow‑tick 支持非玩家（移速/攻速/氧气）。
- 血滴蛊（Xiedigu）：slow‑tick 支持非玩家（掉血与粒子/虚弱切换）。
- 铁血蛊（Tiexuegu）：slow‑tick 支持非玩家（生命消耗与 INCREASE 效果叠加；真元/精力回复限玩家）。

Candidates (add non‑player handler paths)
- 冰肌蛊（BingJiGu）：
  - 当前 onSlowTick 仅玩家生效，非玩家应走“生命值替代”扣费，保留回血/寒冷/护盾等效果中可适用于生物的部分。
  - 主动“冰爆”保持玩家专属；非玩家版本暂不实现主动引爆（后续如需可在 OnIncomingDamage 10% 规则下触发小型 AoE 冰爆）。
- 木道/石道/水道的三转器官（示例：木骨、石筋、水绳等，如存在）：
  - 按已有玩家逻辑镜像非玩家路径，支付走 `GuzhenrenResourceCostHelper`。
  - 仅应用对非玩家合理的效果（去除饥饿、氧气之类玩家专属分支）。
- 其它剑/毒/杜等器官若仍缺非玩家路径，逐器官评估加入。

Branching (parallel, small PRs)
- 每个器官/家族开独立分支，降低冲突。建议一次只处理一个器官，便于回滚与验证。

Planned branches to add handlerNonPlayer (HP fallback via GuzhenrenResourceCostHelper)
- `feature/mob-bingjigu-support`（冰肌蛊，bing_xue_dao）
  - 非玩家 slow‑tick：按效率回血/寒冷/护盾，扣费走 HP 替代。
  - 主动“冰爆”暂不对非玩家开放（可选：被击10%小范围爆）。
- `feature/mob-shuishengu-support`（水绳蛊，shui_dao）
  - 非玩家 slow‑tick：充能/护盾数值保留；精力/真元改用 HP 替代；去除饥饿等玩家限定分支。
- `feature/mob-roubaigu-support`（肉白骨，gu_dao）
  - 非玩家 slow‑tick：骨生长与被动治疗保留；资源支付改用 HP 替代；关闭玩家 UI/物品修复分支或降级。
- `feature/mob-yugugu-support`（玉骨蛊，gu_dao）
  - 非玩家 slow‑tick：只保留可适用增益；资源支付走 HP；移除仅玩家可见/可交互逻辑。
- `feature/mob-gangjingu-support`（钢筋蛊，gu_dao）
  - 非玩家 slow‑tick：吸收盾恢复与容量维持；扣费走 HP；保留既有间隔门限逻辑。
- `feature/mob-fandaicaogu-support`（帆带草蛊，shi_dao）
  - 非玩家 slow‑tick：按器官语义迁移 buff/debuff；支付 HP 替代。
- `feature/mob-yuanlaogu-support`（元老蛊，yu_dao）
  - 非玩家 slow‑tick：元石吸收/释放逻辑可选支持；真元/精力→HP 替代；上限守卫复用。
- `feature/mob-jianjiteng-support`（缄棘藤，gu_cai）
  - 非玩家 slow‑tick：保留对生物生效的环境/属性分支；支付 HP 替代。
- `feature/mob-tupigu-support`（土皮蛊，wu_hang/土）
  - 非玩家 slow‑tick：与玩家路径对齐；支付 HP 替代。
- `feature/mob-jinfeigu-support`（金肺蛊，wu_hang/金）
  - 非玩家 slow‑tick：与玩家路径对齐（护盾/移动等）；支付 HP 替代。
- `feature/mob-mugangu-support`（木肝蛊，wu_hang/木）
  - 非玩家 slow‑tick：生物通用效果保留；支付 HP 替代。

Per‑branch TODO template
1) Wire non‑player slow‑tick/on‑hit/被击（按器官语义）
   - 使用 `GuzhenrenResourceCostHelper` 扣费；失败则早退或按器官既定失败逻辑处理。
2) 效果筛选
   - 玩家专属效果（饥饿、氧气、GUI、按键引爆等）在非玩家路径关闭或降级。
3) Debug logs（可选）
   - 统一加开关：`-Dchestcavity.debug<OrganName>=true`，仅在 Server 端打印关键信息（进入/退出、扣费、阈值守卫）。
4) Light tests（如可）
   - 头部单元：方法级别验证“HP 回退扣费”路径；
   - JSON/构建：`./gradlew compileJava` + 现有 GuScript JSON 装载测试通过。

Acceptance checklist per organ
- [ ] 非玩家路径能运行且不会因资源缺失报错。
- [ ] HP 替代扣费与玩家真元/精力扣费互不干扰。
- [ ] 仅应用非玩家合理效果，无客户端特有依赖。
- [ ] Debug 开关默认关闭，开启时提供足够排错信息。

2) `feature/guscript-ability-relocation-phase2` (P2)
   - Switch registrations/imports to `net.tigereye.chestcavity.guscript.ability.guzhenren`; remove deprecated shims.
   - Validate abilities still trigger and render.
3) `feature/guscript-ordering-tests` (P2)
   - Add tests to confirm unordered roots retain compilation order and ordered roots only reposition themselves.
4) `feature/guscript-keybind-aggregate-tests` (P2)
   - Add tests for multi-page keybind aggregation caps and ordering; ensure all eligible pages/roots execute.
5) `feature/guscript-ui-gutter` (P3)
   - Finalize responsive spacing with a configurable minimum gutter; verify no overlap at varied GUI scales.

### Validation checklist (post-FX move)
- Client logs show FX definitions loaded (>0). Search: "[GuScript] Loaded .* FX definitions".
- Trigger an `emit.fx` action; ensure no "unknown FX id" warnings and visuals/sounds play.
- Dedicated server starts without any FX loader logs or CNFE.

## 2025-09-28 Flow inputs, selection, and state→FX (Web Codex TODOs)

Temporary decision
- Flows are disabled by default to follow the original immediate-execution path. Toggle via config: `CCConfig -> GUSCRIPT_EXECUTION -> enableFlows`.

Goal
- 1) Bind client keys for flow inputs (RELEASE/CANCEL)
- 2) Let scripts/pages choose `flow_id` instead of a hardcoded demo
- 3) Add direct state→FX hooks with a minimal test case

Branches (create separately to avoid conflicts)
- `feature/flow-input-keybinds` — client input mapping + payload wiring
- `feature/guscript-flow-selection` — per-root/page `flow_id` selection (fallback to immediate)
- `feature/flow-state-fx-and-tests` — state→FX linkage and minimal tests

### 2025-09-28 Tests status and how to run
- Current status: full test suite passes locally.
- Commands:
  - Compile only: `cd ChestCavityForge && ./gradlew -g .gradle-home compileJava`
  - Run tests: `cd ChestCavityForge && ./gradlew -g .gradle-home test`
- Flow toggle and tests:
  - By default `enableFlows=false`; tests exercise the immediate-execution path.
  - If adding flow-dependent tests later, either enable via config fixture or keep such tests guarded/skipped when flows are disabled.

### Immediate next actions (flows remain disabled)
- Tests (ordering/aggregation): ensure coverage for
  - Unordered roots preserve compilation order; ordered roots reposition themselves only.
  - Multi-page keybind aggregation executes all eligible roots once and respects caps.
- UI polish (no-flow mode):
  - Hide/disable any flow-related UI affordances when `enableFlows=false`.
  - Verify minimum gutters across GUI scales; keep page text clear of buttons.
- Logging and config:
  - Gate INFO-level hot-path logs behind a debug flag; keep FX/execute logs concise.
  - Expose caps (pages/roots per trigger) in config and document defaults.
- Optional DX:
  - Add a lightweight `/guscript run [page]` command for headless/dedicated testing of immediate execution.

1) Flow input keybindings (RELEASE/CANCEL)
- Scope
  - Add client keys and send `FlowInputPayload` on key events.
  - Optional: map key-up of `GUSCRIPT_EXECUTE` (B) → RELEASE; map `C` → CANCEL.
- Files
  - Client: `src/main/java/net/tigereye/chestcavity/listeners/KeybindingClientListeners.java`
  - Keys: `src/main/java/net/tigereye/chestcavity/registration/CCKeybindings.java`
  - Network: already present `FlowInputPayload`/`NetworkHandler`
- Steps
  - Register two mappings: `guscript_release` (key-up of B) and `guscript_cancel` (e.g., C).
  - In client tick, when released or pressed, send `new FlowInputPayload(FlowInput.RELEASE|CANCEL)`.
  - Ensure no classloading on server (client-only code guarded by event distro).
- Acceptance
  - Press/hold B starts flow (existing). Releasing B sends RELEASE and transitions from CHARGED→RELEASING if guards pass.
  - Pressing C cancels a running flow (CHARGING/CHARGED→CANCEL→COOLDOWN path in demo JSON).

2) Per-script/page `flow_id` selection (removes hardcoding)
- Scope
  - Allow a reduced operator/root (or page binding) to specify `flow_id` and optional params.
  - Executor: if a root has `flow_id`, start that flow; if missing, immediate execution.
- Files
  - Runtime: `src/main/java/net/tigereye/chestcavity/guscript/runtime/exec/GuScriptExecutor.java`
  - AST/operator: extend `OperatorGuNode` to carry optional `flow_id`/`flow_params` (codec/loader as needed)
  - Rule loader/compiler: where operator metadata is built (ensure JSON can carry `flow_id`)
- Steps
  - Add optional `flow_id` (ResourceLocation) to operator metadata.
  - In `GuScriptExecutor.executeRootsWithSession`, detect `flow_id` and call `FlowControllerManager.get(player).start(program, target, time)`; skip immediate actions when flow is used.
  - Log fallback when `flow_id` missing or not found.
- Acceptance
  - A page with two roots: one with `flow_id` uses flow timeline; the other executes immediately. Both behave as expected when pressing B.

3) State→FX linkage + minimal tests
- Scope
  - Data-driven FX bundles when entering states and/or on transitions.
  - Minimal tests for guard/cooldown and state progress.
- Files
  - Loader: `src/main/java/net/tigereye/chestcavity/guscript/registry/GuScriptFlowLoader.java`
  - Runtime client: `src/main/java/net/tigereye/chestcavity/guscript/runtime/flow/client/GuScriptClientFlows.java`
  - FX: reuse `emit.fx` via S2C payloads; add a small dispatcher call on client mirror when state changes.
  - Tests: `src/test/java/net/tigereye/chestcavity/guscript/runtime/flow/*` (unit tests)
- Steps
  - Extend flow state JSON to allow `enter_fx: ["namespace:id", ...]` (and optionally `update_fx`).
  - GuScriptFlowLoader: parse and store `enter_fx` bundles into `FlowStateDefinition`.
  - Server: on state change, include FX ids in FlowSyncPayload or broadcast a dedicated FxEventPayload from server (preferred client-only: emit via client when mirror updated to avoid server imports).
  - Client: `GuScriptClientFlows` resolves FX ids and calls `FxClientDispatcher.play` for each bundle with reasonable context (origin/look from player).
  - Tests (minimal): verify guard passes/blocks; verify cooldown timestamp logic; simulate a sequence of triggers to assert state ordering.
- Acceptance
  - Demo flow plays a sound/particle burst on CHARGED enter and on RELEASING enter.
  - Tests pass; client shows FX without server CNFE.

Notes
- Keep client/server separation strict. No client imports in common/loader code paths.
- Preserve backward compatibility: flows optional; scripts without `flow_id` execute immediately.
- Update this file after merging each branch with status and any follow-ups.

### 2025-09-28 Test compile failure after merge (Web Codex TODO)
- Symptom: `./gradlew test` fails during `compileTestJava`.
- Error:
  - `GuScriptRuntimeTest.java: RecordingBridge is not abstract and does not override abstract method playFx(ResourceLocation,FxEventParameters) in GuScriptExecutionBridge`.
- Root cause:
  - Interface `guscript/runtime/exec/GuScriptExecutionBridge` added `playFx(ResourceLocation, FxEventParameters)`.
  - Test stub `RecordingBridge` in `src/test/java/net/tigereye/chestcavity/guscript/runtime/exec/GuScriptRuntimeTest.java` not updated.
- Fix to implement:
  - Add a no-op method to the inner class `RecordingBridge`:
    - `public void playFx(net.minecraft.resources.ResourceLocation fxId, net.tigereye.chestcavity.guscript.fx.FxEventParameters parameters) { /* no-op for tests */ }`
  - Re-run `./gradlew test` and address any follow-on errors.
- Context references:
  - Interface: `ChestCavityForge/src/main/java/net/tigereye/chestcavity/guscript/runtime/exec/GuScriptExecutionBridge.java`
  - Default impl: `ChestCavityForge/src/main/java/net/tigereye/chestcavity/guscript/runtime/action/DefaultGuScriptExecutionBridge.java: playFx(...)`
  - Failing test: `ChestCavityForge/src/test/java/net/tigereye/chestcavity/guscript/runtime/exec/GuScriptRuntimeTest.java`

## 2025-10-XX Pending tasks handoff (requires web Codex implementation)
- **New request 2025-??-??**: Implement Flow core MVP (`feature/guscript-flow-modules`). Deliverables include FlowProgram/FlowInstance/FlowController with Idle/Charging/Charged/Releasing/Cooldown/Cancel states, resource/cooldown guards, edge actions (resource deduction, cooldown set, GuScript action triggers). Load and validate `data/chestcavity/guscript/flows/*.json` with error logging. Mirror state/time via `S2C FlowSyncPayload` containing minimal fields. Acceptance: server-driven "charge → release" demo stays in sync on client.
- **Bug: multiple kill moves on shared trigger execute only one**
  - Scenario: same GuScript page stores several fully reduced “杀招” nodes with identical trigger (e.g., keybind or listener). Currently `GuScriptExecutor`/`GuScriptRuntime` only fires the first result; later roots never cast. Investigate `GuScriptCompiler` + `GuScriptRuntime.executeAll` to ensure every compiled root invokes its actions without being short-circuited.
  - Notes from CLI review: compiler now iterates all page slots (minus binding slot) when building leaf list. Verify reducer isn’t collapsing siblings or returning mixed leaf remnants. Add regression coverage once fixed.
  - User retest (post-commit 8c6ea456): still only one projectile emitted when two kill moves share the keybind. Need additional instrumentation/logging to confirm cache root count vs. runtime dispatch.

### Multi-trigger emits one projectile — probable causes (analysis only)
- Context reuse across roots: If `GuScriptRuntime.executeAll` uses a single `GuScriptContext` instance for multiple roots, state mutations (damage multipliers, cooldown flags, resource failures) can suppress later casts. Mitigation: switch to a `Supplier<GuScriptContext>` factory to create a fresh context per root (see upstream change pattern in 8c6ea456 and unit test strategy).
- Reduction output is singular: Rules or inventory composition may yield only one composite root (the second “kill move” might be just a leaf/operator without `emit.projectile`). Add debug to print reduced root count, kinds, and names to corroborate expectations.
- Stale program cache: `GuScriptProgramCache` might return outdated `roots()` if signature hashing misses some slots or metadata. Confirm signature covers all item slots (excluding the binding slot) and binding/listener selections; force recompilation on page dirty to compare.
- Bridge gating on client: `DefaultGuScriptExecutionBridge.emitProjectile` early-returns on client. Ensure execution runs strictly on server (it should), and log side/context per root.
- Keybind plumbing only sends one trigger: Client sends one payload by design; server must iterate all compiled roots. Instrument `GuScriptExecutor.trigger` to log `roots.size()` and “executing N/total” per root.

Status
- Branch `feature/guscript-multi-trigger-fix`: Completed (roots on the current page now all execute; fresh context per root; logs present).

### Proposed diagnostics (to implement by web Codex)
- Add INFO logs:
  - After compilation: page index, `roots.size()`, list of `root.kind()` + `root.name()`.
  - During execution: per-root context creation (unique id), server/client side, and action dispatch counts (e.g., how many `emit.projectile`).
- Add a focused unit test mirroring 8c6ea456’s approach: verify two composites produce two projectile emissions and independent context creation counts.

### Implementation checklist (branch: `feature/guscript-multi-trigger-fix`)
1) Update `GuScriptRuntime` to include `executeAll(List<GuNode>, Supplier<GuScriptContext>)` and refactor callers to use a context supplier.
2) In `GuScriptExecutor.trigger`, pass a supplier that constructs a new `DefaultGuScriptContext`/`DefaultGuScriptExecutionBridge` per root.
3) Add the diagnostics above and remove/guard them behind debug flags if needed.

### UI spacing tweak (branch: `feature/guscript-ui-spacing-tweak`)
- Shift page navigation button row left by 10 px; shift trigger/listener button stack down by 10 px. Keep proportional spacing and min gutters intact; update defaults or layout math accordingly.

Status
- Completed (merged via UI spacing PRs: configurable spacing, button offsets, and page info spacing). Visual overlap resolved per user verification.

### Relocate Guzhenren abilities into GuScript module

Goal: Move `src/main/java/net/tigereye/chestcavity/compat/guzhenren/ability` under the GuScript module so that Guzhenren-related active skills live alongside GuScript runtime/bridges.

Proposed package target
- From: `net.tigereye.chestcavity.compat.guzhenren.ability`
- To (phase 1): `net.tigereye.chestcavity.guscript.ability.guzhenren`

Branching
- Phase 1 (non-breaking): `feature/guscript-ability-relocation-phase1`
  - Create new packages/classes and add shims in old package annotated `@Deprecated` forwarding to new implementations.
  - Keep all registrations and event subscriptions wired through old package to avoid ripple changes.
  - Compile + run validation.
- Phase 2 (cutover + decomposition): `feature/guscript-ability-relocation-phase2`
  - Update all call sites/registrations/imports to new package.
  - Remove deprecated shims; clean imports.
  - Decompose abilities into two concerns:
    - Damage/logic (server): pure gameplay actions (damage calc, status, projectiles) implemented via GuScript actions or dedicated server utilities.
    - FX/visuals (client): particles/sounds/trails implemented via the GuScript FX registry and `emit.fx` payloads.
  - Final compile + in‑game smoke test.

Decomposition details
- Server (damage/logic)
  - Package: `net.tigereye.chestcavity.guscript.ability.guzhenren.logic`
  - Use/extend existing GuScript actions: `emit.projectile`, `modifier.damage_multiplier`, `apply.status`, etc.
  - Add small utilities for damage number composition if needed (reuse ExecutionSession stacking where applicable).
- Client (FX/visuals)
  - Package: `net.tigereye.chestcavity.guscript.ability.guzhenren.fx` (client-only classes guarded by Dist).
  - Drive visuals with data under `assets/chestcavity/guscript/fx/*.json` and play via `FxClientDispatcher`.
  - No client imports from server/common code paths.

Cutover steps
1) Replace imports from `compat.guzhenren.ability.*` to `guscript.ability.guzhenren.*` across codebase.
2) Remove deprecated shims created in Phase 1.
3) Split any mixed classes into logic vs FX packages; keep server actions free of client imports.
4) Ensure registrations (events, renderers, keybindings) reference the new packages; client-only subscribers annotated with `@EventBusSubscriber(value = Dist.CLIENT)`.
5) Build and smoke test in-game: hotkey triggers still function; damage/effects both fire; no CNFE on dedicated server.

Acceptance criteria
- All ability triggers build and run from the new `guscript.ability.guzhenren` packages.
- Damage logic executes on server; FX plays only on clients; no cross-dist classloading.
- No deprecated shims remain; `rg` for old package returns no usages.

Status (2025-10-XX)
- Phase 1 completed (per user confirmation). Proceed to Phase 2 cutover after parallel branches below are merged or rebased.

Detailed steps (Phase 1)
1) Copy ability classes to `net.tigereye.chestcavity.guscript.ability.guzhenren` preserving public APIs.
2) Review client/server split: any client-only renderers or keymaps must remain under client-dist guarded subscriptions.
3) In old package, create thin wrappers that extend/delegate to the new classes; mark `@Deprecated`.
4) Ensure registry/network IDs remain unchanged; do not rename `ResourceLocation`s or payload `TYPE`s.
5) Build: `./gradlew compileJava`; address visibility issues by relaxing to `public` where package-private was used.

Detailed steps (Phase 2)
1) Update registrations (event listeners, keybindings, entity renderers) to import from `guscript.ability.guzhenren`.
2) Remove shim classes and obsolete imports.
3) Search usages via `rg` for the old package to confirm zero references.
4) Final compile and functional test: verify abilities still trigger, render, and consume resources correctly.

Risks & mitigations
- Reflection/Mixin references to FQCNs: search for fully-qualified names in mixins/config; keep names or update configs accordingly.
- Access modifiers: classes relying on package-private collaborators may need `public`.
- Dist separation: keep `@EventBusSubscriber(value = Dist.CLIENT)` where appropriate to avoid classloading on server.

## 2025-09-29 Time.accelerate 嵌入优化（交付给 Web Codex）

预准备
- 配置与环境
  - 默认保持 `enableFlowQueue=false` 以兼容旧行为；需要验证队列时再开启。
  - 保持现有 Flow 运行日志（FlowInstance.enterState 的 INFO 已插桩）。
  - 使用 `/guscript flow start <flow_id> [timeScale] [k=v …]` 命令直接启动 Flow 验证（已添加）。
- 现状问题
  - “时念加速”通过单独启动一个 flow 占用控制器，导致“血骨爆弹”等主技无法同时启动 → 出现“started flow 但不发射”的假象。
  - `time.accelerate` 目前只作为 flowParams 的一个数值被简单解析，缺乏统一的会话导出/多来源叠加/可观测能力。

计划（实现步骤）
1) 新增时间加速导出动作（Action）
   - 新建：`ExportTimeScaleMultiplierAction`（id: `export.time_scale_mult`）、`ExportTimeScaleFlatAction`（id: `export.time_scale_flat`）。
   - 放置路径：`ChestCavityForge/src/main/java/net/tigereye/chestcavity/guscript/actions/`。
   - 在 `ActionRegistry.registerDefaults()` 注册解析：读取 `amount`（double）。
   - 语义：将时间倍率作为“会话导出”存入执行会话（mult/flat 两通道）。

2) 扩展会话与上下文
   - 在 `ExecutionSession` 增加字段：`currentTimeScaleMult`（默认 1.0）、`currentTimeScaleFlat`（默认 0.0），并提供：
     - `exportTimeScaleMult(double)`, `exportTimeScaleFlat(double)`
     - `currentTimeScale()`：按策略合并（默认 MULTIPLY）
   - 在 `DefaultGuScriptContext` 暴露导出 API 以便动作调用。

3) 执行器注入与合并（核心）
   - 修改 `GuScriptExecutor.executeRootsWithSession(...)`：
     - 先执行所有 operator roots 以收集“导出”（包括本次新增的 timeScale 导出）；此步不启动任何 flow。
     - 启动 flow 前，读取 page/rule 的 `flowParams["time.accelerate"]`（若缺省视作 1.0），与 `session.currentTimeScale()` 合并：
       - `effectiveScale = combine(flowParamScale, sessionScale)`，clamp 到 [0.1, 100]。
       - 将 `effectiveScale` 写回 `flowParams["time.accelerate"]`，并作为 `start(program, timeScale=effectiveScale, flowParams=...)` 的 timeScale 参数。
     - 打印一条 INFO：包含 root 名称、flowId、flowParamScale、sessionScale、effectiveScale、params 摘要。
   - 合并策略配置：见第 5 步。

4) 规则与 JSON 调整
   - 将“时念加速”规则从“启动 time_acceleration flow”改为仅导出 timeScale（使用新动作）+ `emit.fx` 播放入场/循环/退出 FX（可选），不再占用控制器。
   - 保留 `demo_charge_release` flow，charging/charged/releasing 不变；`update_period=20` 维持“总成本不变”。

5) 配置项
   - 在 `CCConfig.GUSCRIPT_EXECUTION` 中新增：
     - `timeScaleCombine`: 字符串枚举，`"MULTIPLY" | "MAX" | "OVERRIDE"`（默认 MULTIPLY）。
   - 在注入处读取策略：
     - MULTIPLY：`effective = flow * session`（若 flow 缺省按 1.0 处理）
     - MAX：`effective = max(flow, session)`
     - OVERRIDE：`effective = session`（会话导出覆盖）

6) 日志与可观测性
   - 启动 flow 时打印：`[GuScript][Flow] <root>#<idx> merged timeScale: flow=<f>, session=<s>, effective=<e>`。
   - Flow 状态进入日志（已存在）：`[Flow] <flowId> entered <STATE> (timeScale=X.XXX, params={...})`。
   - `/guscript flow start` 命令已支持 `timeScale` 与 `key=value` 参数注入，用于手动验证合并与注入是否生效。

7) 保留与兼容
   - 不改动 FlowInstance 的 tickAccumulator 逻辑：时间加速只改变“到达逻辑 tick 上限的速度”，不改变触发次数，总扣费不变。
   - 队列逻辑保持现状（默认关闭）。如需验证队列行为，服务端 `config/chestcavity.json(5)` 中开启 `enableFlowQueue` 并重启。

测试（新增/调整）
1) 单元测试：时间合并策略（新建 `FlowTimeScaleCombineTest`）
   - MULTIPLY：flow=1.2, session=1.5 → effective≈1.8；
   - MAX：flow=1.2, session=1.5 → 1.5；
   - OVERRIDE：flow=2.0, session=1.5 → 1.5。
   - clamping：超界值被 clamp 至 [0.1, 100]。

2) 执行器注入测试（新建 `GuScriptExecutorTimeScaleTest`）
   - 构造两个 roots：
     - root A（operator）：导出 `export.time_scale_mult(1.5)`；
     - root B（operator）：启动 flow `test:progress`，不显式给 `time.accelerate`；
   - 断言：`start(...)` 的 timeScale≈1.5 且 `controller.resolveFlowParam("time.accelerate")≈"1.5"`。
   - 再测：root B 的 flowParams 提供 `time.accelerate=1.2`，策略 MULTIPLY → effective≈1.8。

3) 行为测试：总成本不变
   - 构造一个 flow：`charging(200 ticks)`、`update_period=20`，在 `update_actions` 累加计数变量；
   - 启动一次 `timeScale=1.0`，完成后计数=10；
   - 再启动 `timeScale=2.0`，完成后计数仍=10（验证更新次数不变）。

4) 回归测试
   - 现有 JSON 载入测试不变；
   - Flow 队列测试不变（默认关闭时拒绝、开启时入队）。

预期效果
- “时念加速”等加速器不再占用 flow 控制器，血骨爆弹等主技能正常启动、蓄力、释放发射。
- `time.accelerate` 的来源可同时来自页面参数与会话导出，按配置策略合并，日志明确显示合并过程与结果。
- Flow 的周期扣费次数不因加速而改变，总成本保持 10s 基线设计；用户体感为“更快完成”。

日志与注释风格
- 使用 INFO 记录关键生命周期（导出合并结果、flow 启动、状态切换、队列行为），DEBUG 用于守卫失败原因与细节。
- 新增类/方法写 Javadoc 简述职责与合并策略；在 ActionRegistry 注册处注释“强类型化参数与默认值”。

提交与发布
- 建议分支：`feature/guscript-time-accelerate-channel`。
- 完成后更新本章节为“已完成/后续优化”（如需要扩展 `consume_*_scaled` 等高级用法）。

## 2025-09-29 Shuishengu 盾牌接口提取（已完成）
- 将水肾蛊的 `onIncomingDamage` 抵挡逻辑抽离为通用接口：新增 `listeners.damage.IncomingDamageShield`（含 `ShieldContext` / `ShieldResult`）与 `util.math.CurveUtil`（提供指数平滑曲线）。
- 新建 `ShuishenguShield` 实现该接口，负责指数曲线、消耗/返还 charge、FX/音效播发、Linkage 比例推送。
- `ShuishenguOrganBehavior` 迁移至 `compat.guzhenren.item.shui_dao.behavior`，慢速充能逻辑保留，OnIncomingDamage 通过 `ShuishenguShield.INSTANCE.absorb(...)` 委托；同时沿用新静态方法复用 charge 计算。
- 更新 `WuHangOrganRegistry` 引用新的包路径；移除旧文件。
- 编译 / 测试：`./gradlew compileJava`、`./gradlew test` 通过。
- 后续：其它 Organ 若需盾牌行为，可实现 `IncomingDamageShield` 并在各自 `OrganIncomingDamageListener` 中委托。若需统一调度，可在未来扩展通用 ShieldRegistry。

Validation checklist
- Hotkey bindings and ability triggers still work (no missing IDs).
- Network payloads unchanged; client/server payload handling intact.
- Logs: no CNFE/NoSuchMethodError from relocated classes.

### Cross-root Damage Stacking (per-trigger ordered accumulation)

Goal
- Allow multiple GuScript roots (kill-moves) within a single trigger to stack damage bonuses in order: earlier roots can export modifiers that affect later roots.

Design
- ExecutionSession: holds cumulativeMultiplier/flat (with caps). Lives for one trigger dispatch.
- Ordering: each composite root may declare `order` (int). Roots execute in ascending order; ties use stable rule/name ordering.
- Export: via actions `export.modifier.multiplier` / `export.modifier.flat`, or node-level `export_modifiers` flags in rule result. Exported deltas accumulate into the session and seed the next root's context.

Runtime changes (to be implemented by web Codex)
- `GuScriptExecutor.trigger`: build session; sort roots by `order`; for each root: seed context with session modifiers → execute → collect exported modifiers → session += exported.
- `DefaultGuScriptContext`: track added multipliers/flats and expose exported values; or mark export actions explicitly.
- `ActionRegistry`: register `export.modifier.*` actions.
- Rule JSON: optional fields `order` (int) and `export_modifiers` ({"multiplier":bool,"flat":bool}).

Acceptance
- With three roots ordered 0/1/2, earlier exports adjust later roots’ final `emit.projectile` damage deterministically. Caps prevent runaway stacking.

## GuScript Effects & Flow Modularization (new feature)

Goal
- Introduce reusable, data-driven modules for combat “flows” (e.g., charge-up → release) and audiovisual “effects” (particles, sounds, screenshake) that any GuScript kill-move can compose without bespoke Java.

Design pillars
- Flow as state machine: Idle → Charging → Charged → Releasing → Cooldown → (Cancel). All states/edges data-declarable with guards (resource/condition) and side effects (actions/effects).
- Effects as composables: small, stateless emitters (particle burst, trail, sound cue, camera kick) that run client-only via a registry and can be scheduled on flow edges.
- Script-agnostic: wiring lives in GuScript runtime; any operator node can attach a Flow program and Effect bundles via JSON.
- Determinism + sync: server owns flow state; clients mirror via lightweight S2C updates with timestamps/normalised progress.

Proposed packages
- `net.tigereye.chestcavity.guscript.flow`
  - `FlowProgram` (immutable data), `FlowState`, `FlowInstance` (runtime), `FlowGuard`, `FlowEdgeAction`
  - Guards: resource available, cooldown ready, target in range/LOS, item present, linkage ≥ X
  - Actions: schedule GuScript actions, set cooldown, consume resource/hp, push velocity
- `net.tigereye.chestcavity.guscript.fx`
  - `FxEmitter` (functional), `FxRegistry`, built-ins: `Particles`, `Sound`, `ScreenShake`, `LightFlash`, `Trail`
  - Client hook: register emitters; server schedules via S2C `FxEventPayload`
- `net.tigereye.chestcavity.guscript.data.flow`/`data/fx`
  - Codecs/JSON schema; datapack loaders; validation and logs

Runtime API (high level)
- Flow
  - `FlowProgram` describes states, edges and callbacks; an edge can reference: (a) GuScript action list, (b) Fx bundle id(s)
  - `FlowInstance.tick(server)` advances by guards/time; emits S2C updates for client mirrors
  - `FlowController` binds a program to a performer+target per trigger (keybind/listener)
- FX
  - `FxRegistry.register(id, FxEmitter)`; `FxBus.play(id, FxContext)` (client)
  - Context carries positions, direction, color/intensity, owner entity id, seeded rng

Data format (example)
```json
{
  "id": "chestcavity:charge_and_burst",
  "states": [
    {"name": "idle"},
    {"name": "charging", "duration": 30, "on_enter_fx": ["fx:charge_loop"], "on_update_fx": ["fx:charge_sparks"]},
    {"name": "charged", "duration": 10, "on_enter_fx": ["fx:charged_glow"]},
    {"name": "releasing", "on_enter_actions": [ {"id":"emit.projectile","projectileId":"minecraft:arrow","damage":6} ], "on_enter_fx": ["fx:burst"]}
  ],
  "edges": [
    {"from": "idle", "to": "charging", "guard": {"id":"resource.ready","zhenyuan":5}},
    {"from": "charging", "to": "charged", "after": 30},
    {"from": "charged", "to": "releasing", "input": "release"},
    {"from": "charging", "to": "idle", "input": "cancel"}
  ],
  "cooldown_ticks": 60
}
```

Networking
- S2C: `FlowSyncPayload` (performer id, program id, state, t, random seed) — drives client-side FX deterministically.
- C2S: `FlowInputPayload` (performer id, input=release/cancel) — e.g., key-up to release。

Phased delivery & branches
- Phase 1 (core flow runtime) — `feature/guscript-flow-modules`
  - Implement `FlowProgram/Instance/Controller`, guards/actions minimal set；wire to keybind/listener trigger path；server-only progression + shallow S2C mirror。
  - Add codecs + loader `data/chestcavity/guscript/flows/*.json`；logs + validation。
- Phase 2 (FX emitters) — `feature/guscript-fx-modules`
  - Implement `FxRegistry` + built-ins；client register on startup；S2C `FxEventPayload`；basic `FxContext` from performer/target。
  - Provide example fx json under `data/chestcavity/guscript/fx/*.json`。
- Phase 3 (integration)
  - Allow GuScript operator nodes to reference `flow_id` + `fx_bundles`；when a root executes, install a flow instance instead of immediate fire (if present)。
  - Backwards compatible: no `flow_id` → current immediate actions。
- Phase 4 (migration)
  - Migrate selected Guzhenren abilities to flow+fx（e.g., charge-up bow, burst, lingering trail）；remove ad hoc timers from scattered listeners。

Acceptance criteria
- A demo flow (charge-hold-release) works across server+client；release emits projectile and plays FX；cancel returns to idle；cooldown enforced。
- Flow-json hot-reload via reload listeners；invalid flows are rejected with clear logs。
- FX emitters render client-only without server classloading; no CNFE; configurable intensity/color via json。

Risks & mitigations
- Desync: include `gameTime` or monotonic tick in sync payloads; clamp client visuals to server state。
- Performance: cap max concurrent flow instances per entity; pool FX spawners；expose config。
- UX: add HUD hints for charge progress later; not in MVP。

## Keybind multi-page dispatch (new)

Goal
- When multiple notebook pages are set to Keybind mode, a single key press should trigger all eligible pages, not only the active page.

Design outline
- Server-side on `GuScriptTriggerPayload`: iterate all `GuScriptAttachment.pages()` whose `bindingTarget==KEYBIND`, compile (or use cache) and execute their roots (respect per-page order, then per-root order if cross-root stacking is enabled).
- Safeguards: per-trigger caps on total pages and roots executed; aggregate logs: `pages=X, roots=Y`.

Branch
- `feature/guscript-keybind-aggregate`

Acceptance
- Pressing the key once triggers every Keybind page; logs enumerate all pages and composite roots executed; performance caps prevent runaway execution.

Status
- Completed (keybind trigger aggregates all keybind pages per press with page/root limits and logging in place).

## Cross-root Damage Stacking (ordered, per-trigger)

Branch
- `feature/guscript-session-stacking`

Goal
- Support ordered stacking of damage modifiers across multiple roots within a single trigger dispatch: earlier roots may export modifiers that seed later roots’ contexts.

Design
- ExecutionSession (server-only): cumulativeMultiplier, cumulativeFlat, with caps.
- Ordering: optional `order` (int) field on composite roots (via rule result JSON). Sort asc; stable tiebreaker by rule/name.
- Export semantics:
  - Actions: `export.modifier.multiplier {amount}` and `export.modifier.flat {amount}` write directly into the session.
  - Optional node flags: `export_modifiers: {"multiplier": true, "flat": false}` to export net deltas from the root after execution.

Implementation steps
1) `GuScriptExecutor.trigger`: build session; sort roots by `order`; per root: seed context with session → execute → collect exported → clamp+accumulate.
2) `DefaultGuScriptContext`: track added multipliers/flats and expose exported values or support explicit export marks.
3) `ActionRegistry`: register `export.modifier.multiplier` and `export.modifier.flat`.
4) Rule loader: parse `order` and `export_modifiers` (optional) from result JSON.
5) Add INFO logs for ordering and exported values per root; add caps in config.

Acceptance
- Given roots with orders 0/1/2, exports from earlier roots deterministically affect later roots’ final damage; unit tests cover 2-root and 3-root cases; caps prevent runaway.

Status
- Completed (session-scoped modifier exports with ordered execution, config caps, and 2-root/3-root unit coverage).


### P1: Preserve compile order for unordered roots

Problem
- The current ordered execution comparator sorts by `order` then `ruleId`/`name`. For roots without explicit `order` (treated as `Integer.MAX_VALUE`), this reorders them alphabetically, changing legacy behaviour where unordered roots executed in their compilation order. Scripts relying on prior implicit ordering now get different stacking without opting in.

Plan
- Keep legacy ordering for unordered ties by making the sort stable and returning 0 when comparing two unordered roots, so their original list order is preserved. Only apply secondary tie‑breakers (`ruleId`, `name`, `originalIndex`) when at least one root has an explicit `order` or both share an explicit equal `order`.

Code touchpoint
- File: `ChestCavityForge/src/main/java/net/tigereye/chestcavity/guscript/runtime/exec/GuScriptExecutor.java`, method `sortRootsForSession`.
- Change comparator from unconditional tie‑breakers to a conditional one:
  - If both `order == Integer.MAX_VALUE` (both unordered), comparator should return 0 to preserve input order (stable sort keeps `originalIndex`).
  - Else fall through to tie‑break on `ruleId`, then `name`, then `originalIndex`.

Branch
- `hotfix/guscript-session-stacking-order-compat`

Acceptance
- Unordered roots retain compilation order; adding a single ordered root only repositions that root relative to others. Legacy scripts keep prior stacking unless they opt in via `order`.


- **UI spacing upgrade**
  - Need responsive layout for GuScript screen controls. Replace hard-coded pixel offsets with proportional spacing (e.g., derived from slot size / GUI width) and enforce minimum gutter so buttons don’t overlap inventory rows on varied resolutions.
  - Deliver configurable constants (JSON or code) so downstream adjustments don’t require recompilation; document any new properties.
- **UI tweak request (2025-10-XX)**
  - Adjust GuScript page navigation buttons to shift 10 px further left.
  - Move trigger/listener buttons 10 px downward relative to current position.
  - Keep alignment responsive after offsets; update config defaults or layout math accordingly.
- **Branching guideline**
  - When dispatching multiple tasks to web Codex, create dedicated git branches per task (e.g., `feature/guscript-multi-trigger-fix`, `feature/guscript-ui-spacing`) and ensure PRs remain isolated. Merge sequencing and rebases must be coordinated by the user relay.

## Staying aligned with the goal
- Focus on stability and feature parity on NeoForge 1.21.1.
- If you get blocked, record what you tried and why it failed before yielding control.
- Before finishing, outline recommended next steps (tests, investigations, refactors) tied to maintenance or new features.

## Post-migration maintenance
- Keep `docs/migration-neoforge.md` as a historical reference; add notes only for backports or new breaking changes.
- Prefer official mappings and keep Gradle/NeoForge versions current within the 1.21.x line.
- Validate networking payloads and data-driven content with headless tests where possible.

## Notes
- Use `rg` for code search; avoid destructive commands unless explicitly required.
- Any tool installation or environment-variable changes must be expressed via Nix (`flake.nix` devShell updates).
- Scoreboard upgrade definitions live in `ChestCavityForge/src/main/java/net/tigereye/chestcavity/registration/CCScoreboardUpgrades.java`; `ScoreboardUpgradeManager` only orchestrates application logic.
- Shared NBT helpers are in `ChestCavityForge/src/main/java/net/tigereye/chestcavity/util/NBTWriter.java`; prefer these over manual `CustomData` plumbing.
- Guzhenren organ data lives in `ChestCavityForge/src/main/resources/data/chestcavity/organs/guzhenren/human/`; currently includes `guzhenren:gucaikongqiao` (空窍)。
- Guzhenren compat: use `net.tigereye.chestcavity.compat.guzhenren.GuzhenrenResourceBridge` for optional access to Guzhenren player resources (zhenyuan, etc.).
- Guzhenren slow tick: `GuzhenrenOrganHandlers` delegates to `WuHangOrganRegistry.register` for五行蛊;在此集中扩展行为。
- Mugangu now restores zhenyuan via `OrganSlowTickListener`; lacking the other四蛊时会先扣精力并按折扣回蓝，集齐后全额回复。
- Jinfeigu consumes hunger when the player is full to grant 60s absorption (stack scaled) if current absorption is below threshold.
- Shuishengu tracks per-stack water charge in item NBT; 在水中消耗真元充能，指数函数折算护盾值并播放水泡/破盾演出。
- Shared charge helpers: use `NBTCharge` in `util` for consistent read/write of `Charge` integers inside custom data.
- Listener reminder: 五行蛊等兼容器官要手动挂载需要的回调。只注册 `OrganSlowTickContext` 不会触发 `onHit`; 若器官依赖命中行为，记得同时添加 `OrganOnHitContext`，否则逻辑永远不会执行。

## 2025-09-24 Guzhenren bridge refresh
- `GuzhenrenResourceBridge` now reflects every field listed in `compat/guzhenren/ForDevelopers.md`; the private `PlayerField` enum matches the NeoForge `PlayerVariables` attachment, including the `dahen_*` typos present upstream.
- Added string-based helpers on `ResourceHandle` (`read`, `writeDouble`, `adjustDouble`, `clampToMax`) so callers can use either doc aliases or Chinese labels without depending on the enum directly.
- Ambiguous doc rows (运道/云道、金道/禁道) map to distinct constants (`*_CLOUD`, `*_FORBIDDEN`); the shared alias keeps resolving to the “primary” field, so use the new suffix names when you need the alternates.
- `最大念头` maps to the actual attachment key `niantou_zhida`; alias `niantou_zuida` remains recognised to preserve doc parity.
- New `compat/guzhenren/network/GuzhenrenNetworkBridge` wraps Guzhenren’s `player_variables_sync` payload on the client, dispatching snapshots through `GuzhenrenPayloadListener` and `NeoForge.EVENT_BUS` (`GuzhenrenPlayerVariablesSyncedEvent`). Hook installs once during mod construction when the compat mod is present.
- Kongqiao module now registers `DaoHenBehavior`, which listens for those payload events, logs any `daohen_*`/`dahen_*` changes with per-player caching, and seeds linkage increase channels. Registration happens via `GuzhenrenOrganHandlers`。
- 当客户端播种联动数据时，会通过 `kongqiao_daohen_seed` C2S payload 回传给服务器，保证 `/kongqiao_debug` 等指令在服务端看到的数值与客户端一致。
- Added verbose tracing in `DaoHenBehavior` and `GuzhenrenNetworkBridge` to log每一步的 early-return；若 Dao痕 日志缺失，可先搜索 `[compat/guzhenren]`。

### 2025-09-24 Update: 1s client polling
- Added a lightweight client tick poll in `DaoHenBehavior` using `ClientTickEvent.Post`, running every 20 ticks (~1s) to diff `daohen_*`/`dahen_*` values from `GuzhenrenResourceBridge` 并保持 linkage 播种为最新。
- Purpose: produce logs even when Guzhenren does not call `syncPlayerVariables` after server-side mutations (e.g., command paths). Polling reuses the same per-player snapshot cache and only logs on change (epsilon 1e-4).
- Scope: client side only; no network hooks changed. Payload-triggered logging remains intact and preferred when available。

### 2025-09-24 Update: Blood Bone Bomb binding
- `BloodBoneBombAbility` now registers against `CCOrganScores.DRAGON_BOMBS`, so the dedicated `[key.chestcavity.dragon_bombs]` binding triggers the charge.
- Client setup strips the id from `ATTACK_ABILITY_LIST`, preventing the generic attack hotkey from firing the combo while Guzhenren compat is active.
- Patched the `onEntityTick` handler to close correctly; Gradle compile confirms no structural issues remain.

### 2025-09-24 Update: Bone spear projectile entity
- Added `BoneGunProjectile` (`ThrowableItemProjectile`) to render the Guzhenren骨枪模型 in-flight; registered via `CCEntities` and `BloodBoneBombClient` (ThrownItemRenderer).
- `BloodBoneBombAbility` now spawns the entity instead of the manual `ProjectileState`, reusing linkage multipliers for damage + status effects.
- Server/client FX parity preserved: ignition remains in ability, trail/fade handled by the projectile itself; lifetime enforced at 60 ticks.

## 2025-09-26 Xue Dao attacker placeholder
- `XieyanguOrganBehavior#onHit` 现针对非玩家攻击者调用 `handleNonPlayerAttack` 占位逻辑，仅记录调试日志并回退原始伤害，为 Nudao 驱动的仆从扩展预留挂载点。
- 占位逻辑只在服务端运行，未来接入时可在此处读取 `GuzhenrenNudaoBridge` 或仆从胸腔以放大伤害/附加特效。
- 同步增加 `onSlowTick` INFO 日志：无论是否为玩家都会输出进入/退出原因，方便确认胸腔是否开启、物品是否识别为血眼蛊以及为何跳出当前 tick。

## 2025-09-27 Resource cost normalisation
- Added `GuzhenrenResourceCostHelper` to centralise zhenyuan/jingli consumption and convert to health only when an entity lacks Guzhenren attachments.
- `TuDaoNonPlayerHandler` now delegates to the helper, and Tiger Bone (`HuGuguOrganBehavior`) reuses the shared path so real players pay resources instead of raw HP.

### 2025-09-27 Xue Dao mob support
- Blood-eye Gu (`Xieyangu`) slow-tick now executes on non-players server-side, applying the periodic health drain scaled by linkage.
- Blood-lung Gu (`XieFeigu`) slow-tick supports non-player entities: movement/attack speed modifiers and oxygen support now apply to any `LivingEntity` (no hunger penalties for mobs).
- Blood-drop Gu (`Xiedigu`) slow-tick supports non-player entities: blood drop generation drains mob HP and spawns particles; weakness effect toggles apply to any `LivingEntity`.
- Iron-blood Gu (`Tiexuegu`) slow-tick supports non-player entities: health drain and INCREASE effect stacking now work for mobs; zhenyuan/jingli recovery remains player-only.

### 2025-09-27 Trigger Model (Active skills via damage)
- Principle: Active skills auto-fire only for non-player entities when they are damaged, with a 1/10 chance. Players retain manual activation only.
- Xiedigu (blood-drop): OnIncomingDamage → if victim is non-player and organ present, 10% chance to trigger detonation; requires capacity>0 (checked inside activation), consumes stored drops only; no resource/HP fallback for mobs.
- XieFeigu (blood-lung): OnIncomingDamage → if victim is non-player, 10% chance to trigger poison fog; only checks cooldown; applies blindness/poison/DoT via scheduled pulses; no resource deductions for mobs.
- Not in scope: Xieyangu (on-hit/slow-tick) and Tiexuegu (defense/INCREASE) do not implement the 1/10 auto-fire rule.

## Code Structure (NeoForge 1.21.1)

- Modules
  - `ChestCavityForge` — primary mod sources and resources
  - `neoforged` — NeoForge 1.21.1 API/event references used during migration
  - Aux: `debug`, `decompile`, `.gradle-home`, `.vscode`

- Core Entry
  - `ChestCavityForge/src/main/java/net/tigereye/chestcavity/ChestCavity.java`

- Packages
  - `ChestCavityForge/src/main/java/net/tigereye/chestcavity/chestcavities`
  - `ChestCavityForge/src/main/java/net/tigereye/chestcavity/config`
  - `ChestCavityForge/src/main/java/net/tigereye/chestcavity/interfaces`
  - `ChestCavityForge/src/main/java/net/tigereye/chestcavity/items`
  - `ChestCavityForge/src/main/java/net/tigereye/chestcavity/listeners`
  - `ChestCavityForge/src/main/java/net/tigereye/chestcavity/mob_effect`
  - `ChestCavityForge/src/main/java/net/tigereye/chestcavity/network`
  - `ChestCavityForge/src/main/java/net/tigereye/chestcavity/network/packets`
  - `ChestCavityForge/src/main/java/net/tigereye/chestcavity/recipes`
  - `ChestCavityForge/src/main/java/net/tigereye/chestcavity/recipes/json`
  - `ChestCavityForge/src/main/java/net/tigereye/chestcavity/registration`
  - `ChestCavityForge/src/main/java/net/tigereye/chestcavity/ui`
  - `ChestCavityForge/src/main/java/net/tigereye/chestcavity/util`

- Representative Classes
  - Chest cavities: `ChestCavityForge/src/main/java/net/tigereye/chestcavity/chestcavities/ChestCavityInventory.java`, `.../ChestCavityType.java`, `.../instance/ChestCavityInstance.java`, `.../organs/OrganManager.java`, `.../types/*`
  - Config: `ChestCavityForge/src/main/java/net/tigereye/chestcavity/config/CCConfig.java`, `.../CCModMenuIntegration.java`
  - Interfaces: `ChestCavityForge/src/main/java/net/tigereye/chestcavity/interfaces/ChestCavityEntity.java`, `.../CCStatusEffect.java`
  - Items: `ChestCavityForge/src/main/java/net/tigereye/chestcavity/items/ChestOpener.java`, `.../CreeperAppendix.java`, `.../VenomGland.java`
  - Listeners: `ChestCavityForge/src/main/java/net/tigereye/chestcavity/listeners/LootEvents.java`, `.../Organ*Listeners.java`
  - Effects: `ChestCavityForge/src/main/java/net/tigereye/chestcavity/mob_effect/OrganRejection.java`, `.../Ruminating.java`, `.../FurnacePower.java`
  - Networking: `ChestCavityForge/src/main/java/net/tigereye/chestcavity/network/NetworkHandler.java`, `.../ServerEvents.java`, `.../network/packets/*Payload.java`
  - Registration: `ChestCavityForge/src/main/java/net/tigereye/chestcavity/registration/CCItems.java`, `.../CCRecipes.java`, `.../CCStatusEffects.java`, `.../CCNetworkingPackets.java`
  - UI: `ChestCavityForge/src/main/java/net/tigereye/chestcavity/ui/ChestCavityScreen.java`, `.../ChestCavityScreenHandler.java`
  - Utils: `ChestCavityForge/src/main/java/net/tigereye/chestcavity/util/ChestCavityUtil.java`, `.../NetworkUtil.java`, `.../ScoreboardUpgradeManager.java`

- Resources
  - Assets: `ChestCavityForge/src/main/resources/assets/chestcavity`
  - Datapacks: `ChestCavityForge/src/main/resources/data/chestcavity`, `.../data/minecraft`, `.../data/nourish`, `.../data/antropophagy`, `.../data/c`
  - Meta: `ChestCavityForge/src/main/resources/META-INF`

- NeoForge References
  - `neoforged/neoforged/neoforge/client/event/ClientTickEvent.java`
  - `neoforged/neoforged/neoforge/network/event/RegisterPayloadHandlersEvent.java`
  - `neoforged/neoforged/neoforge/network/registration/PayloadRegistrar.java`

## 2025-09-26 NeoForge event registration cleanup
- Removed deprecated `@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)` usages.
- Mod-lifecycle events are now registered via the mod constructor (`ChestCavity(IEventBus bus)`):
  - Added `bus.addListener(JiandaoEntityAttributes::onAttributeCreation)`.
  - Converted `JiandaoEntityAttributes` to a plain holder with a static listener method (no `@SubscribeEvent`).
- Runtime events continue to use `@SubscribeEvent` with `@EventBusSubscriber(modid = ChestCavity.MODID)` (and `value = Dist.CLIENT` for client-only), without any `bus` parameter.
- Goal: silence deprecation warnings and conform to NeoForge 1.21.1 best practices.

## 2025-09-26 Jian Dao skin URL fix
- Fixed crash when JianYingGu captured player skin URLs starting with `http://`.
- `PlayerSkinUtil.urlToResource` now:
  - Accepts both `http` and `https` Mojang texture URLs.
  - Reduces to the last path segment (the hex hash), strips query/fragment.
  - Falls back to `sha1(url)` if the segment contains invalid characters.
  - Always builds a valid `ResourceLocation` like `guzhenren:skins/<hash>`.
- Follow-up: if we want to render true custom skins, add a client-only dynamic texture loader bound to those keys; otherwise clone uses default/missing texture gracefully.

## 2025-09-26 JianYingGu true-damage recursion fix
- Crash: StackOverflowError caused by `applyTrueDamage` calling `target.hurt(...)`, which fires `LivingIncomingDamageEvent` and re-enters `JianYingGuOrganBehavior.onHit`, looping indefinitely.
- Fix: add a thread-local reentry guard in `JianYingGuOrganBehavior`.
  - Early-return in `onHit` if `REENTRY_GUARD` is true.
  - Wrap `applyTrueDamage` internals in `try { REENTRY_GUARD.set(true); ... } finally { REENTRY_GUARD.remove(); }`.
- Rationale: server events run on a single thread; thread-local guard is sufficient to prevent self-recursion while not blocking other attacks processed later in the tick.
- Added detailed ability logging in `activateAbility` to trace cooldown/resource failures and successful clone spawns; useful when `/cc ability` triggers appear to do nothing.
- Cooldown handling now respects multiple JianYingGu organs: `CLONE_COOLDOWN_TICKS` tracked via per-player queues sized to the current organ count. Multiple organs can fire sequential casts without being blocked by a single timestamp, and cooldown logs show queue usage.
- JianYingGu skins: until a dynamic downloader is implemented, server snapshots stop assigning custom `guzhenren:skins/<hash>` textures (which caused missing-resource warnings). Entities still broadcast tint + skin URL for future use; rendering falls back to the vanilla default skin.
- JianYingGu passive strike now respects a 10% trigger chance before paying zhenyuan, so ordinary攻击不会每次都额外触发伴随剑影。
- 影随步残影改用主动技的默认渲染：不再抓取玩家皮肤，而是直接生成带固定色调的 `SingleSwordProjectile`（避免缺失纹理，方便观察打击特效）。
- 影随步触发时额外生成短命 `SwordShadowClone`（1s 寿命、固定偏暗色），配合剑气实体让“残影”在客户端可见。
- 血眼蛊近战命中时调用 `CommonHooks.fireCriticalHit`：对外广播强制暴击并沿用事件返回的倍率，使其他监听者/系统（如影随步）能正确识别暴击。
- JianYingGu 增设 `markExternalCrit`：血眼蛊触发后记录玩家在当期 tick 内的暴击状态，`isCritical` 会消费该标记，从而同步触发影随步等依赖暴击的行为。


## Planned: Generic Entity Strike Action
- Problem: GuScript lacks a reusable action to make spawned/allied entities perform a directed melee strike.
- Solution sketch: add `FlowActions.entityStrike` (and companion JSON wiring) that:
  1. Resolves a source entity (performer pet, entity id variable, or freshly spawned id).
  2. Applies a relative offset/rotation based on performer yaw to position entity before striking.
  3. Sets target = performer (or flow-provided `target`), triggers `swing`/`doHurtTarget`, and optionally teleports for dash distance.
  4. Plays configurable sound (`minecraft:entity.polar_bear.attack` as default) via existing FX sound module or a direct server call.
- Implementation needs: new Flow action + helpers in `FlowActions`, `GuScriptFlowLoader` parser, and tests verifying targeting/alignment.
- Output: re-usable for tiger strike, summoned clones, or Jian Dao companions.

## Planned: Tiger Ambush Flow Using Entity Strike Action
- Once `entityStrike` exists, create `test/tiger_ambush_slash` flow:
  - `enter_actions`: spawn `guzhenren:hu` at performer back/above (relative offset), optionally disable AI.
  - Use `entityStrike` to align tiger toward performer facing direction, dash to 4 blocks behind → charge through player, apply damage + knockback.
  - Trigger sound `minecraft:entity.polar_bear.attack` and optional particles during strike.
- Testing: `/guscript flow start chestcavity:test/tiger_ambush_slash` should produce visible tiger lunge + audio; confirm it respects different player yaw/pitch.

### Pending Soul Beast follow-up
- Consolidate the pending Soul Beast tasks (state sync, resource costs, command hooks) once the conversion path is in place.
- Emit a dedicated hook when entities enter/exit the soul beast state (from `SoulBeastStateManager#setActive/#setEnabled/#setPermanent`) so other systems can initialise auras、HUD 等：
  - 添加 `SoulBeastStateChangedEvent`（包含实体、旧状态、目标状态）。
  - 在上述 setter 中比较 active/permanent/enabled 变化并发布事件；服务端同步后客户端可监听。
  - 更新现有魂兽模块（DoT、指令、Intimidation 等）按需监听该事件。
- Shared intimidation utility now lives at `compat/guzhenren/util/IntimidationHelper`; wire attituded threshold checks through it when implementing威慑效果。
- Parallel plan: 实现大魂蛊（itemID `item.guzhenren.dahungu` “大魂蛊”。心脏）行为——每秒恢复 2 点魂魄与 1 点念头；若胸腔内存在小魂蛊且角色非魂兽，按 `HunDaoOrganRegistry` 列表统计魂道蛊数量为魂魄恢复效率提供 1%/只（上限 20%）的「魂意」增益；若角色为魂兽，赋予「威灵」让攻击魂魄消耗减少 10 点并调用 `IntimidationHelper` 威慑当前生命值低于玩家魂魄值的敌对实体；
{
  "itemID": "guzhenren:dahungu",
  "organScores": [
    { "id": "guzhenren:zuida_hunpo", "value": "50" },
    {"id":"chestcavity:health","value": "2"}
  ]
}
- Parallel plan: `hun_dao/hun_shou_hua` 一次性主动技 flow + FX
  - Flow 阶段：`prepare -> channel (periodic cost) -> completion/transform -> cooldown/fail`；在 `channel` 状态每秒扣 200 真元（`GuzhenrenResourceCostHelper.consumeStrict`）+ 1 生命值（`GuzhenrenResourceCostHelper.drainHealth`）。持续10s，若没有就跳转失败
  - 首次成功后写入玩家附件标记（GuScript action），后续触发直接跳转失败分支并播放 `fail_toSoulBeast` 音效/FX。
  - 成功时调用 `SoulBeastAPI.toSoulBeast(player, true, source)`（永久魂兽化）并依赖 `SoulBeastStateChangedEvent` 补满魂魄、启动威慑（`IntimidationHelper` 每秒检查敌对实体当前生命值 < 玩家魂魄值）。
  - GuScript 扩展：
    1. `predicate.hun_shou_hua.is_used` / `action.hun_shou_hua.mark_used`
    2. `action.soulbeast.transform`（封装 `SoulBeastAPI.toSoulBeast` + 事件）
    3. `action.consume_resources_combo`（按周期扣 200 真元 + 1 HP，失败跳转）
    4. `action.emit_fail_fx`（播放 `fail_toSoulBeast` 声音与特效）
  - FX 资源：新增蓝/黑魂焰 FX（用原版粒子效果实现）并在客户端注册；失败分支共用 `fail_toSoulBeast` 声音(ChestCavityForge/src/main/resources/assets/chestcavity/sounds/custom/soulbeast/fail_soulbeast_transform.ogg) 与特效。
- Parallel plan: 鬼气蛊（itemID `guzhenren.guiqigu` “鬼气蛊”）
  - 被动：
    1. 每秒恢复 3 点魂魄、1 点精力（复用 `GuzhenrenResourceCostHelper`）。
    2. 普通攻击附加真伤 = 当前魂魄上限的 1%（magic 类型）。
    3. 魂兽态获得「噬魂」：击杀生命值 > 40 的生物 → 12% 概率提升魂魄上限 0.1% × 被击杀生物最大生命，同时扣 5% 魂魄稳定度。
  - 主动技 “鬼雾”：施放后在目标前方生成黑雾粒子，让敌人陷入缓慢 IV + 失明，持续时间待定（flow + FX 实现）。
  - 数据：
    ```json
    {
      "itemID": "guzhenren:guiqigu",
      "organScores": [
        { "id": "guzhenren:zuida_hunpo", "value": "35" },
        { "id": "chestcavity:health", "value": "1" }
      ]
    }
    ```
  - 任务：新增被动监听（攻击/击杀 Hook）、稳定度扣除逻辑、魂兽态判断、主动技 flow + 黑雾 FX 与音效。
- Parallel plan: ChestCavityForge/src/main/java/net/tigereye/chestcavity/compat/guzhenren/item/li_dao 三转全力以赴蛊（itemID `guzhenren.quan_li_yi_fu_gu`, 心脏）
  - 被动：每 15s 扣 500 真元并恢复 `20 * (1 + 力道INCREASE)` 精力；根据胸腔肌肉器官数量提升恢复速度：每个肌肉格提供 `0.5 * (1 + 力道INCREASE)`，总上限 `15 + (1 + 力道INCREASE)`，不可叠加。
  - 实现：
    * 周期计时（SlowTick 或定时器）使用 `GuzhenrenResourceCostHelper.consumeStrict` 扣真元。
    * 统计胸腔内 `strength` 标记的器官数量（JSON 判定字段），按公式计算额外恢复量并通过 `GuzhenrenResourceBridge` 调整精力。
    * 防止叠加：记录唯一状态或在装配时互斥。
  - 数据：
    ```json
    {
      "itemID": "guzhenren:quan_li_yi_fu_gu",
      "organScores": [
        {"id":"chestcavity:defense","value":"10"},
        {"id":"chestcavity:nerves","value":"1"},
        {"id":"chestcavity:strength","value":"32"}
      ]
    }
    ```
- Parallel plan: 三转自力更生蛊（itemID `guzhenren.zi_li_geng_sheng_gu_3`, 肾脏）
  - 被动：每 10s 扣 500 真元恢复 `30 * (1 + 力道INCREASE)` 生命。
  - 主动技（ATTACK_ABILITY）：消耗胸腔内的肌肉器官（匹配 `chestcavity:*muscle`），获得 30 秒生命回复 `1 * (1 + 力道INCREASE)`，结束后施加 `虚弱` 持续 `30 / (1 + 力道INCREASE)` 秒；每消耗 1 个肌肉播放进食音效。不可叠加。
  - 实现要点：
    * 周期被动同上复用 Helper。
    * 主动技 flow：
      1. 检查/扣除肌肉器官（按消耗数量控制效果强度）。
      2. 启动持续恢复效果（计时器或自定义 MobEffect），在结束时施加虚弱。
      3. 播放音效和 FX；保证仅限单实例。
  - 数据：
    ```json
    {
      "itemID": "guzhenren:zi_li_geng_sheng_gu_3",
      "organScores": [
        {"id":"chestcavity:filtration","value":"1"},
        {"id":"chestcavity:nerves","value":"1"},
        {"id":"chestcavity:strength","value":"32"}
      ]
    }
    ```
- Parallel plan: 火人蛊（itemID `guzhenren:huorengu`，心脏）
  - 被动：每秒恢复生命上限 0.5%、恢复 4 点精力，提供火焰抗性与喷气式飞行（长按空格持续上升），胸腔内仅允许一个实例。
  - 粒子/音效建议：
    * 粒子：以 `minecraft:flame`、`small_flame` 为主，必要时混合少量 `campfire_cosy_smoke` 增加层次；联动状态可额外使用 `lava` 或 `soul_fire_flame` 点缀。
    * 音效：飞行/喷射触发 `minecraft:entity.blaze.shoot`，持续阶段低音量播放 `entity.blaze.burn` 或 `block.fire.ambient`。
  - 实现建议：SlowTick 扣真元 / 调整生命与精力；为玩家启用自定义飞行推进（与原生飞行能力兼容），卸载时清理；使用附件或状态防止多例。
  - 数据：
    ```json
    {
      "itemID": "guzhenren:huorengu",
      "organScores": [
        {"id":"chestcavity:fire_resistant","value":"2"},
        {"id":"chestcavity:health","value":"4"}
      ]
    }
    ```
- 联动计划：火心孕灵（火心蛊 + 火人蛊）
  - 炎道效率 +26%；赋予「火灵」（急迫 I + 普攻附加 10 分钟火焰，播放持续炽烈特效）。
  - 粒子/音效：围绕角色持续生成 `flame` + `lava` 组合，并保持 `entity.blaze.burn` 低音频；攻击触发额外 `blaze.shoot`。
  - 需实现专属火焰粒子 FX 与音效；检测两蛊共存时启用，缺失任一器官即撤销。
- 通用工具：新增 `compat/guzhenren/item/li_dao` 包的 `LiDaoConstants`、`LiDaoHelper`、`AbstractLiDaoOrganBehavior`，封装力道增益访问、肌肉统计/判定逻辑，后续三转蛊实现统一复用。
- Parallel plan: 体魄蛊（itemID `guzhenren.tipogu`，心脏）
  - 被动：每秒恢复 3 点魂魄、1 点精力。
  - 魂兽态：普通攻击额外造成 `当前魂魄上限 * (1 + 魂道INCREASE)` 真伤（按百分比换算）并消耗 0.1% 魂魄。
  - 非魂兽态：获得「滋魂哺身」（不可叠加）——魂道 INCREASE EFFECT +10%，每 10s 根据魂魄上限 `(0.5 * (1 + 魂道INCREASE))%` 刷新一次吸收护盾。
  - 实现要点：
    * 周期恢复使用 SlowTick；检测魂兽状态，以不同模式执行。
    * 攻击监听：在 `OrganOnHitListener` 中加入额外真伤 & 魂魄消耗；注意魂魄不足时的处理与日志。
    * 护盾刷新：使用定时任务或变量记录上次刷新 tick，并调用 `LivingEntity#setAbsorptionAmount`。
  - 数据：
    ```json
    {
      "itemID": "guzhenren:tipogu",
      "organScores": [
        {"id":"guzhenren:zuida_hunpo","value":"77"},
        {"id":"chestcavity:strength","value":"32"}
      ]
    }
    ```
- 数据提醒：火人蛊、体魄蛊实现后需同步更新 `src/main/resources/data/chestcavity/organs/guzhenren/human` 下的 JSON（新增或调整对应器官描述）。
- Follow-up: 威慑 → 敌对生物主动远离玩家
  - 新增自定义 `MobEffect`（例如 `SoulBeastIntimidatedEffect`）或 Goal：当实体处于该效果时，动态插入 `AvoidEntityGoal<Player>`/`RetreatGoal`，令其持续远离威慑来源。
  - `IntimidationHelper.applyIntimidation` 改为施加此自定义效果，并记录施法者 UUID（用于 Goal 判断 flee 目标）。效果结束/实体移除时清理 Goal。
  - 覆盖玩家/非玩家：非玩家也要响应 flee；注意线程安全（仅在主线程添加/移除 Goal）。
  - 测试验证：被威慑的敌对生物会短暂停止追击并远离；效果叠加刷新时间，结束后恢复原有 AI。

## 2025-10-?? 灵魂系统目录框架（进行中）
- 新增包路径 `net.tigereye.chestcavity.soul`，已预创建子目录：`api/`、`adapter/`、`container/`、`engine/`、`profile/`、`command/`、`fakeplayer/`、`ai/`、`network/`、`storage/`。
- 目标：承载多魂容器（SoulContainer）、魂档案（SoulProfile）、适配器（SoulAdapter）、切换引擎（SoulSwitchEngine）、伪玩家/AI 控制及网络同步逻辑。
- 下一步：按照换魂计划分配接口与实现骨架（先定义 `SoulContainerCapability`、`SoulProfileSnapshot`、`SoulAdapter` 接口），随后补充命令 `/soul` 与 FakePlayer 行为。
- TODO：在实现前明确各模块之间的能力接口与事件广播，安排数据持久化与序列化格式（建议定义 `storage/` 子包用于 NBT 编解码器）。
- 指令入口：注册 `/soul test spawnFakePlayer` 测试子命令，现已通过 `SoulFakePlayerSpawner` 使用 `FakePlayerFactory` 在执行者所在位置生成伪玩家（若生成失败会反馈原因）；后续需完善权限校验、重复实例管理与分魂绑定逻辑。
- 自定义 `SoulPlayer`（派生自 `FakePlayer`）取代原工厂对象：允许受伤/死亡、启用重力物理并预留能力/AI 钩子；生成时与指挥者坐标对齐并调用待定的 Cap attach TODO。
- `SoulPlayer` 现在在构造时套用生存模式能力（禁飞、禁创、可受伤），并调用 `setGameMode(GameType.SURVIVAL)` 与 `onUpdateAbilities()`；后续可在该类中扩展 AI/能力挂载。
- `SoulPlayer.tick()` 现在强制 `travel(Vec3.ZERO)`，配合关闭飞行能力确保服务器端持续应用重力；若后续需要自定义输入，可在此替换为实际移动向量。
- SoulPlayer 自行处理受伤：覆盖 `isInvulnerable()` 与 `hurt()`，手动结算吸收值与生命值、记录战斗日志并触发死亡，这样命中会真正扣血而不再只播动画。
- 新增 `/soul enable` 指令：执行时调用 `SoulFeatureToggle.enable`，并向玩家发出“会破坏旧存档”的警告；其余功能默认保持关闭，仅此命令才允许后续 NBT 转化。
- 初始化 `SoulContainer` (Capability 数据结构) 与 `SoulProfile`：支持多魂 ID → Profile 映射、当前激活魂记录，并以 `InventorySnapshot` 捕捉/恢复 36+4+1 槽位的背包、保存/读取 NBT 时通过 registry provider 解析物品。
- `InventorySnapshot` 现提供 capture/restore/save/load，序列化使用 `ItemStack.save(provider, tag)` 与 `parseOptional(provider, tag)`，为后续器官/能力快照预留接口。
- `/soul test SoulPlayerList` 与 `/soul test SoulPlayerSwitch <UUID>`：前者枚举当前存活的 SoulPlayer（含是否为当前选中、可选 owner UUID）；后者在 `/soul enable` 后允许执行者切换活跃 SoulPlayer（仅限拥有者）。
- `SoulFakePlayerSpawner` 记录 activeSoulPlayerId，提供 `listActive()`/`switchTo()`；移除时自动清理。
- SoulFakePlayerSpawner 现维护 SoulPlayer ↔ Visual 映射、玩家活跃映射；生成时先广播 PlayerInfo 再入场，列表显示 soul/visual UUID 并提供 UUID 补全；支持按视觉 UUID 解析。移除/登出/关服时清理映射并发送移除包。
- `/soul test SoulPlayerList` 与 `SoulPlayerSwitch` 更新：使用 `UuidArgument` 并提供建议，switch 会校验 owner 权限并实际更新活跃 SoulPlayer。
- 新增 `SoulFakePlayerEvents` 监听玩家登出与服务器关闭，调用 Spawner 清理，避免换档/重启残留导致崩溃或幽灵实体。
- `PlayerStatsSnapshot` 纳入 soul profile：捕获/恢复 XP、生命/吸收、饱食度/饱和/疲劳与同步性属性；切换 profile 时主玩家与 SoulPlayer 均同步基础属性，避免数据错乱。

## 2025-10-?? Web Codex 适配：SoulProfile 快照结构与扩展计划

- 目标
  - 统一“魂档（SoulProfile）”跨模块快照/恢复接口，便于 Web Codex 追加能力/器官/外部模组数据，而不影响已存在的背包/原版属性。

- 快照树（当前/预留）
  - 所有 `SoulProfile`
    - 物品存储（`InventorySnapshot`）
    - 原版玩家属性（`PlayerStatsSnapshot`：XP/饥饿/生命/吸收/基础属性）
    - 能力存储（`CapabilitySnapshot`）— 预留接口
    - 器官信息（`ChestCavitySnapshot`）— 预留接口（建议直接复用 ChestCavityInstance 的 `toTag/fromTag`）
    - 蛊真人数据（`GuzhenrenSnapshot`）— 预留接口（通过 `GuzhenrenResourceBridge` 读写）
    - 其他模组附着数据（注册式扩展）— 预留接口（建议 `SnapshotAdapterRegistry`）

- 适配策略
  - 定义 `SnapshotAdapter` SPI：supports/capture/restore/save/load 五件套。
  - 在 `SoulProfile` 内维护 `List<SnapshotAdapter>`，加载时按注册顺序 load，保存时顺序 save 合并到复合 NBT；与既有 `inventory/stats/position` 并行。
  - ChestCavity 适配：从 `CCAttachments.getChestCavity(living)` 取实例，`toTag`/`fromTag` 落 NBT；谨慎处理版本与缺省空值。
  - Guzhenren 适配：通过 `GuzhenrenResourceBridge.open(player)`，将核心字段写入 `GuzhenrenSnapshot`；必要时触发 `syncPlayerVariables` 以刷新客户端。

- 所有权与并发
  - `SoulFakePlayerSpawner` 只允许 Owner 操作其魂档；命令补全/移除均校验所有权。
  - 切换流程使用公共快照：
    - 切到 SoulPlayer：保存目标快照→移除目标实体→将玩家恢复为目标快照→在原 owner 位置重生一个 FakePlayer（使用 owner 快照）。
    - 切回 Owner：保存玩家（当前魂）快照→移除当前活跃魂实体→重生该魂 FakePlayer→恢复玩家为 owner 快照。

- 后续任务建议
  - 实现 `CapabilitySnapshot`/`ChestCavitySnapshot`/`GuzhenrenSnapshot` 的最小可用版本与注册中心。
  - 抽象 `/soul test saveAll` → `SnapshotPersistence.saveAll(server)`，可被世界保存钩子复用。
  - 为 FakePlayer 名称添加 `[Soul]` 前缀或 team 前缀，便于多人服区分。
## 2025-10-06 Soul System — 分魂击杀导致 Owner 覆盖：因/果/解决/避免

问题背景
- 现象：玩家在频繁切换/击杀分魂后，偶发 Owner 背包/状态被某个分魂覆盖；客户端偶发收到 “Ignoring player info update for unknown player … ([UPDATE_GAME_MODE]/[UPDATE_LATENCY])”。
- 影响：登录/后台快照后 Owner 档案不正确，分魂壳与 PlayerInfo 生命周期不一致，日志难以追踪。

根因（因）
- 在分魂死亡移除回调中，错误使用了“实体 UUID”而非“Profile UUID”（分魂档 ID）去保存和移除。
- 结果是 `saveSoulPlayerState(entityUuid)` 和 `handleRemoval(entityUuid)` 均未命中活动映射，导致：
  - 该分魂“未保存快照/未从 ACTIVE 表清除/未撤销 OWNER_ACTIVE_SOUL/未移除 ENTITY_TO_SOUL”；
  - 后续后台快照/切换依据“脏状态”做出错误判断，选择了错误读源，最终把分魂数据写回 Owner 档案。

后果（果）
- Owner 被覆盖：当系统误以为 Owner 未附身或读源本体时，会从 `ServerPlayer`（此刻实际代表某分魂）读取，覆盖 Owner 档。
- PlayerInfo 异常：外化壳已被移除但映射未清理，客户端收到针对未知 UUID 的 UPDATE 包，出现 IGNORE 日志。

修复（如何解决）
- 统一以“Profile UUID”处理分魂死亡/移除，提供实体→档案的容错 remap 并打印 WARN：
  - `ChestCavityForge/src/main/java/net/tigereye/chestcavity/soul/fakeplayer/SoulFakePlayerSpawner.java:716`
  - `ChestCavityForge/src/main/java/net/tigereye/chestcavity/soul/fakeplayer/SoulFakePlayerSpawner.java:736`
- `saveSoulPlayerState` 增加兜底：若误传实体 UUID，尝试通过 `ENTITY_TO_SOUL` 反解为 Profile UUID，并 WARN 一次后继续保存。
- `handleRemoval` 增强：若未命中活动表，尝试 remap；无论成功与否都输出清晰日志，保证 ACTIVE/ENTITY_TO_SOUL/OWNER_ACTIVE_SOUL 被一致清理。
- 写入围栏（Write Fence）：
  - 刷新 Owner 档案时，附身态只允许从“Owner 外化壳（shell）”读取；无壳时跳过并记录（不再从分魂态 ServerPlayer 覆盖 Owner）。
  - 背景快照仅刷新分魂 Profile，不触碰 Owner Profile；在线 owner 按分魂分组标脏并 `saveDirty(owner)`；离线 owner 直接落 `OfflineStore`。

预防（如何避免）
- 代码层面：
  - 任何保存/移除/刷新入口，不接受不明来源 UUID；若确需容错 remap，则必须记录 `entityUuid -> profileId` 的 WARN 日志。
  - Owner 写入路径只允许 SELF（未附身）或 SHELL（外化壳）读源，禁止从分魂态 ServerPlayer 刷新 Owner 档案。
  - 背景保存默认排除 Owner（不置脏/不刷新 Owner），仅对目标分魂生效。
- 流程层面：
  - 切换 `switchTo` 时序固定：先保存当前激活档→外化/传送→应用目标档→消费目标壳→更新激活指针。
  - 击杀/移除分魂时，必须走 `onSoulPlayerRemoved`/`handleRemoval`，不可直接 `discard()` 绕过协调器。

审计与定位（日志基线）
- Owner 刷新：`[soul] updateActiveProfile source=SELF|SHELL|SKIP` / `refreshSnapshot owner=SELF|SHELL|SKIP`（含原因）。
- 分魂刷新：`refreshSnapshot soul=OK|SKIP reason=...`。
- 背景快照：`background-snapshot savedSouls=.. ownersTouched=.. offlineSouls=..`。
- 容错 remap：`onSoulPlayerRemoved received entityUuid=.. remapped to profileId=..` / `handleRemoval received entityUuid=.. remapped to profileId=..`。

相关触点（便于跳转）
- 分魂移除/兜底：`soul/fakeplayer/SoulFakePlayerSpawner.java:716, 736`
- Owner 写入围栏：`soul/container/SoulContainer.java:118`、`soul/fakeplayer/SoulFakePlayerSpawner.java:444`
- 后台快照：`soul/fakeplayer/SoulFakePlayerSpawner.java:303`、事件：`soul/fakeplayer/SoulFakePlayerEvents.java:50`

测试建议
- 附身 A、击杀 B：期望看到 remap WARN 与 onSoulPlayerRemoved/handleRemoval 完整清理，后台快照不刷新 Owner。
- 频繁切换+后台保存：Owner 只在 SELF/SHELL 下刷新，且保存统计符合现场。

---

## 扩展能力存储（CapabilitySnapshot）接入教程（预留接口）

目标
- 为 SoulProfile 增加可扩展的“能力快照层”，用于保存与恢复：
  - 器官信息（ChestCavitySnapshot）(已实现)
  - 蛊真人数据（GuzhenrenSnapshot）
  - 其他模组附着数据（AttachmentSnapshot）
- 约束：独立数据不被覆盖；读写围栏明确；同步安全、可回放；可按需启用/禁用。

建议目录结构（预留）
```
src/main/java/net/tigereye/chestcavity/soul/profile/capability/
  CapabilitySnapshot.java            # 统一接口（capture/apply/save/load/isDirty/clearDirty）
  CapabilitySnapshotRegistry.java    # 统一注册表（ResourceLocation→工厂）
  CapabilityPipeline.java            # 统一编排（capture/apply/save/load 按顺序串行）

  chestcavity/ChestCavitySnapshot.java   # 器官信息快照（预留）
  guzhenren/GuzhenrenSnapshot.java       # 蛊真人快照（预留）
  attach/AttachmentSnapshot.java         # 其他附着数据（预留注册接口）
```

接口契约（草案）
```java
public interface CapabilitySnapshot {
  ResourceLocation id();

  // 从玩家/实体捕获当前状态（仅服务端）。
  CapabilitySnapshot capture(ServerPlayer player);

  // 将快照应用到玩家（服务端为准；客户端仅显示层可选实现）。
  void apply(ServerPlayer player);

  // NBT 持久化（与 SoulProfile.save/load 对齐）。
  CompoundTag save(HolderLookup.Provider provider);
  CapabilitySnapshot load(CompoundTag tag, HolderLookup.Provider provider);

  boolean isDirty();
  void clearDirty();
}
```

注册表（预留）
```java
public final class CapabilitySnapshotRegistry {
  private static final Map<ResourceLocation, Supplier<CapabilitySnapshot>> REG = new HashMap<>();

  public static void register(ResourceLocation id, Supplier<CapabilitySnapshot> factory) {
    REG.put(id, factory);
  }

  public static Collection<ResourceLocation> keys() { return REG.keySet(); }
  public static Optional<CapabilitySnapshot> create(ResourceLocation id) {
    Supplier<CapabilitySnapshot> f = REG.get(id);
    return f == null ? Optional.empty() : Optional.of(f.get());
  }
}
```

在 SoulProfile 中的挂载方式（思路）
- 为 `SoulProfile` 增加 `Map<ResourceLocation, CapabilitySnapshot> extraSnapshots`；
- `capture(player)`/`apply(player)` 前后由 `CapabilityPipeline` 统一遍历调用；
- `save/load` 将每个快照写入 `tag.put(id.toString(), snapshot.save(provider))`。

器官信息（ChestCavitySnapshot）预留要点
- 捕获：读取玩家胸腔库存、器官得分、Ledger 状态；不要在客户端捕获。
- 应用：先验证 Ledger，应用得分类增减后恢复内容物；避免二次触发监听（必要时加抑制标志）。
- 冲突：与当前物品栏/效果冲突时以“快照→目标”优先，但必须在 `switchTo` 的顺序中进行（参考 SoulProfileOps）。

蛊真人数据（GuzhenrenSnapshot）预留要点
- 捕获：通过 `GuzhenrenResourceBridge` 只在服务端读取 zhenyuan/jingli/境界等；可选记录上次同步戳。
- 应用：优先通过桥提供的 clamp/set API；避免直接操纵附件底层字段；
- 同步：客户端 HUD 由对方模组负责；本仓库不主动 C2S 修改，保持服务端权威。

其他模组附着数据（AttachmentSnapshot）预留注册接口
- 通过 `CapabilitySnapshotRegistry.register(mod:id, MySnapshot::new)` 挂入；
- 绝不在客户端 capture；应用时校验来源模组已加载；
- 建议为第三方定义“读写围栏 + NBT 版本号”，保证前后兼容与回放安全。

如何避免独立数据被覆盖（重点）
- 写入围栏（Write Fence）
  - Owner Profile：未附身用 SELF；已附身只允许 SHELL；无壳则 SKIP，不得退回 ServerPlayer（分魂态）。
  - 分魂 Profile：只允许其对应的 SoulPlayer 写入。
  - 背景保存：只刷新目标分魂；不要顺带刷新 Owner Profile。
- ID 严格区分
  - Profile UUID ≠ 实体 UUID：所有保存/移除/刷新入口都用 Profile UUID；如收到实体 UUID 必须 remap 并 WARN。
- 原子时序
  - 切换 switchTo：先保存当前激活档→外化/传送→应用目标档→消费目标壳→更新激活指针（已在协调器实现）。

如何安全同步数据（建议）
- 服务器权威：所有 capture/apply 都在服务端；客户端仅展示。
- PayLoad 注册：在 `RegisterPayloadHandlersEvent` 中注册自有快照同步（若需要 UI/HUD），避免与第三方冲突；尽量沿用第三方模组的官方同步点（如 Guzhenren 的 player_variables_sync）。
- 限流与幂等：快照级别同步需带版本/戳；同内容重复包直接丢弃。
- 断网/崩服：利用 `SoulOfflineStore` 在登出/停服前落地所有快照；登录时优先合并离线快照。

最小实现路线（落地顺序）
1) 定义 CapabilitySnapshot/Registry/Pipeline。
2) 在 SoulProfile 增加 `extraSnapshots` 与 save/load/exec 钩子。
3) 实现 ChestCavitySnapshot 与 GuzhenrenSnapshot 的空壳（只做 NBT 结构与开关）；
4) 为两者逐步补充 capture/apply 逻辑，先从只读指标开始（不写回），再扩展到完整恢复；
5) 提供一组单元测试：capture→save→load→apply 的往返一致性；切换/后台保存/登出流程的幂等与防覆盖验证。



---

## Guzhenren Ops Migration Plan (step-by-step)

### 2025-02-15 Implementation Record (web Codex)
- Migrated `XiediguOrganBehavior`, `XieyanguOrganBehavior`, and `ShiPiGuOrganBehavior` to the shared Ops stack:
  - Replaced direct `LinkageManager` usage with `LedgerOps` helpers and added context-safe channel lookups.
  - Ported organ timers to `MultiCooldown` (health drain cadence + Shi Pi Gu recharge) and removed bespoke NBT counters.
  - Standardised effect application via the extended `EffectOps.ensure/remove` helpers (blood weakness aura, etc.).
  - Swapped Shi Pi Gu’s absorption potion buff for `AbsorptionHelper` capacity management with a persistent modifier id.
- Extended `EffectOps`/`LedgerOps` utility surfaces to support the migration (new ensure/lookup overloads).
- Follow-up backlog:
  - Batch A remaining items (`XieFeigu`, `TuQiangGu`, `LiandaoGu`) still need conversion.
  - Re-run `./gradlew compileJava` once the Gradle cache contention on `decompile_*` resolves (current run aborted after a long lock wait).

Purpose
- Unify compat/guzhenren behaviours on shared Ops: `LedgerOps`, `ResourceOps`, `MultiCooldown`/`CooldownOps`, `EffectOps`, `AttributeOps`, `TargetingOps`, `TickOps`, and the global `AbsorptionHelper`.
- Eliminate manual `LinkageManager`/direct resource writes/bespoke timers/hand-rolled attribute and potion logic.

Phases
- Phase 0 — Inventory (once):
  - Search patterns: `LinkageManager`, `increase_effect`, `GuzhenrenResourceBridge.open`, `new MobEffectInstance`, `addEffect/removeEffect`, `getAttribute/new AttributeModifier`, `Timer|Cooldown|ticks|last|next` in behaviours.
  - Produce a short list per family: Blood(Xue), Bone(Gu), Earth(Tu), Wood(Mu), Water(Shui), Fire(Yan), Ice(Bing), Stone(Shi), Poison(Du), Sword(Jian), Soul(Hun), Kongqiao, etc.

- Phase 1 — Migration per bucket (repeat until done):
  1) LedgerOps (Increase Effects)
     - Implement `IncreaseEffectContributor` when the organ contributes to any `*_increase_effect` channel.
     - Replace direct `LinkageManager.getContext(cc).increaseEffects().set/remove` with:
       - `LedgerOps.set(cc, channelId, contributorId, value)` on equip/enable paths.
       - `LedgerOps.remove(cc, channelId, contributorId)` on remove/disable paths.
       - Call `LedgerOps.rebuildIncreaseEffects(cc)` after bulk toggles or when drift is detected.
     - Normative snippet:
       - On equip: `LedgerOps.set(cc, CHANNEL_ID, CONTRIBUTOR_ID, 0.10);`
       - On remove: `LedgerOps.remove(cc, CHANNEL_ID, CONTRIBUTOR_ID);`
       - On tick end: optionally `LedgerOps.verifyAndRebuildIfNeeded(cc)` when debugging ledger mismatches.

  2) ResourceOps (Zhenyuan/Jingli/Hunpo)
     - Replace `GuzhenrenResourceBridge.open(...).get()/write(...)` with `ResourceOps`:
       - Read: `ResourceOps.readDouble(player, key, fallback)`.
       - Adjust: `ResourceOps.adjustDouble(player, key, delta)` or `consumeStrict` helpers when no HP fallback is allowed.
       - Player vs. non-player: keep separate handler paths; for non-player fallback, centralise in `GuzhenrenResourceCostHelper` (if available) or add a util method next to the behavior.

  3) MultiCooldown/CooldownOps (Timers)
     - Replace ad-hoc NBT `Timer/Cooldown/last/next` with `MultiCooldown` entries keyed by `ResourceLocation`.
     - Normative snippet:
       - Define keys: `private static final ResourceLocation COOLDOWN_KEY = rl("modid:path");`
       - Read: `long next = MultiCooldown.entry(state, COOLDOWN_KEY).getReadyTick();`
       - Arm: `MultiCooldown.entry(state, COOLDOWN_KEY).arm(gameTime + ticks);`
       - Guard: `if (gameTime < next) return;`

  4) EffectOps (Potions)
     - Replace direct `addEffect/removeEffect/new MobEffectInstance` with `EffectOps`:
       - Give: `EffectOps.give(entity, effect, durationTicks, amplifier, showIcon, showParticles)`.
       - Remove: `EffectOps.remove(entity, effect)`.
       - Toggle patterns use `EffectOps.ensure(entity, effect, duration, amp, visibility)`.

  5) AttributeOps/AbsorptionHelper (Attributes + Shield)
     - Avoid hand-rolled attribute modifiers. For absorption, always use `AbsorptionHelper`:
       - Ensure capacity: `AbsorptionHelper.applyAbsorption(entity, cap, modifierId, true)`.
       - Set/refresh value: `AbsorptionHelper.applyAbsorption(entity, value, modifierId, false)`.
       - Clear on removal: `AbsorptionHelper.clearAbsorptionCapacity(entity, modifierId)`.
     - Prerequisite: `SteelBoneAttributeHooks` must inject `MAX_ABSORPTION` for players. Keep helper null-safe for entities missing the attribute.

  6) TargetingOps/TickOps (Iteration + selection)
     - Use `TargetingOps` for sphere/ring/line scans and entity filters; avoid custom AABB math unless specialised.
     - Use `TickOps.every(serverLevel, periodTicks, key)` for periodic tasks instead of manual modulo counters when possible.

Registration & Activation (AttackAbility)
- Follow JianYingGu pattern strictly:
  - Enum singleton behavior with a static block: `OrganActivationListeners.register(ABILITY_ID, Behavior::activateAbility)`.
  - No constructor side-effects; avoid `<clinit>` heavy logic.
  - Client adds literal ability ids to `ATTACK_ABILITY_LIST` (string `ResourceLocation`), never class references.
  - Server hotkey path logs remain DEBUG; INFO only when diagnosing.

Validation Checklist
- Build: `./gradlew compileJava` (always before yield).
- Manual checks (single-player or test server):
  - Equip/unequip: ledger totals update and fully reset on removal; no `Rebuilt increase effects ... totals=0` after final removal.
  - Cooldowns: no negative ticks; re-equip doesn’t resurrect stale timers; multi-organ stacks don’t share one timer unless design says so.
  - Absorption: caps respected (e.g., 20) and cleared on removal; no stacking beyond cap; steel-bone combo capacity registered.
  - Effects: durations and amplifier follow spec; detach clears buffs/debuffs as intended.
  - Soul/soulbeast: early-return paths present; non-player handlers avoid accessing player-only resources.

Concrete Targets (first three batches)
- Batch A (high drift/old paths):
  - XueDao: `XiediguOrganBehavior`, `XieFeiguOrganBehavior`, `XieyanguOrganBehavior` → LedgerOps, MultiCooldown, EffectOps, AttributeOps where applicable.
  - TuDao: `ShiPiGuOrganBehavior`, `TuQiangGuOrganBehavior` → LedgerOps/AbsorptionHelper (TuQiangGu 已部分迁移，以 Helper 统一)。
  - MuDao: `LiandaoGuOrganBehavior` → MultiCooldown + ResourceOps where it spends/arms.
- Batch B (resource/attr unification):
  - GuDao: `YuGuguOrganBehavior`, `LeGuDunGuOrganBehavior`, `GangjinguOrganBehavior` → ResourceOps + LedgerOps; attributes to AttributeOps where needed.
  - SanZhuan/WuHang: `JinfeiguOrganBehavior` → AbsorptionHelper (替换药水型吸收)。
- Batch C (registries/seed/aux):
  - Kongqiao: `DaoHenBehavior`, `DaoHenClientBehavior`, `DaoHenSeedHandler` → ResourceOps + guard logs; avoid direct bridge writes.
  - Remaining families with direct `LinkageManager` calls → LedgerOps.

Coding Conventions (normative)
- Never mutate `LinkageChannel` directly in behaviours; always go through `LedgerOps`.
- Separate `handlerPlayer(...)` and `handlerNonPlayer(...)` for Guzhenren resources; centralise fallback to HP if design requires (via shared helper).
- Use unique `ResourceLocation` keys for every cooldown/attribute modifier to avoid collisions.
- Keep INFO logs minimal; prefer DEBUG with clear early-return reasons guarded by a feature toggle when diagnosing.

Exit Criteria per file
- No direct imports of `LinkageManager`, `GuzhenrenResourceBridge` (outside ResourceOps), `new MobEffectInstance`/`addEffect/removeEffect`, `new AttributeModifier/getAttribute` in behaviours after migration.
- All timers expressed as `MultiCooldown` or `CooldownOps` entries.
- Equip/unequip paths call the right `LedgerOps.set/remove` and `AbsorptionHelper.clear...` when applicable.

How to verify (fast loop)
- Compile: `./gradlew compileJava`.
- In-game quick script:
  - Equip organ A → observe expected increase/shield/effects.
  - Unequip organ A → observe totals return to baseline; no lingering shields/effects.
  - Re-equip organ A twice → ensure unique keys don’t collide; cooldowns remain per-instance if intended.
  - Trigger abilities (hotkey + soul handler) → activation path logs only on DEBUG.


---
