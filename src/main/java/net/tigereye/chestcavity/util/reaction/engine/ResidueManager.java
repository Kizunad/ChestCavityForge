package net.tigereye.chestcavity.util.reaction.engine;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 残留体（AEC）合并/刷新管理，避免重复生成。
 */
public final class ResidueManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ResidueManager() {}

    private static final Map<String, UUID> FROST_KEYS = new HashMap<>();
    private static final Map<String, UUID> CORROSION_KEYS = new HashMap<>();
    private static final Map<String, UUID> BLOOD_KEYS = new HashMap<>();

    private static String key(ServerLevel level, double x, double y, double z) {
        int gx = (int) Math.floor(x + 0.5);
        int gy = (int) Math.floor(y);
        int gz = (int) Math.floor(z + 0.5);
        return level.dimension().location().toString() + ":" + gx + ":" + gy + ":" + gz;
    }

    public static void spawnOrRefreshFrost(ServerLevel level, double x, double y, double z,
                                           float radius, int durationTicks, int slowAmplifier) {
        String k = key(level, x, y, z);
        UUID id = FROST_KEYS.get(k);
        AreaEffectCloud aec = id != null ? find(level, id) : null;
        if (aec == null) {
            aec = new AreaEffectCloud(level, x, y, z);
            aec.setRadius(Math.max(0.5F, radius));
            aec.setDuration(Math.max(20, durationTicks));
            aec.setWaitTime(0);
            aec.setRadiusPerTick(0.0F);
            aec.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, Math.max(0, slowAmplifier), false, true));
            level.addFreshEntity(aec);
            FROST_KEYS.put(k, aec.getUUID());
        } else {
            aec.setRadius(Math.max(aec.getRadius(), radius));
            aec.setDuration(Math.max(aec.getDuration(), durationTicks));
        }
    }

    public static void spawnOrRefreshCorrosion(ServerLevel level, double x, double y, double z,
                                               float radius, int durationTicks) {
        String k = key(level, x, y, z);
        UUID id = CORROSION_KEYS.get(k);
        AreaEffectCloud aec = id != null ? find(level, id) : null;
        if (aec == null) {
            aec = new AreaEffectCloud(level, x, y, z);
            aec.setRadius(Math.max(0.5F, radius));
            aec.setDuration(Math.max(20, durationTicks));
            aec.setWaitTime(0);
            aec.setRadiusPerTick(0.0F);
            aec.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, true));
            level.addFreshEntity(aec);
            CORROSION_KEYS.put(k, aec.getUUID());
        } else {
            aec.setRadius(Math.max(aec.getRadius(), radius));
            aec.setDuration(Math.max(aec.getDuration(), durationTicks));
        }
    }

    public static void spawnOrRefreshBlood(ServerLevel level, double x, double y, double z,
                                           float radius, int durationTicks) {
        String k = key(level, x, y, z);
        UUID id = BLOOD_KEYS.get(k);
        AreaEffectCloud aec = id != null ? find(level, id) : null;
        if (aec == null) {
            aec = new AreaEffectCloud(level, x, y, z);
            aec.setRadius(Math.max(0.5F, radius));
            aec.setDuration(Math.max(20, durationTicks));
            aec.setWaitTime(0);
            aec.setRadiusPerTick(0.0F);
            // 轻度虚弱 + 短暂缓慢，营造“血雾”压制感
            aec.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true));
            aec.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true));
            level.addFreshEntity(aec);
            BLOOD_KEYS.put(k, aec.getUUID());
        } else {
            aec.setRadius(Math.max(aec.getRadius(), radius));
            aec.setDuration(Math.max(aec.getDuration(), durationTicks));
        }
    }

    private static AreaEffectCloud find(ServerLevel level, UUID id) {
        var e = level.getEntity(id);
        return e instanceof AreaEffectCloud a ? a : null;
    }
}
