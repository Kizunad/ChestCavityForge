package net.tigereye.chestcavity.soul.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.service.SoulIdentityViews;

/**
 * Utility for a soul to proactively send formatted messages to its owner. Format: "[soul] [分魂]
 * {name} : {content} (context)"
 */
public final class SoulMessenger {
  private SoulMessenger() {}

  private static final boolean MSG_ENABLED = getBoolProp("chestcavity.soul.msgEnabled", true);
  private static final boolean FLEE_MSG_ENABLED =
      getBoolProp("chestcavity.soul.fleeMsgEnabled", true);
  private static final long DEFAULT_COOLDOWN_TICKS =
      getLongProp("chestcavity.soul.msgCooldownTicks", 100L, 0L, 20L * 60L);
  private static final Map<UUID, Long> LAST_FLEE_SENT = new ConcurrentHashMap<>();

  public static void sendToOwner(SoulPlayer soul, String content, String context) {
    if (!MSG_ENABLED) {
      return;
    }
    var ownerIdOpt = soul.getOwnerId();
    if (ownerIdOpt.isEmpty()) {
      return;
    }
    ServerPlayer owner = soul.serverLevel().getServer().getPlayerList().getPlayer(ownerIdOpt.get());
    if (owner == null) {
      return; // owner offline
    }
    String name = SoulIdentityViews.resolveDisplayName(owner, soul.getSoulId());
    String ctx = (context == null || context.isBlank()) ? "" : " (" + context + ")";
    Component line = Component.literal("[soul] [分魂] " + name + " : " + content + ctx);
    owner.sendSystemMessage(line);
  }

  /** Convenience: send a "fleeing" cry for help with basic cooldown. */
  public static void sendFleeing(SoulPlayer soul) {
    if (!MSG_ENABLED || !FLEE_MSG_ENABLED) {
      return;
    }
    long now = soul.serverLevel().getGameTime();
    UUID id = soul.getUUID();
    long last = LAST_FLEE_SENT.getOrDefault(id, Long.MIN_VALUE);
    if (now - last < DEFAULT_COOLDOWN_TICKS) {
      return; // suppress spam
    }
    LAST_FLEE_SENT.put(id, now);
    sendToOwner(soul, "老大，救命", "triggered when fleeing");
  }

  private static boolean getBoolProp(String key, boolean def) {
    String v = System.getProperty(key);
    if (v == null) {
      return def;
    }
    return v.equalsIgnoreCase("true") || v.equalsIgnoreCase("1") || v.equalsIgnoreCase("yes");
  }

  private static long getLongProp(String key, long def, long lo, long hi) {
    try {
      String v = System.getProperty(key);
      if (v == null) {
        return def;
      }
      long x = Long.parseLong(v);
      if (x < lo) {
        return lo;
      }
      if (x > hi) {
        return hi;
      }
      return x;
    } catch (Throwable ignored) {
      return def;
    }
  }
}
