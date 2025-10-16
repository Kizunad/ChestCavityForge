package net.tigereye.chestcavity.skill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.client.modernui.network.SkillHotbarSnapshotPayload;
import net.tigereye.chestcavity.client.modernui.skill.SkillHotbarKey;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SkillHotbarServerData {

    private static final String TAG_ROOT = ChestCavity.MODID + "_skill_hotbar";
    private static final String TAG_KEY = "Key";
    private static final String TAG_SKILLS = "Skills";

    private SkillHotbarServerData() {}

    public static void save(ServerPlayer player, Map<SkillHotbarKey, List<ResourceLocation>> bindings) {
        CompoundTag persisted = player.getPersistentData();
        CompoundTag container = persisted.getCompound(TAG_ROOT);
        ListTag entries = new ListTag();
        if (bindings != null) {
            for (Map.Entry<SkillHotbarKey, List<ResourceLocation>> entry : bindings.entrySet()) {
                SkillHotbarKey key = entry.getKey();
                if (key == null) {
                    continue;
                }
                List<ResourceLocation> skills = entry.getValue();
                if (skills == null || skills.isEmpty()) {
                    continue;
                }
                CompoundTag tag = new CompoundTag();
                tag.putString(TAG_KEY, key.id());
                ListTag skillList = new ListTag();
                for (ResourceLocation id : skills) {
                    skillList.add(StringTag.valueOf(id.toString()));
                }
                tag.put(TAG_SKILLS, skillList);
                entries.add(tag);
            }
        }
        container.put("Entries", entries);
        persisted.put(TAG_ROOT, container);
    }

    public static Map<SkillHotbarKey, List<ResourceLocation>> load(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData();
        CompoundTag container = persisted.getCompound(TAG_ROOT);
        ListTag entries = container.getList("Entries", ListTag.TAG_COMPOUND);
        Map<SkillHotbarKey, List<ResourceLocation>> map = new EnumMap<>(SkillHotbarKey.class);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag tag = entries.getCompound(i);
            String keyId = tag.getString(TAG_KEY);
            SkillHotbarKey key = SkillHotbarKey.fromId(keyId);
            if (key == null) {
                continue;
            }
            ListTag skillList = tag.getList(TAG_SKILLS, ListTag.TAG_STRING);
            if (skillList.isEmpty()) {
                continue;
            }
            List<ResourceLocation> skills = new ArrayList<>();
            for (int j = 0; j < skillList.size(); j++) {
                String raw = skillList.getString(j);
                ResourceLocation id = ResourceLocation.tryParse(raw);
                if (id == null) {
                    continue;
                }
                if (!ActiveSkillRegistry.isSkillRegistered(id)) {
                    ChestCavity.LOGGER.warn("[modernui][skill-hotbar] skipping unregistered skill {} for key {}", id, key.id());
                    continue;
                }
                skills.add(id);
            }
            if (!skills.isEmpty()) {
                map.put(key, skills);
            }
        }
        return map;
    }

    public static Map<SkillHotbarKey, List<ResourceLocation>> fromWire(Map<String, List<ResourceLocation>> payload) {
        Map<SkillHotbarKey, List<ResourceLocation>> map = new EnumMap<>(SkillHotbarKey.class);
        if (payload == null) {
            return map;
        }
        for (Map.Entry<String, List<ResourceLocation>> entry : payload.entrySet()) {
            SkillHotbarKey key = SkillHotbarKey.fromId(entry.getKey());
            if (key == null) {
                continue;
            }
            List<ResourceLocation> ids = entry.getValue();
            if (ids == null || ids.isEmpty()) {
                continue;
            }
            map.put(key, new ArrayList<>(ids));
        }
        return map;
    }

    public static Map<String, List<ResourceLocation>> toWire(ServerPlayer player) {
        Map<SkillHotbarKey, List<ResourceLocation>> serverBindings = load(player);
        Map<String, List<ResourceLocation>> wire = new java.util.LinkedHashMap<>();
        for (Map.Entry<SkillHotbarKey, List<ResourceLocation>> entry : serverBindings.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            wire.put(entry.getKey().id(), new ArrayList<>(entry.getValue()));
        }
        return wire;
    }

    public static void sendSnapshot(ServerPlayer player) {
        Map<String, List<ResourceLocation>> payload = toWire(player);
        player.connection.send(new SkillHotbarSnapshotPayload(payload));
    }
}
