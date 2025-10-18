package net.tigereye.chestcavity.soul.runtime;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.fakeplayer.actions.api.Action;
import net.tigereye.chestcavity.soul.fakeplayer.actions.registry.ActionRegistry;
import net.tigereye.chestcavity.soul.fakeplayer.actions.state.ActionStateManager;
import net.tigereye.chestcavity.soul.fakeplayer.brain.BrainController;
import net.tigereye.chestcavity.soul.fakeplayer.brain.intent.BrainIntent;
import net.tigereye.chestcavity.soul.fakeplayer.brain.intent.CombatIntent;
import net.tigereye.chestcavity.soul.fakeplayer.brain.intent.CombatStyle;
import net.tigereye.chestcavity.soul.fakeplayer.brain.intent.FollowIntent;
import net.tigereye.chestcavity.soul.fakeplayer.brain.intent.HoldIntent;
import net.tigereye.chestcavity.soul.registry.SoulRuntimeHandler;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 将 LLM 指令翻译为大脑意图或动作调用的运行时处理器。
 */
public final class SoulLLMControlHandler implements SoulRuntimeHandler {

    @Override
    public void onTickEnd(SoulPlayer player) {
        if (player.level().isClientSide) return;
        ActionStateManager manager = ActionStateManager.of(player);
        ServerPlayer owner = player.getOwnerId()
                .map(id -> player.serverLevel().getServer().getPlayerList().getPlayer(id))
                .orElse(null);
        while (true) {
            Optional<SoulLLMInstructionChannel.Instruction> maybe = SoulLLMInstructionChannel.poll(player.getUUID());
            if (maybe.isEmpty()) {
                break;
            }
            SoulLLMInstructionChannel.Instruction instruction = maybe.get();
            SoulLLMInstructionChannel.Result result;
            try {
                result = processInstruction(player, owner, manager, instruction);
            } catch (Exception ex) {
                result = SoulLLMInstructionChannel.Result.error(instruction,
                        "exception: " + ex.getClass().getSimpleName());
            }
            SoulLLMInstructionChannel.publishResult(result);
        }
    }

    private SoulLLMInstructionChannel.Result processInstruction(SoulPlayer player, ServerPlayer owner,
                                                                ActionStateManager manager,
                                                                SoulLLMInstructionChannel.Instruction instruction) {
        String normalized = normalize(instruction.command());
        return switch (normalized) {
            case "intent:combat" -> applyCombatIntent(player, instruction);
            case "intent:follow" -> applyFollowIntent(player, owner, instruction);
            case "intent:hold" -> applyHoldIntent(player, instruction);
            case "intent:clear" -> clearIntent(player, instruction);
            case "action:start" -> startAction(player, owner, manager, instruction);
            case "action:cancel" -> cancelAction(player, owner, manager, instruction);
            default -> SoulLLMInstructionChannel.Result.ignored(instruction, "unknown_command: " + normalized);
        };
    }

    private SoulLLMInstructionChannel.Result applyCombatIntent(SoulPlayer player,
                                                               SoulLLMInstructionChannel.Instruction instruction) {
        String styleParam = optional(instruction, "style", "force_fight");
        CombatStyle style = parseCombatStyle(styleParam);
        if (style == null) {
            return SoulLLMInstructionChannel.Result.error(instruction, "invalid_combat_style:" + styleParam);
        }
        UUID focus = parseUUID(optional(instruction, "target", null));
        int ttl = parseInt(optional(instruction, "ttl", null), 200);
        BrainIntent intent = new CombatIntent(style, focus, ttl);
        BrainController.get().pushIntent(player.getUUID(), intent);
        Map<String, String> meta = Map.of(
                "intent", "combat",
                "style", style.name(),
                "ttl", Integer.toString(ttl)
        );
        return SoulLLMInstructionChannel.Result.success(instruction, "combat_intent_set", meta);
    }

    private SoulLLMInstructionChannel.Result applyFollowIntent(SoulPlayer player, ServerPlayer owner,
                                                               SoulLLMInstructionChannel.Instruction instruction) {
        UUID target = parseFollowTarget(optional(instruction, "target", "owner"), owner);
        if (target == null) {
            return SoulLLMInstructionChannel.Result.error(instruction, "follow_target_missing");
        }
        double distance = parseDouble(optional(instruction, "distance", null), 3.0);
        int ttl = parseInt(optional(instruction, "ttl", null), 200);
        BrainIntent intent = new FollowIntent(target, distance, ttl);
        BrainController.get().pushIntent(player.getUUID(), intent);
        Map<String, String> meta = Map.of(
                "intent", "follow",
                "target", target.toString(),
                "distance", Double.toString(distance)
        );
        return SoulLLMInstructionChannel.Result.success(instruction, "follow_intent_set", meta);
    }

