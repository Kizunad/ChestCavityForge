# NeoForge 1.21.1 Migration Notes

## 2025-09-20 – Guzhenren Compatibility Work
- Created feature branch `feature/guzhenren-migration` off `neoforge-1.21.1` to hold 1.21.1-ready assets.
- Added custom cavity profiles at `data/chestcavity/types/compatibility/guzhenren/*.json` for wolves, bears, tigers, and hounds.
- Added per-entity assignment JSONs in `data/chestcavity/entity_assignment/guzhenren/**` that point each Guzhenren mob to the new cavity definitions, plus refreshed `non_gu_entities.md`.
- Verified `./gradlew clean compileJava` on the NeoForge toolchain (Gradle 8.8 + Java 21) to confirm the branch still builds after importing the resources.

- Expanded `guzhenren` assignments to cover sheep, cultivators, serpents, insects, and jiangshi entities with per-entity placeholder cavity types to enable future tuning.
- Removed the chest-equipment gate from ChestOpener/DefaultChestCavityType so NeoForge builds allow opening armored targets once health or ease-of-access conditions are met.
- Applied the same chest opener accessibility rule to GeneratedChestCavityType so JSON-defined entities no longer require empty chest slots.
- Added debug logging to ChestOpener blocked cases to capture UUID/health/ease scores while GG remains in debug mode.

## Follow-up TODOs
- Hook the new types into gameplay validation (summon each mob and inspect cavity contents) once gameplay-side systems compile for NeoForge.
- Replace placeholder cavity layouts with lore-faithful organs once gameplay balancing notes are available.
- Audit mixins and listeners for 1.21.1 API changes (loot contexts, DamageSources, CreativeTab rebuild) and document blockers as they surface.
- Update docs with results of future smoke tests (data generators, headless server launch) before merging back to `neoforge-1.21.1`.
