package net.tigereye.chestcavity.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.client.modernui.network.ActiveSkillTriggerPayload;
import net.tigereye.chestcavity.client.modernui.skill.SkillHotbarClientData;
import net.tigereye.chestcavity.client.modernui.skill.SkillHotbarKey;
import net.tigereye.chestcavity.client.modernui.skill.SkillHotbarState;
import net.tigereye.chestcavity.client.ui.ModernUiClientState;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class ModernUIKeyDispatcher {

  private static final boolean DEBUG_ENABLED = Boolean.getBoolean("chestcavity.debugHotkeys");
  private static final int DEBUG_KEY = GLFW.GLFW_KEY_F8;
  private static final ResourceLocation DEBUG_SKILL_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "debug/f8_probe");

  private static final Map<Integer, Boolean> LAST_STATE = new HashMap<>();

  private ModernUIKeyDispatcher() {}

  public static void onClientTick(ClientTickEvent.Post event) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft == null) {
      return;
    }

    SkillHotbarClientData.tick(minecraft);

    if (minecraft.player == null || minecraft.level == null) {
      return;
    }

    long windowHandle = minecraft.getWindow().getWindow();
    boolean consumedCapture = SkillHotbarClientData.processCapture(windowHandle);
    if (SkillHotbarClientData.isCapturing() || consumedCapture) {
      return;
    }

    if (!ModernUiClientState.isKeyListenEnabled()) {
      return;
    }
    if (minecraft.isPaused() || minecraft.screen != null) {
      return;
    }

    SkillHotbarState state = SkillHotbarClientData.state();
    boolean triggeredAny = false;
    for (SkillHotbarKey slot : SkillHotbarKey.values()) {
      int keyCode = SkillHotbarClientData.getKeyCode(slot);
      if (keyCode <= GLFW.GLFW_KEY_UNKNOWN) {
        continue;
      }
      List<ResourceLocation> skills = state.getSkills(slot);
      if (skills.isEmpty()) {
        continue;
      }
      triggeredAny = true;
      handleKey(minecraft, keyCode, slot.label(), skills);
    }

    if (!triggeredAny && DEBUG_ENABLED) {
      handleKey(minecraft, DEBUG_KEY, "F8", List.of(DEBUG_SKILL_ID));
    }
  }

  private static void handleKey(
      Minecraft minecraft, int keyCode, String label, List<ResourceLocation> skills) {
    long windowHandle = minecraft.getWindow().getWindow();
    boolean down = InputConstants.isKeyDown(windowHandle, keyCode);
    boolean wasDown = LAST_STATE.getOrDefault(keyCode, Boolean.FALSE);
    if (!down) {
      LAST_STATE.put(keyCode, Boolean.FALSE);
      return;
    }
    if (wasDown) {
      return;
    }
    LAST_STATE.put(keyCode, Boolean.TRUE);

    if (minecraft.getConnection() != null) {
      minecraft.execute(
          () -> {
            if (minecraft.getConnection() == null) {
              return;
            }
            for (ResourceLocation id : skills) {
              minecraft.getConnection().send(new ActiveSkillTriggerPayload(id));
            }
          });
    }

    if (DEBUG_ENABLED || ChestCavity.isDebugMode()) {
      ChestCavity.LOGGER.info("[modernui][hotkey] {} pressed (no GUI)", label);
    }
  }

  public static void onKeyRebind(int oldKeyCode, int newKeyCode) {
    if (oldKeyCode != newKeyCode) {
      LAST_STATE.remove(oldKeyCode);
    }
    LAST_STATE.remove(newKeyCode);
  }

  public static void resetKeyStates() {
    LAST_STATE.clear();
  }

  public static boolean isDebugEnabled() {
    return DEBUG_ENABLED;
  }
}
