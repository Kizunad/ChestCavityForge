package net.tigereye.chestcavity.listeners;

import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.client.command.ModernUIClientCommands;
import net.tigereye.chestcavity.guscript.network.packets.FlowInputPayload;
import net.tigereye.chestcavity.guscript.network.packets.GuScriptOpenPayload;
import net.tigereye.chestcavity.guscript.network.packets.GuScriptTriggerPayload;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowInput;
import net.tigereye.chestcavity.guscript.ui.GuScriptScreen;
import net.tigereye.chestcavity.network.packets.ChestCavityHotkeyPayload;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.registration.CCKeybindings;
import net.tigereye.chestcavity.registration.CCOrganScores;
import net.tigereye.chestcavity.util.SingleplayerTickController;

public class KeybindingClientListeners {
  private static final float WOODEN_SHOVEL_DEBUG_TICK_RATE = 5.0F;
  private static boolean wasExecuteDown = false;

  public static void onClientTick(ClientTickEvent.Post event) {
    Player player = Minecraft.getInstance().player;
    while (CCKeybindings.GUSCRIPT_OPEN != null && CCKeybindings.GUSCRIPT_OPEN.consumeClick()) {
      if (player != null) {
        ChestCavity.LOGGER.info("[GuScript] Sending open request from client");
        sendOpenRequest();
      }
    }
    while (CCKeybindings.GUSCRIPT_EXECUTE != null
        && CCKeybindings.GUSCRIPT_EXECUTE.consumeClick()) {
      if (player != null) {
        sendTriggerRequest();
      }
    }
    while (CCKeybindings.GUSCRIPT_CANCEL != null && CCKeybindings.GUSCRIPT_CANCEL.consumeClick()) {
      if (player != null) {
        sendFlowInput(FlowInput.CANCEL);
      }
    }

    boolean executeDown =
        CCKeybindings.GUSCRIPT_EXECUTE != null && CCKeybindings.GUSCRIPT_EXECUTE.isDown();
    if (wasExecuteDown && !executeDown && player != null) {
      sendFlowInput(FlowInput.RELEASE);
    }
    wasExecuteDown = executeDown;

    while (CCKeybindings.MODERN_UI_CONFIG != null
        && CCKeybindings.MODERN_UI_CONFIG.consumeClick()) {
      if (player != null) {
        ModernUIClientCommands.openConfigViaHotkey();
      }
    }

    while (CCKeybindings.UTILITY_ABILITIES != null
        && CCKeybindings.UTILITY_ABILITIES.consumeClick()) {
      if (player != null) {
        for (ResourceLocation i : CCKeybindings.UTILITY_ABILITY_LIST) {
          sendHotkeyToServer(new ChestCavityHotkeyPayload(i));
        }
      }
    }
    while (CCKeybindings.ATTACK_ABILITIES != null
        && CCKeybindings.ATTACK_ABILITIES.consumeClick()) {
      if (player != null) {
        for (ResourceLocation i : CCKeybindings.ATTACK_ABILITY_LIST) {
          if (i.equals(ChestCavity.id("attack_abilities"))) {
            continue; // skip category id
          }
          sendHotkeyToServer(new ChestCavityHotkeyPayload(i));
        }
      }
    }

    checkSpecificKey(player, CCKeybindings.CREEPY, CCOrganScores.CREEPY);
    checkSpecificKey(player, CCKeybindings.DRAGON_BREATH, CCOrganScores.DRAGON_BREATH);
    checkSpecificKey(player, CCKeybindings.DRAGON_BOMBS, CCOrganScores.DRAGON_BOMBS);
    checkSpecificKey(player, CCKeybindings.FORCEFUL_SPIT, CCOrganScores.FORCEFUL_SPIT);
    checkSpecificKey(player, CCKeybindings.FURNACE_POWERED, CCOrganScores.FURNACE_POWERED);
    checkSpecificKey(player, CCKeybindings.IRON_REPAIR, CCOrganScores.IRON_REPAIR);
    checkSpecificKey(player, CCKeybindings.GHASTLY, CCOrganScores.GHASTLY);
    checkSpecificKey(player, CCKeybindings.GRAZING, CCOrganScores.GRAZING);
    checkSpecificKey(player, CCKeybindings.PYROMANCY, CCOrganScores.PYROMANCY);
    checkSpecificKey(player, CCKeybindings.SHULKER_BULLETS, CCOrganScores.SHULKER_BULLETS);
    checkSpecificKey(player, CCKeybindings.SILK, CCOrganScores.SILK);

    handleWoodenShovelDebugKeys(player);
  }

