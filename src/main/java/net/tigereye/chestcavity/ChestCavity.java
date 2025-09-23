package net.tigereye.chestcavity;


//import com.github.alexthe666.alexsmobs.AlexsMobs;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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
import net.tigereye.chestcavity.listeners.KeybindingClientListeners;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.GuDaoClientAbilities;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.GuDaoClientRenderLayers;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

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

    if (FMLEnvironment.dist.isClient()) {
            bus.addListener(GuDaoClientAbilities::onClientSetup);
    }

    bus.addListener(GuDaoClientRenderLayers::onAddLayers);


		NeoForge.EVENT_BUS.addListener(ServerEvents::onPlayerLogin);
		NeoForge.EVENT_BUS.addListener(ServerEvents::onPlayerRespawn);
		NeoForge.EVENT_BUS.addListener(ServerEvents::onPlayerClone);
		NeoForge.EVENT_BUS.addListener(ServerEvents::onPlayerChangedDimension);
		NeoForge.EVENT_BUS.addListener(ServerEvents::onLivingDeath);
		NeoForge.EVENT_BUS.addListener(this::registerReloadListeners);
		if (FMLEnvironment.dist.isClient()) {
			NeoForge.EVENT_BUS.addListener(KeybindingClientListeners::onClientTick);
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
		CCRecipes.RECIPE_SERIALIZERS.register(bus);
		CCRecipes.RECIPE_TYPES.register(bus);
		CCEnchantments.ENCHANTMENTS.register(bus);
		CCListeners.register();
		CCStatusEffects.MOB_EFFECTS.register(bus);
		bus.addListener(CCKeybindings::register);
		CCTagOrgans.init();
		OrganRetentionRules.registerNamespace(MODID);
		OrganRetentionRules.registerNamespace("guzhenren");
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
	}

	private void registerMenuScreens(RegisterMenuScreensEvent event) {
		event.register(CCContainers.CHEST_CAVITY_SCREEN_HANDLER.get(), ChestCavityScreen::new);
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
