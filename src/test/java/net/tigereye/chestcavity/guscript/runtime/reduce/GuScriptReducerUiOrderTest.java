package net.tigereye.chestcavity.guscript.runtime.reduce;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.guscript.ast.GuNode;
import net.tigereye.chestcavity.guscript.ast.GuNodeKind;
import net.tigereye.chestcavity.guscript.ast.LeafGuNode;
import net.tigereye.chestcavity.guscript.ast.OperatorGuNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GuScriptReducerUiOrderTest {

    @BeforeEach
    void setUp() {
        ChestCavity.config = new CCConfig();
        ChestCavity.config.GUSCRIPT_EXECUTION.preferUiOrder = true;
    }

    @AfterEach
    void tearDown() {
        ChestCavity.config = null;
    }

    @Test
    void prefersCandidateWithLowerPrimarySlotIndex() {
        GuScriptReducer reducer = new GuScriptReducer();
        List<GuNode> leaves = List.of(
                leaf("A", "a", 0),
                leaf("B", "b", 1),
                leaf("C", "c", 2),
                leaf("D", "d", 3)
        );

        ReactionRule leftmostRule = rule(
                "test:leftmost",
                ImmutableMultiset.of("a", "c"),
                "Leftmost"
        );
        ReactionRule rightRule = rule(
                "test:right",
                ImmutableMultiset.of("b", "d"),
                "Right"
        );

        GuScriptReducer.ReductionResult result = reducer.reduce(leaves, List.of(leftmostRule, rightRule));

        assertFalse(result.applications().isEmpty(), "Expected at least one reaction application");
        assertEquals("test:leftmost", result.applications().getFirst().rule().id(),
                "Rule covering the lower UI slot should have been selected first");
        assertEquals("Leftmost", result.roots().getFirst().name());
    }

    private static LeafGuNode leaf(String name, String tag, int slotIndex) {
        return new LeafGuNode(name, ImmutableMultiset.of(tag), List.of(), 0, slotIndex);
    }

    private static ReactionRule rule(String id, ImmutableMultiset<String> requiredTags, String resultName) {
        return ReactionRule.builder(id)
                .arity(requiredTags.size())
                .requiredTags(requiredTags)
                .priority(10)
                .operator((ruleId, inputs) -> new OperatorGuNode(
                        ruleId,
                        resultName,
                        GuNodeKind.OPERATOR,
                        HashMultiset.create(),
                        List.of(),
                        inputs,
                        null,
                        false,
                        false
                ))
                .build();
    }
}
