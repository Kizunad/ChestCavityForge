package net.tigereye.chestcavity.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.registration.CCEnchantments;
import net.tigereye.chestcavity.registration.CCOrganScores;
import net.tigereye.chestcavity.registration.CCAttachments;

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
            Component tierComponent = Component.translatable(tier);
            tooltip.add(Component.translatable("organscore." + organ.getNamespace() + "." + organ.getPath(), tierComponent));
        });
    }

    public static void displayCompatibility(ItemStack itemStack, Level world, List<Component> tooltip, TooltipFlag tooltipContext) {
        CompoundTag tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        MutableComponent message;
        int compatLevel = 0;
        net.minecraft.server.MinecraftServer server = world.getServer();
        if(server == null) {
            server = Minecraft.getInstance().getSingleplayerServer();
        }
        if(server != null) {
            Player serverPlayer = server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
            if(serverPlayer != null){
                compatLevel = ChestCavityUtil.getCompatibilityLevel(CCAttachments.getChestCavity(serverPlayer), itemStack);
            }
        }
        else{
            compatLevel = -1;
        }

        HolderLookup.Provider provider = world != null ? world.registryAccess() : (server != null ? server.registryAccess() : null);
        Holder<Enchantment> malpractice = provider != null
                ? CCEnchantments.resolveHolder(provider, CCEnchantments.MALPRACTICE).orElse(null)
                : null;
        Holder<Enchantment> oNegativeHolder = provider != null
                ? CCEnchantments.resolveHolder(provider, CCEnchantments.O_NEGATIVE).orElse(null)
                : null;

        boolean hasMalpractice = malpractice != null ? itemStack.getEnchantmentLevel(malpractice) > 0
                : EnchantmentHelper.getItemEnchantmentLevel(Holder.direct(CCEnchantments.MALPRACTICE.get()), itemStack) > 0;
        int oNegativeLevel = oNegativeHolder != null ? itemStack.getEnchantmentLevel(oNegativeHolder)
                : EnchantmentHelper.getItemEnchantmentLevel(Holder.direct(CCEnchantments.O_NEGATIVE.get()), itemStack);

        if(hasMalpractice){
            message = Component.translatable("tooltip.chestcavity.compatibility.unsafe");
        }
        else if (!tag.isEmpty() && tag.contains(ChestCavity.COMPATIBILITY_TAG.toString())
                && oNegativeLevel <= 0) {
            CompoundTag compatibility = tag.getCompound(ChestCavity.COMPATIBILITY_TAG.toString());
            String name = compatibility.getString("name");
            message = Component.translatable("tooltip.chestcavity.compatibility.bound", name);
        }
        else{
            message = Component.translatable("tooltip.chestcavity.compatibility.safe");
        }
        ChatFormatting color = compatLevel > 0 ? ChatFormatting.GREEN : (compatLevel == 0 ? ChatFormatting.RED : ChatFormatting.YELLOW);
        tooltip.add(message.copy().withStyle(color));
    }
}
