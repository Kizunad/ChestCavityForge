package net.tigereye.chestcavity.guscript.command;

import com.google.common.collect.ImmutableMultiset;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tigereye.chestcavity.guscript.GuScriptModule;
import net.tigereye.chestcavity.guscript.ability.AbilityFxDispatcher;
import net.tigereye.chestcavity.guscript.actions.ConsumeHealthAction;
import net.tigereye.chestcavity.guscript.actions.ConsumeZhenyuanAction;
import net.tigereye.chestcavity.guscript.actions.EmitProjectileAction;
import net.tigereye.chestcavity.guscript.ast.GuNode;
import net.tigereye.chestcavity.guscript.ast.GuNodeKind;
import net.tigereye.chestcavity.guscript.ast.LeafGuNode;
import net.tigereye.chestcavity.guscript.ast.OperatorGuNode;
import net.tigereye.chestcavity.guscript.runtime.action.DefaultGuScriptExecutionBridge;
import net.tigereye.chestcavity.guscript.runtime.exec.DefaultGuScriptContext;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptRuntime;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowControllerManager;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgram;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgramRegistry;
import net.tigereye.chestcavity.guscript.runtime.reduce.GuScriptReducer;
import net.tigereye.chestcavity.guscript.runtime.reduce.ReactionRule;

public final class GuScriptCommands {

  private GuScriptCommands() {}

