package net.tigereye.chestcavity.compat.guzhenren.domain.network;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 领域移除包（服务端 -> 客户端）
 *
 * <p>通用的领域渲染移除包，支持所有领域类型
 */
public record DomainRemovePayload(UUID domainId) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<DomainRemovePayload> TYPE =
      new CustomPacketPayload.Type<>(
          ResourceLocation.fromNamespaceAndPath("guzhenren", "domain_remove"));

  public static final StreamCodec<FriendlyByteBuf, DomainRemovePayload> STREAM_CODEC =
      StreamCodec.of(DomainRemovePayload::write, DomainRemovePayload::read);

  private static void write(FriendlyByteBuf buf, DomainRemovePayload payload) {
    buf.writeUUID(payload.domainId);
  }

  private static DomainRemovePayload read(FriendlyByteBuf buf) {
    return new DomainRemovePayload(buf.readUUID());
  }

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  /**
   * 客户端处理
   *
   * @param context 上下文
   */
  public void handle(IPayloadContext context) {
    context.enqueueWork(
        () -> {
          handleClient();
        });
  }

  /** 客户端处理逻辑 */
  private void handleClient() {
    try {
      Class<?> clazz =
          Class.forName(
              "net.tigereye.chestcavity.compat.guzhenren.domain.client.DomainRenderer");
      clazz.getMethod("removeDomain", UUID.class).invoke(null, domainId);
    } catch (Exception e) {
      // 服务端或客户端类未加载时忽略
    }
  }
}
