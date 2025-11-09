package kizuna.guzhenren_event_ext.common.config;

import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import net.minecraft.world.level.GameRules;

public class Gamerules {

    public static GameRules.Key<GameRules.BooleanValue> RULE_EVENT_EXTENSION_ENABLED;

    public static void register() {
        RULE_EVENT_EXTENSION_ENABLED = GameRules.register(
            GuzhenrenEventExtension.MODID + ":eventExtensionEnabled",
            GameRules.Category.PLAYER,
            GameRules.BooleanValue.create(true)
        );
    }
}
