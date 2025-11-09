package kizuna.guzhenren_event_ext;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(GuzhenrenEventExtension.MODID)
public class GuzhenrenEventExtension {

  public static final String MODID = "guzhenren_event_ext";
  public static final Logger LOGGER = LogManager.getLogger();

  public GuzhenrenEventExtension(IEventBus modEventBus) {
    // 事件注册置于构造函数（符合本工程总线注册规范）
    modEventBus.addListener(this::onCommonSetup);
  }

  private void onCommonSetup(FMLCommonSetupEvent event) {
    LOGGER.info("[{}] 初始化完成", MODID);
  }
}

