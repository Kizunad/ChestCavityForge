package net.tigereye.chestcavity.guscript.runtime.reduce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import java.util.List;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.guscript.ast.GuNode;
import net.tigereye.chestcavity.guscript.ast.GuNodeKind;
import net.tigereye.chestcavity.guscript.ast.LeafGuNode;
import net.tigereye.chestcavity.guscript.ast.OperatorGuNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GuScriptReducerAdjacencyTieBreakTest {

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
  void tighterAdjacencyWinsWhenPrimarySlotEquals() {
    GuScriptReducer reducer = new GuScriptReducer();
    List<GuNode> leaves =
        List.of(leaf("Core", "core", 0), leaf("Adj", "adj", 1), leaf("Wide", "wide", 2));

    ReactionRule tight = rule("test:tight", ImmutableMultiset.of("core", "adj"), "Tight");
    ReactionRule wide = rule("test:wide", ImmutableMultiset.of("core", "wide"), "Wide");

    GuScriptReducer.ReductionResult result = reducer.reduce(leaves, List.of(tight, wide));

    assertFalse(result.applications().isEmpty());
    assertEquals("test:tight", result.applications().getFirst().rule().id());
    assertEquals("Tight", result.roots().getFirst().name());
  }

  private static LeafGuNode leaf(String name, String tag, int slotIndex) {
    return new LeafGuNode(name, ImmutableMultiset.of(tag), List.of(), 0, slotIndex);
  }

  private static ReactionRule rule(
      String id, ImmutableMultiset<String> requiredTags, String resultName) {
    return ReactionRule.builder(id)
        .arity(requiredTags.size())
        .requiredTags(requiredTags)
        .priority(10)
        .operator(
            (ruleId, inputs) ->
                new OperatorGuNode(
                    ruleId,
                    resultName,
                    GuNodeKind.OPERATOR,
                    HashMultiset.create(),
                    List.of(),
                    inputs,
                    null,
                    false,
                    false))
        .build();
  }
}
