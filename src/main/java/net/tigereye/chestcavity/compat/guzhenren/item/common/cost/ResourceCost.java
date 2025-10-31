package net.tigereye.chestcavity.compat.guzhenren.item.common.cost;

public record ResourceCost(
    double zhenyuan, double jingli, double hunpo, double niantou, int hunger, float health) {
  public boolean isZero() {
    return zhenyuan <= 0.0D
        && jingli <= 0.0D
        && hunpo <= 0.0D
        && niantou <= 0.0D
        && hunger <= 0
        && health <= 0.0f;
  }
}
