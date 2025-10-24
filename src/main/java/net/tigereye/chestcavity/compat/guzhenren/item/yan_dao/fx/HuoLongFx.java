package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.fx;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.ability.AbilityFxDispatcher;

/**
 * 火龙蛊专用 FX 触发器。
 *
 * <p>全部在服务端调用，通过 AbilityFxDispatcher 广播到客户端。
 */
public final class HuoLongFx {

  private HuoLongFx() {}

  private static final ResourceLocation BREATH_CAST = ChestCavity.id("huo_long_gu/breath_cast");
  private static final ResourceLocation BREATH_IMPACT = ChestCavity.id("huo_long_gu/breath_impact");
  private static final ResourceLocation ASCEND_BURST = ChestCavity.id("huo_long_gu/ascend_burst");
  private static final ResourceLocation ASCEND_LOOP = ChestCavity.id("huo_long_gu/ascend_loop");
  private static final ResourceLocation DIVE_START = ChestCavity.id("huo_long_gu/dive_start");
  private static final ResourceLocation DIVE_TRAIL = ChestCavity.id("huo_long_gu/dive_trail");
  private static final ResourceLocation DIVE_IMPACT = ChestCavity.id("huo_long_gu/dive_impact");
  private static final ResourceLocation FORTUNE_AURA = ChestCavity.id("huo_long_gu/fortune_aura");
  private static final ResourceLocation SCALE_GUARD = ChestCavity.id("huo_long_gu/scale_guard");
  private static final ResourceLocation BLOOD_STREAM = ChestCavity.id("huo_long_gu/blood_stream");
  private static final ResourceLocation DRAGONFLAME_MARK =
      ChestCavity.id("huo_long_gu/dragonflame_mark");
  private static final ResourceLocation REACTION_OIL =
      ChestCavity.id("huo_long_gu/reaction_oil_burst");
  private static final ResourceLocation REACTION_FIRE_COAT =
      ChestCavity.id("huo_long_gu/reaction_fire_baptism");

  public static final ResourceLocation DRAGONFLAME_DOT =
      ChestCavity.id("huo_long_gu/dragonflame_dot");

  public static void playBreathCast(Player player) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 look = player.getLookAngle();
    Vec3 origin = player.getEyePosition().add(look.scale(0.6D));
    AbilityFxDispatcher.play(
        level, BREATH_CAST, origin, look, look, toServerPlayer(player), null, 1.0F);
  }

  public static void playBreathImpact(LivingEntity target, int stacks) {
    if (!(target.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 origin = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
    float intensity = 0.8F + 0.1F * Math.max(0, Math.min(stacks, 6));
    AbilityFxDispatcher.play(
        level, BREATH_IMPACT, origin, Vec3.ZERO, Vec3.ZERO, null, target, intensity);
  }

  public static void playAscendBurst(Player player) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 origin = player.position().add(0.0D, 0.2D, 0.0D);
    Vec3 look = player.getLookAngle();
    AbilityFxDispatcher.play(
        level, ASCEND_BURST, origin, look, look, toServerPlayer(player), player, 1.0F);
  }

  public static void playAscendLoop(ServerLevel level, Player player) {
    if (level == null || player == null) {
      return;
    }
    Vec3 origin = player.position().add(0.0D, 0.2D, 0.0D);
    Vec3 look = player.getLookAngle();
    AbilityFxDispatcher.play(
        level, ASCEND_LOOP, origin, look, look, toServerPlayer(player), player, 1.0F);
  }

  public static void playDiveStart(Player player) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 origin = player.position().add(0.0D, 0.1D, 0.0D);
    Vec3 dir = player.getLookAngle();
    AbilityFxDispatcher.play(
        level, DIVE_START, origin, dir, dir, toServerPlayer(player), player, 1.0F);
  }

  public static void playDiveTrail(ServerLevel level, Player player, Vec3 velocity) {
    if (level == null || player == null) {
      return;
    }
    Vec3 direction = velocity.lengthSqr() > 1.0E-4D ? velocity.normalize() : player.getLookAngle();
    Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
    AbilityFxDispatcher.play(
        level, DIVE_TRAIL, origin, direction, direction, toServerPlayer(player), player, 1.0F);
  }

  public static void playDiveImpact(
      ServerLevel level, Player player, int hits, float bonusMultiplier) {
    if (level == null || player == null) {
      return;
    }
    float intensity = 0.9F + Math.min(0.6F, bonusMultiplier);
    Vec3 origin = player.position();
    AbilityFxDispatcher.play(
        level,
        DIVE_IMPACT,
        origin,
        player.getLookAngle(),
        player.getLookAngle(),
        toServerPlayer(player),
        player,
        Math.max(0.8F, intensity));
  }

  public static void playFortuneAura(ServerLevel level, Player player) {
    if (level == null || player == null) {
      return;
    }
    Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.6D, 0.0D);
    AbilityFxDispatcher.play(
        level,
        FORTUNE_AURA,
        origin,
        player.getLookAngle(),
        player.getLookAngle(),
        toServerPlayer(player),
        player,
        1.0F);
  }

  public static void playScaleGuard(ServerLevel level, Player player) {
    if (level == null || player == null) {
      return;
    }
    Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.6D, 0.0D);
    AbilityFxDispatcher.play(
        level,
        SCALE_GUARD,
        origin,
        player.getLookAngle(),
        player.getLookAngle(),
        toServerPlayer(player),
        player,
        1.0F);
  }

  public static void playBloodStream(
      ServerLevel level, Player attacker, LivingEntity victim, float intensity) {
    if (level == null || attacker == null || victim == null) {
      return;
    }
    Vec3 origin = victim.position().add(0.0D, victim.getBbHeight() * 0.6D, 0.0D);
    AbilityFxDispatcher.play(
        level,
        BLOOD_STREAM,
        origin,
        victim.getLookAngle(),
        attacker.getLookAngle(),
        toServerPlayer(attacker),
        attacker,
        Math.max(0.6F, intensity));
  }

  public static void playDragonflameMark(LivingEntity target, int stacks) {
    if (!(target.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 origin = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
    float intensity = 0.6F + 0.1F * Math.max(0, Math.min(stacks, 6));
    AbilityFxDispatcher.play(
        level, DRAGONFLAME_MARK, origin, Vec3.ZERO, Vec3.ZERO, null, target, intensity);
  }

  public static void playReactionOil(LivingEntity target, int stacks) {
    if (!(target.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 origin = target.position().add(0.0D, 0.2D, 0.0D);
    float intensity = 0.8F + 0.1F * Math.max(1, stacks);
    AbilityFxDispatcher.play(
        level, REACTION_OIL, origin, Vec3.ZERO, Vec3.ZERO, null, target, intensity);
  }

  public static void playReactionFireCoat(LivingEntity target, LivingEntity attacker, int stacks) {
    if (!(target.level() instanceof ServerLevel level)) {
      return;
    }
    Vec3 origin = target.position().add(0.0D, 0.2D, 0.0D);
    ServerPlayer performer = attacker instanceof Player player ? toServerPlayer(player) : null;
    float intensity = 0.9F + 0.05F * Math.max(0, stacks);
    AbilityFxDispatcher.play(
        level,
        REACTION_FIRE_COAT,
        origin,
        target.getLookAngle(),
        target.getLookAngle(),
        performer,
        target,
        intensity);
  }

  private static ServerPlayer toServerPlayer(Player player) {
    return player instanceof ServerPlayer sp ? sp : null;
  }
}
