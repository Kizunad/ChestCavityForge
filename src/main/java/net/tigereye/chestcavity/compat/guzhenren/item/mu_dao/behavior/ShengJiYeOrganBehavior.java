package net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.tigereye.chestcavity.compat.guzhenren.util.GuzhenrenFlowTooltipResolver;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

import java.util.Locale;
import java.util.Optional;

/**
 * 生机叶（木道器官）被动行为：
 * <ul>
 *     <li>每秒恢复 1 点生命与 100 点真元。</li>
 *     <li>濒死共鸣（生命低于 25%）时触发 5 秒恢复倍率 ×3，冷却 30 秒。</li>
 *     <li>草地 / 森林环境额外 +20% 效率，沙漠 / 下界减半。</li>
 *     <li>同腔存在木道器官时，真元回复效率再 +10%（灵根反馈）。</li>
 *     <li>伴随淡绿色脉冲粒子与柔和音效反馈。</li>
 * </ul>
 */
public final class ShengJiYeOrganBehavior extends AbstractGuzhenrenOrganBehavior implements OrganSlowTickListener {

    public static final ShengJiYeOrganBehavior INSTANCE = new ShengJiYeOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "sheng_ji_xie");

    private static final String STATE_ROOT = "ShengJiYe";
    private static final String KEY_RESONANCE_READY = "ResonanceReadyAt";
    private static final String KEY_RESONANCE_ACTIVE_UNTIL = "ResonanceActiveUntil";
    private static final String KEY_ABILITY_READY = "AbilityReadyAt";

    private static final double BASE_HEAL_PER_SECOND = 1.0D;
    private static final double BASE_ZHENYUAN_PER_SECOND = 100.0D;

    private static final double RESONANCE_HEALTH_THRESHOLD = 0.25D;
    private static final int RESONANCE_DURATION_TICKS = 5 * 20;
    private static final int RESONANCE_COOLDOWN_TICKS = 30 * 20;
    private static final double RESONANCE_MULTIPLIER = 3.0D;

    private static final double ENVIRONMENT_BONUS = 0.20D;
    private static final double ENVIRONMENT_PENALTY = 0.50D;

    private static final String FLOW_KEY_MU_DAO = "木道";
    private static final double LINGGEN_ZHENYUAN_BONUS = 0.10D;

    private static final ResourceLocation ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "sheng_ji_xie_burst");
    private static final int ABILITY_COOLDOWN_TICKS = 25 * 20;
    private static final double ABILITY_ZHENYUAN_COST = 120.0D;
    private static final double ABILITY_JINGLI_COST = 10.0D;
    private static final double ABILITY_SELF_HEAL = 6.0D;
    private static final double ABILITY_ALLY_HEAL = 4.0D;
    private static final int ABILITY_RADIUS = 4;
    private static final int ABILITY_RESIST_DURATION = 6 * 20;
    private static final int ABILITY_RESIST_AMPLIFIER = 0;
    private static final int ABILITY_REGEN_DURATION = 6 * 20;
    private static final int ABILITY_REGEN_AMPLIFIER = 0;

    private static final DustColorTransitionOptions BASE_PARTICLE =
            new DustColorTransitionOptions(new Vec3(0.52D, 0.82D, 0.52D).toVector3f(),
                    new Vec3(0.25D, 0.58D, 0.38D).toVector3f(), 1.0F);

    static {
        OrganActivationListeners.register(ABILITY_ID, ShengJiYeOrganBehavior::activateAbility);
    }

    private ShengJiYeOrganBehavior() {
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel) || level.isClientSide() || !player.isAlive()) {
            return;
        }
        if (!INSTANCE.matchesOrgan(organ, ORGAN_ID)) {
            return;
        }

        long now = serverLevel.getGameTime();
        OrganState state = INSTANCE.organState(organ, STATE_ROOT);
        OrganStateOps.Collector collector = OrganStateOps.collector(cc, organ);

        long readyAt = Math.max(0L, state.getLong(KEY_RESONANCE_READY, 0L));
        long activeUntil = Math.max(0L, state.getLong(KEY_RESONANCE_ACTIVE_UNTIL, 0L));
        boolean active = activeUntil > now;

        if (!active && readyAt <= now && shouldTriggerResonance(player)) {
            active = true;
            activeUntil = now + RESONANCE_DURATION_TICKS;
            readyAt = now + RESONANCE_COOLDOWN_TICKS;
            collector.record(state.setLong(KEY_RESONANCE_ACTIVE_UNTIL, activeUntil, value -> Math.max(0L, value), 0L));
            collector.record(state.setLong(KEY_RESONANCE_READY, readyAt, value -> Math.max(0L, value), 0L));
            spawnResonanceBurst(serverLevel, player);
        } else {
            collector.record(state.setLong(KEY_RESONANCE_ACTIVE_UNTIL, activeUntil, value -> Math.max(0L, value), 0L));
            collector.record(state.setLong(KEY_RESONANCE_READY, readyAt, value -> Math.max(0L, value), 0L));
        }

        double healAmount = BASE_HEAL_PER_SECOND;
        double zhenyuanAmount = BASE_ZHENYUAN_PER_SECOND;
        if (active) {
            healAmount *= RESONANCE_MULTIPLIER;
            zhenyuanAmount *= RESONANCE_MULTIPLIER;
        }

        double environmentMultiplier = environmentMultiplier(player);
        healAmount *= environmentMultiplier;
        zhenyuanAmount *= environmentMultiplier;

        if (hasMuDaoFlow(cc, serverLevel)) {
            zhenyuanAmount *= (1.0D + LINGGEN_ZHENYUAN_BONUS);
        }

        if (healAmount > 0.0D) {
            player.heal((float) healAmount);
        }

        if (zhenyuanAmount > 0.0D) {
            Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
            final double replenish = zhenyuanAmount;
            handleOpt.ifPresent(handle -> ResourceOps.tryReplenishScaledZhenyuan(handle, replenish, true));
        }

        spawnAmbientParticles(serverLevel, player, active);
        collector.commit();
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof ServerPlayer player) || cc == null || entity.level().isClientSide()) {
            return;
        }
        if (!player.isAlive()) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        OrganState state = INSTANCE.organState(organ, STATE_ROOT);
        MultiCooldown cooldown = INSTANCE.cooldown(cc, organ, state);
        MultiCooldown.Entry abilityEntry = cooldown.entry(KEY_ABILITY_READY);
        if (!abilityEntry.isReady(now)) {
            return;
        }
        var consumption = ResourceOps.consumeStrict(player, ABILITY_ZHENYUAN_COST, ABILITY_JINGLI_COST);
        if (!consumption.succeeded()) {
            return;
        }

        double multiplier = environmentMultiplier(player);
        double selfHeal = ABILITY_SELF_HEAL * multiplier;
        if (selfHeal > 0.0D) {
            player.heal((float) selfHeal);
        }
        double allyHeal = ABILITY_ALLY_HEAL * multiplier;
        applySupportEffects(level, player, allyHeal, ABILITY_RESIST_DURATION, ABILITY_RESIST_AMPLIFIER, ABILITY_REGEN_DURATION, ABILITY_REGEN_AMPLIFIER, ABILITY_RADIUS);

        long resonanceUntil = now + RESONANCE_DURATION_TICKS;
        OrganStateOps.setLong(state, cc, organ, KEY_RESONANCE_ACTIVE_UNTIL, resonanceUntil, value -> Math.max(0L, value), 0L);
        OrganStateOps.setLong(state, cc, organ, KEY_RESONANCE_READY, now + RESONANCE_COOLDOWN_TICKS, value -> Math.max(0L, value), 0L);
        spawnResonanceBurst(level, player);

        abilityEntry.setReadyAt(now + ABILITY_COOLDOWN_TICKS);
        ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, abilityEntry.getReadyTick(), now);
    }

    private MultiCooldown cooldown(ChestCavityInstance cc, ItemStack organ, OrganState state) {
        return MultiCooldown.builder(state)
                .withSync(cc, organ)
                .withLongClamp(value -> Math.max(0L, value), 0L)
                .build();
    }

    private static ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack candidate = cc.inventory.getItem(i);
            if (!candidate.isEmpty() && INSTANCE.matchesOrgan(candidate, ORGAN_ID)) {
                return candidate;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void applySupportEffects(ServerLevel level, ServerPlayer caster, double allyHeal, int resistDuration, int resistAmplifier, int regenDuration, int regenAmplifier, int radius) {
        AABB area = caster.getBoundingBox().inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, other -> isFriendly(caster, other))) {
            boolean self = target == caster;
            if (!self && allyHeal > 0.0D) {
                target.heal((float) allyHeal);
            }
            if (resistDuration > 0 && resistAmplifier >= 0) {
                target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, resistDuration, resistAmplifier, false, true, true));
            }
            if (regenDuration > 0 && regenAmplifier >= 0) {
                target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenDuration, regenAmplifier, false, true, true));
            }
            level.sendParticles(BASE_PARTICLE, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 6, 0.25D, 0.2D, 0.25D, 0.005D);
        }
    }

    private static boolean isFriendly(Player owner, LivingEntity candidate) {
        if (candidate == null || !candidate.isAlive()) {
            return false;
        }
        if (candidate == owner) {
            return true;
        }
        if (candidate instanceof Player other) {
            return other == owner || other.isAlliedTo(owner);
        }
        return false;
    }

    private static boolean shouldTriggerResonance(Player player) {
        float max = player.getMaxHealth();
        if (!(max > 0.0F)) {
            return false;
        }
        float ratio = player.getHealth() / max;
        return ratio <= RESONANCE_HEALTH_THRESHOLD;
    }

    private static void spawnResonanceBurst(ServerLevel level, Player player) {
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9F, 1.1F);
        level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 0.6D, player.getZ(), 12,
                0.3D, 0.4D, 0.3D, 0.01D);
    }

    private static void spawnAmbientParticles(ServerLevel level, Player player, boolean resonanceActive) {
        int count = resonanceActive ? 10 : 5;
        double radius = resonanceActive ? 0.6D : 0.4D;
        level.sendParticles(BASE_PARTICLE,
                player.getX(),
                player.getY() + player.getBbHeight() * 0.5D,
                player.getZ(),
                count,
                radius, radius * 0.6D, radius,
                0.005D);
        if (resonanceActive) {
            level.playSound(null, player.blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.PLAYERS, 0.35F, 1.3F);
        }
    }

    static boolean hasMuDaoFlow(ChestCavityInstance cc, ServerLevel level) {
        if (cc == null || cc.inventory == null) {
            return false;
        }
        TooltipContext context = TooltipContext.of(level);
        TooltipFlag flag = TooltipFlag.NORMAL;
        for (int i = 0, size = cc.inventory.getContainerSize(); i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            GuzhenrenFlowTooltipResolver.FlowInfo info = GuzhenrenFlowTooltipResolver.inspect(stack, context, flag, null);
            if (!info.hasFlow()) {
                continue;
            }
            for (String flow : info.flows()) {
                if (flow != null && flow.toLowerCase(Locale.ROOT).contains(FLOW_KEY_MU_DAO)) {
                    return true;
                }
            }
        }
        return false;
    }

    static double environmentMultiplier(Player player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        boolean warmPenalty = isDesertOrNether(level, pos);
        if (warmPenalty) {
            return ENVIRONMENT_PENALTY;
        }
        boolean lushBonus = isGrassOrForest(level, pos);
        return lushBonus ? (1.0D + ENVIRONMENT_BONUS) : 1.0D;
    }

    private static boolean isGrassOrForest(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        boolean standingOnGrass = level.getBlockState(below).is(Blocks.GRASS_BLOCK);
        boolean forestBiome = level.getBiome(pos).is(BiomeTags.IS_FOREST);
        return standingOnGrass || forestBiome;
    }

    private static boolean isDesertOrNether(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        if (level.getBlockState(below).is(BlockTags.SAND)) {
            return true;
        }
        if (level.getBiome(pos).is(BiomeTags.IS_NETHER)) {
            return true;
        }
        return level.dimension() == Level.NETHER;
    }
}
