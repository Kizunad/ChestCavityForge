package net.tigereye.chestcavity.guscript.runtime;

import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.ast.GuNode;
import net.tigereye.chestcavity.guscript.ast.GuNodeKind;
import net.tigereye.chestcavity.guscript.ast.OperatorGuNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Performs recursive reduction from leaf nodes into an operator tree according to reaction rules.
 */
public final class GuScriptReducer {

    private static final int MAX_ITERATIONS = 512;

    public record ReductionResult(List<GuNode> roots, List<ReactionApplication> applications) {
        public ReductionResult {
            roots = List.copyOf(roots);
            applications = List.copyOf(applications);
        }
    }

    public record ReactionApplication(ReactionRule rule, List<GuNode> inputs, GuNode output) {
        public ReactionApplication {
            inputs = List.copyOf(inputs);
        }
    }

    public ReductionResult reduce(List<GuNode> leafNodes, List<ReactionRule> rules) {
        if (leafNodes == null || leafNodes.isEmpty()) {
            return new ReductionResult(List.of(), List.of());
        }
        List<GuNode> pool = new ArrayList<>(leafNodes);
        List<ReactionApplication> journal = new ArrayList<>();
        List<ReactionRule> sortedRules = new ArrayList<>(rules);
        sortedRules.sort(Comparator.comparingInt(ReactionRule::priority).reversed());

        Set<FailureKey> failedCombos = new HashSet<>();
        int iterations = 0;
        while (iterations++ < MAX_ITERATIONS) {
            Optional<ApplicationCandidate> candidate = findBestApplication(pool, sortedRules, failedCombos);
            if (candidate.isEmpty()) {
                break;
            }
            ApplicationCandidate best = candidate.get();
            Optional<GuNode> maybeNode = best.rule.tryApply(best.selectedNodes);
            if (maybeNode.isEmpty()) {
                ChestCavity.LOGGER.warn("[GuScript] Rule {} refused nodes {}, skipping", best.rule.id(), best.selectedNodes);
                failedCombos.add(FailureKey.from(best.rule, best.selectedNodes));
                continue;
            }
            GuNode result = adjustCompositeKind(maybeNode.get());
            journal.add(new ReactionApplication(best.rule, best.selectedNodes, result));
            removeSelected(pool, best.selectedIndices);
            pool.add(result);
            ChestCavity.LOGGER.debug("[GuScript] Applied rule {} -> {}", best.rule.id(), result.name());
        }

        if (iterations >= MAX_ITERATIONS) {
            ChestCavity.LOGGER.warn("[GuScript] Reduction aborted after {} iterations to prevent infinite loop", MAX_ITERATIONS);
        }

        return new ReductionResult(pool, journal);
    }

    private static Optional<ApplicationCandidate> findBestApplication(List<GuNode> pool, List<ReactionRule> rules,
                                                                      Set<FailureKey> failures) {
        ApplicationCandidate best = null;
        for (ReactionRule rule : rules) {
            Optional<ApplicationCandidate> candidate = findApplicationForRule(pool, rule, failures);
            if (candidate.isEmpty()) {
                continue;
            }
            ApplicationCandidate app = candidate.get();
            if (best == null || app.isBetterThan(best)) {
                best = app;
            }
        }
        return Optional.ofNullable(best);
    }

    private static Optional<ApplicationCandidate> findApplicationForRule(List<GuNode> pool, ReactionRule rule,
                                                                         Set<FailureKey> failures) {
        int arity = rule.arity();
        if (pool.size() < arity) {
            return Optional.empty();
        }
        ApplicationCandidate[] best = new ApplicationCandidate[1];
        search(pool, rule, failures, 0, -1, new ArrayList<>(), new HashMap<>(), candidate -> {
            if (best[0] == null || candidate.isBetterThan(best[0])) {
                best[0] = candidate;
            }
        });
        return Optional.ofNullable(best[0]);
    }

