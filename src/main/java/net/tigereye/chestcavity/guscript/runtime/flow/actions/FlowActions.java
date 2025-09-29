package net.tigereye.chestcavity.guscript.runtime.flow.actions;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Enemy;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.action.DefaultGuScriptExecutionBridge;
import net.tigereye.chestcavity.guscript.runtime.exec.DefaultGuScriptContext;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guscript.fx.FxEventParameters;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;

/**
 * Built-in flow actions used by the MVP implementation.
 */
public final class FlowActions {

    private static final double RESOURCE_TOLERANCE = 1.0E-6D;

    private FlowActions() {
    }

    private static Function<Player, Optional<GuzhenrenResourceBridge.ResourceHandle>> RESOURCE_OPENER = GuzhenrenResourceBridge::open;

    public static void overrideResourceOpenerForTests(Function<Player, Optional<GuzhenrenResourceBridge.ResourceHandle>> opener) {
        RESOURCE_OPENER = opener == null ? GuzhenrenResourceBridge::open : opener;
    }

    public static void resetResourceOpenerForTests() {
        RESOURCE_OPENER = GuzhenrenResourceBridge::open;
    }

    public static FlowEdgeAction consumeResource(String identifier, double amount) {
        if (identifier == null || identifier.isBlank()) {
            return describe(() -> "consume_resource(nop)");
        }
        double sanitizedAmount = Math.max(0.0D, amount);
        if (sanitizedAmount <= 0.0D) {
            return describe(() -> "consume_resource(nop)");
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

                Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = RESOURCE_OPENER.apply(performer);
                if (handleOpt.isEmpty()) {
                    logFailureAndCancel(performer, controller, gameTime, canonicalId, scaledAmount, sanitizedAmount, timeScale,
                            OptionalDouble.empty(), OptionalDouble.empty(), "missing_attachment");
                    return;
                }

                GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
                String normalized = canonicalId.toLowerCase(java.util.Locale.ROOT);
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
                                    "insufficient_paid"
                            );
                            return;
                        }
                    }
                }
            }

            @Override
            public String describe() {
                return "consume_resource(" + canonicalId + ", " + sanitizedAmount + ")";
            }
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

    private static OptionalDouble readResourceSnapshot(GuzhenrenResourceBridge.ResourceHandle handle, String normalized, String identifier) {
        return switch (normalized) {
            case "jingli" -> handle.getJingli();
            case "zhenyuan" -> handle.getZhenyuan();
            default -> handle.read(identifier);
        };
    }

    private static String formatDouble(double value) {
        if (!Double.isFinite(value)) {
            return "NaN";
        }
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    public static FlowEdgeAction setCooldown(String key, long durationTicks) {
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

    public static FlowEdgeAction runActions(List<Action> actions) {
        List<Action> immutable = actions == null ? List.of() : List.copyOf(actions);
        if (immutable.isEmpty()) {
            return describe(() -> "run_actions(nop)");
        }
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null) {
                    return;
                }
                DefaultGuScriptExecutionBridge bridge = new DefaultGuScriptExecutionBridge(performer, target);
                DefaultGuScriptContext context = new DefaultGuScriptContext(performer, target, bridge);
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

    /** Flow edge action: consume health (true HP loss), server-authoritative. */
    public static FlowEdgeAction consumeHealth(double amount) {
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

    /** Flow edge action: apply an arbitrary MobEffect to the performer. */
    public static FlowEdgeAction applyEffect(ResourceLocation effectId, int duration, int amplifier, boolean showParticles, boolean showIcon) {
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

    /** Flow edge action: apply true damage to the performer (ignores armor/absorption heuristically). */
    public static FlowEdgeAction trueDamage(double amount) {
        double value = Math.max(0.0, amount);
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null || value <= 0.0) return;
                // Approximate true damage by bypassing invulnerability and correcting absorption/health.
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

    /** Flow edge action: create an explosion at performer position. */
    public static FlowEdgeAction explode(float power) {
        float p = Math.max(0.0f, power);
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null || p <= 0.0f) return;
                if (!(performer.level() instanceof ServerLevel level)) return;
                level.explode(performer, performer.getX(), performer.getY(), performer.getZ(), p, net.minecraft.world.level.Level.ExplosionInteraction.MOB);
            }

            @Override
            public String describe() { return "explode(" + p + ")"; }
        };
    }

    public static FlowEdgeAction applyAttributeModifier(ResourceLocation attributeId, ResourceLocation modifierKey, AttributeModifier.Operation operation, double amount) {
        if (attributeId == null || modifierKey == null || operation == null || amount == 0.0D) {
            return describe(() -> "apply_attribute(nop)");
        }
        double sanitized = amount;
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null) {
                    return;
                }
                Holder.Reference<net.minecraft.world.entity.ai.attributes.Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(attributeId).orElse(null);
                if (attribute == null) {
                    ChestCavity.LOGGER.warn("[Flow] Unknown attribute {} in apply_attribute", attributeId);
                    return;
                }
                AttributeInstance instance = performer.getAttribute(attribute);
                if (instance == null) {
                    return;
                }
                instance.removeModifier(modifierKey);
                AttributeModifier modifier = new AttributeModifier(modifierKey, sanitized, operation);
                instance.addTransientModifier(modifier);
            }

            @Override
            public String describe() {
                return "apply_attribute(" + attributeId + ")";
            }
        };
    }

    public static FlowEdgeAction removeAttributeModifier(ResourceLocation attributeId, ResourceLocation modifierKey) {
        if (attributeId == null || modifierKey == null) {
            return describe(() -> "remove_attribute(nop)");
        }
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null) {
                    return;
                }
                Holder.Reference<net.minecraft.world.entity.ai.attributes.Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(attributeId).orElse(null);
                if (attribute == null) {
                    return;
                }
                AttributeInstance instance = performer.getAttribute(attribute);
                if (instance == null) {
                    return;
                }
                instance.removeModifier(modifierKey);
            }

            @Override
            public String describe() {
                return "remove_attribute(" + attributeId + ")";
            }
        };
    }

    public static FlowEdgeAction areaEffect(ResourceLocation effectId, int duration, int amplifier, double radius, String radiusVariable, boolean hostilesOnly, boolean includeSelf, boolean showParticles, boolean showIcon) {
        int actualDuration = Math.max(1, duration);
        double defaultRadius = Math.max(0.5D, radius);
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null || effectId == null) {
                    return;
                }
                var holderOpt = BuiltInRegistries.MOB_EFFECT.getHolder(effectId);
                if (holderOpt.isEmpty()) {
                    ChestCavity.LOGGER.warn("[Flow] Unknown effect {} in area_effect", effectId);
                    return;
                }
                if (!(performer.level() instanceof ServerLevel level)) {
                    return;
                }
                double resolvedRadius = defaultRadius;
                if (controller != null && radiusVariable != null) {
                    resolvedRadius = Math.max(0.5D, controller.getDouble(radiusVariable, defaultRadius));
                }
                Vec3 origin = performer.position();
                AABB box = new AABB(origin, origin).inflate(resolvedRadius);
                var effect = holderOpt.get();
                List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box, entity -> entity.isAlive() && (includeSelf || entity != performer));
                for (LivingEntity entity : entities) {
                    if (hostilesOnly && !(entity instanceof Enemy)) {
                        continue;
                    }
                    entity.addEffect(new MobEffectInstance(effect, actualDuration, Math.max(0, amplifier), false, showParticles, showIcon));
                }
            }

            @Override
            public String describe() {
                return "area_effect(" + effectId + ")";
            }
        };
    }

    public static FlowEdgeAction dampenProjectiles(double radius, String radiusVariable, double factor, int capPerTick) {
        double defaultRadius = Math.max(0.5D, radius);
        double actualFactor = Math.max(0.0D, factor);
        int cap = Math.max(1, capPerTick);
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null || !(performer.level() instanceof ServerLevel level)) {
                    return;
                }
                double resolvedRadius = defaultRadius;
                if (controller != null && radiusVariable != null) {
                    resolvedRadius = Math.max(0.5D, controller.getDouble(radiusVariable, defaultRadius));
                }
                Vec3 origin = performer.position();
                AABB box = new AABB(origin, origin).inflate(resolvedRadius);
                int remaining = cap;
                for (AbstractArrow arrow : level.getEntitiesOfClass(AbstractArrow.class, box, entity -> entity.isAlive())) {
                    dampenVelocity(arrow);
                    if (--remaining <= 0) {
                        return;
                    }
                }
                if (remaining <= 0) {
                    return;
                }
                for (ThrowableProjectile projectile : level.getEntitiesOfClass(ThrowableProjectile.class, box, entity -> entity.isAlive())) {
                    dampenVelocity(projectile);
                    if (--remaining <= 0) {
                        return;
                    }
                }
            }

            private void dampenVelocity(AbstractArrow arrow) {
                Vec3 motion = arrow.getDeltaMovement();
                Vec3 scaled = motion.scale(actualFactor);
                arrow.setDeltaMovement(scaled);
                arrow.hasImpulse = true;
            }

            private void dampenVelocity(ThrowableProjectile projectile) {
                Vec3 motion = projectile.getDeltaMovement();
                projectile.setDeltaMovement(motion.scale(actualFactor));
            }

            @Override
            public String describe() {
                return "dampen_projectiles(radius=" + defaultRadius + ", factor=" + actualFactor + ")";
            }
        };
    }

    public static FlowEdgeAction highlightHostiles(double radius, String radiusVariable, int durationTicks) {
        double defaultRadius = Math.max(0.5D, radius);
        int duration = Math.max(1, durationTicks);
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null || !(performer.level() instanceof ServerLevel level)) {
                    return;
                }
                double resolvedRadius = defaultRadius;
                if (controller != null && radiusVariable != null) {
                    resolvedRadius = Math.max(0.5D, controller.getDouble(radiusVariable, defaultRadius));
                }
                Vec3 origin = performer.position();
                AABB box = new AABB(origin, origin).inflate(resolvedRadius);
                List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box, entity -> entity instanceof Enemy && entity.isAlive());
                for (LivingEntity hostile : entities) {
                    hostile.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false, false));
                }
            }

            @Override
            public String describe() {
                return "highlight_hostiles(radius=" + defaultRadius + ")";
            }
        };
    }

    public static FlowEdgeAction setVariable(String name, double value, boolean asDouble) {
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (controller == null || name == null) {
                    return;
                }
                if (asDouble) {
                    controller.setDouble(name, value);
                } else {
                    controller.setLong(name, (long) Math.round(value));
                }
            }

            @Override
            public String describe() {
                return "set_variable(" + name + ")";
            }
        };
    }

    public static FlowEdgeAction addVariable(String name, double delta, boolean asDouble) {
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (controller == null || name == null) {
                    return;
                }
                if (asDouble) {
                    controller.addDouble(name, delta);
                } else {
                    controller.addLong(name, (long) Math.round(delta));
                }
            }

            @Override
            public String describe() {
                return "add_variable(" + name + ")";
            }
        };
    }

    public static FlowEdgeAction addVariableFromVariable(String targetName, String sourceName, double scale, boolean asDouble) {
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (controller == null || targetName == null || sourceName == null) {
                    return;
                }
                double delta = controller.getDouble(sourceName, 0.0D) * scale;
                if (asDouble) {
                    controller.addDouble(targetName, delta);
                } else {
                    controller.addLong(targetName, Math.round(delta));
                }
            }

            @Override
            public String describe() {
                return "add_variable_from_variable(" + sourceName + " -> " + targetName + ")";
            }
        };
    }

    public static FlowEdgeAction clampVariable(String name, double min, double max, boolean asDouble) {
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (controller == null || name == null) {
                    return;
                }
                if (asDouble) {
                    controller.clampDouble(name, min, max);
                } else {
                    controller.clampLong(name, (long) Math.floor(min), (long) Math.ceil(max));
                }
            }

            @Override
            public String describe() {
                return "clamp_variable(" + name + ")";
            }
        };
    }

    public static FlowEdgeAction setVariableFromParam(String paramKey, String variableName, double defaultValue) {
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (controller == null || variableName == null || paramKey == null) {
                    return;
                }
                double value = controller.resolveFlowParamAsDouble(paramKey, defaultValue);
                controller.setDouble(variableName, value);
            }

            @Override
            public String describe() {
                return "set_variable_from_param(" + paramKey + ")";
            }
        };
    }

    public static FlowEdgeAction copyVariable(String sourceName, String targetName, double scale, double offset, boolean asDouble) {
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (controller == null || sourceName == null || targetName == null) {
                    return;
                }
                double value = controller.getDouble(sourceName, 0.0D) * scale + offset;
                if (asDouble) {
                    controller.setDouble(targetName, value);
                } else {
                    controller.setLong(targetName, Math.round(value));
                }
            }

            @Override
            public String describe() {
                return "copy_variable(" + sourceName + " -> " + targetName + ")";
            }
        };
    }

    public static FlowEdgeAction emitFx(String fxId, float baseIntensity, String variableName, double defaultScale) {
        ResourceLocation fx = fxId == null ? null : ResourceLocation.tryParse(fxId);
        if (fx == null) {
            return describe(() -> "emit_fx(nop)");
        }
        float baseline = Math.max(0.0F, baseIntensity);
        double fallback = defaultScale;
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null) {
                    return;
                }
                DefaultGuScriptExecutionBridge bridge = DefaultGuScriptExecutionBridge.forPlayer(performer);
                double scale = fallback;
                if (controller != null && variableName != null) {
                    scale = controller.getDouble(variableName, fallback);
                }
                float intensity = (float) Mth.clamp(baseline * scale, 0.0D, 16.0D);
                bridge.playFx(fx, new FxEventParameters(Vec3.ZERO, Vec3.ZERO, intensity));
            }

            @Override
            public String describe() {
                return "emit_fx(" + fx + ")";
            }
        };
    }

    private static FlowEdgeAction describe(java.util.function.Supplier<String> description) {
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
            }

            @Override
            public String describe() {
                return description.get();
            }
        };
    }
}
