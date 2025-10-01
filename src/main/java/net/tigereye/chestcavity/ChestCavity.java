package net.tigereye.chestcavity;


//import com.github.alexthe666.alexsmobs.AlexsMobs;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.chestcavities.organs.OrganManager;
import net.tigereye.chestcavity.chestcavities.types.json.GeneratedChestCavityAssignmentManager;
import net.tigereye.chestcavity.chestcavities.types.json.GeneratedChestCavityTypeManager;
import net.tigereye.chestcavity.network.NetworkHandler;
import net.tigereye.chestcavity.network.ServerEvents;
import net.tigereye.chestcavity.registration.*;
import net.tigereye.chestcavity.util.retention.OrganRetentionRules;
import net.tigereye.chestcavity.ui.ChestCavityScreen;
import net.tigereye.chestcavity.guscript.ui.GuScriptScreen;
import net.tigereye.chestcavity.listeners.KeybindingClientListeners;

import net.tigereye.chestcavity.guscript.registry.GeckoFxDefinitionLoader;
import net.tigereye.chestcavity.client.render.ChestCavityClientRenderers;
import net.tigereye.chestcavity.guscript.registry.GuScriptFlowLoader;
import net.tigereye.chestcavity.guscript.registry.GuScriptLeafLoader;
import net.tigereye.chestcavity.guscript.registry.GuScriptRuleLoader;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptListenerHooks;
import net.tigereye.chestcavity.guscript.runtime.flow.GuScriptFlowEvents;

import net.tigereye.chestcavity.guscript.registry.FxDefinitionLoader;
import net.tigereye.chestcavity.guscript.fx.client.FxClientHooks;
import net.tigereye.chestcavity.guscript.fx.gecko.client.GeckoFxClient;
import net.tigereye.chestcavity.guscript.command.GuScriptCommands;
import net.tigereye.chestcavity.command.RecipeDebugCommands;
import net.tigereye.chestcavity.debug.RecipeResourceProbe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import net.tigereye.chestcavity.guzhenren.GuzhenrenModule;



@Mod(ChestCavity.MODID)
public class ChestCavity { //TODO: fix 1.19 version to include color thing, fix organUtil class, possibly update to 4?, add alexs mobs and other mods compat

	//Ideas: make smaller one that can only be used on small animals, and make it with a durability so that it can only be used a certain amount of times and not too op

	//Changelog:
	//fixed a bug where cloth config would cause the game to not load
	//fixed bugs related to multiplayer and not being able to see organ qualities
	//updated the version to 2.16.4


