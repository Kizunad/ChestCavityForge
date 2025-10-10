package net.tigereye.chestcavity.compat.guzhenren.item.tu_dao.behavior;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.AbsorptionHelper;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Behaviour implementation for 土墙蛊 (Tu Qiang Gu).
 *
 * <p>The behaviour is intentionally incremental – it focuses on delivering the
 * defensive loop (absorption reservoir + hunger upkeep) and the 玉皮蛊反伤联动 so
 * the surrounding infrastructure can build on it later. Active ability support
 * and world interactions (placing temporary walls) will be layered on top in a
 * follow-up patch once the groundwork is proven stable.</p>
 */
public final class TuQiangGuOrganBehavior extends AbstractGuzhenrenOrganBehavior implements OrganSlowTickListener, OrganIncomingDamageListener, OrganRemovalListener {
    public static final TuQiangGuOrganBehavior INSTANCE = new TuQiangGuOrganBehavior();

    private TuQiangGuOrganBehavior() {
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String MOD_ID = "guzhenren";
    public static final ResourceLocation ABILITY_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "tu_qiang_gu");

    private static final String STATE_ROOT = "TuQiangGu";
    private static final String RESERVE_KEY = "Reserve";
    private static final String HUNGER_PROGRESS_KEY = "HungerProgress";
    private static final String BARRIER_TICKS_KEY = "BarrierTicks";
    private static final String BARRIER_REMAINING_KEY = "BarrierRemaining";
    private static final String EMERALD_BLOCKS_KEY = "EmeraldBlocks";
    private static final String JADE_PRISON_UNLOCKED_KEY = "JadePrisonUnlocked";

    private static final double MAX_RESERVE = 25.0;
    private static final double REGEN_PER_SECOND = MAX_RESERVE / 60.0; // 25 absorption per minute
    private static final double HUNGER_COST_PER_SECOND = 10.0 / 60.0; // hunger points per second
    private static final double REFLECT_BASE = 0.05; // 5%反伤
    private static final double REFLECT_JADE_PRISON_BONUS = 0.03; // +3% when fully upgraded
    private static final double BARRIER_DAMAGE_RATIO = 0.10; // 10% local hardening while barrier active
    private static final double BARRIER_MAX_ABSORPTION = 10.0; // 最大吸收10点

