package net.tigereye.chestcavity.listeners;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.SteelBoneComboHelper;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.util.ChestCavityUtil;

import java.util.Optional;

/**
 * Guards natural regeneration so combination effects can suspend vanilla healing.
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class OrganHealingEvents {

    private OrganHealingEvents() {
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (ChestCavityUtil.isOrganHealGuardActive()) {
            return;
        }

        Optional<ChestCavityEntity> optional = ChestCavityEntity.of(entity);
        if (optional.isEmpty()) {
            return;
        }

        ChestCavityInstance cc = optional.get().getChestCavityInstance();
        if (entity instanceof Player player) {
            if (shouldCancelPlayerHeal(player, cc, event.getAmount())) {
                event.setCanceled(true);
            }
        }
    }

    private static boolean shouldCancelPlayerHeal(Player player, ChestCavityInstance cc, float amount) {
        if (player == null || cc == null) {
            return false;
        }
        if (!player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION)) {
            return false;
        }
        if (amount > 1.01f) {
            return false;
        }
        if (player.hasEffect(MobEffects.REGENERATION) || player.hasEffect(MobEffects.HEAL)) {
            return false;
        }
        FoodData foodData = player.getFoodData();
        if (foodData == null || foodData.getFoodLevel() < 18) {
            return false;
        }
        SteelBoneComboHelper.ComboState state = SteelBoneComboHelper.analyse(cc);
        if (!SteelBoneComboHelper.shouldBlockNaturalRegen(player, cc, state)) {
            return false;
        }
        ChestCavity.LOGGER.debug(
                "[compat/guzhenren] Blocking natural regeneration for {} (amount={})",
                player.getScoreboardName(),
                amount
        );
        return true;
    }
}

