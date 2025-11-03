package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.rift.RiftEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.RiftTuning;
import net.tigereye.chestcavity.registration.CCSoundEvents;
import org.joml.Vector3f;

/**
 * 裂隙特效
 *
 * <p>集中管理裂隙系统的所有视觉与音效特效。
 */
public final class RiftFx {

  private RiftFx() {}

  /**
   * 生成特效
   *
   * @param level 世界
   * @param pos 位置
   * @param isMajor 是否主要裂隙
   */
  public static void spawnFx(ServerLevel level, Vec3 pos, boolean isMajor) {
    if (!RiftTuning.FX_ENABLED) return;

    if (RiftTuning.PARTICLE_ENABLED) {
      int count = RiftTuning.SPAWN_PARTICLE_COUNT;
      double radius = isMajor ? 2.5 : 1.5;

      // 环形粒子圈
      for (int i = 0; i < count; i++) {
        double angle = (Math.PI * 2 * i) / count;
        double x = pos.x + Math.cos(angle) * radius * 0.8;
        double z = pos.z + Math.sin(angle) * radius * 0.8;
        double y = pos.y + 0.1;

        // 紫青过渡粒子
        level.sendParticles(
            new DustColorTransitionOptions(
                new Vector3f(RiftTuning.COLOR_PRIMARY),
                new Vector3f(RiftTuning.COLOR_SECONDARY),
                isMajor ? 1.2f : 0.8f),
            x, y, z, 1,
            0.0, 0.05, 0.0, 0.02);

        // 烟雾粒子
        if (i % 3 == 0) {
          level.sendParticles(
              ParticleTypes.SMOKE,
              x, y, z, 1,
              0.0, 0.01, 0.0, 0.01);
        }
      }

      // 中心传送门粒子
      level.sendParticles(
          ParticleTypes.REVERSE_PORTAL,
          pos.x, pos.y + 0.5, pos.z, 15,
          radius * 0.3, 0.2, radius * 0.3, 0.05);
    }

    if (RiftTuning.SOUND_ENABLED) {
      float pitch = RiftTuning.PITCH_BASE + randomPitch();
      level.playSound(
          null,
          pos.x, pos.y, pos.z,
          SoundEvents.ENDER_EYE_DEATH,
          SoundSource.PLAYERS,
          RiftTuning.VOL_SPAWN,
          pitch);

      // 叠加音效
      level.playSound(
          null,
          pos.x, pos.y, pos.z,
          SoundEvents.RESPAWN_ANCHOR_SET_SPAWN,
          SoundSource.PLAYERS,
          RiftTuning.VOL_SPAWN * 0.5f,
          pitch * 0.8f);
    }
  }

