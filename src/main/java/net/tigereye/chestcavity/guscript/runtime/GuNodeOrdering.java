package net.tigereye.chestcavity.guscript.runtime;

import net.tigereye.chestcavity.guscript.ast.GuNode;
import net.tigereye.chestcavity.guscript.ast.GuNodeKind;
import net.tigereye.chestcavity.guscript.ast.LeafGuNode;
import net.tigereye.chestcavity.guscript.ast.OperatorGuNode;

import java.util.Comparator;
import java.util.List;

/**
 * Shared helpers for ordering GuScript nodes deterministically.
 */
public final class GuNodeOrdering {

    private static final int DEFAULT_INDEX = Integer.MAX_VALUE;
    private static final int DEFAULT_ADJACENCY = Integer.MAX_VALUE;
    private static final int DEFAULT_PAGE_INDEX = Integer.MAX_VALUE;

    private GuNodeOrdering() {
    }

    public static void sort(List<GuNode> nodes, boolean preferUiOrder) {
        if (nodes == null || nodes.size() < 2) {
            return;
        }
        nodes.sort(comparator(preferUiOrder));
    }

    public static Comparator<GuNode> comparator(boolean preferUiOrder) {
        if (preferUiOrder) {
            return Comparator
                    .comparingInt(GuNodeOrdering::pageIndex)
                    .thenComparingInt(GuNodeOrdering::primarySlotIndex)
                    .thenComparingInt(GuNodeOrdering::executionOrder)
                    .thenComparingInt(GuNodeOrdering::adjacencySpan)
                    .thenComparing(GuNodeOrdering::operatorOrKindId)
                    .thenComparing(GuNode::name);
        }
        return Comparator
                .comparingInt(GuNodeOrdering::executionOrder)
                .thenComparing(GuNodeOrdering::operatorOrKindId)
                .thenComparing(GuNode::name);
    }

    public static int primarySlotIndex(GuNode node) {
        if (node instanceof LeafGuNode leaf) {
            int slot = leaf.slotIndex();
            return slot >= 0 ? slot : DEFAULT_INDEX;
        }
        if (node instanceof OperatorGuNode operator) {
            return operator.primarySlotIndex().orElse(DEFAULT_INDEX);
        }
        return DEFAULT_INDEX;
    }

    public static int executionOrder(GuNode node) {
        if (node instanceof OperatorGuNode operator) {
            return operator.executionOrder().orElse(Integer.MAX_VALUE);
        }
        return Integer.MAX_VALUE;
    }

    public static int adjacencySpan(GuNode node) {
        if (node instanceof OperatorGuNode operator) {
            return operator.adjacencySpanHint().orElse(DEFAULT_ADJACENCY);
        }
        return DEFAULT_ADJACENCY;
    }

    public static int pageIndex(GuNode node) {
        if (node instanceof LeafGuNode leaf) {
            int page = leaf.pageIndex();
            return page >= 0 ? page : DEFAULT_PAGE_INDEX;
        }
        if (node instanceof OperatorGuNode operator) {
            return operator.pageIndexHint().orElse(DEFAULT_PAGE_INDEX);
        }
        return DEFAULT_PAGE_INDEX;
    }

    public static String operatorOrKindId(GuNode node) {
        if (node instanceof OperatorGuNode operator) {
            return operator.operatorId();
        }
        GuNodeKind kind = node.kind();
        return kind == null ? "UNKNOWN" : kind.name();
    }
}
