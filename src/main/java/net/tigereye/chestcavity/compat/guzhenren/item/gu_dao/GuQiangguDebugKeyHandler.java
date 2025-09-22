package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import com.mojang.math.Axis;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "chestcavity", value = Dist.CLIENT)
final class GuQiangguDebugKeyHandler {

    private static final String CATEGORY = "key.categories.chestcavity.debug";

    private static final KeyMapping KEY_X_POS = create("gu_qiang.debug.x_pos", GLFW.GLFW_KEY_RIGHT);
    private static final KeyMapping KEY_X_NEG = create("gu_qiang.debug.x_neg", GLFW.GLFW_KEY_LEFT);
    private static final KeyMapping KEY_Y_POS = create("gu_qiang.debug.y_pos", GLFW.GLFW_KEY_UP);
    private static final KeyMapping KEY_Y_NEG = create("gu_qiang.debug.y_neg", GLFW.GLFW_KEY_DOWN);
    private static final KeyMapping KEY_Z_POS = create("gu_qiang.debug.z_pos", GLFW.GLFW_KEY_PAGE_UP);
    private static final KeyMapping KEY_Z_NEG = create("gu_qiang.debug.z_neg", GLFW.GLFW_KEY_PAGE_DOWN);

    private GuQiangguDebugKeyHandler() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(KEY_X_POS);
        event.register(KEY_X_NEG);
        event.register(KEY_Y_POS);
        event.register(KEY_Y_NEG);
        event.register(KEY_Z_POS);
        event.register(KEY_Z_NEG);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !Screen.hasControlDown()) {
            return;
        }
        boolean rotate = Screen.hasShiftDown();
        boolean firstPerson = Screen.hasAltDown();
        process(mc, KEY_X_POS, rotate, firstPerson, AxisRole.X, true);
        process(mc, KEY_X_NEG, rotate, firstPerson, AxisRole.X, false);
        process(mc, KEY_Y_POS, rotate, firstPerson, AxisRole.Y, true);
        process(mc, KEY_Y_NEG, rotate, firstPerson, AxisRole.Y, false);
        process(mc, KEY_Z_POS, rotate, firstPerson, AxisRole.Z, true);
        process(mc, KEY_Z_NEG, rotate, firstPerson, AxisRole.Z, false);
    }

    private static void process(Minecraft mc, KeyMapping key, boolean rotate, boolean firstPerson, AxisRole axis, boolean positive) {
        if (!key.consumeClick()) {
            return;
        }
        float delta = (rotate ? GuQiangguDebugTuner.ROTATION_STEP : GuQiangguDebugTuner.TRANSLATION_STEP) * (positive ? 1f : -1f);
        String feedback = switch (axis) {
            case X -> rotate
                    ? GuQiangguDebugTuner.adjustRotation(Axis.XP, delta, firstPerson)
                    : GuQiangguDebugTuner.adjustOffset(Axis.XP, delta, firstPerson);
            case Y -> rotate
                    ? GuQiangguDebugTuner.adjustRotation(Axis.YP, delta, firstPerson)
                    : GuQiangguDebugTuner.adjustOffset(Axis.YP, delta, firstPerson);
            case Z -> rotate
                    ? GuQiangguDebugTuner.adjustRotation(Axis.ZP, delta, firstPerson)
                    : GuQiangguDebugTuner.adjustOffset(Axis.ZP, delta, firstPerson);
        };
        if (feedback != null && mc.player != null) {
            mc.player.displayClientMessage(Component.literal(feedback), true);
        }
    }

    private static KeyMapping create(String key, int glfwKey) {
        return new KeyMapping("key.chestcavity." + key, glfwKey, CATEGORY);
    }

    private enum AxisRole { X, Y, Z }
}
