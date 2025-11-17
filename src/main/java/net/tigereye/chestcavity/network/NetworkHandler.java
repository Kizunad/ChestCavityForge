package net.tigereye.chestcavity.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.tigereye.chestcavity.chestcavities.organs.OrganData;
import net.tigereye.chestcavity.chestcavities.organs.OrganManager;
import net.tigereye.chestcavity.client.modernui.config.network.PlayerPreferenceRequestPayload;
import net.tigereye.chestcavity.client.modernui.config.network.PlayerPreferenceSyncPayload;
import net.tigereye.chestcavity.client.modernui.config.network.PlayerPreferenceUpdatePayload;
import net.tigereye.chestcavity.client.modernui.config.network.SoulConfigActivatePayload;
import net.tigereye.chestcavity.client.modernui.config.network.SoulConfigForceTeleportPayload;
import net.tigereye.chestcavity.client.modernui.config.network.SoulConfigRenamePayload;
import net.tigereye.chestcavity.client.modernui.config.network.SoulConfigRequestPayload;
import net.tigereye.chestcavity.client.modernui.config.network.SoulConfigSetOrderPayload;
import net.tigereye.chestcavity.client.modernui.config.network.SoulConfigSetVacuumPayload;
import net.tigereye.chestcavity.client.modernui.config.network.SoulConfigSyncPayload;
import net.tigereye.chestcavity.client.modernui.container.network.TestModernUIContainerRequestPayload;
import net.tigereye.chestcavity.client.modernui.network.ActiveSkillTriggerPayload;
import net.tigereye.chestcavity.client.modernui.network.SkillHotbarSnapshotPayload;
import net.tigereye.chestcavity.client.modernui.network.SkillHotbarUpdatePayload;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.network.HunDaoNetworkInit;
import net.tigereye.chestcavity.compat.guzhenren.item.kongqiao.behavior.DaoHenSeedHandler;
import net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastRequestSyncPayload;
import net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastStateManager;
import net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.state.SoulBeastSyncPayload;
import net.tigereye.chestcavity.compat.guzhenren.network.packets.TiePiProgressPayload;
import net.tigereye.chestcavity.guscript.network.packets.FlowInputPayload;
import net.tigereye.chestcavity.guscript.network.packets.FlowSyncPayload;
import net.tigereye.chestcavity.guscript.network.packets.FxEventPayload;
import net.tigereye.chestcavity.guscript.network.packets.GeckoFxEventPayload;
import net.tigereye.chestcavity.guscript.network.packets.GuScriptBindingTogglePayload;
import net.tigereye.chestcavity.guscript.network.packets.GuScriptOpenPayload;
import net.tigereye.chestcavity.guscript.network.packets.GuScriptPageChangePayload;
import net.tigereye.chestcavity.guscript.network.packets.GuScriptSimulateCompilePayload;
import net.tigereye.chestcavity.guscript.network.packets.GuScriptTriggerPayload;
import net.tigereye.chestcavity.guzhenren.network.packets.KongqiaoDaoHenSeedPayload;
import net.tigereye.chestcavity.guzhenren.network.packets.PlayerSkinUploadPayload;
import net.tigereye.chestcavity.network.packets.ChestCavityHotkeyPayload;
import net.tigereye.chestcavity.network.packets.ChestCavityOrganSlotUpdatePayload;
import net.tigereye.chestcavity.network.packets.ChestCavityUpdatePayload;
import net.tigereye.chestcavity.network.packets.OrganDataPayload;

public final class NetworkHandler {

  private NetworkHandler() {}

  // 防重注册：部分环境下 RegisterPayloadHandlersEvent 可能被多次触发
  private static boolean FLYINGSWORD_TUI_PAYLOADS_REGISTERED = false;

