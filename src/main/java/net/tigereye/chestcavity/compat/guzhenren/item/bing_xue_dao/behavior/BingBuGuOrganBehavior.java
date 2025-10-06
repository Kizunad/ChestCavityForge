package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.registration.CCItems;

/**
 * Behaviour implementation for 冰布蛊 (Bing Bu Gu).
 */
public final class BingBuGuOrganBehavior extends AbstractGuzhenrenOrganBehavior implements OrganSlowTickListener {

    public static final BingBuGuOrganBehavior INSTANCE = new BingBuGuOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "bing_bu_gu");

    private static final String STATE_ROOT = "BingBuGu";
    private static final String NON_PLAYER_COOLDOWN_KEY = "NonPlayerCooldown";

    private static final int PLAYER_REGEN_DURATION_TICKS = 5 * 20;
    private static final int PLAYER_REGEN_AMPLIFIER = 1;
    private static final int NON_PLAYER_REGEN_DURATION_TICKS = 10 * 20;
    private static final int NON_PLAYER_REGEN_AMPLIFIER = 1;
    private static final int SATURATION_DURATION_TICKS = 20;
    private static final int EFFECT_REFRESH_THRESHOLD_TICKS = 40;
    private static final int NON_PLAYER_INTERVAL_SECONDS = 120;

    private static final float BURP_VOLUME = 0.6f;
    private static final float BURP_PITCH_MIN = 0.9f;
    private static final float BURP_PITCH_VARIANCE = 0.1f;

    private BingBuGuOrganBehavior() {
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (!matchesOrgan(organ, CCItems.GUZHENREN_BING_BU_GU, ORGAN_ID)) {
            return;
        }
        if (!entity.isAlive()) {
            return;
        }
        if (entity instanceof Player player) {
            handlePlayer(player);
        } else {
            handleNonPlayer(entity, cc, organ);
        }
    }

    private void handlePlayer(Player player) {
        Inventory inventory = player.getInventory();
        boolean hasSnowball = inventory != null && inventory.countItem(Items.SNOWBALL) > 0;
        boolean hasIce = inventory != null && inventory.countItem(Items.ICE) > 0;

        if (hasSnowball && tryGrantSaturation(player)) {
            playEatingSound(player);
            return;
        }

        if (hasIce && tryGrantPlayerRegeneration(player)) {
            playEatingSound(player);
        }
    }

    private void handleNonPlayer(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        OrganState state = organState(organ, STATE_ROOT);
        int cooldown = state.getInt(NON_PLAYER_COOLDOWN_KEY, -1);
        if (cooldown < 0) {
            cooldown = entity.getRandom().nextInt(NON_PLAYER_INTERVAL_SECONDS + 1);
            var change = state.setInt(NON_PLAYER_COOLDOWN_KEY, cooldown);
            if (change.changed() && cc != null) {
                sendSlotUpdate(cc, organ);
            }
        }

        if (cooldown > 0) {
            int next = cooldown - 1;
            var change = state.setInt(NON_PLAYER_COOLDOWN_KEY, next);
            if (change.changed() && cc != null) {
                sendSlotUpdate(cc, organ);
            }
            return;
        }

        boolean applied = tryGrantNonPlayerRegeneration(entity);
        int nextCooldown = applied ? NON_PLAYER_INTERVAL_SECONDS : 1;
        var change = state.setInt(NON_PLAYER_COOLDOWN_KEY, nextCooldown);
        if (change.changed() && cc != null) {
            sendSlotUpdate(cc, organ);
        }
    }

    private boolean tryGrantSaturation(Player player) {
        if (player == null) {
            return false;
        }
        FoodData foodData = player.getFoodData();
        if (foodData == null) {
            return false;
        }
        float saturation = foodData.getSaturationLevel();
        float maxSaturation = Math.min(foodData.getFoodLevel(), 20);
        if (saturation >= maxSaturation) {
            return false;
        }
        MobEffectInstance existing = player.getEffect(MobEffects.SATURATION);
        if (existing != null && existing.getDuration() > 1) {
            return false;
        }
        return player.addEffect(new MobEffectInstance(MobEffects.SATURATION, SATURATION_DURATION_TICKS, 0));
    }

    private boolean tryGrantPlayerRegeneration(Player player) {
        if (player == null) {
            return false;
        }
        if (player.getHealth() >= player.getMaxHealth()) {
            return false;
        }
        MobEffectInstance existing = player.getEffect(MobEffects.REGENERATION);
        if (existing != null && existing.getAmplifier() >= PLAYER_REGEN_AMPLIFIER
                && existing.getDuration() > EFFECT_REFRESH_THRESHOLD_TICKS) {
            return false;
        }
        return player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, PLAYER_REGEN_DURATION_TICKS,
                PLAYER_REGEN_AMPLIFIER));
    }

    private boolean tryGrantNonPlayerRegeneration(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        MobEffectInstance existing = entity.getEffect(MobEffects.REGENERATION);
        if (existing != null && existing.getAmplifier() >= NON_PLAYER_REGEN_AMPLIFIER
                && existing.getDuration() > EFFECT_REFRESH_THRESHOLD_TICKS) {
            return false;
        }
        return entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, NON_PLAYER_REGEN_DURATION_TICKS,
                NON_PLAYER_REGEN_AMPLIFIER));
    }

    private void playEatingSound(LivingEntity entity) {
        Level level = entity == null ? null : entity.level();
        if (level == null) {
            return;
        }
        float pitch = BURP_PITCH_MIN + level.getRandom().nextFloat() * BURP_PITCH_VARIANCE;
        SoundSource source = entity instanceof Player ? SoundSource.PLAYERS : SoundSource.NEUTRAL;
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_BURP, source,
                BURP_VOLUME, pitch);
    }
}
