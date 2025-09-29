package net.tigereye.chestcavity.guscript.ast;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.util.ArrayList;
import java.util.List;

/**
 * Leaf node representing an individual Gu (蛊虫) with intrinsic tags and actions.
 */
public final class LeafGuNode implements GuNode {
    private final String name;
    private final Multiset<String> tags;
    private final List<Action> actions;
    private final int pageIndex;
    private final int slotIndex;

    public LeafGuNode(String name, Multiset<String> tags, List<Action> actions) {
        this(name, tags, actions, -1, -1);
    }

    public LeafGuNode(String name, Multiset<String> tags, List<Action> actions, int pageIndex, int slotIndex) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Leaf node must have a name");
        }
        this.name = name;
        this.tags = tags == null ? HashMultiset.create() : HashMultiset.create(tags);
        this.actions = actions == null ? new ArrayList<>() : new ArrayList<>(actions);
        this.pageIndex = pageIndex;
        this.slotIndex = slotIndex;
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

    /**
     * Zero-based page index for UI-driven ordering. {@code -1} if unspecified.
     */
    public int pageIndex() {
        return pageIndex;
    }

    /**
     * Zero-based slot index within the originating page. {@code -1} if unspecified.
     */
    public int slotIndex() {
        return slotIndex;
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
                ", pageIndex=" + pageIndex +
                ", slotIndex=" + slotIndex +
                '}';
    }
}
