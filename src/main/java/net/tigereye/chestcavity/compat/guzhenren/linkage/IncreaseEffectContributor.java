package net.tigereye.chestcavity.compat.guzhenren.linkage;

import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;

/**
 * Implemented by organ behaviours that contribute to the generic INCREASE_EFFECT linkage
 * channels. Implementations are responsible for reporting their per-stack contribution during
 * rebuild passes so the ledger can restore consistent channel totals.
 */
public interface IncreaseEffectContributor {

    /**
     * Recomputes the organ's INCREASE_EFFECT contribution for the provided stack and reports it via
     * the supplied registrar. Implementations should call {@link IncreaseEffectLedger.Registrar}
     * for each linkage channel they affect. The registrar will aggregate totals and update linkage
     * channels once the rebuild pass completes.
     */
    void rebuildIncreaseEffects(
            ChestCavityInstance chestCavity,
            ActiveLinkageContext context,
            ItemStack organ,
            IncreaseEffectLedger.Registrar registrar
    );
}
