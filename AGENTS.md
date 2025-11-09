# Guzhenren Event Extension - Architecture & Roadmap

This document outlines the architecture and development roadmap for the Guzhenren Event Extension module.

## Architecture Overview

The Guzhenren Event Extension is a modular, data-driven system designed to create complex in-game events based on player actions and status changes, with a particular focus on integration with the Guzhenren mod's core mechanics.

The architecture is based on a **Polling Detection Model**.

### Core Concepts

1.  **Data-Driven by JSON:** Instead of hard-coding events, all event logic (Triggers, Conditions, Actions) is defined in `.json` files located in `data/guzhenren_event_ext/events/`. This allows for easy creation, modification, and expansion of events without changing the mod's source code.

2.  **Polling Detection:** The system does not directly hook into the Guzhenren mod. Instead, it periodically checks (polls) player data to detect changes. This ensures compatibility and stability, as it doesn't rely on the internal workings of the Guzhenren mod.

### Key Components

-   **`PlayerStatWatcher`**: This is the heart of the system. It's a server-side manager that runs periodically.
    -   It maintains a cache of players' last known stats (e.g., `zhenyuan`, `hunpo`).
    -   At a configurable interval (e.g., every 2 seconds), it compares current stats against the cached values.
    -   If a change is detected, it fires a `GuzhenrenStatChangeEvent`.
    -   For performance, it only watches stats that are actively used by at least one event definition in the JSON files.

-   **`GuzhenrenStatChangeEvent`**: A custom event that is fired by the `PlayerStatWatcher`. It contains information about the change: the player, the stat that changed, the old value, and the new value.

-   **`CustomEventBus`**: An isolated event bus used exclusively for this module's events, preventing conflicts with other parts of the mod or Minecraft itself.

-   **`EventManager` (To be implemented)**: This component will listen for events on the `CustomEventBus`. When an event occurs, it will search through all loaded JSON definitions to find a matching `trigger`. If found, it will evaluate the `conditions` and, if they pass, execute the defined `actions`.

-   **Registries (To be implemented)**: A set of registries (`TriggerRegistry`, `ConditionRegistry`, `ActionRegistry`) will allow developers to easily add new types of triggers, conditions, and actions to the system, making it highly extensible.

### Workflow

1.  **Load:** The server starts, and the `EventLoader` reads all event JSONs, determining which stats need to be watched.
2.  **Poll:** The `PlayerStatWatcher` periodically checks the watched stats for all online players.
3.  **Detect & Fire:** It detects a change in a player's stat (e.g., `zhenyuan` drops below a threshold) and fires a `GuzhenrenStatChangeEvent`.
4.  **Process:** The `EventManager` catches the event, finds a matching JSON rule, checks its conditions (e.g., is the player in a specific biome?), and if all checks pass, runs the associated actions (e.g., send a chat message, run a command).

---

## Development Roadmap

### Phase 1: Core Framework (90% Complete)

-   [x] Polling mechanism (`PlayerStatWatcher`).
-   [x] Custom event classes (`GuzhenrenStatChangeEvent`).
-   [x] Custom Event Bus.
-   [x] Gamerule for enabling/disabling the system.
-   [x] Basic file and package structure.
-   [ ] **TODO:** `ModConfig` file for settings like polling interval.

### Phase 2: Event Processing Engine (Next Steps)

-   [ ] Implement `EventLoader` to parse event definition JSONs.
-   [ ] Implement `EventManager` to orchestrate event processing.
-   [ ] Implement `TriggerRegistry`, `ConditionRegistry`, and `ActionRegistry`.

### Phase 3: Basic Module Implementation

-   [ ] Implement the `player_stat_change` trigger logic.
-   [ ] Implement a set of standard conditions (e.g., `minecraft:random_chance`, `guzhenren:player_health_percent`).
-   [ ] Implement a set of standard actions (e.g., `guzhenren_event_ext:send_message`, `guzhenren_event_ext:run_command`, `guzhenren_event_ext:adjust_player_stat`).

### Phase 4: Advanced Features & Optimization

-   [ ] Implement more trigger types (e.g., `player_kill_mob`, `player_craft_item`).
-   [ ] Implement more advanced conditions and actions (e.g., checking inventory, giving items, applying potion effects).
-   [ ] Performance profiling and optimization of the polling mechanism.
-   [ ] **(Low Priority)** Implement an optional real-time hook into `GuzhenrenResourceBridge` for instant feedback on self-initiated actions.