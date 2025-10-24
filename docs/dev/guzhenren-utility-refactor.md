Guzhenren Compat — Utility Refactor Plan (NeoForge 1.21.1)

Goals
- Consolidate repeated behavior logic into shared, well‑named helpers.
- Reduce copy/paste across item organ behaviors; improve readability and testability.
- Keep player/non‑player code paths explicit and safe.

What exists already
- Resource costs: `guzhenren/GuzhenrenResourceCostHelper` centralizes zhenyuan/jingli/HP fallback.
- NBT helpers: shared organ state via `OrganState`, charges via `util/NBTCharge`.
- Combat helpers: `compat/guzhenren/util/TrueDamageHelper`, `CombatEntityUtil`, etc.

New utilities (initial slice)
- `compat/guzhenren/util/behavior/AttributeOps`
  - `replaceTransient(attr, id, modifier)` — remove by id then add transient.
  - `removeById(attr, id)` — guard‑safe removal.
- `compat/guzhenren/util/behavior/TickOps`
  - `schedule(level, runnable, delayTicks)` — tiny delayed run helper.

First adoption
- `XieFeiguOrganBehavior` (血肺蛊):
  - Replaced local `replaceModifier` with `AttributeOps.replaceTransient`.
  - Replaced local `schedule` with `TickOps.schedule`.

Proposed next extractions (phased)
1) FX helpers
   - `FxOps.emit(level/player, fxId, intensity)` and `FxOps.emitGecko(...)` unify S2C dispatch.
2) EffectOps
   - Apply/remove common `MobEffectInstance` patterns (dur/amp/flags), area‑of‑effect loops with hostiles filters.
3) CooldownOps
   - Per‑player cooldown map with namespaced keys, safe reads/writes (backed by OrganState or controller when in flows).
4) TargetingOps
   - AABB collection with hostiles/ally predicates, line‑of‑sight checks, radius var scaling.
5) PlayerDispatch
   - `handlerPlayer/handlerNonPlayer` routing helpers to enforce split logic.

Principles
- Keep utilities dumb and side‑effect local; pass in all dependencies.
- Do not hide resource consumption; use `GuzhenrenResourceCostHelper` explicitly at call sites.
- Maintain clear logging and early‑return reasons, esp. in compat paths.

Migration cadence
- Migrate one behavior per PR to avoid merge pain.
- Add unit tests where feasible (JSON loaders, guard logic, simple attribute ops).

Notes
- No behavioral changes intended by utility adoption.
- If a behavior relies on unique timing or IDs, keep those in the behavior and pass through to the utility.
