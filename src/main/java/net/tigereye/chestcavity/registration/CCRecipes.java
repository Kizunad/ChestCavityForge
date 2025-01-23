package net.tigereye.chestcavity.registration;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.recipes.InfuseVenomGland;
import net.tigereye.chestcavity.recipes.SalvageRecipe;
import net.tigereye.chestcavity.recipes.json.SalvageRecipeSerializer;

public class CCRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ChestCavity.MODID);

    public static RegistryObject<SimpleRecipeSerializer<InfuseVenomGland>> INFUSE_VENOM_GLAND = RECIPE_SERIALIZERS.register("crafting_special_infuse_venom_gland", () -> new SimpleRecipeSerializer<InfuseVenomGland>(InfuseVenomGland::new));
    public static ResourceLocation SALVAGE_RECIPE_ID = new ResourceLocation(ChestCavity.MODID,"crafting_salvage");
    public static RecipeType<SalvageRecipe> SALVAGE_RECIPE_TYPE = new RecipeType<SalvageRecipe>() {public String toString() {return SALVAGE_RECIPE_ID.toString();}};
    public static RegistryObject<SalvageRecipeSerializer> SALVAGE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("crafting_salvage", SalvageRecipeSerializer::new);
}