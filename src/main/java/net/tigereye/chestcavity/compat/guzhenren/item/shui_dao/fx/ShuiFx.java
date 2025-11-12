package net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.fx;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.tuning.ShuiTuning;
import org.joml.Vector3f;

/**
 * 水道特效工具类。
 *
 * <p>集中管理水道相关的粒子特效和音效,便于维护和配置。
 */
public final class ShuiFx {

  /** 玩家治疗特效颜色(浅青色)。 */
  private static final DustParticleOptions PLAYER_GLOW =
      new DustParticleOptions(new Vector3f(90f / 255f, 210f / 255f, 215f / 255f), 1.0f);

  /** 非玩家治疗特效颜色(深青色)。 */
  private static final DustParticleOptions NON_PLAYER_GLOW =
      new DustParticleOptions(new Vector3f(60f / 255f, 150f / 255f, 170f / 255f), 0.6f);

  private ShuiFx() {}

  /**
   * 播放治疗特效。
   *
   * @param entity 实体
   * @param stackCount 器官数量
   * @param isPlayer 是否玩家
   * @param isStress 是否应激反应
   */
  public static void playHealingFx(
      LivingEntity entity,
      int stackCount,
      boolean isPlayer,
      boolean isStress) {
    if (!(entity.level() instanceof ServerLevel server)) {
      return;
    }
    if (ShuiTuning.PARTICLE_ENABLED) {
      spawnHealingParticles(server, entity, stackCount, isPlayer, isStress);
    }
    if (ShuiTuning.SOUND_ENABLED) {
      playHealingSound(server, entity, isPlayer, isStress);
    }
  }

  /**
   * 播放水盾伤害特效。
   *
   * @param entity 实体
   * @param stackCount 器官数量
   * @param shieldBroken 护盾是否破碎
   */
  public static void playShieldDamageFx(
      LivingEntity entity,
      int stackCount,
      boolean shieldBroken) {
    if (!(entity.level() instanceof ServerLevel server)) {
      return;
    }
    if (ShuiTuning.PARTICLE_ENABLED) {
      spawnShieldDamageParticles(server, entity, stackCount, shieldBroken);
    }
    if (ShuiTuning.SOUND_ENABLED) {
      playShieldDamageSound(entity, shieldBroken);
    }
  }

  /**
   * 播放水流环绕特效。
   *
   * @param level 世界
   * @param center 中心位置
   * @param radius 半径
   * @param density 密度
   */
  public static void playWaterRingFx(
      ServerLevel level,
      Vec3 center,
      double radius,
      int density) {
    if (!ShuiTuning.PARTICLE_ENABLED) {
      return;
    }
    for (int i = 0; i < density; i++) {
      double angle = (Math.PI * 2 * i) / density;
      double x = center.x + Math.cos(angle) * radius;
      double z = center.z + Math.sin(angle) * radius;
      level.sendParticles(
          ParticleTypes.BUBBLE_POP,
          x,
          center.y + 0.5,
          z,
          2,
          0.1,
          0.1,
          0.1,
          0.01);
    }
  }

  private static void spawnHealingParticles(
      ServerLevel server,
      LivingEntity entity,
      int stackCount,
      boolean isPlayer,
      boolean stress) {
    RandomSource random = entity.getRandom();
    DustParticleOptions glow = isPlayer ? PLAYER_GLOW : NON_PLAYER_GLOW;
    int glowBursts = (isPlayer ? 6 : 3) * stackCount;
    if (stress) {
      glowBursts *= 2;
    }
    Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
    double radius = 0.35 + 0.1 * stackCount;
    for (int i = 0; i < glowBursts; i++) {
      double angle = random.nextDouble() * Math.PI * 2.0;
      double distance = radius * (0.35 + random.nextDouble() * 0.65);
      double offsetX = Math.cos(angle) * distance;
      double offsetZ = Math.sin(angle) * distance;
      double offsetY = (random.nextDouble() - 0.5) * 0.4;
      server.sendParticles(
          glow,
          center.x + offsetX,
          center.y + offsetY,
          center.z + offsetZ,
          1,
          0.02,
          0.02,
          0.02,
          0.01);
    }
    int dropletCount = (isPlayer ? 12 : 6) * stackCount;
    if (stress) {
      dropletCount += (isPlayer ? 8 : 4) * stackCount;
    }
    server.sendParticles(
        stress ? ParticleTypes.SPLASH : ParticleTypes.DRIPPING_DRIPSTONE_WATER,
        center.x,
        center.y,
        center.z,
        dropletCount,
        0.25,
        0.35,
        0.25,
        stress ? 0.04 : 0.02);
    int cloudCount = stackCount * (stress ? 6 : 3);
    cloudCount = Math.max(3, Math.min(24, cloudCount));
    server.sendParticles(
        ParticleTypes.CLOUD,
        center.x,
        center.y + 0.1,
        center.z,
        cloudCount,
        0.25,
        0.15,
        0.25,
        0.01);
  }

  private static void playHealingSound(
      ServerLevel level,
      LivingEntity entity,
      boolean isPlayer,
      boolean stress) {
    double x = entity.getX();
    double y = entity.getY();
    double z = entity.getZ();
    float dropletPitch = isPlayer ? 1.05f : 0.9f;
    if (stress) {
      dropletPitch += 0.1f;
    }
    level.playSound(
        null,
        x,
        y,
        z,
        SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE,
        SoundSource.PLAYERS,
        isPlayer ? 0.7f : 0.5f,
        dropletPitch);
    level.playSound(
        null,
        x,
        y,
        z,
        SoundEvents.GENERIC_DRINK,
        SoundSource.PLAYERS,
        stress ? 0.8f : 0.6f,
        isPlayer ? 1.2f : 0.95f);
  }

  private static void spawnShieldDamageParticles(
      ServerLevel server,
      LivingEntity entity,
      int stackCount,
      boolean shieldBroken) {
    double spread = shieldBroken ? 0.9 : 0.6;
    int splashCount = shieldBroken ? 12 + stackCount * 3 : 6 + stackCount * 2;
    server.sendParticles(
        ParticleTypes.SPLASH,
        entity.getX(),
        entity.getY() + entity.getBbHeight() * 0.6,
        entity.getZ(),
        splashCount,
        spread,
        0.3,
        spread,
        0.2);
    server.sendParticles(
        ParticleTypes.BUBBLE_POP,
        entity.getX(),
        entity.getY() + entity.getBbHeight() * 0.6,
        entity.getZ(),
        splashCount / 2,
        spread * 0.5,
        0.2,
        spread * 0.5,
        0.1);
  }

  private static void playShieldDamageSound(LivingEntity entity, boolean shieldBroken) {
    entity
        .level()
        .playSound(
            null,
            entity.getX(),
            entity.getY(),
            entity.getZ(),
            shieldBroken ? SoundEvents.SHIELD_BREAK : SoundEvents.GENERIC_SPLASH,
            SoundSource.PLAYERS,
            shieldBroken ? 0.8f : 0.5f,
            shieldBroken ? 1.0f : 1.1f);
  }
}
