package net.tigereye.chestcavity.config;

import net.minecraft.world.level.GameRules;

public class CCGameRules {

    public static final GameRules.Key<GameRules.BooleanValue> SPAWN_FUN_ENTITIES =
            GameRules.register("spawnFunEntities", GameRules.Category.SPAWNING, createBooleanRule(true));

    private static GameRules.Type<GameRules.BooleanValue> createBooleanRule(boolean defaultValue) {
        return new GameRules.Type<>(
                () -> com.mojang.brigadier.arguments.BoolArgumentType.bool(),
                type -> new GameRules.BooleanValue(type, defaultValue),
                (server, value) -> {},
                (visitor, key, type, consumer) -> {} // Simplified visitor, should be fine for a boolean
        );
    }

    public static void register() {
        // Static fields are initialized when the class is loaded.
        // This method is called to ensure the class is loaded and the gamerules are registered.
    }
}
