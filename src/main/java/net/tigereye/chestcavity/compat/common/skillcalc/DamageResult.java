package net.tigereye.chestcavity.compat.common.skillcalc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 伤害计算结果与明细。 */
public final class DamageResult {
  public static final class Entry {
    public enum Kind { MULTIPLY, ADD, CLAMP }
    private final String label;
    private final Kind kind;
    private final double value;
    private final double after;
    Entry(String label, Kind kind, double value, double after) {
      this.label = label; this.kind = kind; this.value = value; this.after = after;
    }
    public String label() { return label; }
    public Kind kind() { return kind; }
    public double value() { return value; }
    public double after() { return after; }
  }

  private final double base;
  private final double scaled;
  private final List<Entry> breakdown;
  private final double predictedAbsorptionSpent;
  private final double predictedHealthDamage;

  DamageResult(
      double base,
      double scaled,
      List<Entry> breakdown,
      double predictedAbsorptionSpent,
      double predictedHealthDamage) {
    this.base = base;
    this.scaled = scaled;
    this.breakdown = Collections.unmodifiableList(new ArrayList<>(breakdown));
    this.predictedAbsorptionSpent = predictedAbsorptionSpent;
    this.predictedHealthDamage = predictedHealthDamage;
  }

  public double base() { return base; }
  public double scaled() { return scaled; }
  public List<Entry> breakdown() { return breakdown; }
  public double predictedAbsorptionSpent() { return predictedAbsorptionSpent; }
  public double predictedHealthDamage() { return predictedHealthDamage; }
}

