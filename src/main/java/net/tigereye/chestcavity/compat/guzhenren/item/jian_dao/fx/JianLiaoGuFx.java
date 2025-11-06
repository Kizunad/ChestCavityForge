package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/**
 * 剑疗蛊特效与音效封装。
 *
 * <p>包含：
 * <ul>
 *   <li>主动技能（剑血互济）的激活、传导、修复特效</li>
 *   <li>被动技能（心跳治疗）的脉动特效</li>
 *   <li>被动技能（飞剑互补）的捐献、传导、接收特效</li>
 * </ul>
 */
public final class JianLiaoGuFx {

  private JianLiaoGuFx() {}

  // ==================== 主动技能：剑血互济 ====================

  /**
   * 主动技能激活特效：玩家位置血色粒子向上飘散（生命力消耗）。
   *
   * @param player 玩家
   * @param hpSpend 消耗的生命值
   */
  public static void playActiveActivate(ServerPlayer player, float hpSpend) {
    if (!JianLiaoGuFxTuning.FX_ENABLED || player == null) {
      return;
    }
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }

    Vec3 pos = player.position();
    double x = pos.x;
    double y = pos.y;
    double z = pos.z;

    // 血色粒子向上飘散（DAMAGE_INDICATOR 表示生命损失）
    int particleCount = Math.max(8, Math.min(32, (int) (hpSpend * 2)));
    for (int i = 0; i < particleCount; i++) {
      double offsetX = (Math.random() - 0.5) * 0.6;
      double offsetY = 0.5 + Math.random() * 0.8;
      double offsetZ = (Math.random() - 0.5) * 0.6;
      double velocityY = 0.02 + Math.random() * 0.04;
      level.sendParticles(
          ParticleTypes.DAMAGE_INDICATOR,
          x + offsetX,
          y + offsetY,
          z + offsetZ,
          1,
          0.0,
          velocityY,
          0.0,
          0.02);
    }

    // 胸口位置闪烁红色粒子（CRIMSON_SPORE - 深红色孢子）
    for (int i = 0; i < 12; i++) {
      double offsetX = (Math.random() - 0.5) * 0.4;
      double offsetY = 1.2 + (Math.random() - 0.5) * 0.2;
      double offsetZ = (Math.random() - 0.5) * 0.4;
      level.sendParticles(
          ParticleTypes.CRIMSON_SPORE, x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, 0.02, 0.0, 0.01);
    }

