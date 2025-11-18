# Hun Dao UI

**Status:** Phase 6 (Complete), Phase 7 (Modern UI Panel Added)

## Purpose
Client-side UI components for Hun Dao, including HUD overlays, notification rendering for hun po status, soul beast timers, game event messages, and Modern UI panel integration.

## Architecture

### Components

#### HunDaoSoulHud
HUD overlay that displays hun dao status information on the client screen:
- **Hun Po Bar:** Resource bar showing current/max hun po (above hotbar)
- **Soul Beast Timer:** Countdown timer when soul beast form is active
- **Soul Flame Stacks:** Indicator showing soul flame stacks on crosshair target
- **Gui Wu Indicator:** Active status and remaining duration for gui wu fog

**Integration:**
- Queries `HunDaoClientState` for current values
- Renders via `GuiGraphics` during `RenderGuiEvent.Post`
- Framework established in Phase 5, full rendering in Phase 6+

**Usage:**
```java
// Register HUD overlay during client initialization
HunDaoSoulHud.register();

// Rendering happens automatically via event system
```

#### HunDaoNotificationRenderer
Toast-style notification system for server-pushed messages:
- **Notification Queue:** FIFO queue with max 5 concurrent notifications
- **Fade-Out Animation:** Smooth alpha fade over last 10 ticks
- **Category Styling:** Different colors for INFO, WARNING, SUCCESS, ERROR
- **Auto-Cleanup:** Expired notifications removed automatically

**Categories:**
- `INFO` - General information
- `WARNING` - Hun po leak warnings, low resource alerts
- `SUCCESS` - Soul beast activation, ability unlocks
- `ERROR` - Ability failures, invalid actions

**Usage:**
```java
// Show notification from server sync handler
HunDaoNotificationRenderer.show(
    Component.translatable("hun_dao.notification.hun_po_low"),
    NotificationCategory.WARNING
);

// Tick notifications (called from HunDaoClientEvents)
HunDaoNotificationRenderer.tick();

// Render notifications (called from rendering event)
HunDaoNotificationRenderer.render(guiGraphics, partialTicks);
```

## Data Flow

### HUD Rendering Flow
```
ClientTickEvent → HunDaoClientState.tick() (update timers)
                         ↓
    RenderGuiEvent.Post → HunDaoSoulHud.render()
                         ↓
              Query HunDaoClientState for values
                         ↓
              Render bars/timers/indicators
```

### Notification Flow
```
Server Event → Network Packet → UI Sync Handler
                                      ↓
                    HunDaoNotificationRenderer.show()
                                      ↓
                         Queue notification
                                      ↓
           RenderGuiEvent.Post → render() (display)
                                      ↓
                 ClientTickEvent → tick() (decay & cleanup)
```

## Integration Points

### With Client State
UI components are read-only consumers of `HunDaoClientState`:
- HUD queries state every frame for current values
- Notifications may trigger based on state thresholds (e.g., hun po < 20%)

### With Network Layer
Notifications receive messages from server via sync handlers:
```java
HunDaoClientSyncHandlers.handleHunPoSync(uuid, current, max);
if (current / max < 0.2) {
  HunDaoNotificationRenderer.show(warning, WARNING);
}
```

### With Behavior Context Helper
UI formatting uses `HunDaoBehaviorContextHelper` for consistent message formatting:
```java
String message = HunDaoBehaviorContextHelper.formatHunPo(current, max);
```

## Phase 5 Status

### Completed
1. ✅ `HunDaoSoulHud` framework (render methods stubbed)
2. ✅ `HunDaoNotificationRenderer` queue and lifecycle
3. ✅ Notification categories and styling structure
4. ✅ Integration with `HunDaoClientState`

### Deferred to Phase 6+
1. ⏳ Full GuiGraphics rendering implementation
2. ⏳ Texture/sprite resources for hun po bar
3. ⏳ Animation interpolation for smooth transitions
4. ⏳ Config-based HUD positioning
5. ⏳ Crosshair target tracking for soul flame stacks

## Rendering Notes

### Rendering API
Phase 5 uses Minecraft's modern `GuiGraphics` API (1.20+):
- `guiGraphics.fill(x, y, width, height, color)` - Colored rectangles
- `guiGraphics.drawString(font, text, x, y, color)` - Text rendering
- `guiGraphics.blit(...)` - Texture/sprite rendering

### Performance Considerations
- HUD renders every frame (60+ FPS), must be efficient
- Minimize string allocations (cache formatted strings)
- Avoid complex calculations in render methods
- Use `partialTicks` for smooth animations

### Positioning Strategy
- **Hun Po Bar:** Center-aligned above hotbar (`screenWidth/2 - 91`, `screenHeight - 42`)
- **Timers:** Top-right corner with vertical stacking
- **Notifications:** Top-right corner, slide-in from off-screen
- **Soul Flame Stacks:** Near crosshair (requires target tracking)

## Phase 7: Modern UI Panel Integration

### Hun Dao Modern Panel
Phase 7 introduces a comprehensive Modern UI panel for viewing soul system information:
- **Entry Point:** `/hundaopanel` command
- **Architecture:** Tab-based interface with extensible design
- **First Tab:** Soul Overview (state, level, rarity, max, attributes)
- **Future Tabs:** Reserved for Phase 8+ expansion

**Key Features:**
- Fallback strategy for missing data (no crashes)
- Type-safe enums for soul state and rarity
- Dynamic attribute list rendering
- Clean separation from HUD rendering

**Documentation:**
See `client/modernui/README.md` for complete Modern UI panel documentation.

## Architecture Alignment
Mirrors `jian_dao/ui/` structure for consistency across weapon systems.

## Related Documentation

- `client/README.md` - Client state and event handling
- `client/modernui/README.md` - Modern UI panel architecture (Phase 7)
- `fx/README.md` - FX system architecture
- `docs/Phase5_Plan.md` - Phase 5 implementation plan
- `docs/Phase5_Report.md` - Phase 5 completion report
- `docs/Phase6_Plan.md` - Phase 6 implementation plan
- `docs/Phase6_Report.md` - Phase 6 completion report
- `docs/Phase7_Plan.md` - Phase 7 implementation plan
- `docs/Phase7_Report.md` - Phase 7 completion report
