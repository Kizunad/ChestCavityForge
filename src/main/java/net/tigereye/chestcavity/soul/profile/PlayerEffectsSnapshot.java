package net.tigereye.chestcavity.soul.profile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

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
        // 清空后重放所有效果
        player.removeAllEffects();
        for (MobEffectInstance inst : effects) {
            if (inst != null) {
                player.addEffect(new MobEffectInstance(inst));
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
