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
    private static final Map<String, UUID> FIRE_KEYS = new HashMap<>();
    private static final Map<String, Boolean> FIRE_FINALIZED = new HashMap<>();

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

    public static void spawnOrRefreshFire(ServerLevel level, double x, double y, double z,
                                          float radius, int durationTicks) {
        String k = key(level, x, y, z);
        UUID id = FIRE_KEYS.get(k);
        AreaEffectCloud aec = id != null ? find(level, id) : null;
        if (aec == null) {
            aec = new AreaEffectCloud(level, x, y, z);
            aec.setRadius(Math.max(0.5F, radius));
            aec.setDuration(Math.max(20, durationTicks));
            aec.setWaitTime(0);
            aec.setRadiusPerTick(0.0F);
            // 视觉上用少量烟雾表现，效果由 tickFireResidues 附带
            aec.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1, 0, false, false)); // 占位，不实际影响
            level.addFreshEntity(aec);
            FIRE_KEYS.put(k, aec.getUUID());
            FIRE_FINALIZED.remove(k);
        } else {
            aec.setRadius(Math.max(aec.getRadius(), radius));
            aec.setDuration(Math.max(aec.getDuration(), durationTicks));
        }
    }

    // 每 tick 调用：为处于余烬的实体附加“燃痕/点燃窗口”，并处理“回光火星”和与血雾重合的汽化
    public static void tickFireResidues(net.minecraft.server.MinecraftServer server) {
        if (server == null) return;
        // 遍历快照，避免并发修改
        var entries = new java.util.ArrayList<>(FIRE_KEYS.entrySet());
        for (var e : entries) {
            UUID id = e.getValue();
            AreaEffectCloud aec = null;
            ServerLevel foundLevel = null;
            for (ServerLevel lvl : server.getAllLevels()) {
                var ent = lvl.getEntity(id);
                if (ent instanceof AreaEffectCloud cloud) { aec = cloud; foundLevel = lvl; break; }
            }
            if (aec == null || foundLevel == null) {
                FIRE_KEYS.remove(e.getKey());
                FIRE_FINALIZED.remove(e.getKey());
                continue;
            }
            // 每 10t 为范围内单位附加燃痕与点燃窗口
            int interval = Math.max(1, net.tigereye.chestcavity.ChestCavity.config.REACTION.HUO_TAN.emberTickIntervalTicks);
            if (server.getTickCount() % interval == 0) {
                float r = Math.max(0.5F, aec.getRadius());
                var box = new net.minecraft.world.phys.AABB(aec.getX()-r, aec.getY()-r, aec.getZ()-r, aec.getX()+r, aec.getY()+r, aec.getZ()+r);
                var list = foundLevel.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box, le -> le != null && le.isAlive());
                int tierBoost = detectNearbyHuoTanTier(foundLevel, aec.getX(), aec.getY(), aec.getZ(), r) >= 3 ? 1 : 0;
                int applied = 0;
                int max = Math.max(1, net.tigereye.chestcavity.ChestCavity.config.REACTION.HUO_TAN.emberMaxEntitiesPerTick);
                for (var le : list) {
                    if (applied >= max) break;
                    net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.addStacked(le,
                            net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys.FLAME_MARK,
                            Math.max(1, net.tigereye.chestcavity.ChestCavity.config.REACTION.HUO_TAN.emberMarkPerTick + tierBoost),
                            Math.max(1, net.tigereye.chestcavity.ChestCavity.config.REACTION.HUO_TAN.flameMarkDurationTicks));
                    net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.add(le,
                            net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys.IGNITE_WINDOW,
                            Math.max(1, net.tigereye.chestcavity.ChestCavity.config.REACTION.HUO_TAN.igniteWindowTicks));
                    int igniteSec = Math.max(0, net.tigereye.chestcavity.ChestCavity.config.REACTION.HUO_TAN.emberIgniteSeconds);
                    if (igniteSec > 0) {
                        le.igniteForSeconds(igniteSec);
                    }
                    applied++;
                }
            }

            // 临终“回光火星”：仅执行一次
            if (aec.getDuration() <= 20 && !Boolean.TRUE.equals(FIRE_FINALIZED.get(e.getKey()))) {
                float r = Math.max(0.5F, aec.getRadius());
                var box = new net.minecraft.world.phys.AABB(aec.getX()-r, aec.getY()-r, aec.getZ()-r, aec.getX()+r, aec.getY()+r, aec.getZ()+r);
                var list = foundLevel.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box, le -> le != null && le.isAlive());
                for (var le : list) {
                    net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.add(le,
                            net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys.IGNITE_WINDOW,
                            40);
                }
                FIRE_FINALIZED.put(e.getKey(), Boolean.TRUE);
            }

            // 与血雾残留重合：触发一次蒸汽爆并清除两者
            // 简化：若有任何 blood AEC 距离 < 0.5 则触发
            for (var be : new java.util.ArrayList<>(BLOOD_KEYS.entrySet())) {
                AreaEffectCloud blood = null;
                for (ServerLevel lvl : server.getAllLevels()) {
                    var ent = lvl.getEntity(be.getValue());
                    if (ent instanceof AreaEffectCloud cloud) { blood = cloud; break; }
                }
                if (blood == null) { BLOOD_KEYS.remove(be.getKey()); continue; }
                double dx = aec.getX()-blood.getX();
                double dy = aec.getY()-blood.getY();
                double dz = aec.getZ()-blood.getZ();
                double dist2 = dx*dx+dy*dy+dz*dz;
                if (dist2 < 0.25) {
                    net.tigereye.chestcavity.util.reaction.engine.ReactionEngine.queueAoEDamage(foundLevel,
                            aec.getX(), aec.getY(), aec.getZ(), 2.0F, 3.0D, null,
                            net.tigereye.chestcavity.util.reaction.engine.ReactionEngine.VisualTheme.STEAM);
                    aec.discard();
                    blood.discard();
                    FIRE_KEYS.remove(e.getKey());
                    FIRE_FINALIZED.remove(e.getKey());
                    BLOOD_KEYS.remove(be.getKey());
                    break;
                }
            }
        }
    }

    private static int detectNearbyHuoTanTier(ServerLevel level, double x, double y, double z, float radius) {
        try {
            var box = new net.minecraft.world.phys.AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
            var players = level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, box, p -> p != null && p.isAlive());
            int maxTier = 0;
            for (var p : players) {
                var opt = net.tigereye.chestcavity.interfaces.ChestCavityEntity.of(p);
                if (opt.isEmpty()) continue;
                var cc = opt.get().getChestCavityInstance();
                if (cc == null || cc.inventory == null) continue;
                for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
                    var stack = cc.inventory.getItem(i);
                    if (stack.isEmpty()) continue;
                    var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
                    if (id != null && id.equals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("guzhenren", "dan_qiao_huo_tan_gu"))) {
                        var state = net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState.of(stack, "HuoTanGu");
                        int tier = Math.max(1, Math.min(4, state.getInt("Tier", 1)));
                        if (tier > maxTier) maxTier = tier;
                        break;
                    }
                }
            }
            return maxTier;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    // 供行为层查询附近火雾数量（用于链爆判定）
    public static int countFireResiduesNear(ServerLevel level, double x, double y, double z, double radius) {
        int count = 0;
        for (UUID id : FIRE_KEYS.values()) {
            var ent = level.getEntity(id);
            if (!(ent instanceof AreaEffectCloud aec)) continue;
            double dx = aec.getX() - x;
            double dy = aec.getY() - y;
            double dz = aec.getZ() - z;
            if (dx*dx + dy*dy + dz*dz <= radius * radius) count++;
        }
        return count;
    }

    private static AreaEffectCloud find(ServerLevel level, UUID id) {
        var e = level.getEntity(id);
        return e instanceof AreaEffectCloud a ? a : null;
    }
}
