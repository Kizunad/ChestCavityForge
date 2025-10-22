package net.tigereye.chestcavity.guscript.runtime.reduce;

import java.util.List;
import net.tigereye.chestcavity.guscript.ast.GuNode;

@FunctionalInterface
public interface ReactionOperator {
  GuNode apply(String ruleId, List<GuNode> inputs);
}
