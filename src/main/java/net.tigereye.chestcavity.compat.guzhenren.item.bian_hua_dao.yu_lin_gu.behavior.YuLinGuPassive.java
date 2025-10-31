package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yu_lin_gu.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.common.passive.PassiveHook;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yu_lin_gu.calculator.YuLinGuCalculator;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.yu_lin_gu.tuning.YuLinGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.util.NetworkUtil;

public class YuLinGuPassive implements PassiveHook {

    @Override
    public void onTick(LivingEntity owner, ChestCavityInstance cc, long now) {
        if (!(owner instanceof Player player) || owner.level().isClientSide()) {
            return;
        }
        if (cc == null) {
            return;
        }
        ItemStack organ = YuLinGuCalculator.findYuLinGuOrgan(player);
        if (organ.isEmpty()) {
            return;
        }

        Level level = player.level();
        long gameTime = level.getGameTime();
        OrganState state = new OrganState(organ.getOrCreateTagElement(YuLinGuTuning.STATE_ROOT));

        boolean moistNow = player.isInWaterRainOrBubble();
        if (moistNow) {
            state.setLong(YuLinGuTuning.LAST_WET_TICK_KEY, gameTime);
        }

        boolean hasFishArmor = state.getBoolean(YuLinGuTuning.HAS_FISH_ARMOR_KEY, false);
        int progress = Mth.clamp(state.getInt(YuLinGuTuning.PROGRESS_KEY, 0), 0, YuLinGuTuning.FISH_ARMOR_MAX_PROGRESS);

        if (!hasFishArmor && progress >= YuLinGuTuning.FISH_ARMOR_MAX_PROGRESS) {
            hasFishArmor = true;
            state.setBoolean(YuLinGuTuning.HAS_FISH_ARMOR_KEY, true);
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
            level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.TURTLE_EGG_CRACK,
                SoundSource.PLAYERS,
                0.7f,
                1.2f);
        }

        if (hasFishArmor) {
            double hungerDebt = Math.max(0.0, state.getDouble(YuLinGuTuning.HUNGER_PROGRESS_KEY, 0.0));
            hungerDebt += YuLinGuTuning.HUNGER_COST_PER_SECOND;
            int hungerToConsume = (int) hungerDebt;
            if (hungerToConsume > 0) {
                hungerDebt -= hungerToConsume;
                YuLinGuCalculator.drainHunger(player, hungerToConsume);
            }
            state.setDouble(YuLinGuTuning.HUNGER_PROGRESS_KEY, hungerDebt);

            YuLinGuCalculator.applyArmorBuffs(player, state.getBoolean(YuLinGuTuning.HAS_SHARK_ARMOR_KEY, false));

            if (!YuLinGuCalculator.isPlayerMoist(player, state, gameTime)) {
                hasFishArmor = false;
                state.setBoolean(YuLinGuTuning.HAS_FISH_ARMOR_KEY, false);
                progress = Math.max(0, progress - 2);
                state.setInt(YuLinGuTuning.PROGRESS_KEY, progress);
                NetworkUtil.sendOrganSlotUpdate(cc, organ);
                level.playSound(
                    null, player.blockPosition(), SoundEvents.FISH_SWIM, SoundSource.PLAYERS, 0.5f, 0.8f);
            }
        }

        handleWaterHeal(player, cc, organ, state, state.getBoolean(YuLinGuTuning.HAS_SHARK_ARMOR_KEY, false), gameTime);
        if (level instanceof ServerLevel serverLevel) {
            YuLinGuCalculator.tickSummons(serverLevel, player, gameTime);
        }
    }

    @Override
    public float onHitMelee(LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, float damage, long now) {
        if (!(attacker instanceof Player player)) {
            return damage;
        }
        if (target == null || !target.isAlive()) {
            return damage;
        }
        ItemStack organ = YuLinGuCalculator.findYuLinGuOrgan(player);
        if (organ.isEmpty()) {
            return damage;
        }
        OrganState state = new OrganState(organ.getOrCreateTagElement(YuLinGuTuning.STATE_ROOT));
        boolean hasFishArmor = state.getBoolean(YuLinGuTuning.HAS_FISH_ARMOR_KEY, false);
        if (hasFishArmor) {
            grantProgress(player, cc, organ, state, 1);
        }
        recordWetContact(player, organ);
        return damage;
    }

    @Override
    public float onHurt(LivingEntity self, ChestCavityInstance cc, DamageSource source, float amount, long now) {
        if (!(self instanceof Player player) || amount <= 0.0f) {
            return amount;
        }
        ItemStack organ = YuLinGuCalculator.findYuLinGuOrgan(player);
        if (organ.isEmpty()) {
            return amount;
        }
        OrganState state = new OrganState(organ.getOrCreateTagElement(YuLinGuTuning.STATE_ROOT));
        if (state.getBoolean(YuLinGuTuning.HAS_FISH_ARMOR_KEY, false)) {
            int bonus = Mth.clamp((int) Math.floor(amount / 4.0f), 0, 2);
            if (bonus > 0) {
                grantProgress(player, cc, organ, state, bonus);
            }
        }
        return amount;
    }

    private void grantProgress(
        Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, int amount) {
        if (player == null || cc == null || organ == null || organ.isEmpty() || amount <= 0) {
            return;
        }
        int previous = Mth.clamp(state.getInt(YuLinGuTuning.PROGRESS_KEY, 0), 0, YuLinGuTuning.FISH_ARMOR_MAX_PROGRESS);
        int updated = Mth.clamp(previous + amount, 0, YuLinGuTuning.FISH_ARMOR_MAX_PROGRESS);
        if (updated != previous) {
            state.setInt(YuLinGuTuning.PROGRESS_KEY, updated);
            if (updated >= YuLinGuTuning.SHARK_ARMOR_THRESHOLD) {
                state.setBoolean(YuLinGuTuning.HAS_FISH_ARMOR_KEY, true);
            }
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }
    }

    private void handleWaterHeal(
        Player player,
        ChestCavityInstance cc,
        ItemStack organ,
        OrganState state,
        boolean hasSharkArmor,
        long gameTime) {
        if (!player.isInWaterOrBubble()) {
            return;
        }
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        if (health >= maxHealth * 0.3f) {
            return;
        }
        MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
        MultiCooldown.Entry entry =
            cooldown
                .entry(YuLinGuTuning.WATER_HEAL_READY_AT_KEY)
                .withClamp(value -> Math.max(0L, value))
                .withDefault(0L);
        int cooldownTicks = hasSharkArmor ? YuLinGuTuning.WATER_HEAL_COOLDOWN_FINAL_TICKS : YuLinGuTuning.WATER_HEAL_COOLDOWN_TICKS;
        if (entry.isReady(gameTime)) {
            float healAmount = hasSharkArmor ? 3.0f : 2.0f;
            player.heal(healAmount);
            entry.setReadyAt(gameTime + cooldownTicks);
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.DOLPHIN_SPLASH,
                    SoundSource.PLAYERS,
                    0.6f,
                    1.0f);
            }
        }
    }

    public void recordWetContact(Player player, ItemStack organ) {
        if (player == null || organ == null || organ.isEmpty()) {
            return;
        }
        OrganState state = new OrganState(organ.getOrCreateTagElement(YuLinGuTuning.STATE_ROOT));
        state.setLong(YuLinGuTuning.LAST_WET_TICK_KEY, player.level().getGameTime());
    }
}