  public static void registerCommon(RegisterPayloadHandlersEvent event) {
    PayloadRegistrar registrar = event.registrar("1");
    registrar.playToServer(
        ChestCavityHotkeyPayload.TYPE,
        ChestCavityHotkeyPayload.STREAM_CODEC,
        ChestCavityHotkeyPayload::handle);
    registrar.playToServer(
        GuScriptOpenPayload.TYPE, GuScriptOpenPayload.STREAM_CODEC, GuScriptOpenPayload::handle);
    registrar.playToServer(
        GuScriptBindingTogglePayload.TYPE,
        GuScriptBindingTogglePayload.STREAM_CODEC,
        GuScriptBindingTogglePayload::handle);
    registrar.playToServer(
        GuScriptPageChangePayload.TYPE,
        GuScriptPageChangePayload.STREAM_CODEC,
        GuScriptPageChangePayload::handle);
    registrar.playToServer(
        GuScriptTriggerPayload.TYPE,
        GuScriptTriggerPayload.STREAM_CODEC,
        GuScriptTriggerPayload::handle);
    registrar.playToServer(
        GuScriptSimulateCompilePayload.TYPE,
        GuScriptSimulateCompilePayload.STREAM_CODEC,
        GuScriptSimulateCompilePayload::handle);
    registrar.playToServer(
        FlowInputPayload.TYPE, FlowInputPayload.STREAM_CODEC, FlowInputPayload::handle);
    registrar.playToServer(
        KongqiaoDaoHenSeedPayload.TYPE,
        KongqiaoDaoHenSeedPayload.STREAM_CODEC,
        DaoHenSeedHandler::handleSeedPayload);
    registrar.playToServer(
        PlayerSkinUploadPayload.TYPE,
        PlayerSkinUploadPayload.STREAM_CODEC,
        PlayerSkinUploadPayload::handle);
    registrar.playToServer(
        SoulBeastRequestSyncPayload.TYPE,
        SoulBeastRequestSyncPayload.STREAM_CODEC,
        SoulBeastStateManager::handleRequestSyncPayload);
    registrar.playToServer(
        TestModernUIContainerRequestPayload.TYPE,
        TestModernUIContainerRequestPayload.STREAM_CODEC,
        TestModernUIContainerRequestPayload::handle);
    if (!FLYINGSWORD_TUI_PAYLOADS_REGISTERED) {
      registrar.playToServer(
          net.tigereye.chestcavity.client.modernui.network.FlyingSwordWithdrawPayload.TYPE,
          net.tigereye.chestcavity.client.modernui.network.FlyingSwordWithdrawPayload.STREAM_CODEC,
          net.tigereye.chestcavity.client.modernui.network.FlyingSwordWithdrawPayload::handle);
      registrar.playToServer(
          net.tigereye.chestcavity.client.modernui.network.FlyingSwordDepositPayload.TYPE,
          net.tigereye.chestcavity.client.modernui.network.FlyingSwordDepositPayload.STREAM_CODEC,
          net.tigereye.chestcavity.client.modernui.network.FlyingSwordDepositPayload::handle);
      FLYINGSWORD_TUI_PAYLOADS_REGISTERED = true;
    }
    registrar.playToServer(
        ActiveSkillTriggerPayload.TYPE,
        ActiveSkillTriggerPayload.STREAM_CODEC,
        ActiveSkillTriggerPayload::handle);
    // Flying Sword TUI actions 已在上方带防重注册的分支中完成注册
    registrar.playToServer(
        SkillHotbarUpdatePayload.TYPE,
        SkillHotbarUpdatePayload.STREAM_CODEC,
        SkillHotbarUpdatePayload::handle);
    registrar.playToServer(
        SoulConfigRequestPayload.TYPE,
        SoulConfigRequestPayload.STREAM_CODEC,
        SoulConfigRequestPayload::handle);
    registrar.playToServer(
        SoulConfigActivatePayload.TYPE,
        SoulConfigActivatePayload.STREAM_CODEC,
        SoulConfigActivatePayload::handle);
    registrar.playToServer(
        SoulConfigRenamePayload.TYPE,
        SoulConfigRenamePayload.STREAM_CODEC,
        SoulConfigRenamePayload::handle);
    registrar.playToServer(
        SoulConfigSetOrderPayload.TYPE,
        SoulConfigSetOrderPayload.STREAM_CODEC,
        SoulConfigSetOrderPayload::handle);
    registrar.playToServer(
        SoulConfigSetVacuumPayload.TYPE,
        SoulConfigSetVacuumPayload.STREAM_CODEC,
        SoulConfigSetVacuumPayload::handle);
    registrar.playToServer(
        SoulConfigForceTeleportPayload.TYPE,
        SoulConfigForceTeleportPayload.STREAM_CODEC,
        SoulConfigForceTeleportPayload::handle);
    registrar.playToClient(
        net.tigereye.chestcavity.client.modernui.config.network.SoulConfigVacuumSyncPayload.TYPE,
        net.tigereye.chestcavity.client.modernui.config.network.SoulConfigVacuumSyncPayload
            .STREAM_CODEC,
        net.tigereye.chestcavity.client.modernui.config.network.SoulConfigVacuumSyncPayload
            ::handle);
    registrar.playToServer(
        net.tigereye.chestcavity.client.modernui.config.network.SoulConfigSetFollowTeleportPayload
            .TYPE,
        net.tigereye.chestcavity.client.modernui.config.network.SoulConfigSetFollowTeleportPayload
            .STREAM_CODEC,
        net.tigereye.chestcavity.client.modernui.config.network.SoulConfigSetFollowTeleportPayload
            ::handle);
    registrar.playToServer(
        PlayerPreferenceRequestPayload.TYPE,
        PlayerPreferenceRequestPayload.STREAM_CODEC,
        PlayerPreferenceRequestPayload::handle);
    registrar.playToServer(
        PlayerPreferenceUpdatePayload.TYPE,
        PlayerPreferenceUpdatePayload.STREAM_CODEC,
        PlayerPreferenceUpdatePayload::handle);
    registrar.playToClient(
        ChestCavityUpdatePayload.TYPE,
        ChestCavityUpdatePayload.STREAM_CODEC,
        ChestCavityUpdatePayload::handle);
    registrar.playToClient(
        OrganDataPayload.TYPE, OrganDataPayload.STREAM_CODEC, OrganDataPayload::handle);
    registrar.playToClient(
        ChestCavityOrganSlotUpdatePayload.TYPE,
        ChestCavityOrganSlotUpdatePayload.STREAM_CODEC,
        ChestCavityOrganSlotUpdatePayload::handle);
    registrar.playToClient(
        FlowSyncPayload.TYPE, FlowSyncPayload.STREAM_CODEC, FlowSyncPayload::handle);
    registrar.playToClient(
        FxEventPayload.TYPE, FxEventPayload.STREAM_CODEC, FxEventPayload::handle);
    registrar.playToClient(
        GeckoFxEventPayload.TYPE, GeckoFxEventPayload.STREAM_CODEC, GeckoFxEventPayload::handle);
    registrar.playToClient(
        SoulBeastSyncPayload.TYPE,
        SoulBeastSyncPayload.STREAM_CODEC,
        SoulBeastStateManager::handleSyncPayload);
    registrar.playToClient(
        SoulConfigSyncPayload.TYPE,
        SoulConfigSyncPayload.STREAM_CODEC,
        SoulConfigSyncPayload::handle);
    registrar.playToClient(
        net.tigereye.chestcavity.client.modernui.config.network.SoulConfigFollowTeleportSyncPayload
            .TYPE,
        net.tigereye.chestcavity.client.modernui.config.network.SoulConfigFollowTeleportSyncPayload
            .STREAM_CODEC,
        net.tigereye.chestcavity.client.modernui.config.network.SoulConfigFollowTeleportSyncPayload
            ::handle);
    registrar.playToClient(
        PlayerPreferenceSyncPayload.TYPE,
        PlayerPreferenceSyncPayload.STREAM_CODEC,
        PlayerPreferenceSyncPayload::handle);
    registrar.playToClient(
        TiePiProgressPayload.TYPE, TiePiProgressPayload.STREAM_CODEC, TiePiProgressPayload::handle);
    registrar.playToClient(
        net.tigereye.chestcavity.network.packets.CooldownReadyToastPayload.TYPE,
        net.tigereye.chestcavity.network.packets.CooldownReadyToastPayload.STREAM_CODEC,
        net.tigereye.chestcavity.network.packets.CooldownReadyToastPayload::handle);
    registrar.playToClient(
        SkillHotbarSnapshotPayload.TYPE,
        SkillHotbarSnapshotPayload.STREAM_CODEC,
        SkillHotbarSnapshotPayload::handle);

    // Soul navigation Baritone planning bridge
    registrar.playToClient(
        net.tigereye.chestcavity.soul.navigation.net.SoulNavPlanRequestPayload.TYPE,
        net.tigereye.chestcavity.soul.navigation.net.SoulNavPlanRequestPayload.STREAM_CODEC,
        net.tigereye.chestcavity.soul.navigation.net.SoulNavPlanRequestPayload::handle);
    registrar.playToServer(
        net.tigereye.chestcavity.soul.navigation.net.SoulNavPlanResponsePayload.TYPE,
        net.tigereye.chestcavity.soul.navigation.net.SoulNavPlanResponsePayload.STREAM_CODEC,
        net.tigereye.chestcavity.soul.navigation.net.SoulNavPlanResponsePayload::handle);

    // Domain sync (通用领域渲染同步)
    registrar.playToClient(
        net.tigereye.chestcavity.compat.guzhenren.domain.network.DomainSyncPayload.TYPE,
        net.tigereye.chestcavity.compat.guzhenren.domain.network.DomainSyncPayload.STREAM_CODEC,
        net.tigereye.chestcavity.compat.guzhenren.domain.network.DomainSyncPayload::handle);
    registrar.playToClient(
        net.tigereye.chestcavity.compat.guzhenren.domain.network.DomainRemovePayload.TYPE,
        net.tigereye.chestcavity.compat.guzhenren.domain.network.DomainRemovePayload.STREAM_CODEC,
        net.tigereye.chestcavity.compat.guzhenren.domain.network.DomainRemovePayload::handle);

    // Hun Dao sync (魂道 HUD/FX 同步)
    HunDaoNetworkInit.register(event);
  }

  public static void sendChestCavityUpdate(ServerPlayer player, ChestCavityUpdatePayload payload) {
    player.connection.send(payload);
  }

  public static void sendOrganData(ServerPlayer player) {
    List<OrganDataPayload.Entry> entries = new ArrayList<>();
    for (Map.Entry<ResourceLocation, OrganData> entry :
        OrganManager.GeneratedOrganData.entrySet()) {
      entries.add(
          new OrganDataPayload.Entry(
              entry.getKey(), entry.getValue().pseudoOrgan, entry.getValue().organScores));
    }
    player.connection.send(new OrganDataPayload(entries));
  }

  public static void sendOrganSlotUpdate(
      ServerPlayer player, ChestCavityOrganSlotUpdatePayload payload) {
    player.connection.send(payload);
  }

  public static void sendFlowSync(ServerPlayer player, FlowSyncPayload payload) {
    player.connection.send(payload);
  }

  public static void sendGeckoFx(ServerPlayer player, GeckoFxEventPayload payload) {
    player.connection.send(payload);
  }

  public static void sendCooldownToast(
      ServerPlayer player,
      net.tigereye.chestcavity.network.packets.CooldownReadyToastPayload payload) {
    player.connection.send(payload);
  }
}
