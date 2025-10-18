package net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.llm;

import java.util.ArrayList;
import java.util.List;

import net.tigereye.chestcavity.soul.fakeplayer.brain.intent.IntentSnapshot;
import net.tigereye.chestcavity.soul.fakeplayer.brain.intent.LLMIntent;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.BrainActionStep;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrain;
import net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrainContext;

/**
 * 负责消费 LLM 下发的自然语言指令，并在 {@link net.tigereye.chestcavity.soul.fakeplayer.brain.subbrain.SubBrainMemory}
 * 中维护对话上下文。暂不直接驱动具体动作，仅记录最新的指令历史，供后续行动子脑读取。
 */
public final class LLMDrivenSubBrain extends SubBrain {

    private static final String MEMORY_KEY_STATE = "llm.conversation";

    public LLMDrivenSubBrain() {
        super("llm.conversation");
        addStep(BrainActionStep.always(this::syncConversationState));
    }

    @Override
    public boolean shouldTick(SubBrainContext ctx) {
        IntentSnapshot snapshot = ctx.intent();
        if (snapshot != null && snapshot.isPresent() && snapshot.intent() instanceof LLMIntent llmIntent) {
            return llmIntent.hasInstructions();
        }
        // 若已有历史上下文未被消费，也允许继续 tick，避免在指令 TTL 结束后丢失状态。
        return ctx.memory().getIfPresent(MEMORY_KEY_STATE) != null;
    }

    @Override
    public void onExit(SubBrainContext ctx) {
        ctx.memory().clear();
    }

    private void syncConversationState(SubBrainContext ctx) {
        IntentSnapshot snapshot = ctx.intent();
        if (snapshot == null || !snapshot.isPresent()) {
            return;
        }
        if (!(snapshot.intent() instanceof LLMIntent llmIntent) || !llmIntent.hasInstructions()) {
            return;
        }
        ConversationState state = ctx.memory().get(MEMORY_KEY_STATE, ConversationState::new);
        List<String> instructions = llmIntent.instructions();
        for (int i = state.processedCount; i < instructions.size(); i++) {
            String line = instructions.get(i);
            if (line == null || line.isBlank()) {
                continue;
            }
            state.history.add(line);
            // 控制对话历史长度，避免无限增长。
            if (state.history.size() > state.maxHistory) {
                state.history.remove(0);
            }
        }
        state.processedCount = instructions.size();
        ctx.memory().put(MEMORY_KEY_STATE, state);
    }

    private static final class ConversationState {
        private final List<String> history = new ArrayList<>();
        private final int maxHistory = 32;
        private int processedCount = 0;
    }
}

