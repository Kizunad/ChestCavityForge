package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.dual_strike.messages;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 两界同击 消息
 */
public final class DualStrikeMessages {
    private DualStrikeMessages() {}

    public static final String INSUFFICIENT_RESOURCES = "资源不足，无法施展两界同击。";
    public static final String WINDOW_OPEN = "两界同击窗口开启 5 秒";

    public static void sendFailure(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }

    public static void sendAction(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }
}
