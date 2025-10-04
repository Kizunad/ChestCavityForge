package net.tigereye.chestcavity.guzhenren;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.ability.guzhenren.Abilities;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.BingXueDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.GuDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.GuDaoClientRenderLayers;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.JiandaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.JiandaoClientRenderers;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.JiandaoEntityAttributes;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.JianYingGuEvents;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.HunDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.LiDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.MuDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.shi_dao.ShiDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.XueDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.SteelBoneAttributeHooks;
import net.tigereye.chestcavity.guscript.GuScriptModule;
import net.tigereye.chestcavity.guscript.ability.guzhenren.blood_bone_bomb.BloodBoneBombClient;
import net.tigereye.chestcavity.guzhenren.network.GuzhenrenNetworkBridge;
import net.tigereye.chestcavity.compat.guzhenren.module.GuzhenrenIntegrationModule;
import net.tigereye.chestcavity.compat.guzhenren.module.GuzhenrenOrganScoreEffects;
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
        modBus.addListener(SteelBoneAttributeHooks::onAttributeModification);
        if (FMLEnvironment.dist.isClient()) {
            modBus.addListener(Abilities::onClientSetup);
            modBus.addListener(GuDaoClientAbilities::onClientSetup);
            modBus.addListener(BingXueDaoClientAbilities::onClientSetup);
            modBus.addListener(LiDaoClientAbilities::onClientSetup);
            modBus.addListener(MuDaoClientAbilities::onClientSetup);
            modBus.addListener(ShiDaoClientAbilities::onClientSetup);
            modBus.addListener(JiandaoClientAbilities::onClientSetup);
            modBus.addListener(XueDaoClientAbilities::onClientSetup);
            modBus.addListener(HunDaoClientAbilities::onClientSetup);
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
        GuzhenrenIntegrationModule.bootstrap();
        GuzhenrenOrganScoreEffects.bootstrap();
        GuzhenrenNetworkBridge.bootstrap();
        GuScriptModule.bootstrap();
    }
}
