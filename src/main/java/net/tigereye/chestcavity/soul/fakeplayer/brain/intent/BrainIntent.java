package net.tigereye.chestcavity.soul.fakeplayer.brain.intent;

/**
 * Marker interface for high-level intents issued by the command/UI层。
 * Intent 仅作为“任务目标”的快照输入给 Brain，不直接触发 Action。
 */
public interface BrainIntent {
    /**
     * 建议的剩余生效时长（tick）。若 <=0 则视为立即过期，仅本tick可见。
     */
    int ttlTicks();
}

