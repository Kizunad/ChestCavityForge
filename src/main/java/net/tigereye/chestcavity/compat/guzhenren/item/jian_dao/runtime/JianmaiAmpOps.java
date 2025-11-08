package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.runtime;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianmaiTuning;

/**
 * 剑脉蛊临时倍率操作类。
 *
 * <p>负责管理剑脉效率（JME）的临时增幅倍率，包括：
 * <ul>
 *   <li>基于 JME 的被动增幅（刷新过期时间，宽限期衰减）</li>
 *   <li>主动技能券（叠乘倍率，延长持续时间）</li>
 *   <li>最终倍率计算（裁剪上限）</li>
 * </ul>
 *
 * <p>重要规则：
 * <ul>
 *   <li>临时增幅仅影响"读取期"的道痕值，严禁写回永久值</li>
 *   <li>过期判断：now >= expireGameTime → 清空节点</li>
 *   <li>宽限期：mult 在 graceTicks 内线性插值回 1.0</li>
 * </ul>
 */
public final class JianmaiAmpOps {

  private JianmaiAmpOps() {}

  /**
   * 基于 JME 刷新临时增幅倍率（被动）。
   *
   * <p>规则：
   * <ul>
   *   <li>当 jme > 0 时，刷新过期时间：expireGameTime = max(expire, now + AMP_BASE_DURATION_TICKS)</li>
   *   <li>计算基础倍率：base = 1 + DAO_JME_K * jme</li>
   *   <li>在宽限期内线性插值：mult = lerp_to_1(base, grace)</li>
   * </ul>
   *
   * @param player 玩家
   * @param jme 当前剑脉效率值
   * @param now 当前游戏刻
   */
  public static void refreshFromJME(ServerPlayer player, double jme, long now) {
    if (jme <= 0.0) {
      // JME 为 0，清空增幅
      JianmaiNBT.clearAmp(player);
      return;
    }

    CompoundTag ampData = JianmaiNBT.readAmp(player);
    double ampK = JianmaiNBT.getAmpK(ampData);
    long expire = JianmaiNBT.getExpire(ampData);
    int grace = JianmaiNBT.getGrace(ampData);

    // 刷新过期时间（取更长）
    long newExpire = Math.max(expire, now + JianmaiTuning.AMP_BASE_DURATION_TICKS);

    // 计算基础倍率
    double baseMult = 1.0 + ampK * jme;

    // 宽限期插值（如果在宽限期内）
    double finalMult = baseMult;
    if (now < newExpire && now >= newExpire - grace) {
      // 在宽限期内：线性插值回 1.0
      long remaining = newExpire - now;
      double ratio = (double) remaining / (double) grace;
      finalMult = 1.0 + (baseMult - 1.0) * ratio;
    }

    // 写回
    JianmaiNBT.writeAmp(player, ampK, finalMult, newExpire, grace);
  }

  /**
   * 应用主动技能增幅券（叠乘）。
   *
   * <p>规则：
   * <ul>
   *   <li>叠乘到 mult：mult *= activeMult</li>
   *   <li>裁剪上限：mult = min(mult, AMP_MULT_CAP)</li>
   *   <li>刷新过期时间：expireGameTime = max(expire, now + durationTicks)</li>
   * </ul>
   *
   * @param player 玩家
   * @param activeMult 主动技能倍率
   * @param now 当前游戏刻
   * @param durationTicks 持续时间（tick）
   */
  public static void applyActiveTicket(
      ServerPlayer player, double activeMult, long now, int durationTicks) {
    CompoundTag ampData = JianmaiNBT.readAmp(player);
    double ampK = JianmaiNBT.getAmpK(ampData);
    double currentMult = JianmaiNBT.getMult(ampData);
    long expire = JianmaiNBT.getExpire(ampData);
    int grace = JianmaiNBT.getGrace(ampData);

    // 叠乘并裁剪
    double newMult = currentMult * activeMult;
    newMult = Math.min(newMult, JianmaiTuning.AMP_MULT_CAP);

    // 刷新过期时间（取更长）
    long newExpire = Math.max(expire, now + durationTicks);

    // 写回
    JianmaiNBT.writeAmp(player, ampK, newMult, newExpire, grace);
  }

  /**
   * 获取最终倍率（供"有效道痕"读取使用）。
   *
   * <p>规则：
   * <ul>
   *   <li>检查过期：now >= expireGameTime → 返回 1.0</li>
   *   <li>宽限期：线性插值回 1.0</li>
   *   <li>裁剪上限：max(mult, AMP_MULT_CAP)</li>
   * </ul>
   *
   * @param player 玩家
   * @param now 当前游戏刻
   * @return 最终倍率，默认 1.0
   */
  public static double finalMult(ServerPlayer player, long now) {
    CompoundTag ampData = JianmaiNBT.readAmp(player);
    if (ampData.isEmpty()) {
      return 1.0;
    }

    double mult = JianmaiNBT.getMult(ampData);
    long expire = JianmaiNBT.getExpire(ampData);
    int grace = JianmaiNBT.getGrace(ampData);

    // 检查过期
    if (now >= expire) {
      // 过期，清空并返回 1.0
      JianmaiNBT.clearAmp(player);
      return 1.0;
    }

    // 宽限期插值
    if (now >= expire - grace) {
      long remaining = expire - now;
      double ratio = (double) remaining / (double) grace;
      mult = 1.0 + (mult - 1.0) * ratio;
    }

    // 裁剪上限
    mult = Math.min(mult, JianmaiTuning.AMP_MULT_CAP);
    return mult;
  }

  /**
   * 清空所有增幅数据（卸载时使用）。
   *
   * @param player 玩家
   */
  public static void clearAll(ServerPlayer player) {
    JianmaiNBT.clearAmp(player);
  }
}
