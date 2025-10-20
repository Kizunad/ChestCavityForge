package net.tigereye.chestcavity.client.modernui.config.docs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Objects;

public record DocEntry(
        ResourceLocation id,
        String title,
        String summary,
        List<String> details,
        List<String> tags,
        ItemStack icon
) {
    public DocEntry {
        Objects.requireNonNull(id, "id");
        title = (title == null || title.isBlank()) ? id.toString() : title;
        summary = summary == null ? "" : summary;
        details = details == null ? List.of() : List.copyOf(details);
        tags = tags == null ? List.of() : List.copyOf(tags);
        icon = normalize(icon);
    }

    private static ItemStack normalize(ItemStack input) {
        ItemStack stack = (input == null || input.isEmpty()) ? new ItemStack(Items.BOOK) : input.copy();
        stack.setCount(1);
        return stack;
    }
}
