# Hun Dao Modern UI

**Status:** Phase 7.1 (Complete - Production Ready)

## Purpose
Modern UI panel for Hun Dao, providing a tabbed interface for viewing soul system information, stats, and future interactive features.

## Architecture

### Panel Structure
```
┌─────────────────────────────────────────────────────────────┐
│           Hun Dao Modern Panel (Phase 7)                    │
├─────────────────┬─────────────────┬─────────────────────────┤
│ Soul Overview   │   Reserved      │        Reserved         │
├─────────────────┴─────────────────┴─────────────────────────┤
│                                                              │
│  [Tab content area - dynamically rendered]                  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Core Components

#### CanvasContentView (Phase 7.1)
Custom view for rendering tab content using Modern UI Canvas API:
```java
public class CanvasContentView extends View {
  private IHunDaoPanelTab activeTab;

  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    if (activeTab != null) {
      activeTab.renderContent(canvas, 0, 0, 0f);
    }
  }

  public void setActiveTab(@Nullable IHunDaoPanelTab tab) {
    this.activeTab = tab;
    invalidate(); // Trigger redraw
  }
}
```

**Key Features:**
- Direct Canvas rendering (no TextView intermediate layer)
- Automatic redraw on tab switch via `invalidate()`
- Proper Modern UI `View` lifecycle integration
- Extensible for future Canvas-based features (icons, graphics, animations)

#### IHunDaoPanelTab
Tab interface defining the contract for all panel tabs:
```java
public interface IHunDaoPanelTab {
  String getId();                    // Unique tab identifier
  String getTitle();                 // Display title
  void renderContent(...);           // Render tab content
  boolean isVisible();               // Show in tab bar
  boolean isEnabled();               // Allow clicking
}
```

**Design Principles:**
- Open/Closed: Easy to add new tabs without modifying existing code
- Single Responsibility: Each tab handles its own rendering and data
- Interface Segregation: Minimal required methods for tab implementation

#### HunDaoModernPanelFragment
Main panel fragment extending Modern UI's `Fragment` class:
- Manages tab lifecycle and switching
- Renders tab navigation bar
- Delegates content rendering to active tab via `CanvasContentView`
- Provides close button and panel chrome

**Key Features:**
- Dynamic tab list (3 tabs by default, easily extensible)
- Active tab highlighting
- Tab switching via button clicks with immediate content refresh
- Integration with Minecraft's Modern UI system

**Phase 7.1 Improvements:**
```java
private void switchTab(int index) {
  if (index >= 0 && index < tabs.size()) {
    activeTabIndex = index;
    // Update content view to render the new active tab
    if (contentView != null) {
      contentView.setActiveTab(tabs.get(index));
    }
  }
}
```
- Removed TODO placeholder
- Actual content refresh on tab switch
- Uses `CanvasContentView.setActiveTab()` to trigger redraw

#### SoulOverviewTab
First implemented tab showing core soul information:
- **Soul State:** Active / Rest / Unknown
- **Soul Level:** Integer or "--" fallback
- **Soul Rarity:** Common / Rare / Epic / Legendary / Unidentified
- **Soul Max:** Hun po maximum or "--"
- **Attributes:** Dynamic key-value list (min 3 placeholders)

**Data Source:**
- Queries `HunDaoClientState.instance()` for all values
- Uses current player's UUID
- Implements comprehensive fallback strategy

**Fallback Strategy:**
```
If soul system is inactive:
  → Display "━━━━━━━━━━━━━━━━━━━━━━━━━━"
  → Display "⚠ Soul System is Inactive" (yellow)
  → Display "━━━━━━━━━━━━━━━━━━━━━━━━━━"
  → Show placeholders for all fields (Unknown, --, Unidentified)
  → No crash, no missing data