  public static void register(RegisterCommandsEvent event) {
    CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
    dispatcher.register(
        Commands.literal("guscript")
            .requires(stack -> stack.hasPermission(0))
            .then(Commands.literal("run").executes(GuScriptCommands::runDemo))
            .then(
                Commands.literal("flow")
                    .then(
                        Commands.literal("start")
                            .then(
                                Commands.argument("id", ResourceLocationArgument.id())
                                    .suggests(GuScriptCommands::suggestFlowIds)
                                    .executes(ctx -> startFlow(ctx, 1.0D, Map.of()))
                                    .then(
                                        Commands.argument(
                                                "timeScale",
                                                DoubleArgumentType.doubleArg(0.0D, 100.0D))
                                            .executes(
                                                ctx ->
                                                    startFlow(
                                                        ctx,
                                                        clampTimeScale(
                                                            DoubleArgumentType.getDouble(
                                                                ctx, "timeScale")),
                                                        Map.of()))
                                            .then(
                                                Commands.argument(
                                                        "params", StringArgumentType.greedyString())
                                                    .executes(
                                                        ctx ->
                                                            startFlow(
                                                                ctx,
                                                                clampTimeScale(
                                                                    DoubleArgumentType.getDouble(
                                                                        ctx, "timeScale")),
                                                                parseFlowParams(
                                                                    StringArgumentType.getString(
                                                                        ctx, "params"))))))
                                    .then(
                                        Commands.argument(
                                                "params", StringArgumentType.greedyString())
                                            .executes(
                                                ctx ->
                                                    startFlow(
                                                        ctx,
                                                        1.0D,
                                                        parseFlowParams(
                                                            StringArgumentType.getString(
                                                                ctx, "params"))))))))
            .then(
                Commands.literal("fx")
                    .then(
                        Commands.literal("play")
                            .then(
                                Commands.argument("id", ResourceLocationArgument.id())
                                    .suggests(GuScriptCommands::suggestFxIds)
                                    .executes(ctx -> playFxHere(ctx, DEFAULT_INTENSITY))
                                    .then(
                                        Commands.argument(
                                                "intensity",
                                                FloatArgumentType.floatArg(0.0F, 16.0F))
                                            .executes(
                                                ctx ->
                                                    playFxHere(
                                                        ctx,
                                                        FloatArgumentType.getFloat(
                                                            ctx, "intensity"))))))
                    .then(
                        Commands.literal("play_at")
                            .then(
                                Commands.argument("id", ResourceLocationArgument.id())
                                    .suggests(GuScriptCommands::suggestFxIds)
                                    .then(
                                        Commands.argument("pos", Vec3Argument.vec3())
                                            .executes(ctx -> playFxAt(ctx, DEFAULT_INTENSITY))
                                            .then(
                                                Commands.argument(
                                                        "intensity",
                                                        FloatArgumentType.floatArg(0.0F, 16.0F))
                                                    .executes(
                                                        ctx ->
                                                            playFxAt(
                                                                ctx,
                                                                FloatArgumentType.getFloat(
                                                                    ctx, "intensity")))))))
                    .then(
                        Commands.literal("list")
                            .executes(ctx -> listFx(ctx, ""))
                            .then(
                                Commands.argument("filter", StringArgumentType.word())
                                    .executes(
                                        ctx ->
                                            listFx(
                                                ctx,
                                                StringArgumentType.getString(ctx, "filter"))))))
            .then(
                Commands.literal("sound")
                    .then(
                        Commands.literal("play")
                            .then(
                                Commands.argument("id", ResourceLocationArgument.id())
                                    .suggests(GuScriptCommands::suggestSoundIds)
                                    .executes(
                                        ctx ->
                                            playSoundHere(
                                                ctx, DEFAULT_SOUND_VOLUME, DEFAULT_SOUND_PITCH))
                                    .then(
                                        Commands.argument(
                                                "volume", FloatArgumentType.floatArg(0.0F, 16.0F))
                                            .executes(
                                                ctx ->
                                                    playSoundHere(
                                                        ctx,
                                                        FloatArgumentType.getFloat(ctx, "volume"),
                                                        DEFAULT_SOUND_PITCH))
                                            .then(
                                                Commands.argument(
                                                        "pitch",
                                                        FloatArgumentType.floatArg(0.1F, 4.0F))
                                                    .executes(
                                                        ctx ->
                                                            playSoundHere(
                                                                ctx,
                                                                FloatArgumentType.getFloat(
                                                                    ctx, "volume"),
                                                                FloatArgumentType.getFloat(
                                                                    ctx, "pitch")))))))
                    .then(
                        Commands.literal("play_at")
                            .then(
                                Commands.argument("id", ResourceLocationArgument.id())
                                    .suggests(GuScriptCommands::suggestSoundIds)
                                    .then(
                                        Commands.argument("pos", Vec3Argument.vec3())
                                            .executes(
                                                ctx ->
                                                    playSoundAt(
                                                        ctx,
                                                        DEFAULT_SOUND_VOLUME,
                                                        DEFAULT_SOUND_PITCH))
                                            .then(
                                                Commands.argument(
                                                        "volume",
                                                        FloatArgumentType.floatArg(0.0F, 16.0F))
                                                    .executes(
                                                        ctx ->
                                                            playSoundAt(
                                                                ctx,
                                                                FloatArgumentType.getFloat(
                                                                    ctx, "volume"),
                                                                DEFAULT_SOUND_PITCH))
                                                    .then(
                                                        Commands.argument(
                                                                "pitch",
                                                                FloatArgumentType.floatArg(
                                                                    0.1F, 4.0F))
                                                            .executes(
                                                                ctx ->
                                                                    playSoundAt(
                                                                        ctx,
                                                                        FloatArgumentType.getFloat(
                                                                            ctx, "volume"),
                                                                        FloatArgumentType.getFloat(
                                                                            ctx, "pitch"))))))))));
  }

  private static final float DEFAULT_INTENSITY = 1.0F;

  private static final float DEFAULT_SOUND_VOLUME = 1.0F;
  private static final float DEFAULT_SOUND_PITCH = 1.0F;

  private static final List<ResourceLocation> BUILTIN_FX_IDS =
      List.of(
          ResourceLocation.parse("chestcavity:time_accel_enter"),
          ResourceLocation.parse("chestcavity:time_accel_loop"),
          ResourceLocation.parse("chestcavity:time_accel_exit"),
          ResourceLocation.parse("chestcavity:mind_thoughts_orbit"),
          ResourceLocation.parse("chestcavity:mind_thoughts_pulse"),
          ResourceLocation.parse("chestcavity:fire_huo_yi"));

  private static final List<String> BUILTIN_FX_ID_STRINGS =
      BUILTIN_FX_IDS.stream().map(ResourceLocation::toString).sorted().toList();

  private static CompletableFuture<Suggestions> suggestFxIds(
      CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
    return SharedSuggestionProvider.suggest(BUILTIN_FX_ID_STRINGS, builder);
  }

  private static final List<ResourceLocation> BUILTIN_SOUND_IDS =
      List.of(ResourceLocation.parse("chestcavity:custom.sword.break_air"));

  private static final List<String> BUILTIN_SOUND_ID_STRINGS =
      BUILTIN_SOUND_IDS.stream().map(ResourceLocation::toString).sorted().toList();

