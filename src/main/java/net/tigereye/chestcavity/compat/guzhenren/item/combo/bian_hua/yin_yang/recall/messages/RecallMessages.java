package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.yin_yang.recall.messages;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 归位 消息
 */
public final class RecallMessages {
    private RecallMessages() {}

    public static final String INSUFFICIENT_RESOURCES = "资源不足，无法归位。";
    public static final String MISSING_ANCHOR = "另一态未设置锚点，无法归位。";
    public static final String UNREACHABLE_ANCHOR = "锚点不可达，归位失败。";

    public static void sendFailure(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }
}
