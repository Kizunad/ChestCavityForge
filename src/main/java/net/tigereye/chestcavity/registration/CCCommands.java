package net.tigereye.chestcavity.registration;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.util.ChestCavityUtil;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = ChestCavity.MODID)
public class CCCommands {

    @SubscribeEvent
    public static void registerCommand(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("chestcavity")
                .then(Commands.literal("getscores")
                        .executes(CCCommands::getScoresNoArgs)
                        .then(Commands.argument("entity", EntityArgument.entity())
                                .executes(CCCommands::getScores)))
        );
        dispatcher.register(Commands.literal("chestcavity")
                .then(Commands.literal("resetChestCavity").requires(source -> source.hasPermission(2)) //getPermissionLevel
                        .executes(CCCommands::resetChestCavityNoArgs)
                        .then(Commands.argument("entity", EntityArgument.entity())
                                .executes(CCCommands::resetChestCavity)))
        );
    }

    private static int getScoresNoArgs(CommandContext<CommandSourceStack> context) {
        Entity entity;
        try {
            entity = context.getSource().getEntityOrException();
        }
        catch(Exception e){
            context.getSource().sendFailure(new TextComponent("getScores failed to get entity"));
            return -1;
        }
        Optional<ChestCavityEntity> optional = ChestCavityEntity.of(entity);
        if(optional.isPresent()){
            ChestCavityUtil.outputOrganScoresString((string) -> {
                context.getSource().sendSuccess(new TextComponent(string),false);
            },optional.get().getChestCavityInstance());
            return 1;
        }
        return 0;
    }

    private static int getScores(CommandContext<CommandSourceStack> context) {
        Entity entity;
        try {
            entity = EntityArgument.getEntity(context, "entity");
        }
        catch(Exception e){
            context.getSource().sendFailure(new TextComponent("getScores failed to get entity"));
            return -1;
        }
        Optional<ChestCavityEntity> optional = ChestCavityEntity.of(entity);
        if(optional.isPresent()){
            ChestCavityUtil.outputOrganScoresString((string) -> {
                context.getSource().sendSuccess(new TextComponent(string),false);
            },optional.get().getChestCavityInstance());
            return 1;
        }
        return 0;
    }

    private static int resetChestCavityNoArgs(CommandContext<CommandSourceStack> context) {
        Entity entity;
        try {
            entity = context.getSource().getEntityOrException();
        }
        catch(Exception e){
            context.getSource().sendFailure(new TextComponent("resetChestCavity failed to get entity"));
            return -1;
        }
        Optional<ChestCavityEntity> optional = ChestCavityEntity.of(entity);
        if(optional.isPresent()){
            ChestCavityUtil.generateChestCavityIfOpened(optional.get().getChestCavityInstance());
            return 1;
        }
        return 0;
    }

    private static int resetChestCavity(CommandContext<CommandSourceStack> context) {
        Entity entity;
        try {
            entity = EntityArgument.getEntity(context, "entity");
        }
        catch(Exception e){
            context.getSource().sendFailure(new TextComponent("getChestCavity failed to get entity"));
            return -1;
        }
        Optional<ChestCavityEntity> optional = ChestCavityEntity.of(entity);
        if(optional.isPresent()){
            ChestCavityUtil.generateChestCavityIfOpened(optional.get().getChestCavityInstance());
            return 1;
        }
        return 0;
    }

}