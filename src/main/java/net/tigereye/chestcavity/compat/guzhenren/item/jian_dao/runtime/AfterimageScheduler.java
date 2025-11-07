package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.skillcalc.DamageKind;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JianYingCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianYingTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;

/**
 * 残影（Afterimage）延迟任务调度器。
 *
 * <p>负责跨 tick 执行残影 AoE 伤害，与通用 DelayedTaskScheduler 可复用。
 */
public final class AfterimageScheduler {

  private static final ResourceLocation JIAN_DAO_INCREASE_EFFECT =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "linkage/jian_dao_increase_effect");
  private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

  private static final ResourceLocation SKILL_AFTERIMAGE_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "jian_dao/afterimage");

  private static final List<AfterimageTask> AFTERIMAGES =
      Collections.synchronizedList(new ArrayList<>());

  private AfterimageScheduler() {}

  /**
   * 添加残影任务到调度队列。
   *
   * @param playerId 玩家 UUID
   * @param level 世界维度
   * @param executeTick 执行时间（游戏刻）
   * @param origin 残影中心位置
   */
  public static void queueAfterimage(
      UUID playerId, ResourceKey<Level> level, long executeTick, Vec3 origin) {
    AFTERIMAGES.add(new AfterimageTask(playerId, level, executeTick, origin));
  }

  /**
   * 每 tick 调用，执行就绪的残影任务。
   *
   * @param level 当前世界
   */
  public static void tickLevel(ServerLevel level) {
    long gameTime = level.getGameTime();
    List<AfterimageTask> pending;
    synchronized (AFTERIMAGES) {
      pending = new ArrayList<>(AFTERIMAGES);
    }
    for (AfterimageTask task : pending) {
      if (!task.level().equals(level.dimension())) {
        continue;
      }
      if (task.executeTick() > gameTime) {
        continue;
      }
      if (executeAfterimage(level, task)) {
        AFTERIMAGES.remove(task);
      }
    }
  }

  private static boolean executeAfterimage(ServerLevel level, AfterimageTask task) {
    Player player = level.getPlayerByUUID(task.playerId());
    if (player == null) {
      return true;
    }

    ChestCavityInstance cc =
        ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).orElse(null);
    double efficiency = 1.0;
    if (cc != null) {
      LinkageChannel channel =
          LedgerOps.ensureChannel(cc, JIAN_DAO_INCREASE_EFFECT, NON_NEGATIVE);
      efficiency += channel.get();
    }

    Vec3 centre = task.origin();
    AABB area = new AABB(centre, centre).inflate(JianYingTuning.AFTERIMAGE_RADIUS);
    List<LivingEntity> victims =
        level.getEntitiesOfClass(
            LivingEntity.class,
            area,
            entity -> entity.isAlive() && entity != player && !entity.isAlliedTo(player));
    if (victims.isEmpty()) {
      return true;
    }

    float damage = JianYingCalculator.afterimageDamage(efficiency);
    for (LivingEntity victim : victims) {
      SwordShadowRuntime.applyPhysicalDamage(
          player,
          victim,
          damage,
          SKILL_AFTERIMAGE_ID,
          java.util.Set.of(DamageKind.MELEE));
      level.sendParticles(
          ParticleTypes.SWEEP_ATTACK,
          victim.getX(),
          victim.getY(0.5),
          victim.getZ(),
          2,
          0.1,
          0.1,
          0.1,
          0.01);
    }
    level.playSound(
        null,
        centre.x,
        centre.y,
        centre.z,
        SoundEvents.PLAYER_ATTACK_SWEEP,
        SoundSource.PLAYERS,
        0.7f,
        0.7f);

    return true;
  }

  private record AfterimageTask(
      UUID playerId, ResourceKey<Level> level, long executeTick, Vec3 origin) {}
}
