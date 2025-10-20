package net.tigereye.chestcavity.compat.guzhenren.client.skin;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.tigereye.chestcavity.compat.guzhenren.item.guang_dao.entity.XiaoGuangIllusionEntity;

import java.util.UUID;

/**
 * Immutable description of a skin request, including owner identity and optional cached fallbacks.
 */
@OnlyIn(Dist.CLIENT)
public record SkinHandle(
        UUID ownerId,
        String propertyValue,
        String propertySignature,
        String model,
        String skinUrl,
        ResourceLocation fallback,
        ResourceLocation overlayFallback
) {
    public static SkinHandle from(XiaoGuangIllusionEntity entity) {
        UUID owner = entity.getOwnerUuid().orElse(entity.getUUID());
        return new SkinHandle(
                owner,
                sanitize(entity.getSkinPropertyValue()),
                sanitize(entity.getSkinPropertySignature()),
                entity.getSkinModel(),
                sanitize(entity.getSkinUrl()),
                entity.getSkinTexture(),
                null
        );
    }

    public String cacheKey() {
        return ownerId + "|" + propertyValue + "|" + propertySignature + "|" + model + "|" + skinUrl;
    }

    private static String sanitize(String value) {
        return value == null ? "" : value;
    }
}
