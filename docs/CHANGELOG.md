# Changelog

## 1.19.x - 41.0

- **[Refactor][bing_xue_dao] De-Ledger-ization**:
  - Active abilities for BingJiGu and ShuangXiGu now calculate damage and effects based on `(1 + daohen)`.
  - Cooldowns are now linearly reduced by `liupai_bingxuedao` experience (down to a 20t minimum).
  - Passives have been consolidated into a single entry point per organ and now run through the `PassiveBus`.
  - `ActivationHookRegistry` now uses the `SkillEffectBus` to snapshot `daohen` and `exp` values in real-time.
