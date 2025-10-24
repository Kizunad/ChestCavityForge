package net.tigereye.chestcavity.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.food.FoodData;
import org.junit.jupiter.api.Test;

class OrganHealingEventsTest {

  @Test
  void computeRefundRestoresSaturationPath() {
    FoodData baseline = new FoodData();
    baseline.setFoodLevel(20);
    baseline.setSaturation(5.0f);

    OrganHealingEvents.HungerSnapshot snapshot =
        OrganHealingEvents.HungerSnapshot.capture(baseline);

    FoodData after = new FoodData();
    after.setFoodLevel(20);
    after.setSaturation(4.0f);
    after.addExhaustion(2.0f);

    OrganHealingEvents.HungerRefund refund = snapshot.computeRefund(after);
    assertEquals(0, refund.foodLevels());
    assertEquals(1.0f, refund.saturation(), 1.0E-4f);
    assertEquals(2.0f, refund.exhaustion(), 1.0E-4f);
  }

  @Test
  void computeRefundRestoresHungerPath() {
    FoodData baseline = new FoodData();
    baseline.setFoodLevel(20);
    baseline.setSaturation(0.0f);

    OrganHealingEvents.HungerSnapshot snapshot =
        OrganHealingEvents.HungerSnapshot.capture(baseline);

    FoodData after = new FoodData();
    after.setFoodLevel(19);
    after.setSaturation(0.0f);
    after.addExhaustion(2.0f);

    OrganHealingEvents.HungerRefund refund = snapshot.computeRefund(after);
    assertEquals(1, refund.foodLevels());
    assertEquals(0.0f, refund.saturation(), 1.0E-4f);
    assertEquals(2.0f, refund.exhaustion(), 1.0E-4f);
  }

  @Test
  void computeRefundNoChangeWhenStatesMatch() {
    FoodData baseline = new FoodData();
    baseline.setFoodLevel(19);
    baseline.setSaturation(2.5f);
    baseline.addExhaustion(1.25f);

    OrganHealingEvents.HungerSnapshot snapshot =
        OrganHealingEvents.HungerSnapshot.capture(baseline);

    FoodData after = new FoodData();
    after.setFoodLevel(19);
    after.setSaturation(2.5f);
    after.addExhaustion(1.25f);

    OrganHealingEvents.HungerRefund refund = snapshot.computeRefund(after);
    assertTrue(refund.isEmpty());
  }

  @Test
  void computeRefundIgnoresImprovements() {
    FoodData baseline = new FoodData();
    baseline.setFoodLevel(16);
    baseline.setSaturation(3.0f);
    baseline.addExhaustion(4.0f);

    OrganHealingEvents.HungerSnapshot snapshot =
        OrganHealingEvents.HungerSnapshot.capture(baseline);

    FoodData after = new FoodData();
    after.setFoodLevel(17);
    after.setSaturation(4.5f);
    after.addExhaustion(2.0f);

    OrganHealingEvents.HungerRefund refund = snapshot.computeRefund(after);
    assertEquals(0, refund.foodLevels());
    assertEquals(0.0f, refund.saturation(), 1.0E-4f);
    assertEquals(0.0f, refund.exhaustion(), 1.0E-4f);
  }
}
