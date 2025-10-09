package net.tigereye.chestcavity.registration;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.tigereye.chestcavity.ChestCavity;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class CCKeybindings {

    private static String ORGAN_ABILITY_KEY_CATEGORY = "organ_abilities";
    public static KeyMapping UTILITY_ABILITIES;
    public static ResourceLocation UTILITY_ABILITIES_ID = ChestCavity.id("utility_abilities");

    public static List<ResourceLocation> UTILITY_ABILITY_LIST = new ArrayList<>();
    public static KeyMapping ATTACK_ABILITIES;

    public static ResourceLocation ATTACK_ABILITIES_ID = ChestCavity.id("attack_abilities");
    public static List<ResourceLocation> ATTACK_ABILITY_LIST = new ArrayList<>();

    public static KeyMapping WOODEN_SHOVEL_TICK_RATE;
    public static KeyMapping WOODEN_SHOVEL_FREEZE;
    public static KeyMapping WOODEN_SHOVEL_UNFREEZE;

    public static KeyMapping CREEPY;
    public static KeyMapping DRAGON_BREATH;
    public static KeyMapping DRAGON_BOMBS;
    public static KeyMapping FORCEFUL_SPIT;
    public static KeyMapping FURNACE_POWERED;
    public static KeyMapping IRON_REPAIR;
    public static KeyMapping PYROMANCY;
    public static KeyMapping GHASTLY;
    public static KeyMapping GRAZING;
    public static KeyMapping SHULKER_BULLETS;
    public static KeyMapping SILK;
    public static KeyMapping GUSCRIPT_OPEN;
    public static KeyMapping GUSCRIPT_EXECUTE;
    public static KeyMapping GUSCRIPT_CANCEL;
    public static KeyMapping MODERN_UI_CONFIG;

    public static void register(RegisterKeyMappingsEvent event){
        UTILITY_ABILITIES = register(event, UTILITY_ABILITIES_ID, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_V, false);
        ATTACK_ABILITIES = register(event, ATTACK_ABILITIES_ID, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_R, true);
        CREEPY = register(event, CCOrganScores.CREEPY, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, true);
        DRAGON_BREATH = register(event, CCOrganScores.DRAGON_BREATH, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, true);
        DRAGON_BOMBS = register(event, CCOrganScores.DRAGON_BOMBS, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, true);
        FORCEFUL_SPIT = register(event, CCOrganScores.FORCEFUL_SPIT, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, true);
        FURNACE_POWERED = register(event, CCOrganScores.FURNACE_POWERED, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, false);
        IRON_REPAIR = register(event, CCOrganScores.IRON_REPAIR, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, false);
        PYROMANCY = register(event, CCOrganScores.PYROMANCY, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, true);
        GHASTLY = register(event, CCOrganScores.GHASTLY, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, true);
        GRAZING = register(event, CCOrganScores.GRAZING, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, false);
        SHULKER_BULLETS = register(event, CCOrganScores.SHULKER_BULLETS, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, true);
        SILK = register(event, CCOrganScores.SILK, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, false);

        WOODEN_SHOVEL_TICK_RATE = registerStandalone(event, "key." + ChestCavity.MODID + ".wooden_shovel_tick_rate", ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_H);
        GUSCRIPT_OPEN = registerStandalone(event, "key." + ChestCavity.MODID + ".guscript_open", ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_G);
        GUSCRIPT_EXECUTE = registerStandalone(event, "key." + ChestCavity.MODID + ".guscript_execute", ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_B);
        GUSCRIPT_CANCEL = registerStandalone(event, "key." + ChestCavity.MODID + ".guscript_cancel", ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_C);
        WOODEN_SHOVEL_FREEZE = registerStandalone(event, "key." + ChestCavity.MODID + ".wooden_shovel_freeze", ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_J);
        WOODEN_SHOVEL_UNFREEZE = registerStandalone(event, "key." + ChestCavity.MODID + ".wooden_shovel_unfreeze", ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_K);
        MODERN_UI_CONFIG = registerStandalone(event, "key." + ChestCavity.MODID + ".modernui_config", ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_O);
    }

    private static KeyMapping register(RegisterKeyMappingsEvent event, ResourceLocation id, String category, int defaultKey, boolean isAttack){
        KeyMapping keyMapping = new KeyMapping(
                "key." + id.getNamespace() + "." + id.getPath(), // The translation key of the keybinding's name
                InputConstants.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                defaultKey, // The keycode of the key
                "category." + id.getNamespace() + "." + category // The translation key of the keybinding's category.
        );
        if(isAttack) {ATTACK_ABILITY_LIST.add(id);}
        else {UTILITY_ABILITY_LIST.add(id);}
        event.register(keyMapping);
        return keyMapping;
    }

    private static KeyMapping registerStandalone(RegisterKeyMappingsEvent event, String translationKey, String category, int defaultKey) {
        KeyMapping keyMapping = new KeyMapping(
                translationKey,
                InputConstants.Type.KEYSYM,
                defaultKey,
                "category." + ChestCavity.MODID + "." + category
        );
        event.register(keyMapping);
        return keyMapping;
    }
}
