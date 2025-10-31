package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganActivation;

/**
 * @deprecated Use {@link net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.shou_pi_gu.behavior.ShouPiGuActive} instead.
 */
@Deprecated
public enum ShouPiGuActive implements OrganActivation {
  INSTANCE;

  @Override
  public void activateAbility(ServerPlayer player, ChestCavityInstance cc) {
    net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.shou_pi_gu.behavior.ShouPiGuActive.INSTANCE.activateAbility(player, cc);
  }
}
