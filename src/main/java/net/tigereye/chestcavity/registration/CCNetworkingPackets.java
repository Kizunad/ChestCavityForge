package net.tigereye.chestcavity.registration;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;

public class CCNetworkingPackets {
    public static final ResourceLocation ORGAN_DATA_PACKET_ID = ChestCavity.id("organ_data");
    public static final ResourceLocation UPDATE_PACKET_ID = ChestCavity.id("update");
    public static final ResourceLocation RECEIVED_UPDATE_PACKET_ID = ChestCavity.id("received_update");
    public static final ResourceLocation HOTKEY_PACKET_ID = ChestCavity.id("hotkey");

    public static void register() {}
}
