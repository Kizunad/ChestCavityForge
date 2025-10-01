package net.tigereye.chestcavity.guscript.runtime.reduce;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.actions.ConsumeHealthAction;
import net.tigereye.chestcavity.guscript.actions.ConsumeZhenyuanAction;
import net.tigereye.chestcavity.guscript.actions.EmitProjectileAction;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.ast.GuNode;
import net.tigereye.chestcavity.guscript.ast.GuNodeKind;
import net.tigereye.chestcavity.guscript.ast.LeafGuNode;
import net.tigereye.chestcavity.guscript.ast.OperatorGuNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Developer-only helper that demonstrates the AST reduction pipeline with sample data.
 */
public final class GuScriptReducerDebug {

    private GuScriptReducerDebug() {}

    public static void logDemo() {
        GuNode bone = new LeafGuNode("骨蛊", ImmutableMultiset.of("骨"), List.of(new ConsumeHealthAction(2)));
        GuNode blood = new LeafGuNode("血蛊", ImmutableMultiset.of("血"), List.of(new ConsumeZhenyuanAction(3)));
        GuNode burst = new LeafGuNode("爆发蛊", ImmutableMultiset.of("爆发"),
                List.of(new EmitProjectileAction("explosion_shard", 6.0, null, null, null, null, null, null, null, null, null, null)));

        ReactionRule bloodBoneCore = ReactionRule.builder("blood_bone_core")
                .arity(2)
                .requiredTags(ImmutableMultiset.of("骨", "血"))
                .priority(10)
                .operator((ruleId, inputs) -> new OperatorGuNode(ruleId, "血骨核心", GuNodeKind.OPERATOR,
                        unionTags(inputs, "核心"), List.of(), inputs))
                .build();

        ReactionRule explosiveLance = ReactionRule.builder("blood_bone_explosion")
                .arity(2)
                .requiredTags(ImmutableMultiset.of("核心", "爆发"))
                .priority(5)
                .operator((ruleId, inputs) -> new OperatorGuNode(ruleId, "血骨爆裂枪", GuNodeKind.COMPOSITE,
                        unionTags(inputs, "杀招"),
                        List.of(new EmitProjectileAction("blood_burst", 12.0, null, null, null, null, null, null, null, null, null, null)), inputs))
                .build();

        GuScriptReducer reducer = new GuScriptReducer();
        GuScriptReducer.ReductionResult result = reducer.reduce(List.of(bone, blood, burst),
                List.of(bloodBoneCore, explosiveLance));

        ChestCavity.LOGGER.info("[GuScriptDemo] Reduction yielded {} root(s)", result.roots().size());
        for (GuNode root : result.roots()) {
            logNode(root, 0);
        }
    }

    private static Multiset<String> unionTags(List<GuNode> nodes, String extra) {
        Multiset<String> merged = HashMultiset.create();
        for (GuNode node : nodes) {
            merged.addAll(node.tags());
        }
        if (extra != null && !extra.isBlank()) {
            merged.add(extra);
        }
        return merged;
    }

    private static void logNode(GuNode node, int depth) {
        String indent = "  ".repeat(depth);
        ChestCavity.LOGGER.info("{}- {} [{}] tags={} actions={}", indent, node.name(), node.kind(),
                node.tags(), node.actions());
        for (GuNode child : node.children()) {
            logNode(child, depth + 1);
        }
    }
}
