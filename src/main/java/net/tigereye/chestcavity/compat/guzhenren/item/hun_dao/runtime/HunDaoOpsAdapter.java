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
 * <p>In future phases, this adapter may be replaced by dedicated implementations for each
 * interface.
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
    Optional<GuzhenrenResourceBridge.ResourceHandle> handle = openHandle(player);
    if (handle.isEmpty()) {
      return 0.0;
    }
    return handle.get().read("hunpo").orElse(0.0);
  }

  @Override
  public double readMaxHunpo(Player player) {
    Optional<GuzhenrenResourceBridge.ResourceHandle> handle = openHandle(player);
    if (handle.isEmpty()) {
      return 0.0;
    }
    return handle.get().read("zuida_hunpo").orElse(0.0);
  }

  @Override
  public double readDouble(Player player, String field) {
    Optional<GuzhenrenResourceBridge.ResourceHandle> handle = openHandle(player);
    if (handle.isEmpty()) {
      return 0.0;
    }
    return handle.get().read(field).orElse(0.0);
  }

  @Override
  public OptionalDouble adjustDouble(
      Player player, String field, double amount, boolean clamp, String maxField) {
    return ResourceOps.tryAdjustDouble(player, field, amount, clamp, maxField);
  }

  // HunDaoFxOps implementation

  @Override
  public void applySoulFlame(
      Player source, LivingEntity target, double perSecondDamage, int seconds) {
    HunDaoFxOpsImpl.INSTANCE.applySoulFlame(source, target, perSecondDamage, seconds);
  }

  @Override
  public void playSoulBeastActivate(Player player) {
    HunDaoFxOpsImpl.INSTANCE.playSoulBeastActivate(player);
  }

  @Override
  public void playSoulBeastDeactivate(Player player) {
    HunDaoFxOpsImpl.INSTANCE.playSoulBeastDeactivate(player);
  }

  @Override
  public void playSoulBeastHit(Player player, LivingEntity target) {
    HunDaoFxOpsImpl.INSTANCE.playSoulBeastHit(player, target);
  }

  @Override
  public void playGuiWuActivate(
      Player player, net.minecraft.world.phys.Vec3 position, double radius) {
    HunDaoFxOpsImpl.INSTANCE.playGuiWuActivate(player, position, radius);
  }

  @Override
  public void playGuiWuDissipate(Player player, net.minecraft.world.phys.Vec3 position) {
    HunDaoFxOpsImpl.INSTANCE.playGuiWuDissipate(player, position);
  }

  @Override
  public void playHunPoLeakWarning(Player player) {
    HunDaoFxOpsImpl.INSTANCE.playHunPoLeakWarning(player);
  }

  @Override
  public void playHunPoRecovery(Player player, double amount) {
    HunDaoFxOpsImpl.INSTANCE.playHunPoRecovery(player, amount);
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
