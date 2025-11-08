package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianmaiTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 剑道道痕有效值计算器。
 *
 * <p>负责计算包含流派经验加成的"有效剑道道痕"：
 * <ul>
 *   <li>读取原始道痕值（daohen_jiandao，已包含 JME 临时增幅）与流派经验（liupai_jiandao）</li>
 *   <li>计算流派经验加成：expMult = 1 + LIUPAI_EXP_K * min(L, SOFTCAP)^ALPHA</li>
 *   <li>最终有效值：effective = daohen * expMult</li>
 * </ul>
 *
 * <p>重要规则：
 * <ul>
 *   <li>JME 增幅已直接应用到原始道痕上，无需额外倍率</li>
 *   <li>使用 {@link SwordOwnerDaohenCache} 提供 20t 缓存</li>
 *   <li>在 JME 或流派经验变化时通过缓存失效保证实时性</li>
 * </ul>
 */
public final class JiandaoDaohenOps {

  private static final Logger LOGGER = LoggerFactory.getLogger(JiandaoDaohenOps.class);

  private JiandaoDaohenOps() {}

  /**
   * 计算有效剑道道痕（无缓存）。
   *
   * <p>公式：
   * <pre>
   * expMult = 1 + LIUPAI_EXP_K * min(L, LIUPAI_SOFTCAP)^LIUPAI_ALPHA
   * effective = daohen * expMult
   * </pre>
   *
   * <p>注意：daohen 已包含 JME 临时增幅（由 JianmaiAmpOps 直接调整原始值）
   *
   * @param owner 实体（玩家）
   * @param now 当前游戏刻（未使用，保留以兼容接口）
   * @return 有效剑道道痕值
   */
  public static double effectiveUncached(LivingEntity owner, long now) {
    if (!(owner instanceof ServerPlayer player)) {
      // 非玩家：只读原始道痕，无加成
      return ResourceOps.openHandle(owner)
          .map(h -> h.read("daohen_jiandao").orElse(0.0))
          .orElse(0.0);
    }

    // 读取原始道痕与流派经验
    GuzhenrenResourceBridge.ResourceHandle handle = ResourceOps.openHandle(player).orElse(null);
    if (handle == null) {
      return 0.0;
    }

    double daohen = handle.read("daohen_jiandao").orElse(0.0);
    double liupai = handle.read("liupai_jiandao").orElse(0.0);

    // 计算流派经验加成：expMult = 1 + LIUPAI_EXP_K * min(L, SOFTCAP)^ALPHA
    double cappedLiupai = Math.min(liupai, JianmaiTuning.LIUPAI_SOFTCAP);
    double expMult = 1.0 + JianmaiTuning.LIUPAI_EXP_K * Math.pow(cappedLiupai, JianmaiTuning.LIUPAI_ALPHA);

    // 最终有效值：daohen * expMult
    // 注意：daohen 已包含 JME 临时增幅
    double effective = daohen * expMult;

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[JiandaoDaohen] Player {} effective daohen: daohen={} (incl JME), liupai={}, expMult={}, effective={}",
          player.getName().getString(),
          daohen,
          liupai,
          expMult,
          effective);
    }

    return effective;
  }

  /**
   * 计算有效剑道道痕（带 20t 缓存）。
   *
   * <p>规则：
   * <ul>
   *   <li>缓存命中且 now - updatedAt < 20：返回缓存值</li>
   *   <li>缓存未命中或过期：重新计算并更新缓存</li>
   *   <li>如果 D/L 与上次一致（|Δ|≤1e-6）且 JME 倍率未变：复用上次有效值</li>
   * </ul>
   *
   * @param owner 实体（玩家）
   * @param now 当前游戏刻
   * @return 有效剑道道痕值
   */
  public static double effectiveCached(LivingEntity owner, long now) {
    if (!(owner instanceof ServerPlayer player)) {
      return effectiveUncached(owner, now);
    }

    return SwordOwnerDaohenCache.getEffective(
        player,
        now,
        () -> effectiveUncached(player, now));
  }
}
