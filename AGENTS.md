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
