package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.JianYingGuOrganBehavior;

/** Tick dispatcher so delayed afterimage effects can be executed server-side. */
public final class JianYingGuEvents {

  private JianYingGuEvents() {}

  @SubscribeEvent
  public static void onServerTick(ServerTickEvent.Post event) {
    for (ServerLevel level : event.getServer().getAllLevels()) {
      JianYingGuOrganBehavior.INSTANCE.tickLevel(level);
    }
  }
}
