package net.tigereye.chestcavity.compat.guzhenren.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.jin_dao.client.TiePiClientState;

/**
 * 服务器 → 客户端：同步铁皮蛊阶段、SP、冷却与联动状态。客户端持久化该数据供 ModernUI/HUD 使用。
 */
public record TiePiProgressPayload(
    int entityId,
    int phase,
    double storedSp,
    double windowGain,
    double windowCap,
    double nextThreshold,
    long serverTime,
    boolean hungerLocked,
    boolean hardeningActive,
    boolean ironwallActive,
    boolean heavyReady,
    boolean slamUnlocked,
    long hardeningReadyTick,
    long ironwallReadyTick,
    long heavyReadyTick,
    long slamReadyTick,
    long hardeningEndTick,
    long ironwallEndTick,
    long heavyExpireTick,
    boolean copperSynergy,
    boolean boneSynergy,
    boolean bellSynergy,
    boolean swordSynergy,
    boolean leiSynergy)
    implements CustomPacketPayload {

  public static final Type<TiePiProgressPayload> TYPE =
      new Type<>(ChestCavity.id("tiepi_progress"));

  public static final StreamCodec<FriendlyByteBuf, TiePiProgressPayload> STREAM_CODEC =
      StreamCodec.of(TiePiProgressPayload::encode, TiePiProgressPayload::decode);

  private static void encode(FriendlyByteBuf buf, TiePiProgressPayload payload) {
    buf.writeVarInt(payload.entityId);
    buf.writeVarInt(payload.phase);
    buf.writeDouble(payload.storedSp);
    buf.writeDouble(payload.windowGain);
    buf.writeDouble(payload.windowCap);
    buf.writeDouble(payload.nextThreshold);
    buf.writeVarLong(payload.serverTime);
    buf.writeBoolean(payload.hungerLocked);
    buf.writeBoolean(payload.hardeningActive);
    buf.writeBoolean(payload.ironwallActive);
    buf.writeBoolean(payload.heavyReady);
    buf.writeBoolean(payload.slamUnlocked);
    buf.writeVarLong(payload.hardeningReadyTick);
    buf.writeVarLong(payload.ironwallReadyTick);
    buf.writeVarLong(payload.heavyReadyTick);
    buf.writeVarLong(payload.slamReadyTick);
    buf.writeVarLong(payload.hardeningEndTick);
    buf.writeVarLong(payload.ironwallEndTick);
    buf.writeVarLong(payload.heavyExpireTick);
    buf.writeBoolean(payload.copperSynergy);
    buf.writeBoolean(payload.boneSynergy);
    buf.writeBoolean(payload.bellSynergy);
    buf.writeBoolean(payload.swordSynergy);
    buf.writeBoolean(payload.leiSynergy);
  }

  private static TiePiProgressPayload decode(FriendlyByteBuf buf) {
    int entityId = buf.readVarInt();
    int phase = buf.readVarInt();
    double storedSp = buf.readDouble();
    double windowGain = buf.readDouble();
    double windowCap = buf.readDouble();
    double nextThreshold = buf.readDouble();
    long serverTime = buf.readVarLong();
    boolean hungerLocked = buf.readBoolean();
    boolean hardeningActive = buf.readBoolean();
    boolean ironwallActive = buf.readBoolean();
    boolean heavyReady = buf.readBoolean();
    boolean slamUnlocked = buf.readBoolean();
    long hardeningReady = buf.readVarLong();
    long ironwallReady = buf.readVarLong();
    long heavyReadyTick = buf.readVarLong();
    long slamReady = buf.readVarLong();
    long hardeningEnd = buf.readVarLong();
    long ironwallEnd = buf.readVarLong();
    long heavyExpire = buf.readVarLong();
    boolean copper = buf.readBoolean();
    boolean bone = buf.readBoolean();
    boolean bell = buf.readBoolean();
    boolean sword = buf.readBoolean();
    boolean lei = buf.readBoolean();
    return new TiePiProgressPayload(
        entityId,
        phase,
        storedSp,
        windowGain,
        windowCap,
        nextThreshold,
        serverTime,
        hungerLocked,
        hardeningActive,
        ironwallActive,
        heavyReady,
        slamUnlocked,
        hardeningReady,
        ironwallReady,
        heavyReadyTick,
        slamReady,
        hardeningEnd,
        ironwallEnd,
        heavyExpire,
        copper,
        bone,
        bell,
        sword,
        lei);
  }

  @Override
  public Type<TiePiProgressPayload> type() {
    return TYPE;
  }

  public static void handle(TiePiProgressPayload payload, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (!FMLEnvironment.dist.isClient()) {
            return;
          }
          TiePiClientState.mirror(payload);
        });
  }
}
