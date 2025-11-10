package net.tigereye.chestcavity.guscript.runtime.exec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.ast.GuNode;
import net.tigereye.chestcavity.guscript.ast.GuNodeKind;

/** Simple interpreter executing GuScript AST nodes depth-first. */
public final class GuScriptRuntime {

  private static final AtomicLong EXECUTION_IDS = new AtomicLong();
  private static final AtomicLong CONTEXT_IDS = new AtomicLong();

  public void execute(GuNode root, GuScriptContext context) {
    Objects.requireNonNull(root, "root");
    Objects.requireNonNull(context, "context");
    ExecutionTrace trace = ExecutionTrace.singleInvocation(root);
    trace.onContextAcquired(context, CONTEXT_IDS.incrementAndGet());
    executeNode(root, context, 0, trace);
    trace.onCompleted(context);
  }

  public void executeAll(List<GuNode> roots, GuScriptContext context) {
    if (roots == null || roots.isEmpty() || context == null) {
      return;
    }
    int index = 0;
    for (GuNode root : roots) {
      ExecutionTrace trace = ExecutionTrace.sharedContext(root, index++);
      trace.onContextAcquired(context, CONTEXT_IDS.incrementAndGet());
      executeNode(root, context, 0, trace);
      trace.onCompleted(context);
    }
  }

  public void executeAll(List<GuNode> roots, Supplier<GuScriptContext> contextFactory) {
    if (roots == null || roots.isEmpty() || contextFactory == null) {
      return;
    }
    for (int index = 0; index < roots.size(); index++) {
      GuNode root = roots.get(index);
      GuScriptContext context;
      try {
        context = contextFactory.get();
      } catch (Exception ex) {
        ChestCavity.LOGGER.error(
            "[GuScript] Context supplier threw for root {} (index {})", root.name(), index, ex);
        continue;
      }
      if (context == null) {
        ChestCavity.LOGGER.warn(
            "[GuScript] Context factory returned null for root {} (index {})", root.name(), index);
        continue;
      }
      ExecutionTrace trace = ExecutionTrace.dedicatedContext(root, index);
      long contextId = CONTEXT_IDS.incrementAndGet();
      trace.onContextAcquired(context, contextId);
      executeNode(root, context, 0, trace);
      trace.onCompleted(context);
    }
  }

  public void executeAll(List<GuNode> roots, IntFunction<GuScriptContext> contextFactory) {
    if (contextFactory == null) {
      return;
    }
    AtomicInteger indexCursor = new AtomicInteger();
    executeAll(roots, () -> contextFactory.apply(indexCursor.getAndIncrement()));
  }

  private void executeNode(GuNode node, GuScriptContext context, int depth, ExecutionTrace trace) {
    for (GuNode child : node.children()) {
      executeNode(child, context, depth + 1, trace);
    }
    for (Action action : node.actions()) {
      trace.recordActionDispatch(action);
      try {
        action.execute(context);
      } catch (Exception ex) {
        ChestCavity.LOGGER.error(
            "[GuScript] Action {} failed at node {}", action.id(), node.name(), ex);
        trace.recordFailure(action);
      }
    }
  }

  private static final class ExecutionTrace {
    private final long executionId;
    private final int rootIndex;
    private final String rootName;
    private final GuNodeKind rootKind;
    private final Map<String, Integer> actionCounts = new HashMap<>();
    private int totalActions;
    private int failedActions;

    private ExecutionTrace(int rootIndex, GuNode root) {
      this.executionId = EXECUTION_IDS.incrementAndGet();
      this.rootIndex = rootIndex;
      this.rootName = root.name();
      this.rootKind = root.kind();
    }

    static ExecutionTrace singleInvocation(GuNode root) {
      return new ExecutionTrace(0, root);
    }

    static ExecutionTrace sharedContext(GuNode root, int index) {
      return new ExecutionTrace(index, root);
    }

    static ExecutionTrace dedicatedContext(GuNode root, int index) {
      return new ExecutionTrace(index, root);
    }

    void onContextAcquired(GuScriptContext context, long contextId) {
      String performer =
          context.performer() != null ? context.performer().getGameProfile().getName() : "<none>";
      boolean clientSide =
          context.performer() != null
              ? context.performer().level().isClientSide()
              : context.target() != null && context.target().level().isClientSide();
      ChestCavity.LOGGER.info(
          "[GuScript] Executing root {}#{} ({}) with context {} on {} side for performer {}",
          rootName,
          rootIndex,
          rootKind,
          contextId,
          clientSide ? "client" : "server",
          performer);
    }

    void recordActionDispatch(Action action) {
      totalActions++;
      actionCounts.merge(action.id(), 1, Integer::sum);
    }

    void recordFailure(Action action) {
      failedActions++;
      actionCounts.merge(action.id() + "(failed)", 1, Integer::sum);
    }

    void onCompleted(GuScriptContext context) {
      String summary =
          actionCounts.entrySet().stream()
              .map(entry -> entry.getKey() + "=" + entry.getValue())
              .sorted()
              .reduce((left, right) -> left + ", " + right)
              .orElse("no actions");
      ChestCavity.LOGGER.info(
          "[GuScript] Root {}#{} ({}) completed execution {}: {} actions dispatched ({} failures). [{}]",
          rootName,
          rootIndex,
          rootKind,
          executionId,
          totalActions,
          failedActions,
          summary);
    }
  }
}
