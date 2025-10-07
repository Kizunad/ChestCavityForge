package net.tigereye.chestcavity.guscript.runtime.flow.actions;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowEdgeAction;

import java.util.Locale;

final class ScoreboardFlowActions {

    private ScoreboardFlowActions() {
    }

    static FlowEdgeAction set(String objectiveName, int value, String playerName) {
        String trimmedObjective = objectiveName == null ? "" : objectiveName.trim();
        String trimmedPlayer = playerName == null ? "" : playerName.trim();
        if (trimmedObjective.isEmpty()) {
            return FlowActionUtils.describe(() -> "scoreboard_set(invalid_objective)" );
        }
        int clampedValue = value;
        return new FlowEdgeAction() {
            @Override
            public void apply(Player performer, LivingEntity target, FlowController controller, long gameTime) {
                if (!(performer instanceof ServerPlayer serverPlayer)) {
                    ChestCavity.LOGGER.debug("[Flow] scoreboard_set skipped: performer is not ServerPlayer ({}).", performer == null ? "null" : performer.getClass().getSimpleName());
                    return;
                }
                Scoreboard scoreboard = serverPlayer.getServer().getScoreboard();
                Objective objective = scoreboard.getObjective(trimmedObjective);
                if (objective == null) {
                    ChestCavity.LOGGER.warn("[Flow] scoreboard_set failed: objective '{}' not found for player {}", trimmedObjective, serverPlayer.getScoreboardName());
                    return;
                }
                String entry = resolveEntryName(serverPlayer, trimmedPlayer);
                String entryArgument = formatEntry(entry);
                String command = String.format(Locale.ROOT, "scoreboard players set %s %s %d", entryArgument, trimmedObjective, clampedValue);
                CommandSourceStack source = serverPlayer.createCommandSourceStack().withPermission(2).withSuppressedOutput();
                serverPlayer.getServer().getCommands().performPrefixedCommand(source, command);
                ChestCavity.LOGGER.debug("[Flow] scoreboard_set objective='{}' entry='{}' value={} by {}", trimmedObjective, entry, clampedValue, serverPlayer.getScoreboardName());
            }

            @Override
            public String describe() {
                return String.format(Locale.ROOT, "scoreboard_set(objective=%s,value=%d,player=%s)", trimmedObjective, clampedValue, trimmedPlayer.isEmpty() ? "<performer>" : trimmedPlayer);
            }
        };
    }

    private static String formatEntry(String entry) {
        if (entry.indexOf(' ') >= 0 || entry.indexOf('"') >= 0) {
            String escaped = entry.replace("\\", "\\\\").replace("\"", "\\\"");
            return "\"" + escaped + "\"";
        }
        return entry;
    }

    private static String resolveEntryName(ServerPlayer performer, String requested) {
        if (requested != null && !requested.isBlank()) {
            return requested;
        }
        if (performer instanceof net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer soul) {
            var ownerId = soul.getOwnerId().orElse(null);
            if (ownerId != null) {
                ServerPlayer owner = soul.serverLevel().getServer().getPlayerList().getPlayer(ownerId);
                if (owner != null) {
                    return owner.getScoreboardName();
                }
            }
        }
        return performer.getScoreboardName();
    }
}