  private static CompletableFuture<Suggestions> suggestSoundIds(
      CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
    return SharedSuggestionProvider.suggest(BUILTIN_SOUND_ID_STRINGS, builder);
  }

  private static CompletableFuture<Suggestions> suggestFlowIds(
      CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
    List<String> ids =
        FlowProgramRegistry.ids().stream().map(ResourceLocation::toString).sorted().toList();
    return SharedSuggestionProvider.suggest(ids, builder);
  }

  private static int playFxHere(CommandContext<CommandSourceStack> ctx, float intensity) {
    CommandSourceStack source = ctx.getSource();
    ServerPlayer performer;
    try {
      performer = source.getPlayerOrException();
    } catch (CommandSyntaxException e) {
      source.sendFailure(Component.literal("需要玩家执行该命令"));
      return 0;
    }

    ResourceLocation fxId = ResourceLocationArgument.getId(ctx, "id");
    float clampedIntensity = clampIntensity(intensity);
    AbilityFxDispatcher.play(performer, fxId, Vec3.ZERO, clampedIntensity);
    source.sendSuccess(
        () ->
            Component.literal(
                "已在玩家位置播放 FX " + fxId + "（强度 " + clampedIntensity + "）。若客户端缺少该 FX 定义，则不会看到任何效果"),
        false);
    return 1;
  }

  private static int playFxAt(CommandContext<CommandSourceStack> ctx, float intensity) {
    CommandSourceStack source = ctx.getSource();
    ServerPlayer performer;
    try {
      performer = source.getPlayerOrException();
    } catch (CommandSyntaxException e) {
      source.sendFailure(Component.literal("需要玩家执行该命令"));
      return 0;
    }

    ResourceLocation fxId = ResourceLocationArgument.getId(ctx, "id");
    Vec3 position = Vec3Argument.getVec3(ctx, "pos");
    Vec3 look = performer.getLookAngle();
    ServerLevel level = performer.serverLevel();
    float clampedIntensity = clampIntensity(intensity);
    AbilityFxDispatcher.play(level, fxId, position, look, look, performer, null, clampedIntensity);
    source.sendSuccess(
        () ->
            Component.literal(
                "已在 "
                    + formatVec(position)
                    + " 播放 FX "
                    + fxId
                    + "（强度 "
                    + clampedIntensity
                    + "）。若客户端缺少该 FX 定义，则不会看到任何效果"),
        false);
    return 1;
  }

  private static float clampIntensity(float intensity) {
    if (intensity < 0.0F) {
      return 0.0F;
    }
    if (intensity > 16.0F) {
      return 16.0F;
    }
    return intensity;
  }

  private static int playSoundHere(
      CommandContext<CommandSourceStack> ctx, float volume, float pitch) {
    CommandSourceStack source = ctx.getSource();
    ServerPlayer performer;
    try {
      performer = source.getPlayerOrException();
    } catch (CommandSyntaxException e) {
      source.sendFailure(Component.literal("需要玩家执行该命令"));
      return 0;
    }

    ResourceLocation soundId = ResourceLocationArgument.getId(ctx, "id");
    Optional<SoundEvent> sound = resolveSound(soundId);
    if (sound.isEmpty()) {
      source.sendFailure(Component.literal("未找到音效: " + soundId));
      return 0;
    }

    float sanitizedPitch = clampPitch(pitch);
    float sanitizedVolume = Math.max(0.0F, volume);
    performer
        .level()
        .playSound(
            null,
            performer.getX(),
            performer.getY(),
            performer.getZ(),
            sound.get(),
            SoundSource.PLAYERS,
            sanitizedVolume,
            sanitizedPitch);
    float finalVolume = sanitizedVolume;
    float finalPitch = sanitizedPitch;
    source.sendSuccess(
        () ->
            Component.literal(
                "已在玩家位置播放音效 "
                    + soundId
                    + "（音量="
                    + formatDouble(finalVolume)
                    + ", 音高="
                    + formatDouble(finalPitch)
                    + "）"),
        false);
    return 1;
  }

