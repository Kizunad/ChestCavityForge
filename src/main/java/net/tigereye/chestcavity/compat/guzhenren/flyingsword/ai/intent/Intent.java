package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent;

import java.util.Optional;

/**
 * 单个 Intent：仅做“是否要做 + 做什么”的决策，不负责移动。
 */
public interface Intent {
  /**
   * 评估是否触发该意图，返回结果与优先级。
   */
  Optional<IntentResult> evaluate(AIContext ctx);

  /**
   * 便于调试/日志的名称。
   */
  String name();
}

