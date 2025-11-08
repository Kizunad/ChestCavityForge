package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianmaiTuning;

/**
 * 剑脉蛊音效封装。
 *
 * <p>包含：
 * <ul>
 *   <li>被动效果：心跳音效</li>
 *   <li>主动效果：激活音效</li>
 * </ul>
 */
public final class JianmaiAudioEffects {

  private JianmaiAudioEffects() {}

  private static final String MOD_ID = "guzhenren";

  // 音效资源位置
  // 注意：这些音效需要在 sounds.json 中注册
  private static final ResourceLocation HEARTBEAT_SOUND_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jianmai.heartbeat");

  private static final ResourceLocation ACTIVATION_SOUND_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jianmai.activate");

  // ==================== 被动效果：心跳音效 ====================

  /**
   * 播放心跳音效（每10秒触发一次）。
   *
   * <p>音量很小，不打扰其他音效。
   *
   * @param player 玩家
   */
  public static void playHeartbeat(ServerPlayer player) {
    if (player == null) {
      return;
    }
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }

    Vec3 pos = player.position();

    // 使用自定义音效（如果资源包中存在）
    // 临时使用原版音效作为占位符
    // TODO: 替换为实际的剑脉蛊心跳音效
    SoundEvent heartbeatSound = SoundEvents.BEACON_AMBIENT;

    level.playSound(
        null, // 所有玩家都能听到
        pos.x,
        pos.y,
        pos.z,
        heartbeatSound,
        SoundSource.PLAYERS,
        JianmaiTuning.HEARTBEAT_VOLUME, // 音量：0.3（很小声）
        0.6f); // 音高：降低以模拟心跳声
  }

  // ==================== 主动效果：激活音效 ====================

  /**
   * 播放主动技能激活音效。
   *
   * <p>清晰有力，音量适中。
   *
   * @param player 玩家
   * @param deltaAmount 增幅量（可用于调整音效强度）
   */
  public static void playActivation(ServerPlayer player, double deltaAmount) {
    if (player == null) {
      return;
    }
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }

    Vec3 pos = player.position();

    // 使用自定义音效（如果资源包中存在）
    // 临时使用原版音效作为占位符
    // TODO: 替换为实际的剑脉蛊激活音效
    SoundEvent activationSound = SoundEvents.BEACON_ACTIVATE;

    // 根据增幅量调整音高（增幅越大，音高越高）
    float pitch = calculatePitchFromDelta(deltaAmount);

    level.playSound(
        null, // 所有玩家都能听到
        pos.x,
        pos.y,
        pos.z,
        activationSound,
        SoundSource.PLAYERS,
        JianmaiTuning.ACTIVATION_VOLUME, // 音量：1.0（清晰可听）
        pitch);

    // 添加电流声强化效果
    playElectricEffect(level, pos);
  }

  /**
   * 播放电流效果音。
   *
   * @param level 世界
   * @param pos 位置
   */
  private static void playElectricEffect(ServerLevel level, Vec3 pos) {
    // 使用雷击音效模拟电流
    level.playSound(
        null,
        pos.x,
        pos.y,
        pos.z,
        SoundEvents.LIGHTNING_BOLT_THUNDER,
        SoundSource.PLAYERS,
        0.4f, // 音量较小
        1.5f); // 音高较高
  }

  /**
   * 根据增幅量计算音高。
   *
   * @param deltaAmount 增幅量
   * @return 音高（0.5 - 2.0）
   */
  private static float calculatePitchFromDelta(double deltaAmount) {
    // 基础音高
    float basePitch = 1.0f;

    // 根据增幅量调整（每100增幅提高0.1音高）
    float pitchBoost = (float) (deltaAmount / 1000.0);

    // 限制在合理范围内
    return Math.max(0.8f, Math.min(1.5f, basePitch + pitchBoost));
  }

  // ==================== 可选：连接音效 ====================

  /**
   * 播放连接音效（可选）。
   *
   * <p>当雷电链击中飞剑时播放。
   *
   * @param level 世界
   * @param pos 位置
   */
  public static void playHitEffect(ServerLevel level, Vec3 pos) {
    if (level == null) {
      return;
    }

    // 微弱的击中音效
    level.playSound(
        null,
        pos.x,
        pos.y,
        pos.z,
        SoundEvents.LIGHTNING_BOLT_IMPACT,
        SoundSource.PLAYERS,
        0.3f, // 音量很小
        1.2f); // 音高略高
  }
}
