package net.tigereye.chestcavity.guscript.runtime.exec;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public record SimpleGuScriptContext(Player performer, LivingEntity target, GuScriptExecutionBridge bridge) implements GuScriptContext {
    public SimpleGuScriptContext {
        if (bridge == null) {
            throw new IllegalArgumentException("bridge must not be null");
        }
    }
}