    private static final int ABILITY_RANGE = 10;
    private static final int PRISON_RADIUS = 4;
    private static final int PRISON_DURATION_TICKS = 20 * 60; // 60 seconds

    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "tu_qiang_gu");
    private static final ResourceLocation YU_PI_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "yu_pi_gu");
    private static final ResourceLocation TU_LAO_BLOCK_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "tu_lao");
    private static final ResourceLocation JADE_LAO_BLOCK_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "yu_lao");
    private static final ResourceLocation TU_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/tu_dao_increase_effect");
    private static final ResourceLocation HE_SHI_BI_LINK =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/he_shi_bi");
    private static final ResourceLocation ABSORPTION_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/tu_qiang_gu_absorption");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final BlockState EARTH_PRISON_BLOCK = resolvePrisonBlock(TU_LAO_BLOCK_ID, Blocks.PACKED_MUD.defaultBlockState());
    private static final BlockState JADE_PRISON_BLOCK = resolvePrisonBlock(JADE_LAO_BLOCK_ID, Blocks.EMERALD_BLOCK.defaultBlockState());

    private static final double RANGE_SQUARED = ABILITY_RANGE * ABILITY_RANGE;

    private static final ConcurrentHashMap<UUID, ActivePrison> ACTIVE_PRISONS = new ConcurrentHashMap<>();
    private static final AtomicLong PRISON_SEQUENCE = new AtomicLong();

    static {
        OrganActivationListeners.register(ABILITY_ID, TuQiangGuOrganBehavior::activateAbility);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide() || organ == null || organ.isEmpty()) {
            return;
        }
        if (cc == null || cc.inventory == null || !entity.isAlive()) {
            return;
        }

        OrganState state = organState(organ, STATE_ROOT);

        double reserve = Mth.clamp(state.getDouble(RESERVE_KEY, MAX_RESERVE), 0.0, MAX_RESERVE);
        double hungerProgress = Math.max(0.0, state.getDouble(HUNGER_PROGRESS_KEY, 0.0));
        int barrierTicks = Math.max(0, state.getInt(BARRIER_TICKS_KEY, 0));
        double barrierRemaining = Mth.clamp(state.getDouble(BARRIER_REMAINING_KEY, 0.0), 0.0, BARRIER_MAX_ABSORPTION);

        boolean slotUpdate = false;

        // Regenerate the absorption reserve towards the 25-point cap.
        if (reserve < MAX_RESERVE) {
            double updated = Math.min(MAX_RESERVE, reserve + REGEN_PER_SECOND);
            if (!Mth.equal(reserve, updated)) {
                reserve = updated;
                OrganState.Change<Double> change = state.setDouble(RESERVE_KEY, reserve);
                logStateChange(LOGGER, prefix(), organ, RESERVE_KEY, change);
                slotUpdate |= change.changed();
            }
        }

        // Apply the upkeep cost: remove roughly 10 hunger points every minute from players.
        if (entity instanceof Player player) {
            hungerProgress += HUNGER_COST_PER_SECOND;
            int hungerDrain = (int) hungerProgress;
            if (hungerDrain > 0) {
                hungerProgress -= hungerDrain;
                applyHungerDrain(player, hungerDrain);
            }
            OrganState.Change<Double> hungerChange = state.setDouble(HUNGER_PROGRESS_KEY, hungerProgress);
            slotUpdate |= hungerChange.changed();
        }

        if (barrierTicks > 0) {
            int updatedTicks = Math.max(0, barrierTicks - 20); // slow tick executes once per second
            if (updatedTicks != barrierTicks) {
                OrganState.Change<Integer> change = state.setInt(BARRIER_TICKS_KEY, updatedTicks);
                slotUpdate |= change.changed();
                barrierTicks = updatedTicks;
            }
            if (barrierTicks <= 0 && barrierRemaining > 0.0) {
                OrganState.Change<Double> change = state.setDouble(BARRIER_REMAINING_KEY, 0.0);
                slotUpdate |= change.changed();
            }
        }

        // Sync the player's absorption value with the stored reserve when possible.
        if (reserve > 0.0) {
            float currentAbsorption = entity.getAbsorptionAmount();
            if (currentAbsorption + 0.05f < reserve) {
                AbsorptionHelper.applyAbsorption(entity, reserve, ABSORPTION_MODIFIER_ID, true);
            }
        } else {
            AbsorptionHelper.clearAbsorptionCapacity(entity, ABSORPTION_MODIFIER_ID);
        }

        if (slotUpdate) {
            sendSlotUpdate(cc, organ);
        }
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        AbsorptionHelper.clearAbsorptionCapacity(entity, ABSORPTION_MODIFIER_ID);
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (victim == null || victim.level().isClientSide() || organ == null || organ.isEmpty()) {
            return damage;
        }
        if (damage <= 0.0f) {
            return damage;
        }

        OrganState state = organState(organ, STATE_ROOT);
        double reserve = Mth.clamp(state.getDouble(RESERVE_KEY, MAX_RESERVE), 0.0, MAX_RESERVE);
        double updatedReserve = reserve;
        boolean slotUpdate = false;

        float absorptionBefore = Math.max(0.0f, victim.getAbsorptionAmount());
        double absorbedByReserve = Math.min(Math.min(reserve, absorptionBefore), damage);
        if (absorbedByReserve > 0.0) {
            updatedReserve = Math.max(0.0, reserve - absorbedByReserve);
            if (!Mth.equal(updatedReserve, reserve)) {
                OrganState.Change<Double> change = state.setDouble(RESERVE_KEY, updatedReserve);
                logStateChange(LOGGER, prefix(), organ, RESERVE_KEY, change);
                slotUpdate |= change.changed();
            }
        }

        boolean hasJade = hasOrgan(cc, YU_PI_GU_ID);
        double barrierRemaining = Math.max(0.0, state.getDouble(BARRIER_REMAINING_KEY, 0.0));
        int barrierTicks = Math.max(0, state.getInt(BARRIER_TICKS_KEY, 0));

        float adjustedDamage = damage;
        if (hasJade && barrierTicks > 0 && barrierRemaining > 0.0) {
            double reduction = Math.min(barrierRemaining, adjustedDamage * BARRIER_DAMAGE_RATIO);
            if (reduction > 0.0) {
                adjustedDamage = (float) Math.max(0.0, adjustedDamage - reduction);
                OrganState.Change<Double> barrierChange = state.setDouble(BARRIER_REMAINING_KEY, Math.max(0.0, barrierRemaining - reduction));
                slotUpdate |= barrierChange.changed();
            }
        }

        if (hasJade) {
            double reflectMultiplier = REFLECT_BASE;
            if (state.getBoolean(JADE_PRISON_UNLOCKED_KEY, false)) {
                reflectMultiplier += REFLECT_JADE_PRISON_BONUS;
            }
            if (reflectMultiplier > 0.0) {
                reflectDamage(victim, source, adjustedDamage, reflectMultiplier);
            }
        }

        if (slotUpdate) {
            sendSlotUpdate(cc, organ);
        }
        return adjustedDamage;
    }

    /** Restores the absorption reserve to its full capacity. */
    public void restoreReserve(ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        OrganState.Change<Double> change = state.setDouble(RESERVE_KEY, MAX_RESERVE);
        if (change.changed()) {
            logStateChange(LOGGER, prefix(), organ, RESERVE_KEY, change);
            sendSlotUpdate(cc, organ);
        }
    }

    /** Records that an emerald block has been consumed. */
    public void onEmeraldBlockConsumed(ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        int previous = Math.max(0, state.getInt(EMERALD_BLOCKS_KEY, 0));
        int updated = Math.min(3, previous + 1);
        OrganState.Change<Integer> countChange = state.setInt(EMERALD_BLOCKS_KEY, updated);
        boolean promote = updated >= 3 && !state.getBoolean(JADE_PRISON_UNLOCKED_KEY, false);
        boolean slotUpdate = false;
        if (countChange.changed()) {
            logStateChange(LOGGER, prefix(), organ, EMERALD_BLOCKS_KEY, countChange);
            slotUpdate = true;
        }
        if (promote) {
            OrganState.Change<Boolean> change = state.setBoolean(JADE_PRISON_UNLOCKED_KEY, true);
            logStateChange(LOGGER, prefix(), organ, JADE_PRISON_UNLOCKED_KEY, change);
            slotUpdate |= change.changed();
        }
        if (slotUpdate) {
            sendSlotUpdate(cc, organ);
        }
        updateHeShiBiChannel(cc, updated);
    }

    /** Activates the temporary barrier that grants local hardening. */
    public void activateBarrier(ChestCavityInstance cc, ItemStack organ, int durationTicks) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        int clampedDuration = Math.max(0, durationTicks);
        if (clampedDuration <= 0) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        OrganState.Change<Integer> tickChange = state.setInt(BARRIER_TICKS_KEY, clampedDuration);
        OrganState.Change<Double> shieldChange = state.setDouble(BARRIER_REMAINING_KEY, BARRIER_MAX_ABSORPTION);
        boolean slotUpdate = tickChange.changed() || shieldChange.changed();
        if (slotUpdate) {
            logStateChange(LOGGER, prefix(), organ, BARRIER_TICKS_KEY, tickChange);
            logStateChange(LOGGER, prefix(), organ, BARRIER_REMAINING_KEY, shieldChange);
            sendSlotUpdate(cc, organ);
        }
    }

    /** Ensures linkage channels are ready when the organ is installed. */
    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        if (context == null) {
            return;
        }
        LinkageChannel tuDaoChannel = ensureChannel(context, TU_DAO_INCREASE_EFFECT);
        if (tuDaoChannel != null) {
            tuDaoChannel.addPolicy(NON_NEGATIVE);
        }
        LinkageChannel heShiBiChannel = ensureChannel(context, HE_SHI_BI_LINK);
        if (heShiBiChannel != null) {
            heShiBiChannel.addPolicy(NON_NEGATIVE);
        }
    }

    /** Utility that checks whether a specific Guzhenren organ is present in the chest cavity. */
    private boolean hasOrgan(ChestCavityInstance cc, ResourceLocation id) {
        if (cc == null || id == null || cc.inventory == null) {
            return false;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (matchesOrgan(stack, id)) {
                return true;
            }
        }
        return false;
    }

    private void applyHungerDrain(Player player, int hungerPoints) {
        if (player == null || hungerPoints <= 0) {
            return;
        }
        FoodData food = player.getFoodData();
        if (food == null) {
            return;
        }
        int previousFood = food.getFoodLevel();
        int updated = Math.max(0, previousFood - hungerPoints);
        if (updated != previousFood) {
            food.setFoodLevel(updated);
        }
        float previousSaturation = food.getSaturationLevel();
        float updatedSaturation = Math.max(0.0f, previousSaturation - hungerPoints);
        if (!Mth.equal(previousSaturation, updatedSaturation)) {
            food.setSaturation(updatedSaturation);
        }
    }

    private void reflectDamage(LivingEntity victim, DamageSource source, float damage, double multiplier) {
        if (victim == null || damage <= 0.0f || multiplier <= 0.0) {
            return;
        }
        Entity attackerEntity = source == null ? null : source.getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) {
            return;
        }
        if (!attacker.isAlive() || attacker == victim) {
            return;
        }
        float reflected = (float) (damage * multiplier);
        if (reflected <= 0.0f) {
            return;
        }
        Level level = victim.level();
        if (level.isClientSide()) {
            return;
        }
        DamageSource thorns = victim.damageSources().thorns(victim);
        attacker.hurt(thorns, reflected);
    }

    private String prefix() {
        return "[compat/guzhenren][tu_qiang_gu]";
    }

    private static void activateAbility(LivingEntity user, ChestCavityInstance cc) {
        if (user == null || cc == null) {
            return;
        }
        Level level = user.level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (user.isSpectator()) {
            return;
        }

        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }

        OrganState state = INSTANCE.organState(organ, STATE_ROOT);
        Player owner = user instanceof Player ? (Player) user : null;
        if (owner != null) {
            int stored = Math.max(0, state.getInt(EMERALD_BLOCKS_KEY, 0));
            int barrierTicks = Math.max(0, state.getInt(BARRIER_TICKS_KEY, 0));
            LOGGER.info(
                    "[compat/guzhenren][tu_qiang_gu][ability] HOTKEY owner={} sneaking={} stored={} barrier_ticks={}",
                    owner.getScoreboardName(),
                    owner.isShiftKeyDown(),
                    stored,
                    barrierTicks
            );
        }

        if (attemptEmeraldUpgrade(user, cc, organ, state)) {
            return;
        }

        int barrierTicks = Math.max(0, state.getInt(BARRIER_TICKS_KEY, 0));
        if (barrierTicks > 0) {
            if (owner != null) {
                LOGGER.info(
                        "[compat/guzhenren][tu_qiang_gu][ability] EXIT owner={} reason=barrier_active ticks={}",
                        owner.getScoreboardName(),
                        barrierTicks
                );
            }
            return;
        }

        LivingEntity target = findPrisonTarget(user, server);
        if (target == null) {
            if (owner != null) {
                LOGGER.info(
                        "[compat/guzhenren][tu_qiang_gu][ability] EXIT owner={} reason=no_target range={}",
                        owner.getScoreboardName(),
                        ABILITY_RANGE
                );
            }
            return;
        }

        boolean jadePrisonUnlocked = state.getBoolean(JADE_PRISON_UNLOCKED_KEY, false);
        BlockState prisonBlock = jadePrisonUnlocked ? JADE_PRISON_BLOCK : EARTH_PRISON_BLOCK;

        if (owner != null) {
            clearActivePrison(owner, server);
        }

        PrisonPlacement placement = buildPrisonShell(server, target, PRISON_RADIUS, prisonBlock);
        if (placement.blocks().isEmpty()) {
            if (owner != null) {
                LOGGER.info(
                        "[compat/guzhenren][tu_qiang_gu][ability] EXIT owner={} reason=placement_blocked shell_attempts={} shell_placed={} shell_blocked={} shell_out_of_bounds={} shell_replaced={} interior_attempts={} interior_cleared={} interior_blocked={} interior_out_of_bounds={} fallback={}",
                        owner.getScoreboardName(),
                        placement.shellAttempts(),
                        placement.shellPlaced(),
                        placement.shellBlocked(),
                        placement.shellOutOfBounds(),
                        placement.shellReplaced(),
                        placement.interiorAttempts(),
                        placement.interiorCleared(),
                        placement.interiorBlocked(),
                        placement.interiorOutOfBounds(),
                        placement.fallbackUsed()
                );
            }
            return;
        }

        if (owner != null) {
            registerActivePrison(owner, server, placement);
        }

        Vec3 center = Vec3.atCenterOf(target.blockPosition());
        target.teleportTo(center.x, center.y, center.z);
        server.playSound(null, center.x, center.y, center.z, SoundEvents.GRAVEL_PLACE, SoundSource.PLAYERS, 1.0f, 0.9f);

        INSTANCE.activateBarrier(cc, organ, PRISON_DURATION_TICKS);

        if (owner != null) {
            String sample = placement.blocks().stream()
                    .limit(5)
                    .map(BlockPos::toShortString)
                    .collect(Collectors.joining(","));
            LOGGER.info(
                    "[compat/guzhenren][tu_qiang_gu][ability] EXIT owner={} reason=success placed={} block={} sample={} fallback={} shell_attempts={} shell_placed={} shell_blocked={} shell_out_of_bounds={} shell_replaced={} interior_attempts={} interior_cleared={} interior_blocked={} interior_out_of_bounds={}",
                    owner.getScoreboardName(),
                    placement.blocks().size(),
                    placement.block(),
                    sample,
                    placement.fallbackUsed(),
                    placement.shellAttempts(),
                    placement.shellPlaced(),
                    placement.shellBlocked(),
                    placement.shellOutOfBounds(),
                    placement.shellReplaced(),
                    placement.interiorAttempts(),
                    placement.interiorCleared(),
                    placement.interiorBlocked(),
                    placement.interiorOutOfBounds()
            );
        }
    }

    private static void clearActivePrison(Player owner, ServerLevel level) {
        UUID ownerId = owner.getUUID();
        ActivePrison existing = ACTIVE_PRISONS.remove(ownerId);
        if (existing == null) {
            return;
        }
        LOGGER.info("[compat/guzhenren][tu_qiang_gu][cleanup] skipped removal owner={} version={}", ownerId, existing.version());
    }

    private static void registerActivePrison(Player owner, ServerLevel level, PrisonPlacement placement) {
        if (placement == null || placement.blocks().isEmpty()) {
            return;
        }
        long version = PRISON_SEQUENCE.incrementAndGet();
        ActivePrison active = new ActivePrison(level.dimension(), placement, version);
        ACTIVE_PRISONS.put(owner.getUUID(), active);
        LOGGER.info("[compat/guzhenren][tu_qiang_gu][scheduler] cleanup disabled owner={} version={}", owner.getUUID(), version);
    }

    private static boolean attemptEmeraldUpgrade(LivingEntity user, ChestCavityInstance cc, ItemStack organ, OrganState state) {
        if (!(user instanceof Player player)) {
            return false;
        }
        if (cc == null || organ == null || organ.isEmpty()) {
            return false;
        }
        int stored = Math.max(0, state.getInt(EMERALD_BLOCKS_KEY, 0));
        if (stored >= 3) {
            boolean unlocked = state.getBoolean(JADE_PRISON_UNLOCKED_KEY, false);
            if (!unlocked) {
                OrganState.Change<Boolean> fix = state.setBoolean(JADE_PRISON_UNLOCKED_KEY, true);
                INSTANCE.logStateChange(LOGGER, INSTANCE.prefix(), organ, JADE_PRISON_UNLOCKED_KEY, fix);
                if (fix.changed()) {
                    INSTANCE.sendSlotUpdate(cc, organ);
                }
            }
            INSTANCE.updateHeShiBiChannel(cc, stored);
            return false;
        }
        if (state.getBoolean(JADE_PRISON_UNLOCKED_KEY, false)) {
            INSTANCE.updateHeShiBiChannel(cc, stored);
            return false;
        }
        if (!player.isShiftKeyDown()) {
            return false;
        }
        if (!consumeEmeraldBlock(player)) {
            return false;
        }
        INSTANCE.onEmeraldBlockConsumed(cc, organ);
        if (player.level() instanceof ServerLevel server) {
            int total = Math.max(0, state.getInt(EMERALD_BLOCKS_KEY, stored + 1));
            playEmeraldUpgradeCue(server, player, total);
        }
        return true;
    }

    private static boolean consumeEmeraldBlock(Player player) {
        if (player == null) {
            return false;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && isEmeraldUpgradeItem(stack)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.setItem(slot, ItemStack.EMPTY);
                }
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    private static boolean isEmeraldUpgradeItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.is(Blocks.EMERALD_BLOCK.asItem())) {
            return true;
        }
        var jadeItem = JADE_PRISON_BLOCK.getBlock().asItem();
        return stack.is(jadeItem);
    }

    private static void playEmeraldUpgradeCue(ServerLevel server, Player player, int total) {
        float clamped = Math.min(total, 3);
        float pitch = 0.9f + (clamped * 0.05f);
        server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.7f, pitch);
    }

    private void updateHeShiBiChannel(ChestCavityInstance cc, int emeraldBlocks) {
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        if (context == null) {
            return;
        }
        LinkageChannel channel = ensureChannel(context, HE_SHI_BI_LINK);
        if (channel != null) {
            channel.set(Math.max(0, emeraldBlocks));
        }
    }

    public ItemStack locateOrgan(ChestCavityInstance cc) {
        return findOrgan(cc);
    }

    public int getEmeraldBlocksConsumed(ItemStack organ) {
        if (organ == null || organ.isEmpty()) {
            return 0;
        }
        OrganState state = organState(organ, STATE_ROOT);
        return Math.max(0, state.getInt(EMERALD_BLOCKS_KEY, 0));
    }

    public int remainingEmeraldUpgrades(ItemStack organ) {
        if (organ == null || organ.isEmpty()) {
            return 0;
        }
        OrganState state = organState(organ, STATE_ROOT);
        if (state.getBoolean(JADE_PRISON_UNLOCKED_KEY, false)) {
            return 0;
        }
        int stored = Math.max(0, state.getInt(EMERALD_BLOCKS_KEY, 0));
        return Math.max(0, 3 - stored);
    }

    private static ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (INSTANCE.matchesOrgan(stack, ORGAN_ID)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static LivingEntity findPrisonTarget(LivingEntity user, ServerLevel level) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(level, "level");
        AABB search = user.getBoundingBox().inflate(ABILITY_RANGE);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, search, candidate -> {
            if (candidate == null) {
                return false;
            }
            if (candidate == user || !candidate.isAlive()) {
                return false;
            }
            if (candidate.isSpectator() || candidate.isAlliedTo(user)) {
                return false;
            }
            if (candidate instanceof Player player && (player.isCreative() || player.isSpectator())) {
                return false;
            }
            return candidate.distanceToSqr(user) <= RANGE_SQUARED;
        });
        return candidates.stream()
                .max(Comparator
                        .comparingDouble(LivingEntity::getHealth)
                        .thenComparingDouble(LivingEntity::getMaxHealth))
                .orElse(null);
    }

    private static PrisonPlacement buildPrisonShell(ServerLevel level, LivingEntity target, int radius, BlockState blockState) {
        if (radius <= 0 || blockState == null || target == null) {
            return new PrisonPlacement(List.<BlockPos>of(), Blocks.AIR, 0, 0, 0, 0, 0, 0, 0, 0, 0, false);
        }
        BlockPos center = target.blockPosition();
        List<BlockPos> shellPositions = new ArrayList<>();
        List<BlockPos> interiorPositions = new ArrayList<>();
        List<BlockPos> placed = new ArrayList<>();
        PlacementAccumulator stats = new PlacementAccumulator();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int outerSq = radius * radius;
        int shellSq = Math.max(1, (radius - 1) * (radius - 1));
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int distanceSq = dx * dx + dy * dy + dz * dz;
                    if (distanceSq > outerSq) {
                        continue;
                    }
                    mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    boolean onBoundary = distanceSq >= shellSq
                            || Math.abs(dx) == radius
                            || Math.abs(dy) == radius
                            || Math.abs(dz) == radius;
                    if (onBoundary) {
                        shellPositions.add(mutable.immutable());
                    } else {
                        interiorPositions.add(mutable.immutable());
                    }
                }
            }
        }
        for (BlockPos pos : interiorPositions) {
            tryClearInterior(level, pos, stats);
        }
        for (BlockPos pos : shellPositions) {
            tryPlacePrisonBlock(level, pos, blockState, placed, stats);
        }
        boolean fallbackUsed = false;
        if (placed.isEmpty()) {
            fallbackUsed = placeFallbackCap(level, center, radius, blockState, placed, stats);
        }
        return new PrisonPlacement(
                placed,
                blockState.getBlock(),
                stats.shellAttempts,
                stats.shellPlaced,
                stats.shellBlocked,
                stats.shellOutOfBounds,
                stats.shellReplaced,
                stats.interiorAttempts,
                stats.interiorCleared,
                stats.interiorBlocked,
                stats.interiorOutOfBounds,
                fallbackUsed
        );
    }

    private static boolean placeFallbackCap(
            ServerLevel level,
            BlockPos center,
            int radius,
            BlockState blockState,
            List<BlockPos> placed,
            PlacementAccumulator stats
    ) {
        int capY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + Math.max(2, radius));
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if ((dx * dx) + (dz * dz) > radius * radius) {
                    continue;
                }
                mutable.set(center.getX() + dx, capY, center.getZ() + dz);
                if (tryPlacePrisonBlock(level, mutable, blockState, placed, stats)) {
                    any = true;
                }
            }
        }
        return any;
    }

    private static boolean tryPlacePrisonBlock(
            ServerLevel level,
            BlockPos pos,
            BlockState blockState,
            List<BlockPos> placed,
            PlacementAccumulator stats
    ) {
        stats.shellAttempts++;
        if (!level.isInWorldBounds(pos)) {
            stats.shellOutOfBounds++;
            return false;
        }
        BlockState existing = level.getBlockState(pos);
        boolean replaced = false;
        if (!existing.isAir()) {
            boolean replaceable = !existing.getFluidState().isEmpty()
                    || existing.getCollisionShape(level, pos).isEmpty()
                    || existing.getDestroySpeed(level, pos) >= 0.0f;
            if (!replaceable || existing.is(Blocks.BEDROCK)) {
                stats.shellBlocked++;
                return false;
            }
            replaced = true;
        }
        level.setBlock(pos, blockState, Block.UPDATE_ALL_IMMEDIATE);
        LOGGER.info("[compat/guzhenren][tu_qiang_gu][placement] shell place owner={} pos={} replaced={} block={}",
                level.players().isEmpty() ? "?" : level.players().get(0).getScoreboardName(),
                pos,
                replaced,
                blockState.getBlock());
        placed.add(pos.immutable());
        stats.shellPlaced++;
        if (replaced) {
            stats.shellReplaced++;
        }
        return true;
    }

    private static boolean tryClearInterior(ServerLevel level, BlockPos pos, PlacementAccumulator stats) {
        stats.interiorAttempts++;
        if (!level.isInWorldBounds(pos)) {
            stats.interiorOutOfBounds++;
            return false;
        }
        BlockState existing = level.getBlockState(pos);
        if (existing.isAir()) {
            return true;
        }
        if (existing.is(Blocks.BEDROCK) || existing.getDestroySpeed(level, pos) < 0.0f) {
            stats.interiorBlocked++;
            return false;
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        stats.interiorCleared++;
        return true;
    }

    private static void schedulePrisonCleanup(
            ServerLevel level,
            UUID ownerId,
            long version,
            PrisonPlacement placement,
            int delayTicks
    ) {
        if (placement == null || placement.blocks().isEmpty()) {
            return;
        }
        MinecraftServer server = level.getServer();
        int effectiveDelay = Math.max(1, delayTicks);
        int currentTick = server.getTickCount();
        int targetTick = currentTick + effectiveDelay;
        LOGGER.info("[compat/guzhenren][tu_qiang_gu][scheduler] owner={} version={} delay={} current_tick={} target_tick={}",
                ownerId,
                version,
                effectiveDelay,
                currentTick,
                targetTick);
        server.tell(new TickTask(targetTick, () -> cleanupPrison(server, ownerId, version)));
    }

    private static void cleanupPrison(MinecraftServer server, UUID ownerId, long version) {
        ActivePrison active = ACTIVE_PRISONS.get(ownerId);
        if (active == null || active.version() != version) {
            return;
        }
        ServerLevel level = server.getLevel(active.levelKey());
        if (level != null) {
        LOGGER.info("[compat/guzhenren][tu_qiang_gu][cleanup] owner={} version={} releasing={} coords={}",
                ownerId,
                version,
                active.placement().blocks().size(),
                summarizeBlocks(active.placement().blocks(), 5));
        releasePrison(level, ownerId, version, active.placement());
        }
        ACTIVE_PRISONS.remove(ownerId, active);
    }

    private static void releasePrison(ServerLevel level, UUID ownerId, long version, PrisonPlacement placement) {
        if (placement == null || placement.blocks().isEmpty()) {
            return;
        }
        Block block = placement.block();
        for (BlockPos pos : placement.blocks()) {
            if (block != null && !level.getBlockState(pos).is(block)) {
                continue;
            }
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            LOGGER.info("[compat/guzhenren][tu_qiang_gu][cleanup] remove owner={} version={} pos={}", ownerId, version, pos);
        }
    }

    private static String summarizeBlocks(List<BlockPos> blocks, int limit) {
        return blocks.stream().limit(limit).map(BlockPos::toShortString).collect(Collectors.joining(","));
    }

    private static BlockState resolvePrisonBlock(ResourceLocation id, BlockState fallback) {
        if (id == null) {
            return fallback;
        }
        return BuiltInRegistries.BLOCK.getOptional(id)
                .map(Block::defaultBlockState)
                .orElse(fallback);
    }

    private static final class PlacementAccumulator {
        private int shellAttempts;
        private int shellPlaced;
        private int shellBlocked;
        private int shellOutOfBounds;
        private int shellReplaced;
        private int interiorAttempts;
        private int interiorCleared;
        private int interiorBlocked;
        private int interiorOutOfBounds;
    }

    private record PrisonPlacement(
            List<BlockPos> blocks,
            Block block,
            int shellAttempts,
            int shellPlaced,
            int shellBlocked,
            int shellOutOfBounds,
            int shellReplaced,
            int interiorAttempts,
            int interiorCleared,
            int interiorBlocked,
            int interiorOutOfBounds,
            boolean fallbackUsed
    ) {
    }

    private record ActivePrison(ResourceKey<Level> levelKey, PrisonPlacement placement, long version) {
    }
}
