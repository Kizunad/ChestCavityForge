package net.tigereye.chestcavity.compat.guzhenren.item.shi_dao.behavior;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.compat.guzhenren.linkage.GuzhenrenLinkageManager;
import net.tigereye.chestcavity.compat.guzhenren.linkage.LinkageChannel;
import net.tigereye.chestcavity.compat.guzhenren.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.compat.guzhenren.util.GuzhenrenCombatUtil;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.registration.CCDamageSources;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Behaviour for 酒虫. Manages the alcohol resource, drunken buffs, active breath skill and overcharge.
 */
public enum JiuChongOrganBehavior implements OrganSlowTickListener, OrganOnHitListener, OrganIncomingDamageListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "jiu_chong");
    private static final ResourceLocation ALCOHOL_CHANNEL = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/jiu_chong_alcohol");

    private static final double MAX_ALCOHOL = 100.0;
    private static final double ALCOHOL_PER_FEED = 1.0;
    private static final double HUNGER_COST = 0.1;
    private static final double DRUNK_THRESHOLD = 20.0;
    private static final double ATTACK_BONUS = 1;
    private static final float BASE_DODGE_CHANCE = 0.1f;
    private static final float MIN_DODGE_DISTANCE = 0.5f;
    private static final float MAX_DODGE_DISTANCE = 1.0f;
    private static final float DODGE_YAW_RANGE = 60.0f;
    private static final float ATTACK_YAW_RANGE = 15.0f;
    private static final float ATTACK_PITCH_RANGE = 5.0f;

    private static final double BREATH_COST = 30.0;
    private static final double REGEN_COST = 10.0;
    private static final float REGEN_HEAL_AMOUNT = 2.0f;
    private static final long REGEN_INTERVAL_TICKS = 40L;

    private static final float HEALTH_EPSILON = 1.0E-4f;

    private static final long MANIA_DURATION_TICKS = 20L * 20L;
    private static final float MANIA_DAMAGE_MULTIPLIER = 2.0f;

    private static final ClampPolicy ALCOHOL_CLAMP = new ClampPolicy(0.0, MAX_ALCOHOL);

    private static final Map<UUID, Long> MANIA_EXPIRY = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_REGEN_TICK = new ConcurrentHashMap<>();

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (!hasOrgan(cc)) {
            return;
        }
        LinkageChannel channel = ensureAlcoholChannel(cc);
        if (entity instanceof Player player) {
            handlePlayer(player, organ, channel);
            return;
        }
        handleNonPlayer(entity, organ, channel);
    }

    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (attacker == null || attacker.level().isClientSide() || cc == null) {
            return damage;
        }
        LinkageChannel channel = ensureAlcoholChannel(cc);
        double alcohol = channel.get();
        double multiplier = 1.0;
        if (alcohol >= DRUNK_THRESHOLD) {
            multiplier += ATTACK_BONUS;
            GuzhenrenCombatUtil.applyRandomAttackOffset(attacker, ATTACK_YAW_RANGE, ATTACK_PITCH_RANGE, alcohol / MAX_ALCOHOL);
        }
        if (attacker instanceof Player player) {
            if (isManiaActive(player, player.level().getGameTime())) {
                multiplier *= MANIA_DAMAGE_MULTIPLIER;
            }
        }
        return (float) (damage * multiplier);
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (victim == null || victim.level().isClientSide() || cc == null) {
            return damage;
        }
        LinkageChannel channel = ensureAlcoholChannel(cc);
        double alcohol = channel.get();
        boolean dodged = false;
        if (alcohol >= DRUNK_THRESHOLD) {
            float stacks = Math.max(1, organ.getCount());
            float chance = Math.min(1.0f, BASE_DODGE_CHANCE * stacks);
            if (victim.getRandom().nextFloat() < chance) {
                LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
                dodged = GuzhenrenCombatUtil.performShortDodge(
                        victim,
                        attacker,
                        MIN_DODGE_DISTANCE,
                        MAX_DODGE_DISTANCE,
                        DODGE_YAW_RANGE,
                        SoundEvents.SLIME_JUMP,
                        ParticleTypes.CLOUD
                );
            }
        }
        if (dodged) {
            return 0.0f;
        }
        double multiplier = 1.0;
        if (victim instanceof Player player && isManiaActive(player, player.level().getGameTime())) {
            multiplier *= MANIA_DAMAGE_MULTIPLIER;
        }
        // 非玩家在受击时有 1/10 概率释放随机吐息（需酒精充足，否则不触发，不扣血）。
        if (!(victim instanceof Player)) {
            if (alcohol >= BREATH_COST) {
                RandomSource random = victim.getRandom();
                if (random.nextInt(10) == 0) {
                    channel.adjust(-BREATH_COST);
                    performDrunkenBreath(victim);
                }
            }
        }
        return (float) (damage * multiplier);
    }

    public void ensureAttached(ChestCavityInstance cc) {
        ensureAlcoholChannel(cc);
    }

    private static LinkageChannel ensureAlcoholChannel(ChestCavityInstance cc) {
        ActiveLinkageContext context = GuzhenrenLinkageManager.getContext(cc);
        return context.getOrCreateChannel(ALCOHOL_CHANNEL).addPolicy(ALCOHOL_CLAMP);
    }

    private static int convertHungerToAlcohol(Player player, int attempts) {
        FoodData foodData = player.getFoodData();
        int successful = 0;
        for (int i = 0; i < attempts; i++) {
            if (tryConsumeHalf(foodData)) {
                successful++;
            } else {
                break;
            }
        }
        return successful;
    }

    private static boolean tryConsumeHalf(FoodData foodData) {
        float saturation = foodData.getSaturationLevel();
        if (saturation > 0.0f) {
            float updated = Math.max(0.0f, saturation - (float) HUNGER_COST);
            foodData.setSaturation(Math.min(updated, foodData.getFoodLevel()));
            return true;
        }
        int hunger = foodData.getFoodLevel();
        if (hunger > 0) {
            foodData.setFoodLevel(Math.max(0, hunger - 1));
            return true;
        }
        return false;
    }

    private static void tryDrunkenRegeneration(Player player, LinkageChannel channel, long gameTime) {
        if (player.getHealth() > player.getMaxHealth() * 0.3f) {
            return;
        }
        if (channel.get() < REGEN_COST) {
            return;
        }
        long last = LAST_REGEN_TICK.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        if (gameTime - last < REGEN_INTERVAL_TICKS) {
            return;
        }
        // Consume from alcohol channel instead of HP for players
        channel.adjust(-REGEN_COST);
        player.heal(REGEN_HEAL_AMOUNT);
        Level level = player.level();
        RandomSource random = player.getRandom();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_HURT_DROWN, SoundSource.PLAYERS, 0.7f, 0.8f + random.nextFloat() * 0.4f);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + player.getBbHeight() * 0.6, player.getZ(), 8, 0.2, 0.2, 0.2, 0.01);
        }
        LAST_REGEN_TICK.put(player.getUUID(), gameTime);
    }

    private static void triggerMania(Player player, long gameTime) {
        long endTick = gameTime + MANIA_DURATION_TICKS;
        MANIA_EXPIRY.put(player.getUUID(), endTick);
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, (int) MANIA_DURATION_TICKS, 0, false, true, true));
        Level level = player.level();
        SoundSource category = SoundSource.PLAYERS;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITCH_AMBIENT, category, 1.0f, 1.0f);
    }

    private static void handleManiaLoop(Player player, long gameTime) {
        if (!isManiaActive(player, gameTime)) {
            return;
        }
        RandomSource random = player.getRandom();
        if (random.nextFloat() < 0.25f) {
            Level level = player.level();
            SoundSource category = SoundSource.PLAYERS;
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    random.nextBoolean() ? SoundEvents.VILLAGER_NO : SoundEvents.WITCH_AMBIENT,
                    category,
                    0.9f,
                    0.8f + random.nextFloat() * 0.4f
            );
        }
    }

    private static boolean isManiaActive(Player player, long gameTime) {
        Long expiry = MANIA_EXPIRY.get(player.getUUID());
        if (expiry == null) {
            return false;
        }
        if (expiry <= gameTime) {
            MANIA_EXPIRY.remove(player.getUUID());
            return false;
        }
        return true;
    }

    private static void performDrunkenBreath(LivingEntity user) {
        if (user == null) {
            return;
        }
        Level level = user.level();
        level.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.8f, 1.2f);
        level.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.8f, 0.8f);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, user.getX(), user.getY() + 0.8, user.getZ(), 12, 0.4, 0.3, 0.4, 0.02);
            server.sendParticles(ParticleTypes.DRAGON_BREATH, user.getX(), user.getY() + 0.6, user.getZ(), 16, 0.5, 0.2, 0.5, 0.01);
        }
        AABB area = user.getBoundingBox().inflate(4.0);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area, target -> target != user && target.isAlive());
        for (LivingEntity target : victims) {
            if (target.isAlliedTo(user)) {
                continue;
            }
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 400, 0, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 400, 0, false, true, true));
        }
        user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1000, 3, false, true, true));
        user.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1000, 0, false, true, true));
    }

    private static void handlePlayer(Player player, ItemStack organ, LinkageChannel channel) {
        long gameTime = player.level().getGameTime();
        double previousAlcohol = channel.get();
        int stackCount = organ == null ? 1 : Math.max(1, organ.getCount());
        int gained = convertHungerToAlcohol(player, stackCount);
        if (gained > 0) {
            double newAlcohol = channel.adjust(ALCOHOL_PER_FEED * gained);
            if (previousAlcohol < MAX_ALCOHOL && newAlcohol >= MAX_ALCOHOL) {
                triggerMania(player, gameTime);
            } else if (newAlcohol >= MAX_ALCOHOL && isManiaActive(player, gameTime)) {
                player.hurt(CCDamageSources.alcoholOverdose(player), gained);
            }
        }
        handleManiaLoop(player, gameTime);
        tryDrunkenRegeneration(player, channel, gameTime);
        // 玩家不触发随机吐息（主动技仅限玩家手动触发；随机吐息是生物替代方案）
    }

    private static void handleNonPlayer(LivingEntity entity, ItemStack organ, LinkageChannel channel) {
        // 随机吐息仅在 OnIncomingDamage 中判定；这里仅以 0.25 效率缓慢恢复酒精。
        if (entity == null || channel == null || !entity.isAlive()) {
            return;
        }
        int stacks = organ == null ? 1 : Math.max(1, organ.getCount());
        double recovered = ALCOHOL_PER_FEED * stacks * 0.25;
        if (recovered > 0) {
            channel.adjust(recovered);
        }
    }

    private static void tryRandomBreath(LivingEntity entity, LinkageChannel channel) {
        if (entity == null || channel == null) {
            return;
        }
        if (channel.get() < BREATH_COST) {
            return;
        }
        RandomSource random = entity.getRandom();
        if (random.nextInt(10) != 0) {
            return;
        }
        // Spend alcohol resource; non-player alcohol is topped up via HP earlier if needed
        channel.adjust(-BREATH_COST);
        performDrunkenBreath(entity);
    }

    private static boolean tryConsumeHealth(LivingEntity entity, double resourceCost) {
        if (entity == null) {
            return false;
        }
        float healthCost = (float) (resourceCost / 100.0);
        if (!Float.isFinite(healthCost) || healthCost <= HEALTH_EPSILON) {
            return true;
        }

        float currentHealth = entity.getHealth();
        float absorption = Math.max(0.0f, entity.getAbsorptionAmount());
        float available = currentHealth + absorption;
        if (available - healthCost <= HEALTH_EPSILON) {
            return false;
        }

        float targetAbsorption = Math.max(0.0f, absorption - healthCost);
        float targetHealth = currentHealth;
        if (healthCost > absorption) {
            float remaining = healthCost - absorption;
            targetHealth = Math.max(0.0f, currentHealth - remaining);
        }

        entity.invulnerableTime = 0;
        entity.hurt(entity.damageSources().generic(), healthCost);
        entity.invulnerableTime = 0;

        if (!entity.isDeadOrDying()) {
            entity.setAbsorptionAmount(targetAbsorption);
            if (entity.getHealth() > targetHealth) {
                entity.setHealth(targetHealth);
            }
            entity.hurtTime = 0;
            entity.hurtDuration = 0;
        }

        return true;
    }

    private static boolean hasOrgan(ChestCavityInstance cc) {
        if (cc == null) {
            return false;
        }
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (ORGAN_ID.equals(id)) {
                return true;
            }
        }
        return false;
    }
}
