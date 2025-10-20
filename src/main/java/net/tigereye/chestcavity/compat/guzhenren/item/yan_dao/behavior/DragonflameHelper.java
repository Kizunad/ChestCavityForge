package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.engine.dot.DoTEngine;
import net.tigereye.chestcavity.util.DoTTypes;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;

/**
 * Shared helpers for applying the龙焰印记 DoT stacks.
 */
public final class DragonflameHelper {

    private static final int MAX_STACKS = 6;
    private static final int BASE_DURATION_TICKS = 120; // 6 秒
    private static final double BASE_DAMAGE_PER_STACK = 6.0D;
    private static final double ATTACK_PERCENT_PER_STACK = 0.005D;
    private static final double TARGET_HEALTH_PERCENT_PER_STACK = 0.002D;
    private static final double OIL_COATING_MULTIPLIER = 1.35D;

    private DragonflameHelper() {
    }

    public static void applyDragonflame(LivingEntity attacker, LivingEntity target, int stacksDelta) {
        applyDragonflame(attacker, target, stacksDelta, true);
    }

    public static void applyDragonflame(LivingEntity attacker, LivingEntity target, int stacksDelta, boolean refreshDuration) {
        if (target == null) {
            return;
        }
        if (!(target.level() instanceof ServerLevel)) {
            return;
        }
        int currentStacks = ReactionTagOps.count(target, ReactionTagKeys.DRAGON_FLAME_MARK);
        int desiredStacks = Math.max(0, Math.min(MAX_STACKS, currentStacks + stacksDelta));
        if (desiredStacks <= 0) {
            ReactionTagOps.clear(target, ReactionTagKeys.DRAGON_FLAME_MARK);
            DoTEngine.cancel(attacker, target, DoTTypes.YAN_DAO_DRAGONFLAME);
            return;
        }
        int delta = desiredStacks - currentStacks;
        int durationTicks = BASE_DURATION_TICKS;
        if (delta != 0) {
            ReactionTagOps.addStacked(target, ReactionTagKeys.DRAGON_FLAME_MARK, delta, durationTicks);
        } else if (refreshDuration) {
            ReactionTagOps.add(target, ReactionTagKeys.DRAGON_FLAME_MARK, durationTicks);
        }

        DoTEngine.cancel(attacker, target, DoTTypes.YAN_DAO_DRAGONFLAME);

        double baseDamage = BASE_DAMAGE_PER_STACK * desiredStacks;
        double attackBonus = 0.0D;
        if (attacker != null) {
            AttributeInstance attribute = attacker.getAttribute(Attributes.ATTACK_DAMAGE);
            double attackBase = attribute != null ? attribute.getBaseValue() : 0.0D;
            attackBonus = attackBase * ATTACK_PERCENT_PER_STACK * desiredStacks;
        }
        double healthBonus = target.getMaxHealth() * TARGET_HEALTH_PERCENT_PER_STACK * desiredStacks;
        double perSecondDamage = baseDamage + attackBonus + healthBonus;
        if (ReactionTagOps.has(target, ReactionTagKeys.OIL_COATING)) {
            perSecondDamage *= OIL_COATING_MULTIPLIER;
        }
        if (perSecondDamage <= 0.0D) {
            return;
        }
        int durationSeconds = Math.max(1, durationTicks / 20);
        DoTEngine.schedulePerSecond(attacker, target, perSecondDamage, durationSeconds,
                null, 1.0f, 1.0f,
                DoTTypes.YAN_DAO_DRAGONFLAME,
                null,
                DoTEngine.FxAnchor.TARGET,
                Vec3.ZERO,
                1.0f);
    }

    public static int getStacks(LivingEntity target) {
        return ReactionTagOps.count(target, ReactionTagKeys.DRAGON_FLAME_MARK);
    }
}
