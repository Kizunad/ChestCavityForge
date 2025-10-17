package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.AttributeOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper.ConsumptionResult;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.reaction.ReactionEvents;
import net.tigereye.chestcavity.util.reaction.ReactionRegistry;
import net.tigereye.chestcavity.util.reaction.ReactionStatuses;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper.ConsumptionResult;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper.ConsumptionResult;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

/**
 * 火油蛊：基于燃油层叠与“火衣+油涂层”反应驱动的 Yan Dao 胃脏器官。
 * 当前实现聚焦基础叠层、被动增益与油涂层施加；高阶效果（喷焰、烈源态等）在后续步骤扩展。
 */
public final class HuoYouGuOrganBehavior extends AbstractGuzhenrenOrganBehavior implements OrganSlowTickListener, OrganOnHitListener {

    public static final HuoYouGuOrganBehavior INSTANCE = new HuoYouGuOrganBehavior();

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_you_gu");
    public static final ResourceLocation ABILITY_ID = ORGAN_ID; // “喷焰”在后续步骤接入

    private static final String STATE_ROOT = "HuoYouGu";
    private static final String KEY_FUEL_STACKS = "FuelStacks";
    private static final String KEY_FRP = "FireRefinePoints";
    private static final String KEY_TIER = "Tier";
    private static final String KEY_LAST_FIRE_STACK_TICK = "LastFireStackTick";
    private static final String KEY_LIEYUAN_EXPIRE_TICK = "LieYuanExpireTick";
    private static final String KEY_COOLDOWN_READY_TICK = "SprayReadyTick";

    private static final int MAX_FUEL_STACKS = 10;
    private static final int FUEL_PER_ON_HIT = 1;
    private static final int OIL_COST_STACKS = 3;
    private static final int OIL_BASE_DURATION_TICKS = 120;
    private static final double ATTACK_PERCENT_PER_STACK = 0.02D; // +2% 攻击
    private static final double ZHENYUAN_PER_STACK = 100.0D;
    private static final ResourceLocation FIRE_COAT_ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_gu");
    private static final String FIRE_COAT_STATE_ROOT = "HuoYiGu";
    private static final String FIRE_COAT_ACTIVE_UNTIL_KEY = "ActiveUntil";

    private static final int SPRAY_COOLDOWN_TICKS = 15 * 20;
    private static final double SPRAY_RANGE = 6.0D;
    private static final double SPRAY_CONE_DOT = 0.55D;
    private static final float SPRAY_BASE_DAMAGE = 4.0F;
    private static final float SPRAY_DAMAGE_PER_STACK = 0.35F;
    private static final int SPRAY_FIRE_TICKS = 80;
    private static final int SPRAY_PARTICLE_STEPS = 18;
    private static final int FIRE_COAT_DISABLE_TICKS = 60;

    private static final int FIRE_REFINE_CAP = 1200;
    private static final int[] FIRE_REFINE_REQUIREMENTS = {0, 0, 200, 450, 800};

    private static final int SELF_BREW_ZHENYUAN_COST = 20;
    private static final int SELF_BREW_JINGLI_COST = 1;
    private static final int SELF_BREW_FOOD_COST = 1;
    private static final int SELF_BREW_INTERVAL_TICKS = 40;

    private static final ResourceLocation ATTACK_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("chestcavity", "huo_you_gu/attack_bonus");

    private static final int SLOW_TICK_INTERVAL = 20; // 每秒处理一次资源/提示

    private HuoYouGuOrganBehavior() {}

    static {
        OrganActivationListeners.register(ABILITY_ID, HuoYouGuOrganBehavior::activateAbility);
        ReactionEvents.registerFireOilListener(ctx -> INSTANCE.onFireOilReaction(ctx));
        ReactionEvents.registerFireOilPowerListener(ctx -> INSTANCE.fireOilPowerBonus(ctx));
        NeoForge.EVENT_BUS.addListener(HuoYouGuOrganBehavior::onLivingDeath);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || player.level().isClientSide() || cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }

        OrganState state = organState(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ, state);

