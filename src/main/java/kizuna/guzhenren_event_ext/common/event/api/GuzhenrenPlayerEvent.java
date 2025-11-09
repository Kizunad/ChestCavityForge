package kizuna.guzhenren_event_ext.common.event.api;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Base class for all custom events related to a player in the Guzhenren Event Extension.
 */
public abstract class GuzhenrenPlayerEvent extends PlayerEvent {

    public GuzhenrenPlayerEvent(Player player) {
        super(player);
    }
}
