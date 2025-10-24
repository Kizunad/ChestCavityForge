# GuScript Module Design

GuScript adds an AST-driven kill-move editor that lives inside ChestCavity but keeps its own UI, storage, and execution pipeline. The feature must:

- present a ChestCavity-style container UI with configurable row counts per page;
- store items inside each page and let players bind listeners / key shortcuts to scripted actions;
- compile an AST description of "蛊虫器官杀招" into runtime actions that integrate with existing ChestCavity listeners;
- reuse ChestCavity bridges (resource, linkage, organ listeners) without treating Guzhenren as an external compat module.

## Package Layout

```
net.tigereye.chestcavity.guscript
├── GuScriptModule (entry / registration helper)
├── data
│   ├── GuScriptAttachment (per-player state, registered via CCAttachments)
│   ├── GuScriptPageState (stored layouts, items, listener bindings)
│   └── serialization (Codec + NBT helpers)
├── ast
│   ├── GuScriptNode (base interface)
│   ├── node package (ActionNode, ConditionNode, ReferenceNode, etc.)
│   ├── GuScriptParser (Json/nbt -> AST)
│   └── GuScriptCompiler (AST -> runtime graph)
├── runtime
│   ├── GuScriptRuntime (evaluates compiled scripts)
│   ├── ListenerBinding (organ listener bridges)
│   └── KeybindBinding (client keymap + network trigger)
├── ui
│   ├── GuScriptMenu (extends AbstractContainerMenu)
│   ├── GuScriptScreen (extends AbstractContainerScreen)
│   ├── PageLayout (rows/columns metadata)
│   ├── NotebookModel (multi-page management)
│   └── networking (C->S updates, S->C sync)
└── bridge
    ├── GuScriptBridge (facade over ChestCavityInstance, LinkageManager, GuzhenrenResourceBridge)
    └── Service hooks (registration into existing listeners / keymaps)
```

The top-level package highlights the AST and UI folders explicitly while still allowing supporting subpackages (data, runtime, bridge) to keep the design maintainable.

## Data & Attachments

- `GuScriptAttachment` stores the player's active notebook: ordered pages, selected page index, compiled scripts per binding, and cached execution context (e.g., linkage multiplier, resource handles).
- Registered inside `CCAttachments` with a new `DeferredHolder<AttachmentType<GuScriptAttachment>>`. Serialization uses `Codec` backed by `CompoundTag` so both UI state and compiled graphs survive world reloads.
- Each `GuScriptPageState` contains:
  - `PageLayout` reference (rows, optional columns if future UIs need >9 wide rows);
  - `NonNullList<ItemStack>` sized `rows * 9` (matching Menu slot count);
  - `List<ListenerBindingDefinition>` describing listener or key triggers; definitions map to runtime bindings at load.

## AST Subsystem

- AST exposes immutable node objects implementing `GuScriptNode`. Common node types:
  - 节点的 `actions` 使用强类型接口 `Action`；各实现（例如 `ConsumeZhenyuanAction`、`EmitProjectileAction`）通过 `execute(GuScriptContext)` 与桥接层交互，`ActionRegistry` 负责 JSON/NBT 反序列化到具体类型。
  - control flow (`SequenceNode`, `ConditionalNode`, `LoopNode`),
  - actions (`EmitProjectileNode`, `ConsumeResourceNode`, `ApplyEffectNode`),
  - queries (`ReadResourceNode`, `CheckOrganNode`, `LinkageValueNode`).
- `GuScriptParser` produces ASTs from JSON/NBT resources placed under `data/chestcavity/guscript/scripts/`. Manual editing in the UI will emit the same format to attachment storage.
- `GuScriptCompiler` folds the AST into executable `GuScriptProgram` objects with validated resource requirements and pre-bound references to `GuScriptBridge`. Compilation happens server-side; client uses lightweight summaries for UI preview. When a page slot holds an `ItemStack` with a count > 1, the compiler multiplies the leaf's tag multiset by that count so reaction rules requiring repeated tags can match a single stacked item.
- Validation errors feed back to the UI via dedicated sync packets so players know why a script fails to arm.
- Reactive nodes subscribe to context updates (linkage/resource deltas, target changes) via a lightweight observer API. When those values change, dependent nodes mark cached results dirty so only affected branches re-evaluate.
- Compilation caches structural metadata (listener graphs, node indices) so repeated triggers reuse existing node instances while swapping in fresh execution context.

### AST Reduction Model
  - 执行阶段通过 `GuScriptExecutionBridge` 接入 Guzhenren 资源与实体行为，默认实现 `DefaultGuScriptExecutionBridge` 会扣真元、伤血并按实体注册表发射投射物；行动注册在 `GuScriptModule.bootstrap()` 中统一初始化。
