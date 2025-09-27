package net.tigereye.chestcavity.guzhenren;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.ability.Abilities;
import net.tigereye.chestcavity.compat.guzhenren.item.du_dao.DuDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_cai.GuCaiOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.GuDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.GuDaoClientRenderLayers;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.GuDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.JiandaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.JiandaoClientRenderers;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.JiandaoEntityAttributes;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.JianYingGuEvents;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.JiandaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.kongqiao.KongqiaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.lei_dao.LeiDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.MuDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.MuDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.san_zhuan.wu_hang.WuHangOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.shi_dao.ShiDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.shi_dao.ShiDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.ShuiDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.tu_dao.TuDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.XueDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.XueDaoOrganRegistry;
import net.tigereye.chestcavity.guscript.GuScriptModule;
import net.tigereye.chestcavity.compat.guzhenren.ability.blood_bone_bomb.BloodBoneBombClient;
import net.tigereye.chestcavity.guzhenren.network.GuzhenrenNetworkBridge;
import net.tigereye.chestcavity.util.retention.OrganRetentionRules;

import java.util.Objects;

/**
 * Central bootstrap for Guzhenren compatibility hooks. All optional wiring lives here so other
 * callers can simply invoke {@link #bootstrap(IEventBus, IEventBus)} when the compat mod is present.
 */
public final class GuzhenrenModule {

    private static final String MOD_ID = "guzhenren";
    private static final Object LOCK = new Object();

    private static IEventBus registeredModBus;
    private static IEventBus registeredForgeBus;
    private static boolean initialised;

    private GuzhenrenModule() {
    }

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
        if (FMLEnvironment.dist.isClient()) {
            modBus.addListener(Abilities::onClientSetup);
            modBus.addListener(GuDaoClientAbilities::onClientSetup);
            modBus.addListener(MuDaoClientAbilities::onClientSetup);
            modBus.addListener(ShiDaoClientAbilities::onClientSetup);
            modBus.addListener(JiandaoClientAbilities::onClientSetup);
            modBus.addListener(XueDaoClientAbilities::onClientSetup);
            modBus.addListener(BloodBoneBombClient::onRegisterRenderers);
            modBus.addListener(JiandaoClientRenderers::onRegisterRenderers);
            modBus.addListener(GuDaoClientRenderLayers::onAddLayers);
        }
    }

    private static void installForgeListeners(IEventBus forgeBus) {
        forgeBus.addListener(JianYingGuEvents::onServerTick);
    }

    private static void initialiseCompat() {
        if (ChestCavity.LOGGER.isDebugEnabled()) {
            ChestCavity.LOGGER.debug("[compat/guzhenren] installing compatibility hooks");
        }
        OrganRetentionRules.registerNamespace(MOD_ID);
        Abilities.bootstrap();
        GuCaiOrganRegistry.bootstrap();
        DuDaoOrganRegistry.bootstrap();
        GuDaoOrganRegistry.bootstrap();
        LeiDaoOrganRegistry.bootstrap();
        KongqiaoOrganRegistry.bootstrap();
        MuDaoOrganRegistry.bootstrap();
        TuDaoOrganRegistry.bootstrap();
        ShuiDaoOrganRegistry.bootstrap();
        XueDaoOrganRegistry.bootstrap();
        WuHangOrganRegistry.bootstrap();
        ShiDaoOrganRegistry.bootstrap();
        JiandaoOrganRegistry.bootstrap();
        GuzhenrenNetworkBridge.bootstrap();
        GuScriptModule.bootstrap();
    }
}
