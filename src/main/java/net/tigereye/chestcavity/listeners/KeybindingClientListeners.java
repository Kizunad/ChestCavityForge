package net.tigereye.chestcavity.listeners;


import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.tigereye.chestcavity.network.packets.ChestCavityHotkeyPayload;
import net.tigereye.chestcavity.registration.CCKeybindings;
import net.tigereye.chestcavity.registration.CCOrganScores;

public class KeybindingClientListeners {
    public static void onClientTick(ClientTickEvent.Post event){
        Player player = Minecraft.getInstance().player;
        while(CCKeybindings.UTILITY_ABILITIES != null && CCKeybindings.UTILITY_ABILITIES.consumeClick()) {
            if(player != null) {
                for(ResourceLocation i : CCKeybindings.UTILITY_ABILITY_LIST) {
                    sendHotkeyToServer(new ChestCavityHotkeyPayload(i));
                }
            }
        }
        while(CCKeybindings.ATTACK_ABILITIES != null && CCKeybindings.ATTACK_ABILITIES.consumeClick()) {
            if(player != null) {
                for(ResourceLocation i : CCKeybindings.ATTACK_ABILITY_LIST) {
                    sendHotkeyToServer(new ChestCavityHotkeyPayload(i));
                }
            }
        }

        checkSpecificKey(player, CCKeybindings.CREEPY,CCOrganScores.CREEPY);
        checkSpecificKey(player, CCKeybindings.DRAGON_BREATH,CCOrganScores.DRAGON_BREATH);
        checkSpecificKey(player, CCKeybindings.DRAGON_BOMBS,CCOrganScores.DRAGON_BOMBS);
        checkSpecificKey(player, CCKeybindings.FORCEFUL_SPIT,CCOrganScores.FORCEFUL_SPIT);
        checkSpecificKey(player, CCKeybindings.FURNACE_POWERED,CCOrganScores.FURNACE_POWERED);
        checkSpecificKey(player, CCKeybindings.IRON_REPAIR,CCOrganScores.IRON_REPAIR);
        checkSpecificKey(player, CCKeybindings.GHASTLY,CCOrganScores.GHASTLY);
        checkSpecificKey(player, CCKeybindings.GRAZING,CCOrganScores.GRAZING);
        checkSpecificKey(player, CCKeybindings.PYROMANCY,CCOrganScores.PYROMANCY);
        checkSpecificKey(player, CCKeybindings.SHULKER_BULLETS,CCOrganScores.SHULKER_BULLETS);
        checkSpecificKey(player, CCKeybindings.SILK,CCOrganScores.SILK);
    }

    private static void checkSpecificKey(Player player, KeyMapping keybinding, ResourceLocation id){
        while(keybinding != null && keybinding.consumeClick()) {
            if(player != null) {
                sendHotkeyToServer(new ChestCavityHotkeyPayload(id));
            }
        }
    }

    private static void sendHotkeyToServer(ChestCavityHotkeyPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(payload);
        }
    }
}
