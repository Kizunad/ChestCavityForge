package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.guscript.ability.AbilityFxDispatcher;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper.ConsumptionResult;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Behaviour for 火衣蛊 – provides a pulsing fire aura and an activated flame burst.
 */
public final class HuoYiGuOrganBehavior extends AbstractGuzhenrenOrganBehavior implements OrganSlowTickListener {

    public static final HuoYiGuOrganBehavior INSTANCE = new HuoYiGuOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_gu");
    public static final ResourceLocation ABILITY_ID = ORGAN_ID; // attack ability trigger key

    private static final ResourceLocation YAN_DAO_INCREASE_CHANNEL =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/yan_dao_increase_effect");
    private static final ResourceLocation FIRE_HUO_YI_FX = ResourceLocation.parse("chestcavity:fire_huo_yi");

    private static final String STATE_ROOT = "HuoYiGu";
    private static final String KEY_COOLDOWN_UNTIL = "CooldownUntil";
    private static final String KEY_ACTIVE_UNTIL = "ActiveUntil";
    private static final String KEY_ACTIVE_NEXT_TICK = "ActiveNextTick";
    private static final String KEY_PASSIVE_ACTIVE = "PassiveActive";
    private static final String KEY_PASSIVE_TOGGLE_TICK = "PassiveToggleTick";
    private static final String KEY_PASSIVE_NEXT_TICK = "PassiveNextTick";

    private static final double ACTIVE_ZHENYUAN_COST = 50.0;
    private static final int ACTIVE_HUNGER_COST = 5; // hunger points
    private static final int ACTIVE_DURATION_TICKS = 200; // 10s
    private static final int ACTIVE_COOLDOWN_TICKS = 220; // 11s (10s active + 1s downtime)
    private static final double ACTIVE_RADIUS = 6.0;
    private static final double ACTIVE_DAMAGE_PER_SECOND = 5.0;
    private static final double ACTIVE_SLOWNESS_BASE = 1.0;
    private static final int ACTIVE_SLOWNESS_DURATION_TICKS = 200;

    private static final double PASSIVE_RADIUS = 10.0;
    private static final double PASSIVE_DAMAGE_PER_SECOND = 0.5;
    private static final int PASSIVE_ACTIVE_DURATION_TICKS = 100; // 5s on
    private static final int PASSIVE_DOWNTIME_TICKS = 100; // 5s off

    private static final Predicate<LivingEntity> HOSTILE_TARGET = entity -> entity != null && entity.isAlive();

    private HuoYiGuOrganBehavior() {
        OrganActivationListeners.register(ABILITY_ID, HuoYiGuOrganBehavior::activateAbility);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        Level level = entity.level();
        if (!(level instanceof ServerLevel serverLevel) || level.isClientSide()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        long gameTime = level.getGameTime();
        double multiplier = resolveYanDaoMultiplier(cc);

        boolean dirty = false;
        dirty |= tickActiveAura(serverLevel, entity, state, gameTime, multiplier);
        dirty |= tickPassivePulse(serverLevel, entity, state, gameTime, multiplier);

        if (dirty) {
            sendSlotUpdate(cc, organ);
        }
    }

    private boolean tickActiveAura(ServerLevel level,
                                   LivingEntity user,
                                   OrganState state,
                                   long gameTime,
                                   double multiplier) {
        boolean dirty = false;
        long activeUntil = state.getLong(KEY_ACTIVE_UNTIL, 0L);
        if (activeUntil <= gameTime) {
            if (activeUntil != 0L) {
                dirty |= state.setLong(KEY_ACTIVE_UNTIL, 0L, value -> Math.max(0L, value), 0L).changed();
                dirty |= state.setLong(KEY_ACTIVE_NEXT_TICK, 0L, value -> Math.max(0L, value), 0L).changed();
            }
            return dirty;
        }

        long nextTick = state.getLong(KEY_ACTIVE_NEXT_TICK, 0L);
        if (nextTick > gameTime) {
            return dirty;
        }

        double damage = Math.max(0.0, ACTIVE_DAMAGE_PER_SECOND * multiplier);
        double slowLevel = Math.max(0.0, ACTIVE_SLOWNESS_BASE * multiplier);
        if (damage <= 0.0) {
            dirty |= state.setLong(KEY_ACTIVE_NEXT_TICK, gameTime + 20L, value -> Math.max(0L, value), 0L).changed();
            return dirty;
        }

        List<LivingEntity> targets = collectTargets(level, user, ACTIVE_RADIUS, HOSTILE_TARGET);
        DamageSource source = resolveDamageSource(user);
        int slowAmplifier = Math.max(0, Mth.floor(slowLevel) - 1);
        for (LivingEntity target : targets) {
            if (target == user || target.isAlliedTo(user)) {
                continue;
            }
            target.hurt(source, (float) damage);
            target.setRemainingFireTicks(4 * 20);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    ACTIVE_SLOWNESS_DURATION_TICKS,
                    slowAmplifier,
                    false,
                    true,
                    true));
        }

