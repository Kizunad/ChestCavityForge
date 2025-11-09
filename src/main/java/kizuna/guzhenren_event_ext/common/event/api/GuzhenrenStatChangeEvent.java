package kizuna.guzhenren_event_ext.common.event.api;

import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

/**
 * Fired when a player's monitored Guzhenren stat changes.
 * This event is fired by the PlayerStatWatcher.
 */
public class GuzhenrenStatChangeEvent extends GuzhenrenPlayerEvent {

    private final String statIdentifier;
    private final double oldValue;
    private final double newValue;
    private final transient GuzhenrenResourceBridge.ResourceHandle resourceHandle;

    public GuzhenrenStatChangeEvent(Player player, String statIdentifier, double oldValue, double newValue, GuzhenrenResourceBridge.ResourceHandle resourceHandle) {
        super(player);
        this.statIdentifier = statIdentifier;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.resourceHandle = resourceHandle;
    }

    /**
     * @return The canonical field name of the stat that changed (e.g., "zhenyuan").
     */
    public String getStatIdentifier() {
        return statIdentifier;
    }

    /**
     * @return The value of the stat before the change was detected.
     */
    public double getOldValue() {
        return oldValue;
    }

    /**
     * @return The new value of the stat.
     */
    public double getNewValue() {
        return newValue;
    }

    /**
     * @return A resource handle for the player, allowing immediate interaction with their stats.
     */
    public GuzhenrenResourceBridge.ResourceHandle getResourceHandle() {
        return resourceHandle;
    }
}
