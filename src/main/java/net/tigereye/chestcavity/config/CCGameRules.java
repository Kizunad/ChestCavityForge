package net.tigereye.chestcavity.config;

import net.minecraft.world.level.GameRules;

public class CCGameRules {

  public static final GameRules.Key<GameRules.BooleanValue> SPAWN_FUN_ENTITIES =
      GameRules.register("spawnFunEntities", GameRules.Category.SPAWNING, createBooleanRule(true));

  private static GameRules.Type<GameRules.BooleanValue> createBooleanRule(boolean defaultValue) {
    return GameRules.BooleanValue.create(defaultValue);
  }

  public static void register() {
    // Static fields are initialized when the class is loaded.
    // This method is called to ensure the class is loaded and the gamerules are registered.
  }
}
