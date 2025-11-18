# Hun Dao Modern UI

**Status:** Phase 7 (Complete)

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
- Delegates content rendering to active tab
- Provides close button and panel chrome

**Key Features:**
- Dynamic tab list (3 tabs by default, easily extensible)
- Active tab highlighting
- Tab switching via button clicks
- Integration with Minecraft's Modern UI system

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
  → Display "Soul System is Inactive"
  → Show placeholders for all fields
  → No crash, no missing data
```

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

### Tab Rendering Flow
```
Fragment.onCreateView()
       ↓
Initialize tabs (Soul Overview + 2 Reserved)
       ↓
Build tab navigation bar
       ↓
Render active tab content
       ↓
Query HunDaoClientState for data
       ↓
Format and display in TextView
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
- Custom Canvas rendering (replace TextView)
- Icon sprites for rarities and states
- Color themes (dark/light mode)
- Smooth transitions and animations

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
