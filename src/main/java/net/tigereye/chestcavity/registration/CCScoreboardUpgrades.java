package net.tigereye.chestcavity.registration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.util.ScoreboardUpgradeManager;
import net.tigereye.chestcavity.util.NBTWriter;

import java.util.List;
import java.util.Optional;

public final class CCScoreboardUpgrades {

    private static final ResourceLocation HOPE_GU_ITEM = ResourceLocation.parse("guzhenren:gucaikongqiao");

    public static final ScoreboardUpgradeManager.ScoreboardUpgrade HOPE_GU = new ScoreboardUpgradeManager.ScoreboardUpgrade(
            ChestCavity.id("hope_gu"),
            "kaiqiao",
            1,
            CCScoreboardUpgrades::createHopeGuStack,
            31,
            true
    );

    public static final List<ScoreboardUpgradeManager.ScoreboardUpgrade> ALL = List.of(HOPE_GU);

    private CCScoreboardUpgrades() {
    }

    private static ItemStack createHopeGuStack(ChestCavityInstance cc) {
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(HOPE_GU_ITEM);
        if (item.isEmpty()) {
            ChestCavity.LOGGER.warn("Scoreboard upgrade item {} is missing from registry", HOPE_GU_ITEM);
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item.get());
        if (cc.owner instanceof Player player) {
            NBTWriter.updateCustomData(stack, tag -> tag.putString("Owner", player.getScoreboardName()));
        }
        return stack;
    }
}
