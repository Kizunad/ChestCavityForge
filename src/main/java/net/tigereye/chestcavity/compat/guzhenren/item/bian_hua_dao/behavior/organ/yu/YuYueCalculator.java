package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ.yu;

import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning.YuYueTuning;

public final class YuYueCalculator {
  private YuYueCalculator() {}

  public static double computeBaseRange(boolean inWater, boolean tailSynergy, boolean upgraded) {
    double base = inWater ? YuYueTuning.RANGE_WATER : YuYueTuning.RANGE_MOIST;
    if (tailSynergy) {
      base += inWater ? YuYueTuning.TAIL_BONUS_WATER : YuYueTuning.TAIL_BONUS_DRY;
    }
    if (upgraded) {
      base += YuYueTuning.UPGRADE_BONUS_RANGE;
    }
    return base;
  }

  public static double computeHorizontalScale(double baseRange) {
    return baseRange * YuYueTuning.HORIZONTAL_SCALE;
  }

  public static double computeVertical(boolean inWater) {
    return inWater ? YuYueTuning.VERTICAL_IN_WATER : YuYueTuning.VERTICAL_OUT_OF_WATER;
  }

  public static void grantAuxiliaryBuffs(Player player, boolean tailSynergy, boolean upgraded) {
    if (tailSynergy) {
      player.addEffect(
          new MobEffectInstance(
              MobEffects.SLOW_FALLING, YuYueTuning.SLOW_FALL_TICKS, 0, false, false));
    }
    if (upgraded) {
      player.addEffect(
          new MobEffectInstance(
              MobEffects.DAMAGE_RESISTANCE, YuYueTuning.UPGRADE_RESIST_TICKS, 0, false, false));
    }
  }

  public static void pushCollisions(Player player, Vec3 direction, double range) {
    Level level = player.level();
    Vec3 start = player.position();
    AABB swept = player.getBoundingBox().expandTowards(direction.scale(range)).inflate(1.0);
    List<LivingEntity> targets =
        level.getEntitiesOfClass(
            LivingEntity.class,
            swept,
            candidate ->
                candidate != player && candidate.isAlive() && !candidate.isAlliedTo(player));
    double strength = 0.35 + (player.isInWaterOrBubble() ? 0.2 : 0.0);
    for (LivingEntity target : targets) {
      target.push(direction.x * strength, 0.25, direction.z * strength);
      target.hurtMarked = true;
    }
    level.playSound(
        null, player.blockPosition(), SoundEvents.DOLPHIN_JUMP, SoundSource.PLAYERS, 0.9f, 1.1f);
  }
}
