package kizuna.guzhenren_event_ext.common.event;

import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import net.neoforged.bus.api.SubscribeEvent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量级内部事件总线实现。
 * <p>
 * 用于在 Guzhenren Event Extension 内部转发自定义事件，避免直接依赖 NeoForge EventBus。
 * 支持通过 {@link SubscribeEvent} 注解注册订阅者方法，并在事件发布时自动调用匹配的方法。
 */
public final class CustomEventBus {

    public static final CustomEventBus EVENT_BUS = new CustomEventBus();

    /**
     * 存储订阅者对象及其订阅方法的映射表。
     * Key: 订阅者对象
     * Value: 该订阅者的所有订阅方法信息列表
     */
    private final Map<Object, List<SubscriberMethod>> subscribers = new ConcurrentHashMap<>();

    private CustomEventBus() {}

    /**
     * 注册一个订阅者对象。
     * <p>
     * 扫描该对象中所有带有 {@link SubscribeEvent} 注解的公共方法，
     * 并将其添加到事件订阅列表中。
     *
     * @param target 要注册的订阅者对象
     */
    public void register(Object target) {
        if (target == null) {
            GuzhenrenEventExtension.LOGGER.warn("Attempted to register null subscriber to CustomEventBus");
            return;
        }

        if (subscribers.containsKey(target)) {
            GuzhenrenEventExtension.LOGGER.warn("Subscriber {} is already registered", target.getClass().getSimpleName());
            return;
        }

        List<SubscriberMethod> methods = new ArrayList<>();
        Class<?> clazz = target.getClass();

        // 扫描所有公共方法
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(SubscribeEvent.class)) {
                // 验证方法签名：必须有且仅有一个参数
                if (method.getParameterCount() != 1) {
                    GuzhenrenEventExtension.LOGGER.warn(
                        "Method {} in {} has @SubscribeEvent but doesn't have exactly one parameter. Skipping.",
                        method.getName(), clazz.getSimpleName()
                    );
                    continue;
                }

                // 获取事件类型（方法的参数类型）
                Class<?> eventType = method.getParameterTypes()[0];
                method.setAccessible(true);

                methods.add(new SubscriberMethod(method, eventType));
                GuzhenrenEventExtension.LOGGER.debug(
                    "Registered event handler: {}.{}({})",
                    clazz.getSimpleName(), method.getName(), eventType.getSimpleName()
                );
            }
        }

        if (!methods.isEmpty()) {
            subscribers.put(target, methods);
            GuzhenrenEventExtension.LOGGER.info(
                "Registered subscriber {} with {} event handler(s)",
                clazz.getSimpleName(), methods.size()
            );
        } else {
            GuzhenrenEventExtension.LOGGER.warn(
                "Subscriber {} has no valid @SubscribeEvent methods",
                clazz.getSimpleName()
            );
        }
    }

    /**
     * 发布事件到所有订阅者。
     * <p>
     * 遍历所有已注册的订阅者方法，找到参数类型匹配的方法并调用。
     * 支持事件类型的继承关系（子类事件可以被父类参数的方法接收）。
     *
     * @param event 要发布的事件对象
     */
    public void post(Object event) {
        if (event == null) {
            GuzhenrenEventExtension.LOGGER.warn("Attempted to post null event to CustomEventBus");
            return;
        }

        Class<?> eventClass = event.getClass();
        int handlerCount = 0;

        // 遍历所有订阅者
        for (Map.Entry<Object, List<SubscriberMethod>> entry : subscribers.entrySet()) {
            Object subscriber = entry.getKey();
            List<SubscriberMethod> methods = entry.getValue();

            // 遍历该订阅者的所有订阅方法
            for (SubscriberMethod subscriberMethod : methods) {
                // 检查事件类型是否匹配（支持继承）
                if (subscriberMethod.eventType.isAssignableFrom(eventClass)) {
                    try {
                        subscriberMethod.method.invoke(subscriber, event);
                        handlerCount++;
                        GuzhenrenEventExtension.LOGGER.debug(
                            "Event {} handled by {}.{}",
                            eventClass.getSimpleName(),
                            subscriber.getClass().getSimpleName(),
                            subscriberMethod.method.getName()
                        );
                    } catch (Exception e) {
                        GuzhenrenEventExtension.LOGGER.error(
                            "Error invoking event handler {}.{} for event {}",
                            subscriber.getClass().getSimpleName(),
                            subscriberMethod.method.getName(),
                            eventClass.getSimpleName(),
                            e
                        );
                    }
                }
            }
        }

        if (handlerCount == 0) {
            GuzhenrenEventExtension.LOGGER.debug(
                "Event {} was posted but no handlers were found",
                eventClass.getSimpleName()
            );
        }
    }

    /**
     * 订阅者方法的元数据封装。
     */
    private static class SubscriberMethod {
        final Method method;
        final Class<?> eventType;

        SubscriberMethod(Method method, Class<?> eventType) {
            this.method = method;
            this.eventType = eventType;
        }
    }
}
