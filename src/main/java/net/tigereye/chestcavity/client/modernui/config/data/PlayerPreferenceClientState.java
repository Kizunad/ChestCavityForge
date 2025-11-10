package net.tigereye.chestcavity.client.modernui.config.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.client.modernui.config.network.PlayerPreferenceRequestPayload;

/** 客户端缓存玩家偏好开关，并通过 ModernUI 请求/更新。 */
public final class PlayerPreferenceClientState {

  private static final Map<ResourceLocation, Boolean> TOGGLES = new ConcurrentHashMap<>();

  private PlayerPreferenceClientState() {}

  public static boolean get(ResourceLocation key, boolean fallback) {
    return TOGGLES.getOrDefault(key, fallback);
  }

  public static void setLocal(ResourceLocation key, boolean value) {
    TOGGLES.put(key, value);
  }

  public static void applyServerPayload(Map<ResourceLocation, Boolean> entries) {
    TOGGLES.putAll(entries);
  }

  public static void requestSync() {
    Minecraft mc = Minecraft.getInstance();
    ClientPacketListener connection = mc.getConnection();
    if (connection == null) {
      return;
    }
    mc.execute(() -> connection.send(new PlayerPreferenceRequestPayload()));
  }
}
