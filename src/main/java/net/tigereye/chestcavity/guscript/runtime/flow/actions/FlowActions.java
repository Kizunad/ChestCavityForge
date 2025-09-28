package net.tigereye.chestcavity.guscript.runtime.flow.actions;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.ast.Action;
import net.tigereye.chestcavity.guscript.runtime.action.DefaultGuScriptExecutionBridge;
import net.tigereye.chestcavity.guscript.runtime.exec.DefaultGuScriptContext;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.tigereye.chestcavity.guscript.ability.guzhenren.blood_bone_bomb.BloodBoneBombAbility;

import java.util.List;

/**
 * Built-in flow actions used by the MVP implementation.
 */
public final class FlowActions {

    private FlowActions() {
    }

    public static FlowEdgeAction consumeResource(String identifier, double amount) {
        if (amount <= 0) {
            return describe(() -> "consume_resource(nop)");
        }
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (performer == null || identifier == null) {
                    return;
                }
                GuzhenrenResourceBridge.open(performer).ifPresent(handle -> {
                    double delta = -Math.abs(amount);
                    handle.adjustDouble(identifier, delta, true);
                });
            }

            @Override
            public String describe() {
                return "consume_resource(" + identifier + ", " + amount + ")";
            }
        };
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
