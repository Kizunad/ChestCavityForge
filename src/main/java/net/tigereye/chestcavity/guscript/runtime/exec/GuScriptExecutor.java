package net.tigereye.chestcavity.guscript.runtime.exec;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.guscript.ast.GuNode;
import net.tigereye.chestcavity.guscript.ast.LeafGuNode;
import net.tigereye.chestcavity.guscript.ast.OperatorGuNode;
import net.tigereye.chestcavity.guscript.data.BindingTarget;
import net.tigereye.chestcavity.guscript.data.GuScriptAttachment;
import net.tigereye.chestcavity.guscript.data.GuScriptPageState;
import net.tigereye.chestcavity.guscript.data.GuScriptProgramCache;
import net.tigereye.chestcavity.guscript.runtime.GuNodeOrdering;
import net.tigereye.chestcavity.guscript.runtime.action.DefaultGuScriptExecutionBridge;
import net.tigereye.chestcavity.guscript.runtime.exec.DefaultGuScriptContext;
import net.tigereye.chestcavity.guscript.runtime.exec.ExecutionSession;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptCompiler;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptRuntime;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowControllerManager;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgram;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgramRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.function.Function;

/**
 * Executes compiled GuScript programs for a player.
 */
public final class GuScriptExecutor {

    private static final GuScriptRuntime RUNTIME = new GuScriptRuntime();
    private static final int DEFAULT_MAX_KEYBIND_PAGES = 16;
    private static final int DEFAULT_MAX_KEYBIND_ROOTS = 64;
    private static final double DEFAULT_SESSION_MULTIPLIER_CAP = 5.0D;
    private static final double DEFAULT_SESSION_FLAT_CAP = 20.0D;
    private static final double MIN_TIME_SCALE = 0.1D;
    private static final double MAX_TIME_SCALE = 100.0D;
    private static Function<ServerPlayer, FlowController> FLOW_CONTROLLER_ACCESSOR = FlowControllerManager::get;

    private GuScriptExecutor() {
    }

    static void setFlowControllerAccessor(Function<ServerPlayer, FlowController> accessor) {
        FLOW_CONTROLLER_ACCESSOR = accessor == null ? FlowControllerManager::get : accessor;
    }

    static void resetFlowControllerAccessor() {
        FLOW_CONTROLLER_ACCESSOR = FlowControllerManager::get;
    }

    public static void trigger(ServerPlayer player, LivingEntity target, GuScriptAttachment attachment) {
        if (player == null || attachment == null) {
            return;
        }
        int pageIndex = attachment.getCurrentPageIndex();
        GuScriptPageState page = attachment.activePage();
        GuScriptProgramCache cache = GuScriptCompiler.compilePageIfNeeded(
                attachment,
                page,
                pageIndex,
                player.level().getGameTime()
        );
        if (cache.roots().isEmpty()) {
            ChestCavity.LOGGER.debug("[GuScript] No compiled roots to execute for {}", player.getGameProfile().getName());
            return;
        }
        ChestCavity.LOGGER.info(
                "[GuScript] Trigger dispatch for {} on page {}: {} roots -> {}",
                player.getGameProfile().getName(),
                pageIndex,
                cache.roots().size(),
                cache.roots().stream()
                        .map(root -> root.kind() + ":" + root.name())
                        .toList()
        );
        LivingEntity actualTarget = target == null ? player : target;
        CCConfig.GuScriptExecutionConfig executionConfig = ChestCavity.config != null
                ? ChestCavity.config.GUSCRIPT_EXECUTION
                : null;
        ExecutionSession session = new ExecutionSession(
                executionConfig != null ? executionConfig.maxCumulativeMultiplier : DEFAULT_SESSION_MULTIPLIER_CAP,
                executionConfig != null ? executionConfig.maxCumulativeFlat : DEFAULT_SESSION_FLAT_CAP
        );
        List<GuNode> sortedRoots = sortRootsForSession(cache.roots());
        logRootOrdering(player, pageIndex, sortedRoots);
        ResourceLocation defaultFlowId = page.flowId().orElse(null);
        Map<String, String> defaultFlowParams = page.flowParams();
        executeRootsWithSession(sortedRoots, player, actualTarget, session, defaultFlowId, defaultFlowParams);
    }

