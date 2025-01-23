package net.tigereye.chestcavity.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ServerLoginPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.chestcavities.organs.OrganData;
import net.tigereye.chestcavity.chestcavities.organs.OrganManager;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.network.NetworkHandler;
import net.tigereye.chestcavity.network.packets.ChestCavityHotkeyPacket;
import net.tigereye.chestcavity.network.packets.ChestCavityUpdatePacket;
import net.tigereye.chestcavity.registration.CCNetworkingPackets;

import java.util.HashMap;
import java.util.Map;

public class NetworkUtil {
    //S2C = SERVER TO CLIENT //I think

    public static boolean SendS2CChestCavityUpdatePacket(ChestCavityInstance cc){
        cc.updateInstantiated = true;
        if((!cc.owner.level.isClientSide()) && cc.owner instanceof ServerPlayer spe) {
            if(spe.connection == null) {
                return false;
            }
            Map<ResourceLocation, Float> organScores = cc.getOrganScores();
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> spe), new ChestCavityUpdatePacket(cc.opened, organScores.size(), organScores));
            return true;
        }
        return false;
    }
}
