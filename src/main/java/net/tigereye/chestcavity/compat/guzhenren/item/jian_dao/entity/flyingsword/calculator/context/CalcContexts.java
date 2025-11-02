package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator.context;

import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordAttributes;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;

/**
 * 上下文构造辅助：从运行时对象提取必要数据，避免在计算器中直接依赖 MC 类。
 */
public final class CalcContexts {
  private CalcContexts() {}

  public static CalcContext from(FlyingSwordEntity sword) {
    CalcContext ctx = new CalcContext();
    ctx.aiMode = sword.getAIMode();
    ctx.currentSpeed = sword.getDeltaMovement().length();
    FlyingSwordAttributes attrs = sword.getSwordAttributes();
    ctx.baseSpeed = attrs.speedBase;
    ctx.maxSpeed = attrs.speedMax;
    ctx.baseDamage = attrs.damageBase;
    ctx.swordLevel = sword.getSwordLevel();
    ctx.worldTime = sword.level().getGameTime();

    @Nullable Player owner = sword.getOwner();
    if (owner != null) {
      ctx.ownerSprinting = owner.isSprinting();
      double hp = Math.max(0.0, owner.getHealth());
      double maxHp = Math.max(1.0, owner.getMaxHealth());
      ctx.ownerHpPercent = hp / maxHp;

      // 玩家：读取剑道道痕、剑道流派经验
      if (owner instanceof Player) {
        net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge
            .open(owner)
            .ifPresent(
                handle -> {
                  handle.read("daohen_jiandao").ifPresent(value -> ctx.ownerJianDaoScar = value);
                  handle.read("liupai_jiandao").ifPresent(value -> ctx.ownerSwordPathExp = value);
                });
      } else {
        // 非玩家：设置默认流派经验（提供耐久减免），道痕保持 0（无加成）
        ctx.ownerSwordPathExp =
            net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.tuning
                .FlyingSwordTuning.NON_PLAYER_DEFAULT_SWORD_PATH_EXP;
      }
    }
    // 其他如 ownerJianDaoScar、ownerSwordPathExp 等，由外部钩子补全

    return ctx;
  }
}
