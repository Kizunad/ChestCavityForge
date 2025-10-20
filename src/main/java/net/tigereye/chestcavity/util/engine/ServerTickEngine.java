package net.tigereye.chestcavity.util.engine;

import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 服务器 Post tick 执行单元。用于在 {@link TickEngineHub} 中按优先级统一调度。
 */
@FunctionalInterface
public interface ServerTickEngine {

    /**
     * 服务端每 tick（Post 阶段）调用。
     * @param event 对应的服务器 tick 事件
     */
    void onServerTick(ServerTickEvent.Post event);
}

