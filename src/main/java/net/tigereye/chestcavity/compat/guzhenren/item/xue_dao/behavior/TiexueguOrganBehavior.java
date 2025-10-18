package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;


import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import org.joml.Vector3f;
import org.slf4j.Logger;
import net.tigereye.chestcavity.registration.CCItems;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;

import java.util.List;

/**
 * Behaviour implementation for 铁血蛊 (Tie Xue Gu).
 */
public final class TiexueguOrganBehavior extends AbstractGuzhenrenOrganBehavior implements OrganSlowTickListener, OrganRemovalListener, IncreaseEffectContributor {
    public static final TiexueguOrganBehavior INSTANCE = new TiexueguOrganBehavior();

    private TiexueguOrganBehavior() {
    }

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "tiexuegu");
    private static final ResourceLocation XUE_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/xue_dao_increase_effect");

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LOG_PREFIX = "[Tie Xue Gu]";

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final String STATE_KEY = "Tiexuegu";
    private static final String TIMER_KEY = "Timer";
    private static final ResourceLocation READY_AT_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "ready_at/tiexuegu_effect");
    private static final String EFFECT_KEY = "Effect";

    private static final int TRIGGER_INTERVAL_SLOW_TICKS = BehaviorConfigAccess.getInt(TiexueguOrganBehavior.class, "TRIGGER_INTERVAL_SLOW_TICKS", 60);
    private static final float HEALTH_DRAIN_PER_STACK = BehaviorConfigAccess.getFloat(TiexueguOrganBehavior.class, "HEALTH_DRAIN_PER_STACK", 5.0f);
    private static final double BASE_EFFICIENCY_MIN = 0.5;
    private static final double BASE_EFFICIENCY_MAX = 1.0;
    private static final double EFFICIENCY_INCREMENT = 0.1;
    private static final double ZHENYUAN_BASE = 10.0;
    private static final double JINGLI_BASE = 5.0;
    private static final float MINIMUM_HEALTH_RESERVE = BehaviorConfigAccess.getFloat(TiexueguOrganBehavior.class, "MINIMUM_HEALTH_RESERVE", 1.0f);

    private static final int DAMAGE_PARTICLE_COUNT = BehaviorConfigAccess.getInt(TiexueguOrganBehavior.class, "DAMAGE_PARTICLE_COUNT", 24);
    private static final int BLOOD_PARTICLE_COUNT = BehaviorConfigAccess.getInt(TiexueguOrganBehavior.class, "BLOOD_PARTICLE_COUNT", 32);
    private static final DustParticleOptions BLOOD_DUST =
            new DustParticleOptions(new Vector3f(0.8f, 0.05f, 0.05f), 1.0f);

    private static final Component DRAIN_MESSAGE =
            Component.literal("铁血蛊吸食了你的鲜血，化为力量流入体内。");
    private static final Component STARVED_MESSAGE =
            Component.literal("你的血量不足，铁血蛊暂时沉寂。");

    /**
     * Invoked when the organ is evaluated inside a chest cavity to ensure bookkeeping is initialised once.
     */
    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ, CCItems.GUZHENREN_TIE_XUE_GU, ORGAN_ID)) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        IncreaseEffectLedger ledger = context.increaseEffects();
        ledger.registerContributor(organ, this, XUE_DAO_INCREASE_EFFECT);

        RemovalRegistration registration = registerRemovalHook(cc, organ, this, staleRemovalContexts);
        if (registration.alreadyRegistered()) {
            return;
        }

        OrganState state = organState(organ, STATE_KEY);
        double storedEffect = state.getDouble(EFFECT_KEY, 0.0);
        if (storedEffect > 0.0) {
            applyEffectDelta(cc, organ, storedEffect);
        }
        if (state.getInt(TIMER_KEY, 0) <= 0) {
            var change = OrganStateOps.setInt(state, cc, organ, TIMER_KEY, initialTimer(cc), value -> Math.max(0, value), 0);
            logStateChange(LOGGER, LOG_PREFIX, organ, TIMER_KEY, change);
        }
        sendSlotUpdate(cc, organ);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ, CCItems.GUZHENREN_TIE_XUE_GU, ORGAN_ID)) {
            return;
        }

        scheduleEffectIfNeeded(entity, cc, organ);
    }

    private void scheduleEffectIfNeeded(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide() || cc == null || organ == null || organ.isEmpty()) return;
        if (!(entity.level() instanceof ServerLevel server)) return;
        OrganState state = organState(organ, STATE_KEY);
        // Preserve legacy int timer for UI/compat, but drive via readyAt schedule
        int timer = Math.max(0, state.getInt(TIMER_KEY, 0));
        if (timer > 0) {
            // normalise to a readyAt future tick and clear int timer
            long nextAt = server.getGameTime() + timer;
            MultiCooldown mc = MultiCooldown.builder(state).withSync(cc, organ).build();
            MultiCooldown.Entry e = mc.entry(READY_AT_ID.toString());
            e.setReadyAt(nextAt);
            OrganStateOps.setInt(state, cc, organ, TIMER_KEY, 0, v -> Math.max(0, v), 0);
        }
        MultiCooldown mc = MultiCooldown.builder(state).withSync(cc, organ).build();
        MultiCooldown.Entry ready = mc.entry(READY_AT_ID.toString());
        long now = server.getGameTime();
        if (ready.getReadyTick() <= 0L || now >= ready.getReadyTick()) {
            ready.setReadyAt(now + TRIGGER_INTERVAL_SLOW_TICKS);
        }
        ready.onReady(server, now, () -> {
            try {
                triggerEffect(entity, cc, organ, state);
                // re-arm next
                long next = server.getGameTime() + TRIGGER_INTERVAL_SLOW_TICKS;
                MultiCooldown.Entry e2 = mc.entry(READY_AT_ID.toString());
                e2.setReadyAt(next);
                e2.onReady(server, server.getGameTime(), () -> {});
                sendSlotUpdate(cc, organ);
            } catch (Throwable ignored) { }
        });
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        OrganState state = organState(organ, STATE_KEY);
        double storedEffect = state.getDouble(EFFECT_KEY, 0.0);
        if (storedEffect != 0.0 && cc != null) {
            applyEffectDelta(cc, organ, -storedEffect);
        }
        if (cc != null) {
            ActiveLinkageContext context = LinkageManager.getContext(cc);
            IncreaseEffectLedger ledger = context.increaseEffects();
            ledger.remove(organ, XUE_DAO_INCREASE_EFFECT);
            ledger.unregisterContributor(organ);
            ledger.verifyAndRebuildIfNeeded();
        }
        var effectChange = OrganStateOps.setDouble(state, cc, organ, EFFECT_KEY, 0.0, value -> Math.max(0.0, value), 0.0);
        var timerChange = OrganStateOps.setInt(state, cc, organ, TIMER_KEY, 0, value -> Math.max(0, value), 0);
        logStateChange(LOGGER, LOG_PREFIX, organ, EFFECT_KEY, effectChange);
        logStateChange(LOGGER, LOG_PREFIX, organ, TIMER_KEY, timerChange);
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
        int stackCount = Math.max(1, organ.getCount());
        OrganState state = organState(organ, STATE_KEY);
        double effect = state.getDouble(EFFECT_KEY, 0.0);
        registrar.record(XUE_DAO_INCREASE_EFFECT, stackCount, effect);
    }

    /** Ensures the linkage channel exists for this cavity. */
    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ensureIncreaseChannel(LinkageManager.getContext(cc));
    }

    private void triggerEffect(LivingEntity entity, ChestCavityInstance cc, ItemStack organ, OrganState state) {
        int stackCount = Math.max(1, organ.getCount());
        float drainAmount = HEALTH_DRAIN_PER_STACK * stackCount;
        if (!applyHealthDrain(entity, drainAmount)) {
            handleFailedDrain(entity, cc, organ, state, stackCount);
            return;
        }
        if (entity.isDeadOrDying()) {
            return;
        }

        double previousEffect = state.getDouble(EFFECT_KEY, 0.0);
        double newEffect = computeEfficiencyBonus(previousEffect, stackCount);
        double delta = newEffect - previousEffect;
        if (delta != 0.0) {
            applyEffectDelta(cc, organ, delta);
        }
        var effectChange = OrganStateOps.setDouble(state, cc, organ, EFFECT_KEY, newEffect, value -> Math.max(0.0, value), 0.0);
        logStateChange(LOGGER, LOG_PREFIX, organ, EFFECT_KEY, effectChange);

        ActiveLinkageContext context = LinkageManager.getContext(cc);
        LinkageChannel channel = ensureIncreaseChannel(context);
        double efficiency = 1.0 + Math.max(0.0, channel.get());

        if (entity instanceof Player player) {
            applyResourceRecovery(player, efficiency);
        }
        playTriggerCues(entity);
        if (entity instanceof Player player && !player.level().isClientSide()) {
            player.sendSystemMessage(DRAIN_MESSAGE);
        }
        // 吸血成功后给予短暂“血祭窗”，强化部分血道反应
        net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.add(
                entity,
                net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys.BLOOD_RITUAL,
                60);
    }

    private static boolean applyHealthDrain(LivingEntity player, float amount) {
        return GuzhenrenResourceCostHelper.drainHealth(
                player,
                amount,
                MINIMUM_HEALTH_RESERVE,
                player == null ? null : player.damageSources().generic()
        );
    }

    private static void applyResourceRecovery(Player player, double efficiencyMultiplier) {
        if (player == null || efficiencyMultiplier <= 0.0) {
            return;
        }
        double zhenyuanGain = ZHENYUAN_BASE * efficiencyMultiplier;
        double jingliGain = JINGLI_BASE * efficiencyMultiplier;
        ResourceOps.replenishScaledZhenyuan(player, zhenyuanGain);
        ResourceOps.adjustJingli(player, jingliGain);
    }

    private static void playTriggerCues(LivingEntity entity) {
        Level level = entity.level();
        RandomSource random = level.getRandom();
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.9f, 0.8f + random.nextFloat() * 0.2f);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.6f, 0.9f + random.nextFloat() * 0.2f);
        if (level instanceof ServerLevel server) {
            spawnTriggerParticles(server, entity);
        }
    }

    private static void spawnTriggerParticles(ServerLevel server, LivingEntity entity) {
        Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
        server.sendParticles(ParticleTypes.DAMAGE_INDICATOR, center.x, center.y, center.z,
                DAMAGE_PARTICLE_COUNT, 0.35, 0.45, 0.35, 0.25);
        server.sendParticles(BLOOD_DUST, center.x, center.y, center.z,
                BLOOD_PARTICLE_COUNT, 0.4, 0.55, 0.4, 0.0);
    }

    private static double computeEfficiencyBonus(double previousEffect, int stackCount) {
        double baseline = baselineEffect(stackCount);
        double raised = Math.max(previousEffect, baseline);
        double increased = raised + EFFICIENCY_INCREMENT;
        return Math.min(BASE_EFFICIENCY_MAX, increased);
    }

    private static double computeDecayTarget(double previousEffect, int stackCount) {
        double baseline = baselineEffect(stackCount);
        if (previousEffect <= baseline) {
            return previousEffect;
        }
        double decayed = Math.max(baseline, previousEffect - EFFICIENCY_INCREMENT);
        return Math.min(BASE_EFFICIENCY_MAX, decayed);
    }

    private static double baselineEffect(int stackCount) {
        int stacks = Math.max(1, stackCount);
        double baseline = BASE_EFFICIENCY_MIN + (stacks - 1) * EFFICIENCY_INCREMENT;
        return Math.min(BASE_EFFICIENCY_MAX, baseline);
    }

    private LinkageChannel ensureIncreaseChannel(ActiveLinkageContext context) {
        return LedgerOps.ensureChannel(context, XUE_DAO_INCREASE_EFFECT, NON_NEGATIVE);
    }

    private void applyEffectDelta(ChestCavityInstance cc, ItemStack organ, double delta) {
        if (cc == null || delta == 0.0) {
            return;
        }
        LedgerOps.adjust(cc, organ, XUE_DAO_INCREASE_EFFECT, delta, NON_NEGATIVE, true);
    }

    private static int initialTimer(ChestCavityInstance cc) {
        if (cc != null && cc.owner != null) {
            RandomSource random = cc.owner.getRandom();
            if (TRIGGER_INTERVAL_SLOW_TICKS > 0) {
                return 1 + random.nextInt(TRIGGER_INTERVAL_SLOW_TICKS);
            }
        }
        return TRIGGER_INTERVAL_SLOW_TICKS;
    }

    private void handleFailedDrain(LivingEntity player, ChestCavityInstance cc, ItemStack organ, OrganState state, int stackCount) {
        double previousEffect = state.getDouble(EFFECT_KEY, 0.0);
        double targetEffect = computeDecayTarget(previousEffect, stackCount);
        double delta = targetEffect - previousEffect;
        if (delta != 0.0) {
            applyEffectDelta(cc, organ, delta);
        }
        var effectChange = OrganStateOps.setDouble(state, cc, organ, EFFECT_KEY, targetEffect, value -> Math.max(0.0, value), 0.0);
        logStateChange(LOGGER, LOG_PREFIX, organ, EFFECT_KEY, effectChange);
        if (player instanceof Player p && !p.level().isClientSide()) {
            p.sendSystemMessage(STARVED_MESSAGE);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} Drain failed for {} (stackCount={}, available={}, required={})",
                    LOG_PREFIX,
                    player.getName().getString(),
                    stackCount,
                    player.getHealth() + player.getAbsorptionAmount(),
                    HEALTH_DRAIN_PER_STACK * stackCount);
        }
    }
}
