package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.ChestCavityInventory;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.chestcavities.organs.OrganManager;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.compat.guzhenren.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.SaturationPolicy;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import net.tigereye.chestcavity.util.NBTWriter;
import net.tigereye.chestcavity.util.NetworkUtil;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Behaviour implementation for the Rou Bai Gu organ.
 */
public enum RouBaiguOrganBehavior implements OrganSlowTickListener, OrganOnHitListener, IncreaseEffectContributor {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation BONE_GROWTH_CHANNEL =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/bone_growth");
    private static final ResourceLocation GU_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/gu_dao_increase_effect");
    private static final ResourceLocation XUE_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/xue_dao_increase_effect");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);
    private static final double SOFT_CAP = 120.0;
    private static final double SOFT_CAP_FALLOFF = 0.5;
    private static final SaturationPolicy SOFT_CAP_POLICY = new SaturationPolicy(SOFT_CAP, SOFT_CAP_FALLOFF);

    private static final String STATE_KEY = "RouBaiguState";
    private static final String TARGET_SLOT_KEY = "TargetSlot";
    private static final String TARGET_ITEM_KEY = "TargetItem";
    private static final String PROGRESS_KEY = "Progress";
    private static final String WORK_TIMER_KEY = "WorkTimer";
    private static final String SELECTION_COOLDOWN_KEY = "SelectionCooldown";
    private static final String LAST_BITE_TICK_KEY = "LastBiteTick";

    private static final double COST_BONE_GROWTH = 20.0;
    private static final double HEAL_RATIO = 0.01;
    private static final double COST_ZHENYUAN = 2000.0;
    private static final double COST_JINGLI = 20.0;
    private static final double COST_HUNGER = 10.0;

    private static final double DIGEST_GROWTH_PER_SECOND = 60.0;
    private static final float DIGEST_EXHAUSTION_PER_SECOND = (float)(COST_HUNGER / 60.0);
    private static final float BASE_EXTRA_HEAL = 0.5f;
    private static final double BASE_REGEN_BONUS = 0.5;
    private static final int OUT_OF_COMBAT_THRESHOLD_TICKS = 20 * 5;

    private static final double BITE_TRIGGER_CHANCE = 0.10;
    private static final int BITE_COOLDOWN_TICKS = 20 * 5;
    private static final double BITE_MAX_DISTANCE_SQR = 10.0 * 10.0;

    private static final int RESTORATION_SELECTION_INTERVAL_SECONDS = 180;
    private static final int RESTORATION_WORK_INTERVAL_SECONDS = 120;
    private static final int RESTORATION_PROGRESS_PER_STEP = 10;
    private static final int RESTORATION_REQUIRED_PROGRESS = 100;

