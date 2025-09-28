package net.tigereye.chestcavity.guscript.runtime.flow;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Predicate that must pass for a transition to proceed.
 */
public interface FlowGuard {

    boolean test(Player performer, LivingEntity target, FlowController controller, long gameTime);

    String describe();
}
