package net.tigereye.chestcavity.soulbeast.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.soulbeast.state.SoulBeastStateManager;

import javax.annotation.Nullable;

/**
 * Public API for manipulating the Soul Beast state without relying on organ listeners.
 *
 * Usage:
 * - Call {@link #toSoulBeast(LivingEntity, boolean, ResourceLocation)} to turn an entity into a soul beast.
 * - Call {@link #clearSoulBeast(LivingEntity)} to clear the state (no-op if permanent).
 * - Call {@link #isSoulBeast(LivingEntity)} to check current state.
 */
public final class SoulBeastAPI {

    private SoulBeastAPI() {}

    /**
     * Transforms the entity into a Soul Beast and synchronises the snapshot if server-side.
     *
     * @param entity     target entity
     * @param permanent  whether the state is permanent (cannot be cleared via {@link #clearSoulBeast})
     * @param source     optional source id (e.g., item or effect id); may be null
     * @return true if state toggled, false if entity was null or already active with same flags
     */
    public static boolean toSoulBeast(LivingEntity entity, boolean permanent, @Nullable ResourceLocation source) {
        if (entity == null) {
            return false;
        }
        boolean changed = false;
        if (!SoulBeastStateManager.isActive(entity)) {
            SoulBeastStateManager.setActive(entity, true);
            changed = true;
        }
        if (SoulBeastStateManager.isPermanent(entity) != permanent) {
            SoulBeastStateManager.setPermanent(entity, permanent);
            changed = true;
        }
        SoulBeastStateManager.setSource(entity, source);
        // If called on the logical server, ensure immediate sync
        if (entity instanceof ServerPlayer sp) {
            SoulBeastStateManager.syncToClient(sp);
        }
        return changed;
    }

    /**
     * Clears the Soul Beast state if it is not permanent.
     * @return true if cleared; false if entity is null, client-only, or state is permanent
     */
    public static boolean clearSoulBeast(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        if (SoulBeastStateManager.isPermanent(entity)) {
            return false;
        }
        if (!SoulBeastStateManager.isActive(entity)) {
            return false;
        }
        SoulBeastStateManager.setActive(entity, false);
        if (entity instanceof ServerPlayer sp) {
            SoulBeastStateManager.syncToClient(sp);
        }
        return true;
    }

    /**
     * Returns whether the entity is currently a Soul Beast.
     */
    public static boolean isSoulBeast(LivingEntity entity) {
        return entity != null && SoulBeastStateManager.isActive(entity);
    }
}