- `guscript/ast` 提供封装的 `GuNode` 层级（`LeafGuNode`、`OperatorGuNode`）和 `GuNodeKind`，反应总是构建包含原子子节点的运算符树，而非平铺产物列表。
- `ReactionRule`（位于 `guscript/runtime`）将每条反应建模为运算符：定义 `arity`、所需/禁用标签以及 `ReactionOperator`，返回新的父节点并保留参与的 children。
- `GuScriptReducer` 穷举候选组合，按覆盖度→优先级挑选最佳反应，将子节点替换为父节点，最终得到一个或多个根节点构成的 AST。
- `GuScriptReducerDebug.logDemo()` 演示 (骨+血)+爆发 → 血骨爆裂枪 的归约流程，方便 UI、脚本编辑器或单元测试复用。

## UI Subsystem

- `GuScriptMenu` mirrors ChestCavity's container: it accepts a `PageLayout`, calculates `imageHeight` using `rows`, and instantiates `Slot` objects for each item slot. Pages share one menu instance at a time; switching pages closes and reopens the container with the new layout.
- `PageLayout` instances are declared via data-driven JSON in `data/chestcavity/guscript/layouts/`. Each layout file specifies:
  - `rows` (required);
  - optional `title` / `background` resource locations;
  - slot tags (e.g., `binding`, `input`, `output`) for custom widgets if needed later.
- `GuScriptScreen` renders the chest-like background but selects the layout texture per page. Tabs or buttons along the top let players swap pages. Hovering listener slots displays bound shortcut info and AST summary.
- `NotebookModel` keeps a paginated list plus metadata (page icon item, name, bound triggers). It lives entirely on the client but synchronizes with `GuScriptAttachment` via S->C packets.
- Item insertion uses vanilla slot mechanics. Special binding slots use custom `Slot` subclasses that either block items or accept token items that stand in for listeners.
- Each page exposes一个“模拟”按钮：运行脚本于模拟上下文（无伤害/消耗），并在 UI 上逐步展示执行路径，便于迭代。

### UI configuration knobs

The responsive layout parameters for `GuScriptScreen` live inside the new `guscript_ui` block of `CCConfig`. They are saved to the standard `config/chestcavity.json` file, so downstream packs can rebalance spacing without recompiling the mod.

- `bindingRightPaddingSlots`, `bindingTopPaddingSlots`, `bindingVerticalSpacingSlots`, and `bindingButtonWidthFraction` scale the listener/binding buttons relative to the slot grid.
- `bindingButtonHeightPx`, `minBindingButtonWidthPx`, `minTopGutterPx`, `minHorizontalGutterPx`, and `minBindingSpacingPx` provide hard floors that keep controls readable on narrow screens. `minHorizontalGutterPx` now defaults to a positive value so controls always retain lateral breathing room.
- `pageButtonWidthPx`, `pageButtonHeightPx`, `pageButtonLeftPaddingSlots`, `pageButtonTopPaddingSlots`, `pageButtonHorizontalSpacingSlots`, `minPageButtonSpacingPx`, `pageButtonHorizontalOffsetPx`, and `pageInfoBelowNavSpacingPx` control the page navigation row and the page info label spacing beneath it.

Tweaking these values automatically updates button placement the next time the UI opens, ensuring the gutter between controls and the inventory rows stays intact across aspect ratios.

## Listener & Keybind Integration

- `ListenerBindingDefinition` enumerates supported trigger types: `ORGAN_SLOW_TICK`, `ORGAN_ON_HIT`, `ORGAN_ON_DAMAGE`, `CLIENT_KEY`, etc. Each definition stores optional filters (organ id, player state) parsed from the UI.
- `GuScriptRuntime` registers `ListenerBinding` objects during attachment load; they subscribe to the appropriate `Organ*Listener` or new `GuScriptEvents` bus. For server listeners, runtime caches compiled `GuScriptProgram` and executes it with the live `GuScriptContext` (player, linkage, resources) when triggered.
- Client key bindings use NeoForge's `KeyMapping`. `KeybindBinding` emits a `GuScriptTriggerPayload` to the server, which validates cooldown/resource costs before execution.
- Execution respects ChestCavity's safety constraints: scripts run inside `GuScriptRuntime#execute`, which handles resource deductions via `GuzhenrenResourceBridge` and `LinkageManager` before delegating to organ behaviors.
- Compiled programs cache by script hash and are stored as weak references; hot triggers like `ORGAN_ON_HIT` reuse the same program instance while garbage-collected entries rebuild on demand.
- Runtime evaluation remains无状态：监听器仅传入新的 `GuScriptExecutionContext`（player、可选 target、linkage 快照、资源适配器），节点通过上下文访问数据而不持有实体引用。
- Reactive绑定会将 linkage/resource 改变写入上下文的可观察映射中，使节点只在需要时重新求值，减轻高频监听的负担。

