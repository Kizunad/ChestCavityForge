package kizuna.guzhenren_event_ext.common.event;

/**
 * 极简的内部事件总线占位实现。
 * <p>
 * 当前仓库中尚无订阅者注册到该事件总线，因此实现保持空操作，
 * 仅保留 API 以兼容现有调用，避免对 NeoForge EventBus 的额外依赖。
 */
public final class CustomEventBus {

    public static final CustomEventBus EVENT_BUS = new CustomEventBus();

    private CustomEventBus() {}

    public void register(Object target) {
        // 尚无订阅者，预留接口
    }

    public void post(Object event) {
        // 尚无订阅者，预留接口
    }
}
