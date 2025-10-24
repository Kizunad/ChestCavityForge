package net.tigereye.chestcavity.soul.profile.capability;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public interface CapabilitySnapshot {

  ResourceLocation id();

  CapabilitySnapshot capture(ServerPlayer player);

  void apply(ServerPlayer player);

  CompoundTag save(HolderLookup.Provider provider);

  CapabilitySnapshot load(CompoundTag tag, HolderLookup.Provider provider);

  boolean isDirty();

  void clearDirty();
}
