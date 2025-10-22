package net.tigereye.chestcavity.guscript.fx.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only helpers for registering FX event hooks such as screen shake. */
public final class FxClientHooks {

  private static final List<ScreenShakeInstance> SHAKES = new ArrayList<>();
  private static final RandomSource RANDOM = RandomSource.create();
  private static boolean registered = false;

  private FxClientHooks() {}

  public static void init() {
    if (registered) {
      return;
    }
    registered = true;
    NeoForge.EVENT_BUS.addListener(FxClientHooks::onClientTick);
    NeoForge.EVENT_BUS.addListener(FxClientHooks::onComputeCameraAngles);
  }

  public static void addScreenShake(double intensity, int durationTicks) {
    if (intensity <= 0.0D || durationTicks <= 0) {
      return;
    }
    SHAKES.add(new ScreenShakeInstance(intensity, durationTicks));
  }

  private static void onClientTick(ClientTickEvent.Post event) {
    Iterator<ScreenShakeInstance> iterator = SHAKES.iterator();
    while (iterator.hasNext()) {
      ScreenShakeInstance instance = iterator.next();
      instance.remainingTicks--;
      if (instance.remainingTicks <= 0) {
        iterator.remove();
      }
    }
  }

  private static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
    if (SHAKES.isEmpty()) {
      return;
    }
    double yaw = 0.0D;
    double pitch = 0.0D;
    double roll = 0.0D;
    for (ScreenShakeInstance instance : SHAKES) {
      double strength = instance.intensity;
      yaw += (RANDOM.nextDouble() - 0.5D) * 2.0D * strength;
      pitch += (RANDOM.nextDouble() - 0.5D) * 2.0D * strength;
      roll += (RANDOM.nextDouble() - 0.5D) * strength;
    }
    event.setYaw((float) (event.getYaw() + yaw));
    event.setPitch((float) (event.getPitch() + pitch));
    event.setRoll((float) (event.getRoll() + roll));
  }

  private static final class ScreenShakeInstance {
    private final double intensity;
    private int remainingTicks;

    private ScreenShakeInstance(double intensity, int duration) {
      this.intensity = intensity;
      this.remainingTicks = duration;
    }
  }
}
