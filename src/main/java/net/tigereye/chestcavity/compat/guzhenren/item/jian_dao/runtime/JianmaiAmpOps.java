package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.runtime;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.SwordOwnerDaohenCache;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianmaiTuning;

/**
 * 剑脉蛊临时倍率操作类。
 *
 * <p>负责管理剑脉效率（JME）的临时增幅倍率，包括：
 * <ul>
 *   <li>基于 JME 的被动增幅（刷新过期时间，宽限期衰减）</li>
 *   <li>主动技能券（独立倍率，延长持续时间）</li>
 *   <li>最终倍率计算：multPassive * multActive（裁剪上限）</li>
 * </ul>
 *
 * <p>重要规则：
 * <ul>
 *   <li>临时增幅仅影响"读取期"的道痕值，严禁写回永久值</li>
 *   <li>被动和主动倍率分离存储，避免相互覆盖</li>
 *   <li>过期判断：now >= expireGameTime → 清空对应节点</li>
 *   <li>宽限期：倍率在 graceTicks 内线性插值回 1.0</li>
 * </ul>
 */
public final class JianmaiAmpOps {

  private JianmaiAmpOps() {}

  /**
   * 基于 JME 刷新被动增幅倍率。
   *
   * <p>规则：
   * <ul>
   *   <li>当 jme > 0 时，刷新被动过期时间：expireGameTime = max(expire, now + AMP_BASE_DURATION_TICKS)</li>
   *   <li>计算被动倍率：multPassive = 1 + DAO_JME_K * jme</li>
   *   <li>在宽限期内线性插值：multPassive = lerp_to_1(base, grace)</li>
   *   <li>保留主动倍率不变</li>
   * </ul>
   *
   * @param player 玩家
   * @param jme 当前剑脉效率值
   * @param now 当前游戏刻
   */
  public static void refreshFromJME(ServerPlayer player, double jme, long now) {
    CompoundTag ampData = JianmaiNBT.readAmp(player);
    double ampK = JianmaiNBT.getAmpK(ampData);
    long passiveExpire = JianmaiNBT.getExpire(ampData);
    int grace = JianmaiNBT.getGrace(ampData);

    // 读取主动倍率（保留不变）
    double multActive = JianmaiNBT.getMultActive(ampData);
    long activeExpire = JianmaiNBT.getActiveExpire(ampData);

    if (jme <= 0.0) {
      // JME 为 0，被动倍率重置为 1.0
      JianmaiNBT.writeAmp(player, ampK, 1.0, multActive, 0L, activeExpire, grace);
      return;
    }

    // 刷新被动过期时间（取更长）
    long newPassiveExpire = Math.max(passiveExpire, now + JianmaiTuning.AMP_BASE_DURATION_TICKS);

    // 计算被动倍率
    double baseMult = 1.0 + ampK * jme;

    // 宽限期插值（如果在宽限期内）
    double multPassive = baseMult;
    if (now < newPassiveExpire && now >= newPassiveExpire - grace) {
      // 在宽限期内：线性插值回 1.0
      long remaining = newPassiveExpire - now;
      double ratio = (double) remaining / (double) grace;
      multPassive = 1.0 + (baseMult - 1.0) * ratio;
    }

    // 写回（保留主动倍率）
    JianmaiNBT.writeAmp(player, ampK, multPassive, multActive, newPassiveExpire, activeExpire, grace);

    // 失效缓存（JME 倍率变化）
    SwordOwnerDaohenCache.invalidate(player);
  }

  /**
   * 应用主动技能增幅券（独立倍率）。
   *
   * <p>规则：
   * <ul>
   *   <li>叠乘主动倍率：multActive *= activeMult</li>
   *   <li>裁剪上限：multActive = min(multActive, AMP_MULT_CAP)</li>
   *   <li>刷新主动过期时间：activeExpireGameTime = max(activeExpire, now + durationTicks)</li>
   *   <li>保留被动倍率不变</li>
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
    int grace = JianmaiNBT.getGrace(ampData);

    // 读取被动倍率（保留不变）
    double multPassive = JianmaiNBT.getMultPassive(ampData);
    long passiveExpire = JianmaiNBT.getExpire(ampData);

    // 读取当前主动倍率并叠乘
    double currentMultActive = JianmaiNBT.getMultActive(ampData);
    long activeExpire = JianmaiNBT.getActiveExpire(ampData);

    // 叠乘并裁剪主动倍率
    double newMultActive = currentMultActive * activeMult;
    newMultActive = Math.min(newMultActive, JianmaiTuning.AMP_MULT_CAP);

    // 刷新主动过期时间（取更长）
    long newActiveExpire = Math.max(activeExpire, now + durationTicks);

    // 写回（保留被动倍率）
    JianmaiNBT.writeAmp(player, ampK, multPassive, newMultActive, passiveExpire, newActiveExpire, grace);

    // 失效缓存（主动券倍率变化）
    SwordOwnerDaohenCache.invalidate(player);
  }

  /**
   * 获取最终倍率（供"有效道痕"读取使用）。
   *
   * <p>规则：
   * <ul>
   *   <li>分别检查被动和主动过期：now >= expireGameTime → 对应倍率重置为 1.0</li>
   *   <li>被动宽限期：线性插值回 1.0</li>
   *   <li>最终倍率 = multPassive * multActive</li>
   *   <li>裁剪上限：min(finalMult, AMP_MULT_CAP)</li>
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

    double multPassive = JianmaiNBT.getMultPassive(ampData);
    double multActive = JianmaiNBT.getMultActive(ampData);
    long passiveExpire = JianmaiNBT.getExpire(ampData);
    long activeExpire = JianmaiNBT.getActiveExpire(ampData);
    int grace = JianmaiNBT.getGrace(ampData);

    // 检查被动过期
    if (now >= passiveExpire) {
      multPassive = 1.0;
    } else if (now >= passiveExpire - grace) {
      // 被动宽限期插值
      long remaining = passiveExpire - now;
      double ratio = (double) remaining / (double) grace;
      multPassive = 1.0 + (multPassive - 1.0) * ratio;
    }

    // 检查主动过期
    if (now >= activeExpire) {
      multActive = 1.0;
    }

    // 最终倍率 = 被动 * 主动
    double finalMult = multPassive * multActive;

    // 裁剪上限
    finalMult = Math.min(finalMult, JianmaiTuning.AMP_MULT_CAP);
    return finalMult;
  }

  /**
   * 清空所有增幅数据（卸载时使用）。
   *
   * @param player 玩家
   */
  public static void clearAll(ServerPlayer player) {
    JianmaiNBT.clearAmp(player);

    // 失效缓存（倍率清空）
    SwordOwnerDaohenCache.invalidate(player);
  }
}
