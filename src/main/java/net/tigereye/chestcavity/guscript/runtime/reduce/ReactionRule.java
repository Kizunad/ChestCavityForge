package net.tigereye.chestcavity.guscript.runtime.reduce;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import net.tigereye.chestcavity.guscript.ast.GuNode;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Defines how a set of nodes react into a new operator node.
 */
public final class ReactionRule {
    private final String id;
    private final ImmutableMultiset<String> requiredTags;
    private final Set<String> inhibitors;
    private final int priority;
    private final int arity;
    private final ReactionOperator operator;
    private final Predicate<List<GuNode>> guard;

    private ReactionRule(Builder builder) {
        this.id = builder.id;
        this.requiredTags = ImmutableMultiset.copyOf(builder.requiredTags);
        this.inhibitors = Collections.unmodifiableSet(new HashSet<>(builder.inhibitors));
        this.priority = builder.priority;
        this.arity = builder.arity;
        this.operator = builder.operator;
        this.guard = builder.guard;
    }

    public String id() {
        return id;
    }

    public ImmutableMultiset<String> requiredTags() {
        return requiredTags;
    }

    public Set<String> inhibitors() {
        return inhibitors;
    }

    public int priority() {
        return priority;
    }

    public int arity() {
        return arity;
    }

    public ReactionOperator operator() {
        return operator;
    }

    public Optional<GuNode> tryApply(List<GuNode> inputs) {
        if (inputs.size() != arity) {
            return Optional.empty();
        }
        if (!guard.test(inputs)) {
            return Optional.empty();
        }
        return Optional.of(operator.apply(id, inputs));
    }

    @Override
    public String toString() {
        return "ReactionRule{" +
                "id='" + id + '\'' +
                ", requiredTags=" + requiredTags +
                ", inhibitors=" + inhibitors +
                ", priority=" + priority +
                ", arity=" + arity +
                '}';
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private final Multiset<String> requiredTags = HashMultiset.create();
        private final Set<String> inhibitors = new HashSet<>();
        private int priority = 0;
        private int arity = 2;
        private ReactionOperator operator;
        private Predicate<List<GuNode>> guard = nodes -> true;

        private Builder(String id) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("ReactionRule id must be provided");
            }
            this.id = id;
        }

        public Builder requiredTags(Set<String> tags) {
            if (tags != null) {
                tags.forEach(tag -> requireTag(tag, 1));
            }
            return this;
        }

        public Builder requiredTags(Map<String, Integer> tagCounts) {
            if (tagCounts != null) {
                tagCounts.forEach(this::requireTag);
            }
            return this;
        }

        public Builder requiredTags(Multiset<String> tags) {
            if (tags != null) {
                tags.forEachEntry(this::requireTag);
            }
            return this;
        }

        public Builder requireTag(String tag) {
            return requireTag(tag, 1);
        }

        public Builder requireTag(String tag, int count) {
            if (tag == null || tag.isBlank()) {
                return this;
            }
            if (count <= 0) {
                throw new IllegalArgumentException("Tag count must be positive");
            }
            this.requiredTags.add(tag, count);
            return this;
        }

        public Builder inhibitors(Set<String> tags) {
            if (tags != null) {
                this.inhibitors.addAll(tags);
            }
            return this;
        }

        public Builder addInhibitor(String tag) {
            if (tag != null && !tag.isBlank()) {
                this.inhibitors.add(tag);
            }
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder arity(int arity) {
            if (arity <= 0) {
                throw new IllegalArgumentException("Arity must be positive");
            }
            this.arity = arity;
            return this;
        }

        public Builder operator(ReactionOperator operator) {
            this.operator = operator;
            return this;
        }

        public Builder guard(Predicate<List<GuNode>> guard) {
            this.guard = guard == null ? nodes -> true : guard;
            return this;
        }

        public ReactionRule build() {
            Objects.requireNonNull(operator, "ReactionRule requires an operator");
            return new ReactionRule(this);
        }
    }
}
