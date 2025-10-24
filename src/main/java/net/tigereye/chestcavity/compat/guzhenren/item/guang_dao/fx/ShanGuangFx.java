package net.tigereye.chestcavity.compat.guzhenren.item.guang_dao.fx;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.guscript.ability.AbilityFxDispatcher;

/** Shared FX entry points for 闪光蛊 effects. Keeps behaviour classes focused on logic. */
public final class ShanGuangFx {

  private ShanGuangFx() {}

  public static final ResourceLocation FLASH_DODGE =
      ResourceLocation.parse("chestcavity:shan_guang_flash_dodge");
  public static final ResourceLocation FLASH_TRAIL_PLACE =
      ResourceLocation.parse("chestcavity:shan_guang_trail_place");
  public static final ResourceLocation FLASH_TRAIL_JUMP =
      ResourceLocation.parse("chestcavity:shan_guang_trail_jump");
  public static final ResourceLocation FLASH_BURST =
      ResourceLocation.parse("chestcavity:shan_guang_flash_burst");

  public static void playDodge(ServerLevel level, LivingEntity entity) {
    if (level == null || entity == null) {
      return;
    }
    AbilityFxDispatcher.play(
        level,
        FLASH_DODGE,
        entity.position(),
        entity.getLookAngle(),
        entity.getLookAngle(),
        entity instanceof ServerPlayer serverPlayer ? serverPlayer : null,
        entity,
        1.0F);
  }

  public static void playTrailPlacement(ServerLevel level, Vec3 origin) {
    if (level == null || origin == null) {
      return;
    }
    AbilityFxDispatcher.play(
        level, FLASH_TRAIL_PLACE, origin, Vec3.ZERO, Vec3.ZERO, null, null, 1.0F);
  }

  public static void playTrailJump(
      ServerLevel level, Vec3 origin, Vec3 direction, @Nullable LivingEntity performer) {
    if (level == null || origin == null) {
      return;
    }
    Vec3 dir = direction == null ? Vec3.ZERO : direction;
    AbilityFxDispatcher.play(
        level,
        FLASH_TRAIL_JUMP,
        origin,
        dir,
        dir,
        performer instanceof ServerPlayer serverPlayer ? serverPlayer : null,
        performer,
        1.0F);
  }

  public static void playBurst(LivingEntity performer) {
    if (performer == null) {
      return;
    }
    if (performer instanceof ServerPlayer serverPlayer) {
      AbilityFxDispatcher.play(serverPlayer, FLASH_BURST, Vec3.ZERO, 1.0F);
      return;
    }
    if (performer.level() instanceof ServerLevel level) {
      Vec3 origin = performer.position().add(0.0D, performer.getBbHeight() * 0.5D, 0.0D);
      Vec3 look = performer.getLookAngle();
      AbilityFxDispatcher.play(level, FLASH_BURST, origin, look, look, null, performer, 1.0F);
    }
  }
}