  private static void checkSpecificKey(Player player, KeyMapping keybinding, ResourceLocation id) {
    while (keybinding != null && keybinding.consumeClick()) {
      if (player != null) {
        sendHotkeyToServer(new ChestCavityHotkeyPayload(id));
      }
    }
  }

  private static void handleWoodenShovelDebugKeys(Player player) {
    if (!(player instanceof LocalPlayer localPlayer)) {
      return;
    }
    Optional<ChestCavityInstance> optional = CCAttachments.getExistingChestCavity(player);
    if (optional.isEmpty()) {
      return;
    }

    ChestCavityInstance cc = optional.get();
    if (!hasWoodenShovel(cc)) {
      return;
    }

    processWoodenShovelKey(
        CCKeybindings.WOODEN_SHOVEL_TICK_RATE, () -> attemptSetTickRate(localPlayer));
    processWoodenShovelKey(
        CCKeybindings.WOODEN_SHOVEL_FREEZE, () -> attemptSetFrozen(localPlayer, true));
    processWoodenShovelKey(
        CCKeybindings.WOODEN_SHOVEL_UNFREEZE, () -> attemptSetFrozen(localPlayer, false));
  }

  private static void processWoodenShovelKey(KeyMapping keyMapping, Runnable action) {
    while (keyMapping != null && keyMapping.consumeClick()) {
      action.run();
    }
  }

  private static void attemptSetTickRate(LocalPlayer player) {
    if (SingleplayerTickController.setTickRate(WOODEN_SHOVEL_DEBUG_TICK_RATE)) {
      player.displayClientMessage(
          Component.translatable(
              "text.chestcavity.wooden_shovel.tick_rate_set",
              String.format(Locale.ROOT, "%.2f", WOODEN_SHOVEL_DEBUG_TICK_RATE)),
          true);
    } else {
      showUnavailableMessage(player);
    }
  }

  private static void attemptSetFrozen(LocalPlayer player, boolean frozen) {
    if (SingleplayerTickController.setPaused(frozen)) {
      String key =
          frozen
              ? "text.chestcavity.wooden_shovel.freeze"
              : "text.chestcavity.wooden_shovel.unfreeze";
      player.displayClientMessage(Component.translatable(key), true);
    } else {
      showUnavailableMessage(player);
    }
  }

  private static void showUnavailableMessage(LocalPlayer player) {
    player.displayClientMessage(
        Component.translatable("text.chestcavity.wooden_shovel.unavailable"), true);
  }

  private static boolean hasWoodenShovel(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return false;
    }
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      if (cc.inventory.getItem(i).is(Items.WOODEN_SHOVEL)) {
        return true;
      }
    }
    return false;
  }

  private static void sendOpenRequest() {
    var connection = Minecraft.getInstance().getConnection();
    if (connection != null) {
      connection.send(new GuScriptOpenPayload());
    }
  }

  private static void sendTriggerRequest() {
    var minecraft = Minecraft.getInstance();
    var connection = minecraft.getConnection();
    if (connection == null) {
      return;
    }
    int pageIndex = -1;
    if (minecraft.screen instanceof GuScriptScreen screen) {
      pageIndex = screen.getMenu().getPageIndex();
    }
    connection.send(new GuScriptTriggerPayload(pageIndex));
  }

  private static void sendHotkeyToServer(ChestCavityHotkeyPayload payload) {
    var connection = Minecraft.getInstance().getConnection();
    if (connection != null) {
      connection.send(payload);
    }
  }

  private static void sendFlowInput(FlowInput input) {
    var connection = Minecraft.getInstance().getConnection();
    if (connection != null) {
      connection.send(new FlowInputPayload(input));
    }
  }
}
