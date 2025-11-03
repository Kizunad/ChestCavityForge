package net.tigereye.chestcavity;

// import com.github.alexthe666.alexsmobs.AlexsMobs;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.tigereye.chestcavity.chestcavities.organs.OrganManager;
import net.tigereye.chestcavity.chestcavities.types.json.GeneratedChestCavityAssignmentManager;
import net.tigereye.chestcavity.chestcavities.types.json.GeneratedChestCavityTypeManager;
import net.tigereye.chestcavity.client.command.ModernUIClientCommands;
import net.tigereye.chestcavity.client.input.ModernUIKeyDispatcher;
import net.tigereye.chestcavity.client.modernui.skill.SkillHotbarClientData;
import net.tigereye.chestcavity.client.render.ChestCavityClientRenderers;
import net.tigereye.chestcavity.command.ModernUiServerCommands;
import net.tigereye.chestcavity.command.RecipeDebugCommands;
import net.tigereye.chestcavity.compat.guzhenren.commands.WuxingConfigCommand;
import net.tigereye.chestcavity.compat.guzhenren.commands.WuxingGuiBianConfigCommand;
import net.tigereye.chestcavity.compat.guzhenren.event.NoDropEvents;
import net.tigereye.chestcavity.compat.guzhenren.util.hun_dao.soulbeast.command.SoulBeastCommands;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.config.CCGameRules;
import net.tigereye.chestcavity.engine.dot.DoTEngine;
import net.tigereye.chestcavity.engine.reaction.ReactionEngine;
import net.tigereye.chestcavity.guscript.command.GuScriptCommands;
import net.tigereye.chestcavity.guscript.fx.client.FxClientHooks;
import net.tigereye.chestcavity.guscript.fx.gecko.client.GeckoFxClient;
import net.tigereye.chestcavity.guscript.registry.FxDefinitionLoader;
import net.tigereye.chestcavity.guscript.registry.GeckoFxDefinitionLoader;
import net.tigereye.chestcavity.guscript.registry.GuScriptFlowLoader;
import net.tigereye.chestcavity.guscript.registry.GuScriptLeafLoader;
import net.tigereye.chestcavity.guscript.registry.GuScriptRuleLoader;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptListenerHooks;
import net.tigereye.chestcavity.guscript.runtime.flow.GuScriptFlowEvents;
import net.tigereye.chestcavity.guzhenren.GuzhenrenModule;
import net.tigereye.chestcavity.listeners.KeybindingClientListeners;
import net.tigereye.chestcavity.network.NetworkHandler;
import net.tigereye.chestcavity.network.ServerEvents;
import net.tigereye.chestcavity.registration.*;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.skill.ComboSkillRegistry;
import net.tigereye.chestcavity.soul.command.SoulCommands;
import net.tigereye.chestcavity.soul.entity.SoulClanSpawner;
import net.tigereye.chestcavity.soul.entity.SoulEntityAttributes;
import net.tigereye.chestcavity.soul.entity.TestSoulSpawner;
import net.tigereye.chestcavity.soul.playerghost.PlayerGhostEvents;
import net.tigereye.chestcavity.soul.playerghost.PlayerGhostSpawner;
import net.tigereye.chestcavity.soul.profile.capability.CapabilitySnapshots;
import net.tigereye.chestcavity.util.retention.OrganRetentionRules;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ChestCavity.MODID)
public class ChestCavity {

  public static final String MODID = "chestcavity";
  private static final boolean DEBUG_MODE = false; // Make SURE that this is false when building
  public static final Logger LOGGER = LogManager.getLogger();
  public static CCConfig config;
  public static final ResourceLocation COMPATIBILITY_TAG = id("organ_compatibility");