  private static int playSoundAt(
      CommandContext<CommandSourceStack> ctx, float volume, float pitch) {
    CommandSourceStack source = ctx.getSource();
    ServerLevel level = source.getLevel();
    ResourceLocation soundId = ResourceLocationArgument.getId(ctx, "id");
    Optional<SoundEvent> sound = resolveSound(soundId);
    if (sound.isEmpty()) {
      source.sendFailure(Component.literal("未找到音效: " + soundId));
      return 0;
    }

    Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
    float sanitizedPitch = clampPitch(pitch);
    float sanitizedVolume = Math.max(0.0F, volume);
    level.playSound(
        null,
        pos.x,
        pos.y,
        pos.z,
        sound.get(),
        SoundSource.PLAYERS,
        sanitizedVolume,
        sanitizedPitch);
    float finalVolume = sanitizedVolume;
    float finalPitch = sanitizedPitch;
    source.sendSuccess(
        () ->
            Component.literal(
                "已在 "
                    + formatVec(pos)
                    + " 播放音效 "
                    + soundId
                    + "（音量="
                    + formatDouble(finalVolume)
                    + ", 音高="
                    + formatDouble(finalPitch)
                    + "）"),
        false);
    return 1;
  }

  private static Optional<SoundEvent> resolveSound(ResourceLocation soundId) {
    if (soundId == null) {
      return Optional.empty();
    }
    Optional<Holder.Reference<SoundEvent>> holder =
        BuiltInRegistries.SOUND_EVENT.getHolder(soundId);
    return holder.map(Holder.Reference::value);
  }

  private static float clampPitch(float pitch) {
    if (pitch < 0.1F) {
      return 0.1F;
    }
    if (pitch > 4.0F) {
      return 4.0F;
    }
    return pitch;
  }

  private static int listFx(CommandContext<CommandSourceStack> ctx, String rawFilter) {
    String filter = rawFilter == null ? "" : rawFilter.trim().toLowerCase(Locale.ROOT);
    List<String> matches =
        BUILTIN_FX_IDS.stream()
            .map(ResourceLocation::toString)
            .filter(id -> filter.isEmpty() || id.startsWith(filter))
            .sorted(Comparator.naturalOrder())
            .toList();

    if (matches.isEmpty()) {
      ctx.getSource()
          .sendFailure(
              Component.literal("未找到匹配的 FX ID" + (filter.isEmpty() ? "" : "（前缀：" + filter + "）")));
      return 0;
    }

    ctx.getSource().sendSuccess(() -> Component.literal("已知 FX（" + matches.size() + "）:"), false);
    matches.forEach(id -> ctx.getSource().sendSuccess(() -> Component.literal(" - " + id), false));
    return matches.size();
  }

  private static String formatVec(Vec3 vec) {
    return String.format(Locale.ROOT, "(%.2f, %.2f, %.2f)", vec.x, vec.y, vec.z);
  }

  private static int runDemo(CommandContext<CommandSourceStack> ctx) {
    CommandSourceStack source = ctx.getSource();
    Player player;
    try {
      player = source.getPlayerOrException();
    } catch (Exception e) {
      source.sendFailure(Component.literal("需要玩家执行该命令"));
      return 0;
    }

    GuScriptModule.bootstrap();

    GuNode bone =
        new LeafGuNode("骨蛊", ImmutableMultiset.of("骨"), List.of(new ConsumeHealthAction(2)));
    GuNode blood =
        new LeafGuNode("血蛊", ImmutableMultiset.of("血"), List.of(new ConsumeZhenyuanAction(5)));
    GuNode burst =
        new LeafGuNode(
            "爆发蛊",
            ImmutableMultiset.of("爆发"),
            List.of(new EmitProjectileAction("minecraft:arrow", 4.0)));

    ReactionRule bloodBoneCore =
        ReactionRule.builder("blood_bone_core")
            .arity(2)
            .requiredTags(ImmutableMultiset.of("骨", "血"))
            .priority(10)
            .operator(
                (ruleId, inputs) ->
                    new OperatorGuNode(
                        ruleId,
                        "血骨核心",
                        GuNodeKind.OPERATOR,
                        ImmutableMultiset.of("核心"),
                        List.of(),
                        inputs))
            .build();

    ReactionRule explosiveLance =
        ReactionRule.builder("blood_bone_explosion")
            .arity(2)
            .requiredTags(ImmutableMultiset.of("核心", "爆发"))
            .priority(5)
            .operator(
                (ruleId, inputs) ->
                    new OperatorGuNode(
                        ruleId,
                        "血骨爆裂枪",
                        GuNodeKind.COMPOSITE,
                        ImmutableMultiset.of("杀招"),
                        List.of(),
                        inputs))
            .build();

    GuScriptReducer reducer = new GuScriptReducer();
    GuScriptReducer.ReductionResult result =
        reducer.reduce(List.of(bone, blood, burst), List.of(bloodBoneCore, explosiveLance));
    if (result.roots().isEmpty()) {
      source.sendFailure(Component.literal("未构建任何杀招 AST"));
      return 0;
    }

    GuScriptRuntime runtime = new GuScriptRuntime();
    AtomicInteger rootIndex = new AtomicInteger();
    runtime.executeAll(
        result.roots(),
        () -> {
          int index = rootIndex.getAndIncrement();
          DefaultGuScriptExecutionBridge bridge =
              new DefaultGuScriptExecutionBridge(player, player, index);
          return new DefaultGuScriptContext(player, player, bridge);
        });

    source.sendSuccess(
        () -> Component.literal("已执行 GuScript 示例，生成 " + result.roots().size() + " 个根节点"), true);
    return result.roots().size();
  }

