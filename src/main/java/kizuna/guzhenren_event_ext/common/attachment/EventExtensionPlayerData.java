package kizuna.guzhenren_event_ext.common.attachment;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EventExtensionPlayerData implements INBTSerializable<CompoundTag> {

    private final Set<String> triggeredOnceEvents = new HashSet<>();
    private final Map<String, Object> variables = new HashMap<>();

    public Set<String> getTriggeredOnceEvents() {
        return triggeredOnceEvents;
    }

    public boolean hasTriggered(String eventId) {
        return triggeredOnceEvents.contains(eventId);
    }

    public void addTriggeredEvent(String eventId) {
        triggeredOnceEvents.add(eventId);
    }

    // 变量操作方法
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    public Object getVariable(String key) {
        return variables.get(key);
    }

    public boolean hasVariable(String key) {
        return variables.containsKey(key);
    }

    public void removeVariable(String key) {
        variables.remove(key);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();

        // 序列化触发过的事件
        ListTag list = new ListTag();
        for (String eventId : triggeredOnceEvents) {
            list.add(StringTag.valueOf(eventId));
        }
        nbt.put("TriggeredOnceEvents", list);

        // 序列化变量
        CompoundTag varsTag = new CompoundTag();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Boolean) {
                varsTag.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                varsTag.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                varsTag.putLong(key, (Long) value);
            } else if (value instanceof Float) {
                varsTag.putFloat(key, (Float) value);
            } else if (value instanceof Double) {
                varsTag.putDouble(key, (Double) value);
            } else if (value instanceof String) {
                varsTag.putString(key, (String) value);
            }
        }
        nbt.put("Variables", varsTag);

        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        // 反序列化触发过的事件
        triggeredOnceEvents.clear();
        ListTag list = nbt.getList("TriggeredOnceEvents", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            triggeredOnceEvents.add(list.getString(i));
        }

        // 反序列化变量
        variables.clear();
        if (nbt.contains("Variables", Tag.TAG_COMPOUND)) {
            CompoundTag varsTag = nbt.getCompound("Variables");
            for (String key : varsTag.getAllKeys()) {
                Tag tag = varsTag.get(key);

                // 根据 NBT Tag 类型推断原始类型
                switch (tag.getId()) {
                    case Tag.TAG_BYTE:  // boolean 存储为 byte
                        variables.put(key, varsTag.getBoolean(key));
                        break;
                    case Tag.TAG_INT:
                        variables.put(key, varsTag.getInt(key));
                        break;
                    case Tag.TAG_LONG:
                        variables.put(key, varsTag.getLong(key));
                        break;
                    case Tag.TAG_FLOAT:
                        variables.put(key, varsTag.getFloat(key));
                        break;
                    case Tag.TAG_DOUBLE:
                        variables.put(key, varsTag.getDouble(key));
                        break;
                    case Tag.TAG_STRING:
                        variables.put(key, varsTag.getString(key));
                        break;
                }
            }
        }
    }
}
