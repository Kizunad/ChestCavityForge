package net.tigereye.chestcavity.compat.guzhenren.item.yin_shi.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.AttributeOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.CountdownOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.util.AbsorptionHelper;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.ElderGuardian;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import org.joml.Vector3f;

/**
 * 核心行为：隐石蛊。
 */
public final class YinShiGuOrganBehavior extends AbstractGuzhenrenOrganBehavior implements OrganSlowTickListener,
        OrganIncomingDamageListener, OrganOnHitListener, OrganRemovalListener {

    public static final YinShiGuOrganBehavior INSTANCE = new YinShiGuOrganBehavior();

    private YinShiGuOrganBehavior() {
    }

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "yin_shi_gu");
    private static final ResourceLocation SKILL_TUNNEL_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "skill/yinshi_tunnel");
    private static final ResourceLocation SKILL_STATUE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "skill/yinshi_statue");
    private static final ResourceLocation LINK_STEALTH = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/yinshi/stealth");
    private static final ResourceLocation LINK_ABSORB_CD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/yinshi/absorb_cd");
    private static final ResourceLocation LINK_TUNNEL_CD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/yinshi/tunnel_cd");
    private static final ResourceLocation LINK_STATUE_CD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/yinshi/statue_cd");
    private static final ResourceLocation ABSORPTION_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/yinshi_shield");
    private static final ResourceLocation WARDEN_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "warden");
    private static final ClampPolicy ZERO_ONE = new ClampPolicy(0.0D, 1.0D);

    private static final ResourceLocation TUNNEL_KNOCKBACK_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID,
            "modifiers/yinshi_tunnel_knockback");
    private static final ResourceLocation STATUE_KNOCKBACK_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID,
            "modifiers/yinshi_statue_knockback");

    private static final DustParticleOptions TUNNEL_SMOKE_PARTICLE =
            new DustParticleOptions(new Vector3f(0.65f, 0.65f, 0.65f), 0.9f);

    private static final String STATE_ROOT = "YinShiGu";
    private static final String KEY_TIER = "tier";
    private static final String KEY_STILL_SINCE = "stillSince";
    private static final String KEY_STEALTH_ACTIVE = "stealthActive";
    private static final String KEY_STEALTH_SINCE = "stealthSince";
    private static final String KEY_STEALTH_GRACE_UNTIL = "stealthGraceUntil";
    private static final String KEY_COMBAT_FREEZE_UNTIL = "combatFreezeUntil";
    private static final String KEY_ABSORB_TARGET = "absorbTarget";
    private static final String KEY_ABSORB_TOTAL = "absorbTotal";
    private static final String KEY_ABSORB_CD_UNTIL = "absorbCdUntil";
    private static final String KEY_LURK_PROGRESS = "lurkProgress";
    private static final String KEY_EP_LURK_TOTAL = "epLurkTotal";
    private static final String KEY_EP_FIRST_TOTAL = "epFirstStrikeTotal";
    private static final String KEY_EP_ABSORB_TOTAL = "epAbsorbTotal";
    private static final String KEY_EP_ABSORB_FLOOR = "epAbsorbFloor";
    private static final String KEY_EP_TUNNEL_TOTAL = "epTunnelTotal";
    private static final String KEY_TUNNEL_WINDOW_START = "tunnelWindowStart";
    private static final String KEY_TUNNEL_WINDOW_COUNT = "tunnelWindowCount";
    private static final String KEY_UNLOCK_PASSIVE = "unlockPassive";
    private static final String KEY_UNLOCK_STATUE = "unlockStatue";
    private static final String KEY_FIRST_STRIKE_READY = "firstStrikeReady";
    private static final String KEY_TUNNEL_READY_AT = "tunnelReadyAt";
    private static final String KEY_STATUE_READY_AT = "statueReadyAt";

    private static final int MAX_TIER = 5;

    private static final int PASSIVE_UNLOCK_THRESHOLD = 90;
    private static final int STATUE_UNLOCK_THRESHOLD = 140;

    private static final int COMBAT_FREEZE_TICKS = 60;
    private static final int ABSORB_BREAK_COOLDOWN_TICKS = 60;
    private static final int FIRST_STRIKE_EP_COOLDOWN_TICKS = 30 * 20;
    private static final int TUNNEL_WINDOW_TICKS = 10 * 20;

    private static final double BASE_ENTRY_SECONDS = BehaviorConfigAccess.getFloat(YinShiGuOrganBehavior.class, "STEALTH_ENTRY_SECONDS", 1.0F);
    private static final double PROJECTILE_REDUCTION_BASE = BehaviorConfigAccess.getFloat(YinShiGuOrganBehavior.class, "PROJECTILE_REDUCTION_BASE", 0.20F);
    private static final double PROJECTILE_REDUCTION_STEP = BehaviorConfigAccess.getFloat(YinShiGuOrganBehavior.class, "PROJECTILE_REDUCTION_STEP", 0.05F);
    private static final double ABSORB_MAX_BASE = BehaviorConfigAccess.getFloat(YinShiGuOrganBehavior.class, "ABSORB_MAX_BASE", 6.0F);
    private static final double ABSORB_MAX_STEP = BehaviorConfigAccess.getFloat(YinShiGuOrganBehavior.class, "ABSORB_MAX_STEP", 2.0F);
    private static final double ABSORB_REGEN_STEP = BehaviorConfigAccess.getFloat(YinShiGuOrganBehavior.class, "ABSORB_REGEN_STEP", 1.5F);
    private static final double ABSORB_CONVERT_RATIO = BehaviorConfigAccess.getFloat(YinShiGuOrganBehavior.class, "ABSORB_CONVERT_RATIO", 0.35F);
    private static final double ABSORB_CONVERT_STEP = BehaviorConfigAccess.getFloat(YinShiGuOrganBehavior.class, "ABSORB_CONVERT_STEP", 0.05F);
    private static final double ABSORB_PER_HIT_CAP = BehaviorConfigAccess.getFloat(YinShiGuOrganBehavior.class, "ABSORB_PER_HIT_CAP", 4.0F);
    private static final double ABSORB_PER_HIT_STEP = BehaviorConfigAccess.getFloat(YinShiGuOrganBehavior.class, "ABSORB_PER_HIT_STEP", 1.0F);

    private static final double TUNNEL_RANGE_BASE = BehaviorConfigAccess.getFloat(YinShiGuOrganBehavior.class, "TUNNEL_RANGE_BASE", 3.5F);
    private static final double TUNNEL_RANGE_STEP = BehaviorConfigAccess.getFloat(YinShiGuOrganBehavior.class, "TUNNEL_RANGE_STEP", 0.5F);
    private static final double TUNNEL_ABSORB_BONUS = BehaviorConfigAccess.getFloat(YinShiGuOrganBehavior.class, "TUNNEL_ABSORB_BONUS", 2.0F);
    private static final double TUNNEL_ABSORB_STEP = BehaviorConfigAccess.getFloat(YinShiGuOrganBehavior.class, "TUNNEL_ABSORB_STEP", 0.5F);
    private static final double TUNNEL_KNOCKBACK_BASE = BehaviorConfigAccess.getFloat(YinShiGuOrganBehavior.class, "TUNNEL_KNOCKBACK_BASE", 0.3F);
    private static final double TUNNEL_KNOCKBACK_STEP = BehaviorConfigAccess.getFloat(YinShiGuOrganBehavior.class, "TUNNEL_KNOCKBACK_STEP", 0.05F);

    private static final double STATUE_KNOCKBACK_VALUE = BehaviorConfigAccess.getFloat(YinShiGuOrganBehavior.class, "STATUE_KNOCKBACK", 1.0F);

    private static final double STATIONARY_SPEED_THRESHOLD = 0.01D;

    private static final double TUNNEL_BASE_COST = 12.0D;
    private static final double STATUE_BASE_COST = 28.0D;

    private static final int TUNNEL_COOLDOWN_BASE_TICKS = 18 * 20;
    private static final int TUNNEL_COOLDOWN_TIER4_TICKS = 16 * 20;
    private static final int STATUE_COOLDOWN_TICKS = 60 * 20;

    private static final int TUNNEL_BUFF_DURATION_TICKS = 40;
    private static final int STATUE_DURATION_TICKS = 50;

    static {
        OrganActivationListeners.register(SKILL_TUNNEL_ID, YinShiGuOrganBehavior::activateTunnel);
        OrganActivationListeners.register(SKILL_STATUE_ID, YinShiGuOrganBehavior::activateStatue);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (entity.level().isClientSide() || !matchesOrgan(organ, ORGAN_ID)) {
            return;
        }

        Level level = entity.level();
        long gameTime = level.getGameTime();

        OrganState state = OrganState.of(organ, STATE_ROOT);
        OrganStateOps.Collector collector = OrganStateOps.collector(cc, organ);

        int tier = Mth.clamp(state.getInt(KEY_TIER, 1), 1, MAX_TIER);

        boolean stealthActive = updateStealthState(player, cc, organ, state, collector, gameTime, tier);
        updateAbsorption(player, cc, organ, state, collector, gameTime, tier, stealthActive);
        updateEpProgress(player, cc, organ, state, collector, gameTime, stealthActive);
        updateUnlocks(state, cc, organ, collector);
        updateCooldownChannels(cc, state, gameTime, tier);

        collector.commit();
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(victim instanceof Player player) || cc == null || organ == null || organ.isEmpty()) {
            return damage;
        }
        if (victim.level().isClientSide() || !matchesOrgan(organ, ORGAN_ID)) {
            return damage;
        }
        OrganState state = OrganState.of(organ, STATE_ROOT);
        int tier = Mth.clamp(state.getInt(KEY_TIER, 1), 1, MAX_TIER);
        Level level = victim.level();
        long gameTime = level.getGameTime();

        boolean stealth = state.getBoolean(KEY_STEALTH_ACTIVE, false);
        if (stealth && source != null && source.is(DamageTypeTags.IS_PROJECTILE)) {
            float reduction = (float) projectileReductionForTier(tier);
            damage = Math.max(0.0F, damage * (1.0F - reduction));
        }
        if (damage <= 0.0F) {
            return 0.0F;
        }

        double ratio = absorptionConvertRatio(tier);
        double perHitCap = absorptionPerHitCap(tier);
        double converted = Math.min(damage * ratio, perHitCap);
        if (converted > 0.0D) {
            double currentTarget = state.getDouble(KEY_ABSORB_TARGET, 0.0D);
            double max = absorptionMaxForTier(tier);
            double target = Math.min(max, currentTarget + converted);
            OrganStateOps.setDouble(state, cc, organ, KEY_ABSORB_TARGET, target, value -> Math.max(0.0D, value), 0.0D);
            AbsorptionHelper.applyAbsorption(player, target, ABSORPTION_MODIFIER_ID, false);
            recordAbsorbProgress(state, cc, organ, converted);
        }

        OrganStateOps.setLong(state, cc, organ, KEY_ABSORB_CD_UNTIL, gameTime + ABSORB_BREAK_COOLDOWN_TICKS, value -> Math.max(0L, value), 0L);
        OrganStateOps.setLong(state, cc, organ, KEY_COMBAT_FREEZE_UNTIL, gameTime + COMBAT_FREEZE_TICKS, value -> Math.max(0L, value), 0L);
        OrganStateOps.setBoolean(state, cc, organ, KEY_STEALTH_ACTIVE, false, false);
        OrganStateOps.setLong(state, cc, organ, KEY_STEALTH_GRACE_UNTIL, 0L, value -> Math.max(0L, value), 0L);
        updateStealthChannel(cc, false);
        updateAbsorbCooldownChannel(cc, state, gameTime);

        return damage;
    }

    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(attacker instanceof Player player) || target == null || cc == null || organ == null || organ.isEmpty()) {
            return damage;
        }
        if (attacker.level().isClientSide() || !matchesOrgan(organ, ORGAN_ID)) {
            return damage;
        }
        OrganState state = OrganState.of(organ, STATE_ROOT);
        if (!state.getBoolean(KEY_STEALTH_ACTIVE, false)) {
            return damage;
        }
        applyFirstStrike(player, target, cc, organ, state);
        return damage;
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player)) {
            return;
        }
        AbsorptionHelper.clearAbsorptionCapacity(player, ABSORPTION_MODIFIER_ID);
        removeKnockbackModifier(player, TUNNEL_KNOCKBACK_ID);
        removeKnockbackModifier(player, STATUE_KNOCKBACK_ID);
        ActiveLinkageContext context = LedgerOps.context(cc);
        if (context != null) {
            Optional.ofNullable(LedgerOps.ensureChannel(context, LINK_STEALTH, ZERO_ONE)).ifPresent(channel -> channel.set(0.0D));
            Optional.ofNullable(LedgerOps.ensureChannel(context, LINK_ABSORB_CD, ZERO_ONE)).ifPresent(channel -> channel.set(0.0D));
            Optional.ofNullable(LedgerOps.ensureChannel(context, LINK_TUNNEL_CD, ZERO_ONE)).ifPresent(channel -> channel.set(0.0D));
            Optional.ofNullable(LedgerOps.ensureChannel(context, LINK_STATUE_CD, ZERO_ONE)).ifPresent(channel -> channel.set(0.0D));
        }
    }

    public void ensureAttached(ChestCavityInstance cc) {
        LedgerOps.ensureChannel(cc, LINK_STEALTH, ZERO_ONE);
        LedgerOps.ensureChannel(cc, LINK_ABSORB_CD, ZERO_ONE);
        LedgerOps.ensureChannel(cc, LINK_TUNNEL_CD, ZERO_ONE);
        LedgerOps.ensureChannel(cc, LINK_STATUE_CD, ZERO_ONE);
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        registerRemovalHook(cc, organ, this, staleRemovalContexts);
        ensureAttached(cc);
        sendSlotUpdate(cc, organ);
    }

    private boolean updateStealthState(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state,
                                       OrganStateOps.Collector collector, long gameTime, int tier) {
        boolean crouching = player.isCrouching();
        boolean onGround = player.onGround();
        boolean movingSlow = player.getDeltaMovement().horizontalDistanceSqr() <= STATIONARY_SPEED_THRESHOLD * STATIONARY_SPEED_THRESHOLD;
        boolean validPose = !player.isSwimming() && !player.isFallFlying() && !player.isPassenger();
        boolean stationary = crouching && onGround && movingSlow && validPose;

        long stillSince = state.getLong(KEY_STILL_SINCE, 0L);
        if (stationary) {
            if (stillSince <= 0L) {
                collector.record(OrganStateOps.setLong(state, cc, organ, KEY_STILL_SINCE, gameTime, value -> Math.max(0L, value), 0L));
                stillSince = gameTime;
            }
        } else if (stillSince != 0L) {
            collector.record(OrganStateOps.setLong(state, cc, organ, KEY_STILL_SINCE, 0L, value -> Math.max(0L, value), 0L));
            stillSince = 0L;
        }

        long graceUntil = state.getLong(KEY_STEALTH_GRACE_UNTIL, 0L);
        long freezeUntil = state.getLong(KEY_COMBAT_FREEZE_UNTIL, 0L);
        long entryDelayTicks = (long) Math.max(1L, Math.round(BASE_ENTRY_SECONDS * 20.0D)) - Math.min(2L, tier - 1);
        entryDelayTicks = Math.max(12L, entryDelayTicks);

        boolean active = state.getBoolean(KEY_STEALTH_ACTIVE, false);
        boolean shouldActivate = active;
        if (stationary && stillSince > 0L && gameTime - stillSince >= entryDelayTicks && gameTime >= freezeUntil) {
            shouldActivate = true;
            collector.record(OrganStateOps.setLong(state, cc, organ, KEY_STEALTH_SINCE, gameTime, value -> Math.max(0L, value), 0L));
        } else if (!stationary) {
            boolean passiveUnlocked = state.getBoolean(KEY_UNLOCK_PASSIVE, false) || tier >= 4;
            if (passiveUnlocked) {
                long desiredGrace = gameTime + 40L;
                if (graceUntil < desiredGrace) {
                    collector.record(OrganStateOps.setLong(state, cc, organ, KEY_STEALTH_GRACE_UNTIL, desiredGrace, value -> Math.max(0L, value), 0L));
                    graceUntil = desiredGrace;
                }
                if (gameTime < graceUntil && gameTime >= freezeUntil) {
                    shouldActivate = true;
                } else {
                    shouldActivate = false;
                }
            } else {
                shouldActivate = false;
            }
        } else if (gameTime >= freezeUntil && graceUntil > 0L && gameTime < graceUntil) {
            shouldActivate = true;
        } else if (gameTime >= freezeUntil && stationary) {
            shouldActivate = active;
        }

        if (shouldActivate != active) {
            collector.record(OrganStateOps.setBoolean(state, cc, organ, KEY_STEALTH_ACTIVE, shouldActivate, false));
            if (!shouldActivate) {
                collector.record(OrganStateOps.setLong(state, cc, organ, KEY_STEALTH_GRACE_UNTIL, 0L, value -> Math.max(0L, value), 0L));
            }
        }
        updateStealthChannel(cc, shouldActivate);
        return shouldActivate;
    }

    private void updateAbsorption(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state,
                                   OrganStateOps.Collector collector, long gameTime, int tier, boolean stealthActive) {
        double target = state.getDouble(KEY_ABSORB_TARGET, 0.0D);
        double max = absorptionMaxForTier(tier);
        long freezeUntil = state.getLong(KEY_COMBAT_FREEZE_UNTIL, 0L);
        long breakUntil = state.getLong(KEY_ABSORB_CD_UNTIL, 0L);
        boolean canRegen = stealthActive && gameTime >= freezeUntil && gameTime >= breakUntil;
        double step = ABSORB_REGEN_STEP + (tier - 1) * 0.35D;
        if (canRegen) {
            target = Math.min(max, target + step);
        } else if (!stealthActive) {
            target = Math.max(0.0D, target - step * 0.5D);
        }
        collector.record(OrganStateOps.setDouble(state, cc, organ, KEY_ABSORB_TARGET, target, value -> Math.max(0.0D, value), 0.0D));
        AbsorptionHelper.applyAbsorption(player, target, ABSORPTION_MODIFIER_ID, false);
        updateAbsorbCooldownChannel(cc, state, gameTime);
    }

    private void updateEpProgress(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state,
                                  OrganStateOps.Collector collector, long gameTime, boolean stealthActive) {
        double progress = state.getDouble(KEY_LURK_PROGRESS, 0.0D);
        boolean inCombat = isPlayerInCombat(player);
        if (stealthActive) {
            progress += inCombat ? 0.5D : 1.0D;
        }
        int gained = (int) Math.floor(progress);
        progress -= gained;
        if (gained > 0) {
            int current = state.getInt(KEY_EP_LURK_TOTAL, 0);
            collector.record(OrganStateOps.setInt(state, cc, organ, KEY_EP_LURK_TOTAL,
                    Math.min(Integer.MAX_VALUE, current + gained), value -> Math.max(0, value), 0));
        }
        collector.record(OrganStateOps.setDouble(state, cc, organ, KEY_LURK_PROGRESS, progress,
                value -> Mth.clamp(value, 0.0D, 1000.0D), 0.0D));

        long windowStart = state.getLong(KEY_TUNNEL_WINDOW_START, 0L);
        int windowCount = state.getInt(KEY_TUNNEL_WINDOW_COUNT, 0);
        if (windowStart > 0L && gameTime - windowStart >= TUNNEL_WINDOW_TICKS) {
            collector.record(OrganStateOps.setLong(state, cc, organ, KEY_TUNNEL_WINDOW_START, 0L, value -> Math.max(0L, value), 0L));
            collector.record(OrganStateOps.setInt(state, cc, organ, KEY_TUNNEL_WINDOW_COUNT, 0, value -> Math.max(0, value), 0));
        }
    }

    private void updateUnlocks(OrganState state, ChestCavityInstance cc, ItemStack organ, OrganStateOps.Collector collector) {
        int lurk = state.getInt(KEY_EP_LURK_TOTAL, 0);
        int first = state.getInt(KEY_EP_FIRST_TOTAL, 0);
        int tunnel = state.getInt(KEY_EP_TUNNEL_TOTAL, 0);
        int absorbFloor = state.getInt(KEY_EP_ABSORB_FLOOR, 0);
        int total = Math.min(Integer.MAX_VALUE, lurk + first + tunnel + absorbFloor);
        boolean passiveUnlocked = total >= PASSIVE_UNLOCK_THRESHOLD;
        boolean statueUnlocked = total >= STATUE_UNLOCK_THRESHOLD;
        collector.record(OrganStateOps.setBoolean(state, cc, organ, KEY_UNLOCK_PASSIVE, passiveUnlocked, false));
        collector.record(OrganStateOps.setBoolean(state, cc, organ, KEY_UNLOCK_STATUE, statueUnlocked, false));
    }

    private void updateCooldownChannels(ChestCavityInstance cc, OrganState state, long gameTime, int tier) {
        long tunnelReadyAt = state.getLong(KEY_TUNNEL_READY_AT, 0L);
        long statueReadyAt = state.getLong(KEY_STATUE_READY_AT, 0L);
        int tunnelCooldown = tunnelCooldownForTier(tier);
        double tunnelValue = 0.0D;
        if (tunnelReadyAt > gameTime) {
            tunnelValue = Math.min(1.0D, (double) (tunnelReadyAt - gameTime) / (double) tunnelCooldown);
        }
        double statueValue = 0.0D;
        if (statueReadyAt > gameTime) {
            statueValue = Math.min(1.0D, (double) (statueReadyAt - gameTime) / (double) STATUE_COOLDOWN_TICKS);
        }
        updateChannel(cc, LINK_TUNNEL_CD, tunnelValue);
        updateChannel(cc, LINK_STATUE_CD, statueValue);
    }

    private static void updateStealthChannel(ChestCavityInstance cc, boolean active) {
        updateChannel(cc, LINK_STEALTH, active ? 1.0D : 0.0D);
    }

    private static void updateAbsorbCooldownChannel(ChestCavityInstance cc, OrganState state, long gameTime) {
        long cdUntil = state.getLong(KEY_ABSORB_CD_UNTIL, 0L);
        double value = cdUntil > gameTime ? 1.0D : 0.0D;
        updateChannel(cc, LINK_ABSORB_CD, value);
    }

    private static void updateChannel(ChestCavityInstance cc, ResourceLocation id, double value) {
        LinkageChannel channel = LedgerOps.ensureChannel(cc, id, ZERO_ONE);
        if (channel != null) {
            channel.set(Mth.clamp(value, 0.0D, 1.0D));
        }
    }

    private void applyFirstStrike(Player player, LivingEntity target, ChestCavityInstance cc, ItemStack organ, OrganState state) {
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int tier = Mth.clamp(state.getInt(KEY_TIER, 1), 1, MAX_TIER);
        int amplifier = firstStrikeAmplifier(tier);
        int duration = firstStrikeDurationTicks(tier);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier, false, true, true));

        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel, target.getX(), target.getY(), target.getZ());
        cloud.setRadius(2.0F);
        cloud.setDuration(40);
        cloud.setRadiusPerTick(0.0F);
        cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true, true));
        cloud.setOwner(player);
        serverLevel.addFreshEntity(cloud);

        double bonusDamage = 2.0D + (tier - 1) * 1.0D;
        if (bonusDamage > 0.0D) {
            target.hurt(player.damageSources().playerAttack(player), (float) bonusDamage);
            target.hurtMarked = true;
        }

        long gameTime = serverLevel.getGameTime();
        OrganStateOps.setBoolean(state, cc, organ, KEY_STEALTH_ACTIVE, false, false);
        OrganStateOps.setLong(state, cc, organ, KEY_STEALTH_GRACE_UNTIL, 0L, value -> Math.max(0L, value), 0L);
        OrganStateOps.setLong(state, cc, organ, KEY_COMBAT_FREEZE_UNTIL, gameTime + COMBAT_FREEZE_TICKS, value -> Math.max(0L, value), 0L);
        updateStealthChannel(cc, false);

        long ready = state.getLong(KEY_FIRST_STRIKE_READY, 0L);
        if (gameTime >= ready) {
            int increment = firstStrikeEpValue(target);
            int current = state.getInt(KEY_EP_FIRST_TOTAL, 0);
            OrganStateOps.setInt(state, cc, organ, KEY_EP_FIRST_TOTAL,
                    Math.min(Integer.MAX_VALUE, current + increment), value -> Math.max(0, value), 0);
            OrganStateOps.setLong(state, cc, organ, KEY_FIRST_STRIKE_READY, gameTime + FIRST_STRIKE_EP_COOLDOWN_TICKS,
                    value -> Math.max(0L, value), 0L);
        }

        NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    private static void activateTunnel(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof ServerPlayer player) || cc == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        OrganState state = OrganState.of(organ, STATE_ROOT);
        int tier = Mth.clamp(state.getInt(KEY_TIER, 1), 1, MAX_TIER);
        MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
        MultiCooldown.Entry readyEntry = cooldown.entry(KEY_TUNNEL_READY_AT).withDefault(0L);
        long now = level.getGameTime();
        if (!readyEntry.isReady(now)) {
            return;
        }
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }
        ResourceHandle handle = handleOpt.get();
        OptionalDouble consume = handle.consumeScaledZhenyuan(TUNNEL_BASE_COST);
        if (consume.isEmpty()) {
            player.displayClientMessage(Component.literal("【隐石蛊】真元不足。"), true);
            return;
        }
        Optional<Vec3> targetOpt = findSafeBlinkTarget(player, tunnelRangeForTier(tier));
        if (targetOpt.isEmpty()) {
            handle.replenishScaledZhenyuan(TUNNEL_BASE_COST, true);
            player.displayClientMessage(Component.literal("【隐石蛊】前方无安全落点。"), true);
            return;
        }
        Vec3 from = player.position();
        Vec3 target = targetOpt.get();
        player.teleportTo(target.x, target.y, target.z);
        player.fallDistance = 0.0F;
        spawnTunnelEffects(level, from, target);
        applyTunnelBuff(player, tier);

        int cooldownTicks = tunnelCooldownForTier(tier);
        readyEntry.setReadyAt(now + cooldownTicks);
        scheduleCooldownToast(player, organ, readyEntry.getReadyTick(), "隐石蛊·石潜", "冷却结束");
        OrganStateOps.setLong(state, cc, organ, KEY_TUNNEL_READY_AT, readyEntry.getReadyTick(), value -> Math.max(0L, value), 0L);
        incrementTunnelEp(state, cc, organ, now);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    private static void activateStatue(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof ServerPlayer player) || cc == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        OrganState state = OrganState.of(organ, STATE_ROOT);
        if (!state.getBoolean(KEY_UNLOCK_STATUE, false)) {
            player.displayClientMessage(Component.literal("【隐石蛊】尚未解锁石像化。"), true);
            return;
        }
        int tier = Mth.clamp(state.getInt(KEY_TIER, 1), 1, MAX_TIER);
        if (tier < 5) {
            player.displayClientMessage(Component.literal("【隐石蛊】需要五转方可施展石像化。"), true);
            return;
        }
        MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
        MultiCooldown.Entry readyEntry = cooldown.entry(KEY_STATUE_READY_AT).withDefault(0L);
        long now = level.getGameTime();
        if (!readyEntry.isReady(now)) {
            return;
        }
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty() || handleOpt.get().consumeScaledZhenyuan(STATUE_BASE_COST).isEmpty()) {
            player.displayClientMessage(Component.literal("【隐石蛊】真元不足。"), true);
            return;
        }
        int amplifier = tier >= 5 ? 3 : 2;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, STATUE_DURATION_TICKS, amplifier, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STATUE_DURATION_TICKS, 3, false, true, true));
        applyStatueKnockback(player);
        readyEntry.setReadyAt(now + STATUE_COOLDOWN_TICKS);
        scheduleCooldownToast(player, organ, readyEntry.getReadyTick(), "隐石蛊·石像化", "冷却结束");
        OrganStateOps.setLong(state, cc, organ, KEY_STATUE_READY_AT, readyEntry.getReadyTick(), value -> Math.max(0L, value), 0L);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    private static Optional<Vec3> findSafeBlinkTarget(Player player, double maxRange) {
        if (!(player.level() instanceof ServerLevel level) || maxRange <= 0.0D) {
            return Optional.empty();
        }
        Vec3 direction = player.getLookAngle();
        if (direction.lengthSqr() < 1.0E-6D) {
            return Optional.empty();
        }
        Vec3 step = direction.normalize().scale(0.25D);
        int maxSteps = (int) Math.ceil(maxRange / 0.25D);
        Vec3 current = player.position().add(0.0D, 0.001D, 0.0D);
        for (int i = 0; i < maxSteps; i++) {
            current = current.add(step);
            BlockPos feet = BlockPos.containing(current);
            if (!level.hasChunkAt(feet) || !level.getWorldBorder().isWithinBounds(feet)) {
                break;
            }
            if (!isPassable(level, feet)) {
                break;
            }
            if (isValidLanding(level, feet)) {
                Vec3 result = new Vec3(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
                if (result.distanceToSqr(player.position()) < 0.25D) {
                    continue;
                }
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }

    private static boolean isPassable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean isValidLanding(ServerLevel level, BlockPos feet) {
        BlockState feetState = level.getBlockState(feet);
        BlockPos head = feet.above();
        BlockState headState = level.getBlockState(head);
        if (!feetState.getCollisionShape(level, feet).isEmpty()) {
            return false;
        }
        if (!headState.getCollisionShape(level, head).isEmpty()) {
            return false;
        }
        BlockPos below = feet.below();
        BlockState belowState = level.getBlockState(below);
        return belowState.isFaceSturdy(level, below, Direction.UP);
    }

    private static void spawnTunnelEffects(ServerLevel level, Vec3 from, Vec3 to) {
        level.playSound(null, to.x, to.y, to.z, SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 0.6F,
                0.85F + level.random.nextFloat() * 0.1F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                to.x, to.y, to.z, 18, 0.2D, 0.1D, 0.2D, 0.02D);
        level.sendParticles(TUNNEL_SMOKE_PARTICLE, from.x, from.y + 0.1D, from.z, 8, 0.2D, 0.1D, 0.2D, 0.01D);
    }

    private static void applyTunnelBuff(ServerPlayer player, int tier) {
        double knockback = TUNNEL_KNOCKBACK_BASE + (tier - 1) * TUNNEL_KNOCKBACK_STEP;
        float absorptionBonus = (float) (TUNNEL_ABSORB_BONUS + (tier - 1) * TUNNEL_ABSORB_STEP);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, TUNNEL_BUFF_DURATION_TICKS, 0, false, true, true));
        AbsorptionHelper.applyAbsorption(player,
                Math.max(player.getAbsorptionAmount(), absorptionBonus),
                ABSORPTION_MODIFIER_ID, true);
        applyTemporaryKnockback(player, TUNNEL_KNOCKBACK_ID, knockback, TUNNEL_BUFF_DURATION_TICKS);
    }

    private static void applyStatueKnockback(ServerPlayer player) {
        applyTemporaryKnockback(player, STATUE_KNOCKBACK_ID, STATUE_KNOCKBACK_VALUE, STATUE_DURATION_TICKS);
    }

    private static void applyTemporaryKnockback(ServerPlayer player, ResourceLocation modifierId, double amount, int duration) {
        AttributeInstance attribute = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attribute == null) {
            return;
        }
        AttributeModifier modifier = new AttributeModifier(modifierId, amount, AttributeModifier.Operation.ADD_VALUE);
        AttributeOps.replaceTransient(attribute, modifierId, modifier);
        TickOps.schedule(player.serverLevel(), () -> removeKnockbackModifier(player, modifierId), duration);
    }

    private static void removeKnockbackModifier(LivingEntity entity, ResourceLocation modifierId) {
        AttributeInstance attribute = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        AttributeOps.removeById(attribute, modifierId);
    }

    private static void scheduleCooldownToast(ServerPlayer player, ItemStack organ, long readyTick, String title, String subtitle) {
        if (readyTick <= 0L) {
            return;
        }
        ServerLevel level = player.serverLevel();
        CountdownOps.scheduleToastAt(level, player, readyTick, level.getGameTime(), organ.copyWithCount(1), title, subtitle);
    }

    private static void incrementTunnelEp(OrganState state, ChestCavityInstance cc, ItemStack organ, long now) {
        long windowStart = state.getLong(KEY_TUNNEL_WINDOW_START, 0L);
        int windowCount = state.getInt(KEY_TUNNEL_WINDOW_COUNT, 0);
        if (windowStart <= 0L || now - windowStart >= TUNNEL_WINDOW_TICKS) {
            windowStart = now;
            windowCount = 0;
        }
        if (windowCount < 2) {
            int current = state.getInt(KEY_EP_TUNNEL_TOTAL, 0);
            OrganStateOps.setInt(state, cc, organ, KEY_EP_TUNNEL_TOTAL,
                    Math.min(Integer.MAX_VALUE, current + 1), value -> Math.max(0, value), 0);
            windowCount++;
        }
        OrganStateOps.setLong(state, cc, organ, KEY_TUNNEL_WINDOW_START, windowStart, value -> Math.max(0L, value), 0L);
        OrganStateOps.setInt(state, cc, organ, KEY_TUNNEL_WINDOW_COUNT, windowCount, value -> Math.max(0, value), 0);
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
            ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (Objects.equals(id, ORGAN_ID)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static int tunnelCooldownForTier(int tier) {
        return tier >= 4 ? TUNNEL_COOLDOWN_TIER4_TICKS : TUNNEL_COOLDOWN_BASE_TICKS;
    }

    private static double tunnelRangeForTier(int tier) {
        return TUNNEL_RANGE_BASE + (tier - 1) * TUNNEL_RANGE_STEP;
    }

    private static boolean isPlayerInCombat(Player player) {
        int currentTick = player.tickCount;
        return player.getLastHurtByMobTimestamp() + 100 > currentTick
                || player.getLastHurtMobTimestamp() + 100 > currentTick;
    }

    private static double absorptionMaxForTier(int tier) {
        return ABSORB_MAX_BASE + (tier - 1) * ABSORB_MAX_STEP;
    }

    private static double absorptionConvertRatio(int tier) {
        return ABSORB_CONVERT_RATIO + (tier - 1) * ABSORB_CONVERT_STEP;
    }

    private static double absorptionPerHitCap(int tier) {
        return ABSORB_PER_HIT_CAP + (tier - 1) * ABSORB_PER_HIT_STEP;
    }

    private static double projectileReductionForTier(int tier) {
        return Math.min(0.8D, PROJECTILE_REDUCTION_BASE + (tier - 1) * PROJECTILE_REDUCTION_STEP);
    }

    private static int firstStrikeAmplifier(int tier) {
        return tier >= 4 ? 5 : tier >= 2 ? 4 : 3;
    }

    private static int firstStrikeDurationTicks(int tier) {
        return tier >= 4 ? 30 : tier >= 2 ? 24 : 16;
    }

    private static int firstStrikeEpValue(LivingEntity target) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (target instanceof EnderDragon || target instanceof WitherBoss) {
            return 12;
        }
        if (typeId != null && typeId.equals(WARDEN_ID)) {
            return 12;
        }
        if (target instanceof Ravager || target instanceof ElderGuardian) {
            return 8;
        }
        if (target instanceof Mob mob) {
            if (mob.getMaxHealth() >= 80.0F) {
                return 12;
            }
            if (mob.getMaxHealth() >= 40.0F) {
                return 8;
            }
        }
        return 5;
    }

    private void recordAbsorbProgress(OrganState state, ChestCavityInstance cc, ItemStack organ, double amount) {
        double total = state.getDouble(KEY_EP_ABSORB_TOTAL, 0.0D) + amount;
        int previousFloor = state.getInt(KEY_EP_ABSORB_FLOOR, 0);
        int newFloor = (int) Math.min(Integer.MAX_VALUE, Math.floor(total));
        if (newFloor > previousFloor) {
            OrganStateOps.setInt(state, cc, organ, KEY_EP_ABSORB_FLOOR, newFloor, value -> Math.max(0, value), 0);
        }
        OrganStateOps.setDouble(state, cc, organ, KEY_EP_ABSORB_TOTAL, total, value -> Math.max(0.0D, value), 0.0D);
    }
}
