package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.runtime;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator.BreathParamOps.BreathParams;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator.ConeFilter;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.fx.ShuangXiFx;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;

/** 执行霜息：筛选目标、附加冷效果与霜痕、按百分比DoT、播放FX。 */
public final class FrostBreathRuntime {
  private FrostBreathRuntime() {}

  public static void execute(
      ServerPlayer player,
      CCConfig.GuzhenrenBingXueDaoConfig.ShuangXiGuConfig cfg,
      ResourceLocation iceColdEffectId,
      BreathParams params,
      double daohen) {
    if (player == null || player.level().isClientSide()) return;
    ServerLevel server = player.serverLevel();

    Vec3 origin = player.getEyePosition();
    Vec3 look = player.getLookAngle().normalize();
    double range = params.range();
    AABB search = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5);
    List<LivingEntity> candidates =
        server.getEntitiesOfClass(
            LivingEntity.class,
            search,
            target -> target != player && target.isAlive() && !target.isAlliedTo(player));
    List<LivingEntity> affected = new ArrayList<>();
    for (LivingEntity target : candidates) {
      Vec3 toTarget = target.getEyePosition().subtract(origin);
      double distance = toTarget.length();
      if (distance <= 0.0001D || distance > range) continue;
      if (!ConeFilter.matches(look.x, look.y, look.z, toTarget.x, toTarget.y, toTarget.z, params.coneDotThreshold())) continue;
      affected.add(target);
      // 冷效果 + 霜痕
      applyColdEffect(target, cfg, iceColdEffectId);
      // 霜蚀DoT：百分比按道痕放大已在 percent 内完成
      if (player.getRandom().nextDouble() < params.frostbiteChance()) {
        net.tigereye.chestcavity.compat.guzhenren.item.common.DamageOverTimeHelper.applyBaseAttackPercentDoT(
            player,
            target,
            params.frostbitePercent(),
            params.frostbiteDurationSeconds(),
            net.minecraft.sounds.SoundEvents.GLASS_BREAK,
            0.55f,
            1.25f,
            net.tigereye.chestcavity.util.DoTTypes.SHUANG_XI_FROSTBITE);
      }
    }

    // FX
    Entity focus = affected.isEmpty() ? player : affected.get(0);
    ShuangXiFx.spawnBreathParticles(server, origin, look, focus, params.particleSteps(), params.particleSpacing());
    ShuangXiFx.playBreathSound(server, player, !affected.isEmpty());
  }

  private static void applyColdEffect(
      LivingEntity target, CCConfig.GuzhenrenBingXueDaoConfig.ShuangXiGuConfig config, ResourceLocation iceColdEffectId) {
    ServerLevel level = (ServerLevel) target.level();
    if (level.isClientSide()) return;
    java.util.Optional<Holder.Reference<MobEffect>> holder = BuiltInRegistries.MOB_EFFECT.getHolder(iceColdEffectId);
    holder.ifPresent(
        effect -> target.addEffect(new MobEffectInstance(effect, Math.max(0, config.coldDurationTicks), 0, false, true, true)));
    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.max(0, config.coldDurationTicks), 0, false, true, true));
    target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, Math.max(0, config.coldDurationTicks), 0, false, true, true));
    int frostMarkTicks =
        ChestCavity.config != null
            ? Math.max(20, ChestCavity.config.REACTION.frostMarkDurationTicks)
            : 120;
    ReactionTagOps.add(target, ReactionTagKeys.FROST_MARK, frostMarkTicks);
  }
}