	public static final String MODID = "chestcavity";
    private static final boolean DEBUG_MODE = false; //Make SURE that this is false when building
	public static final Logger LOGGER = LogManager.getLogger();
	public static CCConfig config;
	public static final ResourceLocation COMPATIBILITY_TAG = id("organ_compatibility");

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}


	public ChestCavity(IEventBus modEventBus) {
		//AlexsMobs
		IEventBus bus = modEventBus;
		bus.addListener(this::setup);
		bus.addListener(this::doClientStuff);
		bus.addListener(this::registerMenuScreens);
		bus.addListener(this::doServerStuff);
        bus.addListener(NetworkHandler::registerCommon);


		NeoForge.EVENT_BUS.addListener(ServerEvents::onPlayerLogin);
		NeoForge.EVENT_BUS.addListener(ServerEvents::onPlayerRespawn);
		NeoForge.EVENT_BUS.addListener(ServerEvents::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(ServerEvents::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(ServerEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(this::registerReloadListeners);
        NeoForge.EVENT_BUS.addListener(GuScriptListenerHooks::onLivingDamage);
                NeoForge.EVENT_BUS.addListener(GuScriptListenerHooks::onPlayerTick);
                NeoForge.EVENT_BUS.addListener(GuScriptFlowEvents::onServerTick);
                NeoForge.EVENT_BUS.addListener(GuScriptFlowEvents::onPlayerLogout);
                NeoForge.EVENT_BUS.addListener(GuScriptCommands::register);
                NeoForge.EVENT_BUS.addListener(RecipeDebugCommands::register);
		if (FMLEnvironment.dist.isClient()) {
                        NeoForge.EVENT_BUS.addListener(KeybindingClientListeners::onClientTick);
                        NeoForge.EVENT_BUS.addListener(GeckoFxClient::onClientTick);
                        NeoForge.EVENT_BUS.addListener(GeckoFxClient::onRenderLevel);
                }

                if (FMLEnvironment.dist.isClient()) {
                        bus.addListener(this::registerClientReloadListeners);
                        bus.addListener(ChestCavityClientRenderers::onRegisterRenderers);
                }

		AutoConfig.register(CCConfig.class, GsonConfigSerializer::new);
		config = AutoConfig.getConfigHolder(CCConfig.class).getConfig();
		//Register mod resources
		//AutoConfig.register(CCConfig.class, GsonConfigSerializer::new);
		//config = AutoConfig.getConfigHolder(CCConfig.class).getConfig();
		CCCreativeTabs.TABS.register(bus);
		CCAttachments.ATTACHMENT_TYPES.register(bus);
		CCContainers.MENU_TYPES.register(bus);
		CCItems.ITEMS.register(bus);
		CCEntities.ENTITY_TYPES.register(bus);
		CCRecipes.RECIPE_SERIALIZERS.register(bus);
		CCRecipes.RECIPE_TYPES.register(bus);
		CCEnchantments.ENCHANTMENTS.register(bus);
		CCListeners.register();
		CCStatusEffects.MOB_EFFECTS.register(bus);
		bus.addListener(CCKeybindings::register);
		CCTagOrgans.init();
    OrganRetentionRules.registerNamespace(MODID);
    if (ModList.get().isLoaded("guzhenren")) {
            GuzhenrenModule.bootstrap(bus, NeoForge.EVENT_BUS);
    }


		//CCCommands.register();
		//CCNetworkingPackets.register();
		//ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new OrganManager());
		//ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new GeneratedChestCavityTypeManager());
		//ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new GeneratedChestCavityAssignmentManager());
		//CrossModContent.register();

	}
	public void setup(FMLCommonSetupEvent event) {
	}

        public void doClientStuff(FMLClientSetupEvent event) {
                FxClientHooks.init();
        }

	private void registerMenuScreens(RegisterMenuScreensEvent event) {
		event.register(CCContainers.CHEST_CAVITY_SCREEN_HANDLER.get(), ChestCavityScreen::new);
		event.register(CCContainers.GUSCRIPT_MENU.get(), GuScriptScreen::new);
	}

	public void doServerStuff(FMLDedicatedServerSetupEvent event) {
		//ServerPlayNetworking.registerGlobalReceiver(CCNetworkingPackets.RECEIVED_UPDATE_PACKET_ID, (server, player, handler, buf, sender) -> {
		//    Optional<ChestCavityEntity> optional = ChestCavityEntity.of(player);
		//    optional.ifPresent(chestCavityEntity -> NetworkUtil.ReadChestCavityReceivedUpdatePacket(chestCavityEntity.getChestCavityInstance()));
		//});
		//ServerPlayNetworking.registerGlobalReceiver(CCNetworkingPackets.HOTKEY_PACKET_ID, (server, player, handler, buf, sender) -> {
		//    Optional<ChestCavityEntity> optional = ChestCavityEntity.of(player);
		//    optional.ifPresent(chestCavityEntity -> NetworkUtil.ReadChestCavityHotkeyPacket(chestCavityEntity.getChestCavityInstance(),buf));
		//});
	}

	private void registerReloadListeners(AddReloadListenerEvent event) {

          event.addListener(new OrganManager());
          event.addListener(new GeneratedChestCavityTypeManager());
          event.addListener(new GeneratedChestCavityAssignmentManager());
          event.addListener(new GuScriptLeafLoader());
          event.addListener(new GuScriptRuleLoader());
          event.addListener(new GuScriptFlowLoader());
          event.addListener(new RecipeResourceProbe());
          // FX definitions are client-only; do not register on server reload
  }

  private void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
          event.registerReloadListener(new GuScriptLeafLoader());
          event.registerReloadListener(new GuScriptRuleLoader());
          event.registerReloadListener(new GeckoFxDefinitionLoader());
          event.registerReloadListener(new FxDefinitionLoader());
  }

	public static boolean isDebugMode() {
		return DEBUG_MODE;
	}

	public static void printOnDebug(String stringToPrint) {
		if(DEBUG_MODE) {
			System.out.println("DEBUG: " + stringToPrint);
		}
	}

	public static void printOnDebug(java.util.function.Supplier<String> supplier) {
		if(DEBUG_MODE) {
			System.out.println("DEBUG: " + supplier.get());
		}
	}
}
