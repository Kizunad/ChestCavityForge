package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.runtime;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import net.minecraft.resources.ResourceLocation;

/** 冰雪系通用冷系效果应用与清理。 */
public final class ColdEffectOps {
  private ColdEffectOps() {}

  public static void applyColdEffect(
      LivingEntity target,
      CCConfig.GuzhenrenBingXueDaoConfig.BingJiGuConfig cfg,
      ResourceLocation iceColdEffectId) {
    if (target == null || target.level().isClientSide()) return;
    Optional<Holder.Reference<MobEffect>> holder =
        BuiltInRegistries.MOB_EFFECT.getHolder(iceColdEffectId);
    holder.ifPresent(
        effect ->
            target.addEffect(
                new MobEffectInstance(effect, cfg.iceEffectDurationTicks, 0, false, true, true)));
    target.addEffect(
        new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, cfg.iceEffectDurationTicks, 0, false, true, true));
    target.addEffect(
        new MobEffectInstance(MobEffects.DIG_SLOWDOWN, cfg.iceEffectDurationTicks, 0, false, true, true));
    int frostMarkTicks =
        ChestCavity.config != null
            ? Math.max(20, ChestCavity.config.REACTION.frostMarkDurationTicks)
            : 120;
    ReactionTagOps.add(target, ReactionTagKeys.FROST_MARK, frostMarkTicks);
  }

  public static void clearBleed(LivingEntity entity, ResourceLocation bleedEffectId) {
    if (entity == null || entity.level().isClientSide()) return;
    BuiltInRegistries.MOB_EFFECT.getHolder(bleedEffectId).ifPresent(entity::removeEffect);
  }
}

