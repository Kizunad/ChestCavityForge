package net.tigereye.chestcavity.soul.runtime;

import net.minecraft.world.phys.Vec3;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 面向 LLM / 脚本的线程安全指令通道。
 * <p>
 * - 外部系统调用 {@link #submit(UUID, String, Map)} 推送指令；
 * - 运行时处理器通过 {@link #poll(UUID)} 消费，并使用 {@link #publishResult(Result)} 写回执行结果；
 * - 外部系统可以调用 {@link #pollResult(UUID)} 或 {@link #getResult(UUID)} 查询执行结论；
 * - {@link #updateEnvironment(UUID, EnvironmentSnapshot)} 用于向外部暴露最新环境摘要（供提示词参考）。
 * </p>
 */
public final class SoulLLMInstructionChannel {

    private SoulLLMInstructionChannel() {}

    private static final Map<UUID, ConcurrentLinkedQueue<Instruction>> INSTRUCTIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, ConcurrentLinkedQueue<Result>> RESULTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Result> RESULT_INDEX = new ConcurrentHashMap<>();
    private static final Map<UUID, EnvironmentSnapshot> ENVIRONMENT = new ConcurrentHashMap<>();

    /** 推送一条指令，使用默认参数集。 */
    public static Instruction submit(UUID soulId, String command) {
        return submit(soulId, command, Map.of());
    }

    /**
     * 推送一条指令。
     *
     * @param soulId 目标魂魄 UUID
     * @param command 指令类型（大小写不敏感，例如 intent:combat / action:start）
     * @param parameters 额外参数，允许为空
     */
    public static Instruction submit(UUID soulId, String command, Map<String, String> parameters) {
        if (soulId == null) throw new IllegalArgumentException("soulId cannot be null");
        if (command == null || command.isBlank()) throw new IllegalArgumentException("command cannot be blank");
        UUID id = UUID.randomUUID();
        Map<String, String> params = parameters == null ? Map.of() : Map.copyOf(parameters);
        Instruction instruction = new Instruction(id, soulId, command.trim(), params, Instant.now().toEpochMilli());
        INSTRUCTIONS.computeIfAbsent(soulId, key -> new ConcurrentLinkedQueue<>()).add(instruction);
        return instruction;
    }

    /**
     * 运行时侧消费（移除并返回）下一条待处理指令。
     */
    public static Optional<Instruction> poll(UUID soulId) {
        if (soulId == null) return Optional.empty();
        ConcurrentLinkedQueue<Instruction> queue = INSTRUCTIONS.get(soulId);
        if (queue == null) return Optional.empty();
        Instruction next = queue.poll();
        if (next == null) {
            INSTRUCTIONS.remove(soulId, queue);
            return Optional.empty();
        }
        if (queue.isEmpty()) {
            INSTRUCTIONS.remove(soulId, queue);
        }
        return Optional.of(next);
    }

    /**
     * 将执行结果写回供外部查询。
     */
    public static void publishResult(Result result) {
        if (result == null) return;
        RESULT_INDEX.put(result.instructionId(), result);
        RESULTS.computeIfAbsent(result.soulId(), key -> new ConcurrentLinkedQueue<>()).add(result);
    }

    /**
     * 外部脚本查询指定魂魄的第一条未读结果（读取后即移除）。
     */
    public static Optional<Result> pollResult(UUID soulId) {
        if (soulId == null) return Optional.empty();
        ConcurrentLinkedQueue<Result> queue = RESULTS.get(soulId);
        if (queue == null) return Optional.empty();
        Result res = queue.poll();
        if (res == null) {
            RESULTS.remove(soulId, queue);
            return Optional.empty();
        }
        if (queue.isEmpty()) {
            RESULTS.remove(soulId, queue);
        }
        RESULT_INDEX.remove(res.instructionId(), res);
        return Optional.of(res);
    }

    /**
     * 外部脚本按指令 ID 查询执行结果（只读，不移除）。
     */
    public static Optional<Result> getResult(UUID instructionId) {
        if (instructionId == null) return Optional.empty();
        return Optional.ofNullable(RESULT_INDEX.get(instructionId));
    }

    /**
     * 外部脚本获取当前环境快照（若存在）。
     */
    public static Optional<EnvironmentSnapshot> getEnvironment(UUID soulId) {
        if (soulId == null) return Optional.empty();
        return Optional.ofNullable(ENVIRONMENT.get(soulId));
    }

    /**
     * 更新环境快照供外部使用。
     */
    public static void updateEnvironment(UUID soulId, EnvironmentSnapshot snapshot) {
        if (soulId == null || snapshot == null) return;
        ENVIRONMENT.put(soulId, snapshot);
    }

    /**
     * 指令结构体。
     */
    public record Instruction(UUID id, UUID soulId, String command, Map<String, String> parameters, long submittedAtMillis) {
        public Instruction {
            if (id == null) throw new IllegalArgumentException("instruction id cannot be null");
            if (soulId == null) throw new IllegalArgumentException("soulId cannot be null");
            if (command == null || command.isBlank()) throw new IllegalArgumentException("command cannot be blank");
            command = command.trim();
            parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        }

        public String parameter(String key) {
            if (key == null) return null;
            return parameters.get(key);
        }
    }

    /**
     * 指令执行结果。
     */
    public record Result(UUID instructionId, UUID soulId, Status status, String message, Map<String, String> metadata) {
        public Result {
            if (instructionId == null) throw new IllegalArgumentException("instructionId cannot be null");
            if (soulId == null) throw new IllegalArgumentException("soulId cannot be null");
            if (status == null) throw new IllegalArgumentException("status cannot be null");
            message = message == null ? "" : message;
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }

        public static Result success(Instruction instruction, String message) {
            return new Result(instruction.id(), instruction.soulId(), Status.SUCCESS, message, Map.of());
        }

        public static Result success(Instruction instruction, String message, Map<String, String> metadata) {
            return new Result(instruction.id(), instruction.soulId(), Status.SUCCESS, message, metadata);
        }

        public static Result ignored(Instruction instruction, String message) {
            return new Result(instruction.id(), instruction.soulId(), Status.IGNORED, message, Map.of());
        }

        public static Result error(Instruction instruction, String message) {
            return new Result(instruction.id(), instruction.soulId(), Status.ERROR, message, Map.of());
        }

        public static Result error(Instruction instruction, String message, Map<String, String> metadata) {
            return new Result(instruction.id(), instruction.soulId(), Status.ERROR, message, metadata);
        }
    }

    /**
     * 结果状态。
     */
    public enum Status {
        SUCCESS,
        IGNORED,
        ERROR
    }

    /**
     * 环境摘要（供提示词或脚本使用）。
     */
    public record EnvironmentSnapshot(long gameTime, String dimension, double x, double y, double z,
                                      float health, float maxHealth, Map<String, String> metadata) {
        public EnvironmentSnapshot {
            dimension = dimension == null ? "" : dimension;
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }

        public String toJson() {
            StringBuilder builder = new StringBuilder();
            builder.append('{');
            builder.append("\"tick\":").append(gameTime).append(',');
            builder.append("\"dimension\":\"").append(dimension.replace("\"", "\\\"")).append("\",");
            builder.append("\"position\":{")
                    .append("\"x\":").append(String.format(Locale.ROOT, "%.2f", x)).append(',')
                    .append("\"y\":").append(String.format(Locale.ROOT, "%.2f", y)).append(',')
                    .append("\"z\":").append(String.format(Locale.ROOT, "%.2f", z)).append('}').append(',');
            builder.append("\"health\":{")
                    .append("\"current\":").append(String.format(Locale.ROOT, "%.1f", health)).append(',')
                    .append("\"max\":").append(String.format(Locale.ROOT, "%.1f", maxHealth)).append('}');
            if (!metadata.isEmpty()) {
                builder.append(',').append("\"meta\":{");
                Iterator<Map.Entry<String, String>> it = metadata.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, String> entry = it.next();
                    builder.append('"').append(entry.getKey().replace("\"", "\\\"")).append('"')
                            .append(':')
                            .append('"').append(entry.getValue().replace("\"", "\\\"")).append('"');
                    if (it.hasNext()) builder.append(',');
                }
                builder.append('}');
            }
            builder.append('}');
            return builder.toString();
        }

        public static EnvironmentSnapshot of(long gameTime, String dimension, Vec3 position,
                                             float health, float maxHealth, Map<String, String> metadata) {
            return new EnvironmentSnapshot(gameTime, dimension,
                    position == null ? 0.0 : position.x(),
                    position == null ? 0.0 : position.y(),
                    position == null ? 0.0 : position.z(),
                    health, maxHealth, metadata);
        }
    }
}
