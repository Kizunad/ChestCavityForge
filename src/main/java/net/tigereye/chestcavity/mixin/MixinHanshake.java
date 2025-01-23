package net.tigereye.chestcavity.mixin;

import net.minecraft.network.Connection;
import net.minecraftforge.network.HandshakeHandler;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.tigereye.chestcavity.chestcavities.organs.OrganManager;
import net.tigereye.chestcavity.network.NetworkHandler;
import net.tigereye.chestcavity.network.packets.OrganDataPacket;
import net.tigereye.chestcavity.util.OrganDataPacketHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(NetworkHooks.class)
//@Mixin(HandshakeHandler.class)
public class MixinHanshake {
    @Inject(at = @At("TAIL"), method = "sendMCRegistryPackets", remap = false)
    private static void sendServerPackets(Connection manager, String direction, CallbackInfo ci) {
        if(direction.equals("PLAY_TO_CLIENT")) {
            ArrayList<Connection> managers = new ArrayList<>();
            managers.add(manager);
            int count = OrganManager.GeneratedOrganData.size();
            ArrayList<OrganDataPacketHelper> helpers = new ArrayList<>();
            OrganManager.GeneratedOrganData.forEach((id, data) -> helpers.add(new OrganDataPacketHelper(id, data.pseudoOrgan, data.organScores.size(), data.organScores)));
            //System.out.println("BOONELDAN TEST PACKET SENT"); //if boolean something == boolean somethingelse xor
            NetworkHandler.CHANNEL.send(PacketDistributor.NMLIST.with(() -> managers), new OrganDataPacket(count, helpers));
        }
    }

    //@Inject(at = @At("TAIL"), method = "<init>")
    //public void sendServerPackets(Connection networkManager, NetworkDirection side, CallbackInfo ci) {
    //    if(side == NetworkDirection.LOGIN_TO_CLIENT) {
    //        int count = OrganManager.GeneratedOrganData.size();
    //        ArrayList<OrganDataPacketHelper> helpers = new ArrayList<>();
    //        OrganManager.GeneratedOrganData.forEach((id, data) -> helpers.add(new OrganDataPacketHelper(id, data.pseudoOrgan, data.organScores.size(), data.organScores)));
    //        NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new OrganDataPacket(count, helpers));
    //    }
    //}
}