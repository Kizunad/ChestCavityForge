package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.domain;

import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.compat.guzhenren.domain.DomainHelper;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.JianXinDomain;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning.JianXinDomainTuning;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.context.CalcContext;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.context.CalcOutputs;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.hooks.FlyingSwordCalcHook;

/** 剑心域伤害修正钩子：友方伤害+50%，敌方伤害-50% */
public final class SwordDomainDamageHook implements FlyingSwordCalcHook {

  private static final SwordDomainDamageHook INSTANCE = new SwordDomainDamageHook();

  private SwordDomainDamageHook() {}

  public static SwordDomainDamageHook getInstance() {
    return INSTANCE;
  }

  @Override
  public void apply(CalcContext ctx, CalcOutputs out) {
    if (ctx == null || out == null) return;

    // 确保飞剑实体可用
    if (!(ctx.caster instanceof FlyingSwordEntity flyingSword)) {
      return;
    }

    // 客户端不计算
    if (flyingSword.level().isClientSide()) {
      return;
    }

    // 获取最高级领域
    var highestDomain = DomainHelper.getHighestLevelDomainAt(flyingSword);
    if (!(highestDomain instanceof JianXinDomain jianXinDomain)) {
      return;
    }

    // 获取飞剑主人
    var swordOwner = flyingSword.getOwner();
    if (swordOwner == null) return;

    // 统一效果乘积缩放
    double effectScale = 1.0;
    if (jianXinDomain.getOwner() instanceof Player ownerPlayer) {
      effectScale =
          net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.state.DomainConfigOps
              .effectScale(ownerPlayer);
    }

    // 友方：伤害 +50%
    if (jianXinDomain.isFriendly(swordOwner)) {
      double buff = JianXinDomainTuning.FRIENDLY_SWORD_DAMAGE_BUFF;
      buff *= effectScale;
      if (jianXinDomain.isEnhanced()) {
        buff *= JianXinDomainTuning.ENHANCED_DEBUFF_MULT; // 强化状态翻倍
      }
      out.damageMult *= (1.0 + Math.max(0.0, buff));
    }
    // 敌方：伤害 -50%
    else {
      double debuff = JianXinDomainTuning.ENEMY_SWORD_DAMAGE_DEBUFF;
      debuff *= effectScale;
      if (jianXinDomain.isEnhanced()) {
        debuff *= JianXinDomainTuning.ENHANCED_DEBUFF_MULT; // 强化状态翻倍
      }
      double damageMult = Math.max(0.05, 1.0 - Math.max(0.0, debuff));
      out.damageMult *= damageMult;
    }
  }
}
