package net.tigereye.chestcavity.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.recipes.InfuseVenomGland;
import net.tigereye.chestcavity.recipes.SalvageRecipe;
import net.tigereye.chestcavity.recipes.json.SalvageRecipeSerializer;

public class CCRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ChestCavity.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, ChestCavity.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<InfuseVenomGland>> INFUSE_VENOM_GLAND =
            RECIPE_SERIALIZERS.register("crafting_special_infuse_venom_gland", () -> new SimpleCraftingRecipeSerializer<>(InfuseVenomGland::new));

    public static final ResourceLocation SALVAGE_RECIPE_ID = ChestCavity.id("crafting_salvage");
    public static final DeferredHolder<RecipeType<?>, RecipeType<SalvageRecipe>> SALVAGE_RECIPE_TYPE =
            RECIPE_TYPES.register("crafting_salvage", () -> new RecipeType<SalvageRecipe>() {
                @Override
                public String toString() {
                    return SALVAGE_RECIPE_ID.toString();
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, SalvageRecipeSerializer> SALVAGE_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register("crafting_salvage", SalvageRecipeSerializer::new);
}
