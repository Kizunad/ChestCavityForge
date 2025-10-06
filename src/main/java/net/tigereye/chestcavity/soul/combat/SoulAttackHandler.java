package net.tigereye.chestcavity.soul.combat;

import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

/**
 * A pluggable combat action. Registry will iterate handlers in registration order
 * and invoke the first that is both in range and returns true from tryAttack.
 */
public interface SoulAttackHandler {
    /** Preferred maximum distance for this attack to engage. */
    double getRange(SoulPlayer self, LivingEntity target);

    /** Attempt to attack. Return true if the handler consumed this tick to perform the action. */
    boolean tryAttack(AttackContext ctx);
}

