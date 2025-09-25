package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior;

import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.GuzhenrenItems;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import net.tigereye.chestcavity.util.NBTWriter;
import net.tigereye.chestcavity.util.NetworkUtil;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Behaviour implementation for 血眼蛊 (Xie Yan Gu).
 */
public enum XieyanguOrganBehavior implements OrganSlowTickListener, OrganOnHitListener, OrganRemovalListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xie_yan_gu");
    private static final ResourceLocation XUE_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/xue_dao_increase_effect");

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String LOG_PREFIX = "[compat/guzhenren][xue_dao]";

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final String STATE_KEY = "Xieyangu";
    private static final String TIMER_KEY = "Timer";

    private static final int HEALTH_DRAIN_INTERVAL_SLOW_TICKS = 60; // once per minute
    private static final float BASE_CRIT_MULTIPLIER = 1.5f;
    private static final int BLOOD_TRAIL_DURATION_TICKS = 200; // 10 seconds
    private static final float FOCUS_THRESHOLD_RATIO = 0.3f;
    private static final float FOCUS_ATTACK_SPEED_BONUS = 0.2f;
    private static final float FOCUS_EXHAUSTION_PER_TICK = 2.5f; // applied once per slow tick when active

    private static final ResourceLocation FOCUS_ATTACK_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/xieyangu_focus_attack_speed");
    private static final AttributeModifier FOCUS_ATTACK_SPEED_MODIFIER = new AttributeModifier(
            FOCUS_ATTACK_SPEED_ID,
            FOCUS_ATTACK_SPEED_BONUS,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
    );

    private static final DustParticleOptions BLOOD_SPARK =
            new DustParticleOptions(new Vector3f(0.78f, 0.08f, 0.12f), 1.0f);
    private static final DustParticleOptions BLOOD_MIST =
            new DustParticleOptions(new Vector3f(0.65f, 0.05f, 0.08f), 1.35f);

    private static final Set<UUID> ACTIVE_FOCUS = new HashSet<>();

    private XieyanguOrganBehavior() {
    }

    /** Initialises bookkeeping for this organ when inserted. */
    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        Objects.requireNonNull(staleRemovalContexts, "staleRemovalContexts");

        int slotIndex = ChestCavityUtil.findOrganSlot(cc, organ);
        boolean alreadyRegistered = staleRemovalContexts.removeIf(old ->
                ChestCavityUtil.matchesRemovalContext(old, slotIndex, organ, this));
        cc.onRemovedListeners.add(new OrganRemovalContext(slotIndex, organ, this));
        if (alreadyRegistered) {
            return;
        }

        if (readTimer(organ) <= 0) {
            int initial = HEALTH_DRAIN_INTERVAL_SLOW_TICKS;
            if (cc.owner != null) {
                RandomSource random = cc.owner.getRandom();
                initial = 1 + random.nextInt(Math.max(1, HEALTH_DRAIN_INTERVAL_SLOW_TICKS));
            }
            writeTimer(organ, initial);
        }
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        String ownerName = entity == null ? "<null>" : entity.getName().getString();
        ResourceLocation organId = organ == null || organ.isEmpty()
                ? null
                : BuiltInRegistries.ITEM.getKey(organ.getItem());

        if (entity == null) {
            LOGGER.info("{} [slow-tick] EXIT owner=<null> reason=entity_null", LOG_PREFIX);
            return;
        }

        LOGGER.info(
                "{} [slow-tick] ENTER owner={} type={} side={} organ={} cc_opened={}",
                LOG_PREFIX,
                ownerName,
                entity.getType().builtInRegistryHolder().key().location(),
                entity.level().isClientSide() ? "client" : "server",
                organId,
                cc != null && cc.opened
        );

        if (entity.level().isClientSide()) {
            LOGGER.info("{} [slow-tick] EXIT owner={} reason=client_side", LOG_PREFIX, ownerName);
            return;
        }
        if (cc == null) {
            LOGGER.info("{} [slow-tick] EXIT owner={} reason=chest_cavity_null", LOG_PREFIX, ownerName);
            return;
        }
        if (!cc.opened) {
            LOGGER.info("{} [slow-tick] EXIT owner={} reason=chest_cavity_closed", LOG_PREFIX, ownerName);
            return;
        }
        if (organ == null || organ.isEmpty()) {
            LOGGER.info("{} [slow-tick] EXIT owner={} reason=organ_empty", LOG_PREFIX, ownerName);
            return;
        }
        if (!isTargetOrgan(organ)) {
            LOGGER.info("{} [slow-tick] EXIT owner={} reason=not_xie_yan_gu organ={}", LOG_PREFIX, ownerName, organId);
            return;
        }

        if (!(entity instanceof Player player)) {
            LOGGER.info("{} [slow-tick] EXIT owner={} reason=non_player_skip", LOG_PREFIX, ownerName);
            return;
        }

        decrementAndTriggerDrain(player, cc, organ);
        updateFocusState(player, cc);
        LOGGER.info("{} [slow-tick] EXIT owner={} reason=success", LOG_PREFIX, ownerName);
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
        if (!(attacker instanceof Player player)) {
            return handleNonPlayerAttack(source, attacker, target, cc, organ, damage);
        }
        if (attacker.level().isClientSide()) {
            return damage;
        }
        if (target == null || cc == null || organ == null || organ.isEmpty()) {
            return damage;
        }
        if (!isTargetOrgan(organ)) {
            return damage;
        }

        Entity direct = source == null ? null : source.getDirectEntity();
        if (direct != attacker) {
            // Only convert direct melee hits into crits.
            return damage;
        }

        float scaled = damage * BASE_CRIT_MULTIPLIER;
        playCriticalCues(player, target);
        XieyanguTrailHandler.applyTrail(target, BLOOD_TRAIL_DURATION_TICKS);
        return scaled;
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player)) {
            return;
        }
        removeFocusModifier(player);
        ACTIVE_FOCUS.remove(player.getUUID());
    }

    /** Ensures the linkage channel exists for the owning chest cavity. */
    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ensureChannel(GuzhenrenLinkageManager.getContext(cc));
    }

    private void decrementAndTriggerDrain(Player player, ChestCavityInstance cc, ItemStack organ) {
        int timer = Math.max(0, readTimer(organ));
        if (timer > 0) {
            writeTimer(organ, timer - 1);
        } else {
            triggerHealthDrain(player, cc);
            writeTimer(organ, HEALTH_DRAIN_INTERVAL_SLOW_TICKS);
        }
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    private float handleNonPlayerAttack(
            DamageSource source,
            LivingEntity attacker,
            LivingEntity target,
            ChestCavityInstance cc,
            ItemStack organ,
            float damage
    ) {
        // Entry log with maximal context
        final String attackerName = attacker == null ? "<null>" : attacker.getName().getString();
        final String targetName = target == null ? "<null>" : target.getName().getString();
        final ResourceLocation organId = (organ == null || organ.isEmpty()) ? null : BuiltInRegistries.ITEM.getKey(organ.getItem());
        final String srcMsg = source == null ? "<null>" : source.type().msgId();
        final Entity direct = source == null ? null : source.getDirectEntity();
        LOGGER.info("{} [placeholder] onHit(non-player) ENTER attacker={}, target={}, organ={}, src={}, direct={} damage={}",
                LOG_PREFIX, attackerName, targetName, organId, srcMsg, direct == null ? "<null>" : direct.getName().getString(), damage);

        // Early outs with reasons
        if (attacker == null) {
            LOGGER.info("{} EXIT reason=attacker_null return={} ", LOG_PREFIX, damage);
            return damage;
        }
        if (attacker.level().isClientSide()) {
            LOGGER.info("{} EXIT reason=client_side return={}", LOG_PREFIX, damage);
            return damage;
        }
        if (target == null) {
            LOGGER.info("{} EXIT reason=target_null return={}", LOG_PREFIX, damage);
            return damage;
        }
        if (cc == null) {
            LOGGER.info("{} EXIT reason=chest_cavity_null return={}", LOG_PREFIX, damage);
            return damage;
        }
        if (!cc.opened) {
            LOGGER.info("{} EXIT reason=chest_cavity_closed owner={} return={}", LOG_PREFIX,
                    cc.owner == null ? "<null>" : cc.owner.getName().getString(), damage);
            return damage;
        }
        if (organ == null || organ.isEmpty()) {
            LOGGER.info("{} EXIT reason=organ_empty return={}", LOG_PREFIX, damage);
            return damage;
        }
        if (!isTargetOrgan(organ)) {
            LOGGER.info("{} EXIT reason=not_xie_yan_gu organ={} return={}", LOG_PREFIX, organId, damage);
            return damage;
        }

        // For visibility: note the direct vs attacker for future melee-only checks
        if (direct != attacker) {
            LOGGER.info("{} note=indirect_damage direct!=attacker (keeping damage unchanged)", LOG_PREFIX);
        }

        // Placeholder end: no amplification yet
        LOGGER.info("{} EXIT reason=placeholder_noop return={} ", LOG_PREFIX, damage);
        return damage;
    }

    private void triggerHealthDrain(Player player, ChestCavityInstance cc) {
        if (player == null || cc == null) {
            return;
        }
        double increase = 1.0 + Math.max(0.0, ensureChannel(GuzhenrenLinkageManager.getContext(cc)).get());
        float damage = (float) (1.0 / Math.max(increase, 1.0));
        if (damage <= 0.0f) {
            return;
        }

        float startingHealth = player.getHealth();
        float startingAbsorption = player.getAbsorptionAmount();

        player.invulnerableTime = 0;
        player.hurt(player.damageSources().generic(), damage);
        player.invulnerableTime = 0;

        if (!player.isDeadOrDying()) {
            float targetAbsorption = Math.max(0.0f, startingAbsorption - damage);
            player.setAbsorptionAmount(targetAbsorption);
            if (player.getHealth() > startingHealth - damage) {
                player.setHealth(Math.max(0.0f, startingHealth - damage));
            }
            player.hurtTime = 0;
            player.hurtDuration = 0;
        }

        playDrainCues(player.level(), player);
    }

    private void updateFocusState(Player player, ChestCavityInstance cc) {
        double maxHealth = player.getMaxHealth();
        double threshold = maxHealth * FOCUS_THRESHOLD_RATIO;
        boolean active = player.getHealth() <= threshold && player.getHealth() > 0.0f;

        AttributeInstance attribute = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attribute != null) {
            boolean present = attribute.hasModifier(FOCUS_ATTACK_SPEED_ID);
            if (active && !present) {
                attribute.addTransientModifier(FOCUS_ATTACK_SPEED_MODIFIER);
            } else if (!active && present) {
                attribute.removeModifier(FOCUS_ATTACK_SPEED_ID);
            }
        }

        UUID id = player.getUUID();
        boolean tracking = ACTIVE_FOCUS.contains(id);
        if (active) {
            applyFocusMaintenance(player);
            if (!tracking) {
                ACTIVE_FOCUS.add(id);
                playFocusCues(player.level(), player);
            }
        } else if (tracking) {
            ACTIVE_FOCUS.remove(id);
        }
    }

    private static void applyFocusMaintenance(Player player) {
        FoodData food = player.getFoodData();
        food.addExhaustion(FOCUS_EXHAUSTION_PER_TICK);
        if (player.level() instanceof ServerLevel server) {
            Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
            server.sendParticles(
                    BLOOD_MIST,
                    center.x,
                    center.y,
                    center.z,
                    12,
                    0.35,
                    0.4,
                    0.35,
                    0.02
            );
        }
    }

    private static void playCriticalCues(Player player, LivingEntity target) {
        Level level = player.level();
        RandomSource random = level.getRandom();
        double x = target.getX();
        double y = target.getY(0.5);
        double z = target.getZ();

        level.playSound(null, x, y, z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.8f, 0.8f + random.nextFloat() * 0.2f);
        level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.6f, 0.9f + random.nextFloat() * 0.2f);

        if (level instanceof ServerLevel server) {
            Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
            server.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 8, 0.25, 0.3, 0.25, 0.05);
            server.sendParticles(BLOOD_SPARK, center.x, center.y, center.z, 16, 0.35, 0.4, 0.35, 0.02);
        }

        level.playSound(null, x, y, z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.15f, 1.5f + random.nextFloat() * 0.1f);
    }

    private static void playDrainCues(Level level, Player player) {
        if (level == null) {
            return;
        }
        RandomSource random = level.getRandom();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.6f, 0.7f + random.nextFloat() * 0.2f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.4f, 0.5f + random.nextFloat() * 0.2f);
        if (level instanceof ServerLevel server) {
            Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
            server.sendParticles(BLOOD_SPARK, center.x, center.y, center.z, 20, 0.45, 0.5, 0.45, 0.03);
        }
    }

    private static void playFocusCues(Level level, Player player) {
        if (level == null) {
            return;
        }
        RandomSource random = level.getRandom();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0f, 0.65f + random.nextFloat() * 0.15f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5f, 0.6f + random.nextFloat() * 0.2f);
    }

    private static void removeFocusModifier(Player player) {
        AttributeInstance attribute = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attribute != null && attribute.hasModifier(FOCUS_ATTACK_SPEED_ID)) {
            attribute.removeModifier(FOCUS_ATTACK_SPEED_ID);
        }
    }

    private static boolean isTargetOrgan(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        if (item == GuzhenrenItems.XIE_YAN_GU) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return ORGAN_ID.equals(id);
    }

    private static LinkageChannel ensureChannel(ActiveLinkageContext context) {
        LinkageChannel channel = context.getOrCreateChannel(XUE_DAO_INCREASE_EFFECT);
        channel.addPolicy(NON_NEGATIVE);
        return channel;
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
            CompoundTag state = tag.contains(STATE_KEY, Tag.TAG_COMPOUND) ?
                    tag.getCompound(STATE_KEY) : new CompoundTag();
            state.putInt(TIMER_KEY, clamped);
            tag.put(STATE_KEY, state);
        });
        logNbtChange(stack, TIMER_KEY, previous, clamped);
    }

    private static void logNbtChange(ItemStack stack, String key, Object oldValue, Object newValue) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[Xie Yan Gu] Updated {} for {} from {} to {}", key, describeStack(stack), oldValue, newValue);
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
