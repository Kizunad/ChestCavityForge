package net.tigereye.chestcavity.compat.guzhenren.item.yu_dao.behavior;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.guzhenren.resource.YuanLaoGuHelper;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * 五转元老蛊：极限容量、恢复与战斗特性。
 */
public final class YuanLaoGuFifthTierBehavior extends AbstractYuanLaoGuBehavior
        implements OrganIncomingDamageListener, OrganOnHitListener {

    public static final YuanLaoGuFifthTierBehavior INSTANCE = new YuanLaoGuFifthTierBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "yuan_lao_gu_5");
    public static final ResourceLocation ABILITY_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "yuan_lao_gu_5_attack");

    private static final double EPS = 1.0e-6;

    private static final double DAMAGE_MAX_STONE_RATIO = 0.01; // 1%
    private static final double DAMAGE_STONES_PER_UNIT = 10.0;
    private static final double DAMAGE_EFFICIENCY = 0.2;
    private static final double DAMAGE_PER_STONE = (DAMAGE_EFFICIENCY / DAMAGE_STONES_PER_UNIT); // 0.02

    private static final double ON_HIT_MAX_STONE_RATIO = 0.0001; // 0.01%
    private static final double ON_HIT_STONES_PER_DAMAGE = 15.0;
    private static final double ON_HIT_EFFICIENCY = 0.15;
    private static final double ON_HIT_DAMAGE_PER_STONE = (ON_HIT_EFFICIENCY / ON_HIT_STONES_PER_DAMAGE); // 0.01

    private static final double ABILITY_MAX_STONE_RATIO = 0.10; // 10%
    private static final double ABILITY_RADIUS_DIVISOR = 10_000.0;
    private static final double ABILITY_DAMAGE_DIVISOR = 100.0;
    private static final double ABILITY_DAMAGE_CAP = 10_000.0;
    private static final Predicate<LivingEntity> ABILITY_TARGET_FILTER = target ->
            target.isAlive() && !target.isInvulnerable() && !(target instanceof Player player && player.isCreative());

    private static final String STATE_ROOT = "YuanLaoGu5";
    private static final String LAST_ABILITY_TICK_KEY = "LastAbilityTick";
    private static final int ABILITY_COOLDOWN_TICKS = 40;

    static {
        OrganActivationListeners.register(ABILITY_ID, YuanLaoGuFifthTierBehavior::activateAbility);
    }

    private YuanLaoGuFifthTierBehavior() {
    }

    @Override
    protected String logPrefix() {
        return "[compat/guzhenren][yu_dao][yuan_lao_gu_5]";
    }

    @Override
    protected boolean matchesOrgan(ItemStack stack) {
        return YuanLaoGuHelper.isFifthTierYuanLaoGu(stack);
    }

    @Override
    protected double zhenyuanPerStone() {
        return 800.0;
    }

    @Override
    protected double stoneRegenPerSlowTick() {
        return 40.0;
    }

    @Override
    protected double configuredStoneCap() {
        return 100_000_000.0;
    }

    @Override
    protected boolean allowAuxiliaryConversion() {
        return true;
    }

    @Override
    protected boolean isAuxiliaryResourceEnabled(AuxiliaryResource resource) {
        return true;
    }

    @Override
    public float onIncomingDamage(
            DamageSource source,
            LivingEntity victim,
            ChestCavityInstance cc,
            ItemStack organ,
            float damage
    ) {
        if (!(victim instanceof Player) || victim.level().isClientSide() || damage <= 0.0f) {
            return damage;
        }
        if (!matchesOrgan(organ)) {
            return damage;
        }
        double cap = resolveCap(organ);
        double maxStones = cap * DAMAGE_MAX_STONE_RATIO;
        if (maxStones <= EPS) {
            return damage;
        }
        double balance = YuanLaoGuHelper.readAmount(organ);
        if (balance <= EPS) {
            return damage;
        }
        double spendable = Math.min(balance, maxStones);
        double stonesNeeded = damage / DAMAGE_PER_STONE;
        double stonesToSpend = Math.min(spendable, stonesNeeded);
        if (stonesToSpend <= EPS) {
            return damage;
        }

        if (!YuanLaoGuHelper.consume(organ, stonesToSpend)) {
            return damage;
        }
        pushOrganUpdate(cc, organ);

        double prevented = stonesToSpend * DAMAGE_PER_STONE;
        double remaining = Math.max(0.0, damage - prevented);

        ChestCavity.LOGGER.debug(
                "{} {} 减免伤害: -{} (消耗元石 {})，剩余 -> {}",
                logPrefix(),
                victim.getScoreboardName(),
                fmt(prevented),
                fmt(stonesToSpend),
                fmt(remaining)
        );
        return (float) remaining;
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
        if (!(attacker instanceof Player) || attacker.level().isClientSide()) {
            return damage;
        }
        if (target == null || !target.isAlive()) {
            return damage;
        }
        if (!matchesOrgan(organ)) {
            return damage;
        }
        if (source == null || source.is(DamageTypeTags.IS_PROJECTILE)) {
            return damage;
        }
        double cap = resolveCap(organ);
        double maxStones = cap * ON_HIT_MAX_STONE_RATIO;
        if (maxStones <= EPS) {
            return damage;
        }
        double balance = YuanLaoGuHelper.readAmount(organ);
        if (balance <= EPS) {
            return damage;
        }
        double stonesToSpend = Math.min(balance, maxStones);
        if (stonesToSpend <= EPS) {
            return damage;
        }
        if (!YuanLaoGuHelper.consume(organ, stonesToSpend)) {
            return damage;
        }
        pushOrganUpdate(cc, organ);

        double bonusDamage = stonesToSpend * ON_HIT_DAMAGE_PER_STONE;
        damage += (float) bonusDamage;

        ChestCavity.LOGGER.debug(
                "{} {} 追加伤害: +{} (消耗元石 {}) -> 总计 {}",
                logPrefix(),
                attacker.getScoreboardName(),
                fmt(bonusDamage),
                fmt(stonesToSpend),
                fmt(damage)
        );
        return damage;
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof ServerPlayer player) || cc == null || player.level().isClientSide()) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }

        OrganState state = OrganState.of(organ, STATE_ROOT);
        long now = player.level().getGameTime();
        long readyAt = state.getLong(LAST_ABILITY_TICK_KEY, 0L);
        if (readyAt > now) {
            return;
        }

        double cap = INSTANCE.resolveCap(organ);
        double balance = YuanLaoGuHelper.readAmount(organ);
        if (balance <= EPS) {
            return;
        }
        double maxSpend = cap * ABILITY_MAX_STONE_RATIO;
        double stonesToSpend = Math.min(balance, maxSpend);
        if (stonesToSpend <= EPS) {
            return;
        }

        if (!YuanLaoGuHelper.consume(organ, stonesToSpend)) {
            return;
        }
        long nextReady = now + ABILITY_COOLDOWN_TICKS;
        OrganStateOps.setLongSync(cc, organ, STATE_ROOT, LAST_ABILITY_TICK_KEY, nextReady, v -> v, now);
        INSTANCE.pushOrganUpdate(cc, organ);

        double radius = Mth.clamp(stonesToSpend / ABILITY_RADIUS_DIVISOR, 2.0, 64.0);
        double damage = Math.min(stonesToSpend / ABILITY_DAMAGE_DIVISOR, ABILITY_DAMAGE_CAP);
        applyAreaDamage(player, radius, (float) damage);
        ChestCavity.LOGGER.debug(
                "{} {} ability radius={} damage={} 消耗={}",
                INSTANCE.logPrefix(),
                player.getScoreboardName(),
                fmt(radius),
                fmt(damage),
                fmt(stonesToSpend)
        );
        ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, nextReady, now);
    }

    private static void applyAreaDamage(ServerPlayer player, double radius, float damage) {
        ServerLevel level = player.serverLevel();
        Vec3 origin = player.position();
        AABB area = new AABB(origin, origin).inflate(radius);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area, entity ->
                entity != player && ABILITY_TARGET_FILTER.test(entity) && !entity.isAlliedTo(player));
        if (victims.isEmpty()) {
            return;
        }

        DamageSource source = player.damageSources().playerAttack(player);
        for (LivingEntity victim : victims) {
            victim.hurt(source, damage);
        }

        level.sendParticles(ParticleTypes.END_ROD, origin.x, origin.y + 0.5, origin.z, 160,
                radius * 0.2, radius * 0.1, radius * 0.2, 0.05);
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 1.0f, 0.6f);
    }

    private static ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (YuanLaoGuHelper.isFifthTierYuanLaoGu(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
