package net.tigereye.chestcavity.recipes;

import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.registration.CCRecipes;

public class SalvageRecipe implements CraftingRecipe {
  private final CraftingBookCategory category;
  private final Ingredient input;
  private final int required;
  private final ItemStack outputStack;

  public SalvageRecipe(
      CraftingBookCategory category, Ingredient input, int required, ItemStack outputStack) {
    this.category = category;
    this.input = input;
    this.required = required;
    this.outputStack = outputStack;
  }

  public Ingredient getInput() {
    return input;
  }

  public int getRequired() {
    return required;
  }

  public ItemStack getOutputPrototype() {
    return outputStack.copy();
  }

  public Item getOutputItem() {
    return outputStack.getItem();
  }

  public int getOutputCount() {
    return outputStack.getCount();
  }

  @Override
  public CraftingBookCategory category() {
    return category;
  }

  @Override
  public NonNullList<Ingredient> getIngredients() {
    return NonNullList.withSize(required, input);
  }

  @Override
  public boolean matches(CraftingInput input, Level level) {
    int count = 0;
    for (ItemStack stack : input.items()) {
      if (stack.isEmpty()) {
        continue;
      }
      if (this.input.test(stack)) {
        count++;
      } else {
        return false;
      }
    }
    return count > 0 && count % required == 0;
  }

  @Override
  public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
    int count = 0;
    List<ItemStack> stacks = input.items();
    for (ItemStack stack : stacks) {
      if (stack.isEmpty()) {
        continue;
      }
      if (this.input.test(stack)) {
        count++;
      } else {
        return ItemStack.EMPTY;
      }
    }
    if (count == 0 || count % required != 0) {
      return ItemStack.EMPTY;
    }
    int resultCount = (count / required) * outputStack.getCount();
    if (resultCount > outputStack.getMaxStackSize()) {
      return ItemStack.EMPTY;
    }
    ItemStack output = outputStack.copy();
    output.setCount(resultCount);
    return output;
  }

  @Override
  public boolean canCraftInDimensions(int width, int height) {
    return width * height >= required;
  }

  @Override
  public ItemStack getResultItem(HolderLookup.Provider lookup) {
    return outputStack.copy();
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return CCRecipes.SALVAGE_RECIPE_SERIALIZER.get();
  }

  @Override
  public RecipeType<?> getType() {
    return CCRecipes.SALVAGE_RECIPE_TYPE.get();
  }
}
