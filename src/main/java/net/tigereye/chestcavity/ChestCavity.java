package net.tigereye.chestcavity;


//import com.github.alexthe666.alexsmobs.AlexsMobs;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.network.NetworkHandler;
import net.tigereye.chestcavity.registration.*;
import net.tigereye.chestcavity.ui.ChestCavityScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	public static final ResourceLocation COMPATIBILITY_TAG = new ResourceLocation(MODID,"organ_compatibility");
	public static final CreativeModeTab ORGAN_ITEM_GROUP = new CreativeModeTab("chestcavity.organs") {
		@Override
		public ItemStack makeIcon() {
			return new ItemStack(CCItems.HUMAN_STOMACH.get());
		}
	};


	public ChestCavity() {
		//AlexsMobs
		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		bus.addListener(this::setup);
		bus.addListener(this::doClientStuff);
		bus.addListener(this::doServerStuff);

		AutoConfig.register(CCConfig.class, GsonConfigSerializer::new);
		config = AutoConfig.getConfigHolder(CCConfig.class).getConfig();
		//Register mod resources
		//AutoConfig.register(CCConfig.class, GsonConfigSerializer::new);
		//config = AutoConfig.getConfigHolder(CCConfig.class).getConfig();
		CCContainers.MENU_TYPES.register(bus);
		CCItems.ITEMS.register(bus);
		CCRecipes.RECIPE_SERIALIZERS.register(bus);
		CCEnchantments.ENCHANTMENTS.register(bus);
		CCListeners.register();
		CCStatusEffects.MOB_EFFECTS.register(bus);
		CCTagOrgans.init();
		//CCCommands.register();
		//CCNetworkingPackets.register();
		//ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new OrganManager());
		//ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new GeneratedChestCavityTypeManager());
		//ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new GeneratedChestCavityAssignmentManager());
		//CrossModContent.register();

	}
	public void setup(FMLCommonSetupEvent event) {
		NetworkHandler.init();
	}

	public void doClientStuff(FMLClientSetupEvent event) {
		MenuScreens.register(CCContainers.CHEST_CAVITY_SCREEN_HANDLER.get(), ChestCavityScreen::new);
		CCKeybindings.init();
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

	public static boolean isDebugMode() {
		return DEBUG_MODE;
	}

	public static void printOnDebug(String stringToPrint) {
		if(DEBUG_MODE) {
			System.out.println("DEBUG: " + stringToPrint);
		}
	}
}
