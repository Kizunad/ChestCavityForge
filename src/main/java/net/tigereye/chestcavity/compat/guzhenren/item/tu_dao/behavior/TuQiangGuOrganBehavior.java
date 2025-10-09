package net.tigereye.chestcavity.compat.guzhenren.item.tu_dao.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Behaviour implementation for 土墙蛊 (Tu Qiang Gu).
 *
 * <p>The behaviour is intentionally incremental – it focuses on delivering the
 * defensive loop (absorption reservoir + hunger upkeep) and the 玉皮蛊反伤联动 so
 * the surrounding infrastructure can build on it later. Active ability support
 * and world interactions (placing temporary walls) will be layered on top in a
 * follow-up patch once the groundwork is proven stable.</p>
 */
public enum TuQiangGuOrganBehavior implements OrganSlowTickListener, OrganIncomingDamageListener {
    INSTANCE;

    private static final Logger LOGGER = ChestCavity.LOGGER;

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
    private static final int PRISON_DURATION_TICKS = 20 * 8;

    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "tu_qiang_gu");
    private static final ResourceLocation YU_PI_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "yu_pi_gu");
    private static final ResourceLocation TU_LAO_BLOCK_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "tu_lao");
    private static final ResourceLocation JADE_LAO_BLOCK_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "yu_lao");
    private static final ResourceLocation TU_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/tu_dao_increase_effect");
    private static final ResourceLocation HE_SHI_BI_LINK =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/he_shi_bi");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final BlockState EARTH_PRISON_BLOCK = resolvePrisonBlock(TU_LAO_BLOCK_ID, Blocks.PACKED_MUD.defaultBlockState());
    private static final BlockState JADE_PRISON_BLOCK = resolvePrisonBlock(JADE_LAO_BLOCK_ID, Blocks.EMERALD_BLOCK.defaultBlockState());

    private static final double RANGE_SQUARED = ABILITY_RANGE * ABILITY_RANGE;

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
                entity.setAbsorptionAmount((float) reserve);
            }
        }

        if (slotUpdate) {
            sendSlotUpdate(cc, organ);
        }
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
        int barrierTicks = Math.max(0, state.getInt(BARRIER_TICKS_KEY, 0));
        if (barrierTicks > 0) {
            return;
        }

        LivingEntity target = findPrisonTarget(user, server);
        if (target == null) {
            return;
        }

        boolean jadePrisonUnlocked = state.getBoolean(JADE_PRISON_UNLOCKED_KEY, false);
        BlockState prisonBlock = jadePrisonUnlocked ? JADE_PRISON_BLOCK : EARTH_PRISON_BLOCK;

        PrisonPlacement placement = buildPrisonShell(server, target, PRISON_RADIUS, prisonBlock);
        if (placement.blocks().isEmpty()) {
            return;
        }

        Vec3 center = Vec3.atCenterOf(target.blockPosition());
        target.teleportTo(center.x, center.y, center.z);
        server.playSound(null, center.x, center.y, center.z, SoundEvents.GRAVEL_PLACE, SoundSource.PLAYERS, 1.0f, 0.9f);

        INSTANCE.activateBarrier(cc, organ, PRISON_DURATION_TICKS);

        schedulePrisonCleanup(server, placement, PRISON_DURATION_TICKS);
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
            return new PrisonPlacement(List.of(), Blocks.AIR);
        }
        BlockPos center = target.blockPosition();
        List<BlockPos> placed = new ArrayList<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int outerSq = radius * radius;
        int innerSq = Math.max(0, (radius - 1) * (radius - 1));
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int distanceSq = dx * dx + dy * dy + dz * dz;
                    if (distanceSq > outerSq || distanceSq < innerSq) {
                        continue;
                    }
                    mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.isInWorldBounds(mutable)) {
                        continue;
                    }
                    if (!level.getBlockState(mutable).isAir()) {
                        continue;
                    }
                    level.setBlockAndUpdate(mutable, blockState);
                    placed.add(mutable.immutable());
                }
            }
        }
        return new PrisonPlacement(placed, blockState.getBlock());
    }

    private static void schedulePrisonCleanup(ServerLevel level, PrisonPlacement placement, int delayTicks) {
        if (placement == null || placement.blocks().isEmpty()) {
            return;
        }
        if (delayTicks <= 0) {
            cleanupPrison(level, placement);
            return;
        }
        int targetTick = level.getServer().getTickCount() + delayTicks;
        level.getServer().tell(new TickTask(targetTick, () -> cleanupPrison(level, placement)));
    }

    private static void cleanupPrison(ServerLevel level, PrisonPlacement placement) {
        if (placement == null || placement.blocks().isEmpty()) {
            return;
        }
        Block block = placement.block();
        for (BlockPos pos : placement.blocks()) {
            if (block != null && !level.getBlockState(pos).is(block)) {
                continue;
            }
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    private static BlockState resolvePrisonBlock(ResourceLocation id, BlockState fallback) {
        if (id == null) {
            return fallback;
        }
        return BuiltInRegistries.BLOCK.getOptional(id)
                .map(Block::defaultBlockState)
                .orElse(fallback);
    }

    private record PrisonPlacement(List<BlockPos> blocks, Block block) {
    }
}
