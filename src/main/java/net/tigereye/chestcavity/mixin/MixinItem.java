package net.tigereye.chestcavity.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.tigereye.chestcavity.chestcavities.organs.OrganManager;
import net.tigereye.chestcavity.chestcavities.organs.OrganData;
import net.tigereye.chestcavity.util.ClientOrganUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Item.class)
public class MixinItem {

    @Inject(at = @At("HEAD"), method = "appendHoverText")
    public void chestCavityItemAppendTooltip(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag context, CallbackInfo info){
        //((Item)(Object)this)
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(((Item)(Object)this)); //Registry.ITEM.getKey(((Item)(Object)this));
        if(OrganManager.GeneratedOrganData.containsKey(id)){
            OrganData data = OrganManager.GeneratedOrganData.get(id);
            if(!data.pseudoOrgan){
                ClientOrganUtil.displayOrganQuality(data.organScores,tooltip);
                ClientOrganUtil.displayCompatibility(stack,world,tooltip,context);
            }
        }
    }

}
