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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.NonNullList;
import net.tigereye.chestcavity.guscript.data.BindingTarget;
import net.tigereye.chestcavity.guscript.data.GuScriptPageState;
import net.tigereye.chestcavity.guscript.data.ListenerType;

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
        LeafGuNode scaled = GuScriptCompiler.toScaledLeaf(definition, 3, 0, 0);
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
        LeafGuNode scaled = GuScriptCompiler.toScaledLeaf(definition, 1, 0, 0);
        assertEquals(1, scaled.tags().count("骨道"));

    }

    @Test
    void computeSignature_ignoresFlowParamOrdering() throws Exception {
        GuScriptPageState forward = newPageState(Map.of(
                "alpha", "1",
                "beta", "2"
        ));

        GuScriptPageState reversed = newPageState(Map.of(
                "beta", "2",
                "alpha", "1"
        ));

        Method computeSignature = GuScriptCompiler.class.getDeclaredMethod("computeSignature", GuScriptPageState.class);
        computeSignature.setAccessible(true);
        int forwardSignature = (int) computeSignature.invoke(null, forward);
        int reversedSignature = (int) computeSignature.invoke(null, reversed);

        assertEquals(forwardSignature, reversedSignature, "Flow parameter ordering should not change signature");
    }

    private static GuScriptPageState newPageState(Map<String, String> params) throws Exception {
        GuScriptPageState page = allocatePageState();
        page.setFlowParams(params);
        return page;
    }

    @SuppressWarnings("unchecked")
    private static GuScriptPageState allocatePageState() throws Exception {
        Constructor<Object> objectConstructor = Object.class.getDeclaredConstructor();
        objectConstructor.setAccessible(true);
        Constructor<GuScriptPageState> serializationCtor = (Constructor<GuScriptPageState>) sun.reflect.ReflectionFactory
                .getReflectionFactory()
                .newConstructorForSerialization(GuScriptPageState.class, objectConstructor);
        serializationCtor.setAccessible(true);
        GuScriptPageState page = serializationCtor.newInstance();
        setField(page, "items", NonNullList.create());
        setField(page, "bindingTarget", BindingTarget.KEYBIND);
        setField(page, "listenerType", ListenerType.ON_HIT);
        setField(page, "title", "Page");
        setField(page, "dirty", true);
        setField(page, "inventorySignature", 0);
        setField(page, "compiledProgram", null);
        setField(page, "listenerCooldowns", new EnumMap<>(ListenerType.class));
        setField(page, "flowId", null);
        setField(page, "flowParams", new HashMap<>());
        return page;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = GuScriptPageState.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
