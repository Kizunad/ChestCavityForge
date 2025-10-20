package net.tigereye.chestcavity.compat.guzhenren.item.yun_dao_cloud.behavior;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.engine.dot.DoTEngine;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * 阴云蛊（云道·肌肉）行为实现。
 */
public final class YinYunGuOrganBehavior extends AbstractGuzhenrenOrganBehavior implements OrganSlowTickListener, OrganOnHitListener {

    public static final YinYunGuOrganBehavior INSTANCE = new YinYunGuOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "yin_yun_gu");
    public static final ResourceLocation ABILITY_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "yin_yun_gu");

    private static final String STATE_ROOT = "YinYunGu";
    private static final String KEY_STACKS = "Stacks";
    private static final String KEY_STACK_TIMER = "StackTimer";
    private static final String KEY_READY_TICK = "NextReadyTick";

    private static final double HEAL_PER_SECOND = 2.0D;
    private static final double JINGLI_DRAIN_PER_SECOND = 2.0D;
    private static final double JINGLI_DRAIN_RADIUS = 2.0D;
    private static final int STACK_INTERVAL_SLOW_TICKS = 6;
    private static final int MAX_STACKS = 20;
    private static final double LIFESTEAL_PER_STACK = 0.01D;

    private static final double DOT_DAMAGE_PER_SECOND = 2.0D;
    private static final int DOT_DURATION_SECONDS = 10;

    private static final int STORM_COOLDOWN_TICKS = 20 * 20;
    private static final int STORM_DELAY_TICKS = 20;
    private static final double STORM_RADIUS = 6.0D;
    private static final double STORM_PULL_FORCE = 0.5D;
    private static final double STORM_UPWARD_FORCE = 0.6D;
    private static final int STACKS_PER_LIGHTNING = 5;
    private static final float STORM_DAMAGE_PER_STACK = 2.0F;
    private static final int STORM_WEAKNESS_DURATION = 60;
    private static final int STORM_WEAKNESS_AMPLIFIER = 1;
    private static final int STORM_SLOW_FALL_DURATION = 20;
    private static final int PRE_PULL_ITERATIONS = 5;
    private static final int PRE_PULL_INTERVAL_TICKS = 4;

    private static final DustParticleOptions GLOOM_TRAIL =
            new DustParticleOptions(new Vector3f(60f / 255f, 50f / 255f, 80f / 255f), 0.8f);
    private static final DustColorTransitionOptions CORRUPTION_FLASH =
            new DustColorTransitionOptions(
                    new Vector3f(70f / 255f, 40f / 255f, 90f / 255f),
                    new Vector3f(156f / 255f, 40f / 255f, 110f / 255f),
                    0.75f
            );

    static {
        OrganActivationListeners.register(ABILITY_ID, YinYunGuOrganBehavior::activateAbility);
    }

    private YinYunGuOrganBehavior() {}

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide() || !entity.isAlive() || cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }

        ServerLevel level = entity.level() instanceof ServerLevel s ? s : null;
        entity.heal((float) HEAL_PER_SECOND);
        ReactionTagOps.add(entity, ReactionTagKeys.CLOUD_SHROUD, 80);

        drainNearbyJingli(entity);

        OrganState state = organState(organ, STATE_ROOT);
        int stacks = Mth.clamp(state.getInt(KEY_STACKS, 0), 0, MAX_STACKS);
        int timer = Mth.clamp(state.getInt(KEY_STACK_TIMER, 0), 0, STACK_INTERVAL_SLOW_TICKS - 1);

        timer++;
        if (timer >= STACK_INTERVAL_SLOW_TICKS) {
            timer = 0;
            if (stacks < MAX_STACKS) {
                stacks++;
            }
        }

        boolean dirty = false;
        dirty |= state.setInt(KEY_STACKS, stacks, value -> Mth.clamp(value, 0, MAX_STACKS), 0).changed();
        dirty |= state.setInt(KEY_STACK_TIMER, timer, value -> Mth.clamp(value, 0, STACK_INTERVAL_SLOW_TICKS - 1), 0).changed();

        if (level != null) {
            spawnPassiveParticles(level, entity, stacks);
        }

        if (dirty) {
            sendSlotUpdate(cc, organ);
        }
    }

    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (attacker == null || attacker.level().isClientSide()) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID) || cc == null || organ == null || organ.isEmpty()) {
            return damage;
        }
        if (damage <= 0.0F || target == null || !target.isAlive() || target == attacker || target.isAlliedTo(attacker)) {
            return damage;
        }
        if (source == null || source.getDirectEntity() != attacker || source.is(DamageTypeTags.IS_PROJECTILE)) {
            return damage;
        }

        OrganState state = organState(organ, STATE_ROOT);
        int stacks = Mth.clamp(state.getInt(KEY_STACKS, 0), 0, MAX_STACKS);
        if (stacks <= 0) {
            return damage;
        }

        scheduleDot(attacker, target);
        applyLifesteal(attacker, damage, stacks);
        return damage;
    }

    private void drainNearbyJingli(LivingEntity holder) {
        Level level = holder.level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        AABB area = holder.getBoundingBox().inflate(JINGLI_DRAIN_RADIUS);
        Predicate<Player> predicate = player -> player != null && player.isAlive() && player != holder && player.distanceTo(holder) <= JINGLI_DRAIN_RADIUS;
        List<Player> victims = server.getEntitiesOfClass(Player.class, area, predicate);

        if (!victims.isEmpty()) {
            for (Player player : victims) {
                ResourceOps.tryAdjustJingli(player, -JINGLI_DRAIN_PER_SECOND, true);
            }
        } else if (holder instanceof Player player) {
            ResourceOps.tryAdjustJingli(player, -JINGLI_DRAIN_PER_SECOND, true);
        }
    }

    private void spawnPassiveParticles(ServerLevel level, LivingEntity entity, int stacks) {
        RandomSource random = entity.getRandom();
        double baseX = entity.getX();
        double baseY = entity.getY();
        double baseZ = entity.getZ();
        double height = entity.getBbHeight();
        double width = Math.max(0.4D, entity.getBbWidth());
        int combatWindow = 100;
        boolean inCombat = entity.getLastHurtMobTimestamp() + combatWindow > entity.tickCount
                || entity.getLastHurtByMobTimestamp() + combatWindow > entity.tickCount;
        int trails = Math.max(2, stacks / 2);
        double swirl = inCombat ? 0.12D : 0.06D;
        for (int i = 0; i < trails; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = width * 0.35D + random.nextDouble() * width * 0.15D;
            double x = baseX + Math.cos(angle) * radius;
            double z = baseZ + Math.sin(angle) * radius;
            double y = baseY + random.nextDouble() * height * 0.8D;
            double dx = -Math.cos(angle) * swirl + (random.nextDouble() - 0.5D) * 0.04D;
            double dy = -0.02D - random.nextDouble() * 0.04D - (inCombat ? 0.02D : 0.0D);
            double dz = -Math.sin(angle) * swirl + (random.nextDouble() - 0.5D) * 0.04D;
            level.sendParticles(GLOOM_TRAIL, x, y, z, 1, dx, dy, dz, 0.0D);
        }
        if (stacks >= 10) {
            double flashY = baseY + height * 0.7D;
            level.sendParticles(CORRUPTION_FLASH, baseX, flashY, baseZ, 6, width * 0.1D, 0.05D, width * 0.1D, 0.01D);
        }
    }

    private void scheduleDot(LivingEntity attacker, LivingEntity target) {
        if (attacker == null || target == null) {
            return;
        }
        // 标记腐蚀，用于触发“腐蚀激增/火×腐蚀”反应
        try {
            int mark = net.tigereye.chestcavity.ChestCavity.config != null
                    ? Math.max(40, net.tigereye.chestcavity.ChestCavity.config.REACTION.corrosionMarkDurationTicks)
                    : 160;
            net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.add(target,
                    net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys.CORROSION_MARK,
                    mark);
        } catch (Throwable ignored) {}
        DoTEngine.schedulePerSecond(attacker, target, DOT_DAMAGE_PER_SECOND, DOT_DURATION_SECONDS, null, 1.0f, 1.0f,
                net.tigereye.chestcavity.util.DoTTypes.YIN_YUN_CORROSION,
                null, net.tigereye.chestcavity.engine.dot.DoTEngine.FxAnchor.TARGET, net.minecraft.world.phys.Vec3.ZERO, 1.0f);
    }

    private void applyLifesteal(LivingEntity attacker, float damage, int stacks) {
        if (attacker == null || damage <= 0.0F || stacks <= 0) {
            return;
        }
        float ratio = (float) (stacks * LIFESTEAL_PER_STACK);
        if (ratio <= 0.0F) {
            return;
        }
        float heal = damage * ratio;
        if (heal > 0.0F) {
            attacker.heal(heal);
            ReactionTagOps.add(attacker, ReactionTagKeys.CLOUD_SHROUD, 60);
        }
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (cc == null) {
            return;
        }
        ItemStack organ = findPrimaryOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }

        OrganState state = OrganState.of(organ, STATE_ROOT);
        int stacks = Mth.clamp(state.getInt(KEY_STACKS, 0), 0, MAX_STACKS);
        if (stacks <= 0) {
            return;
        }

        MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ);
        long now = player.level().getGameTime();
        MultiCooldown.Entry readyEntry = cooldown.entry(KEY_READY_TICK);
        if (readyEntry.getReadyTick() > now) {
            return;
        }

        List<UUID> targetIds = pullTargets(player);
        if (targetIds.isEmpty()) {
            targetIds = Collections.singletonList(player.getUUID());
        }

        int lightningCount = Math.max(1, (stacks + STACKS_PER_LIGHTNING - 1) / STACKS_PER_LIGHTNING);
        int consumedStacks = stacks;
        state.setInt(KEY_STACKS, 0, value -> 0, 0);
        state.setInt(KEY_STACK_TIMER, 0, value -> 0, 0);
        INSTANCE.sendSlotUpdate(cc, organ);

        readyEntry.setReadyAt(now + STORM_COOLDOWN_TICKS);
        long readyAt = readyEntry.getReadyTick();

        if (player instanceof ServerPlayer sp) {
            ActiveSkillRegistry.scheduleReadyToast(sp, ABILITY_ID, readyAt, now);
        }

        ServerLevel level = (ServerLevel) player.level();
        BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.fromNamespaceAndPath("chestcavity", "custom.wind.storm"))
                .ifPresent(sound -> level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.1f, 1.0f));
        schedulePrePulls(level, player);
        scheduleStorm(level, player, targetIds, lightningCount, consumedStacks);
    }

    private static void schedulePrePulls(ServerLevel level, Player caster) {
        if (level == null || caster == null) {
            return;
        }
        for (int i = 0; i < PRE_PULL_ITERATIONS; i++) {
            int delay = i * PRE_PULL_INTERVAL_TICKS;
            TickOps.schedule(level, () -> {
                if (caster.isAlive()) {
                    pullTargets(caster);
                }
            }, delay);
        }
    }

    private static void scheduleStorm(ServerLevel level,
                                      Player caster,
                                      List<UUID> targetIds,
                                      int lightningCount,
                                      int consumedStacks) {
        if (level == null || caster == null) {
            return;
        }
        TickOps.schedule(level, () -> {
            if (!caster.isAlive()) {
                return;
            }
            strikeLightning(level, caster, targetIds, lightningCount);
            applyStormDamage(level, caster, targetIds, consumedStacks);
        }, STORM_DELAY_TICKS);
    }

    private static void strikeLightning(ServerLevel level,
                                        Player caster,
                                        List<UUID> targetIds,
                                        int lightningCount) {
        if (lightningCount <= 0) {
            return;
        }
        for (int i = 0; i < lightningCount; i++) {
            LivingEntity anchor = findTarget(level, targetIds, i);
            Vec3 pos = anchor != null ? anchor.position() : caster.position();
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt == null) {
                continue;
            }
            bolt.moveTo(pos.x, pos.y, pos.z);
            if (caster instanceof ServerPlayer sp) {
                bolt.setCause(sp);
            }
            level.addFreshEntity(bolt);
        }
    }

    private static void applyStormDamage(ServerLevel level,
                                         Player caster,
                                         List<UUID> targetIds,
                                         int consumedStacks) {
        float damage = Math.max(0.0F, consumedStacks * STORM_DAMAGE_PER_STACK);
        if (damage <= 0.0F) {
            return;
        }
        for (UUID id : targetIds) {
            LivingEntity target = findTarget(level, id);
            if (target == null || !target.isAlive() || target == caster) {
                continue;
            }
            if (target.isAlliedTo(caster)) {
                continue;
            }
            DamageSource source = caster.damageSources().lightningBolt();
            target.hurt(source, damage);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, STORM_WEAKNESS_DURATION, STORM_WEAKNESS_AMPLIFIER));
            target.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, STORM_SLOW_FALL_DURATION, 0));
        }
    }

    private static LivingEntity findTarget(ServerLevel level, List<UUID> ids, int index) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        UUID id = ids.get(index % ids.size());
        return findTarget(level, id);
    }

    private static LivingEntity findTarget(ServerLevel level, UUID id) {
        if (level == null || id == null) {
            return null;
        }
        Entity entity = level.getEntity(id);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static List<UUID> pullTargets(Player player) {
        Level level = player.level();
        if (!(level instanceof ServerLevel server)) {
            return Collections.emptyList();
        }
        List<LivingEntity> targets = server.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(STORM_RADIUS),
                entity -> entity != player && entity.isAlive() && !entity.isAlliedTo(player)
        );
        if (targets.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> ids = new ArrayList<>(targets.size());
        double baseAngle = player.tickCount * 0.1D;
        for (LivingEntity target : targets) {
            Vec3 pull = player.position().subtract(target.position()).normalize().scale(STORM_PULL_FORCE);
            Vec3 velocity = target.getDeltaMovement().add(pull.x, STORM_UPWARD_FORCE, pull.z);
            double angle = baseAngle + target.getId() * 0.3D;
            double swirl = 0.2D;
            velocity = velocity.add(Math.cos(angle) * swirl, 0.0D, Math.sin(angle) * swirl);
            target.setDeltaMovement(velocity);
            target.hurtMarked = true;
            target.hasImpulse = true;
            spawnVortexParticles(server, target.position());
            level.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.6f, 0.8f + server.random.nextFloat() * 0.4f);
            ids.add(target.getUUID());
        }
        return ids;
    }

    private static void spawnVortexParticles(ServerLevel level, Vec3 pos) {
        RandomSource random = level.random;
        double radius = 0.6D;
        for (int i = 0; i < 10; i++) {
            double angle = (random.nextDouble() * Math.PI * 2.0D);
            double x = pos.x + Math.cos(angle) * radius;
            double z = pos.z + Math.sin(angle) * radius;
            double y = pos.y + random.nextDouble() * 1.2D;
            double dx = -Math.sin(angle) * 0.2D;
            double dz = Math.cos(angle) * 0.2D;
            level.sendParticles(GLOOM_TRAIL, x, y, z, 1, dx, 0.05D, dz, 0.0D);
        }
    }

    private MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ) {
        MultiCooldown.Builder builder = MultiCooldown.builder(OrganState.of(organ, STATE_ROOT))
                .withLongClamp(value -> Math.max(0L, value), 0L);
        if (cc != null) {
            builder.withSync(cc, organ);
        } else {
            builder.withOrgan(organ);
        }
        return builder.build();
    }

    private static ItemStack findPrimaryOrgan(ChestCavityInstance cc) {
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
}
