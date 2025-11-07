package net.tigereye.chestcavity.playerprefs;

import java.util.function.BooleanSupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.registration.CCAttachments;

/** Utility accessor for player preference toggles. */
public final class PlayerPreferenceOps {

  public static final ResourceLocation SWORD_SLASH_BLOCK_BREAK =
      ChestCavity.id("player_prefs/sword_slash_block_break");

  public static final ResourceLocation ROU_BAIGU_PASSIVE_RESTORATION =
      ChestCavity.id("player_prefs/rou_baigu_passive");

  public static final ResourceLocation ROU_BAIGU_REQUIRE_EMPTY_SLOT =
      ChestCavity.id("player_prefs/rou_baigu_require_empty_slot");

  private PlayerPreferenceOps() {}

  public static boolean resolve(Player player, ResourceLocation key, BooleanSupplier defaultValue) {
    PlayerPreferenceSettings settings = CCAttachments.getPlayerPreferences(player);
    return settings.resolveBoolean(key, defaultValue);
  }

  public static void set(Player player, ResourceLocation key, boolean value) {
    CCAttachments.getPlayerPreferences(player).setBoolean(key, value);
  }

  public static boolean getCachedOrDefault(
      Player player, ResourceLocation key, BooleanSupplier defaultValue) {
    PlayerPreferenceSettings settings = CCAttachments.getPlayerPreferences(player);
    return settings.getBoolean(
        key, defaultValue == null ? false : defaultValue.getAsBoolean());
  }

  public static boolean defaultSwordSlashBlockBreak() {
    if (ChestCavity.config == null || ChestCavity.config.SWORD_SLASH == null) {
      return true;
    }
    return ChestCavity.config.SWORD_SLASH.enableBlockBreaking;
  }

  public static boolean defaultRouBaiguPassive() {
    if (ChestCavity.config == null) {
      return true;
    }
    return ChestCavity.config.GUZHENREN_ROU_BAIGU_PASSIVE_RESTORATION;
  }

  public static boolean defaultRouBaiguRequireEmpty() {
    if (ChestCavity.config == null) {
      return false;
    }
    return ChestCavity.config.GUZHENREN_ROU_BAIGU_REQUIRE_EMPTY_SLOT;
  }

  public static void ensureBootstrapped(Player player) {
    if (player == null) {
      return;
    }
    resolve(player, SWORD_SLASH_BLOCK_BREAK, PlayerPreferenceOps::defaultSwordSlashBlockBreak);
    resolve(player, ROU_BAIGU_PASSIVE_RESTORATION, PlayerPreferenceOps::defaultRouBaiguPassive);
    resolve(player, ROU_BAIGU_REQUIRE_EMPTY_SLOT, PlayerPreferenceOps::defaultRouBaiguRequireEmpty);
  }
}
