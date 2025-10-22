package net.tigereye.chestcavity.guscript.runtime.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class FlowTimeScaleInvarianceTest {

  @Test
  void updateActionsFireSameCountRegardlessOfTimeScale() {
    AtomicInteger counter = new AtomicInteger();
    FlowEdgeAction countingAction =
        new FlowEdgeAction() {
          @Override
          public void apply(
              net.minecraft.world.entity.player.Player performer,
              net.minecraft.world.entity.LivingEntity target,
              FlowController controller,
              long gameTime) {
            counter.incrementAndGet();
          }

          @Override
          public String describe() {
            return "count";
          }
        };

    FlowTransition start =
        new FlowTransition(FlowTrigger.START, FlowState.CHARGING, List.of(), List.of(), 0);
    FlowTransition chargingComplete =
        new FlowTransition(FlowTrigger.AUTO, FlowState.CHARGED, List.of(), List.of(), 200);
    FlowTransition chargedComplete =
        new FlowTransition(FlowTrigger.AUTO, FlowState.COOLDOWN, List.of(), List.of(), 1);
    FlowTransition cooldownComplete =
        new FlowTransition(FlowTrigger.AUTO, FlowState.IDLE, List.of(), List.of(), 40);

    FlowProgram program =
        new FlowProgram(
            ResourceLocation.fromNamespaceAndPath("test", "invariance"),
            FlowState.IDLE,
            Map.of(
                FlowState.IDLE, new FlowStateDefinition(List.of(), List.of(), List.of(start)),
                FlowState.CHARGING,
                    new FlowStateDefinition(
                        List.of(),
                        List.of(),
                        List.of(chargingComplete),
                        List.of(countingAction),
                        20),
                FlowState.CHARGED,
                    new FlowStateDefinition(List.of(), List.of(), List.of(chargedComplete)),
                FlowState.COOLDOWN,
                    new FlowStateDefinition(List.of(), List.of(), List.of(cooldownComplete))));

    FlowController normalController = new FlowController();
    FlowInstance normal =
        new FlowInstance(program, null, null, normalController, 1.0D, Map.of(), 0L);
    normal.attemptStart(0L);
    runUntilFinished(normal);
    assertEquals(10, counter.get(), "timeScale=1 should run ten update ticks");

    counter.set(0);
    FlowController fastController = new FlowController();
    FlowInstance accelerated =
        new FlowInstance(program, null, null, fastController, 2.0D, Map.of(), 0L);
    accelerated.attemptStart(0L);
    runUntilFinished(accelerated);
    assertEquals(10, counter.get(), "timeScale=2 should still run ten update ticks");
  }

  private static void runUntilFinished(FlowInstance instance) {
    for (int tick = 0; tick < 500 && !instance.isFinished(); tick++) {
      instance.tick(tick);
    }
  }
}
