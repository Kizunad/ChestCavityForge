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
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.config.CCConfig;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
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

    private static final CCConfig.GuzhenrenBingXueDaoConfig.BingBuGuConfig DEFAULTS =
            new CCConfig.GuzhenrenBingXueDaoConfig.BingBuGuConfig();

    private BingBuGuOrganBehavior() {
    }

    private static CCConfig.GuzhenrenBingXueDaoConfig.BingBuGuConfig cfg() {
        CCConfig root = ChestCavity.config;
        if (root != null) {
            CCConfig.GuzhenrenBingXueDaoConfig group = root.GUZHENREN_BING_XUE_DAO;
            if (group != null && group.BING_BU_GU != null) {
                return group.BING_BU_GU;
            }
        }
        return DEFAULTS;
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
        CCConfig.GuzhenrenBingXueDaoConfig.BingBuGuConfig config = cfg();
        if (entity instanceof Player player) {
            handlePlayer(player, config);
        } else {
            handleNonPlayer(entity, cc, organ, config);
        }
    }

    private void handlePlayer(Player player, CCConfig.GuzhenrenBingXueDaoConfig.BingBuGuConfig config) {
        Inventory inventory = player.getInventory();
        boolean hasSnowball = inventory != null && inventory.countItem(Items.SNOWBALL) > 0;
        boolean hasIce = inventory != null && inventory.countItem(Items.ICE) > 0;

        if (hasSnowball && tryGrantSaturation(player, config)) {
            playEatingSound(player, config);
            return;
        }

        if (hasIce && tryGrantPlayerRegeneration(player, config)) {
            playEatingSound(player, config);
        }
    }

    private void handleNonPlayer(
            LivingEntity entity,
            ChestCavityInstance cc,
            ItemStack organ,
            CCConfig.GuzhenrenBingXueDaoConfig.BingBuGuConfig config
    ) {
        OrganState state = organState(organ, STATE_ROOT);
        int cooldown = state.getInt(NON_PLAYER_COOLDOWN_KEY, -1);
        if (cooldown < 0) {
            int interval = Math.max(0, config.nonPlayerIntervalSeconds);
            cooldown = interval > 0 ? entity.getRandom().nextInt(interval + 1) : 0;
            OrganStateOps.setInt(state, cc, organ, NON_PLAYER_COOLDOWN_KEY, cooldown, value -> value, -1);
        }

        if (cooldown > 0) {
            int next = cooldown - 1;
            OrganStateOps.setInt(state, cc, organ, NON_PLAYER_COOLDOWN_KEY, next, value -> value, -1);
            return;
        }

        boolean applied = tryGrantNonPlayerRegeneration(entity, config);
        int interval = Math.max(0, config.nonPlayerIntervalSeconds);
        int nextCooldown = applied ? interval : 1;
        OrganStateOps.setInt(state, cc, organ, NON_PLAYER_COOLDOWN_KEY, nextCooldown, value -> value, -1);
    }

    private boolean tryGrantSaturation(
            Player player,
            CCConfig.GuzhenrenBingXueDaoConfig.BingBuGuConfig config
    ) {
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
        int duration = Math.max(0, config.saturationDurationTicks);
        return player.addEffect(new MobEffectInstance(MobEffects.SATURATION, duration, 0));
    }

    private boolean tryGrantPlayerRegeneration(
            Player player,
            CCConfig.GuzhenrenBingXueDaoConfig.BingBuGuConfig config
    ) {
        if (player == null) {
            return false;
        }
        if (player.getHealth() >= player.getMaxHealth()) {
            return false;
        }
        MobEffectInstance existing = player.getEffect(MobEffects.REGENERATION);
        int threshold = Math.max(0, config.effectRefreshThresholdTicks);
        int amplifier = Math.max(0, config.playerRegenAmplifier);
        if (existing != null && existing.getAmplifier() >= amplifier
                && existing.getDuration() > threshold) {
            return false;
        }
        int duration = Math.max(0, config.playerRegenDurationTicks);
        return player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, amplifier));
    }

    private boolean tryGrantNonPlayerRegeneration(
            LivingEntity entity,
            CCConfig.GuzhenrenBingXueDaoConfig.BingBuGuConfig config
    ) {
        if (entity == null) {
            return false;
        }
        MobEffectInstance existing = entity.getEffect(MobEffects.REGENERATION);
        int threshold = Math.max(0, config.effectRefreshThresholdTicks);
        int amplifier = Math.max(0, config.nonPlayerRegenAmplifier);
        if (existing != null && existing.getAmplifier() >= amplifier
                && existing.getDuration() > threshold) {
            return false;
        }
        int duration = Math.max(0, config.nonPlayerRegenDurationTicks);
        return entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, amplifier));
    }

    private void playEatingSound(
            LivingEntity entity,
            CCConfig.GuzhenrenBingXueDaoConfig.BingBuGuConfig config
    ) {
        Level level = entity == null ? null : entity.level();
        if (level == null) {
            return;
        }
        float variance = Math.max(0.0F, config.burpPitchVariance);
        float pitch = config.burpPitchMin + level.getRandom().nextFloat() * variance;
        SoundSource source = entity instanceof Player ? SoundSource.PLAYERS : SoundSource.NEUTRAL;
        float volume = Math.max(0.0F, config.burpVolume);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_BURP, source,
                volume, pitch);
    }
}
