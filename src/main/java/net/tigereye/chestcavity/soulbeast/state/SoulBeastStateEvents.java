package net.tigereye.chestcavity.soulbeast.state;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * @deprecated 迁移至 {@link
 *     net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateEvents}
 */
@Deprecated(forRemoval = true)
public final class SoulBeastStateEvents {

  private SoulBeastStateEvents() {}

  @SubscribeEvent
  public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
    net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateEvents
        .onPlayerLogin(event);
  }

  @SubscribeEvent
  public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
    net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateEvents
        .onPlayerRespawn(event);
  }

  @SubscribeEvent
  public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
    net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateEvents
        .onDimensionChange(event);
  }

  @SubscribeEvent
  public static void onClone(PlayerEvent.Clone event) {
    net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateEvents
        .onClone(event);
  }

  @SubscribeEvent
  public static void onServerTick(ServerTickEvent.Post event) {
    net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateEvents
        .onServerTick(event);
  }

  @OnlyIn(Dist.CLIENT)
  @SubscribeEvent
  public static void onClientUnload(LevelEvent.Unload event) {
    net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateEvents
        .onClientUnload(event);
  }
}
