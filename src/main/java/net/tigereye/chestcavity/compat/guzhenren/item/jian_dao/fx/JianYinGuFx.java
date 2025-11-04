package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

/**
 * 剑引蛊的轻量表现占位：负责冷却提示、音效与后续粒子入口。
 *
 * <p>当前实现仅包含占位接口，便于后续逐步填充具体表现。
 */
public final class JianYinGuFx {

  private JianYinGuFx() {}

  /** 冷却提示（客户端 toast）。 */
  public static void scheduleCooldownToast(
      ServerPlayer player, ResourceLocation abilityId, long readyAtTick, long nowTick) {
    if (player == null || abilityId == null) {
      return;
    }
    ActiveSkillRegistry.scheduleReadyToast(player, abilityId, readyAtTick, nowTick);
  }

  /** 激活音效占位：后续可挂粒子或动画。 */
  public static void playActivationFx(ServerPlayer player) {
    if (player == null) {
      return;
    }
    Vec3 pos = player.position();
    player.level().playSound(
        /* player = */ null,
        pos.x,
        pos.y,
        pos.z,
        SoundEvents.ANVIL_PLACE,
        SoundSource.PLAYERS,
        0.4f,
        1.25f);
  }

  /** 资源不足反馈占位。 */
  public static void playResourceFailureFx(ServerPlayer player) {
    if (player == null) {
      return;
    }
    Vec3 pos = player.position();
    player.level().playSound(
        /* player = */ null,
        pos.x,
        pos.y,
        pos.z,
        SoundEvents.NOTE_BLOCK_BELL,
        SoundSource.PLAYERS,
        0.5f,
        0.75f);
  }

  public static void playScanFx(ServerPlayer player, int count) {
    if (player == null) {
      return;
    }
    Vec3 pos = player.position();
    player.level().playSound(
        null,
        pos.x,
        pos.y,
        pos.z,
        SoundEvents.BEACON_POWER_SELECT,
        SoundSource.PLAYERS,
        0.6f,
        1.35f);
  }

  public static void playScanEmptyFx(ServerPlayer player) {
    if (player == null) {
      return;
    }
    Vec3 pos = player.position();
    player.level().playSound(
        null,
        pos.x,
        pos.y,
        pos.z,
        SoundEvents.UI_BUTTON_CLICK,
        SoundSource.PLAYERS,
        0.4f,
        1.2f);
  }

  public static void playCancelFx(ServerPlayer player) {
    if (player == null) {
      return;
    }
    Vec3 pos = player.position();
    player.level().playSound(
        null,
        pos.x,
        pos.y,
        pos.z,
        SoundEvents.NOTE_BLOCK_BASS,
        SoundSource.PLAYERS,
        0.5f,
        0.8f);
  }

  public static void playGuardBlockFx(
      ServerPlayer player, FlyingSwordEntity blocker, Entity attacker) {
    if (player == null) {
      return;
    }
    Vec3 origin = blocker != null ? blocker.position() : player.position();
    player.level().playSound(
        null,
        origin.x,
        origin.y,
        origin.z,
        SoundEvents.SHIELD_BLOCK,
        SoundSource.PLAYERS,
        0.7f,
        1.05f);
    if (player.level() instanceof ServerLevel server) {
      server.sendParticles(
          ParticleTypes.CRIT,
          origin.x,
          origin.y + 0.5,
          origin.z,
          6,
          0.2,
          0.2,
          0.2,
          0.1);
      if (attacker != null) {
        Vec3 atk = attacker.position();
        server.sendParticles(
            ParticleTypes.SMOKE,
            atk.x,
            atk.y + attacker.getBbHeight() * 0.5,
            atk.z,
            6,
            0.25,
            0.25,
            0.25,
            0.02);
      }
    }
  }
}
