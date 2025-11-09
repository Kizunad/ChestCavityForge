package kizuna.guzhenren_event_ext;

import kizuna.guzhenren_event_ext.common.attachment.ModAttachments;
import kizuna.guzhenren_event_ext.common.config.Gamerules;
import kizuna.guzhenren_event_ext.common.config.ModConfig;
import kizuna.guzhenren_event_ext.common.event.CustomEventBus;
import kizuna.guzhenren_event_ext.common.system.EventManager;
import kizuna.guzhenren_event_ext.common.system.PlayerInventoryWatcher;
import kizuna.guzhenren_event_ext.common.system.PlayerStatWatcher;
import kizuna.guzhenren_event_ext.common.system.loader.EventLoader;
import kizuna.guzhenren_event_ext.common.system.registry.ActionRegistry;
import kizuna.guzhenren_event_ext.common.system.registry.ConditionRegistry;
import kizuna.guzhenren_event_ext.common.system.registry.TriggerRegistry;
import kizuna.guzhenren_event_ext.common.system_modules.actions.AdjustPlayerStatAction;
import kizuna.guzhenren_event_ext.common.system_modules.actions.RunCommandAction;
import kizuna.guzhenren_event_ext.common.system_modules.actions.SendMessageAction;
import kizuna.guzhenren_event_ext.common.system_modules.conditions.PlayerHealthPercentCondition;
import kizuna.guzhenren_event_ext.common.system_modules.conditions.RandomChanceCondition;
import kizuna.guzhenren_event_ext.common.system_modules.triggers.PlayerObtainedItemTrigger;
import kizuna.guzhenren_event_ext.common.system_modules.triggers.PlayerStatChangeTrigger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(GuzhenrenEventExtension.MODID)
public class GuzhenrenEventExtension {

  public static final String MODID = "guzhenren_event_ext";
  public static final Logger LOGGER = LogManager.getLogger();

  public GuzhenrenEventExtension(IEventBus modEventBus) {
    // 事件注册置于构造函数（符合本工程总线注册规范）
    modEventBus.addListener(this::onCommonSetup);

    // Register configuration
    ModLoadingContext.get().registerConfig(Type.SERVER, ModConfig.SPEC, MODID + "-server.toml");

    // Register our DeferredRegister to the mod event bus
    ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

    // Register event listeners to the Forge event bus
    NeoForge.EVENT_BUS.register(this); // For onAddReloadListener
    NeoForge.EVENT_BUS.register(PlayerStatWatcher.getInstance());
    NeoForge.EVENT_BUS.register(PlayerInventoryWatcher.getInstance());

    // Register the EventManager to our custom bus
    CustomEventBus.EVENT_BUS.register(EventManager.getInstance());
  }

  private void onCommonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(() -> {
      Gamerules.register();
      registerTriggersAndActions();
    });
    LOGGER.info("[{}] 初始化完成", MODID);
  }

  /**
   * Register all triggers, conditions, and actions for the event system
   */
  private void registerTriggersAndActions() {
    // Register triggers
    TriggerRegistry.getInstance().register("guzhenren_event_ext:player_obtained_item", new PlayerObtainedItemTrigger());
    TriggerRegistry.getInstance().register("guzhenren_event_ext:player_stat_change", new PlayerStatChangeTrigger());

    // Register conditions
    ConditionRegistry.getInstance().register("minecraft:random_chance", new RandomChanceCondition());
    ConditionRegistry.getInstance().register("guzhenren:player_health_percent", new PlayerHealthPercentCondition());

    // Register actions
    ActionRegistry.getInstance().register("guzhenren_event_ext:send_message", new SendMessageAction());
    ActionRegistry.getInstance().register("guzhenren_event_ext:run_command", new RunCommandAction());
    ActionRegistry.getInstance().register("guzhenren_event_ext:adjust_player_stat", new AdjustPlayerStatAction());

    LOGGER.info("[{}] Registered 2 triggers, 2 conditions, 3 actions", MODID);
  }

  @SubscribeEvent
  public void onAddReloadListener(AddReloadListenerEvent event) {
    event.addListener(EventLoader.getInstance());
  }
}
