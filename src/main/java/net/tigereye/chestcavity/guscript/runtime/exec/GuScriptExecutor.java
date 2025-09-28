package net.tigereye.chestcavity.guscript.runtime.exec;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.guscript.ast.GuNode;
import net.tigereye.chestcavity.guscript.data.BindingTarget;
import net.tigereye.chestcavity.guscript.data.GuScriptAttachment;
import net.tigereye.chestcavity.guscript.data.GuScriptPageState;
import net.tigereye.chestcavity.guscript.data.GuScriptProgramCache;
import net.tigereye.chestcavity.guscript.runtime.action.DefaultGuScriptExecutionBridge;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executes compiled GuScript programs for a player.
 */
public final class GuScriptExecutor {

    private static final GuScriptRuntime RUNTIME = new GuScriptRuntime();
    private static final int DEFAULT_MAX_KEYBIND_PAGES = 16;
    private static final int DEFAULT_MAX_KEYBIND_ROOTS = 64;

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
        AtomicInteger rootIndex = new AtomicInteger();
        RUNTIME.executeAll(cache.roots(), () -> {
            int index = rootIndex.getAndIncrement();
            DefaultGuScriptExecutionBridge bridge = new DefaultGuScriptExecutionBridge(player, actualTarget, index);
            return new DefaultGuScriptContext(player, actualTarget, bridge);
        });
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
        AtomicInteger rootIndex = new AtomicInteger();
        RUNTIME.executeAll(aggregatedRoots, () -> {
            int index = rootIndex.getAndIncrement();
            DefaultGuScriptExecutionBridge bridge = new DefaultGuScriptExecutionBridge(player, actualTarget, index);
            return new DefaultGuScriptContext(player, actualTarget, bridge);
        });
    }
}
