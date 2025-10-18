package net.tigereye.chestcavity.compat.guzhenren.item.yun_dao_cloud.behavior;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Vector3f;

/**
 * 云道·白云蛊行为实现。
 *
 * <p>基础数值说明（OrganScore 将在后续配置时补充具体值）：</p>
 * <ul>
 *     <li>每秒恢复：生命 +4、精力 +1。</li>
 *     <li>云纹：每 6 秒获得 1 层，最多 20 层。</li>
 *     <li>减伤：单次受击最多消耗 8 层，每层提供 90% 减伤，防止的伤害最多 10 点/层。</li>
 *     <li>主动技「云爆」：半径 5 格，伤害 = 云纹 *10，敌方缓慢 III 3 秒，自身抗性 III 3 秒。</li>
 *     <li>若云爆消耗云纹 >10，则生成 3 分钟雾，雾内持续施加缓慢 I、虚弱 I；雾中生物死亡按 HP:100 换算为云纹返还。</li>
 * </ul>
 */
public final class BaiYunGuOrganBehavior extends AbstractGuzhenrenOrganBehavior implements OrganSlowTickListener, OrganIncomingDamageListener, OrganRemovalListener {

    public static final BaiYunGuOrganBehavior INSTANCE = new BaiYunGuOrganBehavior();

    private BaiYunGuOrganBehavior() {}

