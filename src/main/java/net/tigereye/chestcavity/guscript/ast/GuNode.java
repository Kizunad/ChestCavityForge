package net.tigereye.chestcavity.guscript.ast;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Base interface for GuScript AST nodes.
 * Nodes expose their tags/actions and maintain child references for tree traversal.
 */
public sealed interface GuNode permits LeafGuNode, OperatorGuNode {

    String name();

    Set<String> tags();

    List<Action> actions();

    List<GuNode> children();

    GuNodeKind kind();

    default boolean isLeaf() {
        return kind() == GuNodeKind.LEAF;
    }

    default boolean isComposite() {
        return kind() == GuNodeKind.COMPOSITE;
    }

    default Set<String> immutableTags() {
        return Collections.unmodifiableSet(tags());
    }

    default List<Action> immutableActions() {
        return Collections.unmodifiableList(actions());
    }

    default List<GuNode> immutableChildren() {
        return Collections.unmodifiableList(children());
    }
}
