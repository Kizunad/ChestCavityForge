package net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper.ConsumptionResult;


import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.AbstractLiDaoOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.LiDaoConstants;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.LiDaoHelper;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NetworkUtil;
import org.slf4j.Logger;

/**
 * Behaviour implementation for 自力更生蛊（三转）。
 */
public final class ZiLiGengShengGuOrganBehavior extends AbstractLiDaoOrganBehavior implements OrganSlowTickListener, OrganRemovalListener {

    public static final ZiLiGengShengGuOrganBehavior INSTANCE = new ZiLiGengShengGuOrganBehavior();

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "zi_li_geng_sheng_gu_3");
    public static final ResourceLocation ABILITY_ID = ORGAN_ID;

    private static final String STATE_ROOT = "ZiLiGengShengGu";
    private static final String NEXT_PASSIVE_TICK_KEY = "NextPassiveTick";
    private static final String ACTIVE_FLAG_KEY = "AbilityActive";
    private static final String ACTIVE_END_TICK_KEY = "AbilityEndTick";
    private static final String NEXT_REGEN_TICK_KEY = "NextRegenTick";
    private static final String ACTIVE_EFFICIENCY_KEY = "AbilityEfficiency";

    private static final long PASSIVE_INTERVAL_TICKS = 200L; // 10 seconds
    private static final double PASSIVE_HEAL_BASE = 30.0;
    private static final double PASSIVE_ZHENYUAN_COST = 500.0;

    private static final long ABILITY_DURATION_TICKS = 600L; // 30 seconds
    private static final double ABILITY_REGEN_PER_SECOND = 1.0;
    private static final double WEAKNESS_BASE_SECONDS = 30.0;
    private static final long REGEN_STEP_TICKS = 20L;
    private static final double EPSILON = 1.0E-4;

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    static {
        OrganActivationListeners.register(ABILITY_ID, ZiLiGengShengGuOrganBehavior::activateAbility);
    }

    private ZiLiGengShengGuOrganBehavior() {
    }

    public void ensureAttached(ChestCavityInstance cc) {
        ActiveLinkageContext context = linkageContext(cc);
        if (context == null) {
            return;
        }
        LinkageChannel channel = context.getOrCreateChannel(LiDaoConstants.LI_DAO_INCREASE_EFFECT);
        if (channel != null) {
            channel.addPolicy(NON_NEGATIVE);
        }
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide() || cc == null || organ == null || organ.isEmpty()) {
            return;
        }

        OrganState state = organState(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ);
        long gameTime = entity.level().getGameTime();

        tickPassive(entity, cc, organ, state, cooldown, gameTime);
        tickAbility(entity, cc, organ, state, cooldown, gameTime);
    }

    private void tickPassive(LivingEntity entity, ChestCavityInstance cc, ItemStack organ, OrganState state, MultiCooldown cooldown, long gameTime) {
        MultiCooldown.Entry passiveEntry = cooldown.entry(NEXT_PASSIVE_TICK_KEY);
        long nextAllowed = passiveEntry.getReadyTick();
        if (nextAllowed > gameTime) {
            return;
        }

        double efficiency = Math.max(0.0, 1.0 + liDaoIncrease(cc));
        ConsumptionResult payment = ResourceOps.consumeStrict(entity, PASSIVE_ZHENYUAN_COST, 0.0);
        long reschedule = Math.max(gameTime + PASSIVE_INTERVAL_TICKS, gameTime + 1);
        passiveEntry.setReadyAt(reschedule);

        if (!payment.succeeded()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[compat/guzhenren][zi_li_geng_sheng] passive upkeep failed: {}", payment.failureReason());
            }
            return;
        }

        float healAmount = (float) (PASSIVE_HEAL_BASE * Math.max(efficiency, 0.0));
        if (healAmount > EPSILON && entity.isAlive()) {
            entity.heal(healAmount);
        }
    }

    private void tickAbility(LivingEntity entity, ChestCavityInstance cc, ItemStack organ, OrganState state, MultiCooldown cooldown, long gameTime) {
        if (!state.getBoolean(ACTIVE_FLAG_KEY, false)) {
            return;
        }
        double efficiency = Math.max(state.getDouble(ACTIVE_EFFICIENCY_KEY, 1.0), 0.0);
        MultiCooldown.Entry abilityEndEntry = cooldown.entry(ACTIVE_END_TICK_KEY);
        MultiCooldown.Entry regenEntry = cooldown.entry(NEXT_REGEN_TICK_KEY);
        long abilityEnd = abilityEndEntry.getReadyTick();

        if (abilityEnd > 0L && gameTime >= abilityEnd) {
            finishAbility(entity, cc, organ, state, cooldown, efficiency);
            return;
        }

        long nextRegen = regenEntry.getReadyTick();
        if (gameTime >= nextRegen) {
            float healAmount = (float) (ABILITY_REGEN_PER_SECOND * Math.max(efficiency, 0.0));
            if (healAmount > EPSILON && entity.isAlive()) {
                entity.heal(healAmount);
            }
            long newNext = Math.max(gameTime + REGEN_STEP_TICKS, nextRegen + REGEN_STEP_TICKS);
            regenEntry.setReadyAt(newNext);
        }
    }

    private void finishAbility(LivingEntity entity, ChestCavityInstance cc, ItemStack organ, OrganState state, MultiCooldown cooldown, double efficiency) {
        OrganStateOps.setBoolean(state, cc, organ, ACTIVE_FLAG_KEY, false, false);
        cooldown.entry(ACTIVE_END_TICK_KEY).setReadyAt(0L);
        cooldown.entry(NEXT_REGEN_TICK_KEY).setReadyAt(0L);
        OrganStateOps.setDouble(state, cc, organ, ACTIVE_EFFICIENCY_KEY, 0.0, value -> Math.max(0.0, value), 0.0);
        if (entity != null) {
            entity.removeEffect(MobEffects.REGENERATION);
        }

        int weaknessTicks = computeWeaknessDurationTicks(efficiency);
        if (weaknessTicks > 0 && entity != null && entity.isAlive()) {
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, weaknessTicks, 0));
        }
    }

    private int computeWeaknessDurationTicks(double efficiency) {
        double safeEfficiency = Math.max(efficiency, 0.001);
        double seconds = WEAKNESS_BASE_SECONDS / safeEfficiency;
        int ticks = (int) Math.round(seconds * 20.0);
        return Math.max(ticks, 0);
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

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (cc == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }

        OrganState state = INSTANCE.organState(organ, STATE_ROOT);
        long gameTime = player.level().getGameTime();
        MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ);
        MultiCooldown.Entry abilityEndEntry = cooldown.entry(ACTIVE_END_TICK_KEY);
        MultiCooldown.Entry regenEntry = cooldown.entry(NEXT_REGEN_TICK_KEY);

        if (state.getBoolean(ACTIVE_FLAG_KEY, false)) {
            long endTick = abilityEndEntry.getReadyTick();
            if (endTick > 0L && gameTime >= endTick) {
                double efficiency = Math.max(state.getDouble(ACTIVE_EFFICIENCY_KEY, 1.0), 0.0);
                INSTANCE.finishAbility(entity, cc, organ, state, cooldown, efficiency);
            }
            if (state.getBoolean(ACTIVE_FLAG_KEY, false)) {
                return;
            }
        }

        int consumed = INSTANCE.consumeMuscles(player, cc);
        if (consumed <= 0) {
            return;
        }

        double efficiency = Math.max(0.0, 1.0 + INSTANCE.liDaoIncrease(cc));
        OrganStateOps.setBoolean(state, cc, organ, ACTIVE_FLAG_KEY, true, false);
        abilityEndEntry.setReadyAt(gameTime + ABILITY_DURATION_TICKS);
        regenEntry.setReadyAt(gameTime + REGEN_STEP_TICKS);
        OrganStateOps.setDouble(state, cc, organ, ACTIVE_EFFICIENCY_KEY, Math.max(efficiency, 0.0), value -> Math.max(0.0, value), 0.0);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, (int) ABILITY_DURATION_TICKS, 0, false, true, true));
    }

    private int consumeMuscles(LivingEntity entity, ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return 0;
        }
        Level level = entity.level();
        RandomSource random = entity.getRandom();
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (!LiDaoHelper.isMuscle(stack)) {
                continue;
            }
            if (stack.getCount() <= 0) {
                continue;
            }
            cc.inventory.removeItem(i, 1);
            cc.inventory.setChanged();
            float pitch = 0.9f + random.nextFloat() * 0.2f;
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.GENERIC_EAT, entity.getSoundSource(), 0.6f, pitch);
            return 1;
        }
        return 0;
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

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide() || organ == null || organ.isEmpty()) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ);
        double efficiency = Math.max(state.getDouble(ACTIVE_EFFICIENCY_KEY, 1.0), 0.0);
        finishAbility(entity, cc, organ, state, cooldown, efficiency);
        cooldown.entry(NEXT_PASSIVE_TICK_KEY).setReadyAt(0L);
    }
}
