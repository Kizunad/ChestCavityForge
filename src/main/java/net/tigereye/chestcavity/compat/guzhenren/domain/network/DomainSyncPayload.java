package net.tigereye.chestcavity.compat.guzhenren.domain.network;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 领域同步包（服务端 -> 客户端）
 *
 * <p>通用的领域PNG渲染同步包，支持所有领域类型
 */
public record DomainSyncPayload(
    UUID domainId,
    UUID ownerUuid,
    double centerX,
    double centerY,
    double centerZ,
    double radius,
    int level,
    String texturePath, // 纹理路径字符串
    double heightOffset,
    float alpha,
    float rotationSpeed)
    implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<DomainSyncPayload> TYPE =
      new CustomPacketPayload.Type<>(
          ResourceLocation.fromNamespaceAndPath("guzhenren", "domain_sync"));

  public static final StreamCodec<FriendlyByteBuf, DomainSyncPayload> STREAM_CODEC =
      StreamCodec.of(DomainSyncPayload::write, DomainSyncPayload::read);

  private static void write(FriendlyByteBuf buf, DomainSyncPayload payload) {
    buf.writeUUID(payload.domainId);
    buf.writeUUID(payload.ownerUuid);
    buf.writeDouble(payload.centerX);
    buf.writeDouble(payload.centerY);
    buf.writeDouble(payload.centerZ);
    buf.writeDouble(payload.radius);
    buf.writeInt(payload.level);
    buf.writeUtf(payload.texturePath);
    buf.writeDouble(payload.heightOffset);
    buf.writeFloat(payload.alpha);
    buf.writeFloat(payload.rotationSpeed);
  }

  private static DomainSyncPayload read(FriendlyByteBuf buf) {
    return new DomainSyncPayload(
        buf.readUUID(),
        buf.readUUID(),
        buf.readDouble(),
        buf.readDouble(),
        buf.readDouble(),
        buf.readDouble(),
        buf.readInt(),
        buf.readUtf(),
        buf.readDouble(),
        buf.readFloat(),
        buf.readFloat());
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
      ResourceLocation texture = ResourceLocation.parse(texturePath);
      Class<?> clazz =
          Class.forName(
              "net.tigereye.chestcavity.compat.guzhenren.domain.client.DomainRenderer");
      clazz
          .getMethod(
              "registerDomain",
              UUID.class,
              UUID.class,
              double.class,
              double.class,
              double.class,
              double.class,
              int.class,
              ResourceLocation.class,
              double.class,
              float.class,
              float.class)
          .invoke(
              null,
              domainId,
              ownerUuid,
              centerX,
              centerY,
              centerZ,
              radius,
              level,
              texture,
              heightOffset,
              alpha,
              rotationSpeed);
    } catch (Exception e) {
      // 服务端或客户端类未加载时忽略
    }
  }
}
