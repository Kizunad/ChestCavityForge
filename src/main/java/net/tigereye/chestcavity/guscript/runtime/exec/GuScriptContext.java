package net.tigereye.chestcavity.guscript.runtime.exec;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Runtime context passed to action execution, providing entity references and bridges. */
public interface GuScriptContext {

  Player performer();

  LivingEntity target();

  GuScriptExecutionBridge bridge();

  void addDamageMultiplier(double multiplier);

  void addFlatDamage(double amount);

  double damageMultiplier();

  double flatDamageBonus();

  default void exportDamageMultiplier(double amount) {}

  default void exportFlatDamage(double amount) {}

  default void enableModifierExports(boolean exportMultiplier, boolean exportFlat) {}

  default double exportedMultiplierDelta() {
    return 0.0;
  }

  default double exportedFlatDelta() {
    return 0.0;
  }

  default double directExportedMultiplier() {
    return 0.0;
  }

  default double directExportedFlat() {
    return 0.0;
  }

  default void exportTimeScaleMultiplier(double multiplier) {}

  default void exportTimeScaleFlat(double amount) {}

  default double directExportedTimeScaleMultiplier() {
    return 1.0;
  }

  default double directExportedTimeScaleFlat() {
    return 0.0;
  }

  default String resolveParameter(String name) {
    return null;
  }

  default double resolveParameterAsDouble(String name, double defaultValue) {
    return defaultValue;
  }

  default double applyDamageModifiers(double baseDamage) {
    double scaled = baseDamage * (1.0 + damageMultiplier());
    return Math.max(0.0, scaled + flatDamageBonus());
  }
}
