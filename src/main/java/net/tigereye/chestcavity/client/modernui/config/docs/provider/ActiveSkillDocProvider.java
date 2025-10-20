package net.tigereye.chestcavity.client.modernui.config.docs.provider;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.client.modernui.config.docs.DocEntry;
import net.tigereye.chestcavity.client.modernui.config.docs.DocProvider;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ActiveSkillDocProvider implements DocProvider {

    @Override
    public String name() {
        return "ActiveSkill";
    }

    @Override
    public Collection<DocEntry> loadAll() {
        ActiveSkillRegistry.bootstrap();
        List<DocEntry> entries = new ArrayList<>();
        for (ActiveSkillRegistry.ActiveSkillEntry entry : ActiveSkillRegistry.entries()) {
            ResourceLocation organId = entry.organId();
            ItemStack icon = ItemStack.EMPTY;
            String organName = "";
            if (organId != null) {
                Item item = BuiltInRegistries.ITEM.getOptional(organId).orElse(null);
                if (item != null) {
                    icon = new ItemStack(item);
                    organName = icon.getHoverName().getString();
                }
            }

            String summary = entry.description() == null ? "" : entry.description();
            List<String> details = new ArrayList<>();
            details.add("技能ID：" + entry.skillId());
            if (entry.abilityId() != null) {
                details.add("能力：" + entry.abilityId());
            }
            if (organId != null) {
                String label = organName.isBlank() ? organId.toString() : organName + " (" + organId + ")";
                details.add("关联器官：" + label);
            }
            ActiveSkillRegistry.CooldownHint hint = entry.cooldownHint();
            if (hint != null) {
                List<String> parts = new ArrayList<>();
                if (hint.title() != null && !hint.title().isBlank()) {
                    parts.add(hint.title());
                }
                if (hint.subtitle() != null && !hint.subtitle().isBlank()) {
                    parts.add(hint.subtitle());
                }
                if (!parts.isEmpty()) {
                    details.add("冷却提示：" + String.join(" ｜ ", parts));
                }
            }

            Set<String> tags = new LinkedHashSet<>();
            tags.addAll(entry.tags());
            tags.add(entry.skillId().toString());
            if (entry.abilityId() != null) {
                tags.add(entry.abilityId().toString());
            }
            if (organId != null) {
                tags.add(organId.toString());
            }
            if (!organName.isBlank()) {
                tags.add(organName);
            }

            entries.add(new DocEntry(
                    entry.skillId(),
                    organName.isBlank() ? entry.skillId().toString() : organName,
                    summary,
                    details,
                    List.copyOf(tags),
                    icon
            ));
        }
        return entries;
    }
}
