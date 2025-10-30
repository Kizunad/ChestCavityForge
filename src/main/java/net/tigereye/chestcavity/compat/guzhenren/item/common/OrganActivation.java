package net.tigereye.chestcavity.compat.guzhenren.item.common;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

public interface OrganActivation {
  void activateAbility(ServerPlayer player, ChestCavityInstance cc);
}
