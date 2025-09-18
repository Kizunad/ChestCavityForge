# ChestCavityForge 1.18.2 Baseline

Goal: capture the current Forge 1.18.2 implementation so we can port feature parity to NeoForge 1.21.1.

## Build & runtime stack
- Forge 1.18.2-40.1.60 (Java 17 toolchain via Gradle toolchains)
- ForgeGradle 6.x, Mixingradle 0.7 snapshot
- Required library: `cloth_config` ≥ 6.2.62 (Autoconfig-backed config screens)
- Mixins defined in `chestcavity.mixins.json` (`required`, Java 17 compatibility, refmap `chestcavity.refmap.json`)
- Dev environment expects `annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'`

## Project layout highlights
- Entry point: `net.tigereye.chestcavity.ChestCavity` (`@Mod`), registers config, deferred registries, listeners, screens, keybinds.
- Packages: `chestcavities` (organ inventories/types), `config`, `enchantments`, `interfaces`, `items`, `listeners`, `mob_effect`, `network`, `recipes`, `registration`, `ui`, `util`, `mixin`, `test`.
- Resources: `assets/chestcavity` (lang, textures, models), data packs in `data/chestcavity` plus compat namespaces (`antropophagy`, `nourish`, `c`).

## Registries & items
- Deferred registers in `registration/` cover items, containers, status effects, enchantments, recipes, food components, tags, organ score tables, listeners, keybindings.
- `CCItems` defines the bulk of gameplay content: surgical tools (cleavers, chest opener), human/animal organ items, meat/sausage variants, toxic variants, and special organs (creeper appendix, venom gland, etc.).
- `CCContainers` exposes `ChestCavityScreenHandler`; `CCStatusEffects`, `CCEnchantments`, `CCRecipes`, `CCOrganScores` define supporting content for organ mechanics.

## Chest cavity system
- Core logic under `chestcavities/`:
  - `ChestCavityInventory`, `ChestCavityInstance`, and `ChestCavityType` manage organ slots, score calculations, and organ compatibility tags.
  - `chestcavities/types` & `/types/json` describe predefined cavity templates (per-entity organ loadouts) and JSON loaders.
  - `organ` subpackage (`OrganData`, `OrganManager`, etc.) handles organ metadata, loot, and scoring.
- `interfaces/` exposes capability-style hooks (`ChestCavityEntity`, `CCHungerManagerInterface`, `CCStatusEffect` etc.) used by mixins to attach cavity data.

## Networking & hotkeys
- `NetworkHandler` creates a Forge `SimpleChannel` with protocol version `2.16.4`; packets: `ChestCavityUpdatePacket`, `OrganDataPacket`, `ChestCavityHotkeyPacket`.
- `NetworkUtil` dispatches S2C updates via `PacketDistributor.PLAYER` and routes hotkey handling.
- `registration/CCNetworkingPackets` declares packet IDs for use with `NetworkHandler` (currently thin wrapper).

## UI & user interaction
- `ui/ChestCavityScreen` + `ChestCavityScreenHandler` render/manage the organ inventory using a custom menu type.
- `listeners/KeybindingClientListeners` plus `registration/CCKeybindings` register hotkeys allowing players to open the cavity or trigger organ abilities.

## Event & gameplay listeners
- `listeners/` hosts organ behavior hooks: activation, tick effects, food effects, loot injection, damage hooks, etc. `CCListeners.register()` only wires `OrganActivationListeners` because other listener classes are called directly via callbacks in `ChestCavityUtil` (`OrganTickCallback`, `OrganUpdateCallback`, status-effect hooks) rather than Forge events. The inline comment (“todo: convert back to events”) signals a future cleanup task.
- Callbacks (`OrganTickCallback`, `OrganFoodCallback`, etc.) provide extension points consumed by listeners/mixins; expect to revisit when adapting to NeoForge event bus.

## Mixins & game integration
- Mixins applied to core gameplay classes (`Entity`, `LivingEntity`, `HungerManager`, `StatusEffect`, `StatusEffectInstance`, `EnchantmentHelper`, `Item`, `PotionEntity`, `Slot`, resource reload) to integrate cavity mechanics with vanilla systems.
- `MixinLivingEntity` contains numerous inner classes targeting specific mob subclasses (Cow, Creeper, Mob, Player, ServerPlayer, Sheep, Wither) — expect significant API drift when moving to 1.21.1.
- `MixinHanshake` handles join handshake tweaks (note spelling) — investigate relevance under NeoForge login pipeline.

## Data assets
- **Chest cavity types (`data/chestcavity/types/.../*.json`)**: each file provides `defaultChestCavity` (list of `{item, position, count?}`), optional `baseOrganScores`, `exceptionalOrgans` (with `ingredient` predicates mapping to organ score overrides), `forbiddenSlots`, plus flags (`bossChestCavity`, `playerChestCavity`, `dropRateMultiplier`). Parsed by `ChestCavityTypeSerializer` into `GeneratedChestCavityType`.
- **Entity assignments (`data/chestcavity/entity_assignment/.../*.json`)**: map entity IDs to type JSON resources via `{ "chestcavity": "namespace:types/.../file.json", "entities": [ "minecraft:entity" ] }` handled by `GeneratedChestCavityAssignmentManager`.
- **Organ definitions (`data/chestcavity/organs/.../*.json`)**: each entry specifies `{ "itemID": "namespace:item", "organScores": [{"id": "namespace:score", "value": "float"}] }`, consumed by `OrganSerializer`/`OrganManager`.
- Compat folders:
  - `data/antropophagy` – integration loot/recipes with Antropophagy mod.
  - `data/nourish` – nutrition compat data.
  - `data/c` – shared tags (likely cross-mod organ categories).
- Client assets for screens/models in `assets/chestcavity`.

## Configuration surface
- `config/CCConfig` (AutoConfig) exposes numerous tuning knobs: organ stats, rejection behavior, loot odds, cooldowns, integration toggles (Backrooms, Bewitchment, Biome Makeover, etc.).
- Config categories (`core`, `more`, `cooldown`, `integration`) inform UI grouping and should survive the migration.

## External integrations & optional deps
- Dependencies commented in `build.gradle` hint at optional compat mods (Aquaculture, Alex's Mobs, Biomes O' Plenty, JEI, etc.) with required libraries (Architectury, Citadel, Placebo, etc.). Keep notes for optional feature testing.
- Cloth Config + AutoConfig provide config UI; ensure NeoForge environment still supports them or plan migration.

## Gaps / migration questions
- Determine whether to keep direct-callback pattern or reinstate NeoForge events for listeners during the port.
- Assess how `SimpleChannel` usage maps to NeoForge's networking API (likely switch to NeoForge `ChannelHandler`).
- Enumerate mixin targets and evaluate method signature changes between 1.18.2 and 1.21.1.
- Review JSON organ definitions (under `chestcavities/types/json`) to understand schema and ensure data-driven behavior survives loader changes.
- Verify all resource locations and registries follow naming conventions compatible with modern data gen/NeoForge expectations.

Update this document as you inventory subsystems or surface migration blockers.
