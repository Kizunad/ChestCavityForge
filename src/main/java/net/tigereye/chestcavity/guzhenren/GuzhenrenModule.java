package net.tigereye.chestcavity.guzhenren;

import java.util.Objects;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.BianHuaDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.BingXueDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.FengDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.GuDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.GuDaoClientRenderLayers;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.SteelBoneAttributeHooks;
import net.tigereye.chestcavity.compat.guzhenren.item.guang_dao.GuangDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.guang_dao.GuangDaoClientRenderers;
import net.tigereye.chestcavity.compat.guzhenren.item.guang_dao.GuangDaoEntityAttributes;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.HunDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.JianYingGuEvents;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.JiandaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.JiandaoClientRenderers;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.JiandaoEntityAttributes;
import net.tigereye.chestcavity.compat.guzhenren.item.jin_dao.JinDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.LiDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.MuDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.RenDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.shi_dao.ShiDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.tu_dao.TuDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.XueDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.YanDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.yu_dao.YuDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.yun_dao_cloud.YunDaoCloudClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.module.GuzhenrenIntegrationModule;
import net.tigereye.chestcavity.compat.guzhenren.module.GuzhenrenOrganScoreEffects;
import net.tigereye.chestcavity.guscript.GuScriptModule;
import net.tigereye.chestcavity.guscript.ability.guzhenren.Abilities;
import net.tigereye.chestcavity.guscript.ability.guzhenren.blood_bone_bomb.BloodBoneBombClient;
import net.tigereye.chestcavity.guzhenren.network.GuzhenrenNetworkBridge;
import net.tigereye.chestcavity.guzhenren.network.PlayerSkinSyncClient;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceEvents;
import net.tigereye.chestcavity.util.retention.OrganRetentionRules;

/**
 * Central bootstrap for Guzhenren compatibility hooks. All optional wiring lives here so other
 * callers can simply invoke {@link #bootstrap(IEventBus, IEventBus)} when the compat mod is
 * present.
 */
public final class GuzhenrenModule {

  private static final String MOD_ID = "guzhenren";
  private static final Object LOCK = new Object();

  private static IEventBus registeredModBus;
  private static IEventBus registeredForgeBus;
  private static boolean initialised;

  private GuzhenrenModule() {}

  public static void bootstrap(IEventBus modBus, IEventBus forgeBus) {
    Objects.requireNonNull(modBus, "GuzhenrenModule requires the mod event bus");
    Objects.requireNonNull(forgeBus, "GuzhenrenModule requires the NeoForge event bus");

    if (!ModList.get().isLoaded(MOD_ID)) {
      if (ChestCavity.LOGGER.isDebugEnabled()) {
        ChestCavity.LOGGER.debug("[compat/guzhenren] bootstrap skipped: mod not loaded");
      }
      return;
    }

    boolean registerModListeners = false;
    boolean registerForgeListeners = false;
    boolean runInitialisation = false;

    synchronized (LOCK) {
      if (registeredModBus != modBus) {
        registeredModBus = modBus;
        registerModListeners = true;
      }
      if (registeredForgeBus != forgeBus) {
        registeredForgeBus = forgeBus;
        registerForgeListeners = true;
      }
      if (!initialised) {
        initialised = true;
        runInitialisation = true;
      }
    }

    if (registerModListeners) {
      installModListeners(modBus);
    }
    if (registerForgeListeners) {
      installForgeListeners(forgeBus);
    }
    if (runInitialisation) {
      initialiseCompat();
    }
  }

  private static void installModListeners(IEventBus modBus) {
    modBus.addListener(JiandaoEntityAttributes::onAttributeCreation);
    modBus.addListener(GuangDaoEntityAttributes::onAttributeCreation);
    modBus.addListener(SteelBoneAttributeHooks::onAttributeModification);
    if (FMLEnvironment.dist.isClient()) {
      modBus.addListener(Abilities::onClientSetup);
      modBus.addListener(GuDaoClientAbilities::onClientSetup);
      modBus.addListener(FengDaoClientAbilities::onClientSetup);
      modBus.addListener(BingXueDaoClientAbilities::onClientSetup);
      modBus.addListener(LiDaoClientAbilities::onClientSetup);
      modBus.addListener(MuDaoClientAbilities::onClientSetup);
      modBus.addListener(ShiDaoClientAbilities::onClientSetup);
      modBus.addListener(JiandaoClientAbilities::onClientSetup);
      modBus.addListener(TuDaoClientAbilities::onClientSetup);
      modBus.addListener(GuangDaoClientAbilities::onClientSetup);
      modBus.addListener(XueDaoClientAbilities::onClientSetup);
      modBus.addListener(YanDaoClientAbilities::onClientSetup);
      modBus.addListener(HunDaoClientAbilities::onClientSetup);
      modBus.addListener(YuDaoClientAbilities::onClientSetup);
      modBus.addListener(JinDaoClientAbilities::onClientSetup);
      modBus.addListener(BianHuaDaoClientAbilities::onClientSetup);
      modBus.addListener(YunDaoCloudClientAbilities::onClientSetup);
      modBus.addListener(RenDaoClientAbilities::onClientSetup);
      modBus.addListener(BloodBoneBombClient::onRegisterRenderers);
      modBus.addListener(JiandaoClientRenderers::onRegisterRenderers);
      modBus.addListener(GuangDaoClientRenderers::onRegisterRenderers);
      modBus.addListener(GuDaoClientRenderLayers::onAddLayers);
      // 注意：渲染事件(RenderLevelStageEvent)是 FORGE 总线事件，不应注册到 modBus
    }
  }

  private static void installForgeListeners(IEventBus forgeBus) {
    forgeBus.addListener(JianYingGuEvents::onServerTick);
    // 领域系统 tick（统一调度）
    forgeBus.addListener(
        net.tigereye.chestcavity.compat.guzhenren.domain.DomainEvents::onServerTick);
    forgeBus.addListener(GuzhenrenResourceEvents::onPlayerLoggedIn);
    forgeBus.addListener(GuzhenrenResourceEvents::onPlayerRespawn);
    forgeBus.addListener(GuzhenrenResourceEvents::onPlayerClone);
    forgeBus.addListener(GuzhenrenResourceEvents::onPlayerChangedDimension);
    if (FMLEnvironment.dist.isClient()) {
      // 通用领域 PNG 渲染（AFTER_PARTICLES 阶段）
      forgeBus.addListener(
          net.tigereye.chestcavity.compat.guzhenren.domain.client.DomainRenderer::render);
      forgeBus.addListener(
          (ClientTickEvent.Post event) -> PlayerSkinSyncClient.onClientTick(event));
      // Hun Dao client events (Phase 5.1 fix: register client tick and level unload handlers)
      forgeBus.addListener(
          net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.HunDaoClientEvents::onClientTick);
      forgeBus.addListener(
          net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.HunDaoClientEvents::onLevelUnload);
    }
  }

  private static void initialiseCompat() {
    if (ChestCavity.LOGGER.isDebugEnabled()) {
      ChestCavity.LOGGER.debug("[compat/guzhenren] installing compatibility hooks");
    }
    OrganRetentionRules.registerNamespace(MOD_ID);
    Abilities.bootstrap();
    GuzhenrenIntegrationModule.bootstrap();
    GuzhenrenOrganScoreEffects.bootstrap();
    GuzhenrenNetworkBridge.bootstrap();
    GuScriptModule.bootstrap();
    // Phase 5.1 fix: Initialize Hun Dao FX templates on both client and server
    net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.fx.HunDaoFxInit.init();
  }
}
