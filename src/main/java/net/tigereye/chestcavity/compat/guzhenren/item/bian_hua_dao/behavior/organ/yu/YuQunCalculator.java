package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ.yu;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning.YuQunTuning;

public final class YuQunCalculator {
  private YuQunCalculator() {}

  public static void performVolley(
      Level level, LivingEntity caster, boolean upgraded, Vec3 origin, Vec3 direction) {
    double maxRange =
        upgraded ? YuQunTuning.RANGE + YuQunTuning.UPGRADE_RANGE_BONUS : YuQunTuning.RANGE;
    AABB searchBox = caster.getBoundingBox().inflate(maxRange, 2.0, maxRange);
    List<LivingEntity> candidates =
        level.getEntitiesOfClass(
            LivingEntity.class,
            searchBox,
            target ->
                target != caster
                    && target.isAlive()
                    && !target.isAlliedTo(caster)
                    && !target.isInvulnerable());
    Optional<Holder.Reference<net.minecraft.world.effect.MobEffect>> bleedEffect =
        BuiltInRegistries.MOB_EFFECT.getHolder(YuQunTuning.BLEED_EFFECT_ID);
    for (LivingEntity target : candidates) {
      Vec3 toTarget = target.getEyePosition().subtract(origin);
      double forward = toTarget.dot(direction);
      if (forward <= 0.0 || forward > maxRange) {
        continue;
      }
      Vec3 lateral = toTarget.subtract(direction.scale(forward));
      if (lateral.lengthSqr() > YuQunTuning.WIDTH * YuQunTuning.WIDTH) {
        continue;
      }
      double pushStrength =
          upgraded ? YuQunTuning.PUSH_STRENGTH_UPGRADE : YuQunTuning.PUSH_STRENGTH_BASE;
      double upward =
          YuQunTuning.PUSH_UPWARD_BASE + (upgraded ? YuQunTuning.PUSH_UPWARD_UPGRADE_BONUS : 0.0);
      target.push(direction.x * pushStrength, upward, direction.z * pushStrength);
      target.hurtMarked = true;
      target.addEffect(
          new MobEffectInstance(
              MobEffects.MOVEMENT_SLOWDOWN,
              YuQunTuning.SLOW_TICKS,
              upgraded ? YuQunTuning.SLOW_AMP_UPGRADE : YuQunTuning.SLOW_AMP_BASE));
      bleedEffect.ifPresent(
          holder ->
              target.addEffect(
                  new MobEffectInstance(
                      holder,
                      upgraded ? YuQunTuning.BLEED_TICKS_UPGRADE : YuQunTuning.BLEED_TICKS_BASE,
                      upgraded ? YuQunTuning.BLEED_AMP_UPGRADE : YuQunTuning.BLEED_AMP_BASE)));
    }
    level.playSound(
        null, caster.blockPosition(), SoundEvents.SALMON_FLOP, SoundSource.PLAYERS, 0.8f, 1.0f);
  }
}
