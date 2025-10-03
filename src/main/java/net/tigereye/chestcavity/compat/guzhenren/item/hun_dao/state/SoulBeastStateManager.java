package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.state;

import com.mojang.logging.LogUtils;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.DistExecutor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.registration.CCAttachments;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central access point for the {@link SoulBeastState} attachment.
 */
public final class SoulBeastStateManager {

    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, ClientSnapshot> CLIENT_CACHE = new ConcurrentHashMap<>();

    private SoulBeastStateManager() {
    }

    public static SoulBeastState getOrCreate(LivingEntity entity) {
        return CCAttachments.getSoulBeastState(entity);
    }

    public static Optional<SoulBeastState> getExisting(LivingEntity entity) {
        return CCAttachments.getExistingSoulBeastState(entity);
    }

    public static boolean isActive(LivingEntity entity) {
        return getExisting(entity).map(SoulBeastState::isActive).orElse(false);
    }

    public static boolean isPermanent(LivingEntity entity) {
        return getExisting(entity).map(SoulBeastState::isPermanent).orElse(false);
    }

    public static void setActive(LivingEntity entity, boolean active) {
        SoulBeastState state = getOrCreate(entity);
        if (state.setActive(active)) {
            touch(entity, state);
            if (entity instanceof ServerPlayer player) {
                syncToClient(player);
            }
        }
    }

    public static void setPermanent(LivingEntity entity, boolean permanent) {
        SoulBeastState state = getOrCreate(entity);
        if (state.setPermanent(permanent)) {
            touch(entity, state);
            if (entity instanceof ServerPlayer player) {
                syncToClient(player);
            }
        }
    }

    public static void setSource(LivingEntity entity, @Nullable net.minecraft.resources.ResourceLocation source) {
        SoulBeastState state = getOrCreate(entity);
        if (state.setSource(source)) {
            touch(entity, state);
            if (entity instanceof ServerPlayer player) {
                syncToClient(player);
            }
        }
    }

    public static void syncToClient(ServerPlayer player) {
        SoulBeastState state = getOrCreate(player);
        var payload = new SoulBeastSyncPayload(player.getId(), state.isActive(), state.isPermanent(), state.getLastTick(),
                state.getSource().orElse(null));
        player.connection.send(payload);
        LOGGER.debug("[compat/guzhenren][hun_dao][state] synced {} -> active={} permanent={} source={} tick={}",
                describe(player), state.isActive(), state.isPermanent(),
                state.getSource().map(Object::toString).orElse("<none>"), state.getLastTick());
    }

    public static void handleSyncPayload(SoulBeastSyncPayload payload, IPayloadContext context) {
        if (context.flow() == PacketFlow.CLIENTBOUND) {
            context.enqueueWork(() -> applyClientSnapshot(payload, context.player()));
            return;
        }
        Player player = context.player();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        context.enqueueWork(() -> {
            SoulBeastState state = getOrCreate(serverPlayer);
            boolean dirty = false;
            dirty |= state.setActive(payload.active());
            dirty |= state.setPermanent(payload.permanent());
            dirty |= state.setSource(payload.source());
            state.setLastTick(payload.lastTick());
            if (dirty) {
                LOGGER.debug("[compat/guzhenren][hun_dao][state] server accepted client payload {} dirty={}", describe(serverPlayer), dirty);
            }
        });
    }

    public static Optional<ClientSnapshot> getClientSnapshot(UUID uuid) {
        return Optional.ofNullable(CLIENT_CACHE.get(uuid));
    }

    public static Optional<ClientSnapshot> getClientSnapshot(Entity entity) {
        return entity == null ? Optional.empty() : getClientSnapshot(entity.getUUID());
    }

    public static void applyClientSnapshot(SoulBeastSyncPayload payload, @Nullable Player contextPlayer) {
        Player resolvedPlayer = contextPlayer;
        if (resolvedPlayer == null) {
            resolvedPlayer = DistExecutor.unsafeCallForDist(() -> SoulBeastStateManager::resolveClientPlayer,
                    () -> () -> null);
        }
        if (resolvedPlayer == null) {
            return;
        }
        Entity target = resolvedPlayer.level().getEntity(payload.entityId());
        UUID uuid = target != null ? target.getUUID() : resolvedPlayer.getUUID();
        ClientSnapshot snapshot = new ClientSnapshot(payload.active(), payload.permanent(), payload.lastTick(), payload.source());
        CLIENT_CACHE.put(uuid, snapshot);
        if (ChestCavity.LOGGER.isDebugEnabled()) {
            ChestCavity.LOGGER.debug("[compat/guzhenren][hun_dao][state] client cached {} -> {}", uuid, snapshot);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static Player resolveClientPlayer() {
        return net.minecraft.client.Minecraft.getInstance().player;
    }

    public static void clearClientCache() {
        CLIENT_CACHE.clear();
    }

    private static void touch(LivingEntity entity, SoulBeastState state) {
        long tick = entity.level() != null ? entity.level().getGameTime() : 0L;
        state.setLastTick(tick);
    }

    private static String describe(Player player) {
        return String.format(Locale.ROOT, "%s(%s)", player.getScoreboardName(), player.getUUID());
    }

    public record ClientSnapshot(boolean active, boolean permanent, long lastTick,
                                 @Nullable net.minecraft.resources.ResourceLocation source) {
    }
}
