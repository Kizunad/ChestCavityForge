package net.tigereye.chestcavity.soulbeast.state;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.tigereye.chestcavity.ChestCavity;

/**
 * @deprecated 迁移至 {@link
 *     net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastRequestSyncPayload}
 */
@Deprecated(forRemoval = true)
public record SoulBeastRequestSyncPayload() implements CustomPacketPayload {

  public static final Type<SoulBeastRequestSyncPayload> TYPE =
      new Type<>(ChestCavity.id("soul_beast_request_sync"));

  public static final StreamCodec<RegistryFriendlyByteBuf, SoulBeastRequestSyncPayload>
      STREAM_CODEC =
          StreamCodec.of(SoulBeastRequestSyncPayload::encode, SoulBeastRequestSyncPayload::decode);

  private static void encode(RegistryFriendlyByteBuf buf, SoulBeastRequestSyncPayload payload) {
    // no fields
  }

  private static SoulBeastRequestSyncPayload decode(RegistryFriendlyByteBuf buf) {
    return new SoulBeastRequestSyncPayload();
  }

  public net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state
          .SoulBeastRequestSyncPayload
      toCompat() {
    return new net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state
        .SoulBeastRequestSyncPayload();
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
