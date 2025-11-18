package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.modernui.tabs;

import icyllis.modernui.annotation.NonNull;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.HunDaoClientState;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.SoulRarity;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.SoulState;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.client.modernui.IHunDaoPanelTab;

/**
 * Soul Overview Tab implementation.
 *
 * <p>Phase 7: Displays soul state, level, rarity, max hun po, and attributes.
 */
@OnlyIn(Dist.CLIENT)
public class SoulOverviewTab implements IHunDaoPanelTab {

  @NonNull
  @Override
  public String getId() {
    return "soul_overview";
  }

  @NonNull
  @Override
  public String getTitle() {
    return "Soul Overview";
  }

  @NonNull
  @Override
  public String getFormattedContent() {
    Minecraft minecraft = Minecraft.getInstance();
    Player player = minecraft.player;

    if (player == null) {
      return "No player data available";
    }

    HunDaoClientState state = HunDaoClientState.instance();
    var playerId = player.getUUID();

    StringBuilder sb = new StringBuilder();
    sb.append("Soul Overview\n\n");

    // Check if soul system is active
    boolean isActive = state.isSoulSystemActive(playerId);
    if (!isActive) {
      sb.append("Soul System is Inactive\n");
      sb.append("→ Display fallback placeholder\n");
      sb.append("→ No crash, no missing data\n\n");
    }

    // Soul State
    sb.append("Soul State: ");
    sb.append(formatSoulState(state.getSoulState(playerId).orElse(null)));
    sb.append("\n");

    // Soul Level
    sb.append("Soul Level: ");
    sb.append(formatSoulLevel(state.getSoulLevel(playerId)));
    sb.append("\n");

    // Soul Rarity
    sb.append("Soul Rarity: ");
    sb.append(formatSoulRarity(state.getSoulRarity(playerId).orElse(null)));
    sb.append("\n");

    // Soul Max (Hun Po Max)
    sb.append("Soul Max: ");
    sb.append(formatSoulMax((int) state.getHunPoMax(playerId)));
    sb.append("\n\n");

    // Attributes
    sb.append("Attributes:\n");
    Map<String, Object> attributes = state.getSoulAttributes(playerId);
    if (attributes.isEmpty()) {
      sb.append("  - Attribute 1: --\n");
      sb.append("  - Attribute 2: --\n");
      sb.append("  - Attribute 3: --\n");
    } else {
      int count = 0;
      for (Map.Entry<String, Object> entry : attributes.entrySet()) {
        count++;
        sb.append("  - ");
        sb.append(entry.getKey());
        sb.append(": ");
        sb.append(entry.getValue());
        sb.append("\n");
      }
      // Fill in placeholders if less than 3 attributes
      while (count < 3) {
        count++;
        sb.append("  - Attribute ");
        sb.append(count);
        sb.append(": --\n");
      }
    }

    return sb.toString();
  }

  private String formatSoulState(SoulState state) {
    if (state == null) {
      return "Unknown";
    }
    return state.getDisplayName();
  }

  private String formatSoulLevel(int level) {
    return level > 0 ? String.valueOf(level) : "--";
  }

  private String formatSoulRarity(SoulRarity rarity) {
    if (rarity == null) {
      return "Unidentified";
    }
    return rarity.getDisplayName();
  }

  private String formatSoulMax(int max) {
    return max > 0 ? String.valueOf(max) : "--";
  }

  @Override
  public boolean isVisible() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
