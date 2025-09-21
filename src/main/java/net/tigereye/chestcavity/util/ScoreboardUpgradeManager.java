package net.tigereye.chestcavity.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.registration.CCScoreboardUpgrades;

import java.util.List;

/**
 * Handles scoreboard-driven upgrades that inject permanent organs/items into the chest cavity.
 * The workflow is intentionally generic so future scoreboard triggers can plug into the same pipeline.
 */
public final class ScoreboardUpgradeManager {

    private ScoreboardUpgradeManager() {
    }

    private static final List<ScoreboardUpgrade> UPGRADES = CCScoreboardUpgrades.ALL;

    public static void applyAll(Player player, ChestCavityInstance cc) {
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        UPGRADES.forEach(upgrade -> applyUpgrade(player, cc, upgrade));
    }

    public static boolean isSlotLocked(ChestCavityInstance cc, int slotIndex) {
        for (ScoreboardUpgrade upgrade : UPGRADES) {
            if (upgrade.lockSlot() && cc.hasScoreboardUpgrade(upgrade.id()) && upgrade.slotIndex() == slotIndex) {
                return true;
            }
        }
        return false;
    }

    private static void applyUpgrade(Player player, ChestCavityInstance cc, ScoreboardUpgrade upgrade) {
        if (cc.hasScoreboardUpgrade(upgrade.id())) {
            ensureItemPresent(cc, upgrade);
            return;
        }
        if (!meetsScoreRequirement(player, upgrade)) {
            return;
        }
        if (upgrade.slotIndex() >= cc.inventory.getContainerSize()) {
            ChestCavity.LOGGER.warn("Scoreboard upgrade {} targets slot {} but inventory size is {}", upgrade.id(), upgrade.slotIndex(), cc.inventory.getContainerSize());
            cc.addScoreboardUpgrade(upgrade.id());
            return;
        }
        ItemStack generated = upgrade.itemFactory().create(cc);
        if (generated.isEmpty()) {
            ChestCavity.LOGGER.warn("Scoreboard upgrade {} could not generate item stack", upgrade.id());
            return;
        }
        cc.addScoreboardUpgrade(upgrade.id());
        if (cc.inventory.getItem(upgrade.slotIndex()).isEmpty()) {
            cc.inventory.setItem(upgrade.slotIndex(), generated.copy());
            cc.inventory.setChanged();
        }
    }

    private static boolean meetsScoreRequirement(Player player, ScoreboardUpgrade upgrade) {
        Scoreboard scoreboard = player.level().getScoreboard();
        Objective objective = scoreboard.getObjective(upgrade.objective());
        if (objective == null) {
            return false;
        }
        ScoreHolder holder = ScoreHolder.forNameOnly(player.getScoreboardName());
        ScoreAccess score = scoreboard.getOrCreatePlayerScore(holder, objective);
        return score.get() >= upgrade.minScore();
    }

    private static void ensureItemPresent(ChestCavityInstance cc, ScoreboardUpgrade upgrade) {
        if (upgrade.slotIndex() >= cc.inventory.getContainerSize()) {
            return;
        }
        if (cc.inventory.getItem(upgrade.slotIndex()).isEmpty()) {
            ItemStack generated = upgrade.itemFactory().create(cc);
            if (!generated.isEmpty()) {
                cc.inventory.setItem(upgrade.slotIndex(), generated.copy());
                cc.inventory.setChanged();
            }
        }
    }

    @FunctionalInterface
    public interface ItemStackGenerator {
        ItemStack create(ChestCavityInstance cc);
    }

    public record ScoreboardUpgrade(ResourceLocation id, String objective, int minScore,
                                    ItemStackGenerator itemFactory, int slotIndex, boolean lockSlot) {
    }
}
