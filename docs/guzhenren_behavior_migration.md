# Guzhenren Behaviour Migration Tracker

This document tracks the ongoing migration of Guzhenren organ behaviours onto the shared helpers under `util.behavior`. The current pass focuses on the Lei / Shi / Chou / Jiu organ families where manual linkage adjustments and ad-hoc timers were still present.

| Behaviour | Manual touch points (pre-migration) | Target helper(s) | Status |
|-----------|-------------------------------------|------------------|--------|
| `DianLiuguOrganBehavior` (雷道) | Direct `LinkageManager` access and `NBTCharge`-based charge counter | `LedgerOps`, `MultiCooldown` | ✅ Migrated via this pass |
| `ChouPiGuOrganBehavior` (臭道) | Direct `LinkageManager` access and bespoke `NBTCharge` interval timer | `LedgerOps`, `MultiCooldown` | ✅ Migrated via this pass |
| `JiuChongOrganBehavior` (食道) | Direct `LinkageManager` access plus static maps for mania / regen timers | `LedgerOps`, `MultiCooldown`, `AbsorptionHelper` (pre-existing) | ✅ Migrated via this pass |

Follow-up buckets (e.g., Yan / Xue lines) remain to be audited; they continue to use legacy linkage/timer logic and will be captured in subsequent passes.
