package net.tigereye.chestcavity.client.input;

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
import net.tigereye.chestcavity.client.modernui.skill.SkillHotbarKeyBinding;
import net.tigereye.chestcavity.client.modernui.skill.SkillHotbarState;
import net.tigereye.chestcavity.client.ui.ModernUiClientState;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class ModernUIKeyDispatcher {

  private static final boolean DEBUG_ENABLED = Boolean.getBoolean("chestcavity.debugHotkeys");
  private static final int DEBUG_KEY = GLFW.GLFW_KEY_F8;
  private static final ResourceLocation DEBUG_SKILL_ID =
      ResourceLocation.fromNamespaceAndPath("guzhenren", "debug/f8_probe");

  private static final Map<String, Boolean> LAST_STATE = new HashMap<>();
  private static final SkillHotbarKeyBinding DEBUG_BINDING =
      SkillHotbarKeyBinding.of(DEBUG_KEY, false, false, false);

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
      SkillHotbarKeyBinding binding = SkillHotbarClientData.getBinding(slot);
      if (binding == null || !binding.isBound()) {
        continue;
      }
      List<ResourceLocation> skills = state.getSkills(slot);
      if (skills.isEmpty()) {
        continue;
      }
      triggeredAny = true;
      handleKey(minecraft, binding, slot.label(), skills);
    }

    if (!triggeredAny && DEBUG_ENABLED) {
      handleKey(minecraft, DEBUG_BINDING, "F8", List.of(DEBUG_SKILL_ID));
    }
  }

  private static void handleKey(
      Minecraft minecraft,
      SkillHotbarKeyBinding binding,
      String label,
      List<ResourceLocation> skills) {
    if (binding == null || !binding.isBound()) {
      return;
    }
    long windowHandle = minecraft.getWindow().getWindow();
    boolean down = binding.isPressed(windowHandle);
    String stateKey = binding.stateKey();
    boolean wasDown = LAST_STATE.getOrDefault(stateKey, Boolean.FALSE);
    if (!down) {
      LAST_STATE.put(stateKey, Boolean.FALSE);
      return;
    }
    if (wasDown) {
      return;
    }
    LAST_STATE.put(stateKey, Boolean.TRUE);

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

  public static void onKeyRebind(
      SkillHotbarKeyBinding oldBinding, SkillHotbarKeyBinding newBinding) {
    if (oldBinding != null && oldBinding.isBound()) {
      LAST_STATE.remove(oldBinding.stateKey());
    }
    if (newBinding != null && newBinding.isBound()) {
      LAST_STATE.remove(newBinding.stateKey());
    }
  }

  public static void resetKeyStates() {
    LAST_STATE.clear();
  }

  public static boolean isDebugEnabled() {
    return DEBUG_ENABLED;
  }
}
