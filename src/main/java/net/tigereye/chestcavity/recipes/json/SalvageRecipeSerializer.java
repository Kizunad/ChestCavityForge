package net.tigereye.chestcavity.recipes.json;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.tigereye.chestcavity.recipes.SalvageRecipe;

public class SalvageRecipeSerializer implements RecipeSerializer<SalvageRecipe> {
    private static final MapCodec<SalvageRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(SalvageRecipe::category),
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(SalvageRecipe::getInput),
            Codec.INT.optionalFieldOf("required", 1).forGetter(SalvageRecipe::getRequired),
            ItemStack.CODEC.fieldOf("result").forGetter(SalvageRecipe::getOutputPrototype)
        ).apply(instance, SalvageRecipe::new)
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, SalvageRecipe> STREAM_CODEC = StreamCodec.composite(
        CraftingBookCategory.STREAM_CODEC, SalvageRecipe::category,
        Ingredient.CONTENTS_STREAM_CODEC, SalvageRecipe::getInput,
        ByteBufCodecs.VAR_INT, SalvageRecipe::getRequired,
        ItemStack.STREAM_CODEC, SalvageRecipe::getOutputPrototype,
        SalvageRecipe::new
    );

    @Override
    public MapCodec<SalvageRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, SalvageRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
