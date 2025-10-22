package net.tigereye.chestcavity.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.mc.MuiModApi;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.tigereye.chestcavity.client.input.ModernUIKeyDispatcher;
import net.tigereye.chestcavity.client.modernui.TestModernUIFragment;
import net.tigereye.chestcavity.client.modernui.config.ChestCavityConfigFragment;
import net.tigereye.chestcavity.client.modernui.container.network.TestModernUIContainerRequestPayload;

/** Client-only brigadier commands for Modern UI bring-up and manual diagnostics. */
@OnlyIn(Dist.CLIENT)
public final class ModernUIClientCommands {

  private ModernUIClientCommands() {}

  public static void register(RegisterClientCommandsEvent event) {
    CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
    dispatcher.register(
        Commands.literal("testmodernUI")
            .executes(context -> openTestScreen())
            .then(Commands.literal("screen").executes(context -> openTestScreen()))
            .then(Commands.literal("container").executes(context -> requestContainer()))
            .then(Commands.literal("config").executes(context -> openConfigScreen()))
            .then(
                Commands.literal("toast")
                    .executes(
                        ctx -> {
                          var mc = net.minecraft.client.Minecraft.getInstance();
                          if (mc != null && mc.getToasts() != null) {
                            net.tigereye.chestcavity.client.ui.ReminderToast.show(
                                "ModernUI Toast",
                                "This is a PNG toast demo.",
                                net.minecraft.resources.ResourceLocation.parse(
                                    "modernui:textures/item/project_builder.png"));
                            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                          }
                          return 0;
                        }))
            .then(
                Commands.literal("toastitem")
                    .then(
                        Commands.argument(
                                "itemId",
                                com.mojang.brigadier.arguments.StringArgumentType.string())
                            .executes(
                                ctx -> {
                                  String idStr =
                                      com.mojang.brigadier.arguments.StringArgumentType.getString(
                                          ctx, "itemId");
                                  ResourceLocation id = ResourceLocation.parse(idStr);
                                  var opt = BuiltInRegistries.ITEM.getOptional(id);
                                  if (opt.isEmpty()) return 0;
                                  Item item = opt.get();
                                  net.tigereye.chestcavity.client.ui.ReminderToast.showItem(
                                      "ModernUI Toast",
                                      "Item icon demo: " + idStr,
                                      new ItemStack(item));
                                  return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                })))
            .then(
                Commands.literal("keylisten")
                    .then(
                        Commands.argument("enabled", BoolArgumentType.bool())
                            .executes(
                                ctx -> {
                                  boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                                  net.tigereye.chestcavity.client.ui.ModernUiClientState
                                      .setKeyListenEnabled(enabled);
                                  Minecraft mc = Minecraft.getInstance();
                                  if (mc.player != null) {
                                    boolean debug = ModernUIKeyDispatcher.isDebugEnabled();
                                    Component message =
                                        Component.literal(
                                            "ModernUI key listener "
                                                + (enabled ? "enabled" : "disabled")
                                                + (debug ? "" : " (debugHotkeys=false)"));
                                    mc.player.displayClientMessage(message, true);
                                  }
                                  return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                })))
            .then(
                Commands.literal("hui")
                    .then(
                        Commands.argument(
                                "enabled", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                            .executes(
                                ctx -> {
                                  boolean enabled =
                                      com.mojang.brigadier.arguments.BoolArgumentType.getBool(
                                          ctx, "enabled");
                                  net.tigereye.chestcavity.client.hud.TestHudOverlay.setEnabled(
                                      enabled);
                                  return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                })))
            .then(
                Commands.literal("huiitem")
                    .then(
                        Commands.argument(
                                "itemId",
                                com.mojang.brigadier.arguments.StringArgumentType.string())
                            .executes(
                                ctx -> {
                                  String idStr =
                                      com.mojang.brigadier.arguments.StringArgumentType.getString(
                                          ctx, "itemId");
                                  ResourceLocation id = ResourceLocation.parse(idStr);
                                  var opt = BuiltInRegistries.ITEM.getOptional(id);
                                  if (opt.isEmpty()) return 0;
                                  Item item = opt.get();
                                  net.tigereye.chestcavity.client.hud.TestHudOverlay.setItemIcon(
                                      new ItemStack(item));
                                  // 不强制开启，由用户用 /testmodernUI hui true 控制
                                  return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                }))));
  }

  private static int openTestScreen() {
    return openFragmentCommand(
        TestModernUIFragment::new, "commands.chestcavity.testmodernui.opened");
  }

  private static int openConfigScreen() {
    return openFragmentCommand(
        ChestCavityConfigFragment::new, "commands.chestcavity.testmodernui.config.opened");
  }

  public static void openConfigViaHotkey() {
    openFragment(ChestCavityConfigFragment::new, null);
  }

  private static int openFragmentCommand(
      Supplier<Fragment> fragmentSupplier, String translationKey) {
    return openFragment(fragmentSupplier, Component.translatable(translationKey))
        ? Command.SINGLE_SUCCESS
        : 0;
  }

  private static boolean openFragment(Supplier<Fragment> fragmentSupplier, Component toastMessage) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null) {
      return false;
    }

    mc.execute(
        () -> {
          var fragment = fragmentSupplier.get();
          var screen = MuiModApi.get().createScreen(fragment);
          mc.setScreen(screen);
          if (toastMessage != null) {
            mc.player.displayClientMessage(toastMessage, true);
          }
        });

    return true;
  }

  private static int requestContainer() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || mc.getConnection() == null) {
      return 0;
    }
    mc.execute(
        () -> {
          if (mc.getConnection() != null) {
            mc.getConnection().send(new TestModernUIContainerRequestPayload());
          }
        });
    return Command.SINGLE_SUCCESS;
  }
}