```

**Phase 7.1 Canvas Rendering:**
```java
@Override
public void renderContent(@NonNull Canvas canvas, int mouseX, int mouseY, float partialTick) {
  // Get player and state
  HunDaoClientState state = HunDaoClientState.instance();
  UUID playerId = player.getUUID();
  boolean isActive = state.isSoulSystemActive(playerId);

  // Render title
  renderText(canvas, "Soul Overview", 10, y, COLOR_WHITE);

  // Warning block if inactive
  if (!isActive) {
    renderText(canvas, "━━━━━━━━━━━━━━━━━━━━━━━━━━", 10, y, COLOR_YELLOW);
    renderText(canvas, "⚠ Soul System is Inactive", 10, y, COLOR_YELLOW);
    renderText(canvas, "━━━━━━━━━━━━━━━━━━━━━━━━━━", 10, y, COLOR_YELLOW);
  }

  // Render all fields with fallback values
  renderText(canvas, "Soul State: " + formatSoulState(...), 10, y, COLOR_GRAY);
  renderText(canvas, "Soul Level: " + formatSoulLevel(...), 10, y, COLOR_GRAY);
  // ... etc
}
```
- Uses Modern UI `Paint.get()` for efficient text rendering
- Color-coded display (white titles, gray data, yellow warnings)
- Returns next Y position for proper line spacing

#### ReservedTab
Placeholder for future expansion (Phase 8+):
- Displays "Coming Soon" message
- Tab is visible but disabled (`isEnabled() = false`)
- Easy to replace with actual implementation

**Usage:**
```java
tabs.add(new ReservedTab("reserved_1", "Reserved"));
tabs.add(new ReservedTab("reserved_2", "Reserved"));
```

**Phase 7.1 Canvas Rendering:**
```java
@Override
public void renderContent(@NonNull Canvas canvas, int mouseX, int mouseY, float partialTick) {
  renderText(canvas, "Coming Soon", 10, y, COLOR_WHITE);
  renderText(canvas, "Reserved for Future Use", 10, y, COLOR_GRAY);
  renderText(canvas, "This tab will be implemented", 10, y, COLOR_GRAY);
  renderText(canvas, "in a future phase.", 10, y, COLOR_GRAY);
}
```
- Renders placeholder content using Canvas API
- Prevents user confusion with clear messaging

## Data Flow

### Panel Opening Flow
```
User: /hundaopanel
       ↓
ModernUIClientCommands.openHunDaoPanel()
       ↓
Create HunDaoModernPanelFragment
       ↓
MuiModApi.createScreen(fragment)
       ↓
Panel renders on screen
```

### Tab Rendering Flow (Phase 7.1)
```
Fragment.onCreateView()
       ↓
Initialize tabs (Soul Overview + 2 Reserved)
       ↓
Build tab navigation bar
       ↓
Create CanvasContentView
       ↓
setActiveTab(tabs.get(0)) → invalidate()
       ↓
CanvasContentView.onDraw()
       ↓
activeTab.renderContent(canvas, ...)
       ↓
Query HunDaoClientState for data
       ↓
Render text with Paint.get() + canvas.drawText()
```

### Tab Switch Flow (Phase 7.1)
```
User clicks tab button
       ↓
switchTab(index)
       ↓
contentView.setActiveTab(tabs.get(index))
       ↓
contentView.invalidate()
       ↓
CanvasContentView.onDraw()
       ↓
New tab's renderContent() executed
       ↓
Content refreshes immediately
```

## Usage

### Opening the Panel

**Via Command:**
```
/hundaopanel
```

**Via Code:**
```java
// From client-side code
ModernUIClientCommands.openHunDaoPanelViaHotkey();

// Or directly
Minecraft mc = Minecraft.getInstance();
mc.execute(() -> {
  var fragment = new HunDaoModernPanelFragment();
  var screen = MuiModApi.get().createScreen(fragment);
  mc.setScreen(screen);
});
```

### Adding a New Tab

1. Create a class implementing `IHunDaoPanelTab`
2. Implement required methods (`getId`, `getTitle`, `renderContent`, etc.)
3. Add to tab list in `HunDaoModernPanelFragment`:
```java
tabs.add(new YourNewTab());
```

**Example:**
```java
public class SoulFlameTab implements IHunDaoPanelTab {
  @Override
  public String getId() { return "soul_flame"; }

  @Override
  public String getTitle() { return "Soul Flame"; }

