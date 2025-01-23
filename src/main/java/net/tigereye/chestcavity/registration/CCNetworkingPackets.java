package net.tigereye.chestcavity.registration;


import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.util.NetworkUtil;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CCNetworkingPackets {
    public static final ResourceLocation ORGAN_DATA_PACKET_ID = new ResourceLocation(ChestCavity.MODID,"organ_data");
    public static final ResourceLocation UPDATE_PACKET_ID = new ResourceLocation(ChestCavity.MODID,"update");
    public static final ResourceLocation RECEIVED_UPDATE_PACKET_ID = new ResourceLocation(ChestCavity.MODID,"received_update");

    public static final ResourceLocation HOTKEY_PACKET_ID = new ResourceLocation(ChestCavity.MODID, "hotkey");

    public static void register() {}
}
