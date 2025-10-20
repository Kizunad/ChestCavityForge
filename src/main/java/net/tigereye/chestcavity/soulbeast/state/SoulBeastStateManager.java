package net.tigereye.chestcavity.soulbeast.state;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * @deprecated 迁移至 {@link net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager}
 */
@Deprecated(forRemoval = true)
public final class SoulBeastStateManager {

    private SoulBeastStateManager() {}

    public static SoulBeastState getOrCreate(LivingEntity entity) {
        return wrap(net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager.getOrCreate(entity));
    }

    public static Optional<SoulBeastState> getExisting(LivingEntity entity) {
        return net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager
                .getExisting(entity)
                .map(SoulBeastState::new);
    }

    public static boolean isActive(LivingEntity entity) {
        return net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager.isActive(entity);
    }

    public static boolean isPermanent(LivingEntity entity) {
        return net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager.isPermanent(entity);
    }

    public static void setActive(LivingEntity entity, boolean active) {
        net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager.setActive(entity, active);
    }

    public static void setPermanent(LivingEntity entity, boolean permanent) {
        net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager.setPermanent(entity, permanent);
    }

    public static boolean isEnabled(LivingEntity entity) {
        return net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager.isEnabled(entity);
    }

    public static void setEnabled(LivingEntity entity, boolean enabled) {
        net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager.setEnabled(entity, enabled);
    }

    public static void setSource(LivingEntity entity, @Nullable net.minecraft.resources.ResourceLocation source) {
        net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager.setSource(entity, source);
    }

    public static void syncToClient(ServerPlayer player) {
        net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager.syncToClient(player);
    }

    public static void handleSyncPayload(SoulBeastSyncPayload payload, IPayloadContext context) {
        net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager.handleSyncPayload(payload.toCompat(), context);
    }

    public static Optional<ClientSnapshot> getClientSnapshot(UUID uuid) {
        return net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager.getClientSnapshot(uuid)
                .map(ClientSnapshot::new);
    }

    public static Optional<ClientSnapshot> getClientSnapshot(Entity entity) {
        return net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager.getClientSnapshot(entity)
                .map(ClientSnapshot::new);
    }

    public static void applyClientSnapshot(SoulBeastSyncPayload payload, @Nullable Player contextPlayer) {
        net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager.applyClientSnapshot(payload.toCompat(), contextPlayer);
    }

    public static void clearClientCache() {
        net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager.clearClientCache();
    }

    /** Handle a serverbound request to (re)sync the player's SoulBeastState to the client. */
    public static void handleRequestSyncPayload(SoulBeastRequestSyncPayload payload, IPayloadContext context) {
        net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager.handleRequestSyncPayload(payload.toCompat(), context);
    }

    private static SoulBeastState wrap(net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastState state) {
        return new SoulBeastState(state);
    }

    public record ClientSnapshot(boolean active, boolean enabled, boolean permanent, long lastTick,
                                 @Nullable net.minecraft.resources.ResourceLocation source) {

        private ClientSnapshot(net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager.ClientSnapshot delegate) {
            this(delegate.active(), delegate.enabled(), delegate.permanent(), delegate.lastTick(), delegate.source());
        }

        net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager.ClientSnapshot toCompat() {
            return new net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager.ClientSnapshot(active, enabled, permanent, lastTick, source);
        }

        public net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.event.SoulBeastStateChangedEvent.Snapshot toEventSnapshot() {
            return toCompat().toEventSnapshot();
        }
    }
}
