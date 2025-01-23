package net.tigereye.chestcavity.recipes.json;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.tigereye.chestcavity.recipes.SalvageRecipe;
import net.tigereye.chestcavity.registration.CCRecipes;
import org.jetbrains.annotations.Nullable;

public class SalvageRecipeSerializer implements RecipeSerializer<SalvageRecipe> {

    @Override
    public SalvageRecipe fromJson(ResourceLocation id, JsonObject json) {
        SalvageRecipeJsonFormat recipeJson = new Gson().fromJson(json, SalvageRecipeJsonFormat.class);

        if (recipeJson.ingredient == null || recipeJson.result == null) {
            throw new JsonSyntaxException("A required attribute is missing!");
        }

        if (recipeJson.required == 0) recipeJson.required = 1;
        if (recipeJson.count == 0) recipeJson.count = 1;
        Ingredient input = Ingredient.fromJson(recipeJson.ingredient);
        Item outputItem = Registry.ITEM.getOptional(new ResourceLocation(recipeJson.result))
                // Validate the inputted item actually exists
                .orElseThrow(() -> new JsonSyntaxException("No such item " + recipeJson.result));
        ItemStack output = new ItemStack(outputItem, recipeJson.count);

        return new SalvageRecipe(input, recipeJson.required, output, id);
    }

    @Override
    public SalvageRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        Ingredient input = Ingredient.fromNetwork(buf);
        int required = buf.readInt();
        ItemStack output = buf.readItem();
        return new SalvageRecipe(input, required, output, id);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, SalvageRecipe recipe) {
        recipe.getInput().toNetwork(buf);
        buf.writeInt(recipe.getRequired());
        buf.writeItem(recipe.getResultItem());
    }

    @Override
    public RecipeSerializer<?> setRegistryName(ResourceLocation name) {
        return this;
    }

    @Override
    public ResourceLocation getRegistryName() {
        return CCRecipes.SALVAGE_RECIPE_ID;
    }

    @Override
    public Class<RecipeSerializer<?>> getRegistryType() {
        return castClass(RecipeSerializer.class);
    }

    private static <G> Class<G> castClass(Class<?> cls)
    {
        return (Class<G>)cls;
    }
}