  @Override
  public void renderContent(Canvas canvas, int mouseX, int mouseY, float partialTick) {
    // Render soul flame details
  }
}
```

### Data Integration

Panel tabs query `HunDaoClientState` for display data:
```java
HunDaoClientState state = HunDaoClientState.instance();
UUID playerId = Minecraft.getInstance().player.getUUID();

// Query soul panel data
Optional<SoulState> soulState = state.getSoulState(playerId);
int soulLevel = state.getSoulLevel(playerId);
Optional<SoulRarity> soulRarity = state.getSoulRarity(playerId);
Map<String, Object> attributes = state.getSoulAttributes(playerId);
boolean isActive = state.isSoulSystemActive(playerId);
```

## Phase 7 Achievements

1. ✅ Extensible Tab architecture (`IHunDaoPanelTab`)
2. ✅ Modern UI Fragment integration (`HunDaoModernPanelFragment`)
3. ✅ Soul Overview Tab with complete data display
4. ✅ Two Reserved Tabs for future expansion
5. ✅ Command entry point (`/hundaopanel`)
6. ✅ Fallback strategy for missing data
7. ✅ Type-safe enums (`SoulState`, `SoulRarity`)

## Phase 7.1 Achievements (Production Hardening)

1. ✅ **CanvasContentView implementation** - Custom view with proper Canvas rendering
2. ✅ **Tab switching fully functional** - `switchTab()` triggers immediate content refresh
3. ✅ **`renderContent()` called properly** - Canvas API integration complete
4. ✅ **Inactive state warning UI** - Yellow warning block with ⚠ symbol
5. ✅ **Real data rendering** - All soul fields displayed (state, level, rarity, max, attributes)
6. ✅ **Zero TODO placeholders** - All implementation complete
7. ✅ **Color-coded UI** - White titles, gray data, yellow warnings, red errors
8. ✅ **Comprehensive documentation** - Report, smoke test, and updated READMEs

**Status:** Production ready, meets all quality requirements

## Future Enhancements (Phase 8+)

### Additional Tabs
- **Soul Flame Tab:** DoT management, stack visualization
- **Soul Beast Tab:** Transformation stats, activation history
- **Gui Wu Tab:** Fog details, coverage area
- **Settings Tab:** Panel preferences, data export

### Interactive Features
- Refresh button to force data sync
- Clickable attributes for detailed tooltips
- Scroll bars for long attribute lists
- Search/filter for attributes

### Visual Enhancements
- ✅ Custom Canvas rendering (Phase 7.1 complete)
- Icon sprites for rarities and states
- Color themes (dark/light mode)
- Smooth transitions and animations
- Custom fonts and text styling

### Data Synchronization
- Server→Client payloads for real-time updates
- Auto-refresh on data change events
- Lazy loading for expensive data

### Internationalization
- i18n keys for all text (`lang/en_us.json`, `lang/zh_cn.json`)
- Localized enums and messages
- RTL support for Arabic/Hebrew

## Technical Notes

### Modern UI Integration
- Uses `icyllis.modernui` framework
- Fragment-based architecture (similar to Android)
- Declarative UI with Views (TextView, Button, LinearLayout)
- Event-driven interaction (OnClickListener)

### Performance Considerations
- Panel only rendered when open (no background overhead)
- Data queries are lightweight (HashMap lookups)
- No heavy computations in render loop
- Future: implement lazy loading for attributes

### Compatibility
- Requires Modern UI library (already in project)
- Client-side only (no server dependencies)
- NeoForge 1.21.3+ compatible

## Related Documentation

- `../README.md` - Client state management
- `../ui/README.md` - HUD and notifications
- `../../docs/Phase7_Plan.md` - Phase 7 implementation plan
- `../../docs/Phase7_Report.md` - Phase 7 completion report
- `../../docs/Phase7.1_Plan.md` - Phase 7.1 production hardening plan
- `../../docs/Phase7.1_Report.md` - Phase 7.1 completion report
- `../../docs/Phase7.1_Smoke_Test.md` - Phase 7.1 testing procedures
