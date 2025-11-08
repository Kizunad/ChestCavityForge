package net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.context;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordAttributes;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JiandaoDaohenOps;

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

    @Nullable net.minecraft.world.entity.LivingEntity owner = sword.getOwner();
    if (owner != null) {
      ctx.ownerSprinting = owner.isSprinting();
      double hp = Math.max(0.0, owner.getHealth());
      double maxHp = Math.max(1.0, owner.getMaxHealth());
      ctx.ownerHpPercent = hp / maxHp;

      // 玩家：读取有效剑道道痕（含流派经验+JME）、原始流派经验（用于耐久减免）
      if (owner instanceof Player) {
        // 使用有效道痕（含 JME 和流派经验加成）
        if (owner instanceof ServerPlayer serverPlayer) {
          ctx.ownerJianDaoScar =
              JiandaoDaohenOps.effectiveCached(serverPlayer, ctx.worldTime);
        } else {
          // 客户端玩家：fallback 到原始道痕
          net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge
              .open(owner)
              .ifPresent(
                  handle -> {
                    handle.read("daohen_jiandao").ifPresent(value -> ctx.ownerJianDaoScar = value);
                  });
        }

        // 流派经验仍用于耐久减免（原始值）
        net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge
            .open(owner)
            .ifPresent(
                handle -> {
                  handle.read("liupai_jiandao").ifPresent(value -> ctx.ownerSwordPathExp = value);
                });
      } else {
        // 非玩家：设置默认流派经验（提供耐久减免），道痕保持 0（无加成）
        ctx.ownerSwordPathExp =
            net.tigereye.chestcavity.compat.guzhenren.flyingsword.tuning
                .FlyingSwordTuning.NON_PLAYER_DEFAULT_SWORD_PATH_EXP;
      }
    }
    // 其他如 ownerJianDaoScar、ownerSwordPathExp 等，由外部钩子补全
    //
    // 注意：JME（剑脉效率）通过 ownerJianDaoScar 间接影响飞剑性能，
    // 不需要再单独注册 JME Hook 避免双重加成。
    // JME 对玩家属性的增益仍由 JianmaiPlayerTickEvents 单独应用。

    return ctx;
  }
}
