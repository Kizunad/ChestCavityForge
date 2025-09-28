package net.tigereye.chestcavity.guscript.runtime.flow.guards;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowGuard;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

/**
 * Built-in flow guards.
 */
public final class FlowGuards {

    private FlowGuards() {
    }

    public static FlowGuard hasResource(String identifier, double minimum) {
        return new FlowGuard() {
            @Override
            public boolean test(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null || identifier == null) {
                    return false;
                }
                var handleOpt = GuzhenrenResourceBridge.open(performer);
                if (handleOpt.isEmpty()) {
                    return false;
                }
                var valueOpt = handleOpt.get().read(identifier);
                return valueOpt.isPresent() && valueOpt.getAsDouble() >= minimum;
            }

            @Override
            public String describe() {
                return "has_resource(" + identifier + ", " + minimum + ")";
            }
        };
    }

    public static FlowGuard cooldownReady(String key) {
        return new FlowGuard() {
            @Override
            public boolean test(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (controller == null || key == null) {
                    return false;
                }
                return controller.isCooldownReady(key, gameTime);
            }

            @Override
            public String describe() {
                return "cooldown_ready(" + key + ")";
            }
        };
    }

    /** Guard: performer has at least the specified health. */
    public static FlowGuard healthAtLeast(double minimum) {
        return new FlowGuard() {
            @Override
            public boolean test(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                return performer != null && performer.getHealth() >= (float) minimum;
            }

            @Override
            public String describe() {
                return "health_at_least(" + minimum + ")";
            }
        };
    }

    /** Guard: performer health is strictly below the specified value. */
    public static FlowGuard healthBelow(double maximumExclusive) {
        return new FlowGuard() {
            @Override
            public boolean test(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                return performer != null && performer.getHealth() < (float) maximumExclusive;
            }

            @Override
            public String describe() {
                return "health_below(" + maximumExclusive + ")";
            }
        };
    }

    /** Guard: given resource is strictly below the specified value. */
    public static FlowGuard resourceBelow(String identifier, double maximumExclusive) {
        return new FlowGuard() {
            @Override
            public boolean test(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null || identifier == null) {
                    return false;
                }
                var handleOpt = GuzhenrenResourceBridge.open(performer);
                if (handleOpt.isEmpty()) {
                    return false;
                }
                var valueOpt = handleOpt.get().read(identifier);
                return valueOpt.isPresent() && valueOpt.getAsDouble() < maximumExclusive;
            }

            @Override
            public String describe() {
                return "resource_below(" + identifier + ", " + maximumExclusive + ")";
            }
        };
    }

    public static FlowGuard variableAtMost(String name, double maximumInclusive, boolean asDouble) {
        return new FlowGuard() {
            @Override
            public boolean test(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (controller == null || name == null) {
                    return false;
                }
                if (asDouble) {
                    return controller.getDouble(name, Double.NEGATIVE_INFINITY) <= maximumInclusive;
                }
                return controller.getLong(name, Long.MIN_VALUE) <= (long) Math.floor(maximumInclusive);
            }

            @Override
            public String describe() {
                return "variable_at_most(" + name + ", " + maximumInclusive + ")";
            }
        };
    }

    public static FlowGuard variableAtLeast(String name, double minimumInclusive, boolean asDouble) {
        return new FlowGuard() {
            @Override
            public boolean test(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (controller == null || name == null) {
                    return false;
                }
                if (asDouble) {
                    return controller.getDouble(name, Double.POSITIVE_INFINITY) >= minimumInclusive;
                }
                return controller.getLong(name, Long.MAX_VALUE) >= (long) Math.ceil(minimumInclusive);
            }

            @Override
            public String describe() {
                return "variable_at_least(" + name + ", " + minimumInclusive + ")";
            }
        };
    }
}
