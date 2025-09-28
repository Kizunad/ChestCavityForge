package net.tigereye.chestcavity.guscript.runtime.flow;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.guscript.runtime.flow.guards.FlowGuards;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowInput;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowRuntimeTest {

    @Test
    void cooldownGuardBlocksUntilReady() {
        FlowController controller = new FlowController();

        FlowTransition start = new FlowTransition(
                FlowTrigger.START,
                FlowState.CHARGING,
                List.of(FlowGuards.cooldownReady("demo")),
                List.of(),
                0
        );

        Map<FlowState, FlowStateDefinition> definitions = new EnumMap<>(FlowState.class);
        definitions.put(FlowState.IDLE, new FlowStateDefinition(List.of(), List.of(), List.of(start)));
        definitions.put(FlowState.CHARGING, new FlowStateDefinition(List.of(), List.of(), List.of()));

        FlowProgram program = new FlowProgram(ResourceLocation.fromNamespaceAndPath("test", "cooldown"), FlowState.IDLE, definitions);

        FlowInstance instance = new FlowInstance(program, null, null, controller, 0L);

        controller.setCooldown("demo", 5L);
        assertFalse(instance.attemptStart(0L), "Start should fail while cooldown is active");
        assertEquals(FlowState.IDLE, instance.state(), "State should remain idle");

        assertTrue(instance.attemptStart(10L), "Cooldown should expire once game time passes threshold");
        assertEquals(FlowState.CHARGING, instance.state(), "State should advance to charging");
        assertEquals(0, instance.ticksInState(), "Entering a state should reset tick counter");
    }

    @Test
    void tickTransitionsRespectMinTicks() throws Exception {
        FlowController controller = new FlowController();

        FlowTransition start = new FlowTransition(
                FlowTrigger.START,
                FlowState.CHARGING,
                List.of(),
                List.of(),
                0
        );
        FlowTransition chargingComplete = new FlowTransition(
                FlowTrigger.RELEASE,
                FlowState.RELEASING,
                List.of(),
                List.of(),
                2
        );

        Map<FlowState, FlowStateDefinition> definitions = new EnumMap<>(FlowState.class);
        definitions.put(FlowState.IDLE, new FlowStateDefinition(List.of(), List.of(), List.of(start)));
        definitions.put(FlowState.CHARGING, new FlowStateDefinition(List.of(), List.of(), List.of(chargingComplete)));
        definitions.put(FlowState.RELEASING, new FlowStateDefinition(List.of(), List.of(), List.of()));

        FlowProgram program = new FlowProgram(ResourceLocation.fromNamespaceAndPath("test", "progress"), FlowState.IDLE, definitions);

        FlowInstance instance = new FlowInstance(program, null, null, controller, 0L);
        assertTrue(instance.attemptStart(0L), "Start transition should succeed without guards");
        assertEquals(FlowState.CHARGING, instance.state(), "Flow should enter charging state");

        Field ticksField = FlowInstance.class.getDeclaredField("ticksInState");
        ticksField.setAccessible(true);

        ticksField.setInt(instance, 1);
        instance.handleInput(FlowInput.RELEASE, 5L);
        assertEquals(FlowState.CHARGING, instance.state(), "Flow should remain charging until min ticks reached");

        ticksField.setInt(instance, 2);
        instance.handleInput(FlowInput.RELEASE, 6L);
        assertEquals(FlowState.RELEASING, instance.state(), "Flow should advance after meeting min tick requirement");
    }
}
