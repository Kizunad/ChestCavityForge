package net.tigereye.chestcavity.client.modernui.skill;

import org.lwjgl.glfw.GLFW;

public enum SkillHotbarKey {
  R("R", "R", GLFW.GLFW_KEY_R),
  F("F", "F", GLFW.GLFW_KEY_F),
  G("G", "G", GLFW.GLFW_KEY_G),
  Z("Z", "Z", GLFW.GLFW_KEY_Z),
  X("X", "X", GLFW.GLFW_KEY_X),
  C("C", "C", GLFW.GLFW_KEY_C);

  private final String id;
  private final String label;
  private final int defaultKeyCode;

  SkillHotbarKey(String id, String label, int keyCode) {
    this.id = id;
    this.label = label;
    this.defaultKeyCode = keyCode;
  }

  public String id() {
    return id;
  }

  public String label() {
    return label;
  }

  public int defaultKeyCode() {
    return defaultKeyCode;
  }

  public static SkillHotbarKey fromId(String id) {
    if (id == null) {
      return null;
    }
    for (SkillHotbarKey key : values()) {
      if (key.id.equalsIgnoreCase(id)) {
        return key;
      }
    }
    return null;
  }
}
