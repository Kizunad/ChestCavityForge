package net.tigereye.chestcavity.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class RecipeDebugCommands {

    private RecipeDebugCommands() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("cc_dump_recipes")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> dumpNamespace(ctx.getSource(), "chestcavity"))
                .then(Commands.argument("namespace", com.mojang.brigadier.arguments.StringArgumentType.string())
                        .executes(ctx -> dumpNamespace(ctx.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "namespace")))));
    }

    private static int dumpNamespace(CommandSourceStack source, String namespace) {
        MinecraftServer server = source.getServer();
        if (server == null) {
            source.sendFailure(Component.literal("无服务器实例"));
            return 0;
        }
        RecipeManager rm = server.getRecipeManager();
        List<ResourceLocation> ids = rm.getAllRecipesFor(RecipeType.CRAFTING).stream()
                .map(RecipeHolder::id)
                .filter(id -> namespace == null || namespace.isBlank() || id.getNamespace().equals(namespace))
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .collect(Collectors.toList());

        int total = ids.size();
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "命名空间 '%s' 的合成配方数: %d", namespace, total)), false);

        // Also print total crafting recipes across all namespaces for context
        int craftingTotal = rm.getAllRecipesFor(RecipeType.CRAFTING).size();
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "全部合成配方总数: %d", craftingTotal)), false);

        int preview = Math.min(10, total);
        for (int i = 0; i < preview; i++) {
            ResourceLocation id = ids.get(i);
            source.sendSuccess(() -> Component.literal(" - " + id), false);
        }
        if (preview < total) {
            source.sendSuccess(() -> Component.literal(" … (仅显示前 " + preview + " 项)"), false);
        }
        return total;
    }
}
