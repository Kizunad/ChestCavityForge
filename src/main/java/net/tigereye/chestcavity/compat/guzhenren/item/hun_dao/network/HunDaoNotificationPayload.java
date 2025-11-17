package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.ui.HunDaoNotificationRenderer.NotificationCategory;

/**
 * Payload for sending notification messages from server to client.
 *
 * <p>Displays toast-style notifications for Hun Dao events.
 *
 * <p>Phase 6: Network synchronization for HUD/FX.
 */
public record HunDaoNotificationPayload(Component message, NotificationCategory category)
    implements CustomPacketPayload {

  public static final Type<HunDaoNotificationPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath("guzhenren", "hun_dao/notification"));

  public static final StreamCodec<FriendlyByteBuf, HunDaoNotificationPayload> STREAM_CODEC =
      StreamCodec.of(HunDaoNotificationPayload::write, HunDaoNotificationPayload::read);

  private static void write(FriendlyByteBuf buf, HunDaoNotificationPayload payload) {
    // Serialize Component as plain text string (sufficient for notifications)
    buf.writeUtf(payload.message.getString());
    buf.writeVarInt(payload.category.ordinal());
  }

  private static HunDaoNotificationPayload read(FriendlyByteBuf buf) {
    // Deserialize Component from plain text string
    String messageText = buf.readUtf();
    Component message = Component.literal(messageText);
    int categoryOrdinal = buf.readVarInt();
    NotificationCategory category =
        NotificationCategory.values()[
            Math.max(0, Math.min(categoryOrdinal, NotificationCategory.values().length - 1))];
    return new HunDaoNotificationPayload(message, category);
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