        long gameTime = player.level().getGameTime();
        int tier = resolveTier(state);
        int fuelStacks = Mth.clamp(state.getInt(KEY_FUEL_STACKS, 0), 0, MAX_FUEL_STACKS);

        // 烈源态计时（占位，后续扩展）
        MultiCooldown.Entry lieYuanEntry = cooldown.entry(KEY_LIEYUAN_EXPIRE_TICK);
        if (lieYuanEntry.getReadyTick() > 0L && lieYuanEntry.getReadyTick() <= gameTime) {
            lieYuanEntry.setReadyAt(0L);
        }

        OrganStateOps.Collector collector = OrganStateOps.collector(cc, organ);
        if (gameTime % SLOW_TICK_INTERVAL == 0) {
            fuelStacks = applyPerSecondEffects(player, cc, organ, state, fuelStacks, tier, gameTime, collector);
            updateAttackModifier(player, fuelStacks);
        }

        collector.record(state.setInt(KEY_FUEL_STACKS, fuelStacks, value -> Mth.clamp(value, 0, MAX_FUEL_STACKS), 0));
        collector.commit();
    }

    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID) || cc == null || organ == null || organ.isEmpty()) {
            return damage;
        }
        if (target == null || !target.isAlive() || target == attacker || target.isAlliedTo(attacker)) {
            return damage;
        }
        if (source == null || source.getEntity() != attacker) {
            return damage;
        }

        OrganState state = organState(organ, STATE_ROOT);
        int currentStacks = Mth.clamp(state.getInt(KEY_FUEL_STACKS, 0), 0, MAX_FUEL_STACKS);
        OrganStateOps.Collector collector = OrganStateOps.collector(cc, organ);

        int gained = computeFuelGain(player, state, FUEL_PER_ON_HIT);
        int newStacks = Mth.clamp(currentStacks + gained, 0, MAX_FUEL_STACKS);
        collector.record(state.setInt(KEY_FUEL_STACKS, newStacks, value -> Mth.clamp(value, 0, MAX_FUEL_STACKS), 0));
        collector.record(state.setLong(KEY_LAST_FIRE_STACK_TICK, attacker.level().getGameTime(), value -> Math.max(0L, value), 0L));
        currentStacks = newStacks;
        spawnFuelParticles(attacker, gained);

        // 至少 3 层燃油即可在本次命中施加油涂层（允许投射/非投射）
        if (currentStacks >= OIL_COST_STACKS && canApplyOil(player, target, state)) {
            boolean applied = tryApplyOil(player, target, state, currentStacks);
            if (applied) {
                int remaining = Math.max(0, currentStacks - OIL_COST_STACKS);
                collector.record(state.setInt(KEY_FUEL_STACKS, remaining, value -> Mth.clamp(value, 0, MAX_FUEL_STACKS), 0));
                currentStacks = remaining;
            }
        }

        collector.commit();
        updateAttackModifier(player, currentStacks);
        return damage;
    }

    private int applyPerSecondEffects(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, int fuelStacks, int tier, long gameTime, OrganStateOps.Collector collector) {
        int stacks = fuelStacks;
        if (stacks > 0) {
            double replenish = stacks * ZHENYUAN_PER_STACK;
            ResourceOps.tryReplenishScaledZhenyuan(player, replenish, true);

            long lieYuanExpire = state.getLong(KEY_LIEYUAN_EXPIRE_TICK, 0L);
            if (lieYuanExpire > 0L && player.level().getGameTime() < lieYuanExpire) {
                ResourceOps.tryAdjustJingli(player, 10.0, true);
                ResourceOps.tryReplenishScaledZhenyuan(player, 10.0, true);
            }

            if (tier >= 3 && isNether(player.level())) {
                long time = player.level().getGameTime();
                if (time % (20L * 60L) == 0L) {
                    addFireRefinePoints(player, cc, organ, state, 2);
                }
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[huo_you_gu] stacks={} replenish={} tier={}", stacks, replenish, tier);
            }
        }

        if (stacks < MAX_FUEL_STACKS) {
            stacks = maybeSelfBrewFuel(player, cc, organ, state, stacks, gameTime, collector);
        }
        return stacks;
    }

    private void updateAttackModifier(Player player, int stacks) {
        AttributeInstance attack = player == null ? null : player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) {
            return;
        }
        if (stacks <= 0) {
            AttributeOps.removeById(attack, ATTACK_MODIFIER_ID);
            return;
        }
        double bonus = stacks * ATTACK_PERCENT_PER_STACK;
        AttributeModifier modifier = new AttributeModifier(ATTACK_MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        AttributeOps.replaceTransient(attack, ATTACK_MODIFIER_ID, modifier);
    }

    private int computeFuelGain(Player player, OrganState state, int baseGain) {
        double factor = 1.0D;
        int tier = resolveTier(state);
        Level level = player.level();
        if (tier >= 3 && isNether(level)) {
            factor *= 1.5D;
        }
        double scaled = baseGain * factor;
        int gain = (int) Math.floor(scaled);
        if (player.getRandom().nextDouble() < scaled - gain) {
            gain++;
        }
        return Math.max(1, gain);
    }

    private boolean canApplyOil(Player player, LivingEntity target, OrganState state) {
        int tier = resolveTier(state);
        if (tier >= 3 && isDampEnvironment(target)) {
            // 雨天/水下概率减半
            if (player.getRandom().nextDouble() < 0.5D) {
                return false;
            }
        }
        return true;
    }

    private boolean tryApplyOil(Player attacker, LivingEntity target, OrganState state, int currentStacks) {
        if (!(target.level() instanceof ServerLevel server)) {
            return false;
        }
        int tier = resolveTier(state);
        int duration = OIL_BASE_DURATION_TICKS;
        if (tier >= 2 && attacker.getHealth() <= attacker.getMaxHealth() * 0.5F) {
            duration = (int) Math.round(duration * 1.5D);
        }

        ReactionRegistry.addStatus(target, ReactionStatuses.OIL_COATING, duration);
        playOilApplyFx(server, target);
        return true;
    }

    private void playOilApplyFx(ServerLevel level, LivingEntity target) {
        RandomSource random = target.getRandom();
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        for (int i = 0; i < 12; i++) {
            double motionX = (random.nextDouble() - 0.5D) * 0.6D;
            double motionY = random.nextDouble() * 0.2D;
            double motionZ = (random.nextDouble() - 0.5D) * 0.6D;
            level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 1, motionX, motionY, motionZ, 0.02D);
            level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, 1, motionX * 0.8D, motionY * 0.5D, motionZ * 0.8D, 0.01D);
        }
        level.playSound(null, center.x, center.y, center.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6F, 1.1F);
    }

    private void spawnFuelParticles(LivingEntity attacker, int gained) {
        if (!(attacker.level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 center = attacker.position().add(0.0D, attacker.getBbHeight() * 0.6D, 0.0D);
        RandomSource random = attacker.getRandom();
        int count = Math.min(6, 2 + gained * 2);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = attacker.getBbWidth() * 0.35D + random.nextDouble() * 0.15D;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y + random.nextDouble() * 0.3D;
            server.sendParticles(ParticleTypes.SMALL_FLAME, x, y, z, 1, 0.0D, 0.005D, 0.0D, 0.01D);
        }
        server.playSound(null, attacker.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.4F, 1.4F);
    }

    private int resolveTier(OrganState state) {
        int tier = state == null ? 1 : Mth.clamp(state.getInt(KEY_TIER, 1), 1, 4);
        if (state != null && tier != state.getInt(KEY_TIER, 1)) {
            state.setInt(KEY_TIER, tier, value -> Mth.clamp(value, 1, 4), 1);
        }
        return tier;
    }

    private boolean isNether(Level level) {
        return level.dimension() == Level.NETHER;
    }

    private boolean isDampEnvironment(LivingEntity entity) {
        Level level = entity.level();
        if (entity.isInWaterOrBubble() || entity.isUnderWater()) {
            return true;
        }
        BlockPos pos = entity.blockPosition();
        if (level.isRaining()) {
            return level.isRainingAt(pos);
        }
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().isSource();
    }

    private void onFireOilReaction(ReactionRegistry.ReactionContext context) {
        if (!(context.attacker() instanceof Player player)) {
            return;
        }
        Optional<ChestCavityEntity> optional = ChestCavityEntity.of(player);
        if (optional.isEmpty()) {
            return;
        }
        ChestCavityInstance cc = optional.get().getChestCavityInstance();
        if (cc == null || cc.inventory == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        boolean targetDead = context.target() != null && !context.target().isAlive();
        addFireRefinePoints(player, cc, organ, state, 25);
        if (targetDead) {
            handleExplosionKill(player, cc, organ, state);
        }
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (cc == null || cc.inventory == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }

        OrganState state = INSTANCE.organState(organ, STATE_ROOT);
        int tier = INSTANCE.resolveTier(state);
        if (tier < 2) {
            return;
        }

        MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ, state);
        long now = player.level().getGameTime();
        MultiCooldown.Entry readyEntry = cooldown.entry(KEY_COOLDOWN_READY_TICK);
        if (readyEntry.getReadyTick() > now) {
            return;
        }

        int fuelStacks = Mth.clamp(state.getInt(KEY_FUEL_STACKS, 0), 0, MAX_FUEL_STACKS);
        if (fuelStacks <= 0) {
            return;
        }

        if (!(player.level() instanceof ServerLevel server)) {
            return;
        }

        Vec3 origin = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB search = player.getBoundingBox().expandTowards(look.scale(SPRAY_RANGE)).inflate(1.5D);
        List<LivingEntity> candidates = server.getEntitiesOfClass(LivingEntity.class, search,
                target -> target != player && target.isAlive() && !target.isAlliedTo(player));

        boolean fireCoatActive = INSTANCE.isFireCoatActive(player);
        int hits = 0;
        for (LivingEntity target : candidates) {
            Vec3 toTarget = target.getEyePosition().subtract(origin);
            double distance = toTarget.length();
            if (distance <= 0.0001D || distance > SPRAY_RANGE) {
                continue;
            }
            Vec3 direction = toTarget.normalize();
            double dot = direction.dot(look);
            if (dot < SPRAY_CONE_DOT) {
                continue;
            }
            boolean consumed = INSTANCE.handleSprayHit(server, player, target, fuelStacks, fireCoatActive);
            if (consumed) {
                fireCoatActive = false;
            }
            hits++;
        }

        INSTANCE.spawnSprayFx(server, player, look, fuelStacks);

        if (hits >= 3) {
            INSTANCE.addFireRefinePoints(player, cc, organ, state, 15);
        }

        OrganStateOps.Collector collector = OrganStateOps.collector(cc, organ);
        collector.record(state.setInt(KEY_FUEL_STACKS, 0, value -> Mth.clamp(value, 0, MAX_FUEL_STACKS), 0));
        collector.commit();

        long readyAt = now + SPRAY_COOLDOWN_TICKS;
        readyEntry.setReadyAt(readyAt);
        if (player instanceof ServerPlayer sp) {
            ActiveSkillRegistry.scheduleReadyToast(sp, ABILITY_ID, readyAt, now);
        }
    }

    private boolean handleSprayHit(ServerLevel level, Player player, LivingEntity target, int fuelStacks, boolean fireCoatActive) {
        boolean exploded = false;
        boolean hasOil = ReactionRegistry.hasStatus(target, ReactionStatuses.OIL_COATING);
        if (hasOil && fireCoatActive) {
            float power = 1.8F + 0.08F * Math.min(fuelStacks, MAX_FUEL_STACKS);
            ReactionRegistry.clearStatus(target, ReactionStatuses.OIL_COATING);
            level.explode(player, target.getX(), target.getY(), target.getZ(), power, Level.ExplosionInteraction.MOB);
            ReactionRegistry.blockFireOil(player, FIRE_COAT_DISABLE_TICKS);
            exploded = true;
        } else {
            float damage = SPRAY_BASE_DAMAGE + SPRAY_DAMAGE_PER_STACK * fuelStacks;
            target.hurt(player.damageSources().playerAttack(player), damage);
        }

        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), SPRAY_FIRE_TICKS));
        Vec3 push = target.position().subtract(player.position()).normalize().scale(0.25D + fuelStacks * 0.02D);
        target.push(push.x, 0.08D, push.z);
        target.hurtMarked = true;

        return exploded;
    }

    private void spawnSprayFx(ServerLevel level, Player player, Vec3 look, int fuelStacks) {
        Vec3 origin = player.getEyePosition();
        RandomSource random = player.getRandom();
        double step = SPRAY_RANGE / Math.max(1, SPRAY_PARTICLE_STEPS);
        for (int i = 0; i < SPRAY_PARTICLE_STEPS; i++) {
            double distance = step * i;
            Vec3 pos = origin.add(look.scale(distance));
            double spread = 0.35D + fuelStacks * 0.025D;
            for (int j = 0; j < 4; j++) {
                double ox = (random.nextDouble() - 0.5D) * spread;
                double oy = (random.nextDouble() - 0.3D) * 0.4D;
                double oz = (random.nextDouble() - 0.5D) * spread;
                level.sendParticles(ParticleTypes.FLAME, pos.x + ox, pos.y + oy, pos.z + oz, 1, 0.0D, 0.0D, 0.0D, 0.015D);
            }
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 1, 0.0D, 0.01D, 0.0D, 0.0D);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS,
                0.9F, 0.7F + fuelStacks * 0.015F);
    }

    private boolean hasFireCoatOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return false;
        }
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (FIRE_COAT_ORGAN_ID.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFireCoatActive(Player player) {
        if (ReactionRegistry.isFireOilBlocked(player)) {
            return false;
        }
        Optional<ChestCavityEntity> optional = ChestCavityEntity.of(player);
        if (optional.isEmpty()) {
            return false;
        }
        ChestCavityInstance cc = optional.get().getChestCavityInstance();
        if (cc == null || cc.inventory == null) {
            return false;
        }
        long now = player.level().getGameTime();
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!FIRE_COAT_ORGAN_ID.equals(id)) {
                continue;
            }
            OrganState fireState = OrganState.of(stack, FIRE_COAT_STATE_ROOT);
            long activeUntil = fireState.getLong(FIRE_COAT_ACTIVE_UNTIL_KEY, 0L);
            if (activeUntil > now) {
                return true;
            }
        }
        return false;
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
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (ORGAN_ID.equals(id)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ, OrganState state) {
        MultiCooldown.Builder builder = MultiCooldown.builder(state)
                .withLongClamp(value -> Math.max(0L, value), 0L);
        if (cc != null) {
            builder.withSync(cc, organ);
        } else if (organ != null) {
            builder.withOrgan(organ);
        }
        return builder.build();
    }

    private void handleExplosionKill(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state) {
        int tier = resolveTier(state);
        if (tier < 3) {
            return;
        }
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        handleOpt.ifPresent(handle -> {
            double maxZhenyuan = handle.read("zuida_zhenyuan").orElse(0.0D);
            if (maxZhenyuan > 0.0D) {
                ResourceOps.tryReplenishScaledZhenyuan(handle, maxZhenyuan * 0.05D, true);
            }
        });

        int currentStacks = Mth.clamp(state.getInt(KEY_FUEL_STACKS, 0), 0, MAX_FUEL_STACKS);
        if (currentStacks < MAX_FUEL_STACKS) {
            state.setInt(KEY_FUEL_STACKS, Math.min(MAX_FUEL_STACKS, currentStacks + 1), value -> Mth.clamp(value, 0, MAX_FUEL_STACKS), 0);
        }
        sendSlotUpdate(cc, organ);
    }

    private int maybeSelfBrewFuel(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, int currentStacks, long gameTime, OrganStateOps.Collector collector) {
        if (player == null || state == null || currentStacks >= MAX_FUEL_STACKS) {
            return currentStacks;
        }
        long lastTick = state.getLong(KEY_LAST_FIRE_STACK_TICK, 0L);
        if (gameTime - lastTick < SELF_BREW_INTERVAL_TICKS) {
            return currentStacks;
        }

        FoodData food = player.getFoodData();
        if (food == null || food.getFoodLevel() < SELF_BREW_FOOD_COST) {
            return currentStacks;
        }

        ConsumptionResult payment = ResourceOps.consumeStrict(player, SELF_BREW_ZHENYUAN_COST, SELF_BREW_JINGLI_COST);
        if (payment == null || !payment.succeeded()) {
            return currentStacks;
        }

        food.setFoodLevel(Math.max(0, food.getFoodLevel() - SELF_BREW_FOOD_COST));
        if (food.getSaturationLevel() > food.getFoodLevel()) {
            food.setSaturation(food.getFoodLevel());
        }

        int newStacks = Math.min(MAX_FUEL_STACKS, currentStacks + 1);
        collector.record(state.setLong(KEY_LAST_FIRE_STACK_TICK, gameTime, value -> Math.max(0L, value), 0L));
        spawnFuelParticles(player, 1);
        player.level().playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.3F, 1.35F);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[huo_you_gu] self-brew fuel stack -> {}", newStacks);
        }
        return newStacks;
    }

    private void addFireRefinePoints(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, int amount) {
        if (player == null || state == null || amount <= 0) {
            return;
        }
        int current = Math.max(0, state.getInt(KEY_FRP, 0));
        int capped = Mth.clamp(current + amount, 0, FIRE_REFINE_CAP);
        if (capped == current) {
            return;
        }
        state.setInt(KEY_FRP, capped, value -> Mth.clamp(value, 0, FIRE_REFINE_CAP), 0);
        checkTierUpgrade(player, cc, organ, state, capped);
    }

    private void checkTierUpgrade(Player player, ChestCavityInstance cc, ItemStack organ, OrganState state, int frp) {
        int tier = resolveTier(state);
        if (tier >= 4) {
            return;
        }
        int index = Math.min(tier + 1, FIRE_REFINE_REQUIREMENTS.length - 1);
        int required = FIRE_REFINE_REQUIREMENTS[index];
        if (frp < required) {
            return;
        }
        state.setInt(KEY_TIER, tier + 1, value -> Mth.clamp(value, 1, 4), 1);
        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(tierUpMessage(tier + 1), true);
        }
        sendSlotUpdate(cc, organ);
    }

    private Component tierUpMessage(int tier) {
        return switch (tier) {
            case 2 -> Component.literal("烈焰之心在胸中翻涌，火油开始自炼！");
            case 3 -> Component.literal("真元灼心，油焰凝形，火油蛊晋入新阶！");
            case 4 -> Component.literal("烈源成形，油焰化作烈源之火！");
            default -> Component.literal("火油蛊晋入第" + tier + "阶");
        };
    }

    private double fireOilPowerBonus(ReactionRegistry.ReactionContext context) {
        if (!(context.attacker() instanceof Player player)) {
            return 0.0D;
        }
        Optional<ChestCavityEntity> optional = ChestCavityEntity.of(player);
        if (optional.isEmpty()) {
            return 0.0D;
        }
        ChestCavityInstance cc = optional.get().getChestCavityInstance();
        if (cc == null || cc.inventory == null) {
            return 0.0D;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return 0.0D;
        }
        OrganState state = organState(organ, STATE_ROOT);
        if (resolveTier(state) < 4 || !hasFireCoatOrgan(cc)) {
            return 0.0D;
        }
        return 1.0D;
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        Optional<ChestCavityEntity> optional = ChestCavityEntity.of(player);
        if (optional.isEmpty()) {
            return;
        }
        ChestCavityInstance cc = optional.get().getChestCavityInstance();
        if (cc == null || cc.inventory == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        OrganState state = INSTANCE.organState(organ, STATE_ROOT);
        int stacks = Math.max(0, state.getInt(KEY_FUEL_STACKS, 0));
        if (stacks < 10) {
            return;
        }
        if (!(player.level() instanceof ServerLevel server)) {
            return;
        }
        server.explode(player, player.getX(), player.getY(), player.getZ(), 2.4F, Level.ExplosionInteraction.MOB);
        state.setInt(KEY_FUEL_STACKS, 0, value -> Mth.clamp(value, 0, MAX_FUEL_STACKS), 0);
        INSTANCE.sendSlotUpdate(cc, organ);
    }

}
