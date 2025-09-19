package net.tigereye.chestcavity.util;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.network.NetworkHandler;
import net.tigereye.chestcavity.network.packets.ChestCavityUpdatePayload;

import java.util.HashMap;
import java.util.Map;

public class NetworkUtil {
    //S2C = SERVER TO CLIENT //I think

    public static boolean SendS2CChestCavityUpdatePacket(ChestCavityInstance cc){
        cc.updateInstantiated = true;
        if((!cc.owner.level().isClientSide()) && cc.owner instanceof ServerPlayer player) {
            if(player.connection == null) {
                return false;
            }
            Map<net.minecraft.resources.ResourceLocation, Float> organScores = new HashMap<>(cc.getOrganScores());
            NetworkHandler.sendChestCavityUpdate(player, new ChestCavityUpdatePayload(cc.opened, organScores));
            return true;
        }
        return false;
    }
}
