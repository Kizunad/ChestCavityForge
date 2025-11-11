package kizuna.guzhenren_event_ext.common.attachment;

import kizuna.guzhenren_event_ext.GuzhenrenEventExtension;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * 世界级别的持久化变量存储
 * <p>
 * 用于存储全局共享的变量,如世界事件状态、世界阶段等
 * 数据存储在主世界(Overworld)的 SavedData 中,确保全局一致性
 */
public final class EventExtensionWorldData extends SavedData {

    private static final String DATA_NAME = "guzhenren_event_ext_world_variables";

    private final Map<String, Object> variables = new HashMap<>();

    /**
     * 获取世界变量数据
     * 始终从主世界获取,确保全局一致性
     */
    public static EventExtensionWorldData get(ServerLevel level) {
        // 从主世界存储数据 (保证全局一致性)
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            overworld = level;
        }

        return overworld
            .getDataStorage()
            .computeIfAbsent(
                new SavedData.Factory<>(
                    EventExtensionWorldData::new,
                    EventExtensionWorldData::load
                ), DATA_NAME);
    }

    /**
     * 从 NBT 加载世界变量数据
     */
    private static EventExtensionWorldData load(CompoundTag tag, HolderLookup.Provider provider) {
        EventExtensionWorldData data = new EventExtensionWorldData();

        if (tag.contains("Variables", Tag.TAG_COMPOUND)) {
            CompoundTag varsTag = tag.getCompound("Variables");
            for (String key : varsTag.getAllKeys()) {
                Tag t = varsTag.get(key);

                // 根据 NBT Tag 类型推断原始类型
                switch (t.getId()) {
                    case Tag.TAG_BYTE:  // boolean 存储为 byte
                        data.variables.put(key, varsTag.getBoolean(key));
                        break;
                    case Tag.TAG_INT:
                        data.variables.put(key, varsTag.getInt(key));
                        break;
                    case Tag.TAG_LONG:
                        data.variables.put(key, varsTag.getLong(key));
                        break;
                    case Tag.TAG_FLOAT:
                        data.variables.put(key, varsTag.getFloat(key));
                        break;
                    case Tag.TAG_DOUBLE:
                        data.variables.put(key, varsTag.getDouble(key));
                        break;
                    case Tag.TAG_STRING:
                        data.variables.put(key, varsTag.getString(key));
                        break;
                }
            }
        }

        GuzhenrenEventExtension.LOGGER.debug("加载世界变量数据，共 {} 个变量", data.variables.size());
        return data;
    }

    /**
     * 保存世界变量数据到 NBT
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
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

        tag.put("Variables", varsTag);
        GuzhenrenEventExtension.LOGGER.debug("保存世界变量数据，共 {} 个变量", variables.size());
        return tag;
    }

    // ==================== 变量操作方法 ====================

    /**
     * 设置世界变量
     */
    public void setVariable(String key, Object value) {
        variables.put(key, value);
        setDirty(); // 标记需要保存
    }

    /**
     * 获取世界变量
     */
    public Object getVariable(String key) {
        return variables.get(key);
    }

    /**
     * 检查世界变量是否存在
     */
    public boolean hasVariable(String key) {
        return variables.containsKey(key);
    }

    /**
     * 删除世界变量
     */
    public void removeVariable(String key) {
        variables.remove(key);
        setDirty(); // 标记需要保存
    }
}
