package net.tigereye.chestcavity.client.modernui.skill;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Immutable value object describing a single skill hotbar key binding. The binding stores the
 * primary key code along with modifier states so that the dispatcher can evaluate combinations like
 * {@code Ctrl + Alt + R}. This centralises the combination parsing logic and keeps persistence
 * straightforward.
 */
public final class SkillHotbarKeyBinding {

  public static final SkillHotbarKeyBinding UNBOUND =
      new SkillHotbarKeyBinding(GLFW.GLFW_KEY_UNKNOWN, false, false, false);

  private final int keyCode;
  private final boolean ctrl;
  private final boolean alt;
  private final boolean shift;

  private SkillHotbarKeyBinding(int keyCode, boolean ctrl, boolean alt, boolean shift) {
    this.keyCode = keyCode;
    this.ctrl = ctrl;
    this.alt = alt;
    this.shift = shift;
  }

  public static SkillHotbarKeyBinding of(int keyCode, boolean ctrl, boolean alt, boolean shift) {
    if (keyCode <= GLFW.GLFW_KEY_UNKNOWN) {
      return UNBOUND;
    }
    return new SkillHotbarKeyBinding(keyCode, ctrl, alt, shift);
  }

  public int keyCode() {
    return keyCode;
  }

  public boolean ctrl() {
    return ctrl;
  }

  public boolean alt() {
    return alt;
  }

  public boolean shift() {
    return shift;
  }

  /** Returns {@code true} when the binding contains a valid primary key. */
  public boolean isBound() {
    return keyCode > GLFW.GLFW_KEY_UNKNOWN;
  }

  /**
   * Unique identifier used by {@link net.tigereye.chestcavity.client.input.ModernUIKeyDispatcher}.
   */
  public String stateKey() {
    if (!isBound()) {
      return "unbound";
    }
    return keyCode + ":" + (ctrl ? 1 : 0) + (alt ? 1 : 0) + (shift ? 1 : 0);
  }

  /** Checks whether the binding is currently pressed within the given GLFW window. */
  public boolean isPressed(long windowHandle) {
    if (!isBound()) {
      return false;
    }
    if (!InputConstants.isKeyDown(windowHandle, keyCode)) {
      return false;
    }
    if (ctrl
        && !InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL)
        && !InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL)) {
      return false;
    }
    if (alt
        && !InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_LEFT_ALT)
        && !InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_RIGHT_ALT)) {
      return false;
    }
    if (shift
        && !InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT)
        && !InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
      return false;
    }
    if (!ctrl
        && (InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL)
            || InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL))) {
      return false;
    }
    if (!alt
        && (InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_LEFT_ALT)
            || InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_RIGHT_ALT))) {
      return false;
    }
    if (!shift
        && (InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT)
            || InputConstants.isKeyDown(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT))) {
      return false;
    }
    return true;
  }

  /**
   * Human readable representation of the binding. Modifiers are appended before the primary key to
   * match typical hotkey descriptions (for example {@code Ctrl + Shift + F}).
   */
  public String describe() {
    if (!isBound()) {
      return "未设置";
    }
    StringBuilder builder = new StringBuilder();
    if (ctrl) {
      builder.append("Ctrl + ");
    }
    if (alt) {
      builder.append("Alt + ");
    }
    if (shift) {
      builder.append("Shift + ");
    }
    InputConstants.Key key = InputConstants.getKey(keyCode, 0);
    Component displayName = key != null ? key.getDisplayName() : null;
    String keyName = displayName != null ? displayName.getString() : null;
    if (keyName == null || keyName.isEmpty()) {
      keyName = "键值 " + keyCode;
    }
    builder.append(keyName);
    return builder.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SkillHotbarKeyBinding other)) {
      return false;
    }
    return keyCode == other.keyCode
        && ctrl == other.ctrl
        && alt == other.alt
        && shift == other.shift;
  }

  @Override
  public int hashCode() {
    return Objects.hash(keyCode, ctrl, alt, shift);
  }

  @Override
  public String toString() {
    if (!isBound()) {
      return "SkillHotbarKeyBinding[UNBOUND]";
    }
    return String.format(
        Locale.ROOT,
        "SkillHotbarKeyBinding[key=%d,ctrl=%s,alt=%s,shift=%s]",
        keyCode,
        ctrl,
        alt,
        shift);
  }
}
