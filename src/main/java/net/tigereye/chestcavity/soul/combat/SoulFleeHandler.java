package net.tigereye.chestcavity.soul.combat;

/** A pluggable flee behaviour. */
public interface SoulFleeHandler {
  /** Try to perform a flee action this tick. Return true if applied. */
  boolean tryFlee(FleeContext ctx);
}
