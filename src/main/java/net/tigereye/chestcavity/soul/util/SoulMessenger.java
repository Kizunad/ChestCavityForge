package net.tigereye.chestcavity.soul.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility for a soul to proactively send formatted messages to its owner.
 * Format: "[soul] [分魂] {name} : {content} (context)"
 */
public final class SoulMessenger {
    private SoulMessenger() {}

    private static final long DEFAULT_COOLDOWN_TICKS = 100; // ~5s @20tps
    private static final Map<UUID, Long> LAST_FLEE_SENT = new ConcurrentHashMap<>();

    public static void sendToOwner(SoulPlayer soul, String content, String context) {
        var ownerIdOpt = soul.getOwnerId();
        if (ownerIdOpt.isEmpty()) return;
        ServerPlayer owner = soul.serverLevel().getServer().getPlayerList().getPlayer(ownerIdOpt.get());
        if (owner == null) return; // owner offline
        String name = soul.getGameProfile() != null && soul.getGameProfile().getName() != null
                ? soul.getGameProfile().getName()
                : "Soul";
        String ctx = (context == null || context.isBlank()) ? "" : " (" + context + ")";
        Component line = Component.literal("[soul] [分魂] " + name + " : " + content + ctx);
        owner.sendSystemMessage(line);
    }

    /** Convenience: send a "fleeing" cry for help with basic cooldown. */
    public static void sendFleeing(SoulPlayer soul) {
        long now = soul.serverLevel().getGameTime();
        UUID id = soul.getUUID();
        long last = LAST_FLEE_SENT.getOrDefault(id, Long.MIN_VALUE);
        if (now - last < DEFAULT_COOLDOWN_TICKS) {
            return; // suppress spam
        }
        LAST_FLEE_SENT.put(id, now);
        sendToOwner(soul, "老大，救命", "triggered when fleeing");
    }
}

