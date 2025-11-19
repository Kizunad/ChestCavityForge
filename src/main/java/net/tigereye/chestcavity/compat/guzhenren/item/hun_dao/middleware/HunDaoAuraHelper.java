package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.middleware;

import java.util.List;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** 根据当前魂魄阈值应用简单的威慑光环效果. */
public final class HunDaoAuraHelper {

  private HunDaoAuraHelper() {}

  /**
   * 对附近实体应用威慑光环效果.
   *
   * @param source 发出光环的玩家
   * @param radius 光环半径
   * @param hunpo 触发效果的魂魄阈值
   */
  public static void applyDeterAura(Player source, double radius, double hunpo) {
    if (source == null || radius <= 0.0D) {
      return;
    }
    Level level = source.level();
    if (level.isClientSide()) {
      return;
    }
    AABB area = source.getBoundingBox().inflate(radius);
    List<LivingEntity> targets =
        level.getEntitiesOfClass(
            LivingEntity.class,
            area,
            entity ->
                entity != source
                    && entity.isAlive()
                    && !entity.isAlliedTo(source)
                    && entity.getHealth() < hunpo);
    for (LivingEntity target : targets) {
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1));
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
    }
  }
}
