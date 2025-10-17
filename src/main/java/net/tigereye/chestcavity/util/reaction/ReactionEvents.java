package net.tigereye.chestcavity.util.reaction;

import com.mojang.logging.LogUtils;
import net.tigereye.chestcavity.util.reaction.ReactionRegistry.ReactionContext;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;

/**
 * 轻量事件分发器，供 ReactionRegistry 将“火衣 + 油涂层”等反应回调给器官行为。
 */
public final class ReactionEvents {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ReactionEvents() {}

    private static final List<Consumer<ReactionContext>> FIRE_OIL_LISTENERS = new ArrayList<>();
    private static final List<ToDoubleFunction<ReactionContext>> FIRE_OIL_POWER_LISTENERS = new ArrayList<>();

    public static void registerFireOilListener(Consumer<ReactionContext> listener) {
        FIRE_OIL_LISTENERS.add(Objects.requireNonNull(listener, "listener"));
    }

    public static void registerFireOilPowerListener(ToDoubleFunction<ReactionContext> listener) {
        FIRE_OIL_POWER_LISTENERS.add(Objects.requireNonNull(listener, "listener"));
    }

    static void fireOil(ReactionContext context) {
        if (FIRE_OIL_LISTENERS.isEmpty()) {
            return;
        }
        for (Consumer<ReactionContext> listener : FIRE_OIL_LISTENERS) {
            try {
                listener.accept(context);
            } catch (Throwable t) {
                LOGGER.warn("[reaction] fireOil listener failed: {}", t.toString());
            }
        }
    }

    static float fireOilPowerBonus(ReactionContext context) {
        if (FIRE_OIL_POWER_LISTENERS.isEmpty()) {
            return 0.0F;
        }
        double total = 0.0D;
        for (ToDoubleFunction<ReactionContext> listener : FIRE_OIL_POWER_LISTENERS) {
            try {
                total += listener.applyAsDouble(context);
            } catch (Throwable t) {
                LOGGER.warn("[reaction] fireOil power listener failed: {}", t.toString());
            }
        }
        return (float) total;
    }
}