  /**
   * 伤害特效
   *
   * @param level 世界
   * @param pos 位置
   */
  public static void damageFx(ServerLevel level, Vec3 pos) {
    if (!RiftTuning.FX_ENABLED) return;

    if (RiftTuning.PARTICLE_ENABLED) {
      int count = RiftTuning.DAMAGE_PARTICLE_COUNT;

      for (int i = 0; i < count; i++) {
        double offsetX = (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.5;
        double offsetY = (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.0;
        double offsetZ = (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.5;

        // 暗紫色粒子
        level.sendParticles(
            new DustColorTransitionOptions(
                new Vector3f(RiftTuning.COLOR_PRIMARY),
                new Vector3f(0.3f, 0.0f, 0.5f),
                0.6f),
            pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ, 1,
            0.0, 0.0, 0.0, 0.01);

        // 龙息粒子（增强视觉冲击）
        if (i % 2 == 0) {
          level.sendParticles(
              ParticleTypes.DRAGON_BREATH,
              pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ, 1,
              0.0, 0.0, 0.0, 0.0);
        }
      }
    }

    if (RiftTuning.SOUND_ENABLED) {
      level.playSound(
          null,
          pos.x, pos.y, pos.z,
          SoundEvents.CONDUIT_ATTACK_TARGET,
          SoundSource.PLAYERS,
          RiftTuning.VOL_DAMAGE,
          RiftTuning.PITCH_BASE + randomPitch() * 0.5f);
    }
  }

  /**
   * 吸收特效
   *
   * @param level 世界
   * @param absorberPos 吸收者位置
   * @param absorbedPos 被吸收者位置
   */
  public static void absorbFx(ServerLevel level, Vec3 absorberPos, Vec3 absorbedPos) {
    if (!RiftTuning.FX_ENABLED) return;

    if (RiftTuning.PARTICLE_ENABLED) {
      int count = RiftTuning.ABSORB_PARTICLE_COUNT;
      Vec3 delta = absorberPos.subtract(absorbedPos);

      // 从被吸收位置到吸收者位置的粒子轨迹
      for (int i = 0; i < count; i++) {
        double t = i / (double) count;
        Vec3 particlePos = absorbedPos.add(delta.scale(t));

        // 金色渐变
        level.sendParticles(
            new DustColorTransitionOptions(
                new Vector3f(RiftTuning.COLOR_ABSORB),
                new Vector3f(RiftTuning.COLOR_PRIMARY),
                0.9f),
            particlePos.x, particlePos.y + 0.2, particlePos.z, 1,
            0.05, 0.05, 0.05, 0.01);

        // 末地烛粒子
        if (i % 3 == 0) {
          level.sendParticles(
              ParticleTypes.END_ROD,
              particlePos.x, particlePos.y + 0.2, particlePos.z, 1,
              0.0, 0.0, 0.0, 0.0);
        }
      }

      // 吸收者处发光
      level.sendParticles(
          ParticleTypes.GLOW,
          absorberPos.x, absorberPos.y + 0.5, absorberPos.z, 8,
          0.5, 0.3, 0.5, 0.05);
    }

    if (RiftTuning.SOUND_ENABLED) {
      level.playSound(
          null,
          absorberPos.x, absorberPos.y, absorberPos.z,
          SoundEvents.BEACON_POWER_SELECT,
          SoundSource.PLAYERS,
          RiftTuning.VOL_ABSORB,
          RiftTuning.PITCH_BASE + randomPitch());

      level.playSound(
          null,
          absorbedPos.x, absorbedPos.y, absorbedPos.z,
          SoundEvents.EXPERIENCE_ORB_PICKUP,
          SoundSource.PLAYERS,
          RiftTuning.VOL_ABSORB * 0.6f,
          1.2f);
    }
  }

  /**
   * 共鸣特效
   *
   * @param level 世界
   * @param pos 位置
   */
  public static void resonanceFx(ServerLevel level, Vec3 pos) {
    if (!RiftTuning.FX_ENABLED) return;

    if (RiftTuning.PARTICLE_ENABLED) {
      int count = RiftTuning.RESONANCE_PARTICLE_COUNT;

      // 环形波纹
      for (int i = 0; i < count; i++) {
        double angle = (Math.PI * 2 * i) / count;
        double radius = 1.5;
        double x = pos.x + Math.cos(angle) * radius;
        double z = pos.z + Math.sin(angle) * radius;

        // 青蓝共鸣色
        level.sendParticles(
            new DustColorTransitionOptions(
                new Vector3f(RiftTuning.COLOR_SECONDARY),
                new Vector3f(RiftTuning.COLOR_WAVE),
                1.0f),
            x, pos.y + 0.3, z, 2,
            0.0, 0.1, 0.0, 0.03);
      }

      // 音爆粒子
      level.sendParticles(
          ParticleTypes.SONIC_BOOM,
          pos.x, pos.y, pos.z, 1,
          0.0, 0.0, 0.0, 0.0);

      // 闪光粒子
      level.sendParticles(
          ParticleTypes.FLASH,
          pos.x, pos.y + 0.5, pos.z, 1,
          0.0, 0.0, 0.0, 0.0);
    }

    if (RiftTuning.SOUND_ENABLED) {
      level.playSound(
          null,
          pos.x, pos.y, pos.z,
          SoundEvents.WARDEN_SONIC_BOOM,
          SoundSource.PLAYERS,
          RiftTuning.VOL_RESONANCE,
          RiftTuning.PITCH_BASE + randomPitch());

      level.playSound(
          null,
          pos.x, pos.y, pos.z,
          SoundEvents.RESPAWN_ANCHOR_CHARGE,
          SoundSource.PLAYERS,
          RiftTuning.VOL_RESONANCE * 0.4f,
          1.3f);
    }
  }

  /**
   * 共鸣波传播特效（从起点到终点）
   *
   * @param level 世界
   * @param fromPos 起点
   * @param toPos 终点
   */
  public static void waveFx(ServerLevel level, Vec3 fromPos, Vec3 toPos) {
    if (!RiftTuning.FX_ENABLED) return;

    if (RiftTuning.PARTICLE_ENABLED) {
      int count = RiftTuning.WAVE_PARTICLE_COUNT;
      Vec3 delta = toPos.subtract(fromPos);
      double distance = delta.length();

      // 沿路径生成粒子
      for (int i = 0; i < count; i++) {
        double t = i / (double) count;
        Vec3 particlePos = fromPos.add(delta.scale(t));

        // 蓝白渐变
        level.sendParticles(
            new DustColorTransitionOptions(
                new Vector3f(RiftTuning.COLOR_WAVE),
                new Vector3f(1.0f, 1.0f, 1.0f),
                0.7f),
            particlePos.x, particlePos.y + 0.3, particlePos.z, 1,
            0.05, 0.05, 0.05, 0.02);

        // 电火花粒子
        if (i % 4 == 0) {
          level.sendParticles(
              ParticleTypes.ELECTRIC_SPARK,
              particlePos.x, particlePos.y + 0.3, particlePos.z, 2,
              0.1, 0.1, 0.1, 0.01);
        }
      }
    }

    if (RiftTuning.SOUND_ENABLED) {
      level.playSound(
          null,
          fromPos.x, fromPos.y, fromPos.z,
          SoundEvents.LIGHTNING_BOLT_THUNDER,
          SoundSource.PLAYERS,
          RiftTuning.VOL_WAVE * 0.3f,
          1.8f + randomPitch() * 0.2f);
    }
  }

  /**
   * 消失特效
   *
   * @param level 世界
   * @param pos 位置
   * @param isMajor 是否主要裂隙
   */
  public static void despawnFx(ServerLevel level, Vec3 pos, boolean isMajor) {
    if (!RiftTuning.FX_ENABLED) return;

    if (RiftTuning.PARTICLE_ENABLED) {
      int count = RiftTuning.DESPAWN_PARTICLE_COUNT;
      double radius = isMajor ? 2.0 : 1.2;

      // 向内收缩的粒子
      for (int i = 0; i < count; i++) {
        double angle = (Math.PI * 2 * i) / count;
        double x = pos.x + Math.cos(angle) * radius;
        double z = pos.z + Math.sin(angle) * radius;

        // 紫黑色粒子
        level.sendParticles(
            new DustColorTransitionOptions(
                new Vector3f(RiftTuning.COLOR_PRIMARY),
                new Vector3f(0.1f, 0.0f, 0.2f),
                0.5f),
            x, pos.y + 0.2, z, 1,
            -Math.cos(angle) * 0.1, 0.05, -Math.sin(angle) * 0.1, 0.01);
      }

      // 中心烟雾
      level.sendParticles(
          ParticleTypes.LARGE_SMOKE,
          pos.x, pos.y + 0.3, pos.z, 5,
          0.2, 0.1, 0.2, 0.01);
    }

    if (RiftTuning.SOUND_ENABLED) {
      level.playSound(
          null,
          pos.x, pos.y, pos.z,
          SoundEvents.ENDER_EYE_DEATH,
          SoundSource.PLAYERS,
          RiftTuning.VOL_DESPAWN,
          0.8f + randomPitch() * 0.3f);
    }
  }

  /**
   * 环境粒子（idle效果，持续播放）
   *
   * @param rift 裂隙实体
   */
  public static void ambientFx(RiftEntity rift) {
    if (!RiftTuning.FX_ENABLED || !RiftTuning.PARTICLE_ENABLED) return;
    if (!(rift.level() instanceof ServerLevel level)) return;

    Vec3 pos = rift.position();
    boolean isMajor = rift.getRiftType() == net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.rift.RiftType.MAJOR;

    // 每5tick播放一次粒子
    if (rift.tickCount % 5 != 0) return;

    // 随机偏移粒子位置
    double offsetX = (ThreadLocalRandom.current().nextDouble() - 0.5) * (isMajor ? 2.0 : 1.2);
    double offsetZ = (ThreadLocalRandom.current().nextDouble() - 0.5) * (isMajor ? 2.0 : 1.2);

    level.sendParticles(
        new DustColorTransitionOptions(
            new Vector3f(RiftTuning.COLOR_PRIMARY),
            new Vector3f(RiftTuning.COLOR_SECONDARY),
            0.4f),
        pos.x + offsetX, pos.y + 0.1, pos.z + offsetZ, 1,
        0.0, 0.02, 0.0, 0.005);
  }

  /**
   * 生成随机音调变化
   */
  private static float randomPitch() {
    float var = RiftTuning.PITCH_VAR;
    return (float) (ThreadLocalRandom.current().nextDouble(-var, var));
  }
}
