package net.tigereye.chestcavity.registration;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.util.NBTWriter;
import net.tigereye.chestcavity.util.ScoreboardUpgradeManager;

public final class CCScoreboardUpgrades {

  private static final ResourceLocation KONGQIAO_ITEM =
      ResourceLocation.parse("guzhenren:gucaikongqiao");

  public static final ScoreboardUpgradeManager.ScoreboardUpgrade KONGQIAO =
      new ScoreboardUpgradeManager.ScoreboardUpgrade(
          ChestCavity.id("kongqiao"),
          "kaiqiao",
          1,
          CCScoreboardUpgrades::createKongQiaoStack,
          31,
          true);

  public static final List<ScoreboardUpgradeManager.ScoreboardUpgrade> ALL = List.of(KONGQIAO);

  private CCScoreboardUpgrades() {}

  private static ItemStack createKongQiaoStack(ChestCavityInstance cc) {
    Optional<Item> item = BuiltInRegistries.ITEM.getOptional(KONGQIAO_ITEM);
    if (item.isEmpty()) {
      ChestCavity.LOGGER.warn("Scoreboard upgrade item {} is missing from registry", KONGQIAO_ITEM);
      return ItemStack.EMPTY;
    }
    ItemStack stack = new ItemStack(item.get());
    if (cc.owner instanceof Player player) {
      NBTWriter.updateCustomData(stack, tag -> tag.putString("Owner", player.getScoreboardName()));
    }
    return stack;
  }
}
