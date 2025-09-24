package net.tigereye.chestcavity.compat.guzhenren.network;

import java.util.Map;

import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.compat.guzhenren.GuzhenrenResourceBridge;

@FunctionalInterface
public interface GuzhenrenPayloadListener {
    void onPlayerVariablesSynced(Player player, GuzhenrenResourceBridge.ResourceHandle handle, Map<String, Double> snapshot);
}