    private SoulLLMInstructionChannel.Result applyHoldIntent(SoulPlayer player,
                                                             SoulLLMInstructionChannel.Instruction instruction) {
        Vec3 anchor = parseAnchor(player.position(), instruction.parameters());
        int ttl = parseInt(optional(instruction, "ttl", null), 200);
        BrainIntent intent = new HoldIntent(anchor, ttl);
        BrainController.get().pushIntent(player.getUUID(), intent);
        Map<String, String> meta = Map.of(
                "intent", "hold",
                "x", String.format(Locale.ROOT, "%.2f", anchor.x()),
                "y", String.format(Locale.ROOT, "%.2f", anchor.y()),
                "z", String.format(Locale.ROOT, "%.2f", anchor.z())
        );
        return SoulLLMInstructionChannel.Result.success(instruction, "hold_intent_set", meta);
    }

    private SoulLLMInstructionChannel.Result clearIntent(SoulPlayer player,
                                                         SoulLLMInstructionChannel.Instruction instruction) {
        BrainController.get().clearIntents(player.getUUID());
        return SoulLLMInstructionChannel.Result.success(instruction, "intent_cleared");
    }

    private SoulLLMInstructionChannel.Result startAction(SoulPlayer player, ServerPlayer owner,
                                                         ActionStateManager manager,
                                                         SoulLLMInstructionChannel.Instruction instruction) {
        String actionId = optional(instruction, "action", null);
        if (actionId == null) {
            return SoulLLMInstructionChannel.Result.error(instruction, "action_id_missing");
        }
        ResourceLocation id = ResourceLocation.tryParse(actionId);
        if (id == null) {
            return SoulLLMInstructionChannel.Result.error(instruction, "invalid_action_id:" + actionId);
        }
        Action action = ActionRegistry.resolveOrCreate(id);
        if (action == null) {
            return SoulLLMInstructionChannel.Result.error(instruction, "action_not_found:" + id);
        }
        boolean started = manager.tryStart(player.serverLevel(), player, action, owner);
        if (!started) {
            return SoulLLMInstructionChannel.Result.ignored(instruction, "action_not_started");
        }
        Map<String, String> meta = Map.of("action", id.toString());
        return SoulLLMInstructionChannel.Result.success(instruction, "action_started", meta);
    }

    private SoulLLMInstructionChannel.Result cancelAction(SoulPlayer player, ServerPlayer owner,
                                                          ActionStateManager manager,
                                                          SoulLLMInstructionChannel.Instruction instruction) {
        String actionId = optional(instruction, "action", null);
        if (actionId == null) {
            return SoulLLMInstructionChannel.Result.error(instruction, "action_id_missing");
        }
        ResourceLocation id = ResourceLocation.tryParse(actionId);
        if (id == null) {
            return SoulLLMInstructionChannel.Result.error(instruction, "invalid_action_id:" + actionId);
        }
        Action action = ActionRegistry.resolveOrCreate(id);
        if (action == null) {
            return SoulLLMInstructionChannel.Result.error(instruction, "action_not_found:" + id);
        }
        boolean wasActive = manager.isActive(action.id());
        manager.cancel(player.serverLevel(), player, action, owner);
        if (!wasActive) {
            return SoulLLMInstructionChannel.Result.ignored(instruction, "action_not_active");
        }
        Map<String, String> meta = Map.of("action", id.toString());
        return SoulLLMInstructionChannel.Result.success(instruction, "action_cancelled", meta);
    }

    private static String normalize(String command) {
        return command == null ? "" : command.trim().toLowerCase(Locale.ROOT);
    }

    private static String optional(SoulLLMInstructionChannel.Instruction instruction, String key, String def) {
        String value = instruction.parameter(key);
        return value == null ? def : value;
    }

    private static CombatStyle parseCombatStyle(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return CombatStyle.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static UUID parseFollowTarget(String raw, ServerPlayer owner) {
        if (raw == null || raw.isBlank()) {
            return owner == null ? null : owner.getUUID();
        }
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        if ((lower.equals("owner") || lower.equals("master") || lower.equals("leader")) && owner != null) {
            return owner.getUUID();
        }
        return parseUUID(raw);
    }

    private static Vec3 parseAnchor(Vec3 fallback, Map<String, String> params) {
        if (params == null || params.isEmpty()) return fallback;
        double x = parseDouble(params.get("x"), fallback.x());
        double y = parseDouble(params.get("y"), fallback.y());
        double z = parseDouble(params.get("z"), fallback.z());
        return new Vec3(x, y, z);
    }

    private static int parseInt(String value, int def) {
        if (value == null || value.isBlank()) return def;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return def;
        }
    }

    private static double parseDouble(String value, double def) {
        if (value == null || value.isBlank()) return def;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return def;
        }
    }

    private static UUID parseUUID(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