  private static int startFlow(
      CommandContext<CommandSourceStack> ctx, double requestedTimeScale, Map<String, String> params)
      throws CommandSyntaxException {
    CommandSourceStack source = ctx.getSource();
    ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
    Optional<FlowProgram> flowOpt = FlowProgramRegistry.get(id);
    if (flowOpt.isEmpty()) {
      source.sendFailure(Component.literal("未找到 Flow 程序: " + id));
      return 0;
    }

    ServerPlayer player = source.getPlayerOrException();
    FlowController controller = FlowControllerManager.get(player);
    boolean wasRunning = controller.isRunning();
    double timeScale = clampTimeScale(requestedTimeScale);
    Map<String, String> safeParams =
        params == null || params.isEmpty() ? Map.of() : Map.copyOf(params);

    boolean accepted =
        controller.start(
            flowOpt.get(),
            player,
            timeScale,
            safeParams,
            player.level().getGameTime(),
            "command:/guscript");
    if (!accepted) {
      source.sendFailure(Component.literal("无法启动 Flow，请检查队列开关或守卫条件"));
      return 0;
    }

    boolean enqueued = wasRunning && controller.hasPending();
    String paramText = describeParams(safeParams);
    if (enqueued) {
      int queueSize = controller.pendingSize();
      source.sendSuccess(
          () ->
              Component.literal(
                  "已加入队列: Flow "
                      + id
                      + " (timeScale="
                      + formatDouble(timeScale)
                      + ", 参数="
                      + paramText
                      + ", 队列长度="
                      + queueSize
                      + ")"),
          false);
    } else {
      source.sendSuccess(
          () ->
              Component.literal(
                  "已启动 Flow "
                      + id
                      + " (timeScale="
                      + formatDouble(timeScale)
                      + ", 参数="
                      + paramText
                      + ")"),
          false);
    }
    return 1;
  }

  private static Map<String, String> parseFlowParams(String raw) {
    if (raw == null || raw.isBlank()) {
      return Map.of();
    }
    Map<String, String> params = new LinkedHashMap<>();
    for (String token : raw.trim().split("\\s+")) {
      if (token.isEmpty()) {
        continue;
      }
      int eq = token.indexOf('=');
      if (eq <= 0) {
        params.put(token, "true");
      } else {
        String key = token.substring(0, eq);
        if (key.isEmpty()) {
          continue;
        }
        String value = token.substring(eq + 1);
        params.put(key, value);
      }
    }
    return params;
  }

  private static double clampTimeScale(double timeScale) {
    if (Double.isNaN(timeScale) || Double.isInfinite(timeScale)) {
      return 1.0D;
    }
    return Math.max(0.1D, Math.min(100.0D, timeScale));
  }

  private static String describeParams(Map<String, String> params) {
    if (params == null || params.isEmpty()) {
      return "无";
    }
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (Map.Entry<String, String> entry : params.entrySet()) {
      if (!first) {
        builder.append(", ");
      }
      builder.append(entry.getKey()).append('=').append(entry.getValue());
      first = false;
    }
    return builder.toString();
  }

  private static String formatDouble(double value) {
    return String.format(Locale.ROOT, "%.3f", value);
  }
}
