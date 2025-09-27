package net.tigereye.chestcavity.guscript.runtime.exec;

import net.minecraft.world.entity.player.Player;

/**
 * Runtime context passed to action execution, providing entity references and bridges.
 */
public interface GuScriptContext {

    Player performer();

    GuScriptExecutionBridge bridge();
}
