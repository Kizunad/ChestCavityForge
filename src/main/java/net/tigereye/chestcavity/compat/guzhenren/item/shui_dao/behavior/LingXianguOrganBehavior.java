package net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.behavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;


import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper.ConsumptionResult;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import org.joml.Vector3f;


/**
 * Behaviour for 灵涎蛊 (Ling Xian Gu).
 *
 * Healing cadence:
 * - Players: attempts a gentle mend every 30s when injured, consuming 30 base zhenyuan per organ.
 * - Emergency response (\"应激分泌\"): when health falls below 30% the organ attempts a stronger burst
 *   costing 60 base zhenyuan per organ and granting Weakness I for 5s.
 * - Non-player entities do not spend resources but operate on doubled cooldowns and receive Weakness III
 *   on emergency discharges to reflect the harsher backlash.
 *
 * Visual/audio cues favour cool aqua droplets with a soft mist. Player activations glow brighter and pulse
 * more frequently than the muted non-player variant.
 */
public enum LingXianguOrganBehavior implements OrganSlowTickListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation SHUI_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/shui_dao_increase_effect");
    private static final ResourceLocation XUE_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/xue_dao_increase_effect");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final String STATE_KEY = "LingXiangu";
    private static final String NORMAL_COOLDOWN_KEY = "NormalCooldown";
    private static final String STRESS_COOLDOWN_KEY = "StressCooldown";
    private static final String NORMAL_READY_AT_KEY = "NormalReadyAt";
    private static final String STRESS_READY_AT_KEY = "StressReadyAt";

    private static final int PLAYER_INTERVAL_SECONDS = BehaviorConfigAccess.getInt(LingXianguOrganBehavior.class, "PLAYER_INTERVAL_SECONDS", 30);
    private static final int NON_PLAYER_INTERVAL_SECONDS = BehaviorConfigAccess.getInt(LingXianguOrganBehavior.class, "NON_PLAYER_INTERVAL_SECONDS", PLAYER_INTERVAL_SECONDS * 2);

    private static final double BASE_NORMAL_ZHENYUAN_COST = 30.0;
    private static final double BASE_STRESS_ZHENYUAN_COST = 60.0;

    private static final float BASE_NORMAL_HEAL = BehaviorConfigAccess.getFloat(LingXianguOrganBehavior.class, "BASE_NORMAL_HEAL", 10.0f);
    private static final float BASE_STRESS_HEAL = BehaviorConfigAccess.getFloat(LingXianguOrganBehavior.class, "BASE_STRESS_HEAL", 20.0f);

    private static final float STRESS_THRESHOLD_RATIO = BehaviorConfigAccess.getFloat(LingXianguOrganBehavior.class, "STRESS_THRESHOLD_RATIO", 0.30f);
    private static final int WEAKNESS_DURATION_TICKS = BehaviorConfigAccess.getInt(LingXianguOrganBehavior.class, "WEAKNESS_DURATION_TICKS", 5 * 20);
    private static final int PLAYER_STRESS_AMPLIFIER = BehaviorConfigAccess.getInt(LingXianguOrganBehavior.class, "PLAYER_STRESS_AMPLIFIER", 0);
    private static final int NON_PLAYER_STRESS_AMPLIFIER = BehaviorConfigAccess.getInt(LingXianguOrganBehavior.class, "NON_PLAYER_STRESS_AMPLIFIER", 2);

    private static final DustParticleOptions PLAYER_GLOW =
            new DustParticleOptions(new Vector3f(90f / 255f, 210f / 255f, 215f / 255f), 1.0f);
    private static final DustParticleOptions NON_PLAYER_GLOW =
            new DustParticleOptions(new Vector3f(60f / 255f, 150f / 255f, 170f / 255f), 0.6f);

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (!entity.isAlive() || organ == null || organ.isEmpty()) {
            return;
        }

        Player player = entity instanceof Player ? (Player) entity : null;
        int normalInterval = getNormalIntervalSeconds(player);
        int stressInterval = getStressIntervalSeconds(player);

        OrganState state = OrganState.of(organ, STATE_KEY);
        MultiCooldown cooldown = MultiCooldown.builder(state)
                .withSync(cc, organ)
                .withIntClamp(value -> Math.max(0, value), 0)
                .build();

        // Switch to readyAt + onReady chain for both tracks
        scheduleBaselineIfNeeded(entity, cc, organ, cooldown, normalInterval);
        scheduleStressIfNeeded(entity, cc, organ, cooldown, stressInterval);
    }

    /** Ensures linkage channels exist for downstream consumers. */
    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        LinkageChannel shuiDao = context.getOrCreateChannel(SHUI_DAO_INCREASE_EFFECT);
        shuiDao.addPolicy(NON_NEGATIVE);
        LinkageChannel xueDao = context.getOrCreateChannel(XUE_DAO_INCREASE_EFFECT);
        xueDao.addPolicy(NON_NEGATIVE);
    }

    private static void scheduleBaselineIfNeeded(LivingEntity entity, ChestCavityInstance cc, ItemStack organ, MultiCooldown cooldown, int intervalSeconds) {
        if (!(entity.level() instanceof ServerLevel server)) return;
        long now = server.getGameTime();
        MultiCooldown.Entry ready = cooldown.entry(NORMAL_READY_AT_KEY);
        if (ready.getReadyTick() <= now) {
            // randomize first trigger within interval
            int offset = entity.getRandom().nextInt(intervalSeconds + 1);
            ready.setReadyAt(now + offset);
        }
        ready.onReady(server, now, () -> {
            try {
                Player player = entity instanceof Player ? (Player) entity : null;
                if (shouldTriggerBaseline(entity)) {
                    if (attemptBaselineHeal(entity, organ, player)) {
                        // success: re-arm next tick
                        long nextAt = server.getGameTime() + intervalSeconds;
                        MultiCooldown.Entry e = cooldown.entry(NORMAL_READY_AT_KEY);
                        e.setReadyAt(nextAt);
                        e.onReady(server, server.getGameTime(), () -> {});
                        return;
                    }
                }
                // if not triggered due to guard, retry shortly (1s) to check again
                MultiCooldown.Entry e = cooldown.entry(NORMAL_READY_AT_KEY);
                e.setReadyAt(server.getGameTime() + 20);
                e.onReady(server, server.getGameTime(), () -> {});
            } catch (Throwable ignored) {}
        });
    }

    private static void scheduleStressIfNeeded(LivingEntity entity, ChestCavityInstance cc, ItemStack organ, MultiCooldown cooldown, int intervalSeconds) {
        if (!(entity.level() instanceof ServerLevel server)) return;
        long now = server.getGameTime();
        MultiCooldown.Entry ready = cooldown.entry(STRESS_READY_AT_KEY);
        if (ready.getReadyTick() <= now) {
            int offset = entity.getRandom().nextInt(intervalSeconds + 1);
            ready.setReadyAt(now + offset);
        }
        ready.onReady(server, now, () -> {
            try {
                Player player = entity instanceof Player ? (Player) entity : null;
                if (shouldTriggerStress(entity)) {
                    if (attemptStressResponse(entity, organ, player)) {
                        long nextAt = server.getGameTime() + intervalSeconds;
                        MultiCooldown.Entry e = cooldown.entry(STRESS_READY_AT_KEY);
                        e.setReadyAt(nextAt);
                        e.onReady(server, server.getGameTime(), () -> {});
                        return;
                    }
                }
                // Retry check in 1s until guard satisfied
                MultiCooldown.Entry e = cooldown.entry(STRESS_READY_AT_KEY);
                e.setReadyAt(server.getGameTime() + 20);
                e.onReady(server, server.getGameTime(), () -> {});
            } catch (Throwable ignored) {}
        });
    }

    private static boolean attemptBaselineHeal(LivingEntity entity, ItemStack organ, Player player) {
        int stackCount = Math.max(1, organ.getCount());
        float healAmount = BASE_NORMAL_HEAL * stackCount;
        if (!canHeal(entity, healAmount)) {
            return false;
        }
        ConsumptionResult payment = null;
        if (player != null) {
            double cost = BASE_NORMAL_ZHENYUAN_COST * stackCount;
            payment = ResourceOps.consumeStrict(player, cost, 0.0);
            if (!payment.succeeded()) {
                return false;
            }
        }
        float healed = applyHealing(entity, healAmount);
        if (healed <= 0.0f) {
            if (player != null && payment != null) {
                ResourceOps.refund(player, payment);
            }
            return false;
        }
        boolean isPlayer = player != null;
        spawnHealingParticles(entity, stackCount, isPlayer, false);
        playHealingSound(entity, isPlayer, false);
        ReactionTagOps.add(entity, ReactionTagKeys.WATER_VEIL, 100);
        return true;
    }

    private static boolean attemptStressResponse(LivingEntity entity, ItemStack organ, Player player) {
        int stackCount = Math.max(1, organ.getCount());
        float healAmount = BASE_STRESS_HEAL * stackCount;
        if (!canHeal(entity, healAmount)) {
            return false;
        }
        ConsumptionResult payment = null;
        if (player != null) {
            double cost = BASE_STRESS_ZHENYUAN_COST * stackCount;
            payment = ResourceOps.consumeStrict(player, cost, 0.0);
            if (!payment.succeeded()) {
                return false;
            }
        }
        float healed = applyHealing(entity, healAmount);
        if (healed <= 0.0f) {
            if (player != null && payment != null) {
                ResourceOps.refund(player, payment);
            }
            return false;
        }
        boolean isPlayer = player != null;
        spawnHealingParticles(entity, stackCount, isPlayer, true);
        playHealingSound(entity, isPlayer, true);
        applyWeakness(entity, isPlayer);
        ReactionTagOps.add(entity, ReactionTagKeys.WATER_VEIL, 140);
        return true;
    }

    private static boolean shouldTriggerBaseline(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return entity.getHealth() < entity.getMaxHealth();
    }

    private static boolean shouldTriggerStress(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        float max = entity.getMaxHealth();
        if (max <= 0.0f) {
            return false;
        }
        float ratio = entity.getHealth() / max;
        return ratio < STRESS_THRESHOLD_RATIO;
    }

    private static float applyHealing(LivingEntity entity, float amount) {
        float before = entity.getHealth();
        entity.heal(amount);
        return entity.getHealth() - before;
    }

    private static boolean canHeal(LivingEntity entity, float amount) {
        if (entity == null || amount <= 0.0f) {
            return false;
        }
        return entity.getHealth() < entity.getMaxHealth();
    }

    private static void applyWeakness(LivingEntity entity, boolean isPlayer) {
        int amplifier = isPlayer ? PLAYER_STRESS_AMPLIFIER : NON_PLAYER_STRESS_AMPLIFIER;
        MobEffectInstance effect = new MobEffectInstance(
                MobEffects.WEAKNESS,
                WEAKNESS_DURATION_TICKS,
                amplifier,
                false,
                true,
                true
        );
        entity.addEffect(effect);
    }

    private static void spawnHealingParticles(LivingEntity entity, int stackCount, boolean isPlayer, boolean stress) {
        Level level = entity.level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        RandomSource random = entity.getRandom();
        DustParticleOptions glow = isPlayer ? PLAYER_GLOW : NON_PLAYER_GLOW;
        int glowBursts = (isPlayer ? 6 : 3) * stackCount;
        if (stress) {
            glowBursts *= 2;
        }
        Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
        double radius = 0.35 + 0.1 * stackCount;
        for (int i = 0; i < glowBursts; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = radius * (0.35 + random.nextDouble() * 0.65);
            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;
            double offsetY = (random.nextDouble() - 0.5) * 0.4;
            server.sendParticles(glow, center.x + offsetX, center.y + offsetY, center.z + offsetZ,
                    1, 0.02, 0.02, 0.02, 0.01);
        }
        int dropletCount = (isPlayer ? 12 : 6) * stackCount;
        if (stress) {
            dropletCount += (isPlayer ? 8 : 4) * stackCount;
        }
        server.sendParticles(
                stress ? ParticleTypes.SPLASH : ParticleTypes.DRIPPING_DRIPSTONE_WATER,
                center.x,
                center.y,
                center.z,
                dropletCount,
                0.25,
                0.35,
                0.25,
                stress ? 0.04 : 0.02
        );
        server.sendParticles(
                ParticleTypes.CLOUD,
                center.x,
                center.y + 0.1,
                center.z,
                Mth.clamp(stackCount * (stress ? 6 : 3), 3, 24),
                0.25,
                0.15,
                0.25,
                0.01
        );
    }

    private static void playHealingSound(LivingEntity entity, boolean isPlayer, boolean stress) {
        Level level = entity.level();
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        float dropletPitch = isPlayer ? 1.05f : 0.9f;
        if (stress) {
            dropletPitch += 0.1f;
        }
        level.playSound(null, x, y, z, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.PLAYERS,
                isPlayer ? 0.7f : 0.5f, dropletPitch);
        level.playSound(null, x, y, z, SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS,
                stress ? 0.8f : 0.6f, isPlayer ? 1.2f : 0.95f);
    }

    private static int getNormalIntervalSeconds(Player player) {
        return player != null ? PLAYER_INTERVAL_SECONDS : NON_PLAYER_INTERVAL_SECONDS;
    }

    private static int getStressIntervalSeconds(Player player) {
        return player != null ? PLAYER_INTERVAL_SECONDS : NON_PLAYER_INTERVAL_SECONDS;
    }


}
