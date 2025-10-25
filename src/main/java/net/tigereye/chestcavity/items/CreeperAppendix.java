package net.tigereye.chestcavity.items;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** An item that represents a creeper appendix. */
public class CreeperAppendix extends Item {

  /** Creates a new CreeperAppendix. */
  public CreeperAppendix() {
    super(new Item.Properties().stacksTo(1));
  }

  @Override
  public void appendHoverText(
      ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    super.appendHoverText(stack, context, tooltip, flag);
    tooltip.add(
        Component.translatable("item.chestcavity.creeper_appendix.tooltip.line1")
            .withStyle(ChatFormatting.ITALIC));
    tooltip.add(
        Component.translatable("item.chestcavity.creeper_appendix.tooltip.line2")
            .withStyle(ChatFormatting.ITALIC));
  }
}