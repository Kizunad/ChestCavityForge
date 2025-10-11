package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

/**
 * Helpers to apply mob effects in a guarded, data-driven way.
 */
public final class EffectOps {

    private EffectOps() {}

    public static boolean applyById(LivingEntity target, ResourceLocation effectId, int durationTicks, int amplifier,
                                    boolean showParticles, boolean showIcon) {
        if (target == null || effectId == null || durationTicks <= 0) {
            return false;
        }
        Optional<Holder.Reference<MobEffect>> effect = BuiltInRegistries.MOB_EFFECT.getHolder(effectId);
        if (effect.isEmpty()) {
            return false;
        }
        target.addEffect(new MobEffectInstance(effect.get(), durationTicks, Math.max(0, amplifier), false, showParticles, showIcon));
        return true;
    }

    public static void ensure(LivingEntity target, Holder<MobEffect> effect, int durationTicks, int amplifier,
                              boolean showParticles, boolean showIcon) {
        if (target == null || effect == null) {
            return;
        }
        if (durationTicks <= 0) {
            remove(target, effect);
            return;
        }
        target.addEffect(new MobEffectInstance(effect, durationTicks, Math.max(0, amplifier), false, showParticles, showIcon));
    }

    public static void ensure(LivingEntity target, MobEffect effect, int durationTicks, int amplifier,
                              boolean showParticles, boolean showIcon) {
        if (effect == null) {
            return;
        }
        ensure(target, holderOf(effect), durationTicks, amplifier, showParticles, showIcon);
    }

    public static void remove(LivingEntity target, Holder<MobEffect> effect) {
        if (target == null || effect == null) {
            return;
        }
        target.removeEffect(effect);
    }

    private static Holder<MobEffect> holderOf(MobEffect effect) {
        if (effect == null) {
            throw new IllegalArgumentException("effect");
        }
        return BuiltInRegistries.MOB_EFFECT.getResourceKey(effect)
                .flatMap(BuiltInRegistries.MOB_EFFECT::getHolder)
                .orElseThrow(() -> new IllegalStateException("Unregistered effect: " + effect));
    }

    public static void remove(LivingEntity target, MobEffect effect) {
        if (effect == null) {
            return;
        }
        remove(target, holderOf(effect));
    }

    public static int applyToAllById(List<LivingEntity> targets, ResourceLocation effectId, int durationTicks, int amplifier,
                                      boolean showParticles, boolean showIcon) {
        if (targets == null || targets.isEmpty()) {
            return 0;
        }
        int applied = 0;
        for (LivingEntity target : targets) {
            if (applyById(target, effectId, durationTicks, amplifier, showParticles, showIcon)) {
                applied++;
            }
        }
        return applied;
    }
}
