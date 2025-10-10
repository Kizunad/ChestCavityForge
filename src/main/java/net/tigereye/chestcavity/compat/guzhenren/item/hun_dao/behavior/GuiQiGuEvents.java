package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.soulbeast.state.SoulBeastStateManager;

import java.util.Optional;

/**
 * Event hooks for Gui Qi Gu's soul-beast specific passive ("噬魂").
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class GuiQiGuEvents {

    private static final double SOUL_EATER_MIN_HEALTH = 40.0D;
    private static final double SOUL_EATER_TRIGGER_CHANCE = 0.12D;
    private static final double SOUL_EATER_HUNPO_SCALE = 0.001D; // 0.1% of victim max health
    private static final double SOUL_EATER_STABILITY_PENALTY = 0.05D; // 5% of stability max

    private GuiQiGuEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim == null || victim.level().isClientSide()) {
            return;
        }
        DamageSource source = event.getSource();
        if (source == null) {
            return;
        }
        Entity direct = source.getEntity();
        if (!(direct instanceof LivingEntity attacker)) {
            return;
        }
        if (!(attacker instanceof Player player)) {
            return; // Gui Qi Gu's resources are only available on players for now.
        }
        if (!SoulBeastStateManager.isActive(attacker)) {
            return;
        }
        if (victim.getMaxHealth() <= SOUL_EATER_MIN_HEALTH) {
            return;
        }
        if (attacker.getRandom().nextDouble() >= SOUL_EATER_TRIGGER_CHANCE) {
            return;
        }

        ChestCavityInstance cc = ChestCavityEntity.of(attacker)
                .map(ChestCavityEntity::getChestCavityInstance)
                .orElse(null);
        if (cc == null || !GuiQiGuOrganBehavior.hasGuiQiGu(cc)) {
            return;
        }

        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();

        double bonus = victim.getMaxHealth() * SOUL_EATER_HUNPO_SCALE;
        if (!(bonus > 0.0D)) {
            return;
        }
        ResourceOps.tryAdjustDouble(handle, "zuida_hunpo", bonus, false, null);

        double stabilityMax = handle.read("hunpo_kangxing_shangxian").orElse(0.0D);
        double penalty = stabilityMax > 0.0D
                ? stabilityMax * SOUL_EATER_STABILITY_PENALTY
                : handle.read("hunpo_kangxing").orElse(0.0D) * SOUL_EATER_STABILITY_PENALTY;
        if (penalty > 0.0D) {
            ResourceOps.tryAdjustDouble(handle, "hunpo_kangxing", -penalty, true, "hunpo_kangxing_shangxian");
        }
    }
}

