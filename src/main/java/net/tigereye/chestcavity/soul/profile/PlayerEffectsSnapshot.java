package net.tigereye.chestcavity.soul.profile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.soul.util.SoulLog;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家药水效果快照
 */
public record PlayerEffectsSnapshot(List<MobEffectInstance> effects) {

    public static PlayerEffectsSnapshot capture(Player player) {
        List<MobEffectInstance> list = new ArrayList<>();
        for (MobEffectInstance inst : player.getActiveEffects()) {
            list.add(new MobEffectInstance(inst));
        }
        return new PlayerEffectsSnapshot(list);
    }

    public void restore(Player player) {
        // 仅替换“快照中包含”的效果类型，保留快照未声明（unexpected/其他模组）效果，最小化对外部状态的破坏。
        try {
            java.util.Set<Holder<MobEffect>> snapshotTypes = new java.util.HashSet<>();
            for (MobEffectInstance inst : effects) {
                if (inst != null && inst.getEffect() != null) {
                    snapshotTypes.add(inst.getEffect());
                }
            }
            // 移除这些类型的现有效果（不触碰其他类型）
            if (!snapshotTypes.isEmpty()) {
                java.util.List<Holder<MobEffect>> toRemove = new java.util.ArrayList<>();
                for (MobEffectInstance active : player.getActiveEffects()) {
                    if (active != null && active.getEffect() != null && snapshotTypes.contains(active.getEffect())) {
                        toRemove.add(active.getEffect());
                    }
                }
                for (Holder<MobEffect> effect : toRemove) {
                    player.removeEffect(effect);
                }
            }
        } catch (Exception e) {
            SoulLog.warn("[soul] effects-partial-clear encountered exception: {}", e.toString());
        }

        // 重放快照内效果
        for (MobEffectInstance inst : effects) {
            if (inst != null) {
                try {
                    player.addEffect(new MobEffectInstance(inst));
                } catch (Exception e) {
                    SoulLog.warn("[soul] addEffect failed: {}", e.toString());
                }
            }
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (MobEffectInstance inst : effects) {
            Tag t = inst.save();
            if (t != null) {
                list.add(t);
            }
        }
        tag.put("list", list);
        return tag;
    }

    public static PlayerEffectsSnapshot load(CompoundTag tag) {
        List<MobEffectInstance> list = new ArrayList<>();
        if (tag.contains("list", Tag.TAG_LIST)) {
            ListTag arr = tag.getList("list", Tag.TAG_COMPOUND);
            for (int i = 0; i < arr.size(); i++) {
                CompoundTag e = arr.getCompound(i);
                MobEffectInstance inst = MobEffectInstance.load(e);
                if (inst != null) {
                    list.add(inst);
                }
            }
        }
        return new PlayerEffectsSnapshot(list);
    }

    public static PlayerEffectsSnapshot empty() {
        return new PlayerEffectsSnapshot(List.of());
    }
}
