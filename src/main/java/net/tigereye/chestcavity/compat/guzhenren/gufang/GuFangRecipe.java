package net.tigereye.chestcavity.compat.guzhenren.gufang;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GuFangRecipe {

    public record IngredientSpec(ResourceLocation item, int count) {
        public boolean matches(ItemStack stack) {
            if (stack.isEmpty()) return false;
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(item);
        }
    }

    public static final class OutputSpec {
        public final ResourceLocation item;
        public final int count;
        public OutputSpec(ResourceLocation item, int count) {
            this.item = Objects.requireNonNull(item);
            this.count = Math.max(1, count);
        }
        public ItemStack createStack() {
            Item it = BuiltInRegistries.ITEM.get(item);
            if (it == null) return ItemStack.EMPTY;
            return new ItemStack(it, count);
        }
    }

    public final ResourceLocation id;
    public final String guFangId;
    public final List<IngredientSpec> inputs;
    public final OutputSpec output;
    public final double baseSuccess;

    public GuFangRecipe(ResourceLocation id, String guFangId, List<IngredientSpec> inputs, OutputSpec output, double baseSuccess) {
        this.id = id;
        this.guFangId = Objects.requireNonNull(guFangId);
        this.inputs = List.copyOf(inputs);
        this.output = Objects.requireNonNull(output);
        this.baseSuccess = Math.max(0.0, Math.min(1.0, baseSuccess));
    }

    public static GuFangRecipe fromJson(ResourceLocation id, JsonObject obj) {
        String guFang = GsonHelper.getAsString(obj, "gufang_id");
        double base = GsonHelper.getAsDouble(obj, "base_success", 0.5);
        List<IngredientSpec> inputs = new ArrayList<>();
        for (var el : GsonHelper.getAsJsonArray(obj, "inputs")) {
            JsonObject ing = el.getAsJsonObject();
            ResourceLocation item = ResourceLocation.parse(GsonHelper.getAsString(ing, "item"));
            int count = Math.max(1, GsonHelper.getAsInt(ing, "count", 1));
            inputs.add(new IngredientSpec(item, count));
        }
        JsonObject out = GsonHelper.getAsJsonObject(obj, "output");
        ResourceLocation outId = ResourceLocation.parse(GsonHelper.getAsString(out, "item"));
        int outCount = Math.max(1, GsonHelper.getAsInt(out, "count", 1));
        return new GuFangRecipe(id, guFang, inputs, new OutputSpec(outId, outCount), base);
    }
}