    public static void triggerKeybind(ServerPlayer player, LivingEntity target, GuScriptAttachment attachment) {
        if (player == null || attachment == null) {
            return;
        }

        CCConfig.GuScriptExecutionConfig executionConfig = ChestCavity.config != null
                ? ChestCavity.config.GUSCRIPT_EXECUTION
                : null;
        int maxPages = executionConfig != null ? executionConfig.maxKeybindPagesPerTrigger : DEFAULT_MAX_KEYBIND_PAGES;
        int maxRoots = executionConfig != null ? executionConfig.maxKeybindRootsPerTrigger : DEFAULT_MAX_KEYBIND_ROOTS;
        boolean unlimitedPages = maxPages <= 0;
        boolean unlimitedRoots = maxRoots <= 0;
        if (unlimitedPages) {
            maxPages = Integer.MAX_VALUE;
        }
        if (unlimitedRoots) {
            maxRoots = Integer.MAX_VALUE;
        }

        List<GuNode> aggregatedRoots = new ArrayList<>();
        int eligiblePages = 0;
        int executedPages = 0;
        int totalRoots = 0;
        boolean pageLimitReached = false;
        boolean rootLimitReached = false;
        long gameTime = player.level().getGameTime();

        List<GuScriptPageState> pages = attachment.pages();
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            GuScriptPageState page = pages.get(pageIndex);
            if (page.bindingTarget() != BindingTarget.KEYBIND) {
                continue;
            }
            eligiblePages++;
            if (executedPages >= maxPages) {
                pageLimitReached = true;
                continue;
            }
            if (totalRoots >= maxRoots) {
                rootLimitReached = true;
                break;
            }

            GuScriptProgramCache cache = GuScriptCompiler.compilePageIfNeeded(attachment, page, pageIndex, gameTime);
            List<GuNode> roots = cache.roots();
            if (roots.isEmpty()) {
                ChestCavity.LOGGER.debug("[GuScript] Keybind page {} contributed no roots", pageIndex);
                continue;
            }

            int remainingRootBudget = maxRoots - totalRoots;
            if (roots.size() > remainingRootBudget) {
                if (remainingRootBudget <= 0) {
                    rootLimitReached = true;
                    if (!unlimitedRoots) {
                        ChestCavity.LOGGER.warn(
                                "[GuScript] Keybind trigger hit max root limit {} before processing page {}",
                                maxRoots,
                                pageIndex
                        );
                    }
                    break;
                }
                aggregatedRoots.addAll(roots.subList(0, remainingRootBudget));
                totalRoots += remainingRootBudget;
                executedPages++;
                rootLimitReached = true;
                ChestCavity.LOGGER.warn(
                        "[GuScript] Keybind page {} truncated to {} roots due to max root limit {} (running total {})",
                        pageIndex,
                        remainingRootBudget,
                        unlimitedRoots ? "unlimited" : maxRoots,
                        totalRoots
                );
                break;
            }

            aggregatedRoots.addAll(roots);
            totalRoots += roots.size();
            executedPages++;
            ChestCavity.LOGGER.info(
                    "[GuScript] Keybind page {} queued {} roots (running total {})",
                    pageIndex,
                    roots.size(),
                    totalRoots
            );
        }

        if (aggregatedRoots.isEmpty()) {
            ChestCavity.LOGGER.info(
                    "[GuScript] Keybind trigger for {} found {} eligible pages but no executable roots",
                    player.getGameProfile().getName(),
                    eligiblePages
            );
            return;
        }

        ChestCavity.LOGGER.info(
                "[GuScript] Keybind trigger for {}: {} keybind pages ({} executed) -> {} roots. Limits: pages={}, roots={}. Executing sequentially.",
                player.getGameProfile().getName(),
                eligiblePages,
                executedPages,
                totalRoots,
                unlimitedPages ? "unlimited" : maxPages,
                unlimitedRoots ? "unlimited" : maxRoots
        );
        if (pageLimitReached) {
            ChestCavity.LOGGER.warn(
                    "[GuScript] Keybind trigger skipped remaining pages due to max page limit {}",
                    unlimitedPages ? "unlimited" : maxPages
            );
        }
        if (rootLimitReached && !unlimitedRoots) {
            ChestCavity.LOGGER.warn(
                    "[GuScript] Keybind trigger reached max root limit {}. Additional roots were not executed.",
                    maxRoots
            );
        }

