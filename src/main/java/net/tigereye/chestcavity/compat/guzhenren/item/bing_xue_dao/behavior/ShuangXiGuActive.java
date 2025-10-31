package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.minecraft.server.level.ServerPlayer;

public enum ShuangXiGuActive {
    INSTANCE;

    private static final ResourceLocation ABILITY_ID = ResourceLocation.fromNamespaceAndPath("guzhenren", "shuang_xi_gu_frost_breath");

    static {
        OrganActivationListeners.register(
            ABILITY_ID,
            (entity, cc) -> {
                if (entity instanceof ServerPlayer player) {
                    ShuangXiGuOrganBehavior.activateAbility(player, cc);
                }
            });
    }
}
