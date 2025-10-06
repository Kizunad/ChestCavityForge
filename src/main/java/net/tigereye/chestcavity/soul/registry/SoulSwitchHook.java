package net.tigereye.chestcavity.soul.registry;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Hook points around the switch flow. Implementations should be fast and robust.
 */
public interface SoulSwitchHook {
    default void preSwitch(ServerPlayer executor, UUID currentId, UUID targetId) {}
    default void postSwitch(ServerPlayer executor, UUID previousId, UUID targetId, boolean success) {}
}