        LivingEntity actualTarget = target == null ? player : target;
        ExecutionSession session = new ExecutionSession(
                executionConfig != null ? executionConfig.maxCumulativeMultiplier : DEFAULT_SESSION_MULTIPLIER_CAP,
                executionConfig != null ? executionConfig.maxCumulativeFlat : DEFAULT_SESSION_FLAT_CAP
        );
        List<GuNode> sortedRoots = sortRootsForSession(aggregatedRoots);
        logRootOrdering(player, -1, sortedRoots);
        executeRootsWithSession(sortedRoots, player, actualTarget, session, null, Map.of());
    }

    static List<GuNode> sortRootsForSession(List<GuNode> roots) {
        if (roots == null || roots.isEmpty()) {
            return List.of();
        }
        List<OrderedRoot> ordered = new ArrayList<>(roots.size());
        for (int i = 0; i < roots.size(); i++) {
            GuNode node = roots.get(i);
            ordered.add(OrderedRoot.from(node, i));
        }

        boolean preferUiOrder = isUiOrderingPreferred();
        ordered.sort((left, right) -> {
            if (preferUiOrder) {
                int pageComparison = Integer.compare(left.primaryPageIndex(), right.primaryPageIndex());
                if (pageComparison != 0) {
                    return pageComparison;
                }
                int slotComparison = Integer.compare(left.primarySlotIndex(), right.primarySlotIndex());
                if (slotComparison != 0) {
                    return slotComparison;
                }
            }
            return OrderedRoot.compareExecutionFirst(left, right);
        });

        return ordered.stream().map(OrderedRoot::node).toList();
    }

    private static boolean isUiOrderingPreferred() {
        if (ChestCavity.config == null) {
            return true;
        }
        CCConfig.GuScriptExecutionConfig execConfig = ChestCavity.config.GUSCRIPT_EXECUTION;
        return execConfig == null || execConfig.preferUiOrder;
    }

    private static void executeRootsWithSession(
            List<GuNode> roots,
            ServerPlayer performer,
            LivingEntity target,
            ExecutionSession session,
            ResourceLocation defaultFlowId,
            Map<String, String> defaultFlowParams
    ) {
        if (roots.isEmpty()) {
            return;
        }
        Map<String, String> safeDefaultFlowParams = defaultFlowParams == null ? Map.of() : defaultFlowParams;
        boolean flowsEnabled = ChestCavity.config != null && ChestCavity.config.GUSCRIPT_EXECUTION.enableFlows;
        boolean defaultFlowConsumed = false;
        AtomicInteger rootIndex = new AtomicInteger();
        List<PendingFlowStart> pendingFlows = new ArrayList<>();
        for (GuNode root : roots) {
            int index = rootIndex.getAndIncrement();
            PendingFlowStart pending = null;
            ResourceLocation flowToStart = null;
            Map<String, String> flowParams = Map.of();
            String source = "operator";
            if (flowsEnabled && root instanceof OperatorGuNode operator) {
                if (operator.flowId().isPresent()) {
                    flowToStart = operator.flowId().get();
                    flowParams = operator.flowParams();
                } else if (!defaultFlowConsumed && defaultFlowId != null) {
                    flowToStart = defaultFlowId;
                    flowParams = safeDefaultFlowParams;
                    source = "page";
                    defaultFlowConsumed = true;
                }
            }

            if (flowsEnabled && flowToStart != null) {
                var program = FlowProgramRegistry.get(flowToStart);
                if (program.isPresent()) {
                    Map<String, String> adjustedParams = flowParams == null || flowParams.isEmpty()
                            ? new LinkedHashMap<>()
                            : new LinkedHashMap<>(flowParams);
                    pending = new PendingFlowStart(program.get(), adjustedParams, source, flowToStart, root.name(), index);
                    pendingFlows.add(pending);
                } else {
                    ChestCavity.LOGGER.warn(
                            "[GuScript] Root {}#{} referenced unknown flow {} (source={}). Falling back to immediate execution.",
                            root.name(),
                            index,
                            flowToStart,
                            source
                    );
                }
            }

            DefaultGuScriptExecutionBridge bridge = new DefaultGuScriptExecutionBridge(performer, target, index);
            DefaultGuScriptContext context = new DefaultGuScriptContext(performer, target, bridge, session);
            if (root instanceof OperatorGuNode operator) {
                context.enableModifierExports(operator.exportMultiplier(), operator.exportFlat());
            }
            double beforeMultiplier = session.currentMultiplier();
            double beforeFlat = session.currentFlat();
            double beforeTimeScale = session.currentTimeScale();
            RUNTIME.execute(root, context);
            double deltaMultiplier = context.exportedMultiplierDelta();
            double deltaFlat = context.exportedFlatDelta();
            if (deltaMultiplier != 0.0D) {
                session.exportMultiplier(deltaMultiplier);
            }
            if (deltaFlat != 0.0D) {
                session.exportFlat(deltaFlat);
            }
            double afterMultiplier = session.currentMultiplier();
            double afterFlat = session.currentFlat();
            double afterTimeScale = session.currentTimeScale();
            if (pending != null) {
                pending.snapshotSessionScale(afterTimeScale);
            }
            ChestCavity.LOGGER.info(
                    "[GuScript] Root {}#{} exported modifiers: delta(multiplier={}, flat={}), direct(multiplier={}, flat={}), timeScale(mult={}, flat={}). Session {} -> {} / {} -> {} / timeScale {} -> {}",
                    root.name(),
                    index,
                    formatDouble(deltaMultiplier),
                    formatDouble(deltaFlat),
                    formatDouble(context.directExportedMultiplier()),
                    formatDouble(context.directExportedFlat()),
                    formatDouble(context.directExportedTimeScaleMultiplier()),
                    formatDouble(context.directExportedTimeScaleFlat()),
                    formatDouble(beforeMultiplier),
                    formatDouble(afterMultiplier),
                    formatDouble(beforeFlat),
                    formatDouble(afterFlat),
                    formatDouble(beforeTimeScale),
                    formatDouble(afterTimeScale)
            );
        }

        if (!flowsEnabled || pendingFlows.isEmpty()) {
            return;
        }

        CCConfig.GuScriptExecutionConfig executionConfig = ChestCavity.config != null
                ? ChestCavity.config.GUSCRIPT_EXECUTION
                : null;
        CCConfig.TimeScaleCombineStrategy strategy = executionConfig != null
                ? executionConfig.timeScaleCombine
                : CCConfig.TimeScaleCombineStrategy.MULTIPLY;
        boolean queueEnabled = executionConfig != null && executionConfig.enableFlowQueue;

        for (PendingFlowStart pending : pendingFlows) {
            Map<String, String> params = pending.params();
            double flowScale = parseTimeScale(params);
            double sessionScale = pending.sessionScale();
            double effectiveScale = combineTimeScale(flowScale, sessionScale, strategy);
            params.put("time.accelerate", Double.toString(effectiveScale));
            ChestCavity.LOGGER.info(
                    "[GuScript][Flow] {}#{} merged timeScale for {}: flow={}, session={}, effective={}, params={}",
                    pending.rootName(),
                    pending.rootIndex(),
                    pending.flowId(),
                    formatDouble(flowScale),
                    formatDouble(sessionScale),
                    formatDouble(effectiveScale),
                    params.isEmpty() ? "{}" : params
            );
            FlowController controller = FLOW_CONTROLLER_ACCESSOR.apply(performer);
            long gameTime = resolveGameTime(performer, target);
            String descriptor = pending.source() + ":" + pending.rootName() + "#" + pending.rootIndex();
            boolean accepted = controller.start(pending.program(), target, effectiveScale, params, gameTime, descriptor);
            ChestCavity.LOGGER.info(
                    "[GuScript] Root {}#{} requested flow {} (source={}, timeScale={}, params={}, accepted={})",
                    pending.rootName(),
                    pending.rootIndex(),
                    pending.flowId(),
                    pending.source(),
                    formatDouble(effectiveScale),
                    params.isEmpty() ? "{}" : params,
                    accepted
            );
            if (!accepted) {
                ChestCavity.LOGGER.info(
                        "[GuScript] Root {}#{} flow {} was rejected (source={}, queueEnabled={}, params={})",
                        pending.rootName(),
                        pending.rootIndex(),
                        pending.flowId(),
                        pending.source(),
                        queueEnabled,
                        params.isEmpty() ? "{}" : params
                );
            }
        }
    }

    private static double parseTimeScale(Map<String, String> flowParams) {
        if (flowParams == null || flowParams.isEmpty()) {
            return 1.0D;
        }
        String raw = flowParams.getOrDefault("time.accelerate", flowParams.getOrDefault("time_accelerate", "1"));
        if (raw == null || raw.isBlank()) {
            return 1.0D;
        }
        try {
            double value = Double.parseDouble(raw);
            return sanitizeTimeScale(value, 1.0D);
        } catch (Exception ignored) {
            return 1.0D;
        }
    }

    static double combineTimeScale(double flowScale, double sessionScale, CCConfig.TimeScaleCombineStrategy strategy) {
        double safeFlow = sanitizeTimeScale(flowScale, 1.0D);
        double safeSession = sanitizeTimeScale(sessionScale, 1.0D);
        CCConfig.TimeScaleCombineStrategy actual = strategy == null
                ? CCConfig.TimeScaleCombineStrategy.MULTIPLY
                : strategy;
        double combined = switch (actual) {
            case MULTIPLY -> safeFlow * safeSession;
            case MAX -> Math.max(safeFlow, safeSession);
            case OVERRIDE -> safeSession;
        };
        return clampTimeScale(combined);
    }

    private static double sanitizeTimeScale(double value, double fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return fallback;
        }
        return value;
    }

    private static double clampTimeScale(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 1.0D;
        }
        if (value < MIN_TIME_SCALE) {
            return MIN_TIME_SCALE;
        }
        if (value > MAX_TIME_SCALE) {
            return MAX_TIME_SCALE;
        }
        return value;
    }

    private static void logRootOrdering(ServerPlayer player, int pageIndex, List<GuNode> roots) {
        if (roots.isEmpty()) {
            return;
        }
        boolean preferUiOrder = isUiOrderingPreferred();
        String descriptor = roots.stream()
                .map(root -> {
                    OrderedRoot wrapper = OrderedRoot.from(root, 0);
                    String orderText = wrapper.executionOrder() == Integer.MAX_VALUE ? "∞" : Integer.toString(wrapper.executionOrder());
                    String pageText = wrapper.primaryPageIndex() == Integer.MAX_VALUE ? "-" : Integer.toString(wrapper.primaryPageIndex());
                    String slotText = wrapper.primarySlotIndex() == Integer.MAX_VALUE ? "-" : Integer.toString(wrapper.primarySlotIndex());
                    if (preferUiOrder) {
                        return wrapper.name() + "[order=" + orderText + ",rule=" + wrapper.ruleId() + ",page=" + pageText + ",slot=" + slotText + "]";
                    }
                    return wrapper.name() + "[order=" + orderText + ",rule=" + wrapper.ruleId() + "]";
                })
                .collect(Collectors.joining(", "));
        ChestCavity.LOGGER.info(
                "[GuScript] Ordered execution for {}{}: {}",
                resolvePlayerName(player),
                pageIndex >= 0 ? " page " + pageIndex : " keybind aggregation",
                descriptor
        );
    }

    private static String formatDouble(double value) {
        if (value == 0.0D) {
            return "0";
        }
        if (Double.isInfinite(value)) {
            return value > 0 ? "+∞" : "-∞";
        }
        return String.format("%.3f", value);
    }

    private static long resolveGameTime(ServerPlayer performer, LivingEntity target) {
        if (performer != null) {
            Level level = performer.level();
            if (level != null) {
                return level.getGameTime();
            }
        }
        if (target != null) {
            Level level = target.level();
            if (level != null) {
                return level.getGameTime();
            }
        }
        return 0L;
    }

    private static String resolvePlayerName(ServerPlayer performer) {
        if (performer != null && performer.getGameProfile() != null) {
            String name = performer.getGameProfile().getName();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        return "unknown";
    }

    private static boolean preferUiOrderEnabled() {
        CCConfig config = ChestCavity.config;
        if (config == null) {
            return true;
        }
        CCConfig.GuScriptExecutionConfig execution = config.GUSCRIPT_EXECUTION;
        return execution == null || execution.preferUiOrder;
    }

    private static final class PendingFlowStart {
        private final FlowProgram program;
        private final Map<String, String> params;
        private final String source;
        private final ResourceLocation flowId;
        private final String rootName;
        private final int rootIndex;
        private double sessionScale = 1.0D;

        private PendingFlowStart(FlowProgram program, Map<String, String> params, String source,
                                 ResourceLocation flowId, String rootName, int rootIndex) {
            this.program = program;
            this.params = params;
            this.source = source == null ? "operator" : source;
            this.flowId = flowId;
            this.rootName = rootName;
            this.rootIndex = rootIndex;
        }

        private void snapshotSessionScale(double sessionScale) {
            this.sessionScale = sessionScale;
        }

        private FlowProgram program() {
            return program;
        }

        private Map<String, String> params() {
            return params;
        }

        private String source() {
            return source;
        }

        private ResourceLocation flowId() {
            return flowId;
        }

        private String rootName() {
            return rootName;
        }

        private int rootIndex() {
            return rootIndex;
        }

        private double sessionScale() {
            return sessionScale;
        }
    }

    private record OrderedRoot(GuNode node,
                               int originalIndex,
                               int executionOrder,
                               String ruleId,
                               String name,
                               int primaryPageIndex,
                               int primarySlotIndex) {

        private static final int NO_INDEX = Integer.MAX_VALUE;

        static OrderedRoot from(GuNode node, int originalIndex) {
            PrimaryIndexCollector indices = gatherIndices(node);
            return new OrderedRoot(
                    node,
                    originalIndex,
                    computeExecutionOrder(node),
                    computeRuleId(node),
                    node.name(),
                    indices.pageIndex(),
                    indices.slotIndex()
            );
        }

        static int compareExecutionFirst(OrderedRoot left, OrderedRoot right) {
            int leftOrder = left.executionOrder;
            int rightOrder = right.executionOrder;
            boolean leftUnordered = leftOrder == Integer.MAX_VALUE;
            boolean rightUnordered = rightOrder == Integer.MAX_VALUE;
            if (leftUnordered && rightUnordered) {
                return 0;
            }
            if (leftOrder != rightOrder) {
                return Integer.compare(leftOrder, rightOrder);
            }
            int ruleComparison = left.ruleId.compareTo(right.ruleId);
            if (ruleComparison != 0) {
                return ruleComparison;
            }
            int nameComparison = left.name.compareTo(right.name);
            if (nameComparison != 0) {
                return nameComparison;
            }
            return Integer.compare(left.originalIndex, right.originalIndex);
        }

        private static int computeExecutionOrder(GuNode node) {
            if (node instanceof OperatorGuNode operator) {
                return operator.executionOrder().orElse(Integer.MAX_VALUE);
            }
            return Integer.MAX_VALUE;
        }

        private static String computeRuleId(GuNode node) {
            if (node instanceof OperatorGuNode operator) {
                return operator.operatorId();
            }
            return node.kind().name();
        }

        private static PrimaryIndexCollector gatherIndices(GuNode node) {
            PrimaryIndexCollector collector = new PrimaryIndexCollector();
            collector.collect(node);
            return collector;
        }

        private static final class PrimaryIndexCollector {
            private int minPage = NO_INDEX;
            private int minSlot = NO_INDEX;

            void collect(GuNode node) {
                if (node == null) {
                    return;
                }
                if (node instanceof LeafGuNode leaf) {
                    if (leaf.pageIndex() >= 0) {
                        minPage = Math.min(minPage, leaf.pageIndex());
                    }
                    if (leaf.slotIndex() >= 0) {
                        minSlot = Math.min(minSlot, leaf.slotIndex());
                    }
                }
                for (GuNode child : node.children()) {
                    collect(child);
                }
            }

            int pageIndex() {
                return minPage;
            }

            int slotIndex() {
                return minSlot;
            }
        }
    }
}
