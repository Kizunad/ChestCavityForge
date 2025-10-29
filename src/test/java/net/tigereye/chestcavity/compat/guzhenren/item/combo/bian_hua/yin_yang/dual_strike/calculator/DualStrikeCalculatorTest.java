package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.dual_strike.calculator;

import net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.dual_strike.tuning.DualStrikeTuning;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("DualStrikeCalculator Tests")
class DualStrikeCalculatorTest {

    private static final double DELTA = 1e-6;

    @Test
    @DisplayName("Bonus damage is calculated from the weaker of the two attacks")
    void testCalculateBonusDamage_YinWeaker() {
        // Arrange
        double yinAttack = 10.0;
        double yangAttack = 20.0;
        double expected = 10.0 * DualStrikeTuning.DAMAGE_FACTOR;

        // Act
        double result = DualStrikeCalculator.calculateBonusDamage(yinAttack, yangAttack);

        // Assert
        assertEquals(expected, result, DELTA, "Damage should be based on the weaker Yin attack");
    }

    @Test
    @DisplayName("Bonus damage is calculated from the weaker of the two attacks")
    void testCalculateBonusDamage_YangWeaker() {
        // Arrange
        double yinAttack = 20.0;
        double yangAttack = 10.0;
        double expected = 10.0 * DualStrikeTuning.DAMAGE_FACTOR;

        // Act
        double result = DualStrikeCalculator.calculateBonusDamage(yinAttack, yangAttack);

        // Assert
        assertEquals(expected, result, DELTA, "Damage should be based on the weaker Yang attack");
    }

    @Test
    @DisplayName("Bonus damage is zero if one attack is zero")
    void testCalculateBonusDamage_OneZero() {
        // Arrange
        double yinAttack = 0.0;
        double yangAttack = 20.0;

        // Act
        double result = DualStrikeCalculator.calculateBonusDamage(yinAttack, yangAttack);

        // Assert
        assertEquals(0.0, result, DELTA, "Damage should be zero if the weaker attack is zero");
    }

    @Test
    @DisplayName("Bonus damage is zero if both attacks are zero")
    void testCalculateBonusDamage_BothZero() {
        // Arrange
        double yinAttack = 0.0;
        double yangAttack = 0.0;

        // Act
        double result = DualStrikeCalculator.calculateBonusDamage(yinAttack, yangAttack);

        // Assert
        assertEquals(0.0, result, DELTA, "Damage should be zero if both attacks are zero");
    }

    @Test
    @DisplayName("Bonus damage is calculated correctly with equal attacks")
    void testCalculateBonusDamage_Equal() {
        // Arrange
        double yinAttack = 15.0;
        double yangAttack = 15.0;
        double expected = 15.0 * DualStrikeTuning.DAMAGE_FACTOR;

        // Act
        double result = DualStrikeCalculator.calculateBonusDamage(yinAttack, yangAttack);

        // Assert
        assertEquals(expected, result, DELTA, "Damage should be calculated correctly when attacks are equal");
    }
}