    private static final String MOD_ID = "guzhenren";
    public static final ResourceLocation ORGAN_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "bai_yun_gu");
    public static final ResourceLocation ABILITY_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "bai_yun_gu/cloud_step");

    private static final double HEAL_PER_SECOND = 4.0D;
    private static final double JINGLI_PER_SECOND = 1.0D;
    private static final int STACK_INTERVAL_SLOW_TICKS = 6;
    private static final int MAX_STACKS = 20;
    private static final int MAX_STACKS_PER_HIT = 8;
    private static final double DAMAGE_REDUCTION_RATIO = 0.90D;
    private static final double DAMAGE_REDUCTION_PER_STACK = 10.0D;
    private static final double BLAST_RADIUS = 5.0D;
    private static final double BLAST_DAMAGE_PER_STACK = 10.0D;
    private static final int BLAST_SLOW_DURATION = 60; // 3 秒
    private static final int BLAST_SLOW_AMPLIFIER = 2; // 缓慢 III
    private static final int SELF_RESIST_DURATION = 60; // 3 秒
    private static final int SELF_RESIST_AMPLIFIER = 2; // 抗性 III
    private static final int FOG_DURATION_TICKS = 3 * 60 * 20; // 3 分钟
    private static final double FOG_RADIUS = 5.0D;
    private static final int FOG_EFFECT_INTERVAL = 20; // 每秒刷新
    private static final int FOG_SLOW_DURATION = 40;
    private static final int FOG_SLOW_AMPLIFIER = 0; // 缓慢 I
    private static final int FOG_WEAKNESS_DURATION = 40;
    private static final int FOG_WEAKNESS_AMPLIFIER = 0; // 虚弱 I
    private static final double FOG_CONVERSION_RATIO = 1.0D / 100.0D;
    private static final int CLOUD_THRESHOLD_FOR_FOG = 10;

    private static final DustParticleOptions CLOUD_GLOW_PARTICLE =
            new DustParticleOptions(new Vector3f(200f / 255f, 240f / 255f, 1.0f), 0.65f);
    private static final DustColorTransitionOptions CLOUD_INNER_PULSE =
            new DustColorTransitionOptions(
                    new Vector3f(200f / 255f, 240f / 255f, 1.0f),
                    new Vector3f(1.0f, 1.0f, 1.0f),
                    0.8f
            );
    private static final Vector3f FOG_COLOR_INNER = new Vector3f(200f / 255f, 240f / 255f, 1.0f);
    private static final Vector3f FOG_COLOR_OUTER = new Vector3f(160f / 255f, 200f / 255f, 220f / 255f);
    private static final float FOG_PARTICLE_SCALE = 0.6f;
    private static final int FOG_BASE_PARTICLE_COUNT = 28;
    private static final int FOG_GLOW_PARTICLE_COUNT = 12;
    private static final int FOG_FLOW_PARTICLE_COUNT = 16;
    private static final int FOG_SOUND_INTERVAL_TICKS = 120;
    private static final float FOG_FLOW_MIN_SPEED = 0.12f;
    private static final float FOG_FLOW_MAX_SPEED = 0.42f;
    private static final float FOG_FLOW_JITTER_DEGREES = 12.0f;
    private static final double BASE_RING_RADIUS = 0.5D;
    private static final double RING_RADIUS_PER_STACK = 0.02D;
    private static final double RING_HEIGHT_STEP = 0.05D;
    private static final int BASE_RING_POINTS = 14;
    private static final int MAX_RING_POINTS = 26;
    private static final double MAX_RING_SPIN_DEGREES = 12.0D;
    private static final float STACK_GAIN_SOUND_VOLUME = 0.32f;
    private static final float STACK_GAIN_SOUND_PITCH = 1.4f;

    private static final String STATE_ROOT = "BaiYunGu";
    private static final String STACKS_KEY = "Stacks";
    private static final String STACK_TIMER_KEY = "StackTimer";
    private static final String RING_PHASE_KEY = "RingPhase";

    private static final int BURST_INNER_TICKS = 10;
    private static final int BURST_WAVE_TICKS = 20;
    private static final int BURST_TAIL_TICKS = 50;
    private static final double BURST_BASE_RADIUS = 1.0D;
    private static final double BURST_RADIUS_PER_STACK = 0.08D;
    private static final double BURST_WAVE_GROWTH = 0.18D;
    private static final double BURST_TAIL_GROWTH = 0.1D;

    static {
        OrganActivationListeners.register(ABILITY_ID, BaiYunGuOrganBehavior::activateAbility);
    }

    /**
     * 预留：保持云纹状态在初次附着时处于合法区间。
     */
    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        OrganState state = OrganState.of(organ, STATE_ROOT);
        int stacks = Mth.clamp(state.getInt(STACKS_KEY, 0), 0, MAX_STACKS);
        int timer = Mth.clamp(state.getInt(STACK_TIMER_KEY, 0), 0, STACK_INTERVAL_SLOW_TICKS - 1);
        double phase = state.getDouble(RING_PHASE_KEY, 0.0D);
        boolean changed = false;
        if (stacks != state.getInt(STACKS_KEY, 0)) {
            changed |= state.setInt(STACKS_KEY, stacks, value -> Mth.clamp(value, 0, MAX_STACKS), 0).changed();
        }
        if (timer != state.getInt(STACK_TIMER_KEY, 0)) {
            changed |= state.setInt(STACK_TIMER_KEY, timer, value -> Mth.clamp(value, 0, STACK_INTERVAL_SLOW_TICKS - 1), 0).changed();
        }
        changed |= state.setDouble(RING_PHASE_KEY, normalizePhase(phase), BaiYunGuOrganBehavior::normalizePhase, 0.0D).changed();
        if (changed) {
            INSTANCE.sendSlotUpdate(cc, organ);
        }
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || cc == null || organ == null || organ.isEmpty() || entity.level().isClientSide() || !entity.isAlive()) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) entity.level();
        entity.heal((float) HEAL_PER_SECOND);
        ResourceOps.tryAdjustJingli(entity, JINGLI_PER_SECOND);
        ReactionTagOps.add(entity, ReactionTagKeys.CLOUD_SHROUD, 80);

        OrganState state = OrganState.of(organ, STATE_ROOT);
        int currentStacks = Mth.clamp(state.getInt(STACKS_KEY, 0), 0, MAX_STACKS);
        int timer = Mth.clamp(state.getInt(STACK_TIMER_KEY, 0), 0, STACK_INTERVAL_SLOW_TICKS - 1);

        timer++;
        if (timer >= STACK_INTERVAL_SLOW_TICKS) {
            timer = 0;
            if (currentStacks < MAX_STACKS) {
                currentStacks++;
            }
        }

        OrganState.Change<Integer> stackChange =
                state.setInt(STACKS_KEY, currentStacks, value -> Mth.clamp(value, 0, MAX_STACKS), 0);
        OrganState.Change<Integer> timerChange =
                state.setInt(STACK_TIMER_KEY, timer, value -> Mth.clamp(value, 0, STACK_INTERVAL_SLOW_TICKS - 1), 0);

        int updatedStacks = stackChange.current();
        int previousStacks = Math.max(0, stackChange.previous());

        if (stackChange.changed()) {
            int gained = Math.max(0, updatedStacks - previousStacks);
            if (gained > 0) {
                boolean reachedMax = updatedStacks >= MAX_STACKS && previousStacks < MAX_STACKS;
                spawnStackGainParticles(serverLevel, entity, updatedStacks, gained, reachedMax);
            }
        }

        boolean dirty = stackChange.changed() || timerChange.changed();

        double phase = state.getDouble(RING_PHASE_KEY, 0.0D);
        if (updatedStacks >= MAX_STACKS) {
            phase = normalizePhase(phase + MAX_RING_SPIN_DEGREES);
            spawnMaxAuraParticles(serverLevel, entity, updatedStacks, phase);
            dirty |= state.setDouble(RING_PHASE_KEY, phase, BaiYunGuOrganBehavior::normalizePhase, 0.0D).changed();
        } else if (!Mth.equal(phase, 0.0D)) {
            dirty |= state.setDouble(RING_PHASE_KEY, 0.0D, ignored -> 0.0D, 0.0D).changed();
        }

        if (dirty) {
            INSTANCE.sendSlotUpdate(cc, organ);
        }
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (victim == null || cc == null || organ == null || organ.isEmpty() || victim.level().isClientSide()) {
            return damage;
        }
        if (!(damage > 0.0F)) {
            return damage;
        }

        OrganState state = OrganState.of(organ, STATE_ROOT);
        int stacks = Mth.clamp(state.getInt(STACKS_KEY, 0), 0, MAX_STACKS);
        if (stacks <= 0) {
            return damage;
        }

        double theoreticalReduction = damage * DAMAGE_REDUCTION_RATIO;
        double stackLimit = stacks * DAMAGE_REDUCTION_PER_STACK;
        double perHitLimit = MAX_STACKS_PER_HIT * DAMAGE_REDUCTION_PER_STACK;
        double reduction = Math.min(Math.min(theoreticalReduction, stackLimit), perHitLimit);
        if (!(reduction > 0.0D)) {
            return damage;
        }

        int consumableStacks = Math.min(stacks, MAX_STACKS_PER_HIT);
        int consumed = Math.min(consumableStacks, (int) Math.ceil(reduction / DAMAGE_REDUCTION_PER_STACK));
        if (consumed <= 0) {
            return damage;
        }

        double adjusted = Math.max(0.0D, damage - reduction);
        boolean dirty = state.setInt(STACKS_KEY, stacks - consumed, value -> Mth.clamp(value, 0, MAX_STACKS), 0).changed();
        if (dirty) {
            INSTANCE.sendSlotUpdate(cc, organ);
        }
        return (float) adjusted;
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || organ == null || organ.isEmpty()) {
            return;
        }
        OrganState state = OrganState.of(organ, STATE_ROOT);
        boolean dirty = false;
        dirty |= state.setInt(STACKS_KEY, 0, value -> 0, 0).changed();
        dirty |= state.setInt(STACK_TIMER_KEY, 0, value -> 0, 0).changed();
        dirty |= state.setDouble(RING_PHASE_KEY, 0.0D, ignored -> 0.0D, 0.0D).changed();
        if (dirty && cc != null) {
            INSTANCE.sendSlotUpdate(cc, organ);
        }
        CloudFogManager.clear(entity.getUUID());
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (entity == null || entity.level().isClientSide() || cc == null) {
            return;
        }
        Level level = entity.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }

        OrganState state = OrganState.of(organ, STATE_ROOT);
        int stacks = Mth.clamp(state.getInt(STACKS_KEY, 0), 0, MAX_STACKS);
        if (stacks <= 0) {
            return;
        }

        state.setInt(STACKS_KEY, 0, value -> 0, 0);
        state.setInt(STACK_TIMER_KEY, 0, value -> 0, 0);
        INSTANCE.sendSlotUpdate(cc, organ);

        double abilityDamage = stacks * BLAST_DAMAGE_PER_STACK;
        applyCloudBlast(serverLevel, entity, abilityDamage);
        applySelfResistance(entity);
        spawnCloudBurstVisuals(serverLevel, entity, stacks);

        if (stacks > CLOUD_THRESHOLD_FOR_FOG) {
            CloudFogManager.activate(serverLevel, entity, entity.position(), FOG_RADIUS, FOG_DURATION_TICKS);
        }
    }

    private static void applyCloudBlast(ServerLevel level, LivingEntity owner, double damageAmount) {
        AABB area = new AABB(owner.position(), owner.position()).inflate(BLAST_RADIUS);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area, target ->
                target.isAlive() && target != owner && !target.isAlliedTo(owner));
        if (victims.isEmpty()) {
            return;
        }

        DamageSource source;
        if (owner instanceof Player player) {
            source = player.damageSources().playerAttack(player);
        } else if (owner instanceof Mob mob) {
            source = owner.damageSources().mobAttack(mob);
        } else {
            source = owner.damageSources().generic();
        }

        float amount = (float) Math.max(0.0D, damageAmount);
        for (LivingEntity victim : victims) {
            victim.hurt(source, amount);
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BLAST_SLOW_DURATION, BLAST_SLOW_AMPLIFIER, false, true, true));
        }
    }

    private static void applySelfResistance(LivingEntity owner) {
        owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, SELF_RESIST_DURATION, SELF_RESIST_AMPLIFIER, false, false, true));
    }

    private static ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (INSTANCE.matchesOrgan(stack, ORGAN_ID)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean grantStacks(ServerLevel level, UUID ownerId, int amount) {
        if (amount <= 0) {
            return false;
        }
        Entity entity = level.getEntity(ownerId);
        if (!(entity instanceof LivingEntity owner) || !owner.isAlive()) {
            return false;
        }
        ChestCavityInstance cc = ChestCavityEntity.of(owner)
                .map(ChestCavityEntity::getChestCavityInstance)
                .orElse(null);
        if (cc == null) {
            return false;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return false;
        }
        OrganState state = OrganState.of(organ, STATE_ROOT);
        int stacks = Mth.clamp(state.getInt(STACKS_KEY, 0), 0, MAX_STACKS);
        int updated = Math.min(MAX_STACKS, stacks + amount);
        if (updated == stacks) {
            return false;
        }
        OrganState.Change<Integer> stackChange =
                state.setInt(STACKS_KEY, updated, value -> Mth.clamp(value, 0, MAX_STACKS), 0);
        if (!stackChange.changed()) {
            return false;
        }
        int newStacks = stackChange.current();
        int previousStacks = Math.max(0, stackChange.previous());
        int gained = Math.max(0, newStacks - previousStacks);
        if (gained > 0) {
            boolean reachedMax = newStacks >= MAX_STACKS && previousStacks < MAX_STACKS;
            spawnStackGainParticles(level, owner, newStacks, gained, reachedMax);
        }
        if (newStacks >= MAX_STACKS) {
            double phase = normalizePhase(state.getDouble(RING_PHASE_KEY, 0.0D) + MAX_RING_SPIN_DEGREES);
            state.setDouble(RING_PHASE_KEY, phase, BaiYunGuOrganBehavior::normalizePhase, 0.0D);
            spawnMaxAuraParticles(level, owner, newStacks, phase);
        }
        INSTANCE.sendSlotUpdate(cc, organ);
        return true;
    }

    private static void spawnStackGainParticles(
            ServerLevel level,
            LivingEntity owner,
            int stackCount,
            int gained,
            boolean reachedMaxThisTick
    ) {
        if (level == null || owner == null || stackCount <= 0) {
            return;
        }
        RandomSource random = level.getRandom();
        Vec3 origin = owner.position().add(0.0D, owner.getBbHeight() * 0.6D, 0.0D);
        int ringCount = Math.min(4, Math.max(1, (stackCount + 5) / 6));
        for (int ring = 0; ring < ringCount; ring++) {
            int points = Math.min(MAX_RING_POINTS, BASE_RING_POINTS + ring * 4 + stackCount / 2);
            double radius = BASE_RING_RADIUS + stackCount * RING_RADIUS_PER_STACK + ring * 0.04D;
            double height = ring * RING_HEIGHT_STEP;
            double angleOffset = random.nextDouble() * Mth.TWO_PI;
            for (int i = 0; i < points; i++) {
                double angle = angleOffset + (Mth.TWO_PI * i / points);
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                double x = origin.x + cos * radius;
                double z = origin.z + sin * radius;
                double y = origin.y + height + random.nextDouble() * 0.05D;
                double upward = 0.015D + stackCount * 0.0015D + random.nextDouble() * 0.01D;
                level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.0D, upward, 0.0D, 0.0D);
                level.sendParticles(CLOUD_GLOW_PARTICLE, x, y, z, 1, 0.0D, upward * 0.55D, 0.0D, 0.0D);
            }
        }
        if (reachedMaxThisTick) {
            playMaxStackSound(level, owner);
        }
    }

    private static void spawnMaxAuraParticles(ServerLevel level, LivingEntity owner, int stackCount, double phaseDegrees) {
        if (level == null || owner == null) {
            return;
        }
        Vec3 origin = owner.position().add(0.0D, owner.getBbHeight() * 0.66D, 0.0D);
        double radius = BASE_RING_RADIUS + stackCount * RING_RADIUS_PER_STACK + 0.18D;
        double phase = Math.toRadians(phaseDegrees);
        int points = MAX_RING_POINTS;
        double jitter = 0.004D;
        double upward = 0.02D + stackCount * 0.001D;
        for (int i = 0; i < points; i++) {
            double angle = phase + (Mth.TWO_PI * i / points);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double x = origin.x + cos * radius;
            double z = origin.z + sin * radius;
            double y = origin.y + Math.sin(angle * 2.0D) * 0.04D;
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, cos * jitter, upward, sin * jitter, 0.0D);
            level.sendParticles(CLOUD_GLOW_PARTICLE, x, y, z, 1, 0.0D, upward * 0.6D, 0.0D, 0.0D);
        }
    }

    private static void playMaxStackSound(ServerLevel level, LivingEntity owner) {
        level.playSound(
                null,
                owner.getX(),
                owner.getY(),
                owner.getZ(),
                SoundEvents.ELYTRA_FLYING,
                SoundSource.PLAYERS,
                STACK_GAIN_SOUND_VOLUME,
                STACK_GAIN_SOUND_PITCH + level.getRandom().nextFloat() * 0.1f
        );
    }

    private static double normalizePhase(double degrees) {
        double value = degrees % 360.0D;
        if (value < 0.0D) {
            value += 360.0D;
        }
        return value;
    }

    private static DustColorTransitionOptions fogParticleOptions(float mix) {
        float clamped = Mth.clamp(mix, 0.0f, 1.0f);
        Vector3f start = new Vector3f(FOG_COLOR_INNER).lerp(FOG_COLOR_OUTER, clamped);
        float glowMix = Math.min(1.0f, clamped + 0.18f);
        Vector3f end = new Vector3f(start).lerp(FOG_COLOR_OUTER, glowMix);
        return new DustColorTransitionOptions(start, end, FOG_PARTICLE_SCALE);
    }

    private static void spawnCloudBurstVisuals(ServerLevel level, LivingEntity owner, int stackCount) {
        if (level == null || owner == null || stackCount <= 0) {
            return;
        }
        Vec3 origin = owner.position().add(0.0D, owner.getBbHeight() * 0.6D, 0.0D);
        float soundVolume = 0.45f + Math.min(stackCount, MAX_STACKS) * 0.02f;
        float soundPitch = 1.0f + Math.min(stackCount, MAX_STACKS) * 0.015f;
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, soundVolume, soundPitch);

        for (int tick = 0; tick < BURST_INNER_TICKS; tick++) {
            final int delay = tick;
            TickOps.schedule(level, () -> executeInnerPulse(level, origin, stackCount, delay), delay);
        }
        for (int tick = 0; tick < BURST_WAVE_TICKS; tick++) {
            final int delay = BURST_INNER_TICKS + tick;
            TickOps.schedule(level, () -> executeWavePulse(level, origin, stackCount, delay - BURST_INNER_TICKS), delay);
        }
        for (int tick = 0; tick < BURST_TAIL_TICKS; tick++) {
            final int delay = BURST_INNER_TICKS + BURST_WAVE_TICKS + tick;
            TickOps.schedule(level, () -> executeTailMist(level, origin, stackCount, delay - BURST_INNER_TICKS - BURST_WAVE_TICKS), delay);
        }
    }

    private static void spawnFogParticles(ServerLevel level, CloudFogManager.FogZone zone, Entity ownerEntity) {
        RandomSource random = level.getRandom();
        Vec3 centre = zone.centre();
        double radius = zone.radius();
        int baseCount = Math.max(FOG_BASE_PARTICLE_COUNT, (int) (radius * 6));
        int glowCount = Math.max(FOG_GLOW_PARTICLE_COUNT, (int) (radius * 3));
        int flowCount = Math.max(FOG_FLOW_PARTICLE_COUNT, (int) (radius * 4));

        double flowAngleRad = Math.toRadians(zone.flowYaw());
        Vec3 flowVector = new Vec3(Math.cos(flowAngleRad), 0.0D, Math.sin(flowAngleRad)).scale(zone.flowSpeed() * 0.08D);
        Vec3 ownerMotion = ownerEntity == null ? Vec3.ZERO : ownerEntity.getDeltaMovement().scale(0.25D);
        Vec3 drift = flowVector.add(ownerMotion);

        for (int i = 0; i < baseCount; i++) {
            double distance = radius * Math.sqrt(random.nextDouble());
            double mix = Math.min(1.0D, distance / Math.max(0.0001D, radius));
            double angle = random.nextDouble() * Mth.TWO_PI;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double x = centre.x + cos * distance;
            double z = centre.z + sin * distance;
            double y = centre.y + (random.nextDouble() - 0.5D) * 0.7D;
            double vx = drift.x + (random.nextDouble() - 0.5D) * 0.02D;
            double vz = drift.z + (random.nextDouble() - 0.5D) * 0.02D;
            double vy = 0.012D - mix * 0.012D + (random.nextDouble() - 0.5D) * 0.01D;
            level.sendParticles(fogParticleOptions((float) mix), x, y, z, 1, vx, vy, vz, 0.0D);
        }

        for (int i = 0; i < glowCount; i++) {
            double distance = radius * (0.3D + random.nextDouble() * 0.7D);
            double angle = random.nextDouble() * Mth.TWO_PI;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double x = centre.x + cos * distance;
            double z = centre.z + sin * distance;
            double y = centre.y + (random.nextDouble() - 0.5D) * 0.4D;
            double vx = drift.x * 0.6D + (random.nextDouble() - 0.5D) * 0.015D;
            double vz = drift.z * 0.6D + (random.nextDouble() - 0.5D) * 0.015D;
            double vy = 0.008D - distance / radius * 0.01D;
            level.sendParticles(CLOUD_GLOW_PARTICLE, x, y, z, 1, vx, vy, vz, 0.0D);
        }

        for (int i = 0; i < flowCount; i++) {
            double distance = radius * (0.6D + random.nextDouble() * 0.4D);
            double angle = random.nextDouble() * Mth.TWO_PI;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double x = centre.x + cos * distance;
            double z = centre.z + sin * distance;
            double y = centre.y + (random.nextDouble() - 0.5D) * 0.6D;
            double vx = drift.x * 0.9D + (random.nextDouble() - 0.5D) * 0.03D;
            double vz = drift.z * 0.9D + (random.nextDouble() - 0.5D) * 0.03D;
            double vy = -0.01D - random.nextDouble() * 0.01D;
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, vx, vy, vz, 0.0D);
        }
    }

    private static void playFogAmbient(ServerLevel level, CloudFogManager.FogZone zone) {
        RandomSource random = level.getRandom();
        Vec3 centre = zone.centre();
        float volume = 0.12f + random.nextFloat() * 0.05f;
        float pitch = 1.35f + random.nextFloat() * 0.2f;
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, SoundSource.AMBIENT, volume, pitch);
    }

    private static void executeInnerPulse(ServerLevel level, Vec3 origin, int stackCount, int tick) {
        RandomSource random = level.getRandom();
        int particles = 18 + stackCount * 2;
        double progress = tick / (double) Math.max(1, BURST_INNER_TICKS - 1);
        double radius = 0.05D + (1.0D - progress) * 0.45D;
        double direction = tick < BURST_INNER_TICKS / 2 ? -1.0D : 1.0D;
        double velocityBase = 0.03D + stackCount * 0.0015D;
        for (int i = 0; i < particles; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double heightJitter = (random.nextDouble() - 0.5D) * 0.2D;
            double x = origin.x + cos * radius;
            double y = origin.y + heightJitter * 0.4D;
            double z = origin.z + sin * radius;
            double speed = velocityBase * (0.8D + random.nextDouble() * 0.4D);
            double vx = cos * speed * direction;
            double vy = 0.015D * direction;
            double vz = sin * speed * direction;
            level.sendParticles(CLOUD_INNER_PULSE, x, y, z, 1, vx, vy, vz, 0.0D);
        }
    }

    private static void executeWavePulse(ServerLevel level, Vec3 origin, int stackCount, int tick) {
        RandomSource random = level.getRandom();
        double progress = (tick + 1) / (double) BURST_WAVE_TICKS;
        double radius = BURST_BASE_RADIUS + stackCount * BURST_RADIUS_PER_STACK + progress * (2.2D + stackCount * BURST_WAVE_GROWTH);
        int angleStep = Math.max(4, 18 - stackCount / 2);
        double upward = 0.012D + progress * 0.02D;
        double alphaScale = 0.2D + progress * 0.5D;
        for (int angleDeg = 0; angleDeg < 360; angleDeg += angleStep) {
            double angle = Math.toRadians(angleDeg);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double jitter = (random.nextDouble() - 0.5D) * 0.05D;
            double x = origin.x + cos * radius + jitter;
            double y = origin.y + Math.sin(progress * Mth.PI) * 0.15D;
            double z = origin.z + sin * radius + jitter;
            double vx = cos * (0.18D + stackCount * 0.005D);
            double vz = sin * (0.18D + stackCount * 0.005D);
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, vx * 0.1D, upward, vz * 0.1D, 0.0D);
            level.sendParticles(CLOUD_GLOW_PARTICLE, x, y, z, 1, vx * 0.05D, upward * 0.6D, vz * 0.05D, alphaScale);
        }
    }

    private static void executeTailMist(ServerLevel level, Vec3 origin, int stackCount, int tick) {
        RandomSource random = level.getRandom();
        double progress = (tick + 1) / (double) BURST_TAIL_TICKS;
        double radius = BURST_BASE_RADIUS + 1.5D + progress * (3.0D + stackCount * BURST_TAIL_GROWTH);
        int particleCount = 12 + stackCount;
        for (int i = 0; i < particleCount; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double distance = radius * (0.7D + random.nextDouble() * 0.3D);
            double x = origin.x + cos * distance;
            double z = origin.z + sin * distance;
            double y = origin.y + random.nextDouble() * 0.4D - 0.2D;
            double downward = -0.01D - random.nextDouble() * 0.015D;
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.0D, downward, 0.0D, 0.0D);
        }
    }

    @EventBusSubscriber(modid = ChestCavity.MODID)
    public static final class CloudFogManager {
        private static final Map<UUID, FogZone> ACTIVE_FOGS = new ConcurrentHashMap<>();

        private CloudFogManager() {
        }

        static void activate(ServerLevel level, LivingEntity owner, Vec3 centre, double radius, int durationTicks) {
            if (level == null || owner == null || centre == null || radius <= 0.0D || durationTicks <= 0) {
                return;
            }
            long now = level.getGameTime();
            RandomSource random = level.getRandom();
            FogZone zone = new FogZone(owner.getUUID(), level.dimension(), centre, radius, now + durationTicks, random, now);
            zone.scheduleNext(now + FOG_EFFECT_INTERVAL);
            zone.scheduleSound(now + FOG_SOUND_INTERVAL_TICKS + random.nextInt(40));
            ACTIVE_FOGS.put(owner.getUUID(), zone);
        }

        static void clear(UUID ownerId) {
            if (ownerId != null) {
                ACTIVE_FOGS.remove(ownerId);
            }
        }

        @SubscribeEvent
        public static void onServerTick(ServerTickEvent.Post event) {
            if (ACTIVE_FOGS.isEmpty()) {
                return;
            }
            var server = event.getServer();
            if (server == null) {
                return;
            }
            List<UUID> toRemove = new ArrayList<>();
            for (Map.Entry<UUID, FogZone> entry : ACTIVE_FOGS.entrySet()) {
                UUID ownerId = entry.getKey();
                FogZone zone = entry.getValue();
                ServerLevel level = server.getLevel(zone.dimension());
                if (level == null) {
                    toRemove.add(ownerId);
                    continue;
                }
                long now = level.getGameTime();
                if (now >= zone.expiresAt()) {
                    toRemove.add(ownerId);
                    continue;
                }
                if (now >= zone.nextEffectTick()) {
                    applyFogEffects(level, zone);
                    zone.scheduleNext(now + FOG_EFFECT_INTERVAL);
                }
                if (now >= zone.nextSoundTick()) {
                    playFogAmbient(level, zone);
                    zone.scheduleSound(now + FOG_SOUND_INTERVAL_TICKS + level.getRandom().nextInt(40));
                }
            }
            for (UUID uuid : toRemove) {
                ACTIVE_FOGS.remove(uuid);
            }
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            if (ACTIVE_FOGS.isEmpty()) {
                return;
            }
            LivingEntity victim = event.getEntity();
            if (victim == null || victim.level().isClientSide()) {
                return;
            }
            Level level = victim.level();
            if (!(level instanceof ServerLevel serverLevel)) {
                return;
            }
            Vec3 position = victim.position();
            double health = Math.max(0.0D, victim.getMaxHealth());
            if (!(health > 0.0D)) {
                return;
            }

            for (FogZone zone : ACTIVE_FOGS.values()) {
                if (!zone.dimension().equals(serverLevel.dimension())) {
                    continue;
                }
                if (!zone.contains(position)) {
                    continue;
                }
                double marks = health * FOG_CONVERSION_RATIO;
                int grant = zone.accumulateAndExtractWholeMarks(marks);
                if (grant > 0) {
                    grantStacks(serverLevel, zone.ownerId(), grant);
                }
            }
        }

        private static void applyFogEffects(ServerLevel level, FogZone zone) {
            AABB area = new AABB(zone.centre(), zone.centre()).inflate(zone.radius());
            Entity ownerEntity = level.getEntity(zone.ownerId());
            spawnFogParticles(level, zone, ownerEntity);
            zone.nudgeFlow(level.getRandom());
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, target ->
                    target.isAlive() && !target.getUUID().equals(zone.ownerId()) && (ownerEntity == null || !target.isAlliedTo(ownerEntity)));
            for (LivingEntity target : targets) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FOG_SLOW_DURATION, FOG_SLOW_AMPLIFIER, false, true, true));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, FOG_WEAKNESS_DURATION, FOG_WEAKNESS_AMPLIFIER, false, true, true));
            }
        }

        private static final class FogZone {
            private final UUID ownerId;
            private final ResourceKey<Level> dimension;
            private final Vec3 centre;
            private final double radius;
            private final long expiresAt;
            private long nextEffectTick;
            private double fractionalMarks;
            private long nextSoundTick;
            private float flowYaw;
            private float flowSpeed;

            FogZone(UUID ownerId, ResourceKey<Level> dimension, Vec3 centre, double radius, long expiresAt, RandomSource random, long currentTime) {
                this.ownerId = ownerId;
                this.dimension = dimension;
                this.centre = centre;
                this.radius = radius;
                this.expiresAt = expiresAt;
                this.nextEffectTick = currentTime;
                this.fractionalMarks = 0.0D;
                this.nextSoundTick = currentTime;
                if (random == null) {
                    random = RandomSource.create();
                }
                this.flowYaw = random.nextFloat() * 360.0f;
                this.flowSpeed = Mth.clamp(0.22f + random.nextFloat() * 0.25f, FOG_FLOW_MIN_SPEED, FOG_FLOW_MAX_SPEED);
            }

            UUID ownerId() {
                return ownerId;
            }

            ResourceKey<Level> dimension() {
                return dimension;
            }

            Vec3 centre() {
                return centre;
            }

            double radius() {
                return radius;
            }

            long expiresAt() {
                return expiresAt;
            }

            long nextEffectTick() {
                return nextEffectTick;
            }

            long nextSoundTick() {
                return nextSoundTick;
            }

            boolean contains(Vec3 point) {
                return point != null && centre.distanceToSqr(point) <= radius * radius;
            }

            void scheduleNext(long tick) {
                nextEffectTick = tick;
            }

            void scheduleSound(long tick) {
                nextSoundTick = tick;
            }

            int accumulateAndExtractWholeMarks(double amount) {
                fractionalMarks += Math.max(0.0D, amount);
                int whole = (int) fractionalMarks;
                fractionalMarks -= whole;
                return whole;
            }

            float flowYaw() {
                return flowYaw;
            }

            float flowSpeed() {
                return flowSpeed;
            }

            void nudgeFlow(RandomSource random) {
                if (random == null) {
                    return;
                }
                flowYaw += random.nextFloat() * FOG_FLOW_JITTER_DEGREES - (FOG_FLOW_JITTER_DEGREES * 0.5f);
                if (flowYaw < 0.0f) {
                    flowYaw += 360.0f;
                } else if (flowYaw >= 360.0f) {
                    flowYaw -= 360.0f;
                }
                float delta = (random.nextFloat() - 0.5f) * 0.06f;
                flowSpeed = Mth.clamp(flowSpeed + delta, FOG_FLOW_MIN_SPEED, FOG_FLOW_MAX_SPEED);
            }
        }
    }
}
