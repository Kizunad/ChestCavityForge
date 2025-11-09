package kizuna.guzhenren_event_ext.common.attachment;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.HashSet;
import java.util.Set;

public class EventExtensionPlayerData implements INBTSerializable<CompoundTag> {

    private final Set<String> triggeredOnceEvents = new HashSet<>();

    public Set<String> getTriggeredOnceEvents() {
        return triggeredOnceEvents;
    }

    public boolean hasTriggered(String eventId) {
        return triggeredOnceEvents.contains(eventId);
    }

    public void addTriggeredEvent(String eventId) {
        triggeredOnceEvents.add(eventId);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();
        ListTag list = new ListTag();
        for (String eventId : triggeredOnceEvents) {
            list.add(StringTag.valueOf(eventId));
        }
        nbt.put("TriggeredOnceEvents", list);
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        triggeredOnceEvents.clear();
        ListTag list = nbt.getList("TriggeredOnceEvents", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            triggeredOnceEvents.add(list.getString(i));
        }
    }
}
