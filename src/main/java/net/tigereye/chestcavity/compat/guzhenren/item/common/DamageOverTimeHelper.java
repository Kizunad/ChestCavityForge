package net.tigereye.chestcavity.compat.guzhenren.item.common;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.TickTask;
import org.slf4j.Logger;
import net.tigereye.chestcavity.engine.dot.DoTEngine;

/**
 * Utility helpers for scheduling simple damage-over-time pulses.
 */
public final class DamageOverTimeHelper {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DEBUG = true;

    private static final int TICKS_PER_SECOND = 20;

    private DamageOverTimeHelper() {
    }

    /**
     * Applies periodic true damage based on the attacker's base attack attribute.
     *
     * @param attacker          entity providing the damage source
     * @param target            entity receiving the damage
     * @param percentPerSecond  fraction of the base attack value dealt per second (e.g. 0.05 for 5%)
     * @param durationSeconds   total duration in seconds
     * @param tickSound         optional sound to play on each tick; may be {@code null}
     */
    public static void applyBaseAttackPercentDoT(
            LivingEntity attacker,
            LivingEntity target,
            double percentPerSecond,
            int durationSeconds,
            SoundEvent tickSound
    ) {
        applyBaseAttackPercentDoT(attacker, target, percentPerSecond, durationSeconds, tickSound, 0.8f, 1.0f);
    }

    /**
     * Applies periodic true damage based on the attacker's base attack attribute.
     *
     * @param attacker          entity providing the damage source
     * @param target            entity receiving the damage
     * @param percentPerSecond  fraction of the base attack value dealt per second (e.g. 0.05 for 5%)
     * @param durationSeconds   total duration in seconds
     * @param tickSound         optional sound to play on each tick; may be {@code null}
     * @param volume            playback volume for {@code tickSound}
     * @param pitch             playback pitch for {@code tickSound}
     */
    public static void applyBaseAttackPercentDoT(
            LivingEntity attacker,
            LivingEntity target,
            double percentPerSecond,
            int durationSeconds,
            SoundEvent tickSound,
            float volume,
            float pitch
    ) {
        if (attacker == null || target == null) {
            return;
        }
        if (percentPerSecond <= 0.0 || durationSeconds <= 0) {
            return;
        }
        Level level = target.level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        AttributeInstance attackAttribute = attacker.getAttribute(Attributes.ATTACK_DAMAGE);
        double baseAttack = attackAttribute != null ? attackAttribute.getBaseValue() : 0.0;
        if (baseAttack <= 0.0) {
            return;
        }
        double perSecondDamage = baseAttack * percentPerSecond;
        if (perSecondDamage <= 0.0) {
            return;
        }

        // 默认类型：按攻击力百分比的通用 DoT
        applyBaseAttackPercentDoT(attacker, target, percentPerSecond, durationSeconds, tickSound, volume, pitch,
                net.tigereye.chestcavity.util.DoTTypes.ATTACK_BASE_PERCENT);
    }

    /**
     * 带类型标识的 DoT（基于攻击力百分比）。
     */
    public static void applyBaseAttackPercentDoT(
            LivingEntity attacker,
            LivingEntity target,
            double percentPerSecond,
            int durationSeconds,
            SoundEvent tickSound,
            float volume,
            float pitch,
            net.minecraft.resources.ResourceLocation dotTypeId
    ) {
        if (attacker == null || target == null) {
            return;
        }
        if (percentPerSecond <= 0.0 || durationSeconds <= 0) {
            return;
        }
        Level level = target.level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        AttributeInstance attackAttribute = attacker.getAttribute(Attributes.ATTACK_DAMAGE);
        double baseAttack = attackAttribute != null ? attackAttribute.getBaseValue() : 0.0;
        if (baseAttack <= 0.0) {
            return;
        }
        double perSecondDamage = baseAttack * percentPerSecond;
        if (perSecondDamage <= 0.0) {
            return;
        }

        DoTEngine.schedulePerSecond(attacker, target, perSecondDamage, durationSeconds, tickSound, volume, pitch,
                dotTypeId, null, net.tigereye.chestcavity.engine.dot.DoTEngine.FxAnchor.TARGET, net.minecraft.world.phys.Vec3.ZERO, 1.0f);
    }

    // Legacy local scheduler retained for reference; avoid using for DoT.
    private static void schedule(ServerLevel server, Runnable runnable, int delayTicks) {
        if (delayTicks <= 0) {
            runnable.run();
            return;
        }
        int targetTick = server.getServer().getTickCount() + delayTicks;
        server.getServer().tell(new TickTask(targetTick, runnable));
    }
}
