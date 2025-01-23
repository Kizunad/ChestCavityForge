package net.tigereye.chestcavity.registration;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ClientRegistry;
import net.tigereye.chestcavity.ChestCavity;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class CCKeybindings {

    private static String ORGAN_ABILITY_KEY_CATEGORY = "organ_abilities";
    public static KeyMapping UTILITY_ABILITIES;
    public static ResourceLocation UTILITY_ABILITIES_ID = new ResourceLocation(ChestCavity.MODID,"utility_abilities");

    public static List<ResourceLocation> UTILITY_ABILITY_LIST = new ArrayList<>();
    public static KeyMapping ATTACK_ABILITIES;

    public static ResourceLocation ATTACK_ABILITIES_ID = new ResourceLocation(ChestCavity.MODID,"attack_abilities");
    public static List<ResourceLocation> ATTACK_ABILITY_LIST = new ArrayList<>();

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

    public static void init(){
        UTILITY_ABILITIES = register(UTILITY_ABILITIES_ID, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_V);
        ATTACK_ABILITIES = register(ATTACK_ABILITIES_ID, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_R);
        CREEPY = register(CCOrganScores.CREEPY, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, true); //GLFW.GLFW_KEY_APOSTROPHE, true);
        DRAGON_BREATH = register(CCOrganScores.DRAGON_BREATH, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, true);
        DRAGON_BOMBS = register(CCOrganScores.DRAGON_BOMBS, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, true);
        FORCEFUL_SPIT = register(CCOrganScores.FORCEFUL_SPIT, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, true);
        FURNACE_POWERED = register(CCOrganScores.FURNACE_POWERED, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, false);
        IRON_REPAIR = register(CCOrganScores.IRON_REPAIR, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, false);
        PYROMANCY = register(CCOrganScores.PYROMANCY, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, true);
        GHASTLY = register(CCOrganScores.GHASTLY, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, true);
        GRAZING = register(CCOrganScores.GRAZING, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, false);
        SHULKER_BULLETS = register(CCOrganScores.SHULKER_BULLETS, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, true);
        SILK = register(CCOrganScores.SILK, ORGAN_ABILITY_KEY_CATEGORY, GLFW.GLFW_KEY_KP_DECIMAL, false);
    }

    public static KeyMapping register(ResourceLocation id, String category, int defaultKey){
        KeyMapping keyMapping = new KeyMapping(
                "key." + id.getNamespace() + "." + id.getPath(), // The translation key of the keybinding's name
                InputConstants.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                defaultKey, // The keycode of the key
                "category." + id.getNamespace() + "." + category // The translation key of the keybinding's category.
        );
        ClientRegistry.registerKeyBinding(keyMapping);
        return keyMapping;
    }
    public static KeyMapping register(ResourceLocation id, String category, int defaultKey, boolean isAttack){
        if(isAttack) {ATTACK_ABILITY_LIST.add(id);}
        else {UTILITY_ABILITY_LIST.add(id);}
        return register(id, category, defaultKey);
    }
}
