package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.runtime;

import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.middleware.HunDaoMiddleware;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

/**
 * Temporary adapter that implements the Hun Dao operation interfaces by delegating to the existing
 * HunDaoMiddleware and ResourceOps.
 *
 * <p>This adapter serves as a transitional layer during Phase 1 of the rearchitecture, allowing
 * behavior classes to depend on interfaces rather than the concrete middleware implementation.
 *
 * <p>In future phases, this adapter may be replaced by dedicated implementations for each interface.
 */
public final class HunDaoOpsAdapter
    implements HunDaoResourceOps, HunDaoFxOps, HunDaoNotificationOps {

  public static final HunDaoOpsAdapter INSTANCE = new HunDaoOpsAdapter();

  private HunDaoOpsAdapter() {}

  // HunDaoResourceOps implementation

  @Override
  public void leakHunpoPerSecond(Player player, double amount) {
    HunDaoMiddleware.INSTANCE.leakHunpoPerSecond(player, amount);
  }

  @Override
  public boolean consumeHunpo(Player player, double amount) {
    return HunDaoMiddleware.INSTANCE.consumeHunpo(player, amount);
  }

  @Override
  public Optional<GuzhenrenResourceBridge.ResourceHandle> openHandle(Player player) {
    return ResourceOps.openHandle(player);
  }

  @Override
  public double readHunpo(Player player) {
    return openHandle(player).flatMap(h -> h.read("hunpo")).orElse(0.0);
  }

  @Override
  public double readMaxHunpo(Player player) {
    return openHandle(player).flatMap(h -> h.read("zuida_hunpo")).orElse(0.0);
  }

  @Override
  public OptionalDouble adjustDouble(
      Player player, String field, double amount, boolean clamp, String maxField) {
    return ResourceOps.tryAdjustDouble(player, field, amount, clamp, maxField);
  }

  // HunDaoFxOps implementation

  @Override
  public void applySoulFlame(Player source, LivingEntity target, double perSecondDamage, int seconds) {
    HunDaoMiddleware.INSTANCE.applySoulFlame(source, target, perSecondDamage, seconds);
  }

  // HunDaoNotificationOps implementation

  @Override
  public void handlePlayer(Player player) {
    HunDaoMiddleware.INSTANCE.handlerPlayer(player);
  }

  @Override
  public void handleNonPlayer(LivingEntity entity) {
    HunDaoMiddleware.INSTANCE.handlerNonPlayer(entity);
  }
}
