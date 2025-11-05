# Changelog

## 2025-11-06
- [flyingsword][P4] 冷却与资源一致性（阶段性完成）
  - 攻击冷却切换为基于 owner 的 Attachment 存储：`FLYING_SWORD_COOLDOWN`
  - 统一入口：`FlyingSwordCooldownOps`（get/set/isReady/tickDown），调用方无需改动
  - Upkeep 覆盖：`UpkeepCheck.finalCost/skipConsumption` 生效，使用 `consumeFixedUpkeep`
  - 文档：更新 P3/P4 阶段现状与“不能做/要做”清单

## 1.19.x - 41.0

- **[Refactor][bing_xue_dao] De-Ledger-ization**:
  - Active abilities for BingJiGu and ShuangXiGu now calculate damage and effects based on `(1 + daohen)`.
  - Cooldowns are now linearly reduced by `liupai_bingxuedao` experience (down to a 20t minimum).
  - Passives have been consolidated into a single entry point per organ and now run through the `PassiveBus`.
  - `ActivationHookRegistry` now uses the `SkillEffectBus` to snapshot `daohen` and `exp` values in real-time.
