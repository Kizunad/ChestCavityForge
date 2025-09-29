package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.listeners.OrganHealListener;

/**
 * Provides the passive regeneration granted by the steel + refined iron bone combo.
 */
public enum SteelBoneComboHealing implements OrganHealListener {
    INSTANCE;

    private static final float HEAL_PER_TICK = 0.5f;

    @Override
    public float getHealingPerTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || cc == null || organ == null || organ.isEmpty()) {
            return 0.0f;
        }
        SteelBoneComboHelper.ComboState state = SteelBoneComboHelper.analyse(cc);
        if (!SteelBoneComboHelper.hasRefinedCombo(state)) {
            return 0.0f;
        }
        if (!SteelBoneComboHelper.isPrimaryRefinedOrgan(cc, organ)) {
            return 0.0f;
        }
        if (entity.getHealth() >= entity.getMaxHealth()) {
            return 0.0f;
        }
        return HEAL_PER_TICK;
    }
}

