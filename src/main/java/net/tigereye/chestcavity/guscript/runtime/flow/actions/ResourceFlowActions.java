package net.tigereye.chestcavity.guscript.runtime.flow.actions;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.action.DefaultGuScriptExecutionBridge;
import net.tigereye.chestcavity.guscript.runtime.exec.DefaultGuScriptContext;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;

/**
 * 资源与基础效果相关的 Flow Action 实现。
 */
final class ResourceFlowActions {

    private static final double RESOURCE_TOLERANCE = 1.0E-6D;
    private static java.util.function.Function<Player, Optional<GuzhenrenResourceBridge.ResourceHandle>> resourceOpener = GuzhenrenResourceBridge::open;

    private ResourceFlowActions() {
    }

    static void overrideResourceOpenerForTests(java.util.function.Function<Player, Optional<ResourceHandle>> opener) {
        resourceOpener = opener == null ? GuzhenrenResourceBridge::open : opener;
    }

    static void resetResourceOpenerForTests() {
        resourceOpener = GuzhenrenResourceBridge::open;
    }

    static FlowEdgeAction consumeResource(String identifier, double amount) {
        if (identifier == null || identifier.isBlank()) {
            return FlowActionUtils.describe(() -> "consume_resource(nop)");
        }
        double sanitizedAmount = Math.max(0.0D, amount);
        if (sanitizedAmount <= 0.0D) {
            return FlowActionUtils.describe(() -> "consume_resource(nop)");
        }
        String canonicalId = identifier.trim();
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                double timeScale = 1.0D;
                if (controller != null) {
                    double resolved = controller.resolveFlowParamAsDouble("time.accelerate", 1.0D);
                    if (Double.isFinite(resolved) && resolved > 0.0D) {
                        timeScale = resolved;
                    }
                }
                double scaledAmount = sanitizedAmount / timeScale;
                if (!Double.isFinite(scaledAmount) || scaledAmount <= 0.0D) {
                    return;
                }

                Optional<ResourceHandle> handleOpt = resourceOpener.apply(performer);
                if (handleOpt.isEmpty()) {
                    logFailureAndCancel(performer, controller, gameTime, canonicalId, scaledAmount, sanitizedAmount, timeScale,
                            OptionalDouble.empty(), OptionalDouble.empty(), "missing_attachment");
                    return;
                }

                ResourceHandle handle = handleOpt.get();
                String normalized = canonicalId.toLowerCase(Locale.ROOT);
                OptionalDouble before = readResourceSnapshot(handle, normalized, canonicalId);

                if (!"zhenyuan".equals(normalized) && before.isPresent()) {
                    double available = before.getAsDouble();
                    if (!Double.isFinite(available) || available + RESOURCE_TOLERANCE < scaledAmount) {
                        logFailureAndCancel(
                                performer,
                                controller,
                                gameTime,
                                canonicalId,
                                scaledAmount,
                                sanitizedAmount,
                                timeScale,
                                before,
                                before,
                                "insufficient"
                        );
                        return;
                    }
                }

                OptionalDouble result;
                switch (normalized) {
                    case "jingli" -> result = handle.adjustJingli(-scaledAmount, true);
                    case "zhenyuan" -> result = handle.consumeScaledZhenyuan(scaledAmount);
                    default -> result = handle.adjustDouble(canonicalId, -scaledAmount, true);
                }

                if (result.isEmpty()) {
                    OptionalDouble after = readResourceSnapshot(handle, normalized, canonicalId);
                    logFailureAndCancel(
                            performer,
                            controller,
                            gameTime,
                            canonicalId,
                            scaledAmount,
                            sanitizedAmount,
                            timeScale,
                            before,
                            after,
                            "write_failed"
                    );
                    return;
                }

                if (!"zhenyuan".equals(normalized) && before.isPresent()) {
                    double available = before.getAsDouble();
                    double remaining = result.orElse(Double.NaN);
                    if (Double.isFinite(available) && Double.isFinite(remaining)) {
                        double paid = available - remaining;
                        if (paid + RESOURCE_TOLERANCE < scaledAmount) {
                            logFailureAndCancel(
                                    performer,
                                    controller,
                                    gameTime,
                                    canonicalId,
                                    scaledAmount,
                                    sanitizedAmount,
                                    timeScale,
                                    before,
                                    OptionalDouble.of(remaining),
                                    "insufficient_post"
                            );
                        }
                    }
                }
            }

