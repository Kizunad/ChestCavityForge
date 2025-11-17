package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.runtime;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Interface for hun-dao visual/audio effect operations.
 *
 * <p>Decouples behavior classes from direct middleware dependencies by providing an abstraction
 * for special effects like soul flame DoT, soul beast transformation, and gui wu fog.
 *
 * <p>Phase 5: Expanded to support data-driven FX dispatch via HunDaoFxRouter.
 */
public interface HunDaoFxOps {

  /**
   * Apply soul flame DoT effect to a target.
   *
   * @param source the player applying the effect
   * @param target the target entity
   * @param perSecondDamage damage per second
   * @param seconds duration in seconds
   */
  void applySoulFlame(Player source, LivingEntity target, double perSecondDamage, int seconds);

  /**
   * Play soul beast activation effect.
   *
   * @param player the player transforming into soul beast form
   */
  void playSoulBeastActivate(Player player);

  /**
   * Play soul beast deactivation effect.
   *
   * @param player the player exiting soul beast form
   */
  void playSoulBeastDeactivate(Player player);

  /**
   * Play soul beast melee hit impact effect.
   *
   * @param player the player in soul beast form
   * @param target the target entity being hit
   */
  void playSoulBeastHit(Player player, LivingEntity target);

  /**
   * Play gui wu (鬼雾) fog activation effect.
   *
   * @param player the player activating gui wu
   * @param position the center position of the fog
   * @param radius the fog radius
   */
  void playGuiWuActivate(Player player, Vec3 position, double radius);

  /**
   * Play gui wu fog dissipation effect.
   *
   * @param player the player whose gui wu is expiring
   * @param position the center position of the fog
   */
  void playGuiWuDissipate(Player player, Vec3 position);

  /**
   * Play hun po leak warning effect.
   *
   * @param player the player whose hun po is critically low
   */
  void playHunPoLeakWarning(Player player);

  /**
   * Play hun po recovery effect.
   *
   * @param player the player recovering hun po
   * @param amount the amount recovered
   */
  void playHunPoRecovery(Player player, double amount);
}
