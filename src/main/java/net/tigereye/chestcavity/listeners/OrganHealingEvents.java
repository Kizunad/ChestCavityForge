package net.tigereye.chestcavity.listeners;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.SteelBoneComboHelper;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.util.ChestCavityUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Guards natural regeneration so combination effects can suspend vanilla healing.
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class OrganHealingEvents {

    private static final float EPSILON = 1.0E-4f;
    private static final float MAX_EXPECTED_SATURATION_REFUND = 1.0f;
    private static final float MAX_EXPECTED_EXHAUSTION_REFUND = 6.0f;
    private static final int MAX_EXPECTED_FOOD_REFUND = 1;

    private static final Map<UUID, HungerSnapshot> HUNGER_SNAPSHOTS = new HashMap<>();

    private OrganHealingEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide()) {
            return;
        }
        FoodData food = player.getFoodData();
        if (food == null) {
            return;
        }
        HUNGER_SNAPSHOTS.put(player.getUUID(), HungerSnapshot.capture(food));
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() != null) {
            HUNGER_SNAPSHOTS.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() != null) {
            HUNGER_SNAPSHOTS.remove(event.getEntity().getUUID());
        }
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
                refundNaturalRegenCosts(player);
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

    private static void refundNaturalRegenCosts(Player player) {
        FoodData food = player.getFoodData();
        if (food == null) {
            return;
        }
        HungerSnapshot baseline = HUNGER_SNAPSHOTS.get(player.getUUID());
        if (baseline == null) {
            fallbackRefund(player, food);
            return;
        }

        HungerRefund refund = baseline.computeRefund(food);
        if (refund.isEmpty()) {
            return;
        }

        if (refund.foodLevels() > 0) {
            int restored = Math.min(refund.foodLevels(), MAX_EXPECTED_FOOD_REFUND);
            food.setFoodLevel(Math.min(food.getFoodLevel() + restored, 20));
        }

        if (refund.saturation() > EPSILON) {
            float restored = Math.min(refund.saturation(), MAX_EXPECTED_SATURATION_REFUND);
            float updated = food.getSaturationLevel() + restored;
            food.setSaturation(Math.min(updated, food.getFoodLevel()));
        }

        if (refund.exhaustion() > EPSILON) {
            float restored = Math.min(refund.exhaustion(), MAX_EXPECTED_EXHAUSTION_REFUND);
            float adjust = -Math.min(restored, food.getExhaustionLevel());
            if (Math.abs(adjust) > EPSILON) {
                player.causeFoodExhaustion(adjust);
            }
        }

        // Refresh the snapshot so subsequent events within the same tick reuse the restored state.
        HUNGER_SNAPSHOTS.put(player.getUUID(), HungerSnapshot.capture(food));
    }

    private static void fallbackRefund(Player player, FoodData food) {
        // Best-effort rollback when no baseline is available (e.g., first tick after login).
        if (food.getSaturationLevel() < food.getFoodLevel()) {
            food.setSaturation(Math.min(food.getSaturationLevel() + MAX_EXPECTED_SATURATION_REFUND, food.getFoodLevel()));
        } else if (food.getFoodLevel() < 20) {
            food.setFoodLevel(Math.min(food.getFoodLevel() + MAX_EXPECTED_FOOD_REFUND, 20));
        }
        if (food.getExhaustionLevel() > EPSILON) {
            player.causeFoodExhaustion(-Math.min(food.getExhaustionLevel(), MAX_EXPECTED_EXHAUSTION_REFUND));
        }
    }

    static final class HungerSnapshot {

        private final int foodLevel;
        private final float saturation;
        private final float exhaustion;

        private HungerSnapshot(int foodLevel, float saturation, float exhaustion) {
            this.foodLevel = foodLevel;
            this.saturation = saturation;
            this.exhaustion = exhaustion;
        }

        static HungerSnapshot capture(FoodData food) {
            return new HungerSnapshot(food.getFoodLevel(), food.getSaturationLevel(), food.getExhaustionLevel());
        }

        HungerRefund computeRefund(FoodData food) {
            int foodDelta = Math.max(0, this.foodLevel - food.getFoodLevel());
            float saturationDelta = Math.max(0.0f, this.saturation - food.getSaturationLevel());
            float exhaustionDelta = Math.max(0.0f, food.getExhaustionLevel() - this.exhaustion);
            return new HungerRefund(foodDelta, saturationDelta, exhaustionDelta);
        }
    }

    static final class HungerRefund {

        private final int foodLevels;
        private final float saturation;
        private final float exhaustion;

        HungerRefund(int foodLevels, float saturation, float exhaustion) {
            this.foodLevels = foodLevels;
            this.saturation = saturation;
            this.exhaustion = exhaustion;
        }

        int foodLevels() {
            return foodLevels;
        }

        float saturation() {
            return saturation;
        }

        float exhaustion() {
            return exhaustion;
        }

        boolean isEmpty() {
            return foodLevels <= 0 && saturation <= EPSILON && exhaustion <= EPSILON;
        }
    }
}
