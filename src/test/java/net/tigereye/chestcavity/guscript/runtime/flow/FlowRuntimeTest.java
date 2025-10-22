package net.tigereye.chestcavity.guscript.runtime.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.guscript.runtime.flow.actions.FlowActions;
import net.tigereye.chestcavity.guscript.runtime.flow.guards.FlowGuards;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FlowRuntimeTest {

  @AfterEach
  void resetConfig() {
    ChestCavity.config = null;
  }

  @Test
  void cooldownGuardBlocksUntilReady() {
    FlowController controller = new FlowController();

    FlowTransition start =
        new FlowTransition(
            FlowTrigger.START,
            FlowState.CHARGING,
            List.of(FlowGuards.cooldownReady("demo")),
            List.of(),
            0);

    Map<FlowState, FlowStateDefinition> definitions = new EnumMap<>(FlowState.class);
    definitions.put(FlowState.IDLE, new FlowStateDefinition(List.of(), List.of(), List.of(start)));
    definitions.put(FlowState.CHARGING, new FlowStateDefinition(List.of(), List.of(), List.of()));

    FlowProgram program =
        new FlowProgram(
            ResourceLocation.fromNamespaceAndPath("test", "cooldown"), FlowState.IDLE, definitions);

    FlowInstance instance = new FlowInstance(program, null, null, controller, 0L);

    controller.setCooldown("demo", 5L);
    assertFalse(instance.attemptStart(0L), "Start should fail while cooldown is active");
    assertEquals(FlowState.IDLE, instance.state(), "State should remain idle");

    assertTrue(
        instance.attemptStart(10L), "Cooldown should expire once game time passes threshold");
    assertEquals(FlowState.CHARGING, instance.state(), "State should advance to charging");
    assertEquals(0, instance.ticksInState(), "Entering a state should reset tick counter");
  }

  @Test
  void tickTransitionsRespectMinTicks() throws Exception {
    FlowController controller = new FlowController();

    FlowTransition start =
        new FlowTransition(FlowTrigger.START, FlowState.CHARGING, List.of(), List.of(), 0);
    FlowTransition chargingComplete =
        new FlowTransition(FlowTrigger.RELEASE, FlowState.RELEASING, List.of(), List.of(), 2);

    Map<FlowState, FlowStateDefinition> definitions = new EnumMap<>(FlowState.class);
    definitions.put(FlowState.IDLE, new FlowStateDefinition(List.of(), List.of(), List.of(start)));
    definitions.put(
        FlowState.CHARGING,
        new FlowStateDefinition(List.of(), List.of(), List.of(chargingComplete)));
    definitions.put(FlowState.RELEASING, new FlowStateDefinition(List.of(), List.of(), List.of()));

    FlowProgram program =
        new FlowProgram(
            ResourceLocation.fromNamespaceAndPath("test", "progress"), FlowState.IDLE, definitions);

    FlowInstance instance = new FlowInstance(program, null, null, controller, 0L);
    assertTrue(instance.attemptStart(0L), "Start transition should succeed without guards");
    assertEquals(FlowState.CHARGING, instance.state(), "Flow should enter charging state");

    Field ticksField = FlowInstance.class.getDeclaredField("ticksInState");
    ticksField.setAccessible(true);

    ticksField.setInt(instance, 1);
    instance.handleInput(FlowInput.RELEASE, 5L);
    assertEquals(
        FlowState.CHARGING,
        instance.state(),
        "Flow should remain charging until min ticks reached");

    ticksField.setInt(instance, 2);
    instance.handleInput(FlowInput.RELEASE, 6L);
    assertEquals(
        FlowState.RELEASING,
        instance.state(),
        "Flow should advance after meeting min tick requirement");
  }

  @Test
  void queueDisabledRejectsNewFlowWhileRunning() {
    CCConfig cfg = new CCConfig();
    // Explicitly disable queue to make this test independent of default config
    cfg.GUSCRIPT_EXECUTION.enableFlowQueue = false;
    ChestCavity.config = cfg;
    FlowController controller = new FlowController();
    FlowProgram program = simpleProgram(List.of());

    assertTrue(controller.start(program, null, Map.of(), 0L));
    assertFalse(controller.start(program, null, Map.of(), 1L));
  }

  @Test
  void queueEnabledEnqueuesUpToCapacity() {
    CCConfig config = new CCConfig();
    config.GUSCRIPT_EXECUTION.enableFlowQueue = true;
    config.GUSCRIPT_EXECUTION.maxFlowQueueLength = 1;
    ChestCavity.config = config;

    FlowController controller = new FlowController();
    FlowProgram program = simpleProgram(List.of());

    assertTrue(controller.start(program, null, Map.of(), 0L));
    assertTrue(controller.start(program, null, Map.of("key", "value"), 1L));
    assertTrue(controller.hasPending(), "Queue should contain enqueued flow");
    assertEquals(1, controller.pendingSize(), "Exactly one entry should be queued");
    assertFalse(controller.start(program, null, Map.of(), 2L), "Queue should reject when full");
  }

  @Test
  void queuedEntryStartsAfterCurrentFinishes() throws Exception {
    CCConfig config = new CCConfig();
    config.GUSCRIPT_EXECUTION.enableFlowQueue = true;
    ChestCavity.config = config;

    FlowController controller = new FlowController();
    FlowProgram program = simpleProgram(List.of());

    assertTrue(controller.start(program, null, Map.of(), 0L));
    assertTrue(controller.start(program, null, Map.of("chain", "1"), 1L));
    assertEquals(1, controller.pendingSize());

    Field instanceField = FlowController.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    FlowInstance active = (FlowInstance) instanceField.get(controller);
    Field finishedField = FlowInstance.class.getDeclaredField("finished");
    finishedField.setAccessible(true);
    finishedField.setBoolean(active, true);

    java.lang.reflect.Method clearRuntimeState =
        FlowController.class.getDeclaredMethod("clearRuntimeState");
    clearRuntimeState.setAccessible(true);
    clearRuntimeState.invoke(controller);
    instanceField.set(controller, null);

    controller.drainQueue(5L);

    assertTrue(controller.isRunning(), "Queued flow should start once previous finishes");
    assertFalse(controller.hasPending(), "Queue should be empty after starting next flow");
  }

  @Test
  void queuedEntryDroppedWhenGuardFails() throws Exception {
    CCConfig config = new CCConfig();
    config.GUSCRIPT_EXECUTION.enableFlowQueue = true;
    config.GUSCRIPT_EXECUTION.revalidateQueuedGuards = true;
    config.GUSCRIPT_EXECUTION.queuedGuardRetryLimit = 0;
    ChestCavity.config = config;

    FlowController controller = new FlowController();
    FlowProgram program = simpleProgram(List.of(FlowGuards.cooldownReady("demo")));

    controller.setCooldown("demo", 0L);
    assertTrue(controller.start(program, null, Map.of(), 0L));
    assertTrue(controller.start(program, null, Map.of(), 1L));
    assertEquals(1, controller.pendingSize());

    controller.setCooldown("demo", 100L);
    Field instanceField = FlowController.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    java.lang.reflect.Method clearRuntimeState =
        FlowController.class.getDeclaredMethod("clearRuntimeState");
    clearRuntimeState.setAccessible(true);
    clearRuntimeState.invoke(controller);
    instanceField.set(controller, null);

    controller.drainQueue(50L);

    assertFalse(controller.isRunning(), "Guard failure should prevent queued flow from starting");
    assertFalse(controller.hasPending(), "Queue should drop entry after guard failure");
  }

  @Test
  void consumeResourceDeductsJingli() {
    FlowEdgeAction consume = FlowActions.consumeResource("jingli", 10.0D);
    FlowController controller = Mockito.mock(FlowController.class);
    Mockito.when(
            controller.resolveFlowParamAsDouble(Mockito.eq("time.accelerate"), Mockito.eq(1.0D)))
        .thenReturn(1.0D);

    GuzhenrenResourceBridge.ResourceHandle handle =
        Mockito.mock(GuzhenrenResourceBridge.ResourceHandle.class);
    AtomicReference<Double> jingli = new AtomicReference<>(200.0D);
    Mockito.when(handle.getJingli()).thenAnswer(inv -> OptionalDouble.of(jingli.get()));
    Mockito.when(handle.adjustJingli(Mockito.anyDouble(), Mockito.eq(true)))
        .thenAnswer(
            inv -> {
              double delta = inv.getArgument(0);
              double next = Math.max(0.0D, jingli.get() + delta);
              jingli.set(next);
              return OptionalDouble.of(next);
            });

    FlowActions.overrideResourceOpenerForTests(player -> Optional.of(handle));
    try {
      consume.apply(null, null, controller, 0L);
    } finally {
      FlowActions.resetResourceOpenerForTests();
    }

    Mockito.verify(handle).adjustJingli(-10.0D, true);
    Mockito.verify(controller, Mockito.never())
        .requestCancel(Mockito.anyString(), Mockito.anyLong());
    assertEquals(190.0D, jingli.get(), 1.0E-6);
  }

  @Test
  void consumeResourceRespectsTimeScale() {
    FlowEdgeAction consume = FlowActions.consumeResource("jingli", 10.0D);
    FlowController controller = Mockito.mock(FlowController.class);
    Mockito.when(
            controller.resolveFlowParamAsDouble(Mockito.eq("time.accelerate"), Mockito.eq(1.0D)))
        .thenReturn(2.0D);

    GuzhenrenResourceBridge.ResourceHandle handle =
        Mockito.mock(GuzhenrenResourceBridge.ResourceHandle.class);
    AtomicReference<Double> jingli = new AtomicReference<>(200.0D);
    Mockito.when(handle.getJingli()).thenAnswer(inv -> OptionalDouble.of(jingli.get()));
    Mockito.when(handle.adjustJingli(Mockito.anyDouble(), Mockito.eq(true)))
        .thenAnswer(
            inv -> {
              double delta = inv.getArgument(0);
              double next = Math.max(0.0D, jingli.get() + delta);
              jingli.set(next);
              return OptionalDouble.of(next);
            });

    FlowActions.overrideResourceOpenerForTests(player -> Optional.of(handle));
    try {
      consume.apply(null, null, controller, 0L);
    } finally {
      FlowActions.resetResourceOpenerForTests();
    }

    Mockito.verify(handle).adjustJingli(-5.0D, true);
    assertEquals(195.0D, jingli.get(), 1.0E-6);
  }

  @Test
  void consumeResourceCancelTriggersOnFailure() {
    FlowEdgeAction consume = FlowActions.consumeResource("jingli", 10.0D);
    FlowController controller = Mockito.mock(FlowController.class);
    Mockito.when(
            controller.resolveFlowParamAsDouble(Mockito.eq("time.accelerate"), Mockito.eq(1.0D)))
        .thenReturn(1.0D);
    Mockito.when(controller.requestCancel(Mockito.anyString(), Mockito.anyLong())).thenReturn(true);

    GuzhenrenResourceBridge.ResourceHandle handle =
        Mockito.mock(GuzhenrenResourceBridge.ResourceHandle.class);
    AtomicReference<Double> jingli = new AtomicReference<>(5.0D);
    Mockito.when(handle.getJingli()).thenAnswer(inv -> OptionalDouble.of(jingli.get()));
    Mockito.when(handle.adjustJingli(Mockito.anyDouble(), Mockito.eq(true)))
        .thenAnswer(inv -> OptionalDouble.empty());

    FlowActions.overrideResourceOpenerForTests(player -> Optional.of(handle));
    try {
      consume.apply(null, null, controller, 42L);
    } finally {
      FlowActions.resetResourceOpenerForTests();
    }

    Mockito.verify(controller)
        .requestCancel(Mockito.eq("resource_failure:jingli"), Mockito.eq(42L));
    assertEquals(5.0D, jingli.get(), 1.0E-6);
  }

  @Test
  void consumeResourceCancelsWhenPoolTooSmall() {
    FlowEdgeAction consume = FlowActions.consumeResource("jingli", 10.0D);
    FlowController controller = Mockito.mock(FlowController.class);
    Mockito.when(
            controller.resolveFlowParamAsDouble(Mockito.eq("time.accelerate"), Mockito.eq(1.0D)))
        .thenReturn(1.0D);
    Mockito.when(controller.requestCancel(Mockito.anyString(), Mockito.anyLong())).thenReturn(true);

    GuzhenrenResourceBridge.ResourceHandle handle =
        Mockito.mock(GuzhenrenResourceBridge.ResourceHandle.class);
    AtomicReference<Double> jingli = new AtomicReference<>(5.0D);
    Mockito.when(handle.getJingli()).thenAnswer(inv -> OptionalDouble.of(jingli.get()));
    Mockito.when(handle.adjustJingli(Mockito.anyDouble(), Mockito.eq(true)))
        .thenAnswer(
            inv -> {
              double delta = inv.getArgument(0);
              double next = Math.max(0.0D, jingli.get() + delta);
              jingli.set(next);
              return OptionalDouble.of(next);
            });

    FlowActions.overrideResourceOpenerForTests(player -> Optional.of(handle));
    try {
      consume.apply(null, null, controller, 100L);
    } finally {
      FlowActions.resetResourceOpenerForTests();
    }

    Mockito.verify(controller)
        .requestCancel(Mockito.eq("resource_failure:jingli"), Mockito.eq(100L));
    Mockito.verify(handle, Mockito.never()).adjustJingli(Mockito.anyDouble(), Mockito.eq(true));
    assertEquals(5.0D, jingli.get(), 1.0E-6);
  }

  private static FlowProgram simpleProgram(List<FlowGuard> guards) {
    FlowTransition start =
        new FlowTransition(FlowTrigger.START, FlowState.CHARGING, guards, List.of(), 0);

    Map<FlowState, FlowStateDefinition> definitions = new EnumMap<>(FlowState.class);
    definitions.put(FlowState.IDLE, new FlowStateDefinition(List.of(), List.of(), List.of(start)));
    definitions.put(FlowState.CHARGING, new FlowStateDefinition(List.of(), List.of(), List.of()));

    return new FlowProgram(
        ResourceLocation.fromNamespaceAndPath("test", "queue"), FlowState.IDLE, definitions);
  }
}
