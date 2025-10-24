package net.tigereye.chestcavity.network.packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.registration.CCAttachments;

/**
 * Synchronises a single chest cavity organ slot to the client. The payload carries the target slot
 * index and the full {@link ItemStack} state so callers can update arbitrary CustomData or other
 * components without specialising the transport for individual organs. Additional organs can reuse
 * this payload for their own state needs.
 */
public record ChestCavityOrganSlotUpdatePayload(int slot, ItemStack stack)
    implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<ChestCavityOrganSlotUpdatePayload> TYPE =
      new CustomPacketPayload.Type<>(ChestCavity.id("organ_slot_update"));

  public static final StreamCodec<RegistryFriendlyByteBuf, ChestCavityOrganSlotUpdatePayload>
      STREAM_CODEC =
          StreamCodec.of(
              (buf, payload) -> {
                buf.writeVarInt(payload.slot);
                ItemStack.STREAM_CODEC.encode(buf, payload.stack);
              },
              buf -> {
                int slot = buf.readVarInt();
                ItemStack stack = ItemStack.STREAM_CODEC.decode(buf);
                return new ChestCavityOrganSlotUpdatePayload(slot, stack);
              });

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(ChestCavityOrganSlotUpdatePayload payload, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          Player player = context.player();
          if (player == null) {
            return;
          }
          ChestCavityInstance cc = CCAttachments.getChestCavity(player);
          if (payload.slot < 0 || payload.slot >= cc.inventory.getContainerSize()) {
            return;
          }
          ItemStack incoming = payload.stack.copy();
          cc.inventory.setItem(payload.slot, incoming);
        });
  }
}
