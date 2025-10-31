package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yu_lin_gu.behavior;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganActivation;

public enum YuLinGuActive implements OrganActivation {
    INSTANCE;

    @Override
    public void activateAbility(ServerPlayer player, ChestCavityInstance cc) {
        // TODO: Move logic from old active class here
    }
}
