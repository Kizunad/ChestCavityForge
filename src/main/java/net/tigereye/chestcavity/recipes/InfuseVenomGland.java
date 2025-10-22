package net.tigereye.chestcavity.recipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.registration.CCItems;
import net.tigereye.chestcavity.registration.CCRecipes;
import net.tigereye.chestcavity.util.CommonOrganUtil;

public class InfuseVenomGland extends CustomRecipe {
  public InfuseVenomGland(CraftingBookCategory category) {
    super(category);
  }

  @Override
  public boolean matches(CraftingInput input, Level world) {
    boolean foundVenomGland = false;
    boolean foundPotion = false;
    for (ItemStack stack : input.items()) {
      if (stack.isEmpty()) {
        continue;
      }
      if (stack.is(CCItems.VENOM_GLAND.get())) {
        if (foundVenomGland) {
          return false;
        }
        foundVenomGland = true;
      } else if (stack.is(Items.POTION)
          || stack.is(Items.SPLASH_POTION)
          || stack.is(Items.LINGERING_POTION)) {
        if (foundPotion) {
          return false;
        }
        foundPotion = true;
      } else {
        return false;
      }
    }
    return foundVenomGland && foundPotion;
  }

  @Override
  public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
    ItemStack venomGland = ItemStack.EMPTY;
    ItemStack potion = ItemStack.EMPTY;
    for (ItemStack stack : input.items()) {
      if (stack.isEmpty()) {
        continue;
      }
      if (stack.is(CCItems.VENOM_GLAND.get())) {
        if (!venomGland.isEmpty()) {
          return ItemStack.EMPTY;
        }
        venomGland = stack.copy();
      } else if (stack.is(Items.POTION)
          || stack.is(Items.SPLASH_POTION)
          || stack.is(Items.LINGERING_POTION)) {
        if (!potion.isEmpty()) {
          return ItemStack.EMPTY;
        }
        potion = stack.copy();
      } else {
        return ItemStack.EMPTY;
      }
    }
    if (!venomGland.isEmpty() && !potion.isEmpty()) {
      ItemStack output = venomGland.copy();
      CommonOrganUtil.setStatusEffects(output, potion);
      return output;
    }
    return ItemStack.EMPTY;
  }

  @Override
  public boolean canCraftInDimensions(int width, int height) {
    return width * height >= 2;
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return CCRecipes.INFUSE_VENOM_GLAND.get();
  }
}
