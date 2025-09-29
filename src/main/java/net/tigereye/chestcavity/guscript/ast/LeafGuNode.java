package net.tigereye.chestcavity.guscript.ast;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Leaf node representing an individual Gu (蛊虫) with intrinsic tags and actions.
 */
public final class LeafGuNode implements GuNode {
    private final String name;
    private final Multiset<String> tags;
    private final List<Action> actions;
    private final int slotIndex;
    private final int pageIndex;

    public LeafGuNode(String name, Multiset<String> tags, List<Action> actions) {
        this(name, tags, actions, -1, -1);
    }

    public LeafGuNode(String name, Multiset<String> tags, List<Action> actions, int slotIndex) {
        this(name, tags, actions, slotIndex, -1);
    }

    public LeafGuNode(String name, Multiset<String> tags, List<Action> actions, int slotIndex, int pageIndex) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Leaf node must have a name");
        }
        this.name = name;
        this.tags = tags == null ? HashMultiset.create() : HashMultiset.create(tags);
        this.actions = actions == null ? new ArrayList<>() : new ArrayList<>(actions);
        this.slotIndex = slotIndex;
        this.pageIndex = pageIndex;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Multiset<String> tags() {
        return tags;
    }

    @Override
    public List<Action> actions() {
        return actions;
    }

    public OptionalInt slotIndex() {
        return slotIndex >= 0 ? OptionalInt.of(slotIndex) : OptionalInt.empty();
    }

    public OptionalInt pageIndex() {
        return pageIndex >= 0 ? OptionalInt.of(pageIndex) : OptionalInt.empty();
    }

    @Override
    public List<GuNode> children() {
        return List.of();
    }

    @Override
    public GuNodeKind kind() {
        return GuNodeKind.LEAF;
    }

    @Override
    public String toString() {
        return "LeafGuNode{" +
                "name='" + name + '\'' +
                ", tags=" + tags +
                ", actions=" + actions +
                ", slotIndex=" + slotIndex +
                ", pageIndex=" + pageIndex +
                '}';
    }
}