            @Override
            public String describe() {
                return "consume_resource(" + canonicalId + ")";
            }
        };
    }

    static FlowEdgeAction setCooldown(String key, long durationTicks) {
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (controller == null || key == null) {
                    return;
                }
                controller.setCooldown(key, gameTime + Math.max(0L, durationTicks));
            }

            @Override
            public String describe() {
                return "set_cooldown(" + key + ", " + durationTicks + ")";
            }
        };
    }

    static FlowEdgeAction runActions(List<Action> actions) {
        List<Action> immutable = actions == null ? List.of() : List.copyOf(actions);
        if (immutable.isEmpty()) {
            return FlowActionUtils.describe(() -> "run_actions(nop)");
        }
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null) {
                    return;
                }
                DefaultGuScriptExecutionBridge bridge = new DefaultGuScriptExecutionBridge(performer, target);
                java.util.function.Function<String, String> resolver = controller != null
                        ? controller::resolveFlowParam
                        : null;
                DefaultGuScriptContext context = new DefaultGuScriptContext(performer, target, bridge, null, resolver);
                for (Action action : immutable) {
                    try {
                        action.execute(context);
                    } catch (Exception ex) {
                        ChestCavity.LOGGER.error("[Flow] Failed to run embedded action {}", action.id(), ex);
                    }
                }
            }

            @Override
            public String describe() {
                return "run_actions(" + immutable.size() + ")";
            }
        };
    }

    static FlowEdgeAction consumeHealth(double amount) {
        double value = Math.max(0.0, amount);
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null || value <= 0.0) return;
                performer.hurt(performer.damageSources().generic(), (float) value);
            }

            @Override
            public String describe() { return "consume_health(" + value + ")"; }
        };
    }

    static FlowEdgeAction applyEffect(ResourceLocation effectId, int duration, int amplifier, boolean showParticles, boolean showIcon) {
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null || effectId == null || duration <= 0) return;
                var holderOpt = BuiltInRegistries.MOB_EFFECT.getHolder(effectId);
                if (holderOpt.isEmpty()) {
                    ChestCavity.LOGGER.warn("[Flow] Unknown effect {} in apply_effect", effectId);
                    return;
                }
                performer.addEffect(new MobEffectInstance(holderOpt.get(), duration, Math.max(0, amplifier), false, showParticles, showIcon));
            }

            @Override
            public String describe() { return "apply_effect(" + effectId + ")"; }
        };
    }

    static FlowEdgeAction trueDamage(double amount) {
        double value = Math.max(0.0, amount);
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null || value <= 0.0) return;
                float startingHealth = performer.getHealth();
                float startingAbsorption = performer.getAbsorptionAmount();
                performer.invulnerableTime = 0;
                performer.hurt(performer.damageSources().magic(), (float) value);
                performer.invulnerableTime = 0;

                float remaining = (float) value;
                float absorptionConsumed = Math.min(startingAbsorption, remaining);
                remaining -= absorptionConsumed;
                performer.setAbsorptionAmount(Math.max(0.0f, startingAbsorption - absorptionConsumed));
                if (!performer.isDeadOrDying() && remaining > 0.0f) {
                    float expected = Math.max(0.0f, startingHealth - remaining);
                    if (performer.getHealth() > expected) {
                        performer.setHealth(expected);
                    }
                }
                performer.hurtTime = 0;
            }

            @Override
            public String describe() { return "true_damage(" + value + ")"; }
        };
    }

    private static OptionalDouble readResourceSnapshot(ResourceHandle handle, String normalized, String identifier) {
        return switch (normalized) {
            case "jingli" -> handle.getJingli();
            case "zhenyuan" -> handle.getZhenyuan();
            default -> handle.read(identifier);
        };
    }

    private static void logFailureAndCancel(
            Player performer,
            FlowController controller,
            long gameTime,
            String identifier,
            double scaledAmount,
            double originalAmount,
            double timeScale,
            OptionalDouble before,
            OptionalDouble after,
            String reason
    ) {
        String name = performer != null ? performer.getScoreboardName() : "<null>";
        double available = before.isPresent() ? before.getAsDouble() : Double.NaN;
        double remaining = after.isPresent() ? after.getAsDouble() : available;
        ChestCavity.LOGGER.debug(
                "[Flow] consume_resource failure for {} (identifier={}, scaledAmount={}, originalAmount={}, timeScale={}, reason={}, available={}, remaining={})",
                name,
                identifier,
                formatDouble(scaledAmount),
                formatDouble(originalAmount),
                formatDouble(timeScale),
                reason,
                formatDouble(available),
                formatDouble(remaining)
        );
        if (controller != null) {
            ChestCavity.LOGGER.warn(
                    "[Flow] {} cancelling due to resource failure (identifier={}, required={}, available={}, remaining={}, timeScale={}, reason={})",
                    name,
                    identifier,
                    formatDouble(scaledAmount),
                    formatDouble(available),
                    formatDouble(remaining),
                    formatDouble(timeScale),
                    reason
            );
            controller.requestCancel("resource_failure:" + identifier, gameTime);
        }
    }

    private static String formatDouble(double value) {
        if (!Double.isFinite(value)) {
            return "NaN";
        }
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
