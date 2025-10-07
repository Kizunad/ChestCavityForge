package net.tigereye.chestcavity.soul.init;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceDefaultConstants;
import net.tigereye.chestcavity.soul.profile.SoulProfile;
import net.tigereye.chestcavity.soul.profile.capability.guzhenren.GuzhenrenSnapshot;

import java.util.Map;

/**
 * Centralised initialisation routines for freshly created SoulProfiles.
 *
 * This is intentionally separate from capability snapshots so we can evolve different
 * initialisation strategies without affecting switch/apply pipelines.
 */
public final class SoulInitializers {

    private SoulInitializers() {}

    /**
     * Default preset used by spawn_soul_custom: directly clone Guzhenren numeric fields from the owner
     * into the new profile's Guzhenren snapshot (if present). Other capabilities are left as created.
     */
    public static void applyDefault(ServerPlayer owner, SoulProfile profile) {
        cloneGuzhenrenNumeric(owner, profile);
    }

    /**
     * Reads every numeric field exposed by GuzhenrenResourceBridge from the owner and writes them into
     * the target profile's GuzhenrenSnapshot via its load path. This avoids depending on implementation
     * details (e.g., presence of mutators) and keeps semantics aligned with snapshot persistence.
     */
    public static void cloneGuzhenrenNumeric(ServerPlayer owner, SoulProfile targetProfile) {
        if (!GuzhenrenResourceBridge.isAvailable()) {
            return;
        }
        var handleOpt = GuzhenrenResourceBridge.open(owner);
        var snapshotOpt = targetProfile.capability(GuzhenrenSnapshot.ID, GuzhenrenSnapshot.class);
        if (handleOpt.isEmpty() || snapshotOpt.isEmpty()) {
            return;
        }
        Map<String, Double> ownerMap = handleOpt.get().snapshotAll();
        CompoundTag root = new CompoundTag();
        CompoundTag values = new CompoundTag();
        for (Map.Entry<String, Double> e : ownerMap.entrySet()) {
            Double v = e.getValue();
            if (v != null && Double.isFinite(v)) {
                values.putDouble(e.getKey(), v);
            }
        }
        root.put("values", values);
        snapshotOpt.get().load(root, owner.registryAccess());
    }

    /**
     * Caps-only preset: set shouyuan/max zhenyuan/max hunpo/max niantou to provided values, zero everything else.
     * This builds a values tag using the owner's known key set (from snapshotAll) so we can explicitly zero them.
     */
    public static void applyGuzhenrenCapsOnly(ServerPlayer owner,
                                              SoulProfile targetProfile,
                                              double shouyuan,
                                              double maxJingli,
                                              double maxHunpo,
                                              double maxNiantou) {
        var snapshotOpt = targetProfile.capability(GuzhenrenSnapshot.ID, GuzhenrenSnapshot.class);
        if (snapshotOpt.isEmpty()) {
            return;
        }

        CompoundTag root = new CompoundTag();
        CompoundTag values = new CompoundTag();

        // Zero all DOUBLE-typed guzhenren fields using the canonical constant list to avoid wrong types
        for (String key : GuzhenrenResourceDefaultConstants.DOUBLE_DEFAULTS.keySet()) {
            values.putDouble(key, 0.0D);
        }

        // Override caps with specified constants
        values.putDouble("shouyuan", shouyuan);
        // 使用“最大精力”字段而非“最大真元”字段
        values.putDouble("zuida_jingli", maxJingli);
        values.putDouble("zuida_hunpo", maxHunpo);
        values.putDouble("niantou_zhida", maxNiantou);

        root.put("values", values);
        snapshotOpt.get().load(root, owner.registryAccess());
    }

    /**
     * Convenience wrapper: shouyuan=100, zuida_zhenyuan=20, zuida_hunpo=1, niantou_zhida=100; everything else set to 0.
     */
    public static void applyGuzhenrenCapsOnlyDefaults(ServerPlayer owner, SoulProfile targetProfile) {
        applyGuzhenrenCapsOnly(owner, targetProfile, 100.0D, 20.0D, 1.0D, 100.0D);
    }
}
