# Agent Kickstart

**Current status**: ChestCavityForge is migrated to Minecraft 1.21.1 on NeoForge.

## Important memory
- Full system access is available; act responsibly and avoid unnecessary destructive commands.
- Any tool installation or environment-variable changes that could pollute the system must be expressed through Nix (e.g., via `flake.nix` devShell updates).
- Upstreams: `origin` = personal fork, `upstream` = BoonelDanForever/ChestCavityForge.
- In NeoForge 21.x the mod event bus is injected via the mod constructor (`public ChestCavity(IEventBus bus)`); `FMLJavaModLoadingContext` is gone.
- 总结时(用户对话): 用中文总结当前完成了什么，然后还剩下什么需要处理的问题 

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
