package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

/**
 * 剑锋蛊特效与提示接口。
 *
 * <p>提供视觉/音效入口，便于后续扩展与调参。
 *
 * <p>特效列表：
 * <ul>
 *   <li>锋芒化形激活：右臂剑刃化粒子效果 + 金铁声</li>
 *   <li>协同突击：飞剑起手扫击粒子 + 命中暴击粒子</li>
 *   <li>剑意共振：环形光屑粒子 + 清脆短音</li>
 * </ul>
 */
public final class JianFengGuFx {

  private JianFengGuFx() {}

  /**
   * 播放冷却提示（客户端 toast）。
   *
   * @param player 玩家
   * @param abilityId 技能 ID
   * @param readyAtTick 冷却结束时间
   * @param nowTick 当前时间
   */
  public static void scheduleCooldownToast(
      ServerPlayer player, ResourceLocation abilityId, long readyAtTick, long nowTick) {
    ActiveSkillRegistry.scheduleReadyToast(player, abilityId, readyAtTick, nowTick);
  }

  /**
   * 播放主动技能激活特效（锋芒化形）。
   *
   * <p>效果：
   * <ul>
   *   <li>粒子：END_ROD（沿右臂→手掌路径 6–8 点）、ENCHANT（短尾迹）</li>
   *   <li>音效：金铁声（ANVIL_PLACE，音高 1.1）</li>
   * </ul>
   *
   * @param player 玩家
   */
  public static void playActivate(ServerPlayer player) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }

    Vec3 playerPos = player.position();
    Vec3 lookAngle = player.getLookAngle();
    Vec3 rightVec = lookAngle.cross(new Vec3(0, 1, 0)).normalize();

    // 音效：金铁声（短促）
    level.playSound(
        null,
        playerPos.x,
        playerPos.y,
        playerPos.z,
        SoundEvents.ANVIL_PLACE,
        SoundSource.PLAYERS,
        0.6F,
        1.1F);

    // 粒子：沿右臂到手掌的路径生成 END_ROD 粒子
    Vec3 shoulderPos = playerPos.add(0, 1.3, 0).add(rightVec.scale(0.3));
    Vec3 handPos = shoulderPos.add(rightVec.scale(0.5)).add(lookAngle.scale(0.3));

    int particleCount = 8;
    for (int i = 0; i < particleCount; i++) {
      double progress = i / (double) particleCount;
      Vec3 particlePos = shoulderPos.add(handPos.subtract(shoulderPos).scale(progress));

      // 主粒子：END_ROD
      level.sendParticles(
          ParticleTypes.END_ROD,
          particlePos.x,
          particlePos.y,
          particlePos.z,
          1,
          0.02,
          0.02,
          0.02,
          0.01);

      // 附加粒子：ENCHANT（尾迹）
      if (i % 2 == 0) {
        level.sendParticles(
            ParticleTypes.ENCHANT,
            particlePos.x,
            particlePos.y,
            particlePos.z,
            2,
            0.03,
            0.03,
            0.03,
            0.02);
      }
    }

    // 手掌位置额外爆发
    level.sendParticles(
        ParticleTypes.SWEEP_ATTACK,
        handPos.x,
        handPos.y,
        handPos.z,
        1,
        0.0,
        0.0,
        0.0,
        0.0);
  }

  /**
   * 播放协同突击特效。
   *
   * <p>效果：
   * <ul>
   *   <li>起手：SWEEP_ATTACK 粒子</li>
   *   <li>命中：CRIT 粒子</li>
   *   <li>音效：PLAYER_ATTACK_SWEEP 起手，ANVIL_LAND 命中</li>
   * </ul>
   *
   * @param level 服务端世界
   * @param swordPos 飞剑位置
   * @param targetPos 目标位置
   */
  public static void playCoopStrike(ServerLevel level, Vec3 swordPos, Vec3 targetPos) {
    if (level == null) {
      return;
    }

    // 起手音效：扫击
    level.playSound(
        null,
        swordPos.x,
        swordPos.y,
        swordPos.z,
        SoundEvents.PLAYER_ATTACK_SWEEP,
        SoundSource.PLAYERS,
        0.5F,
        1.0F);

    // 起手粒子：SWEEP_ATTACK
    level.sendParticles(
        ParticleTypes.SWEEP_ATTACK,
        swordPos.x,
        swordPos.y,
        swordPos.z,
        2,
        0.3,
        0.3,
        0.3,
        0.1);

    // 命中粒子：CRIT（目标位置）
    if (targetPos != null) {
      level.sendParticles(
          ParticleTypes.CRIT,
          targetPos.x,
          targetPos.y + 0.5,
          targetPos.z,
          8,
          0.3,
          0.3,
          0.3,
          0.2);

      // 命中音效：铁砧落地
      level.playSound(
          null,
          targetPos.x,
          targetPos.y,
          targetPos.z,
          SoundEvents.ANVIL_LAND,
          SoundSource.PLAYERS,
          0.4F,
          1.2F);
    }
  }

  /**
   * 播放剑意共振特效。
   *
   * <p>效果：
   * <ul>
   *   <li>粒子：以宿主为圆心 3.5 格环状"花瓣状光屑"</li>
   *   <li>音效：玻璃轻鸣叠铃（GLASS_BREAK，音高 1.2）</li>
   * </ul>
   *
   * @param entity 宿主实体
   */
  public static void playResonance(LivingEntity entity) {
    if (!(entity.level() instanceof ServerLevel level)) {
      return;
    }

    Vec3 entityPos = entity.position();

    // 音效：玻璃轻鸣叠铃
    level.playSound(
        null,
        entityPos.x,
        entityPos.y,
        entityPos.z,
        SoundEvents.GLASS_BREAK,
        SoundSource.PLAYERS,
        0.5F,
        1.2F);

    // 粒子：环形"花瓣状光屑"（密度低，1 圈）
    double radius = 3.5;
    int particleCount = 24; // 密度较低

    for (int i = 0; i < particleCount; i++) {
      double angle = (Math.PI * 2.0 * i) / particleCount;
      double x = entityPos.x + Math.cos(angle) * radius;
      double z = entityPos.z + Math.sin(angle) * radius;
      double y = entityPos.y + 1.0; // 腰部高度

      // 主粒子：END_ROD（花瓣状）
      level.sendParticles(
          ParticleTypes.END_ROD,
          x,
          y,
          z,
          1,
          0.0,
          0.05,
          0.0,
          0.01);

      // 附加粒子：ENCHANT（光屑效果）
      if (i % 3 == 0) {
        level.sendParticles(
            ParticleTypes.ENCHANT,
            x,
            y,
            z,
            2,
            0.05,
            0.05,
            0.05,
            0.02);
      }
    }

    // 中心位置额外粒子（能量汇聚感）
    level.sendParticles(
        ParticleTypes.TOTEM_OF_UNDYING,
        entityPos.x,
        entityPos.y + 1.0,
        entityPos.z,
        5,
        0.3,
        0.3,
        0.3,
        0.05);
  }
}
