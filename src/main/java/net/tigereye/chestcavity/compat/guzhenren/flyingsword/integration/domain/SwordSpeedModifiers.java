package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.domain;

import java.util.Optional;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.compat.guzhenren.domain.Domain;
import net.tigereye.chestcavity.compat.guzhenren.domain.DomainHelper;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.JianXinDomain;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

/** 统一管理飞剑速度类修正（域/道痕/控制效果）。 */
public final class SwordSpeedModifiers {

  private SwordSpeedModifiers() {}

  /**
   * 计算域内速度缩放（剑心域）。
   *
   * <p>当前实现：若飞剑处于某个剑心域内且与该域主人不为友，应用缩放系数 scale = (enemyDaohen / ownerDaohen) 并裁剪到 [0.25, 1.0]。
   *
   * <p>后续可扩展：考虑域等级、增强状态、额外减速上限等。
   */
  public static double computeDomainSpeedScale(FlyingSwordEntity sword) {
    if (sword == null || sword.level().isClientSide()) {
      return 1.0;
    }
    net.minecraft.world.entity.LivingEntity swordOwner = sword.getOwner();
    if (swordOwner == null) {
      return 1.0;
    }

    Domain highest = DomainHelper.getHighestLevelDomainAt(sword);
    if (!(highest instanceof JianXinDomain jianXin)) {
      return 1.0;
    }

    // 友方：给予加速（随道痕/经验略增），强化状态下进一步提升
    if (jianXin.isFriendly(swordOwner)) {
      double ownerDaohen = readJiandaoDaohen(jianXin.getOwner());
      double ownerSchool =
          net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps.openHandle(
                  jianXin.getOwner())
              .map(h -> h.read("liupai_jiandao").orElse(0.0))
              .orElse(0.0);
      double intensity =
          ownerDaohen
                  * net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning
                      .JianXinDomainTuning.DAOHEN_INTENSITY_COEF
              + ownerSchool
                  * net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning
                      .JianXinDomainTuning.SCHOOL_EXP_INTENSITY_COEF;
      double baseBoost =
          net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning.JianXinDomainTuning
              .FRIENDLY_SWORD_SPEED_BOOST_BASE;
      double maxBoost =
          net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning.JianXinDomainTuning
              .FRIENDLY_SWORD_SPEED_BOOST_MAX;
      double boost = Math.min(maxBoost, baseBoost * (1.0 + intensity));
      // 统一效果乘积缩放（剑域蛊/配置可调整）
      double effectScale = 1.0;
      if (jianXin.getOwner() instanceof Player ownerP) {
        effectScale =
            net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.state.DomainConfigOps
                .effectScale(ownerP);
      }
      boost *= effectScale;
      if (jianXin.isEnhanced()) {
        boost *=
            net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning.JianXinDomainTuning
                .FRIENDLY_SWORD_BOOST_ENHANCED_MULT;
      }
      return 1.0 + Math.max(0.0, boost);
    }

    // 敌方：减速，按道痕比缩放
    double ownerDaohen = readJiandaoDaohen(jianXin.getOwner());
    double enemyDaohen = readJiandaoDaohen(swordOwner);
    if (!(ownerDaohen > 0.0)) {
      return 1.0; // 域主道痕未知/为0，不做缩放
    }
    double ratio = enemyDaohen / ownerDaohen;
    if (!Double.isFinite(ratio)) {
      return 1.0;
    }
    // 保守裁剪：避免过慢或加速
    double baseScale = Math.max(0.25, Math.min(1.0, ratio));
    // 统一效果乘积缩放：越大→越抑制（向极限下限拉近），并裁剪到 [0.05,1.0]
    double effectScale = 1.0;
    if (jianXin.getOwner() instanceof Player ownerP) {
      effectScale =
          net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.state.DomainConfigOps
              .effectScale(ownerP);
    }
    double scale = 1.0 - (1.0 - baseScale) * Math.max(0.0, effectScale);
    scale = Math.max(0.05, Math.min(1.0, scale));
    return scale;
  }

  private static double readJiandaoDaohen(net.minecraft.world.entity.LivingEntity entity) {
    if (entity == null) return 0.0;
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = ResourceOps.openHandle(entity);
    if (handleOpt.isEmpty()) return 0.0;
    return handleOpt.get().read("daohen_jiandao").orElse(0.0);
  }
}
