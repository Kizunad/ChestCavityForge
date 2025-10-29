package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.tai_ji_swap.messages;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 太极错位 消息
 */
public final class TaiJiSwapMessages {
    private TaiJiSwapMessages() {}

    public static final String INSUFFICIENT_RESOURCES = "资源不足，无法施展太极错位。";
    public static final String MISSING_ANCHOR = "尚未记录另一态锚点，先使用“阴阳身”建立基点。";
    public static final String UNREACHABLE_ANCHOR = "目标锚点不可达，太极错位失败。";

    public static void sendFailure(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }
}
