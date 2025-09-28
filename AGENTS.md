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

## 2025-10-XX Coordination mandate (CLI agent only)
- The CLI agent acts purely as coordinator/analyst: observe repository state, capture requirements, and run read-only validation (e.g., builds/tests). Authoring or modifying feature code/config outside of `AGENTS.md` is prohibited.
- Record each user request, design intent, and blocking issue in this file so downstream web Codex agents receive accurate marching orders.
- When implementation work is needed, hand the task back to the user (or designated relay) with a clear TODO summary instead of editing source files directly.
- If an instruction would require code changes beyond documentation/notes, pause and request the user to route it to the web Codex worker.

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
- Phase 2 (cutover): `feature/guscript-ability-relocation-phase2`
  - Update all call sites/registrations/imports to new package.
  - Remove deprecated shims; clean imports.
  - Final compile + in-game smoke test.

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
