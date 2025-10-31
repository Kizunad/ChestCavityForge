package net.tigereye.chestcavity.compat.guzhenren.item.du_dao.messages;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.compat.guzhenren.item.du_dao.tuning.ChouPiTuning;

public final class ChouPiMessages {
  private ChouPiMessages() {}

  public static void notify(ServerLevel level, LivingEntity entity, RandomSource random) {
    if (!(entity instanceof Player player)) {
      return;
    }
    if (ChouPiTuning.MESSAGE_SELF_ENABLED) {
      Component selfMessage =
          random.nextBoolean()
              ? Component.translatable("message.guzhenren.chou_pi_gu.uncomfortable")
              : Component.translatable("message.guzhenren.chou_pi_gu.stench");
      player.sendSystemMessage(selfMessage);
    }
    if (ChouPiTuning.MESSAGE_BROADCAST_ENABLED) {
      Component broadcast =
          Component.translatable(
              "message.guzhenren.chou_pi_gu.odor_broadcast", player.getDisplayName());
      for (ServerPlayer other : level.players()) {
        if (other == player) {
          continue;
        }
        if (other.distanceTo(player) > 32.0f) {
          continue;
        }
        other.sendSystemMessage(broadcast);
      }
    }
  }
}

