# Hun Dao FX

**Status:** Phase 5 (Complete)

## Purpose
Centralized FX (visual and audio effects) subsystem for Hun Dao, implementing data-driven effect dispatch through a router-registry architecture.

## Architecture

### Core Components

#### HunDaoFxRouter
Central dispatcher for all Hun Dao audio/visual effects. Routes FX requests to appropriate subsystems:
- Sound events → Minecraft SoundSystem
- Particle effects → FxEngine (ChestCavity's centralized FX system)

**Key Methods:**
- `dispatch(ServerLevel, Entity, ResourceLocation)` - One-shot FX at entity
- `dispatch(ServerLevel, Vec3, ResourceLocation)` - One-shot FX at position
- `dispatchContinuous(ServerLevel, Entity, ResourceLocation, int)` - Continuous FX with duration
- `stop(ServerLevel, Entity, ResourceLocation)` - Stop continuous FX early

#### HunDaoFxRegistry
Template registry for FX metadata. Stores sound events, particle templates, durations, and behavioral flags for each FX type.

**Template Fields:**
- `soundEvent` - Optional sound to play
- `soundVolume/soundPitch` - Audio parameters
- `continuous` - One-shot vs ambient effect flag
- `durationTicks` - Default duration for continuous effects
- `particleTemplate` - FxEngine particle template ID
- `category` - FX category for filtering

**Registration:**
```java
HunDaoFxRegistry.register(
    HunDaoFxDescriptors.SOUL_FLAME_TICK,
    HunDaoFxRegistry.FxTemplate.builder()
        .sound(CCSoundEvents.CUSTOM_SOULBEAST_DOT.get(), 0.6F, 1.0F)
        .continuous(true)
        .duration(5)
        .particles("soul_flame_tick")
        .category(FxCategory.SOUL_FLAME)
        .build());
```

#### HunDaoFxDescriptors
Declares all available FX IDs as `ResourceLocation` constants. Categorizes effects by type (SOUL_FLAME, SOUL_BEAST, GUI_WU, HUN_PO).

**Available FX:**
- **Soul Flame:** `SOUL_FLAME_TICK`, `SOUL_FLAME_IGNITE`, `SOUL_FLAME_EXPIRE`
- **Soul Beast:** `SOUL_BEAST_ACTIVATE`, `SOUL_BEAST_AMBIENT`, `SOUL_BEAST_DEACTIVATE`, `SOUL_BEAST_HIT`
- **Gui Wu:** `GUI_WU_ACTIVATE`, `GUI_WU_AMBIENT`, `GUI_WU_DISSIPATE`
- **Hun Po:** `HUN_PO_LEAK`, `HUN_PO_RECOVERY`, `HUN_PO_LOW_WARNING`

#### HunDaoFxInit
Initialization class that registers all FX templates on mod startup. Called during FMLCommonSetupEvent.

#### HunDaoFxTuning
Data-driven tuning parameters for all FX (volumes, pitches, particle counts, intervals, radii).

**Tuning Classes:**
- `SoulFlame` - Soul flame DoT parameters
- `SoulBeast` - Soul beast transformation parameters
- `GuiWu` - Gui wu fog parameters
- `HunPoDrain` - Hun po resource effect parameters
- `General` - General FX constants

### Legacy Components

#### HunDaoSoulFlameFx
Legacy soul flame effect utility. Retained for backward compatibility with DoTEngine integration. Will be gradually migrated to use HunDaoFxRouter in Phase 6+.

## Data Flow

```
Server → HunDaoFxOps (interface)
         ↓
       HunDaoFxOpsImpl
         ↓
       HunDaoFxRouter.dispatch()
         ↓
   ┌────┴────┐
   ↓         ↓
SoundSystem  FxEngine → Client particles/animations
```

## Usage Examples

### Playing One-Shot FX
```java
// Via HunDaoFxRouter directly (server-side)
ServerLevel level = ...;
Player player = ...;
HunDaoFxRouter.dispatch(level, player, HunDaoFxDescriptors.SOUL_BEAST_ACTIVATE);
```

### Playing Continuous FX
```java
// Soul flame DoT (5 seconds)
HunDaoFxRouter.dispatchContinuous(
    level,
    target,
    HunDaoFxDescriptors.SOUL_FLAME_TICK,
    5 * 20); // 5 seconds in ticks
```

### Via HunDaoFxOps Interface
```java
// Preferred approach for behavior classes
HunDaoRuntimeContext ctx = ...;
ctx.fx().playSoulBeastActivate(player);
ctx.fx().playGuiWuActivate(player, position, radius);
```

## Extension Guide

### Adding New FX

1. **Declare FX ID** in `HunDaoFxDescriptors.java`:
   ```java
   public static final ResourceLocation MY_NEW_FX = ChestCavity.id("my_new_fx");
   ```

2. **Add Tuning Parameters** in `HunDaoFxTuning.java`:
   ```java
   public static final class MyNewFx {
     public static final float SOUND_VOLUME = 0.7F;
     public static final int PARTICLE_COUNT = 10;
   }
   ```

3. **Register Template** in `HunDaoFxInit.java`:
   ```java
   HunDaoFxRegistry.register(
       HunDaoFxDescriptors.MY_NEW_FX,
       HunDaoFxRegistry.FxTemplate.builder()
           .sound(CCSoundEvents.MY_SOUND.get(), MyNewFx.SOUND_VOLUME, 1.0F)
           .particles("my_particle_template")
           .build());
   ```

4. **Add Interface Method** (if needed) in `HunDaoFxOps.java` and implement in `HunDaoFxOpsImpl.java`

5. **Dispatch FX** from behavior classes via `HunDaoRuntimeContext.fx()`

## Design Principles

### KISS (Keep It Simple, Stupid)
- Only implements FX for existing Hun Dao features (no speculative Phase 6+ content)
- Simple registry-router pattern without complex state machines
- Minimal abstraction layers

### Data-Driven
- All FX parameters in `HunDaoFxTuning` (no magic numbers in FX dispatch code)
- Template-based FX registration (consistent structure for all effects)
- ResourceLocation-based FX IDs (standard Minecraft resource format)

### Server-Client Decoupling
- Server dispatches FX via `HunDaoFxRouter`
- FxEngine handles client-side rendering
- No client code dependencies in FX router

### Dependency Inversion
- Behavior classes depend on `HunDaoFxOps` interface, not concrete implementations
- FX router delegates to FxEngine (abstraction), not direct client calls

## Integration Points

### With DoTEngine
Soul flame DoT effects integrate with ChestCavity's DoTEngine for damage-over-time mechanics. `HunDaoSoulFlameFx` handles both DoT registration and FX dispatch.

### With FxEngine
All particle effects route through FxEngine's data-driven particle system. If FxEngine is disabled or FX templates are not registered, effects gracefully degrade (sound-only or silent).

### With Behavior Layer
Behavior classes access FX via `HunDaoRuntimeContext.fx()`, which returns `HunDaoFxOps` interface. This decouples behaviors from FX implementation details.

## Phase 5 Achievements

1. ✅ FX router-registry architecture established
2. ✅ Data-driven FX tuning parameters
3. ✅ All Hun Dao FX registered (soul flame, soul beast, gui wu, hun po)
4. ✅ Server-client FX dispatch decoupled from middleware
5. ✅ HunDaoFxOps interface expanded and implemented
6. ✅ FxEngine integration with fallback handling

## Future Work (Phase 6+)

- Migrate `HunDaoSoulFlameFx` to pure router-based dispatch
- Add client-side FX state tracking for ambient effects
- Implement FX customization via config/resource packs
- Add FX performance metrics and debugging tools
- Support dynamic FX templates loaded from data packs

## Related Documentation

- `runtime/README.md` - Runtime context and operations
- `client/README.md` - Client-side architecture
- `ui/README.md` - HUD and notification systems
- `calculator/README.md` - Combat calculation system
- `docs/Phase5_Plan.md` - Phase 5 implementation plan
- `docs/Phase5_Report.md` - Phase 5 completion report
