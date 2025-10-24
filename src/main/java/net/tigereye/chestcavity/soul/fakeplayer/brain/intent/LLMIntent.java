package net.tigereye.chestcavity.soul.fakeplayer.brain.intent;

import java.util.List;
import java.util.Objects;

/** 面向 LLM/SaaS 指令的意图封装。允许一次性携带多条自然语言任务描述， 子脑将依据这些描述与运行时上下文驱动后续行动。 */
public record LLMIntent(List<String> instructions, int ttlTicks) implements BrainIntent {

  public LLMIntent {
    instructions = List.copyOf(Objects.requireNonNullElseGet(instructions, List::of));
    ttlTicks = Math.max(0, ttlTicks);
  }

  /** 是否存在至少一条待处理的 LLM 指令。 */
  public boolean hasInstructions() {
    return !instructions.isEmpty();
  }

  public static LLMIntent of(List<String> instructions, int ttl) {
    return new LLMIntent(instructions, ttl);
  }
}
