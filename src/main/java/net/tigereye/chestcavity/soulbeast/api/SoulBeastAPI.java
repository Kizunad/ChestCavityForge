package net.tigereye.chestcavity.soulbeast.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.soulbeast.damage.SoulBeastDamageListener;

import javax.annotation.Nullable;

/**
 * @deprecated 迁移至 {@link net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.api.SoulBeastAPI}
 */
@Deprecated(forRemoval = true)
public final class SoulBeastAPI {

    private SoulBeastAPI() {}

    public static boolean toSoulBeast(LivingEntity entity, boolean permanent, @Nullable ResourceLocation source) {
        return net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.api.SoulBeastAPI.toSoulBeast(entity, permanent, source);
    }

    public static boolean clearSoulBeast(LivingEntity entity) {
        return net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.api.SoulBeastAPI.clearSoulBeast(entity);
    }

    public static boolean isSoulBeast(LivingEntity entity) {
        return net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.api.SoulBeastAPI.isSoulBeast(entity);
    }

    public static void registerDamageListener(SoulBeastDamageListener listener) {
        net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.api.SoulBeastAPI.registerDamageListener(listener);
    }

    public static void unregisterDamageListener(SoulBeastDamageListener listener) {
        net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.api.SoulBeastAPI.unregisterDamageListener(listener);
    }
}
