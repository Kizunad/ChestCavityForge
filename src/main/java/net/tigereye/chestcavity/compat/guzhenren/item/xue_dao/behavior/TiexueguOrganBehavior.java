package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior;

import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import net.tigereye.chestcavity.util.NBTWriter;
import net.tigereye.chestcavity.util.NetworkUtil;
import org.joml.Vector3f;
import org.slf4j.Logger;
import net.tigereye.chestcavity.registration.CCItems;

import java.util.List;
import java.util.Objects;

/**
 * Behaviour implementation for 铁血蛊 (Tie Xue Gu).
 */
public enum TiexueguOrganBehavior implements OrganSlowTickListener, OrganRemovalListener, IncreaseEffectContributor {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "tiexuegu");
    private static final ResourceLocation XUE_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/xue_dao_increase_effect");

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final String STATE_KEY = "Tiexuegu";
    private static final String TIMER_KEY = "Timer";
    private static final String EFFECT_KEY = "Effect";

    private static final int TRIGGER_INTERVAL_SLOW_TICKS = 60;
    private static final float HEALTH_DRAIN_PER_STACK = 5.0f;
    private static final double BASE_EFFICIENCY_MIN = 0.5;
    private static final double BASE_EFFICIENCY_MAX = 1.0;
    private static final double EFFICIENCY_INCREMENT = 0.1;
    private static final double ZHENYUAN_BASE = 10.0;
    private static final double JINGLI_BASE = 5.0;
    private static final float MINIMUM_HEALTH_RESERVE = 1.0f;

    private static final int DAMAGE_PARTICLE_COUNT = 24;
    private static final int BLOOD_PARTICLE_COUNT = 32;
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
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        IncreaseEffectLedger ledger = context.increaseEffects();
        ledger.registerContributor(organ, this, XUE_DAO_INCREASE_EFFECT);

        int slotIndex = ChestCavityUtil.findOrganSlot(cc, organ);
        boolean alreadyRegistered = staleRemovalContexts.removeIf(old ->
                ChestCavityUtil.matchesRemovalContext(old, slotIndex, organ, this));
        cc.onRemovedListeners.add(new OrganRemovalContext(slotIndex, organ, this));
        if (alreadyRegistered) {
            return;
        }

        double storedEffect = readEffect(organ);
        if (storedEffect > 0.0) {
            applyEffectDelta(cc, organ, storedEffect);
        }
        if (readTimer(organ) <= 0) {
            int initial = initialTimer(cc);
            writeTimer(organ, initial);
        }
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!organ.is(CCItems.GUZHENREN_TIE_XUE_GU)) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(organ.getItem());
            if (!ORGAN_ID.equals(id)) {
                return;
            }
        }

        int timer = Math.max(0, readTimer(organ));
        if (timer > 0) {
            writeTimer(organ, timer - 1);
            return;
        }

        triggerEffect(entity, cc, organ);
        writeTimer(organ, TRIGGER_INTERVAL_SLOW_TICKS);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        double storedEffect = readEffect(organ);
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
        writeEffect(organ, 0.0);
        writeTimer(organ, 0);
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
        double effect = readEffect(organ);
        registrar.record(XUE_DAO_INCREASE_EFFECT, stackCount, effect);
    }

    /** Ensures the linkage channel exists for this cavity. */
    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ensureChannel(LinkageManager.getContext(cc));
    }

    private void triggerEffect(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        int stackCount = Math.max(1, organ.getCount());
        float drainAmount = HEALTH_DRAIN_PER_STACK * stackCount;
        if (!applyHealthDrain(entity, drainAmount)) {
            handleFailedDrain(entity, cc, organ, stackCount);
            return;
        }
        if (entity.isDeadOrDying()) {
            return;
        }

        double previousEffect = readEffect(organ);
        double newEffect = computeEfficiencyBonus(previousEffect, stackCount);
        double delta = newEffect - previousEffect;
        if (delta != 0.0) {
            applyEffectDelta(cc, organ, delta);
        }
        writeEffect(organ, newEffect);

        ActiveLinkageContext context = LinkageManager.getContext(cc);
        LinkageChannel channel = ensureChannel(context);
        double efficiency = 1.0 + Math.max(0.0, channel.get());

        if (entity instanceof Player player) {
            applyResourceRecovery(player, efficiency);
        }
        playTriggerCues(entity);
        if (entity instanceof Player player && !player.level().isClientSide()) {
            player.sendSystemMessage(DRAIN_MESSAGE);
        }
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
        GuzhenrenResourceCostHelper.withHandle(player, handle -> {
            handle.replenishScaledZhenyuan(zhenyuanGain, true);
            handle.adjustJingli(jingliGain, true);
        });
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

    private static LinkageChannel ensureChannel(ActiveLinkageContext context) {
        return context.getOrCreateChannel(XUE_DAO_INCREASE_EFFECT).addPolicy(NON_NEGATIVE);
    }

    private static void applyEffectDelta(ChestCavityInstance cc, ItemStack organ, double delta) {
        if (cc == null || delta == 0.0) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        LinkageChannel channel = ensureChannel(context);
        channel.adjust(delta);
        context.increaseEffects().adjust(organ, XUE_DAO_INCREASE_EFFECT, delta);
    }

    private static int readTimer(ItemStack stack) {
        CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0;
        }
        CompoundTag root = data.copyTag();
        if (!root.contains(STATE_KEY, Tag.TAG_COMPOUND)) {
            return 0;
        }
        CompoundTag state = root.getCompound(STATE_KEY);
        return state.getInt(TIMER_KEY);
    }

    private static void writeTimer(ItemStack stack, int value) {
        int clamped = Math.max(0, value);
        int previous = readTimer(stack);
        NBTWriter.updateCustomData(stack, tag -> {
            CompoundTag state = tag.contains(STATE_KEY, Tag.TAG_COMPOUND) ? tag.getCompound(STATE_KEY) : new CompoundTag();
            state.putInt(TIMER_KEY, clamped);
            tag.put(STATE_KEY, state);
        });
        logNbtChange(stack, TIMER_KEY, previous, clamped);
    }

    private static double readEffect(ItemStack stack) {
        CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0.0;
        }
        CompoundTag root = data.copyTag();
        if (!root.contains(STATE_KEY, Tag.TAG_COMPOUND)) {
            return 0.0;
        }
        CompoundTag state = root.getCompound(STATE_KEY);
        if (!state.contains(EFFECT_KEY, Tag.TAG_DOUBLE)) {
            return 0.0;
        }
        return state.getDouble(EFFECT_KEY);
    }

    private static void writeEffect(ItemStack stack, double value) {
        double clamped = Math.max(0.0, value);
        double previous = readEffect(stack);
        NBTWriter.updateCustomData(stack, tag -> {
            CompoundTag state = tag.contains(STATE_KEY, Tag.TAG_COMPOUND) ? tag.getCompound(STATE_KEY) : new CompoundTag();
            state.putDouble(EFFECT_KEY, clamped);
            tag.put(STATE_KEY, state);
        });
        logNbtChange(stack, EFFECT_KEY, previous, clamped);
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

    private void handleFailedDrain(LivingEntity player, ChestCavityInstance cc, ItemStack organ, int stackCount) {
        double previousEffect = readEffect(organ);
        double targetEffect = computeDecayTarget(previousEffect, stackCount);
        double delta = targetEffect - previousEffect;
        if (delta != 0.0) {
            applyEffectDelta(cc, organ, delta);
        }
        writeEffect(organ, targetEffect);
        if (player instanceof Player p && !p.level().isClientSide()) {
            p.sendSystemMessage(STARVED_MESSAGE);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[Tie Xue Gu] Drain failed for {} (stackCount={}, available={}, required={})",
                    player.getName().getString(),
                    stackCount,
                    player.getHealth() + player.getAbsorptionAmount(),
                    HEALTH_DRAIN_PER_STACK * stackCount);
        }
    }

    private static void logNbtChange(ItemStack stack, String key, Object oldValue, Object newValue) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[Tie Xue Gu] Updated {} for {} from {} to {}", key, describeStack(stack), oldValue, newValue);
        }
    }

    private static String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<empty>";
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return stack.getCount() + "x " + id;
    }
}