    // 音效：生命消耗（玩家受伤音效，低音量）
    if (JianLiaoGuFxTuning.SOUND_ENABLED) {
      level.playSound(
          null,
          x,
          y,
          z,
          SoundEvents.PLAYER_HURT,
          SoundSource.PLAYERS,
          JianLiaoGuFxTuning.SOUND_VOLUME_ACTIVE_CONSUME,
          JianLiaoGuFxTuning.SOUND_PITCH_ACTIVE_CONSUME);
    }
  }

  /**
   * 主动技能能量传导特效：从玩家到飞剑的能量束。
   *
   * @param player 玩家
   * @param sword 目标飞剑
   */
  public static void playActiveTransfer(ServerPlayer player, FlyingSwordEntity sword) {
    if (!JianLiaoGuFxTuning.FX_ENABLED || player == null || sword == null) {
      return;
    }
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }

    Vec3 playerPos = player.position().add(0, player.getEyeHeight() * 0.6, 0);
    Vec3 swordPos = sword.position();

    // 从玩家到飞剑的能量束粒子（ENCHANT - 附魔粒子，带有治疗效果的视觉）
    Vec3 direction = swordPos.subtract(playerPos);
    double distance = direction.length();
    if (distance < 0.1) {
      return;
    }

    Vec3 step = direction.normalize().scale(0.4);
    int steps = Math.min(20, (int) (distance / 0.4));
    for (int i = 0; i < steps; i++) {
      Vec3 point = playerPos.add(step.scale(i));
      // ENCHANT 粒子沿路径
      level.sendParticles(ParticleTypes.ENCHANT, point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.01);
      // 少量 HEART 粒子（生命能量）
      if (i % 3 == 0) {
        level.sendParticles(ParticleTypes.HEART, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.0);
      }
    }
  }

  /**
   * 主动技能飞剑修复特效：飞剑被修复时的恢复效果。
   *
   * @param sword 被修复的飞剑
   * @param repairAmount 修复量
   */
  public static void playActiveRepair(FlyingSwordEntity sword, double repairAmount) {
    if (!JianLiaoGuFxTuning.FX_ENABLED || sword == null || repairAmount <= 0.0) {
      return;
    }
    if (!(sword.level() instanceof ServerLevel level)) {
      return;
    }

    Vec3 pos = sword.position();
    double x = pos.x;
    double y = pos.y;
    double z = pos.z;

    // 修复闪光（GLOW - 发光鱿鱼粒子）
    int particleCount = Math.max(6, Math.min(20, (int) (repairAmount / 10)));
    for (int i = 0; i < particleCount; i++) {
      double offsetX = (Math.random() - 0.5) * 0.5;
      double offsetY = (Math.random() - 0.5) * 0.5;
      double offsetZ = (Math.random() - 0.5) * 0.5;
      level.sendParticles(
          ParticleTypes.GLOW, x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, 0.0, 0.0, 0.05);
    }

    // 治疗心形粒子
    for (int i = 0; i < 3; i++) {
      double offsetX = (Math.random() - 0.5) * 0.3;
      double offsetY = (Math.random() - 0.5) * 0.3;
      double offsetZ = (Math.random() - 0.5) * 0.3;
      level.sendParticles(
          ParticleTypes.HEART, x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, 0.02, 0.0, 0.01);
    }
  }

  /**
   * 主动技能完成音效：所有飞剑修复完毕。
   *
   * @param player 玩家
   * @param totalRepaired 总修复量
   */
  public static void playActiveComplete(ServerPlayer player, double totalRepaired) {
    if (!JianLiaoGuFxTuning.SOUND_ENABLED || player == null || totalRepaired <= 0.0) {
      return;
    }
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }

    Vec3 pos = player.position();
    // 音效：经验球收集音效（表示恢复/成功）
    level.playSound(
        null,
        pos.x,
        pos.y,
        pos.z,
        SoundEvents.EXPERIENCE_ORB_PICKUP,
        SoundSource.PLAYERS,
        JianLiaoGuFxTuning.SOUND_VOLUME_ACTIVE_COMPLETE,
        JianLiaoGuFxTuning.SOUND_PITCH_ACTIVE_COMPLETE);
  }

  // ==================== 被动技能1：心跳治疗 ====================

  /**
   * 心跳治疗特效：周期性生命脉动。
   *
   * @param player 玩家
   * @param healAmount 治疗量
   */
  public static void playHeartbeat(ServerPlayer player, float healAmount) {
    if (!JianLiaoGuFxTuning.FX_ENABLED || player == null || healAmount <= 0.0f) {
      return;
    }
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }

    Vec3 pos = player.position();
    double x = pos.x;
    double y = pos.y;
    double z = pos.z;

    // 绿色生命粒子心跳效果（HAPPY_VILLAGER - 绿色爱心）
    int particleCount = Math.max(4, Math.min(12, (int) (healAmount * 10)));
    for (int i = 0; i < particleCount; i++) {
      double angle = (i / (double) particleCount) * Math.PI * 2;
      double radius = 0.4 + Math.random() * 0.2;
      double offsetX = Math.cos(angle) * radius;
      double offsetZ = Math.sin(angle) * radius;
      double offsetY = 0.5 + Math.random() * 0.3;
      level.sendParticles(
          ParticleTypes.HAPPY_VILLAGER,
          x + offsetX,
          y + offsetY,
          z + offsetZ,
          1,
          0.0,
          0.01,
          0.0,
          0.01);
    }

    // 音效：轻微的治疗声音（低音量，高音调）
    if (JianLiaoGuFxTuning.SOUND_ENABLED && Math.random() < JianLiaoGuFxTuning.SOUND_CHANCE_HEARTBEAT) {
      level.playSound(
          null,
          x,
          y,
          z,
          SoundEvents.PLAYER_LEVELUP,
          SoundSource.PLAYERS,
          JianLiaoGuFxTuning.SOUND_VOLUME_HEARTBEAT,
          JianLiaoGuFxTuning.SOUND_PITCH_HEARTBEAT);
    }
  }

  // ==================== 被动技能2：飞剑互补修复 ====================

  /**
   * 飞剑捐献耐久特效：健康飞剑贡献能量。
   *
   * @param donor 捐献飞剑
   * @param cost 捐献的耐久值
   */
  public static void playSwordDonate(FlyingSwordEntity donor, double cost) {
    if (!JianLiaoGuFxTuning.FX_ENABLED || donor == null || cost <= 0.0) {
      return;
    }
    if (!(donor.level() instanceof ServerLevel level)) {
      return;
    }

    Vec3 pos = donor.position();
    double x = pos.x;
    double y = pos.y;
    double z = pos.z;

    // 蓝色能量流失粒子（END_ROD - 末地烛光）
    int particleCount = Math.max(3, Math.min(10, (int) (cost / 5)));
    for (int i = 0; i < particleCount; i++) {
      double offsetX = (Math.random() - 0.5) * 0.3;
      double offsetY = (Math.random() - 0.5) * 0.3;
      double offsetZ = (Math.random() - 0.5) * 0.3;
      double velocityY = -0.02 - Math.random() * 0.02;
      level.sendParticles(
          ParticleTypes.END_ROD, x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, velocityY, 0.0, 0.01);
    }
  }

  /**
   * 飞剑间能量传导特效：从捐献飞剑到接收飞剑的能量传导。
   *
   * @param donor 捐献飞剑
   * @param receiver 接收飞剑
   */
  public static void playSwordTransfer(FlyingSwordEntity donor, FlyingSwordEntity receiver) {
    if (!JianLiaoGuFxTuning.FX_ENABLED || donor == null || receiver == null) {
      return;
    }
    if (!(donor.level() instanceof ServerLevel level)) {
      return;
    }

    Vec3 donorPos = donor.position();
    Vec3 receiverPos = receiver.position();

    // 能量传导线（SOUL_FIRE_FLAME - 青色魂火）
    Vec3 direction = receiverPos.subtract(donorPos);
    double distance = direction.length();
    if (distance < 0.1) {
      return;
    }

    Vec3 step = direction.normalize().scale(0.5);
    int steps = Math.min(15, (int) (distance / 0.5));
    for (int i = 0; i < steps; i++) {
      Vec3 point = donorPos.add(step.scale(i));
      level.sendParticles(
          ParticleTypes.SOUL_FIRE_FLAME, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.01);
    }
  }

  /**
   * 飞剑接收修复特效：低耐久飞剑接收能量恢复。
   *
   * @param receiver 接收飞剑
   * @param repairAmount 接收的修复量
   */
  public static void playSwordReceive(FlyingSwordEntity receiver, double repairAmount) {
    if (!JianLiaoGuFxTuning.FX_ENABLED || receiver == null || repairAmount <= 0.0) {
      return;
    }
    if (!(receiver.level() instanceof ServerLevel level)) {
      return;
    }

    Vec3 pos = receiver.position();
    double x = pos.x;
    double y = pos.y;
    double z = pos.z;

    // 蓝色粒子汇聚（SOUL - 灵魂粒子）
    int particleCount = Math.max(5, Math.min(15, (int) (repairAmount / 8)));
    for (int i = 0; i < particleCount; i++) {
      double offsetX = (Math.random() - 0.5) * 0.5;
      double offsetY = (Math.random() - 0.5) * 0.5;
      double offsetZ = (Math.random() - 0.5) * 0.5;
      level.sendParticles(
          ParticleTypes.SOUL, x + offsetX, y + offsetY, z + offsetZ, 1, -offsetX * 0.1, -offsetY * 0.1, -offsetZ * 0.1, 0.05);
    }

    // GLOW 粒子闪烁（修复完成）
    for (int i = 0; i < 4; i++) {
      double offsetX = (Math.random() - 0.5) * 0.3;
      double offsetY = (Math.random() - 0.5) * 0.3;
      double offsetZ = (Math.random() - 0.5) * 0.3;
      level.sendParticles(
          ParticleTypes.GLOW, x + offsetX, y + offsetY, z + offsetZ, 1, 0.0, 0.0, 0.0, 0.03);
    }
  }

  /**
   * 飞剑互补完成音效：所有低耐久飞剑修复完毕。
   *
   * @param level 世界
   * @param player 玩家
   */
  public static void playSwordRepairComplete(ServerLevel level, ServerPlayer player) {
    if (!JianLiaoGuFxTuning.SOUND_ENABLED || level == null || player == null) {
      return;
    }

    Vec3 pos = player.position();
    // 音效：铁砧使用音效（金属修复）
    level.playSound(
        null,
        pos.x,
        pos.y,
        pos.z,
        SoundEvents.ANVIL_USE,
        SoundSource.PLAYERS,
        JianLiaoGuFxTuning.SOUND_VOLUME_SWORD_REPAIR,
        JianLiaoGuFxTuning.SOUND_PITCH_SWORD_REPAIR);
  }
}