  public static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath(MODID, path);
  }

  public ChestCavity(IEventBus modEventBus) {
    // AlexsMobs
    IEventBus bus = modEventBus;
    bus.addListener(this::setup);
    bus.addListener(this::doClientStuff);
    bus.addListener(this::doServerStuff);
    bus.addListener(NetworkHandler::registerCommon);

    NeoForge.EVENT_BUS.addListener(ServerEvents::onPlayerLogin);
    NeoForge.EVENT_BUS.addListener(ServerEvents::onPlayerRespawn);
    NeoForge.EVENT_BUS.addListener(ServerEvents::onPlayerClone);
    NeoForge.EVENT_BUS.addListener(ServerEvents::onPlayerChangedDimension);
    NeoForge.EVENT_BUS.addListener(ServerEvents::onLivingDeath);
    NeoForge.EVENT_BUS.addListener(PlayerGhostEvents::onPlayerDeath);
    NeoForge.EVENT_BUS.addListener(this::registerReloadListeners);
    NeoForge.EVENT_BUS.addListener(GuScriptListenerHooks::onLivingDamage);
    NeoForge.EVENT_BUS.addListener(GuScriptListenerHooks::onPlayerTick);
    NeoForge.EVENT_BUS.addListener(GuScriptFlowEvents::onServerTick);
    NeoForge.EVENT_BUS.addListener(GuScriptFlowEvents::onPlayerLogout);
    NeoForge.EVENT_BUS.addListener(GuScriptCommands::register);
    NeoForge.EVENT_BUS.addListener(RecipeDebugCommands::register);
    NeoForge.EVENT_BUS.addListener(ModernUiServerCommands::register);
    NeoForge.EVENT_BUS.addListener(SoulBeastCommands::register);
    NeoForge.EVENT_BUS.addListener(SoulCommands::register);
    NeoForge.EVENT_BUS.addListener(WuxingConfigCommand::register);
    NeoForge.EVENT_BUS.addListener(WuxingGuiBianConfigCommand::register);
    // Central DoT manager ticking
    DoTEngine.bootstrap();
    NeoForge.EVENT_BUS.addListener(TestSoulSpawner::onServerTick);
    NeoForge.EVENT_BUS.addListener(PlayerGhostSpawner::onServerTick);
    SoulClanSpawner.init();
    if (FMLEnvironment.dist.isClient()) {
      NeoForge.EVENT_BUS.addListener(KeybindingClientListeners::onClientTick);
      NeoForge.EVENT_BUS.addListener(ModernUIKeyDispatcher::onClientTick);
      NeoForge.EVENT_BUS.addListener(GeckoFxClient::onClientTick);
      NeoForge.EVENT_BUS.addListener(GeckoFxClient::onRenderLevel);
      NeoForge.EVENT_BUS.addListener(ModernUIClientCommands::register);
      // 通用领域PNG渲染器
      NeoForge.EVENT_BUS.addListener(
          net.tigereye.chestcavity.compat.guzhenren.domain.client.DomainRenderer::render);
    }

    if (FMLEnvironment.dist.isClient()) {
      bus.addListener(this::registerClientReloadListeners);
      bus.addListener(ChestCavityClientRenderers::onRegisterRenderers);
      // 注册菜单界面（避免使用已弃用的注解总线参数）
      bus.addListener(net.tigereye.chestcavity.client.event.ChestCavityClientEvents::registerMenuScreens);
    }

    AutoConfig.register(CCConfig.class, GsonConfigSerializer::new);
    config = AutoConfig.getConfigHolder(CCConfig.class).getConfig();
    CCGameRules.register();
    // Register mod resources
    // AutoConfig.register(CCConfig.class, GsonConfigSerializer::new);
    // config = AutoConfig.getConfigHolder(CCConfig.class).getConfig();
    CCCreativeTabs.TABS.register(bus);
    CCAttachments.ATTACHMENT_TYPES.register(bus);
    CCContainers.MENU_TYPES.register(bus);
    CCItems.ITEMS.register(bus);
    CCEntities.ENTITY_TYPES.register(bus);
    CCSoundEvents.SOUND_EVENTS.register(bus);
    CCRecipes.RECIPE_SERIALIZERS.register(bus);
    CCRecipes.RECIPE_TYPES.register(bus);
    CCEnchantments.ENCHANTMENTS.register(bus);
    CCListeners.register();
    CCStatusEffects.MOB_EFFECTS.register(bus);
    bus.addListener(CCKeybindings::register);
    bus.addListener(SoulEntityAttributes::onAttributeCreation);
    bus.addListener(this::registerSpawnPlacements);
    CCTagOrgans.init();
    CapabilitySnapshots.bootstrap();
    net.tigereye.chestcavity.soul.runtime.SoulRuntimeHandlers.bootstrap();
    OrganRetentionRules.registerNamespace(MODID);
    if (ModList.get().isLoaded("guzhenren")) {
      ActiveSkillRegistry.bootstrap();
      ComboSkillRegistry.bootstrap();
      ActivationHookRegistry.register();
      UseItemHookRegistry.register();
      net.tigereye.chestcavity.compat.common.passive.PassiveBus.init();
      net.tigereye.chestcavity.compat.guzhenren.GuzhenrenCompatBootstrap
          .registerBianHuaDaoPassives();
      net.tigereye.chestcavity.compat.guzhenren.GuzhenrenCompatBootstrap
          .registerBingXueDaoPassives();
      GuzhenrenModule.bootstrap(bus, NeoForge.EVENT_BUS);
      ReactionEngine.bootstrap();
      // 提前注册古真人召唤相关的无掉落事件，避免召唤物产生战利品。
      NoDropEvents.init();
    }

    // CCCommands.register();
    // CCNetworkingPackets.register();
    // ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new
    // OrganManager());
    // ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new
    // GeneratedChestCavityTypeManager());
    // ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new
    // GeneratedChestCavityAssignmentManager());
    // CrossModContent.register();

  }

  public void setup(FMLCommonSetupEvent event) {}

  public void doClientStuff(FMLClientSetupEvent event) {
    FxClientHooks.init();
    event.enqueueWork(SkillHotbarClientData::initialize);
  }

  public void doServerStuff(FMLDedicatedServerSetupEvent event) {
    // ServerPlayNetworking.registerGlobalReceiver(CCNetworkingPackets.RECEIVED_UPDATE_PACKET_ID,
    // (server, player, handler, buf, sender) -> {
    //    Optional<ChestCavityEntity> optional = ChestCavityEntity.of(player);
    //    optional.ifPresent(chestCavityEntity ->
    // NetworkUtil.ReadChestCavityReceivedUpdatePacket(chestCavityEntity.getChestCavityInstance()));
    // });
    // ServerPlayNetworking.registerGlobalReceiver(CCNetworkingPackets.HOTKEY_PACKET_ID, (server,
    // player, handler, buf, sender) -> {
    //    Optional<ChestCavityEntity> optional = ChestCavityEntity.of(player);
    //    optional.ifPresent(chestCavityEntity ->
    // NetworkUtil.ReadChestCavityHotkeyPacket(chestCavityEntity.getChestCavityInstance(),buf));
    // });
  }

  private void registerReloadListeners(AddReloadListenerEvent event) {

    event.addListener(new OrganManager());
    event.addListener(new GeneratedChestCavityTypeManager());
    event.addListener(new GeneratedChestCavityAssignmentManager());
    event.addListener(new GuScriptLeafLoader());
    event.addListener(new GuScriptRuleLoader());
    event.addListener(new GuScriptFlowLoader());
    event.addListener(new net.tigereye.chestcavity.compat.guzhenren.gufang.GuFangRecipeLoader());
    // FX definitions are client-only; do not register on server reload
  }

  private void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
    event.registerReloadListener(new GuScriptLeafLoader());
    event.registerReloadListener(new GuScriptRuleLoader());
    event.registerReloadListener(new GeckoFxDefinitionLoader());
    event.registerReloadListener(new FxDefinitionLoader());
    // 新：飞剑视觉Profile（数据驱动渲染配置）
    event.registerReloadListener(
        new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.client
            .profile.SwordVisualProfileLoader());
    // 客户端：飞剑模型覆盖定义（Gecko/Item 渲染覆盖）
    event.registerReloadListener(
        new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.client
            .override.SwordModelOverrideLoader());
  }

  public static boolean isDebugMode() {
    return DEBUG_MODE;
  }

  public static void printOnDebug(String stringToPrint) {
    if (DEBUG_MODE) {
      System.out.println("DEBUG: " + stringToPrint);
    }
  }

  public static void printOnDebug(java.util.function.Supplier<String> supplier) {
    if (DEBUG_MODE) {
      System.out.println("DEBUG: " + supplier.get());
    }
  }

  private void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
    event.register(
        CCEntities.TEST_SOUL.get(),
        SpawnPlacementTypes.ON_GROUND,
        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
        net.tigereye.chestcavity.soul.entity.TestSoulEntity::checkSpawnRules,
        RegisterSpawnPlacementsEvent.Operation.REPLACE);
  }
}
