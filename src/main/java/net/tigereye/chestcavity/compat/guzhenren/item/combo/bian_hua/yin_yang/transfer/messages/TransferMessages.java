package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.transfer.messages;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 阴阳互渡 消息
 */
public final class TransferMessages {
    private TransferMessages() {}

    public static final String INSUFFICIENT_RESOURCES = "资源不足，无法施展阴阳互渡。";
    public static final String CANNOT_READ_ATTACHMENT = "无法读取真元附件，互渡失败。";
    public static final String INSUFFICIENT_TRANSFER_AMOUNT = "资源不足以完成 30% 互渡。";
    public static final String TRANSFER_COMPLETE = "阴阳互渡完成";

    public static void sendFailure(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }

    public static void sendAction(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }
}
