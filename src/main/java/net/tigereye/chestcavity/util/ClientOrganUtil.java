package net.tigereye.chestcavity.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.registration.CCEnchantments;
import net.tigereye.chestcavity.registration.CCOrganScores;

import java.util.*;
import java.util.List;

public class ClientOrganUtil {

    public static void displayOrganQuality(Map<ResourceLocation,Float> organQualityMap, List<Component> tooltip){
        organQualityMap.forEach((organ,score) -> {
            String tier;
            if(organ.equals(CCOrganScores.HYDROALLERGENIC)){
                if(score >= 2){
                    tier = "quality.chestcavity.severely";
                }
                else{
                    tier = "";
                }
            }
            else {
                if (score >= 1.5f) {
                    tier = "quality.chestcavity.supernatural";
                } else if (score >= 1.25) {
                    tier = "quality.chestcavity.exceptional";
                } else if (score >= 1) {
                    tier = "quality.chestcavity.good";
                } else if (score >= .75f) {
                    tier = "quality.chestcavity.average";
                } else if (score >= .5f) {
                    tier = "quality.chestcavity.poor";
                } else if (score >= 0) {
                    tier = "quality.chestcavity.pathetic";
                } else if (score >= -.25f) {
                    tier = "quality.chestcavity.slightly_reduces";
                } else if (score >= -.5f) {
                    tier = "quality.chestcavity.reduces";
                } else if (score >= -.75f) {
                    tier = "quality.chestcavity.greatly_reduces";
                } else {
                    tier = "quality.chestcavity.cripples";
                }
            }
            TranslatableComponent text = new TranslatableComponent("organscore." + organ.getNamespace() + "." + organ.getPath(), new TranslatableComponent(tier));
            tooltip.add(text);
        });
    }

    public static void displayCompatibility(ItemStack itemStack, Level world, List<Component> tooltip, TooltipFlag tooltipContext) {
        CompoundTag tag = itemStack.getTag();
        String textString;
        boolean uuidMatch = false;
        int compatLevel = 0;
        Player serverPlayer = null;
        net.minecraft.server.MinecraftServer server = world.getServer();
        if(server == null) {
            server = Minecraft.getInstance().getSingleplayerServer();
        }
        if(server != null) {
            serverPlayer = server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
            if(serverPlayer instanceof ChestCavityEntity ccPlayer){
                UUID ccID = ccPlayer.getChestCavityInstance().compatibility_id;
                //tooltip.add(Text.literal("ServerPlayerCC: "+ccID));
                compatLevel = ChestCavityUtil.getCompatibilityLevel(ccPlayer.getChestCavityInstance(),itemStack);
            }
        }
        else{
            compatLevel = -1;
        }

        if(EnchantmentHelper.getItemEnchantmentLevel(CCEnchantments.MALPRACTICE.get(),itemStack) > 0){
            textString = "Unsafe to use";
        }
        else if (tag != null && tag.contains(ChestCavity.COMPATIBILITY_TAG.toString())
                && EnchantmentHelper.getItemEnchantmentLevel(CCEnchantments.O_NEGATIVE.get(),itemStack) <= 0) {
            tag = tag.getCompound(ChestCavity.COMPATIBILITY_TAG.toString());
            String name = tag.getString("name");
            //tooltip.add(new TextComponent("OrganOwnerCC: " + tag.getUUID("owner")));
            textString = "Only Compatible With: " + name;// + " (" + compatLevel + " compat)";
        }
        else{
            textString = "Safe to Use";
        }

        TextComponent text = new TextComponent("");
        if(compatLevel > 0) {
            text.withStyle(ChatFormatting.GREEN);
        }
        else if(compatLevel == 0){
            text.withStyle(ChatFormatting.RED);
        }
        else{
            text.withStyle(ChatFormatting.YELLOW);
        }
        text.append(textString);
        tooltip.add(text);
    }
}
