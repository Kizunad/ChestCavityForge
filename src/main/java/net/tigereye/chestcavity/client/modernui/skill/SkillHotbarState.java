package net.tigereye.chestcavity.client.modernui.skill;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SkillHotbarState {

    private final EnumMap<SkillHotbarKey, List<ResourceLocation>> bindings = new EnumMap<>(SkillHotbarKey.class);

    public SkillHotbarState() {
        reset();
    }

    public void reset() {
        bindings.clear();
        for (SkillHotbarKey key : SkillHotbarKey.values()) {
            bindings.put(key, new ArrayList<>());
        }
    }

    public boolean isEmpty() {
        for (List<ResourceLocation> list : bindings.values()) {
            if (!list.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public List<ResourceLocation> getSkills(SkillHotbarKey key) {
        List<ResourceLocation> list = bindings.get(key);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(list);
    }

    public Map<SkillHotbarKey, List<ResourceLocation>> snapshot() {
        EnumMap<SkillHotbarKey, List<ResourceLocation>> copy = new EnumMap<>(SkillHotbarKey.class);
        for (Map.Entry<SkillHotbarKey, List<ResourceLocation>> entry : bindings.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    public boolean bind(SkillHotbarKey key, ResourceLocation skillId) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(skillId, "skillId");
        List<ResourceLocation> list = bindings.computeIfAbsent(key, k -> new ArrayList<>());
        if (list.contains(skillId)) {
            return false;
        }
        list.add(skillId);
        return true;
    }

    public boolean unbind(SkillHotbarKey key, ResourceLocation skillId) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(skillId, "skillId");
        List<ResourceLocation> list = bindings.get(key);
        if (list == null) {
            return false;
        }
        return list.remove(skillId);
    }

    public void clear(SkillHotbarKey key) {
        Objects.requireNonNull(key, "key");
        List<ResourceLocation> list = bindings.get(key);
        if (list != null) {
            list.clear();
        }
    }

    public void replaceAll(Map<SkillHotbarKey, List<ResourceLocation>> map) {
        reset();
        if (map == null) {
            return;
        }
        for (Map.Entry<SkillHotbarKey, List<ResourceLocation>> entry : map.entrySet()) {
            SkillHotbarKey key = entry.getKey();
            if (key == null) {
                continue;
            }
            List<ResourceLocation> source = entry.getValue();
            if (source == null || source.isEmpty()) {
                continue;
            }
            List<ResourceLocation> target = bindings.computeIfAbsent(key, k -> new ArrayList<>());
            for (ResourceLocation id : source) {
                if (id != null && !target.contains(id)) {
                    target.add(id);
                }
            }
        }
    }

    public List<SkillHotbarKey> orderedKeys() {
        return List.copyOf(java.util.Arrays.asList(SkillHotbarKey.values()));
    }
}
