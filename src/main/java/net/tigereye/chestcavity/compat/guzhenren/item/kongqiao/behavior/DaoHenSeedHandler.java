package net.tigereye.chestcavity.compat.guzhenren.item.kongqiao.behavior;

import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guzhenren.network.packets.KongqiaoDaoHenSeedPayload;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;

import java.util.Locale;
import java.util.Map;

/** Server-side handler for DaoHen seed payloads. */
public final class DaoHenSeedHandler {
    private static final double EPSILON = 1.0e-4;

    private DaoHenSeedHandler() {}

    public static void handleSeedPayload(KongqiaoDaoHenSeedPayload payload, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return;
        }
        Player player = context.player();
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        Map<ResourceLocation, Double> seeds = payload.seeds();
        if (seeds == null || seeds.isEmpty()) {
            return;
        }
        context.enqueueWork(() -> applyChannelSeeds(player, seeds));
    }

    public static void applyChannelSeeds(Player player, Map<ResourceLocation, Double> seeds) {
        if (player == null || seeds == null || seeds.isEmpty()) {
            return;
        }
        ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).ifPresent(cc -> {
            var context = LinkageManager.getContext(cc);
            seeds.forEach((channelId, value) -> {
                if (channelId == null) {
                    return;
                }
                LinkageChannel channel = context.getOrCreateChannel(channelId);
                double previous = channel.get();
                if (Math.abs(previous - value) <= EPSILON) {
                    return;
                }
                channel.set(value);
                ChestCavity.LOGGER.debug("[compat/guzhenren][kongqiao] seeded {} with {} (Δ {})",
                        channelId, format(value), format(value - previous));
            });
        });
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
