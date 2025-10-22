package net.tigereye.chestcavity.guscript.fx;

import net.minecraft.world.phys.Vec3;

/** Additional parameters supplied when dispatching an FX event from the server. */
public record FxEventParameters(Vec3 originOffset, Vec3 targetOffset, float intensity) {

  public static final FxEventParameters DEFAULT = new FxEventParameters(Vec3.ZERO, Vec3.ZERO, 1.0F);

  public FxEventParameters {
    originOffset = originOffset == null ? Vec3.ZERO : originOffset;
    targetOffset = targetOffset == null ? Vec3.ZERO : targetOffset;
    intensity = Float.isNaN(intensity) || intensity <= 0.0F ? 1.0F : intensity;
  }
}
