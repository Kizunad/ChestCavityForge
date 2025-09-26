package net.tigereye.chestcavity.compat.guzhenren.network.event;

import java.util.Map;
import java.util.OptionalDouble;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

public final class GuzhenrenPlayerVariablesSyncedEvent extends Event {

    private final Player player;
    private final GuzhenrenResourceBridge.ResourceHandle handle;
    private final Map<String, Double> snapshot;

    public GuzhenrenPlayerVariablesSyncedEvent(Player player, GuzhenrenResourceBridge.ResourceHandle handle, Map<String, Double> snapshot) {
        this.player = player;
        this.handle = handle;
        this.snapshot = snapshot;
    }

    public Player player() {
        return player;
    }

    public GuzhenrenResourceBridge.ResourceHandle handle() {
        return handle;
    }

    public Map<String, Double> snapshot() {
        return snapshot;
    }

    public OptionalDouble get(String identifier) {
        if (handle == null) {
            return OptionalDouble.empty();
        }
        return handle.read(identifier);
    }
}
