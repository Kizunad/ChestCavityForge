# Hun Dao Client

**Status:** Phase 6 (Complete)

## Purpose
Client-side infrastructure for Hun Dao, providing state management, event handling, network synchronization, and FX coordination.

## Architecture

### Core Components

#### HunDaoClientState
Singleton client-side state cache that stores:
- **Soul Flame:** DoT stacks and remaining duration per entity
- **Soul Beast:** Active state and remaining duration per player
- **Hun Po:** Current and max hun po values per player
- **Gui Wu:** Active state and remaining duration per player

**Key Features:**
- Automatic timer decay via `tick()` method (called every client tick)
- UUID-based state storage for multi-entity tracking
- Cleanup methods for entity unload and dimension changes

**Usage:**
```java
HunDaoClientState state = HunDaoClientState.instance();

// Query state
boolean isSoulBeastActive = state.isSoulBeastActive(playerUUID);
int soulFlameStacks = state.getSoulFlameStacks(entityUUID);
double hunPoPercentage = state.getHunPoPercentage(playerUUID);

// Update state (called by sync handlers)
state.setSoulBeastActive(playerUUID, true);
state.setSoulFlameDuration(entityUUID, 100);
state.setHunPo(playerUUID, current, max);
```

#### HunDaoClientRegistries
Handles client-side registration during `FMLClientSetupEvent`:
- Registers Hun Dao abilities with ChestCavity keybinding system
- Registers FX hooks (placeholder for future particle renderers)
- Idempotent initialization (safe to call multiple times)

**Registered Abilities:**
- `guzhenren:gui_qi_gu` - Gui Qi Gu active ability
- `guzhenren:synergy/hun_shou_hua` - Hun Shou Hua synergy ability

#### HunDaoClientEvents
NeoForge event handlers for client-side logic:
- **ClientTickEvent.Post:** Ticks `HunDaoClientState` to decay timers
- **LevelEvent.Unload:** Clears all client state on level unload

**Event Registration:**
Register this class with NeoForge event bus during mod initialization:
```java
NeoForge.EVENT_BUS.register(HunDaoClientEvents.class);
```

#### HunDaoClientSyncHandlers
Processes server-to-client sync packets and updates `HunDaoClientState`:
- `handleSoulFlameSync(UUID, int, int)` - Soul flame state
- `handleSoulBeastSync(UUID, boolean, int)` - Soul beast state
- `handleHunPoSync(UUID, double, double)` - Hun po resources
- `handleGuiWuSync(UUID, boolean, int)` - Gui wu state
- `handleClearEntity(UUID)` - Clear specific entity state
- `handleClearAll()` - Clear all state

**Usage from Custom Payloads:**
```java
public class HunDaoSyncPayload implements CustomPacketPayload {
  @Override
  public void handle(PlayPayloadContext ctx) {
    ctx.enqueueWork(() -> {
      HunDaoClientSyncHandlers.handleSoulFlameSync(entityId, stacks, duration);
    });
  }
}
```

## Data Flow

### Client State Update Flow
```
Server → CustomPacketPayload → HunDaoClientSyncHandlers
                                         ↓
                                 HunDaoClientState (update)
                                         ↓
                                 HUD / FX (render)
```

### Client Tick Flow
```
ClientTickEvent.Post → HunDaoClientEvents.onClientTick()
                               ↓
                       HunDaoClientState.tick() (decay timers)
                               ↓
                       Ambient FX (if needed)
```

## Integration Points

### With FX System
Client state drives FX rendering decisions:
- Soul beast ambient particles play while `isSoulBeastActive() == true`
- Soul flame particles intensity scales with `getSoulFlameStacks()`
- FX rendering handled by FxEngine, not client code

### With UI System
HUD components query client state for rendering:
- `HunDaoSoulHud` displays hun po bar using `getHunPoPercentage()`
- Soul beast timer shows remaining duration from `getSoulBeastDuration()`
- Soul flame stacks indicator uses `getSoulFlameStacks()`

### With Network Layer
Client sync handlers provide the bridge between network payloads and client state:
- Payloads decode server data
- Handlers update `HunDaoClientState`
- HUD/FX systems react to state changes

## Phase 5 Achievements

1. ✅ Client state cache implemented (HunDaoClientState)
2. ✅ Client registries refactored from HunDaoClientAbilities
3. ✅ NeoForge client event handlers implemented
4. ✅ Client sync handler infrastructure created
5. ✅ Automatic timer decay and cleanup logic
6. ✅ UUID-based multi-entity state tracking

## Phase 6 Achievements
1. ✅ `HunDaoClientConfig.java` - Client config for HUD/notifications toggles
2. ✅ Network sync handlers complete with Phase6 payloads (SoulFlame/SoulBeast/HunPo/GuiWu/Clear/Notification)
3. ✅ `HunDaoClientEvents` - RenderGuiEvent.Post for HUD, ClientTickEvent for state/timers
4. ✅ Integration with `ui/` HUD/notification rendering

## Future Work (Phase 7+)

- Implement custom network payloads for state synchronization
- Add client-side particle renderers for hun-dao-specific effects
- Implement smooth interpolation for hun po bar animations
- Add client config for HUD positioning and visibility
- Performance profiling and optimization for large multiplayer servers

## Related Documentation

- `fx/README.md` - FX system architecture
- `ui/README.md` - HUD and notification systems (Phase 6)
- `network/README.md` - Payloads & sync (Phase 6, new)
- `runtime/README.md` - Server-side runtime operations
- `docs/Phase5_Plan.md` - Phase 5 implementation plan
- `docs/Phase5_Report.md` - Phase 5 completion report
