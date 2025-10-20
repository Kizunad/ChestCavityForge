package net.tigereye.chestcavity.util.reaction;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.tigereye.chestcavity.engine.dot.DoTEngine;
import net.tigereye.chestcavity.util.DoTTypes;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 公共龙焰助手：负责管理龙焰印记层数、持续时间与 DoT 调度。
 */
public final class DragonFlameHelper {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MAX_STACKS = 6;
    private static final int BASE_DURATION_TICKS = 6 * 20;
    private static final double BASE_DPS_PER_STACK = 6.0D;
    private static final double ATTACK_PERCENT_PER_STACK = 0.005D;
    private static final double TARGET_MAX_HEALTH_PERCENT = 0.002D;

    private static final Map<UUID, UUID> LAST_ATTACKER = new HashMap<>();

    private DragonFlameHelper() {
    }

    public static int baseDurationTicks() {
        return BASE_DURATION_TICKS;
    }

    public static int getStacks(LivingEntity target) {
        return ReactionTagOps.count(target, ReactionTagKeys.DRAGON_FLAME_MARK);
    }

    public static int applyStacks(LivingEntity attacker, LivingEntity target, int deltaStacks) {
        return applyStacks(attacker, target, deltaStacks, BASE_DURATION_TICKS);
    }

    public static int applyStacks(LivingEntity attacker, LivingEntity target, int deltaStacks, int durationTicks) {
        if (target == null || deltaStacks <= 0) {
            return getStacks(target);
        }
        int current = getStacks(target);
        int desired = Math.min(MAX_STACKS, current + deltaStacks);
        return setStacks(attacker, target, desired, durationTicks);
    }

    public static void refreshDuration(LivingEntity attacker, LivingEntity target, int extraTicks) {
        if (target == null || extraTicks <= 0) {
            return;
        }
        int stacks = getStacks(target);
        if (stacks <= 0) {
            return;
        }
        int duration = BASE_DURATION_TICKS + Math.max(0, extraTicks);
        setStacks(attacker, target, stacks, duration);
    }

    public static void clear(LivingEntity target) {
        if (target == null) {
            return;
        }
        UUID targetId = target.getUUID();
        ReactionTagOps.clear(target, ReactionTagKeys.DRAGON_FLAME_MARK);
        UUID attackerId = LAST_ATTACKER.remove(targetId);
        DoTEngine.cancel(attackerId, targetId, DoTTypes.YAN_DAO_DRAGONFLAME);
    }

    private static int setStacks(LivingEntity attacker, LivingEntity target, int stacks, int durationTicks) {
        if (target == null || stacks <= 0) {
            if (target != null) {
                clear(target);
            }
            return 0;
        }
        durationTicks = Math.max(20, durationTicks);
        stacks = Mth.clamp(stacks, 1, MAX_STACKS);

        int current = getStacks(target);
        int delta = stacks - current;
        ReactionTagOps.addStacked(target, ReactionTagKeys.DRAGON_FLAME_MARK, delta, durationTicks);

        LivingEntity storedAttacker = resolveStoredAttacker(target);
        LivingEntity effectiveAttacker = attacker != null ? attacker : storedAttacker;
        UUID targetId = target.getUUID();
        if (effectiveAttacker != null) {
            LAST_ATTACKER.put(targetId, effectiveAttacker.getUUID());
        } else {
            LAST_ATTACKER.remove(targetId);
        }

        UUID cancelAttackerId = effectiveAttacker != null ? effectiveAttacker.getUUID() : null;
        DoTEngine.cancel(cancelAttackerId, targetId, DoTTypes.YAN_DAO_DRAGONFLAME);

        if (effectiveAttacker == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[dragon_flame] skip scheduling due to missing attacker (target={})", targetId);
            }
            return stacks;
        }

        scheduleDoT(effectiveAttacker, target, stacks, durationTicks);
        return stacks;
    }

    private static void scheduleDoT(LivingEntity attacker, LivingEntity target, int stacks, int durationTicks) {
        if (attacker == null || target == null || stacks <= 0) {
            return;
        }
        int seconds = Math.max(1, Mth.ceil(durationTicks / 20.0F));
        double attackDamage = attacker.getAttribute(Attributes.ATTACK_DAMAGE) != null
                ? attacker.getAttribute(Attributes.ATTACK_DAMAGE).getValue() : 0.0D;
        double base = BASE_DPS_PER_STACK * stacks;
        double attackBonus = attackDamage * ATTACK_PERCENT_PER_STACK * stacks;
        double healthBonus = target.getMaxHealth() * TARGET_MAX_HEALTH_PERCENT;
        double perSecond = Math.max(0.0D, base + attackBonus + healthBonus);
        if (perSecond <= 0.0D) {
            return;
        }
        DoTEngine.schedulePerSecond(attacker, target, perSecond, seconds,
                null, 1.0f, 1.0f,
                DoTTypes.YAN_DAO_DRAGONFLAME,
                null, DoTEngine.FxAnchor.TARGET, net.minecraft.world.phys.Vec3.ZERO, 1.0f);
    }

    private static LivingEntity resolveStoredAttacker(LivingEntity target) {
        if (target == null || !(target.level() instanceof ServerLevel level)) {
            return null;
        }
        UUID stored = LAST_ATTACKER.get(target.getUUID());
        if (stored == null) {
            return null;
        }
        Entity entity = level.getEntity(stored);
        return entity instanceof LivingEntity living ? living : null;
    }
}

