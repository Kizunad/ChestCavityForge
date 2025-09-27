package net.tigereye.chestcavity.guscript.runtime;

import net.tigereye.chestcavity.guscript.ast.GuNode;

import java.util.List;

@FunctionalInterface
public interface ReactionOperator {
    GuNode apply(String ruleId, List<GuNode> inputs);
}
