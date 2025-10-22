package net.tigereye.chestcavity.soul.fakeplayer.brain.debug;

/** Consumer of debug telemetry emitted by the brain runtime. */
@FunctionalInterface
public interface BrainTelemetrySink {

  void publish(BrainDebugEvent event);
}
