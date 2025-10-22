package net.tigereye.chestcavity.guscript.data;

import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum ListenerType {
  ON_HIT("on_hit"),
  ON_FIRE("on_fire"),
  ON_GROUND("on_ground"),
  ON_ITEM_HELD("on_item_held");

  private final String translationKey;

  ListenerType(String translationKey) {
    this.translationKey = translationKey;
  }

  public ListenerType next() {
    return switch (this) {
      case ON_HIT -> ON_FIRE;
      case ON_FIRE -> ON_GROUND;
      case ON_GROUND -> ON_ITEM_HELD;
      case ON_ITEM_HELD -> ON_HIT;
    };
  }

  public Component label() {
    return Component.translatable("gui.chestcavity.guscript.listener." + translationKey);
  }

  public String getSerializedName() {
    return name().toLowerCase(Locale.ROOT);
  }

  public static ListenerType fromSerializedName(String name) {
    if (name == null || name.isBlank()) {
      return ON_HIT;
    }
    String normalized = name.toUpperCase(Locale.ROOT);
    for (ListenerType value : values()) {
      if (value.name().equals(normalized)) {
        return value;
      }
    }
    return ON_HIT;
  }

  public static ListenerType fromOrdinal(int ordinal) {
    ListenerType[] values = values();
    if (ordinal < 0 || ordinal >= values.length) {
      return ON_HIT;
    }
    return values[ordinal];
  }
}
