package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

/**
 * {@link Scaling} 的默认实现。
 */
public final class StandardScaling implements Scaling {

  public static final StandardScaling INSTANCE = new StandardScaling();

  @Override
  public double clamp(double value, double min, double max) {
    if (min > max) {
      double t = min;
      min = max;
      max = t;
    }
    return Math.max(min, Math.min(max, value));
  }

  @Override
  public double scaleByDao(double base, double dao, double coeff, double min, double max) {
    double scaled = base * (1.0 + coeff * dao);
    return clamp(scaled, min, max);
  }

  @Override
  public double cooldown(double baseSeconds, double cdr, double minSeconds, double cdrCap) {
    double cap = clamp(cdrCap, 0.0, 0.99);
    double c = clamp(cdr, 0.0, cap);
    double cd = baseSeconds * (1.0 - c);
    return Math.max(minSeconds, cd);
  }

  @Override
  public double cooldownTicks(double baseTicks, double cdr, double minTicks, double cdrCap) {
    double cdSeconds = cooldown(ticksToSeconds((long) Math.round(baseTicks)), cdr,
        ticksToSeconds((long) Math.round(minTicks)), cdrCap);
    return secondsToTicks(cdSeconds);
  }

  @Override
  public double withDaoCdr(
      double baseSeconds, double dao, double daoCoeff, double extraCdr, double minSeconds, double cdrCap) {
    double totalCdr = extraCdr + daoCoeff * dao;
    return cooldown(baseSeconds, totalCdr, minSeconds, cdrCap);
  }

  @Override
  public long secondsToTicks(double seconds) {
    return Math.round(seconds * 20.0);
  }

  @Override
  public double ticksToSeconds(long ticks) {
    return ticks / 20.0;
  }
}

