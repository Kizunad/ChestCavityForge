# Guzhenren Event Extension - Architecture & Roadmap

This document outlines the architecture and development roadmap for the Guzhenren Event Extension module.

## Architecture Overview

The Guzhenren Event Extension is a modular, data-driven system designed to create complex in-game events based on player actions and status changes, with a particular focus on integration with the Guzhenren mod's core mechanics.

The architecture is based on a **Polling Detection Model**.

### Core Concepts

1.  **Data-Driven by JSON:** Instead of hard-coding events, all event logic (Triggers, Conditions, Actions) is defined in `.json` files located in `data/guzhenren_event_ext/events/`. This allows for easy creation, modification, and expansion of events without changing the mod's source code.

2.  **Polling Detection:** The system does not directly hook into the Guzhenren mod. Instead, it periodically checks (polls) player data to detect changes. This ensures compatibility and stability, as it doesn't rely on the internal workings of the Guzhenren mod.

### Key Components

-   **`PlayerStatWatcher` / `PlayerInventoryWatcher`**: These are the system's sensors. They are server-side managers that run on configurable timers (e.g., every 2 seconds for stats, every 1 minute for inventory). They compare current player data against a cached version and fire specific events (`GuzhenrenStatChangeEvent`, `PlayerObtainedItemEvent`) when a change is detected.

-   **`EventLoader`**: This component automatically loads and parses all event definition `.json` files when the server starts or when the `/reload` command is used. It also intelligently activates the necessary watchers only if corresponding triggers are found in the definitions.

-   **`EventManager`**: This is the central hub of the system. It listens for events fired by the watchers, finds the matching event definitions provided by the `EventLoader`, and processes them.

-   **Registries (`TriggerRegistry`, `ConditionRegistry`, `ActionRegistry`)**: These are simple mapping systems that allow developers to register new, reusable logic components. For example, one can register a new "has_potion_effect" condition, and it can then be used by its type ID in any event JSON file.

### Workflow

1.  **Load:** The server starts, and the `EventLoader` reads all event JSONs, determining which stats and watchers need to be active.
2.  **Poll:** The active `Watcher` components periodically check player data.
3.  **Detect & Fire:** A watcher detects a change (e.g., a new item in the inventory) and fires the corresponding event (e.g., `PlayerObtainedItemEvent`) onto `NeoForge.EVENT_BUS`.
4.  **Process:** The `EventManager` catches the event, finds a matching JSON rule (e.g., one with a `player_obtained_item` trigger), and validates it.
5.  **Validate & Execute:** The `EventManager` checks the `trigger_once` flag and all `conditions`. If all checks pass, it runs the associated `actions`.

---

## Development Roadmap

### Phase 1: Core Framework (Complete)

-   [x] Polling mechanisms (`PlayerStatWatcher`, `PlayerInventoryWatcher`).
-   [x] Custom event classes (`GuzhenrenStatChangeEvent`, `PlayerObtainedItemEvent`).
-   [x] NeoForge Event Bus wiring（Watcher 直接向 `NeoForge.EVENT_BUS` 投递事件，`EventManager` 监听处理）。
-   [x] Gamerule for enabling/disabling the system.
-   [x] Player data attachment for `trigger_once` state.
-   [x] Basic file and package structure.
-   [x] `ModConfig` file for settings like polling interval.

### Phase 2: Event Processing Engine (Complete)

-   [x] `EventLoader` implementation for parsing JSON files from data packs.
-   [x] `EventManager` implementation for handling the core event processing loop.
-   [x] `TriggerRegistry`, `ConditionRegistry`, and `ActionRegistry` implementation.
-   [x] Integration of all components into the mod's lifecycle.

### Phase 3: Basic Module Implementation (Complete)

-   [x] Implement the `player_stat_change` trigger logic.
-   [x] Implement the `player_obtained_item` trigger logic.
-   [x] Implement a set of standard conditions (`minecraft:random_chance`, `guzhenren:player_health_percent`).
-   [x] Implement a set of standard actions (`guzhenren_event_ext:send_message`, `guzhenren_event_ext:run_command`, `guzhenren_event_ext:adjust_player_stat`).
-   [x] 验证触发器/动作全链路（NeoForge Event Bus 分发链路已验证）。

### Phase 4: Advanced Features & Optimization

-   [ ] Implement more trigger types (e.g., `player_kill_mob`, `player_craft_item`).
-   [ ] Implement more advanced conditions and actions (e.g., checking inventory, giving items, applying potion effects).
-   [ ] Performance profiling and optimization of the polling mechanism.
-   [ ] **(Low Priority)** Implement an optional real-time hook into `GuzhenrenResourceBridge` for instant feedback on self-initiated actions.