    private static void search(List<GuNode> pool, ReactionRule rule, Set<FailureKey> failures,
                               int depth, int lastIndex, List<Integer> indices,
                               Map<String, Integer> tagCounts, Consumer<ApplicationCandidate> acceptor) {
        if (depth == rule.arity()) {
            List<GuNode> selected = indices.stream().map(pool::get).toList();
            if (!coversRequired(tagCounts, rule.requiredTagCounts())) {
                return;
            }
            if (containsInhibitor(tagCounts, rule.inhibitors())) {
                return;
            }
            FailureKey key = FailureKey.from(rule, selected);
            if (failures.contains(key)) {
                return;
            }
            ApplicationCandidate candidate = new ApplicationCandidate(rule, new ArrayList<>(indices), selected,
                    Map.copyOf(tagCounts), coverageScore(tagCounts, rule));
            acceptor.accept(candidate);
            return;
        }

        for (int i = lastIndex + 1; i <= pool.size() - (rule.arity() - depth); i++) {
            GuNode node = pool.get(i);
            indices.add(i);
            Map<String, Integer> nextCounts = new HashMap<>(tagCounts);
            node.tags().forEach(tag -> nextCounts.merge(tag, 1, Integer::sum));
            search(pool, rule, failures, depth + 1, i, indices, nextCounts, acceptor);
            indices.remove(indices.size() - 1);
        }
    }

    private static boolean coversRequired(Map<String, Integer> tagCounts, Map<String, Integer> requiredCounts) {
        for (Map.Entry<String, Integer> entry : requiredCounts.entrySet()) {
            if (tagCounts.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsInhibitor(Map<String, Integer> tagCounts, Set<String> inhibitors) {
        for (String inhibitor : inhibitors) {
            if (tagCounts.containsKey(inhibitor)) {
                return true;
            }
        }
        return false;
    }

    private static int coverageScore(Map<String, Integer> counts, ReactionRule rule) {
        int score = 0;
        if (rule.requiredTagCounts().isEmpty()) {
            return counts.values().stream().mapToInt(Integer::intValue).sum();
        }
        for (Map.Entry<String, Integer> entry : rule.requiredTagCounts().entrySet()) {
            int have = counts.getOrDefault(entry.getKey(), 0);
            score += Math.min(have, entry.getValue());
        }
        return score;
    }

    private static void removeSelected(List<GuNode> pool, List<Integer> selectedIndices) {
        List<Integer> sorted = new ArrayList<>(selectedIndices);
        Collections.sort(sorted, Comparator.reverseOrder());
        for (int index : sorted) {
            pool.remove(index);
        }
    }

    private static GuNode adjustCompositeKind(GuNode node) {
        if (node instanceof OperatorGuNode operator) {
            if (operator.kind() == GuNodeKind.OPERATOR && operator.children().stream().noneMatch(GuNode::isLeaf)) {
                return new OperatorGuNode(operator.operatorId(), operator.name(), GuNodeKind.COMPOSITE,
                        operator.tags(), operator.actions(), operator.children());
            }
        }
        return node;
    }

    private record ApplicationCandidate(ReactionRule rule, List<Integer> selectedIndices,
                                        List<GuNode> selectedNodes, Map<String, Integer> tagCounts,
                                        int coverageScore) {
        boolean isBetterThan(ApplicationCandidate other) {
            if (coverageScore != other.coverageScore) {
                return coverageScore > other.coverageScore;
            }
            if (rule.priority() != other.rule.priority()) {
                return rule.priority() > other.rule.priority();
            }
            return rule.id().compareTo(other.rule.id()) < 0;
        }
    }

    private record FailureKey(String ruleId, List<Integer> nodeIdentity) {
        static FailureKey from(ReactionRule rule, List<GuNode> nodes) {
            List<Integer> ids = nodes.stream()
                    .map(System::identityHashCode)
                    .sorted()
                    .toList();
            return new FailureKey(rule.id(), ids);
        }
    }
}