        dirty |= state.setLong(KEY_ACTIVE_NEXT_TICK, gameTime + 20L, value -> Math.max(0L, value), 0L).changed();
        return dirty;
    }

    private boolean tickPassivePulse(ServerLevel level,
                                     LivingEntity user,
                                     OrganState state,
                                     long gameTime,
                                     double multiplier) {
        boolean dirty = false;
        boolean passiveActive = state.getBoolean(KEY_PASSIVE_ACTIVE, true);
        long toggleTick = state.getLong(KEY_PASSIVE_TOGGLE_TICK, 0L);
        if (toggleTick <= 0L) {
            passiveActive = true;
            dirty |= state.setBoolean(KEY_PASSIVE_ACTIVE, true, true).changed();
            dirty |= state.setLong(KEY_PASSIVE_TOGGLE_TICK,
                    gameTime + PASSIVE_ACTIVE_DURATION_TICKS,
                    value -> Math.max(0L, value),
                    0L).changed();
            dirty |= state.setLong(KEY_PASSIVE_NEXT_TICK, gameTime, value -> Math.max(0L, value), 0L).changed();
            toggleTick = gameTime + PASSIVE_ACTIVE_DURATION_TICKS;
        } else if (gameTime >= toggleTick) {
            passiveActive = !passiveActive;
            dirty |= state.setBoolean(KEY_PASSIVE_ACTIVE, passiveActive, true).changed();
            long nextToggle = gameTime + (passiveActive ? PASSIVE_ACTIVE_DURATION_TICKS : PASSIVE_DOWNTIME_TICKS);
            dirty |= state.setLong(KEY_PASSIVE_TOGGLE_TICK,
                    nextToggle,
                    value -> Math.max(0L, value),
                    0L).changed();
            if (passiveActive) {
                dirty |= state.setLong(KEY_PASSIVE_NEXT_TICK, gameTime, value -> Math.max(0L, value), 0L).changed();
            }
        }

        if (!passiveActive) {
            return dirty;
        }

        long nextTick = state.getLong(KEY_PASSIVE_NEXT_TICK, 0L);
        if (nextTick > gameTime) {
            return dirty;
        }

        double damage = Math.max(0.0, PASSIVE_DAMAGE_PER_SECOND * multiplier);
        if (damage > 0.0) {
            List<LivingEntity> targets = collectTargets(level, user, PASSIVE_RADIUS, HOSTILE_TARGET);
            DamageSource source = resolveDamageSource(user);
            for (LivingEntity target : targets) {
                if (target == user || target.isAlliedTo(user)) {
                    continue;
                }
                target.hurt(source, (float) damage);
                target.setRemainingFireTicks(2 * 20);
            }
        }

        dirty |= state.setLong(KEY_PASSIVE_NEXT_TICK, gameTime + 20L, value -> Math.max(0L, value), 0L).changed();
        return dirty;
    }

    private double resolveYanDaoMultiplier(ChestCavityInstance cc) {
        if (cc == null) {
            return 1.0;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        if (context == null) {
            return 1.0;
        }
        LinkageChannel channel = context.lookupChannel(YAN_DAO_INCREASE_CHANNEL).orElse(null);
        double increase = channel == null ? 0.0 : Math.max(0.0, channel.get());
        return Math.max(0.0, 1.0 + increase);
    }

    private static DamageSource resolveDamageSource(LivingEntity user) {
        if (user instanceof Player player) {
            return player.damageSources().playerAttack(player);
        }
        return user.damageSources().mobAttack(user);
    }

    private static List<LivingEntity> collectTargets(ServerLevel level,
                                                     LivingEntity user,
                                                     double radius,
                                                     Predicate<LivingEntity> predicate) {
        Vec3 center = user.position();
        double radiusSq = radius * radius;
        AABB box = new AABB(center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        return level.getEntitiesOfClass(LivingEntity.class, box, entity ->
                entity != null
                        && entity.isAlive()
                        && entity != user
                        && entity.distanceToSqr(center) <= radiusSq
                        && predicate.test(entity));
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof Player player) || cc == null || entity.level().isClientSide()) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        Level level = entity.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        long gameTime = level.getGameTime();
        OrganState state = INSTANCE.organState(organ, STATE_ROOT);
        long cooldown = state.getLong(KEY_COOLDOWN_UNTIL, 0L);
        if (cooldown > gameTime) {
            return;
        }

        ConsumptionResult payment = GuzhenrenResourceCostHelper.consumeStrict(player, ACTIVE_ZHENYUAN_COST, 0.0);
        if (!payment.succeeded()) {
            return;
        }
        if (!tryConsumeHunger(player, ACTIVE_HUNGER_COST)) {
            GuzhenrenResourceCostHelper.refund(player, payment);
            return;
        }

        long activeUntil = gameTime + ACTIVE_DURATION_TICKS;
        state.setLong(KEY_ACTIVE_UNTIL, activeUntil, value -> Math.max(0L, value), 0L);
        state.setLong(KEY_ACTIVE_NEXT_TICK, gameTime, value -> Math.max(0L, value), 0L);
        state.setLong(KEY_COOLDOWN_UNTIL, gameTime + ACTIVE_COOLDOWN_TICKS, value -> Math.max(0L, value), 0L);
        INSTANCE.sendSlotUpdate(cc, organ);

        if (player instanceof ServerPlayer serverPlayer) {
            AbilityFxDispatcher.play(serverPlayer, FIRE_HUO_YI_FX, Vec3.ZERO, 1.0F);
        } else {
            BuiltInRegistries.SOUND_EVENT.getOptional(FIRE_HUO_YI_FX).ifPresent(sound ->
                    serverLevel.playSound(null, entity.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, 1.0F));
        }
    }

    private static boolean tryConsumeHunger(Player player, int hungerCost) {
        if (player == null || hungerCost <= 0) {
            return true;
        }
        FoodData foodData = player.getFoodData();
        if (foodData == null) {
            return false;
        }
        if (foodData.getFoodLevel() < hungerCost) {
            return false;
        }
        foodData.setFoodLevel(Math.max(0, foodData.getFoodLevel() - hungerCost));
        if (foodData.getSaturationLevel() > foodData.getFoodLevel()) {
            foodData.setSaturation(foodData.getFoodLevel());
        }
        return true;
    }

    private static ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (Objects.equals(id, ORGAN_ID)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
