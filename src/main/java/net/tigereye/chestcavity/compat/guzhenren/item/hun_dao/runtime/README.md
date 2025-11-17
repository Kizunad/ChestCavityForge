# Hun Dao Runtime

**Status:** Fully Implemented (Phase 1 + Phase 2)

## Purpose
This package contains runtime state management and operation interfaces for Hun Dao, providing a clean abstraction layer between behaviors and underlying systems.

## Implemented Components

### Phase 1: Interface Layer

#### HunDaoResourceOps
**File:** `HunDaoResourceOps.java`

Interface for hun-dao resource operations (hunpo management).

**Methods:**
- `leakHunpoPerSecond(Player, double)` - Leak hunpo at specified rate
- `consumeHunpo(Player, double)` - Consume hunpo, return success/failure
- `openHandle(Player)` - Open resource handle
- `readHunpo(Player)` - Read current hunpo value
- `readMaxHunpo(Player)` - Read maximum hunpo value
- `readDouble(Player, String)` - Read any double resource field
- `adjustDouble(Player, String, double, boolean, String)` - Adjust resource field

#### HunDaoFxOps
**File:** `HunDaoFxOps.java`

Interface for special effects operations.

**Methods:**
- `applySoulFlame(Player, LivingEntity, double, int)` - Apply soul flame DOT

#### HunDaoNotificationOps
**File:** `HunDaoNotificationOps.java`

Interface for maintenance and notification operations.

**Methods:**
- `handlePlayer(Player)` - Player upkeep (saturation, etc.)
- `handleNonPlayer(LivingEntity)` - Non-player entity upkeep

#### HunDaoOpsAdapter
**File:** `HunDaoOpsAdapter.java`

Singleton adapter implementing all three operation interfaces by delegating to existing middleware and resource systems.

**Usage:**
```java
HunDaoResourceOps resourceOps = HunDaoOpsAdapter.INSTANCE;
resourceOps.consumeHunpo(player, 10.0);
```

### Phase 2: Runtime Context & State Management

#### HunDaoRuntimeContext
**File:** `HunDaoRuntimeContext.java`

Unified runtime context providing access to all hun-dao operations and state for an entity.

**Features:**
- Single point of access for all hun-dao systems
- Builder pattern for testing with mock implementations
- Convenience methods for entity type checking

**Usage:**
```java
// Get context for a player
HunDaoRuntimeContext context = HunDaoRuntimeContext.get(player);

// Access operations
context.getResourceOps().consumeHunpo(player, 10.0);
context.getFxOps().applySoulFlame(player, target, 5.0, 3);
context.getStateMachine().activateSoulBeast();

// Access persistent state
context.getSoulState().ifPresent(state -> {
    state.incrementSoulBeastActivationCount();
});
```

#### HunDaoStateMachine
**File:** `HunDaoStateMachine.java`

State machine for managing hun-dao entity states (soul beast transformation, draining, etc.).

**States:**
- `NORMAL` - Normal state, no transformation
- `SOUL_BEAST_ACTIVE` - Soul beast active, hunpo draining
- `SOUL_BEAST_PERMANENT` - Permanent soul beast, no drain

**Operations:**
- `getCurrentState()` - Query current state
- `isSoulBeastMode()` - Check if in any soul beast state
- `isDraining()` - Check if hunpo is draining
- `activateSoulBeast()` - Activate transformation
- `deactivateSoulBeast()` - Deactivate transformation
- `makePermanent()` - Make transformation permanent
- `removePermanent()` - Remove permanent status (admin)
- `syncToClient()` - Sync state to client

**State Transitions:**
```
NORMAL ⇄ SOUL_BEAST_ACTIVE ⇄ SOUL_BEAST_PERMANENT
         (activate/deactivate)    (makePermanent)
```

**Integration:**
Wraps `SoulBeastState` and `SoulBeastStateManager` with state machine semantics and transition validation.

#### HunPoDrainScheduler
**File:** `HunPoDrainScheduler.java`

Scheduler for periodic hunpo drainage during soul beast transformation.

**Features:**
- Ticks once per second (every 20 game ticks)
- Drains hunpo from players in soul beast mode
- Auto-deactivates soul beast when hunpo exhausted
- Can be enabled/disabled globally

**Registration:**
Registered to `NeoForge.EVENT_BUS` in `ChestCavity.java` constructor.

**Event:** Listens to `LevelTickEvent.Post` (server-side only)

**Usage:**
```java
// Scheduler runs automatically after registration
HunPoDrainScheduler.INSTANCE.enable();
HunPoDrainScheduler.INSTANCE.disable();
```

## Architecture

### Dependency Flow
```
Behavior Classes
    ↓ (use)
HunDaoRuntimeContext
    ↓ (provides)
├─ HunDaoResourceOps ──→ HunDaoOpsAdapter ──→ Middleware/ResourceOps
├─ HunDaoFxOps ────────→ HunDaoOpsAdapter ──→ Middleware
├─ HunDaoNotificationOps → HunDaoOpsAdapter ──→ Middleware
├─ HunDaoStateMachine ──→ SoulBeastStateManager ──→ SoulBeastState
└─ HunDaoSoulState ────→ CCAttachments
```

### Design Principles
- **Dependency Inversion (DIP):** Behaviors depend on interfaces, not implementations
- **Single Responsibility:** Each component has one clear purpose
- **Testability:** Builder pattern allows mock injection for testing
- **KISS:** Simple, focused APIs

## Migration Guide

### Phase 1 to Phase 2
Behaviors can optionally migrate from direct ops injection to runtime context:

**Before (Phase 1):**
```java
private final HunDaoResourceOps resourceOps = HunDaoOpsAdapter.INSTANCE;
resourceOps.consumeHunpo(player, amount);
```

**After (Phase 2):**
```java
HunDaoRuntimeContext context = HunDaoRuntimeContext.get(player);
context.getResourceOps().consumeHunpo(player, amount);
context.getStateMachine().activateSoulBeast();
```

**Note:** Both approaches are valid. Runtime context is recommended when state machine or persistent state access is needed.

## Future Enhancements (Phase 3+)
- Additional state machines for other hun-dao mechanics
- Performance monitoring and metrics
- Advanced scheduler features (priority, throttling)
- Context caching/pooling for high-frequency operations
