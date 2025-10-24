package net.tigereye.chestcavity.guscript.runtime.exec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;

import com.google.common.collect.HashMultiset;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.guscript.actions.ExportTimeScaleMultiplierAction;
import net.tigereye.chestcavity.guscript.ast.GuNode;
import net.tigereye.chestcavity.guscript.ast.GuNodeKind;
import net.tigereye.chestcavity.guscript.ast.OperatorGuNode;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgram;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgramRegistry;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowState;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowStateDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GuScriptExecutorTimeScaleTest {

  private Method executeMethod;
  private FlowController controller;
  private List<StartCall> startCalls;
  private Constructor<FlowController> controllerConstructor;

  @BeforeEach
  void setUp() throws Exception {
    executeMethod =
        GuScriptExecutor.class.getDeclaredMethod(
            "executeRootsWithSession",
            List.class,
            ServerPlayer.class,
            net.minecraft.world.entity.LivingEntity.class,
            ExecutionSession.class,
            ResourceLocation.class,
            Map.class);
    executeMethod.setAccessible(true);

    ChestCavity.config = new CCConfig();

    controllerConstructor = FlowController.class.getDeclaredConstructor();
    controllerConstructor.setAccessible(true);
    installRecordingController();

    FlowProgram program =
        new FlowProgram(
            ResourceLocation.fromNamespaceAndPath("test", "progress"),
            FlowState.IDLE,
            Map.of(FlowState.IDLE, new FlowStateDefinition(List.of(), List.of(), List.of())));
    FlowProgramRegistry.update(Map.of(program.id(), program));
  }

  @AfterEach
  void tearDown() {
    GuScriptExecutor.resetFlowControllerAccessor();
    FlowProgramRegistry.update(Map.of());
    ChestCavity.config = null;
  }

  @Test
  void flowStartUsesSessionScaleWhenParamMissing() throws Exception {
    OperatorGuNode exporter =
        new OperatorGuNode(
            "test:export",
            "Export",
            GuNodeKind.OPERATOR,
            HashMultiset.create(),
            List.of(new ExportTimeScaleMultiplierAction(1.5D)),
            List.of(),
            0,
            false,
            false);
    OperatorGuNode flowStarter =
        new OperatorGuNode(
            "test:start",
            "StartFlow",
            GuNodeKind.OPERATOR,
            HashMultiset.create(),
            List.of(),
            List.of(),
            1,
            false,
            false,
            ResourceLocation.fromNamespaceAndPath("test", "progress"),
            Map.of());

    List<GuNode> roots = GuScriptExecutor.sortRootsForSession(List.of(flowStarter, exporter));
    ExecutionSession session = new ExecutionSession(5.0D, 5.0D);

    executeMethod.invoke(null, roots, null, null, session, null, Map.of());

    assertEquals(1, startCalls.size());
    StartCall call = startCalls.getFirst();
    assertEquals(1.5D, call.timeScale(), 1.0E-6);
    assertEquals(1.5D, Double.parseDouble(call.flowParams().get("time.accelerate")), 1.0E-6);
  }

  @Test
  void flowStartCombinesWithExistingParam() throws Exception {
    OperatorGuNode exporter =
        new OperatorGuNode(
            "test:export",
            "Export",
            GuNodeKind.OPERATOR,
            HashMultiset.create(),
            List.of(new ExportTimeScaleMultiplierAction(1.5D)),
            List.of(),
            0,
            false,
            false);
    OperatorGuNode flowStarter =
        new OperatorGuNode(
            "test:start",
            "StartFlow",
            GuNodeKind.OPERATOR,
            HashMultiset.create(),
            List.of(),
            List.of(),
            1,
            false,
            false,
            ResourceLocation.fromNamespaceAndPath("test", "progress"),
            Map.of("time.accelerate", "1.2"));

    List<GuNode> roots = GuScriptExecutor.sortRootsForSession(List.of(flowStarter, exporter));
    ExecutionSession session = new ExecutionSession(5.0D, 5.0D);

    executeMethod.invoke(null, roots, null, null, session, null, Map.of());

    assertEquals(1, startCalls.size());
    StartCall call = startCalls.getFirst();
    assertEquals(1.8D, call.timeScale(), 1.0E-6);
    assertEquals(1.8D, Double.parseDouble(call.flowParams().get("time.accelerate")), 1.0E-6);
  }

  private void installRecordingController() throws Exception {
    FlowController rawController = controllerConstructor.newInstance();
    controller = Mockito.spy(rawController);
    startCalls = new ArrayList<>();
    Mockito.doAnswer(
            invocation -> {
              FlowProgram program = invocation.getArgument(0);
              net.minecraft.world.entity.LivingEntity target = invocation.getArgument(1);
              double timeScale = invocation.getArgument(2);
              Map<String, String> params = invocation.getArgument(3);
              long gameTime = invocation.getArgument(4);
              String descriptor = invocation.getArgument(5);
              startCalls.add(
                  new StartCall(
                      program,
                      target,
                      timeScale,
                      params == null ? Map.of() : Map.copyOf(params),
                      gameTime,
                      descriptor));
              return true;
            })
        .when(controller)
        .start(any(FlowProgram.class), any(), anyDouble(), anyMap(), anyLong(), any());
    GuScriptExecutor.setFlowControllerAccessor(player -> controller);
  }

  private record StartCall(
      FlowProgram program,
      net.minecraft.world.entity.LivingEntity target,
      double timeScale,
      Map<String, String> flowParams,
      long gameTime,
      String sourceDescriptor) {}
}
