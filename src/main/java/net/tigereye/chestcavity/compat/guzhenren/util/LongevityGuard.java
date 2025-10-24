package net.tigereye.chestcavity.compat.guzhenren.util;

import java.util.function.Consumer;
import net.minecraft.world.entity.player.Player;

/**
 * Compatibility shim that allows external mods to clamp longevity drain while a soul beast state is
 * active.
 */
public final class LongevityGuard {

  private LongevityGuard() {}

  public static void whileSoulBeastClamp(Player player, Runnable action) {
    if (player == null || action == null) {
      return;
    }
    action.run();
  }

  public static void whileSoulBeastClamp(Player player, Consumer<Player> consumer) {
    if (player == null || consumer == null) {
      return;
    }
    consumer.accept(player);
  }
}
