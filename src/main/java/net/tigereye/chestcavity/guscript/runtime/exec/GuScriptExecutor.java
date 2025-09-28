package net.tigereye.chestcavity.guscript.runtime.exec;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.guscript.ast.GuNode;
import net.tigereye.chestcavity.guscript.ast.OperatorGuNode;
import net.tigereye.chestcavity.guscript.data.BindingTarget;
import net.tigereye.chestcavity.guscript.data.GuScriptAttachment;
import net.tigereye.chestcavity.guscript.data.GuScriptPageState;
import net.tigereye.chestcavity.guscript.data.GuScriptProgramCache;
import net.tigereye.chestcavity.guscript.runtime.action.DefaultGuScriptExecutionBridge;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Executes compiled GuScript programs for a player.
 */
public final class GuScriptExecutor {

    private static final GuScriptRuntime RUNTIME = new GuScriptRuntime();
    private static final int DEFAULT_MAX_KEYBIND_PAGES = 16;
    private static final int DEFAULT_MAX_KEYBIND_ROOTS = 64;
    private static final double DEFAULT_SESSION_MULTIPLIER_CAP = 5.0D;
    private static final double DEFAULT_SESSION_FLAT_CAP = 20.0D;

    private GuScriptExecutor() {
    }

    public static void trigger(ServerPlayer player, LivingEntity target, GuScriptAttachment attachment) {
        if (player == null || attachment == null) {
            return;
        }
        int pageIndex = attachment.getCurrentPageIndex();
        GuScriptProgramCache cache = GuScriptCompiler.compilePageIfNeeded(
                attachment,
                attachment.activePage(),
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
        executeRootsWithSession(sortedRoots, player, actualTarget, session);
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
        executeRootsWithSession(sortedRoots, player, actualTarget, session);
    }

    static List<GuNode> sortRootsForSession(List<GuNode> roots) {
        if (roots == null || roots.isEmpty()) {
            return List.of();
        }
        List<OrderedRoot> ordered = new ArrayList<>(roots.size());
        for (int i = 0; i < roots.size(); i++) {
            GuNode node = roots.get(i);
            ordered.add(new OrderedRoot(node, i));
        }
        ordered.sort((left, right) -> {
            int leftOrder = left.order();
            int rightOrder = right.order();
            boolean leftUnordered = leftOrder == Integer.MAX_VALUE;
            boolean rightUnordered = rightOrder == Integer.MAX_VALUE;
            if (leftUnordered && rightUnordered) {
                return 0;
            }
            if (leftOrder != rightOrder) {
                return Integer.compare(leftOrder, rightOrder);
            }
            int ruleComparison = left.ruleId().compareTo(right.ruleId());
            if (ruleComparison != 0) {
                return ruleComparison;
            }
            int nameComparison = left.name().compareTo(right.name());
            if (nameComparison != 0) {
                return nameComparison;
            }
            return Integer.compare(left.originalIndex(), right.originalIndex());
        });
        return ordered.stream().map(OrderedRoot::node).toList();
    }

    private static void executeRootsWithSession(List<GuNode> roots, ServerPlayer performer, LivingEntity target, ExecutionSession session) {
        if (roots.isEmpty()) {
            return;
        }
        AtomicInteger rootIndex = new AtomicInteger();
        for (GuNode root : roots) {
            int index = rootIndex.getAndIncrement();
            DefaultGuScriptExecutionBridge bridge = new DefaultGuScriptExecutionBridge(performer, target, index);
            DefaultGuScriptContext context = new DefaultGuScriptContext(performer, target, bridge, session);
            if (root instanceof OperatorGuNode operator) {
                context.enableModifierExports(operator.exportMultiplier(), operator.exportFlat());
            }
            double beforeMultiplier = session.currentMultiplier();
            double beforeFlat = session.currentFlat();
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
            ChestCavity.LOGGER.info(
                    "[GuScript] Root {}#{} exported modifiers: delta(multiplier={}, flat={}), direct(multiplier={}, flat={}). Session {} -> {} / {} -> {}",
                    root.name(),
                    index,
                    formatDouble(deltaMultiplier),
                    formatDouble(deltaFlat),
                    formatDouble(context.directExportedMultiplier()),
                    formatDouble(context.directExportedFlat()),
                    formatDouble(beforeMultiplier),
                    formatDouble(afterMultiplier),
                    formatDouble(beforeFlat),
                    formatDouble(afterFlat)
            );
        }
    }

    private static void logRootOrdering(ServerPlayer player, int pageIndex, List<GuNode> roots) {
        if (roots.isEmpty()) {
            return;
        }
        String descriptor = roots.stream()
                .map(root -> {
                    OrderedRoot wrapper = new OrderedRoot(root, 0);
                    return wrapper.name() + "[order=" + (wrapper.order() == Integer.MAX_VALUE ? "∞" : wrapper.order()) + ",rule=" + wrapper.ruleId() + "]";
                })
                .collect(Collectors.joining(", "));
        ChestCavity.LOGGER.info(
                "[GuScript] Ordered execution for {}{}: {}",
                player.getGameProfile().getName(),
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

    private record OrderedRoot(GuNode node, int originalIndex) {
        int order() {
            if (node instanceof OperatorGuNode operator) {
                return operator.executionOrder().orElse(Integer.MAX_VALUE);
            }
            return Integer.MAX_VALUE;
        }

        String ruleId() {
            if (node instanceof OperatorGuNode operator) {
                return operator.operatorId();
            }
            return node.kind().name();
        }

        String name() {
            return node.name();
        }
    }
}
