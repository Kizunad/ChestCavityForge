package net.tigereye.chestcavity.guscript.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.tigereye.chestcavity.guscript.GuScriptModule;
import net.tigereye.chestcavity.guscript.actions.ConsumeHealthAction;
import net.tigereye.chestcavity.guscript.actions.ConsumeZhenyuanAction;
import net.tigereye.chestcavity.guscript.actions.EmitProjectileAction;
import net.tigereye.chestcavity.guscript.ast.GuNode;
import net.tigereye.chestcavity.guscript.ast.GuNodeKind;
import net.tigereye.chestcavity.guscript.ast.LeafGuNode;
import net.tigereye.chestcavity.guscript.ast.OperatorGuNode;
import net.tigereye.chestcavity.guscript.runtime.action.DefaultGuScriptExecutionBridge;
import net.tigereye.chestcavity.guscript.runtime.exec.DefaultGuScriptContext;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;
import net.tigereye.chestcavity.guscript.runtime.reduce.GuScriptReducer;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptRuntime;
import net.tigereye.chestcavity.guscript.runtime.reduce.ReactionRule;

import java.util.List;
import java.util.Set;

public final class GuScriptCommands {

    private GuScriptCommands() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("guscript")
                .requires(stack -> stack.hasPermission(2))
                .then(Commands.literal("run").executes(GuScriptCommands::runDemo)));
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

        GuNode bone = new LeafGuNode("骨蛊", Set.of("骨"), List.of(new ConsumeHealthAction(2)));
        GuNode blood = new LeafGuNode("血蛊", Set.of("血"), List.of(new ConsumeZhenyuanAction(5)));
        GuNode burst = new LeafGuNode("爆发蛊", Set.of("爆发"), List.of(new EmitProjectileAction("minecraft:arrow", 4.0)));

        ReactionRule bloodBoneCore = ReactionRule.builder("blood_bone_core")
                .arity(2)
                .requiredTags(Set.of("骨", "血"))
                .priority(10)
                .operator((ruleId, inputs) -> new OperatorGuNode(ruleId, "血骨核心", GuNodeKind.OPERATOR,
                        Set.of("核心"), List.of(), inputs))
                .build();

        ReactionRule explosiveLance = ReactionRule.builder("blood_bone_explosion")
                .arity(2)
                .requiredTags(Set.of("核心", "爆发"))
                .priority(5)
                .operator((ruleId, inputs) -> new OperatorGuNode(ruleId, "血骨爆裂枪", GuNodeKind.COMPOSITE,
                        Set.of("杀招"), List.of(), inputs))
                .build();

        GuScriptReducer reducer = new GuScriptReducer();
        GuScriptReducer.ReductionResult result = reducer.reduce(List.of(bone, blood, burst), List.of(bloodBoneCore, explosiveLance));
        if (result.roots().isEmpty()) {
            source.sendFailure(Component.literal("未构建任何杀招 AST"));
            return 0;
        }

        DefaultGuScriptExecutionBridge bridge = DefaultGuScriptExecutionBridge.forPlayer(player);
        GuScriptContext context = new DefaultGuScriptContext(player, player, bridge);
        GuScriptRuntime runtime = new GuScriptRuntime();
        runtime.executeAll(result.roots(), context);

        source.sendSuccess(() -> Component.literal("已执行 GuScript 示例，生成 " + result.roots().size() + " 个根节点"), true);
        return result.roots().size();
    }
}
