package net.tigereye.chestcavity.soulbeast.command;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.event.SoulBeastStateChangedEvent;

/**
 * @deprecated 迁移至 {@link
 *     net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.command.SoulBeastCommands}
 */
@Deprecated(forRemoval = true)
public final class SoulBeastCommands {

  private SoulBeastCommands() {}

  public static void register(RegisterCommandsEvent event) {
    net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.command.SoulBeastCommands
        .register(event);
  }

  @SubscribeEvent
  public static void onSoulBeastStateChanged(SoulBeastStateChangedEvent event) {
    net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.command.SoulBeastCommands
        .onSoulBeastStateChanged(event);
  }
}
