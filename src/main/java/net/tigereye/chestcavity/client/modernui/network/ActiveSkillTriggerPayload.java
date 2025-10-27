package net.tigereye.chestcavity.client.modernui.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.skill.ComboSkillRegistry;

public record ActiveSkillTriggerPayload(ResourceLocation skillId) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<ActiveSkillTriggerPayload> TYPE =
      new CustomPacketPayload.Type<>(ChestCavity.id("modernui/active_skill_trigger"));

  public static final StreamCodec<FriendlyByteBuf, ActiveSkillTriggerPayload> STREAM_CODEC =
      StreamCodec.of(ActiveSkillTriggerPayload::write, ActiveSkillTriggerPayload::read);

  private static void write(FriendlyByteBuf buf, ActiveSkillTriggerPayload payload) {
    buf.writeResourceLocation(payload.skillId());
  }

  private static ActiveSkillTriggerPayload read(FriendlyByteBuf buf) {
    return new ActiveSkillTriggerPayload(buf.readResourceLocation());
  }

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(ActiveSkillTriggerPayload payload, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            ChestCavity.LOGGER.warn(
                "[modernui][hotkey] Received ActiveSkillTriggerPayload without server player");
            return;
          }

          // 先尝试触发主动技能
          ActiveSkillRegistry.TriggerResult activeResult =
              ActiveSkillRegistry.trigger(serverPlayer, payload.skillId());

          // 如果不是主动技能，尝试触发组合杀招
          if (activeResult == ActiveSkillRegistry.TriggerResult.NOT_REGISTERED) {
            ComboSkillRegistry.TriggerResult comboResult =
                ComboSkillRegistry.trigger(serverPlayer, payload.skillId());

            switch (comboResult) {
              case SUCCESS -> {
                ChestCavity.LOGGER.info(
                    "[modernui][hotkey] player={} triggered combo skill {}",
                    serverPlayer.getScoreboardName(),
                    payload.skillId());
                return;
              }
              case NOT_REGISTERED -> {
                // 既不是主动技能也不是组合杀招
                ChestCavity.LOGGER.warn(
                    "[modernui][hotkey] player={} requested unregistered skill id={}",
                    serverPlayer.getScoreboardName(),
                    payload.skillId());
                return;
              }
              case NO_CHEST_CAVITY -> {
                ChestCavity.LOGGER.warn(
                    "[modernui][hotkey] player={} lacks chest cavity for combo skill {}",
                    serverPlayer.getScoreboardName(),
                    payload.skillId());
                return;
              }
              case MISSING_ORGAN -> {
                ChestCavity.LOGGER.warn(
                    "[modernui][hotkey] player={} missing required organs for combo skill {}",
                    serverPlayer.getScoreboardName(),
                    payload.skillId());
                return;
              }
              case FAILED -> {
                ChestCavity.LOGGER.warn(
                    "[modernui][hotkey] player={} combo skill {} activation failed",
                    serverPlayer.getScoreboardName(),
                    payload.skillId());
                return;
              }
            }
          }

          // 处理主动技能的触发结果
          switch (activeResult) {
            case SUCCESS -> ChestCavity.LOGGER.info(
                "[modernui][hotkey] player={} triggered skill {}",
                serverPlayer.getScoreboardName(),
                payload.skillId());
            case NOT_REGISTERED -> {} // 已在上面处理
            case NO_CHEST_CAVITY -> ChestCavity.LOGGER.warn(
                "[modernui][hotkey] player={} lacks chest cavity instance for skill {}",
                serverPlayer.getScoreboardName(),
                payload.skillId());
            case MISSING_ORGAN -> ChestCavity.LOGGER.warn(
                "[modernui][hotkey] player={} missing required organ for skill {}",
                serverPlayer.getScoreboardName(),
                payload.skillId());
            case ABILITY_NOT_REGISTERED -> ChestCavity.LOGGER.warn(
                "[modernui][hotkey] player={} triggered skill {} but no ability handler registered",
                serverPlayer.getScoreboardName(),
                payload.skillId());
          }
        });
  }
}
