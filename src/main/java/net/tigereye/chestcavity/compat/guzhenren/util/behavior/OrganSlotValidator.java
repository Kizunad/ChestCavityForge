package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.util.ChestCavityUtil;

import java.util.Objects;

/**
 * Lightweight validator that snapshots an organ's slot + item fingerprint
 * and detects changes efficiently. Typical usage:
 *
 *   OrganSlotValidator v = OrganSlotValidator.capture(cc, organ);
 *   // later on tick/trigger
 *   boolean stable = v.validateAndUpdateIfChanged(cc, organ);
 *   if (!stable) return false; // skip logic if organ/slot changed
 */
public final class OrganSlotValidator {

    private int slotIndex;
    private ResourceLocation itemId;
    private int count;
    private int nbtHash;

    private OrganSlotValidator(int slotIndex, ResourceLocation itemId, int count, int nbtHash) {
        this.slotIndex = slotIndex;
        this.itemId = itemId;
        this.count = count;
        this.nbtHash = nbtHash;
    }

    public static OrganSlotValidator capture(ChestCavityInstance cc, ItemStack organ) {
        int slot = resolveSlot(cc, organ);
        Fingerprint fp = fingerprint(organ);
        return new OrganSlotValidator(slot, fp.id, fp.count, fp.nbtHash);
    }

    public boolean validate(ChestCavityInstance cc, ItemStack organ) {
        if (organ == null || organ.isEmpty()) {
            return false;
        }
        int currentSlot = resolveSlot(cc, organ);
        Fingerprint fp = fingerprint(organ);
        return currentSlot == this.slotIndex
                && Objects.equals(fp.id, this.itemId)
                && fp.count == this.count;
    }

    public boolean validateStrict(ChestCavityInstance cc, ItemStack organ) {
        if (!validate(cc, organ)) {
            return false;
        }
        Fingerprint fp = fingerprint(organ);
        return fp.nbtHash == this.nbtHash;
    }

    /**
     * Returns true if unchanged; if changed, updates the snapshot and returns false.
     */
    public boolean validateAndUpdateIfChanged(ChestCavityInstance cc, ItemStack organ) {
        boolean ok = validate(cc, organ);
        if (!ok) {
            int slot = resolveSlot(cc, organ);
            Fingerprint fp = fingerprint(organ);
            this.slotIndex = slot;
            this.itemId = fp.id;
            this.count = fp.count;
            this.nbtHash = fp.nbtHash;
        }
        return ok;
    }

    public boolean validateAndUpdateIfChangedStrict(ChestCavityInstance cc, ItemStack organ) {
        boolean ok = validateStrict(cc, organ);
        if (!ok) {
            int slot = resolveSlot(cc, organ);
            Fingerprint fp = fingerprint(organ);
            this.slotIndex = slot;
            this.itemId = fp.id;
            this.count = fp.count;
            this.nbtHash = fp.nbtHash;
        }
        return ok;
    }

    public int slotIndex() { return slotIndex; }
    public ResourceLocation itemId() { return itemId; }
    public int count() { return count; }

    private static int resolveSlot(ChestCavityInstance cc, ItemStack organ) {
        if (cc == null) {
            return -1;
        }
        return ChestCavityUtil.findOrganSlot(cc, organ);
    }

    private record Fingerprint(ResourceLocation id, int count, int nbtHash) {}

    private static Fingerprint fingerprint(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new Fingerprint(ResourceLocation.fromNamespaceAndPath("minecraft","air"), 0, 0);
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        int count = Math.max(0, stack.getCount());
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        int hash = 0;
        if (data != null) {
            CompoundTag tag = data.copyTag();
            // Use string form for a stable-ish fingerprint
            hash = Objects.hash(tag == null ? null : tag.toString());
        }
        return new Fingerprint(id, count, hash);
    }
}
