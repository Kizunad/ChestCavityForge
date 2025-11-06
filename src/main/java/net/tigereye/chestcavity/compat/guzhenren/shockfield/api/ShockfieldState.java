package net.tigereye.chestcavity.compat.guzhenren.shockfield.api;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;

/**
 * Shockfield 实例的运行时状态。
 *
 * <p>包含波源位置、振幅、周期、相位等核心参数。
 */
public final class ShockfieldState {
  private final WaveId waveId;
  private final UUID ownerId;
  private final Vec3 center;
  private final long birthTick;
  private double amplitude;
  private double period;
  private double phase;
  private double radius;

  public ShockfieldState(
      WaveId waveId,
      UUID ownerId,
      Vec3 center,
      long birthTick,
      double amplitude,
      double period,
      double phase) {
    this.waveId = Objects.requireNonNull(waveId, "waveId");
    this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
    this.center = Objects.requireNonNull(center, "center");
    this.birthTick = birthTick;
    this.amplitude = amplitude;
    this.period = period;
    this.phase = phase;
    this.radius = 0.0;
  }

  public WaveId getWaveId() {
    return waveId;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public Vec3 getCenter() {
    return center;
  }

  public long getBirthTick() {
    return birthTick;
  }

  public double getAmplitude() {
    return amplitude;
  }

  public void setAmplitude(double amplitude) {
    this.amplitude = amplitude;
  }

  public double getPeriod() {
    return period;
  }

  public void setPeriod(double period) {
    this.period = period;
  }

  public double getPhase() {
    return phase;
  }

  public void setPhase(double phase) {
    this.phase = phase;
  }

  public double getRadius() {
    return radius;
  }

  public void setRadius(double radius) {
    this.radius = radius;
  }

  public long getAge(long currentTick) {
    return currentTick - birthTick;
  }

  public double getAgeSeconds(long currentTick) {
    return getAge(currentTick) / 20.0;
  }

  @Override
  public String toString() {
    return String.format(
        "Shockfield[%s, amp=%.3f, period=%.2f, radius=%.1f]",
        waveId, amplitude, period, radius);
  }
}
