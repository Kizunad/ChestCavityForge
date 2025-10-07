package net.tigereye.chestcavity.soul.engine;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Guards destructive soul feature migrations. Only explicit enablement should flip the toggle.
 */
public final class SoulFeatureToggle {

    // 默认启用魂体生态：无需先执行 /soul enable
    private static final AtomicBoolean SOUL_SYSTEM_ENABLED = new AtomicBoolean(true);

    private SoulFeatureToggle() {
    }

    public static boolean isEnabled() {
        return SOUL_SYSTEM_ENABLED.get();
    }

    public static void enable(ServerPlayer player) {
        if (SOUL_SYSTEM_ENABLED.compareAndSet(false, true)) {
            player.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                    "[soul] 魂体生态已启用。警告：已执行 NBT 迁移，旧存档若未备份可能被破坏！")));
        } else {
            player.sendSystemMessage(Component.literal("[soul] 魂体生态已处于启用状态。"));
        }
    }
}
 