## Bridge & Data Access

- `GuScriptBridge` centralizes reads into ChestCavity systems:
  - exposes `ChestCavityInstance` for slot lookup and organ checks;
  - exposes `LinkageView` (wrapper around `LinkageManager`) so scripts can read and modify linkage channels;
  - exposes `ResourceAccess` from `GuzhenrenResourceBridge` (zhenyuan, jingli, hunpo stability, etc.).
- The bridge stores weak references where possible to avoid leaking entity instances across ticks. It also provides context builders used both in compiler validation and runtime evaluation.
- Additional helper methods fetch scoreboard upgrades or scoreboard-based cooldowns so kill moves can integrate with future progression systems.
- Context watchers提供 `onResourceChanged`、`onLinkageChanged` 等钩子，优先复用现有 Bridge API；若缺失则退回至逐 tick diff。

## Networking

- New payloads under `net.tigereye.chestcavity.guscript.network`:
  - `OpenGuScriptNotebookPayload` (S->C) – opens the UI with selected page.
  - `SyncGuScriptAttachmentPayload` (S->C) – replicates notebook/item state.
  - `UpdateGuScriptPagePayload` (C->S) – sends edits (items moved, listeners configured, script text changes).
  - `TriggerGuScriptPayload` (C->S) – fires keybound actions.
- Reuse the existing NeoForge `RegisterPayloadHandlersEvent` wiring, grouping GuScript channels beside other chestcavity payloads.
- 开发者模式新增 `guscript debug run <player> <script>` 指令，打印 AST 遍历顺序、上下文变更与 reactive 失效日志，辅助排查。
- 模拟按钮复用 `GuScriptSimulationPayload`，确保客户端预览与服务端校验共享同一执行路径。

## Implementation Roadmap

1. **Scaffold module:** add `guscript` package with `GuScriptModule`, register attachments, menu type, networking, and key mappings.
2. **Data foundations:** implement `GuScriptAttachment`, `PageLayout` loader, and server/client sync.
3. **UI build:** implement `GuScriptMenu` / `Screen`, tab widgets, and inventory slot layout using layout definitions.
4. **AST & compilation:** implement parser/compiler pipeline with a minimal action set (resource read/write, effect triggers). Add validation feedback loops.
5. **Runtime bindings:** wire listener registration, keybind handling, and script execution with bridge lookups.
6. **Reactive runtime:** 实现上下文观察者与程序缓存，验证多次触发与缓存驱逐后的重建路径。
7. **QA & Tooling:**
   - 实装 `guscript debug run` 指令并输出结构化日志；
   - 打通 UI 模拟预览（模拟上下文）并校验其与正式执行共享验证逻辑；
   - 编写示例脚本与集成测试，覆盖 reactive 更新、资源/链接差分与 UI 持久化。

### Flow runtime resource scaling

- `FlowActions.consume_resource` now normalises per-tick deductions against the effective `time.accelerate` parameter exported by the executor. Every invocation divides the configured amount by the resolved time scale so that a 10 HP/20 真元/10 精力 “per second” cost remains stable even when the flow accelerates or slows down.
- When consuming Guzhenren resources the action routes to dedicated helpers: `jingli` uses `ResourceHandle.adjustJingli`, `zhenyuan` honours cultivation scaling via `ResourceHandle.consumeScaledZhenyuan`, and other identifiers fall back to the generic `adjustDouble` path.
- Failures (missing attachment, unknown identifier, insufficient pool, or write errors) emit a DEBUG trace with the requested amount, resolved time scale, and before/after snapshots, then immediately raise a WARN and request `FlowTrigger.CANCEL`. That pushes the runtime into the flow’s cancel branch so punishment FX/fireballs trigger reliably instead of silently skipping the cost.

## Open Questions / Follow-ups

- Determine whether script layouts should be per-player configurable via data packs or fixed presets shipped with the mod.
- Decide if UI editing of AST will use drag-and-drop blocks, textual JSON, or hybrid chips; affects required widgets.
- Evaluate cooldown governance: should scripts declare cooldowns directly or reuse existing organ cooldown infrastructure?
- Plan for localization strings (page titles, listener names) and accessibility (narration for keybind descriptions).
