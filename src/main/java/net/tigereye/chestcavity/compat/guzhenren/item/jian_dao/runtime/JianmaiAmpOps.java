package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.runtime;

import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.SwordOwnerDaohenCache;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianmaiTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 剑脉蛊临时道痕增幅操作类。
 *
 * <p>负责管理剑脉效率（JME）的临时道痕增幅，采用"直接调整原始值"方案：
 * <ul>
 *   <li>基于 JME 计算道痕增量：delta = baseDaohen * ampK * JME</li>
 *   <li>直接调整 daohen_jiandao：ResourceBridge.adjust(+delta)</li>
 *   <li>记录已应用的增量用于过期回滚</li>
 *   <li>过期时自动回滚：ResourceBridge.adjust(-delta)</li>
 * </ul>
 *
 * <p>重要规则：
 * <ul>
 *   <li>增幅直接改变显示值，用户可见道痕增加</li>
 *   <li>过期必须回滚，避免永久改变道痕</li>
 *   <li>下线/重登录后继续有效，到期自动回滚</li>
 * </ul>
 */
public final class JianmaiAmpOps {

  private static final Logger LOGGER = LoggerFactory.getLogger(JianmaiAmpOps.class);

  private JianmaiAmpOps() {}

  /**
   * 基于 JME 刷新被动道痕增幅。
   *
   * <p>规则：
   * <ul>
   *   <li>读取当前基础道痕值（减去被动和主动增量）</li>
   *   <li>计算目标被动增量：delta = baseDaohen * ampK * jme</li>
   *   <li>如果与已应用被动增量不同，先回滚旧增量，再应用新增量</li>
   *   <li>刷新被动过期时间，保持主动增量不变</li>
   * </ul>
   *
   * @param player 玩家
   * @param jme 当前剑脉效率值
   * @param now 当前游戏刻
   */
  public static void refreshFromJME(ServerPlayer player, double jme, long now) {
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = ResourceOps.openHandle(player);
    if (handleOpt.isEmpty()) {
      return;
    }

    GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
    CompoundTag ampData = JianmaiNBT.readAmp(player);
    double ampK = JianmaiNBT.getAmpK(ampData);
    double oldPassiveDelta = JianmaiNBT.getPassiveDelta(ampData);
    double activeDelta = JianmaiNBT.getActiveDelta(ampData);
    long activeExpire = JianmaiNBT.getActiveExpire(ampData);
    int grace = JianmaiNBT.getGrace(ampData);

    // 读取当前道痕（包含被动和主动增幅）
    double currentDaohen = handle.read("daohen_jiandao").orElse(0.0);

    // 计算基础道痕（减去所有增量）
    double baseDaohen = currentDaohen - oldPassiveDelta - activeDelta;

    if (jme <= 0.0) {
      // JME 为 0，回滚被动增幅（保留主动）
      if (oldPassiveDelta != 0.0) {
        handle.adjustDouble("daohen_jiandao", -oldPassiveDelta, true);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "[JianmaiAmp] JME=0, rolled back passive delta {} for player {}",
              oldPassiveDelta,
              player.getName().getString());
        }
      }
      JianmaiNBT.writeAmp(player, ampK, 0.0, 0L, activeDelta, activeExpire, grace);
      SwordOwnerDaohenCache.invalidate(player);
      return;
    }

    // 计算新被动增量：delta = baseDaohen * ampK * jme
    double newPassiveDelta = baseDaohen * ampK * jme;

    // 如果增量有变化，更新道痕
    if (Math.abs(newPassiveDelta - oldPassiveDelta) > 1e-6) {
      // 先回滚旧增量，再应用新增量
      double netChange = newPassiveDelta - oldPassiveDelta;
      handle.adjustDouble("daohen_jiandao", netChange, true);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "[JianmaiAmp] Player {} JME={}, baseDaohen={}, oldPassive={}, newPassive={}, netChange={}",
            player.getName().getString(),
            jme,
            baseDaohen,
            oldPassiveDelta,
            newPassiveDelta,
            netChange);
      }
    }

    // 刷新被动过期时间（保留主动）
    long newPassiveExpire = now + JianmaiTuning.AMP_BASE_DURATION_TICKS;
    JianmaiNBT.writeAmp(player, ampK, newPassiveDelta, newPassiveExpire, activeDelta, activeExpire, grace);

    // 失效缓存
    SwordOwnerDaohenCache.invalidate(player);
  }

  /**
   * 应用主动技能道痕增幅。
   *
   * <p>规则：
   * <ul>
   *   <li>直接增加指定的道痕增量</li>
   *   <li>记录主动增量和过期时间</li>
   *   <li>与被动增幅独立，可以叠加</li>
   * </ul>
   *
   * @param player 玩家
   * @param deltaAmount 道痕增量（直接值，不基于倍率计算）
   * @param duration 持续时间（ticks）
   * @param now 当前游戏刻
   */
  public static void applyActiveBuff(ServerPlayer player, double deltaAmount, int duration, long now) {
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = ResourceOps.openHandle(player);
    if (handleOpt.isEmpty()) {
      return;
    }

    GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
    CompoundTag ampData = JianmaiNBT.readAmp(player);
    double ampK = JianmaiNBT.getAmpK(ampData);
    double passiveDelta = JianmaiNBT.getPassiveDelta(ampData);
    long passiveExpire = JianmaiNBT.getPassiveExpire(ampData);
    double oldActiveDelta = JianmaiNBT.getActiveDelta(ampData);
    int grace = JianmaiNBT.getGrace(ampData);

    // 先回滚旧的主动增幅
    if (oldActiveDelta != 0.0) {
      handle.adjustDouble("daohen_jiandao", -oldActiveDelta, true);
    }

    // 应用新的主动增幅
    handle.adjustDouble("daohen_jiandao", deltaAmount, true);

    // 设置主动过期时间
    long newActiveExpire = now + duration;

    // 写回数据（保留被动）
    JianmaiNBT.writeAmp(player, ampK, passiveDelta, passiveExpire, deltaAmount, newActiveExpire, grace);

    // 失效缓存
    SwordOwnerDaohenCache.invalidate(player);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[JianmaiAmp] Applied active buff for player {}: oldActive={}, newActive={}, duration={}s",
          player.getName().getString(),
          oldActiveDelta,
          deltaAmount,
          duration / 20.0);
    }
  }

  /**
   * 检查并回滚过期的道痕增幅（被动和主动分别检查）。
   *
   * <p>规则：
   * <ul>
   *   <li>如果被动过期，回滚被动增量</li>
   *   <li>如果主动过期，回滚主动增量</li>
   *   <li>如果都过期，清空增幅数据</li>
   * </ul>
   *
   * @param player 玩家
   * @param now 当前游戏刻
   * @return 是否执行了回滚
   */
  public static boolean rollbackIfExpired(ServerPlayer player, long now) {
    CompoundTag ampData = JianmaiNBT.readAmp(player);
    if (ampData.isEmpty()) {
      return false;
    }

    double passiveDelta = JianmaiNBT.getPassiveDelta(ampData);
    long passiveExpire = JianmaiNBT.getPassiveExpire(ampData);
    double activeDelta = JianmaiNBT.getActiveDelta(ampData);
    long activeExpire = JianmaiNBT.getActiveExpire(ampData);

    boolean passiveExpired = now >= passiveExpire && passiveDelta != 0.0;
    boolean activeExpired = now >= activeExpire && activeDelta != 0.0;

    if (!passiveExpired && !activeExpired) {
      return false; // 都未过期
    }

    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = ResourceOps.openHandle(player);
    if (handleOpt.isEmpty()) {
      return false;
    }

    GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
    boolean rolled = false;

    // 回滚被动
    if (passiveExpired) {
      handle.adjustDouble("daohen_jiandao", -passiveDelta, true);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "[JianmaiAmp] Passive expired! Rolled back {} for player {}",
            passiveDelta,
            player.getName().getString());
      }
      passiveDelta = 0.0;
      passiveExpire = 0L;
      rolled = true;
    }

    // 回滚主动
    if (activeExpired) {
      handle.adjustDouble("daohen_jiandao", -activeDelta, true);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "[JianmaiAmp] Active expired! Rolled back {} for player {}",
            activeDelta,
            player.getName().getString());
      }
      activeDelta = 0.0;
      activeExpire = 0L;
      rolled = true;
    }

    // 如果都过期了，清空数据
    if (passiveDelta == 0.0 && activeDelta == 0.0) {
      JianmaiNBT.clearAmp(player);
    } else {
      // 否则更新数据
      double ampK = JianmaiNBT.getAmpK(ampData);
      int grace = JianmaiNBT.getGrace(ampData);
      JianmaiNBT.writeAmp(player, ampK, passiveDelta, passiveExpire, activeDelta, activeExpire, grace);
    }

    // 失效缓存
    if (rolled) {
      SwordOwnerDaohenCache.invalidate(player);
    }

    return rolled;
  }

  /**
   * 清空所有增幅数据（卸载时使用）。
   *
   * <p>先回滚被动和主动增幅，再清空数据。
   *
   * @param player 玩家
   */
  public static void clearAll(ServerPlayer player) {
    CompoundTag ampData = JianmaiNBT.readAmp(player);
    if (ampData.isEmpty()) {
      return;
    }

    double passiveDelta = JianmaiNBT.getPassiveDelta(ampData);
    double activeDelta = JianmaiNBT.getActiveDelta(ampData);
    double totalDelta = passiveDelta + activeDelta;

    // 回滚所有增幅
    if (totalDelta != 0.0) {
      Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = ResourceOps.openHandle(player);
      if (handleOpt.isPresent()) {
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
        handle.adjustDouble("daohen_jiandao", -totalDelta, true);

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "[JianmaiAmp] Cleared! Rolled back passive={}, active={}, total={} for player {}",
              passiveDelta,
              activeDelta,
              totalDelta,
              player.getName().getString());
        }
      }
    }

    // 清空数据
    JianmaiNBT.clearAmp(player);

    // 失效缓存
    SwordOwnerDaohenCache.invalidate(player);
  }

  /**
   * 清理当前的主动增幅（登出/断线时调用）。
   *
   * <p>避免玩家在主动技持续期间退出游戏后保留永久道痕。
   *
   * @param player 玩家
   */
  public static void clearActive(ServerPlayer player) {
    CompoundTag ampData = JianmaiNBT.readAmp(player);
    if (ampData.isEmpty()) {
      return;
    }

    double activeDelta = JianmaiNBT.getActiveDelta(ampData);
    if (activeDelta == 0.0) {
      return;
    }

    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = ResourceOps.openHandle(player);
    if (handleOpt.isEmpty()) {
      return;
    }

    GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
    handle.adjustDouble("daohen_jiandao", -activeDelta, true);

    double ampK = JianmaiNBT.getAmpK(ampData);
    double passiveDelta = JianmaiNBT.getPassiveDelta(ampData);
    long passiveExpire = JianmaiNBT.getPassiveExpire(ampData);
    int grace = JianmaiNBT.getGrace(ampData);

    JianmaiNBT.writeAmp(player, ampK, passiveDelta, passiveExpire, 0.0, 0L, grace);
    SwordOwnerDaohenCache.invalidate(player);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[JianmaiAmp] Cleared active delta {} for player {} on logout",
          activeDelta,
          player.getName().getString());
    }
  }

  /**
   * 清理当前的被动增幅（登出/断线时调用）。
   *
   * @param player 玩家
   */
  public static void clearPassive(ServerPlayer player) {
    CompoundTag ampData = JianmaiNBT.readAmp(player);
    if (ampData.isEmpty()) {
      return;
    }

    double passiveDelta = JianmaiNBT.getPassiveDelta(ampData);
    if (passiveDelta == 0.0) {
      return;
    }

    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = ResourceOps.openHandle(player);
    if (handleOpt.isEmpty()) {
      return;
    }

    GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
    handle.adjustDouble("daohen_jiandao", -passiveDelta, true);

    double ampK = JianmaiNBT.getAmpK(ampData);
    double activeDelta = JianmaiNBT.getActiveDelta(ampData);
    long activeExpire = JianmaiNBT.getActiveExpire(ampData);
    int grace = JianmaiNBT.getGrace(ampData);

    JianmaiNBT.writeAmp(player, ampK, 0.0, 0L, activeDelta, activeExpire, grace);
    SwordOwnerDaohenCache.invalidate(player);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[JianmaiAmp] Cleared passive delta {} for player {} on logout",
          passiveDelta,
          player.getName().getString());
    }
  }
}
