package net.tigereye.chestcavity.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.organs.OrganData;
import net.tigereye.chestcavity.chestcavities.organs.OrganManager;
import net.tigereye.chestcavity.compat.guzhenren.item.kongqiao.behavior.DaoHenSeedHandler;
import net.tigereye.chestcavity.guzhenren.network.packets.KongqiaoDaoHenSeedPayload;
import net.tigereye.chestcavity.network.packets.ChestCavityHotkeyPayload;
import net.tigereye.chestcavity.network.packets.ChestCavityOrganSlotUpdatePayload;
import net.tigereye.chestcavity.network.packets.ChestCavityUpdatePayload;
import net.tigereye.chestcavity.network.packets.OrganDataPayload;
import net.tigereye.chestcavity.guscript.network.packets.GuScriptBindingTogglePayload;
import net.tigereye.chestcavity.guscript.network.packets.GuScriptOpenPayload;

public final class NetworkHandler {

    private NetworkHandler() {}

    public static void registerCommon(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ChestCavityHotkeyPayload.TYPE, ChestCavityHotkeyPayload.STREAM_CODEC, ChestCavityHotkeyPayload::handle);
        registrar.playToServer(GuScriptOpenPayload.TYPE, GuScriptOpenPayload.STREAM_CODEC, GuScriptOpenPayload::handle);
        registrar.playToServer(GuScriptBindingTogglePayload.TYPE, GuScriptBindingTogglePayload.STREAM_CODEC, GuScriptBindingTogglePayload::handle);
        registrar.playToServer(KongqiaoDaoHenSeedPayload.TYPE, KongqiaoDaoHenSeedPayload.STREAM_CODEC, DaoHenSeedHandler::handleSeedPayload);
        registrar.playToClient(ChestCavityUpdatePayload.TYPE, ChestCavityUpdatePayload.STREAM_CODEC, ChestCavityUpdatePayload::handle);
        registrar.playToClient(OrganDataPayload.TYPE, OrganDataPayload.STREAM_CODEC, OrganDataPayload::handle);
        registrar.playToClient(ChestCavityOrganSlotUpdatePayload.TYPE, ChestCavityOrganSlotUpdatePayload.STREAM_CODEC, ChestCavityOrganSlotUpdatePayload::handle);
    }


    public static void sendChestCavityUpdate(ServerPlayer player, ChestCavityUpdatePayload payload) {
        player.connection.send(payload);
    }

    public static void sendOrganData(ServerPlayer player) {
        List<OrganDataPayload.Entry> entries = new ArrayList<>();
        for (Map.Entry<ResourceLocation, OrganData> entry : OrganManager.GeneratedOrganData.entrySet()) {
            entries.add(new OrganDataPayload.Entry(entry.getKey(), entry.getValue().pseudoOrgan, entry.getValue().organScores));
        }
        player.connection.send(new OrganDataPayload(entries));
    }

    public static void sendOrganSlotUpdate(ServerPlayer player, ChestCavityOrganSlotUpdatePayload payload) {
        player.connection.send(payload);
    }
}
