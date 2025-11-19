package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Hun Dao 客户端事件处理器.
 *
 * <p>Listens to NeoForge client events (ClientTickEvent, RenderGuiEvent, etc.) and updates client
 * state, triggers FX, or renders HUD elements.
 *
 * <p>Phase 5: Client event handling decoupled from abilities registration.
 */
public final class HunDaoClientEvents {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * 每个客户端 tick 调用一次.
   *
   * <p>更新 HunDaoClientState 计时器，并在需要时触发周期性特效。
   *
   * @param event 客户端 tick 事件
   */
  @SubscribeEvent
  public static void onClientTick(ClientTickEvent.Post event) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null || mc.isPaused()) {
      return;
    }

    // Tick client state (decay timers)
    HunDaoClientState.instance().tick();

    // Optional: Trigger periodic client-side FX based on state
    // Example: Play ambient soul beast particles if soul beast is active
    LocalPlayer player = mc.player;
    if (player != null && HunDaoClientState.instance().isSoulBeastActive(player.getUUID())) {
      // Ambient FX handled by FxEngine, no client-side logic needed here
      // This is a placeholder for future client-only FX logic
    }
  }

  /**
   * 在客户端卸载某个世界时调用.
   *
   * <p>清除缓存的客户端状态，避免陈旧数据。
   *
   * @param event 世界卸载事件
   */
  @SubscribeEvent
  public static void onLevelUnload(LevelEvent.Unload event) {
    if (event.getLevel().isClientSide()) {
      LOGGER.debug("[hun_dao][client_events] Clearing client state on level unload");
      HunDaoClientState.instance().clearAll();
    }
  }

  /**
   * 在 GUI 渲染完成后调用.
   *
   * <p>预留用于绘制 Hun Dao HUD 覆盖层和通知。
   *
   * @param event GUI 渲染事件
   */
  @SubscribeEvent
  public static void onRenderGui(RenderGuiEvent.Post event) {
    // Phase 6 HUD/notification logic has been disabled per user request.
    // This handler remains registered to keep future extensions simple.
  }
}
