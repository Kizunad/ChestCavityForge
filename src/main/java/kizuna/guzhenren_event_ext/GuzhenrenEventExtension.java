package kizuna.guzhenren_event_ext;

import kizuna.guzhenren_event_ext.common.attachment.ModAttachments;
import kizuna.guzhenren_event_ext.common.config.Gamerules;
import kizuna.guzhenren_event_ext.common.event.CustomEventBus;
import kizuna.guzhenren_event_ext.common.system.EventManager;
import kizuna.guzhenren_event_ext.common.system.PlayerInventoryWatcher;
import kizuna.guzhenren_event_ext.common.system.PlayerStatWatcher;
import kizuna.guzhenren_event_ext.common.system.loader.EventLoader;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
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
    event.enqueueWork(Gamerules::register);
    LOGGER.info("[{}] 初始化完成", MODID);
  }

  @SubscribeEvent
  public void onAddReloadListener(AddReloadListenerEvent event) {
    event.addListener(EventLoader.getInstance());
  }
}
