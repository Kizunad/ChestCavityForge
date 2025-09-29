package net.tigereye.chestcavity.guscript.runtime.exec;

import com.google.common.collect.ImmutableMultiset;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.guscript.ast.GuNode;
import net.tigereye.chestcavity.guscript.ast.GuNodeKind;
import net.tigereye.chestcavity.guscript.ast.LeafGuNode;
import net.tigereye.chestcavity.guscript.ast.OperatorGuNode;
import net.tigereye.chestcavity.guscript.registry.GuScriptRegistry;
import net.tigereye.chestcavity.guscript.runtime.reduce.GuScriptReducer;
import net.tigereye.chestcavity.guscript.runtime.reduce.ReactionRule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuScriptCompilerTest {

    private static final ResourceLocation TEST_ITEM_ID = ResourceLocation.fromNamespaceAndPath("chestcavity", "test_leaf");

    @BeforeEach
    void setUpRegistry() {
        GuScriptRegistry.updateLeaves(Map.of(
                TEST_ITEM_ID,
                new GuScriptRegistry.LeafDefinition(
                        "骨道叶",
                        ImmutableMultiset.of("骨道"),
                        List.of()
                )
        ));

        ReactionRule doubleBone = ReactionRule.builder("chestcavity:test_double_bone")
                .arity(1)
                .requiredTags(ImmutableMultiset.of("骨道", "骨道"))
                .priority(10)
                .operator((ruleId, inputs) -> new OperatorGuNode(
                        ruleId,
                        "骨道合鸣",
                        GuNodeKind.OPERATOR,
                        ImmutableMultiset.of("测试"),
                        List.of(),
                        inputs
                ))
                .build();

        GuScriptRegistry.updateReactionRules(List.of(doubleBone));
    }

    @AfterEach
    void tearDownRegistry() {
        GuScriptRegistry.updateLeaves(Map.of());
        GuScriptRegistry.updateReactionRules(List.of());
    }

    @Test
    void toScaledLeaf_multipliesTagsByCount() {
        GuScriptRegistry.LeafDefinition definition = GuScriptRegistry.leaf(TEST_ITEM_ID).orElseThrow();
        LeafGuNode scaled = GuScriptCompiler.toScaledLeaf(definition, 3, 0);
        assertEquals(3, scaled.tags().count("骨道"));

        GuScriptReducer reducer = new GuScriptReducer();
        GuScriptReducer.ReductionResult result = reducer.reduce(List.of(scaled), GuScriptRegistry.reactionRules());

        List<GuNode> roots = result.roots();
        assertEquals(1, roots.size(), "Expected scaled leaf to satisfy double tag requirement");
        assertEquals("骨道合鸣", roots.getFirst().name());
    }

    @Test
    void toScaledLeaf_requiresEnoughCountToMatch() {
        GuScriptRegistry.LeafDefinition definition = GuScriptRegistry.leaf(TEST_ITEM_ID).orElseThrow();
        LeafGuNode scaled = GuScriptCompiler.toScaledLeaf(definition, 1, 0);
        assertEquals(1, scaled.tags().count("骨道"));

    }
}
