package net.tigereye.chestcavity.engine.reaction;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import org.slf4j.Logger;

/** 残留体（AEC）合并/刷新管理，避免重复生成。 搬迁自 util.reaction.engine.ResidueManager。 */
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

  public static void spawnOrRefreshFrost(
      ServerLevel level,
      double x,
      double y,
      double z,
      float radius,
      int durationTicks,
      int slowAmplifier) {
    String k = key(level, x, y, z);
    UUID id = FROST_KEYS.get(k);
    AreaEffectCloud aec = id != null ? find(level, id) : null;
    if (aec == null) {
      aec = new AreaEffectCloud(level, x, y, z);
      aec.setRadius(Math.max(0.5F, radius));
      aec.setDuration(Math.max(20, durationTicks));
      aec.setWaitTime(0);
      aec.setRadiusPerTick(0.0F);
      aec.addEffect(
          new MobEffectInstance(
              MobEffects.MOVEMENT_SLOWDOWN, 40, Math.max(0, slowAmplifier), false, true));
      level.addFreshEntity(aec);
      FROST_KEYS.put(k, aec.getUUID());
    } else {
      aec.setRadius(Math.max(aec.getRadius(), radius));
      aec.setDuration(Math.max(aec.getDuration(), durationTicks));
    }
  }

  public static void spawnOrRefreshCorrosion(
      ServerLevel level, double x, double y, double z, float radius, int durationTicks) {
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

  public static void spawnOrRefreshBlood(
      ServerLevel level, double x, double y, double z, float radius, int durationTicks) {
    String k = key(level, x, y, z);
    UUID id = BLOOD_KEYS.get(k);
    AreaEffectCloud aec = id != null ? find(level, id) : null;
    if (aec == null) {
      aec = new AreaEffectCloud(level, x, y, z);
      aec.setRadius(Math.max(0.5F, radius));
      aec.setDuration(Math.max(20, durationTicks));
      aec.setWaitTime(0);
      aec.setRadiusPerTick(0.0F);
      aec.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true));
      aec.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true));
      level.addFreshEntity(aec);
      BLOOD_KEYS.put(k, aec.getUUID());
    } else {
      aec.setRadius(Math.max(aec.getRadius(), radius));
      aec.setDuration(Math.max(aec.getDuration(), durationTicks));
    }
  }

  public static void spawnOrRefreshFire(
      ServerLevel level, double x, double y, double z, float radius, int durationTicks) {
    String k = key(level, x, y, z);
    UUID id = FIRE_KEYS.get(k);
    AreaEffectCloud aec = id != null ? find(level, id) : null;
    if (aec == null) {
      aec = new AreaEffectCloud(level, x, y, z);
      aec.setRadius(Math.max(0.5F, radius));
      aec.setDuration(Math.max(20, durationTicks));
      aec.setWaitTime(0);
      aec.setRadiusPerTick(0.0F);
      aec.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1, 0, false, false));
      level.addFreshEntity(aec);
      FIRE_KEYS.put(k, aec.getUUID());
      FIRE_FINALIZED.remove(k);
    } else {
      aec.setRadius(Math.max(aec.getRadius(), radius));
      aec.setDuration(Math.max(aec.getDuration(), durationTicks));
    }
  }

  public static void tickFireResidues(MinecraftServer server) {
    if (server == null) {
      return;
    }
    var entries = new java.util.ArrayList<>(FIRE_KEYS.entrySet());
    for (var e : entries) {
      UUID id = e.getValue();
      AreaEffectCloud aec = null;
      ServerLevel foundLevel = null;
      for (ServerLevel lvl : server.getAllLevels()) {
        var ent = lvl.getEntity(id);
        if (ent instanceof AreaEffectCloud cloud) {
          aec = cloud;
          foundLevel = lvl;
          break;
        }
      }
      if (aec == null || foundLevel == null) {
        FIRE_KEYS.remove(e.getKey());
        FIRE_FINALIZED.remove(e.getKey());
        continue;
      }
      int interval =
          Math.max(
              1,
              net.tigereye.chestcavity.ChestCavity.config.REACTION.HUO_TAN.emberTickIntervalTicks);
      if (server.getTickCount() % interval == 0) {
        float r = Math.max(0.5F, aec.getRadius());
        var box =
            new net.minecraft.world.phys.AABB(
                aec.getX() - r,
                aec.getY() - r,
                aec.getZ() - r,
                aec.getX() + r,
                aec.getY() + r,
                aec.getZ() + r);
        var list =
            foundLevel.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                box,
                le -> le != null && le.isAlive());
        int tierBoost =
            detectNearbyHuoTanTier(foundLevel, aec.getX(), aec.getY(), aec.getZ(), r) >= 3 ? 1 : 0;
        int applied = 0;
        int max =
            Math.max(
                1,
                net.tigereye.chestcavity.ChestCavity.config
                    .REACTION
                    .HUO_TAN
                    .emberMaxEntitiesPerTick);
        for (var le : list) {
          if (applied >= max) {
            break;
          }
          net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.addStacked(
              le,
              net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys.FLAME_MARK,
              Math.max(
                  1,
                  net.tigereye.chestcavity.ChestCavity.config.REACTION.HUO_TAN.emberMarkPerTick
                      + tierBoost),
              Math.max(
                  1,
                  net.tigereye.chestcavity.ChestCavity.config
                      .REACTION
                      .HUO_TAN
                      .flameMarkDurationTicks));
          net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.add(
              le,
              net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys.IGNITE_WINDOW,
              Math.max(
                  1,
                  net.tigereye.chestcavity.ChestCavity.config.REACTION.HUO_TAN.igniteWindowTicks));
          int igniteSec =
              Math.max(
                  0,
                  net.tigereye.chestcavity.ChestCavity.config.REACTION.HUO_TAN.emberIgniteSeconds);
          if (igniteSec > 0) {
            le.igniteForSeconds(igniteSec);
          }
          applied++;
        }
      }
      if (aec.getDuration() <= 20 && !Boolean.TRUE.equals(FIRE_FINALIZED.get(e.getKey()))) {
        float r = Math.max(0.5F, aec.getRadius());
        var box =
            new net.minecraft.world.phys.AABB(
                aec.getX() - r,
                aec.getY() - r,
                aec.getZ() - r,
                aec.getX() + r,
                aec.getY() + r,
                aec.getZ() + r);
        var list =
            foundLevel.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                box,
                le -> le != null && le.isAlive());
        for (var le : list) {
          net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.add(
              le, net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys.IGNITE_WINDOW, 40);
        }
        FIRE_FINALIZED.put(e.getKey(), Boolean.TRUE);
      }
      for (var be : new java.util.ArrayList<>(BLOOD_KEYS.entrySet())) {
        AreaEffectCloud blood = null;
        for (ServerLevel lvl : server.getAllLevels()) {
          var ent = lvl.getEntity(be.getValue());
          if (ent instanceof AreaEffectCloud cloud) {
            blood = cloud;
            break;
          }
        }
        if (blood == null) {
          BLOOD_KEYS.remove(be.getKey());
          continue;
        }
        double dx = aec.getX() - blood.getX();
        double dy = aec.getY() - blood.getY();
        double dz = aec.getZ() - blood.getZ();
        double dist2 = dx * dx + dy * dy + dz * dz;
        if (dist2 < 0.25) {
          EffectsEngine.queueAoEDamage(
              foundLevel,
              aec.getX(),
              aec.getY(),
              aec.getZ(),
              2.0F,
              3.0D,
              null,
              EffectsEngine.VisualTheme.STEAM);
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

  public static int countFireResiduesNear(
      ServerLevel level, double x, double y, double z, double radius) {
    if (level == null || radius <= 0.0D) {
      return 0;
    }
    double r2 = radius * radius;
    int count = 0;
    for (var e : new java.util.ArrayList<>(FIRE_KEYS.entrySet())) {
      UUID id = e.getValue();
      var ent = level.getEntity(id);
      if (ent instanceof AreaEffectCloud aec) {
        double dx = aec.getX() - x, dy = aec.getY() - y, dz = aec.getZ() - z;
        if (dx * dx + dy * dy + dz * dz <= r2) {
          count++;
        }
      }
    }
    return count;
  }

  private static AreaEffectCloud find(ServerLevel level, UUID id) {
    if (level == null || id == null) {
      return null;
    }
    var e = level.getEntity(id);
    return e instanceof AreaEffectCloud a ? a : null;
  }

  private static int detectNearbyHuoTanTier(
      ServerLevel level, double x, double y, double z, float r) {
    // 简化：根据粒子与随机，作为占位实现；后续可接入真实“火炭层级”来源
    return level.getRandom().nextInt(4);
  }
}