    private static final DustParticleOptions DIGEST_PARTICLE =
            new DustParticleOptions(new Vector3f(0.9f, 0.2f, 0.2f), 1.0f);
    private static final DustParticleOptions BITE_BLOOD_PARTICLE =
            new DustParticleOptions(new Vector3f(0.7f, 0.05f, 0.05f), 1.2f);
    private static final DustParticleOptions RESTORE_PARTICLE =
            new DustParticleOptions(new Vector3f(0.2f, 0.8f, 0.2f), 0.9f);

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }

        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        LinkageChannel boneChannel = ensureSoftChannel(context, BONE_GROWTH_CHANNEL);
        LinkageChannel guChannel = ensureChannel(context, GU_DAO_INCREASE_EFFECT);
        LinkageChannel xueChannel = ensureChannel(context, XUE_DAO_INCREASE_EFFECT);

        double efficiency = computeEfficiency(guChannel, xueChannel);
        applyDigestiveGrowth(player, boneChannel, efficiency);
        applyPassiveHealing(player, boneChannel, efficiency);
        processOrganRestoration(player, cc, organ, boneChannel);
    }

    @Override
    public float onHit(
            DamageSource source,
            LivingEntity attacker,
            LivingEntity target,
            ChestCavityInstance cc,
            ItemStack organ,
            float damage
    ) {
        if (attacker == null || attacker.level().isClientSide()) {
            return damage;
        }
        if (!(target instanceof LivingEntity victim) || !victim.isAlive()) {
            return damage;
        }

        // Delegate to specialised handlers so that player and mob attackers can share the organ.
        if (attacker instanceof Player player) {
            return handlePlayerHit(player, victim, cc, organ, damage);
        }
        return handleNonPlayerHit(attacker, victim, cc, organ, damage);
    }

    /**
     * Handles Rou Bai Gu's bite when the attacker is a player.
     */
    private float handlePlayerHit(
            Player player,
            LivingEntity victim,
            ChestCavityInstance cc,
            ItemStack organ,
            float damage
    ) {
        if (!isEligibleVictim(victim) || !isWithinBiteRange(player, victim)) {
            return damage;
        }

        CompoundTag state = readState(organ);
        int now = player.tickCount;
        if (!isCooldownReady(state, now) || !shouldTriggerBite(player)) {
            return damage;
        }

        double increaseSum = resolveIncreaseSum(cc);
        if (wasDodged(player, increaseSum)) {
            playBiteFailure(player);
            stampLastBiteTick(organ, now);
            return damage;
        }
        if (!hasArmorAdvantage(player, victim, increaseSum)) {
            playBiteFailure(player);
            stampLastBiteTick(organ, now);
            return damage;
        }

        float healAmount = calculateBiteHealing(victim, increaseSum);
        applyBiteHealing(player, healAmount);
        applyPlayerExhaustion(player);
        playBiteSuccess(player, victim);
        stampLastBiteTick(organ, now);
        return damage;
    }

    /**
     * Handles Rou Bai Gu's bite when the attacker is a non-player mob.
     */
    private float handleNonPlayerHit(
            LivingEntity attacker,
            LivingEntity victim,
            ChestCavityInstance cc,
            ItemStack organ,
            float damage
    ) {
        if (!isEligibleVictim(victim) || !isWithinBiteRange(attacker, victim)) {
            return damage;
        }

        CompoundTag state = readState(organ);
        int now = attacker.tickCount;
        if (!isCooldownReady(state, now) || !shouldTriggerBite(attacker)) {
            return damage;
        }

        double increaseSum = resolveIncreaseSum(cc);
        if (wasDodged(attacker, increaseSum)) {
            playBiteFailure(attacker);
            stampLastBiteTick(organ, now);
            return damage;
        }
        if (!hasArmorAdvantage(attacker, victim, increaseSum)) {
            playBiteFailure(attacker);
            stampLastBiteTick(organ, now);
            return damage;
        }

        float healAmount = calculateBiteHealing(victim, increaseSum);
        attacker.heal(healAmount);
        playBiteSuccess(attacker, victim);
        stampLastBiteTick(organ, now);
        return damage;
    }

    @Override
    public void rebuildIncreaseEffects(
            ChestCavityInstance cc,
            ActiveLinkageContext context,
            ItemStack organ,
            IncreaseEffectLedger.Registrar registrar
    ) {
        if (organ == null || organ.isEmpty()) {
            return;
        }
        int count = Math.max(1, organ.getCount());
        registrar.record(GU_DAO_INCREASE_EFFECT, count, 0.0);
        registrar.record(XUE_DAO_INCREASE_EFFECT, count, 0.0);
    }

    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        ensureSoftChannel(context, BONE_GROWTH_CHANNEL);
        ensureChannel(context, GU_DAO_INCREASE_EFFECT);
        ensureChannel(context, XUE_DAO_INCREASE_EFFECT);
    }

    /**
     * Validates whether the intended victim can be affected by Rou Bai Gu's bite.
     */
    private static boolean isEligibleVictim(LivingEntity victim) {
        if (victim == null || !victim.isAlive()) {
            return false;
        }
        if (victim instanceof Player victimPlayer) {
            return !(victimPlayer.isCreative() || victimPlayer.isSpectator());
        }
        return true;
    }

    /**
     * Checks if the victim is within the allowed melee distance for the bite to trigger.
     */
    private static boolean isWithinBiteRange(LivingEntity attacker, LivingEntity victim) {
        return attacker != null && victim != null && attacker.distanceToSqr(victim) <= BITE_MAX_DISTANCE_SQR;
    }

    /**
     * Ensures the bite cooldown has elapsed before allowing another trigger.
     */
    private static boolean isCooldownReady(CompoundTag state, int currentTick) {
        int lastTick = state.getInt(LAST_BITE_TICK_KEY);
        return currentTick - lastTick >= BITE_COOLDOWN_TICKS;
    }

    /**
     * Applies the shared trigger chance for both players and mobs.
     */
    private static boolean shouldTriggerBite(LivingEntity attacker) {
        return attacker.getRandom().nextDouble() <= BITE_TRIGGER_CHANCE;
    }

    /**
     * Resolves the total linkage-based increase for Rou Bai Gu calculations, ensuring channels exist.
     */
    private static double resolveIncreaseSum(ChestCavityInstance cc) {
        if (cc == null) {
            return 2.0; // Base efficiency when linkage data is unavailable.
        }
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        LinkageChannel guChannel = ensureChannel(context, GU_DAO_INCREASE_EFFECT);
        LinkageChannel xueChannel = ensureChannel(context, XUE_DAO_INCREASE_EFFECT);
        return computeIncreaseSum(guChannel, xueChannel);
    }

    /**
     * Executes the dodge roll that can negate Rou Bai Gu's bite.
     */
    private static boolean wasDodged(LivingEntity attacker, double increaseSum) {
        double dodgeChance = Math.min(0.95, 0.5 / Math.max(0.5, increaseSum));
        return attacker.getRandom().nextDouble() < dodgeChance;
    }

    /**
     * Verifies the attacker has sufficient armour advantage to pierce the victim.
     */
    private static boolean hasArmorAdvantage(LivingEntity attacker, LivingEntity victim, double increaseSum) {
        double attackerArmor = Math.max(0.0, attacker.getArmorValue());
        double threshold = attackerArmor + increaseSum;
        double targetArmor = Math.max(0.0, victim.getArmorValue());
        return targetArmor < threshold;
    }

    /**
     * Computes how much health is restored when a bite succeeds.
     */
    private static float calculateBiteHealing(LivingEntity victim, double increaseSum) {
        return (float)(victim.getMaxHealth() * HEAL_RATIO * increaseSum);
    }

    /**
     * Applies the stamina tax associated with the bite for player attackers.
     */
    private static void applyPlayerExhaustion(Player player) {
        player.getFoodData().addExhaustion((float)(COST_HUNGER * 0.2f));
    }

    /**
     * Records the tick when the bite last succeeded or was blocked, enforcing cooldown.
     */
    private static void stampLastBiteTick(ItemStack organ, int tick) {
        writeState(organ, tag -> tag.putInt(LAST_BITE_TICK_KEY, tick));
    }

    /**
     * Plays the bite failure cue for both player and non-player attackers.
     */
    private static void playBiteFailure(LivingEntity attacker) {
        if (attacker instanceof Player player) {
            playBiteFailure(player);
            return;
        }
        Level level = attacker.level();
        level.playSound(null, attacker.blockPosition(), SoundEvents.SKELETON_STEP, SoundSource.HOSTILE, 0.5f, 1.6f);
    }

    /**
     * Plays the bite success cue for both player and non-player attackers.
     */
    private static void playBiteSuccess(LivingEntity attacker, LivingEntity target) {
        if (attacker instanceof Player player) {
            playBiteSuccess(player, target);
            return;
        }
        Level level = attacker.level();
        level.playSound(null, target.blockPosition(), SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 1.0f, 0.9f);
        level.playSound(null, target.blockPosition(), SoundEvents.SKELETON_HURT, SoundSource.HOSTILE, 0.6f, 1.2f);
        if (level instanceof ServerLevel server) {
            spawnBloodSpray(server, target);
        }
    }

    private static void applyDigestiveGrowth(Player player, LinkageChannel boneChannel, double efficiency) {
        if (boneChannel == null || boneChannel.get() >= SOFT_CAP) {
            return;
        }
        if (!hasSufficientFood(player)) {
            return;
        }

        double missing = Math.max(0.0, SOFT_CAP - boneChannel.get());
        double gain = Math.min(missing, DIGEST_GROWTH_PER_SECOND * Math.max(0.0, efficiency));
        if (gain <= 0.0) {
            return;
        }

        player.getFoodData().addExhaustion(DIGEST_EXHAUSTION_PER_SECOND);
        boneChannel.adjust(gain);
        spawnDigestParticles(player);
    }

    private static void applyPassiveHealing(Player player, LinkageChannel boneChannel, double efficiency) {
        if (player == null || !player.isAlive()) {
            return;
        }
        if (player.getHealth() >= player.getMaxHealth()) {
            return;
        }

        int lastCombatTick = Math.max(player.getLastHurtByMobTimestamp(), player.getLastHurtMobTimestamp());
        int idleTicks = Math.max(0, player.tickCount - lastCombatTick);

        double regenBonus = BASE_REGEN_BONUS;
        boolean boosted = false;
        if (idleTicks >= OUT_OF_COMBAT_THRESHOLD_TICKS && boneChannel != null && boneChannel.get() >= COST_BONE_GROWTH) {
            boneChannel.adjust(-COST_BONE_GROWTH);
            regenBonus += BASE_REGEN_BONUS;
            boosted = true;
        }

        float healAmount = (float)(BASE_EXTRA_HEAL * regenBonus * Math.max(0.0, efficiency));
        if (healAmount <= 0f) {
            return;
        }

        player.heal(healAmount);
        if (boosted) {
            spawnBoostParticles(player);
        }
    }

    private static void processOrganRestoration(
            Player player,
            ChestCavityInstance cc,
            ItemStack organ,
            LinkageChannel boneChannel
    ) {
        if (player == null || cc == null || organ == null) {
            return;
        }

        CompoundTag state = readState(organ);
        int selectionCooldown = Math.max(0, state.getInt(SELECTION_COOLDOWN_KEY));
        int targetSlot = state.contains(TARGET_SLOT_KEY, Tag.TAG_INT) ? state.getInt(TARGET_SLOT_KEY) : -1;
        String targetItemId = state.contains(TARGET_ITEM_KEY, Tag.TAG_STRING) ? state.getString(TARGET_ITEM_KEY) : "";
        int progress = Math.max(0, state.getInt(PROGRESS_KEY));
        int workTimer = Math.max(0, state.getInt(WORK_TIMER_KEY));

        boolean dirty = false;

        if (selectionCooldown > 0) {
            selectionCooldown--;
            dirty = true;
        }

        if (targetSlot < 0 || targetItemId.isEmpty()) {
            if (selectionCooldown <= 0 && selectMissingOrgan(player, cc, state)) {
                dirty = true;
                targetSlot = state.getInt(TARGET_SLOT_KEY);
                targetItemId = state.getString(TARGET_ITEM_KEY);
                progress = state.getInt(PROGRESS_KEY);
                workTimer = state.getInt(WORK_TIMER_KEY);
                selectionCooldown = state.getInt(SELECTION_COOLDOWN_KEY);
            }
        } else {
            workTimer++;
            dirty = true;

            if (workTimer >= RESTORATION_WORK_INTERVAL_SECONDS) {
                boolean hungerAvailable = hasHungerForRestoration(player);
                if (!hungerAvailable) {
                    mutateOrganSlot(player, cc, targetSlot);
                    resetRestorationState(state);
                    targetSlot = -1;
                    targetItemId = "";
                    progress = 0;
                    workTimer = 0;
                    selectionCooldown = RESTORATION_SELECTION_INTERVAL_SECONDS;
                    dirty = true;
                } else if (consumeRestorationResources(player, boneChannel)) {
                    consumeRestorationHunger(player);
                    progress = Math.min(RESTORATION_REQUIRED_PROGRESS, progress + RESTORATION_PROGRESS_PER_STEP);
                    workTimer = 0;
                    dirty = true;
                    spawnRestorationPulse(player);
                    if (progress >= RESTORATION_REQUIRED_PROGRESS) {
                        restoreOrgan(player, cc, targetSlot, targetItemId);
                        resetRestorationState(state);
                        targetSlot = -1;
                        targetItemId = "";
                        progress = 0;
                        workTimer = 0;
                        selectionCooldown = RESTORATION_SELECTION_INTERVAL_SECONDS;
                        dirty = true;
                    }
                } else {
                    workTimer = 0;
                    dirty = true;
                }
            }
        }

        if (dirty) {
            state.putInt(SELECTION_COOLDOWN_KEY, selectionCooldown);
            if (targetSlot >= 0 && !targetItemId.isEmpty()) {
                state.putInt(TARGET_SLOT_KEY, targetSlot);
                state.putString(TARGET_ITEM_KEY, targetItemId);
                state.putInt(PROGRESS_KEY, progress);
                state.putInt(WORK_TIMER_KEY, workTimer);
            } else {
                state.remove(TARGET_SLOT_KEY);
                state.remove(TARGET_ITEM_KEY);
                state.remove(PROGRESS_KEY);
                state.remove(WORK_TIMER_KEY);
            }
            CompoundTag snapshot = state.copy();
            writeState(organ, updated -> {
                for (String key : new ArrayList<>(updated.getAllKeys())) {
                    updated.remove(key);
                }
                updated.merge(snapshot);
            });
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }
    }

    private static boolean hasSufficientFood(Player player) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        FoodData foodData = player.getFoodData();
        return foodData.getFoodLevel() > 0 || foodData.getSaturationLevel() > 0.0f;
    }

    private static boolean consumeRestorationResources(Player player, LinkageChannel boneChannel) {
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return false;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
        double jingliCost = COST_JINGLI * 0.2;
        double zhenyuanCost = COST_ZHENYUAN * 0.2;

        var jingliBeforeOpt = handle.getJingli();
        if (jingliBeforeOpt.isEmpty() || jingliBeforeOpt.getAsDouble() + 1.0E-4 < jingliCost) {
            return false;
        }

        if (handle.adjustJingli(-jingliCost, true).isEmpty()) {
            return false;
        }

        if (handle.consumeScaledZhenyuan(zhenyuanCost).isEmpty()) {
            handle.adjustJingli(jingliCost, true);
            return false;
        }

        if (boneChannel != null) {
            boneChannel.adjust(COST_BONE_GROWTH * 0.5);
        }

        return true;
    }

    private static void consumeRestorationHunger(Player player) {
        if (player.getAbilities().instabuild) {
            return;
        }
        FoodData food = player.getFoodData();
        int newFood = Math.max(0, food.getFoodLevel() - (int)COST_HUNGER);
        food.setFoodLevel(newFood);
        food.setSaturation(Math.max(0.0f, food.getSaturationLevel() - (float)COST_HUNGER));
    }

    private static boolean hasHungerForRestoration(Player player) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        return player.getFoodData().getFoodLevel() >= COST_HUNGER;
    }

    private static boolean selectMissingOrgan(Player player, ChestCavityInstance cc, CompoundTag state) {
        ChestCavityInventory defaults = cc.getChestCavityType().getDefaultChestCavity();
        if (defaults == null) {
            return false;
        }

        List<MissingOrgan> missing = new ArrayList<>();
        for (int i = 0; i < defaults.getContainerSize(); i++) {
            ItemStack defaultStack = defaults.getItem(i);
            if (defaultStack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(defaultStack.getItem());
            if (!"chestcavity".equals(id.getNamespace())) {
                continue;
            }
            ItemStack current = cc.inventory.getItem(i);
            if (current.isEmpty() || !ItemStack.isSameItem(current, defaultStack)) {
                missing.add(new MissingOrgan(i, id));
            }
        }

        if (missing.isEmpty()) {
            return false;
        }

        MissingOrgan selected = missing.get(player.getRandom().nextInt(missing.size()));
        state.putInt(TARGET_SLOT_KEY, selected.slot());
        state.putString(TARGET_ITEM_KEY, selected.id().toString());
        state.putInt(PROGRESS_KEY, 0);
        state.putInt(WORK_TIMER_KEY, 0);
        state.putInt(SELECTION_COOLDOWN_KEY, RESTORATION_SELECTION_INTERVAL_SECONDS);
        return true;
    }

    private static void restoreOrgan(Player player, ChestCavityInstance cc, int slot, String itemId) {
        if (slot < 0 || itemId == null || itemId.isBlank()) {
            return;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(itemId);
        if (parsed == null) {
            return;
        }
        Item restoredItem = BuiltInRegistries.ITEM.get(parsed);
        if (restoredItem == null || restoredItem == net.minecraft.world.item.Items.AIR) {
            return;
        }

        ItemStack previous = cc.inventory.getItem(slot);
        if (!previous.isEmpty()) {
            player.spawnAtLocation(previous);
        }

        cc.inventory.setItem(slot, new ItemStack(restoredItem));
        ChestCavityUtil.evaluateChestCavity(cc);
        playRestorationEffects(player);
    }

    private static void mutateOrganSlot(Player player, ChestCavityInstance cc, int slot) {
        if (slot < 0) {
            return;
        }
        Item randomOrgan = pickRandomOrgan(player.getRandom());
        if (randomOrgan == null) {
            return;
        }
        ItemStack mutated = new ItemStack(randomOrgan);
        ItemStack previous = cc.inventory.getItem(slot);
        if (!previous.isEmpty()) {
            player.spawnAtLocation(previous);
        }
        cc.inventory.setItem(slot, mutated);
        ChestCavityUtil.evaluateChestCavity(cc);
        playMutationCue(player);
    }

    private static Item pickRandomOrgan(RandomSource random) {
        if (OrganManager.GeneratedOrganData.isEmpty()) {
            return null;
        }
        List<ResourceLocation> ids = new ArrayList<>(OrganManager.GeneratedOrganData.keySet());
        ResourceLocation id = ids.get(random.nextInt(ids.size()));
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
    }

    private static void resetRestorationState(CompoundTag state) {
        state.remove(TARGET_SLOT_KEY);
        state.remove(TARGET_ITEM_KEY);
        state.remove(PROGRESS_KEY);
        state.remove(WORK_TIMER_KEY);
    }

    private static void playRestorationEffects(Player player) {
        Level level = player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f);
        if (level instanceof ServerLevel serverLevel) {
            spawnParticlesAround(serverLevel, player, RESTORE_PARTICLE, 24, 0.8);
            spawnParticlesAround(serverLevel, player, ParticleTypes.HAPPY_VILLAGER, 12, 0.6);
        }
    }

    private static void playMutationCue(Player player) {
        Level level = player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMITE_STEP, SoundSource.PLAYERS, 0.8f, 0.8f);
        if (level instanceof ServerLevel serverLevel) {
            spawnParticlesAround(serverLevel, player, ParticleTypes.SMOKE, 10, 0.5);
        }
    }

    private static void spawnRestorationPulse(Player player) {
        if (!(player.level() instanceof ServerLevel server)) {
            return;
        }
        spawnParticlesAround(server, player, ParticleTypes.HEART, 4, 0.4);
    }

    private static void spawnBoostParticles(Player player) {
        if (!(player.level() instanceof ServerLevel server)) {
            return;
        }
        spawnParticlesAround(server, player, ParticleTypes.END_ROD, 4, 0.5);
    }

    private static void spawnDigestParticles(Player player) {
        if (!(player.level() instanceof ServerLevel server)) {
            return;
        }
        spawnParticlesAround(server, player, DIGEST_PARTICLE, 6, 0.5);
    }

    private static void playBiteSuccess(Player player, LivingEntity target) {
        Level level = player.level();
        level.playSound(null, target.blockPosition(), SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.PLAYERS, 1.0f, 0.9f);
        level.playSound(null, target.blockPosition(), SoundEvents.SKELETON_HURT, SoundSource.PLAYERS, 0.6f, 1.2f);
        if (level instanceof ServerLevel server) {
            spawnBloodSpray(server, target);
        }
    }

    private static void playBiteFailure(Player player) {
        Level level = player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.SKELETON_STEP, SoundSource.PLAYERS, 0.5f, 1.6f);
    }

    private static void spawnBloodSpray(ServerLevel server, LivingEntity target) {
        Vec3 pos = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
        RandomSource random = target.getRandom();
        for (int i = 0; i < 12; i++) {
            double speed = 0.1 + random.nextDouble() * 0.1;
            double vx = (random.nextDouble() - 0.5) * speed;
            double vy = random.nextDouble() * speed;
            double vz = (random.nextDouble() - 0.5) * speed;
            server.sendParticles(BITE_BLOOD_PARTICLE, pos.x, pos.y, pos.z, 1, vx, vy, vz, 0.02);
            server.sendParticles(ParticleTypes.ITEM_SLIME, pos.x, pos.y, pos.z, 1, vx, vy, vz, 0.01);
        }
    }

    private static void spawnParticlesAround(ServerLevel server, LivingEntity entity, ParticleOptions particle, int count, double radius) {
        Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
        RandomSource random = entity.getRandom();
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i / count) + random.nextDouble() * 0.25;
            double distance = radius + (random.nextDouble() - 0.5) * 0.1;
            double x = center.x + Math.cos(angle) * distance;
            double z = center.z + Math.sin(angle) * distance;
            double y = center.y + (random.nextDouble() - 0.5) * 0.3;
            server.sendParticles(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void applyBiteHealing(Player player, float amount) {
        float maxHealth = player.getMaxHealth();
        float current = player.getHealth();
        float target = Math.min(maxHealth, current + amount);
        player.setHealth(target);
        float overflow = Math.max(0.0f, current + amount - maxHealth);
        if (overflow > 0.0f) {
            int amplifier = Math.max(0, (int)Math.ceil(overflow / 4.0f) - 1);
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 180, amplifier, true, true, true));
        }
    }

    private static double computeIncreaseSum(LinkageChannel guChannel, LinkageChannel xueChannel) {
        double gu = guChannel == null ? 0.0 : Math.max(0.0, guChannel.get());
        double xue = xueChannel == null ? 0.0 : Math.max(0.0, xueChannel.get());
        return (1.0 + gu) + (1.0 + xue);
    }

    private static double computeEfficiency(LinkageChannel guChannel, LinkageChannel xueChannel) {
        return computeIncreaseSum(guChannel, xueChannel) / 2.0;
    }

    private static LinkageChannel ensureChannel(ActiveLinkageContext context, ResourceLocation id) {
        return context.getOrCreateChannel(id).addPolicy(NON_NEGATIVE);
    }

    private static LinkageChannel ensureSoftChannel(ActiveLinkageContext context, ResourceLocation id) {
        return context.getOrCreateChannel(id)
                .addPolicy(NON_NEGATIVE)
                .addPolicy(SOFT_CAP_POLICY);
    }

    private static CompoundTag readState(ItemStack organ) {
        if (organ == null || organ.isEmpty()) {
            return new CompoundTag();
        }
        CustomData data = organ.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return new CompoundTag();
        }
        CompoundTag root = data.copyTag();
        if (!root.contains(STATE_KEY, Tag.TAG_COMPOUND)) {
            return new CompoundTag();
        }
        return root.getCompound(STATE_KEY).copy();
    }

    private static void writeState(ItemStack organ, java.util.function.Consumer<CompoundTag> modifier) {
        if (organ == null || organ.isEmpty()) {
            return;
        }
        NBTWriter.updateCustomData(organ, tag -> {
            CompoundTag state = tag.contains(STATE_KEY, Tag.TAG_COMPOUND) ? tag.getCompound(STATE_KEY) : new CompoundTag();
            modifier.accept(state);
            tag.put(STATE_KEY, state);
        });
    }

    private record MissingOrgan(int slot, ResourceLocation id) {}
}
