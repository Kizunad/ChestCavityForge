package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.rarity;

/**
 * Encapsulates all numeric bonuses derived from a soul rarity tier.
 *
 * <p>Units:
 *
 * <ul>
 *   <li>{@code movementSpeedMultiplier}: multiplicative factor applied to MOVEMENT_SPEED.
 *   <li>{@code soulBeastDamageReduction}: percentage (0–1) removed from incoming damage while in
 *       soul beast form.
 *   <li>{@code attackMultiplier}: multiplicative factor for melee/fist damage.
 *   <li>{@code hunPoRegenPerSecond}: flat hun po recovered per second (默认 0)。
 *   <li>{@code hunPoMaxBonus}: additive hun po capacity bonus when not in soul beast form.
 * </ul>
 */
public record HunDaoSoulRarityBonuses(
    double movementSpeedMultiplier,
    double soulBeastDamageReduction,
    double attackMultiplier,
    double hunPoRegenPerSecond,
    double hunPoMaxBonus) {

  /**
   * Returns the clamped movement speed multiplier (bonus capped at +1.0 to avoid runaway speed).
   */
  @Override
  public double movementSpeedMultiplier() {
    double increase = Math.max(0.0D, movementSpeedMultiplier - 1.0D);
    return 1.0D + Math.min(increase, 1.0D);
  }
}
