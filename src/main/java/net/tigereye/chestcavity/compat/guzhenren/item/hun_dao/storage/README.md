# Hun Dao Storage

**Status:** Implemented (Phase 2)

## Purpose
This package contains data persistence logic for Hun Dao, including:
- NBT serialization/deserialization
- State persistence
- Save/load operations
- NeoForge attachment integration

## Implemented Components

### HunDaoSoulState
**File:** `HunDaoSoulState.java`

Persistent state container for hun-dao soul-related data that is not managed by the resource system.

**Features:**
- **DOT Tracking:** Soul flame remaining ticks and DPS
- **Soul Beast Statistics:** Total duration, activation count
- **Scheduler State:** Last hunpo leak tick

**Persistence:**
- NBT serialization via `save()` and `load(CompoundTag)` methods
- Registered as NeoForge attachment in `CCAttachments.HUN_DAO_SOUL_STATE`
- Automatically persisted across save/load cycles

**Access:**
```java
// Get soul state through runtime context
HunDaoRuntimeContext context = HunDaoRuntimeContext.get(player);
HunDaoSoulState state = context.getOrCreateSoulState();

// Track soul flame
state.setSoulFlameDps(5.0);
state.setSoulFlameRemainingTicks(100);

// Track soul beast stats
state.incrementSoulBeastActivationCount();
state.addSoulBeastDuration(20L);
```

## Integration

### NeoForge Attachments
Registered in `CCAttachments.java`:
- **Attachment Key:** `HUN_DAO_SOUL_STATE`
- **Serializer:** `HunDaoSoulStateSerializer`
- **Accessor:** `CCAttachments.getHunDaoSoulState(LivingEntity)`

### Runtime Access
Accessed through `HunDaoRuntimeContext`:
```java
HunDaoRuntimeContext context = HunDaoRuntimeContext.get(player);
Optional<HunDaoSoulState> state = context.getSoulState();
```

## Architecture Alignment
Mirrors `jian_dao/storage/` structure for consistency across weapon systems.

## Future Enhancements (Phase 3+)
- Migration utilities for legacy NBT data (if needed)
- Additional state tracking for new hun-dao features
- Performance optimizations for large-scale state queries
