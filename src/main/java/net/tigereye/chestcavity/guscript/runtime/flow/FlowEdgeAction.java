package net.tigereye.chestcavity.guscript.runtime.flow;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Side-effect performed when a transition fires.
 */
public interface FlowEdgeAction {

    void apply(Player performer, LivingEntity target, FlowController controller, long gameTime);

    String describe();
}
