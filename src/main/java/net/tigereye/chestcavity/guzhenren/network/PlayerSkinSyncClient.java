package net.tigereye.chestcavity.guzhenren.network;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.util.Iterator;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guzhenren.network.packets.PlayerSkinUploadPayload;

/**
 * Client-side helper that uploads the local player's Mojang skin property to the server once per
 * connection. This keeps integrated servers in sync with the true skin data so server-side clones
 * and tints render correctly.
 */
@OnlyIn(Dist.CLIENT)
public final class PlayerSkinSyncClient {

  private static boolean sentForConnection = false;

  private PlayerSkinSyncClient() {}

  public static void onClientTick(ClientTickEvent.Post event) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft == null) {
      return;
    }
    if (minecraft.player == null || minecraft.getConnection() == null) {
      sentForConnection = false;
      return;
    }
    if (sentForConnection) {
      return;
    }

    Property textures = resolveTextures(minecraft);
    if (textures == null) {
      return;
    }
    String value = textures.value();
    if (value == null || value.isBlank()) {
      return;
    }
    var connection = minecraft.getConnection();
    if (connection == null) {
      return;
    }
    connection.send(new PlayerSkinUploadPayload(value, textures.signature()));
    sentForConnection = true;
    ChestCavity.LOGGER.debug(
        "[compat/guzhenren][skin] Uploaded client skin payload (signed={})",
        textures.signature() != null && !textures.signature().isBlank());
  }

  private static Property resolveTextures(Minecraft minecraft) {
    if (minecraft.player != null) {
      return extractTextures(minecraft.player.getGameProfile());
    }
    return null;
  }

  private static Property extractTextures(GameProfile profile) {
    if (profile == null) {
      return null;
    }
    PropertyMap properties = profile.getProperties();
    if (properties == null || properties.isEmpty()) {
      return null;
    }
    Iterator<Property> iterator = properties.get("textures").iterator();
    return iterator.hasNext() ? iterator.next() : null;
  }
}
