package net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * 九叶生机草（木道器官）多阶段成长、寄生与枯萎逻辑。
 *
 * <p>要点：
 * <ul>
 *     <li>维持饱食度与精力供给以避免寄生吸血与枯萎。</li>
 *     <li>根据玩家修炼境界（转数）与真元充盈度逐级进化。</li>
 *     <li>草地 / 森林环境提供 20% 回复加成，沙漠 / 下界减半。</li>
 *     <li>同腔存在木道器官时，真元回复效率再 +10%（灵根反馈）。</li>
 *     <li>寄生状态会吸血补给自身；连续匮乏 5 秒将导致退化或沉眠。</li>
 *     <li>持续供养 5 秒可唤醒沉眠形态，恢复至 I 阶。</li>
 * </ul>
 * </p>
 */
public final class JiuYeShengJiCaoOrganBehavior extends AbstractGuzhenrenOrganBehavior implements OrganSlowTickListener {

    public static final JiuYeShengJiCaoOrganBehavior INSTANCE = new JiuYeShengJiCaoOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "jiu_xie_sheng_ji_cao");

    private static final String STATE_ROOT = "JiuYeShengJiCao";
    private static final String KEY_TIER = "Tier";
    private static final String KEY_WITHERED = "Withered";
    private static final String KEY_HUNGER_DEBT = "HungerDebt";
    private static final String KEY_FEED_TIMER = "FeedTimer";
    private static final String KEY_WITHER_TIMER = "WitherTimer";
    private static final String KEY_REVIVE_TIMER = "ReviveTimer";
    private static final String KEY_PARASITIC = "Parasitic";
    private static final String KEY_SUCCESSFUL_CASTS = "SuccessfulCasts";
    private static final String KEY_ABILITY_READY = "AbilityReadyAt";

    private static final double LINGGEN_BONUS = 0.10D;
    private static final double PARASITIC_DAMAGE = 1.0D;
    private static final int WITHER_THRESHOLD_TICKS = 5 * 20;
    private static final int REVIVE_REQUIRED_TICKS = 5 * 20;
    private static final int MAX_TIMER_GUARD = 60 * 20;
    private static final int REQUIRED_CONSECUTIVE_CASTS_FOR_TIER_THREE = 3;

    private static final ResourceLocation ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "jiu_xie_sheng_ji_cao_cui_sheng");

    private static final int[] ABILITY_COOLDOWN_TICKS = { 30 * 20, 24 * 20, 22 * 20 };
    private static final double[] ABILITY_ZHENYUAN_COST = { 160.0D, 220.0D, 300.0D };
    private static final double[] ABILITY_JINGLI_COST = { 10.0D, 18.0D, 26.0D };
    private static final double[] ABILITY_SELF_HEAL = { 8.0D, 10.0D, 14.0D };
    private static final double[] ABILITY_ALLY_HEAL = { 4.0D, 6.0D, 8.0D };
    private static final double[] ABILITY_ABSORPTION = { 0.0D, 2.0D, 6.0D };
    private static final int[] ABILITY_RADIUS = { 4, 5, 6 };
    private static final int[] ABILITY_RESIST_DURATION = { 0, 6 * 20, 8 * 20 };
    private static final int[] ABILITY_RESIST_AMPLIFIER = { -1, 0, 1 }; // -1 = skip effect

    private static final DustColorTransitionOptions PASSIVE_PARTICLE =
            new DustColorTransitionOptions(new Vec3(0.40D, 0.85D, 0.50D).toVector3f(),
                    new Vec3(0.18D, 0.55D, 0.35D).toVector3f(), 1.0F);
    private static final DustColorTransitionOptions PARASITIC_PARTICLE =
            new DustColorTransitionOptions(new Vec3(0.52D, 0.15D, 0.15D).toVector3f(),
                    new Vec3(0.20D, 0.05D, 0.05D).toVector3f(), 1.0F);

    private static final Map<Integer, TierSettings> SETTINGS_BY_TIER = new java.util.HashMap<>();

    static {
        // 阶段 I —— 九叶初成
        // 参数说明（依序）：
        // tier（阶段）=1；
        // hungerCost（每秒饱食度消耗）=0.5；
        // energyCost（每秒精力消耗）=0；
        // healPerSecond（每秒治疗量）=1；
        // zhenyuanPerSecond（每秒真元回复，当前为“直接加值”路径）=3.0；
        // requiredZhuanshu（晋升下一阶段所需“转数”）=0；
        // requiredZhenyuanRatio（晋升下一阶段所需真元比例阈值）=0.0；
        // stabilityWindowTicks（稳定供养窗口长度，单位tick）=10s；
        registerTier(new TierSettings(
                1,
                0.5D,
                0.5D,
                1.0D,
                0.8D,
                0,
                0.0D,
                10 * 20
        ));

        // 阶段 II —— 灵根稳固
        // tier=2；hungerCost=1.0；energyCost=1.0；heal=3.0；zhenyuan=3.3；
        // requiredZhuanshu=2（需≥二转）；requiredZhenyuanRatio=0.50（50%）；
        // 稳定窗口=12s（持续满足供养/条件一段时间后可晋级）。
        registerTier(new TierSettings(
                2,
                4.0D,
                4.0D,
                6.0D,
                1D,
                2,
                0.50D,
                12 * 20
        ));

        // 阶段 III —— 生命母树
        // tier=3；hungerCost=2.0；energyCost=3.0；heal=4.0；zhenyuan=3.8；
        // requiredZhuanshu=3（需≥三转）；requiredZhenyuanRatio=0.70（70%）；
        // 稳定窗口=15s（且需连续成功施放“催生”3次，见逻辑中 successfulCasts 判定）。
        registerTier(new TierSettings(
                3,
                8.0D,
                15.0D,
                10.0D,
                2.0D,
                3,
                0.70D,
                15 * 20
        ));

        OrganActivationListeners.register(ABILITY_ID, JiuYeShengJiCaoOrganBehavior::activateAbility);
    }

    // 将阶段配置放入映射，供运行时按阶段读取数值。
    private static void registerTier(TierSettings settings) {
        SETTINGS_BY_TIER.put(settings.tier(), settings);
    }

    private JiuYeShengJiCaoOrganBehavior() {
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

        OrganState state = INSTANCE.organState(organ, STATE_ROOT);
        OrganStateOps.Collector collector = OrganStateOps.collector(cc, organ);
        Optional<ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);

        int tier = Mth.clamp(state.getInt(KEY_TIER, 1), 1, 3);
        boolean withered = state.getBoolean(KEY_WITHERED, false);
        double hungerDebt = clampDebt(state.getDouble(KEY_HUNGER_DEBT, 0.0D));
        int feedTimer = Mth.clamp(state.getInt(KEY_FEED_TIMER, 0), 0, MAX_TIMER_GUARD);
        int witherTimer = Mth.clamp(state.getInt(KEY_WITHER_TIMER, 0), 0, MAX_TIMER_GUARD);
        int reviveTimer = Mth.clamp(state.getInt(KEY_REVIVE_TIMER, 0), 0, MAX_TIMER_GUARD);
        int successfulCasts = Mth.clamp(state.getInt(KEY_SUCCESSFUL_CASTS, 0), 0, REQUIRED_CONSECUTIVE_CASTS_FOR_TIER_THREE);

        collector.record(state.setInt(KEY_TIER, tier, value -> Mth.clamp(value, 1, 3), 1));
        collector.record(state.setBoolean(KEY_WITHERED, withered, false));
        collector.record(state.setDouble(KEY_HUNGER_DEBT, hungerDebt, JiuYeShengJiCaoOrganBehavior::clampDebt, 0.0D));
        collector.record(state.setInt(KEY_FEED_TIMER, feedTimer, value -> Mth.clamp(value, 0, MAX_TIMER_GUARD), 0));
        collector.record(state.setInt(KEY_WITHER_TIMER, witherTimer, value -> Mth.clamp(value, 0, MAX_TIMER_GUARD), 0));
        collector.record(state.setInt(KEY_REVIVE_TIMER, reviveTimer, value -> Mth.clamp(value, 0, MAX_TIMER_GUARD), 0));
        collector.record(state.setInt(KEY_SUCCESSFUL_CASTS, successfulCasts,
                value -> Mth.clamp(value, 0, REQUIRED_CONSECUTIVE_CASTS_FOR_TIER_THREE), 0));

        TierSettings settings = SETTINGS_BY_TIER.getOrDefault(tier, SETTINGS_BY_TIER.get(1));

        FeedingResult feeding = consumeNutrients(player, state, collector, settings, hungerDebt, handleOpt.orElse(null));
        boolean fed = feeding.fed();

        if (fed) {
            feedTimer = Mth.clamp(feedTimer + 1, 0, MAX_TIMER_GUARD);
            witherTimer = 0;
            if (withered) {
                reviveTimer = Mth.clamp(reviveTimer + 1, 0, REVIVE_REQUIRED_TICKS);
            } else {
                reviveTimer = 0;
            }
        } else {
            feedTimer = 0;
            witherTimer = Mth.clamp(witherTimer + 1, 0, MAX_TIMER_GUARD);
            reviveTimer = 0;
            applyParasiticDrain(serverLevel, player);
        }

        collector.record(state.setInt(KEY_FEED_TIMER, feedTimer, value -> Mth.clamp(value, 0, MAX_TIMER_GUARD), 0));
        collector.record(state.setInt(KEY_WITHER_TIMER, witherTimer, value -> Mth.clamp(value, 0, MAX_TIMER_GUARD), 0));
        collector.record(state.setInt(KEY_REVIVE_TIMER, reviveTimer, value -> Mth.clamp(value, 0, MAX_TIMER_GUARD), 0));
        collector.record(state.setBoolean(KEY_PARASITIC, !fed, false));

        if (!withered && witherTimer >= WITHER_THRESHOLD_TICKS) {
            if (tier > 1) {
                tier = Mth.clamp(tier - 1, 1, 3);
                collector.record(state.setInt(KEY_TIER, tier, value -> Mth.clamp(value, 1, 3), 1));
                settings = SETTINGS_BY_TIER.getOrDefault(tier, SETTINGS_BY_TIER.get(1));
                feedTimer = 0;
                collector.record(state.setInt(KEY_FEED_TIMER, feedTimer, value -> Mth.clamp(value, 0, MAX_TIMER_GUARD), 0));
                collector.record(state.setInt(KEY_SUCCESSFUL_CASTS, 0,
                        value -> Mth.clamp(value, 0, REQUIRED_CONSECUTIVE_CASTS_FOR_TIER_THREE), 0));
                notifyTierRetreat(serverLevel, player, tier);
            } else {
                withered = true;
                collector.record(state.setBoolean(KEY_WITHERED, true, false));
                collector.record(state.setInt(KEY_SUCCESSFUL_CASTS, 0,
                        value -> Mth.clamp(value, 0, REQUIRED_CONSECUTIVE_CASTS_FOR_TIER_THREE), 0));
                notifyWither(serverLevel, player);
            }
            witherTimer = 0;
            collector.record(state.setInt(KEY_WITHER_TIMER, 0, value -> Mth.clamp(value, 0, MAX_TIMER_GUARD), 0));
        }

        if (withered && reviveTimer >= REVIVE_REQUIRED_TICKS) {
            withered = false;
            tier = 1;
            settings = SETTINGS_BY_TIER.get(1);
            collector.record(state.setBoolean(KEY_WITHERED, false, false));
            collector.record(state.setInt(KEY_TIER, tier, value -> Mth.clamp(value, 1, 3), 1));
            collector.record(state.setInt(KEY_REVIVE_TIMER, 0, value -> Mth.clamp(value, 0, MAX_TIMER_GUARD), 0));
            collector.record(state.setInt(KEY_SUCCESSFUL_CASTS, 0,
                    value -> Mth.clamp(value, 0, REQUIRED_CONSECUTIVE_CASTS_FOR_TIER_THREE), 0));
            notifyRevive(serverLevel, player);
        }

        if (!withered) {
            tier = Mth.clamp(state.getInt(KEY_TIER, tier), 1, 3);
            settings = SETTINGS_BY_TIER.getOrDefault(tier, SETTINGS_BY_TIER.get(1));

            boolean eligibleForUpgrade = maybeAdvanceTier(serverLevel, player, state, collector, handleOpt.orElse(null), feedTimer, tier, successfulCasts);
            if (eligibleForUpgrade) {
                tier = state.getInt(KEY_TIER, tier);
                settings = SETTINGS_BY_TIER.getOrDefault(tier, SETTINGS_BY_TIER.get(1));
            }

            if (fed) {
                double heal = settings.healPerSecond();
                double zhenyuan = settings.zhenyuanPerSecond();
                double envMultiplier = ShengJiYeOrganBehavior.environmentMultiplier(player);
                heal *= envMultiplier;
                zhenyuan *= envMultiplier;
                if (ShengJiYeOrganBehavior.hasMuDaoFlow(cc, serverLevel)) {
                    zhenyuan *= (1.0D + LINGGEN_BONUS);
                }
                if (heal > 0.0D) {
                    player.heal((float) heal);
                }
                if (zhenyuan > 0.0D) {
                    final double replenish = zhenyuan;
                    handleOpt.ifPresent(handle -> ResourceOps.tryAdjustZhenyuan(handle, replenish, true));
                }
                spawnPassiveParticles(serverLevel, player, tier);
            }
        } else if (!fed) {
            spawnParasiticParticles(serverLevel, player);
        }

        collector.commit();
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof ServerPlayer player) || cc == null || !player.isAlive()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        OrganState state = INSTANCE.organState(organ, STATE_ROOT);
        if (state.getBoolean(KEY_WITHERED, false)) {
            return;
        }
        int tier = Mth.clamp(state.getInt(KEY_TIER, 1), 1, 3);
        MultiCooldown cooldown = INSTANCE.cooldown(cc, organ, state);
        MultiCooldown.Entry abilityEntry = cooldown.entry(KEY_ABILITY_READY);
        long now = level.getGameTime();
        if (!abilityEntry.isReady(now)) {
            return;
        }

        double zhenyuanCost = ABILITY_ZHENYUAN_COST[tier - 1];
        double jingliCost = ABILITY_JINGLI_COST[tier - 1];

        var consumption = ResourceOps.consumeStrict(player, zhenyuanCost, jingliCost);
        if (!consumption.succeeded()) {
            return;
        }

        double selfHeal = ABILITY_SELF_HEAL[tier - 1];
        double allyHeal = ABILITY_ALLY_HEAL[tier - 1];
        double absorption = ABILITY_ABSORPTION[tier - 1];
        int radius = Math.max(1, ABILITY_RADIUS[tier - 1]);
        int resistDuration = ABILITY_RESIST_DURATION[tier - 1];
        int resistAmplifier = ABILITY_RESIST_AMPLIFIER[tier - 1];

        if (selfHeal > 0.0D) {
            player.heal((float) selfHeal);
        }
        applySupportEffects(level, player, allyHeal, absorption, resistDuration, resistAmplifier, radius);

        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.15F);
        level.sendParticles(PASSIVE_PARTICLE,
                player.getX(), player.getY() + player.getBbHeight() * 0.6D, player.getZ(),
                14, radius * 0.35D, radius * 0.15D, radius * 0.35D, 0.01D);

        long readyAt = now + ABILITY_COOLDOWN_TICKS[tier - 1];
        abilityEntry.setReadyAt(readyAt);
        ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, readyAt, now);

        int successfulCasts = Mth.clamp(state.getInt(KEY_SUCCESSFUL_CASTS, 0), 0, REQUIRED_CONSECUTIVE_CASTS_FOR_TIER_THREE);
        if (tier < 3) {
            OrganStateOps.setInt(state, cc, organ, KEY_SUCCESSFUL_CASTS,
                    Math.min(REQUIRED_CONSECUTIVE_CASTS_FOR_TIER_THREE, successfulCasts + 1),
                    value -> Mth.clamp(value, 0, REQUIRED_CONSECUTIVE_CASTS_FOR_TIER_THREE), 0);
        } else if (successfulCasts != 0) {
            OrganStateOps.setInt(state, cc, organ, KEY_SUCCESSFUL_CASTS, 0,
                    value -> Mth.clamp(value, 0, REQUIRED_CONSECUTIVE_CASTS_FOR_TIER_THREE), 0);
        }
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
            ItemStack stack = cc.inventory.getItem(i);
            if (!stack.isEmpty() && INSTANCE.matchesOrgan(stack, ORGAN_ID)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void applySupportEffects(ServerLevel level,
                                            ServerPlayer caster,
                                            double allyHeal,
                                            double absorptionHearts,
                                            int resistDuration,
                                            int resistAmplifier,
                                            int radius) {
        AABB area = caster.getBoundingBox().inflate(radius);
        int absorptionAmplifier = absorptionHearts > 0.0D
                ? Math.max(0, (int) Math.ceil(absorptionHearts / 2.0D) - 1)
                : -1;

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                candidate -> isFriendly(caster, candidate))) {
            boolean isCaster = target == caster;
            if (!isCaster && allyHeal > 0.0D) {
                target.heal((float) allyHeal);
            }
            if (resistDuration > 0 && resistAmplifier >= 0) {
                target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, resistDuration, resistAmplifier, false, true, true));
            }
            if (absorptionAmplifier >= 0) {
                target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 8 * 20, absorptionAmplifier, false, true, true));
            }
            if (!isCaster) {
                level.sendParticles(PASSIVE_PARTICLE,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        6, 0.2D, 0.2D, 0.2D, 0.004D);
            }
            int absorptionDuration = absorptionAmplifier >= 0 ? 8 * 20 : 0;
            int tagDuration = Math.max(Math.max(resistDuration, absorptionDuration), 40);
            ReactionTagOps.add(target, ReactionTagKeys.WOOD_GROWTH, tagDuration);
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

    private static FeedingResult consumeNutrients(Player player,
                                                  OrganState state,
                                                  OrganStateOps.Collector collector,
                                                  TierSettings settings,
                                                  double hungerDebt,
                                                  ResourceHandle handle) {
        boolean hungerOk = true;
        boolean energyOk = true;

        double hungerCost = settings.hungerCost();
        double energyCost = settings.energyCost();

        double hungerAvailable = player.getFoodData().getFoodLevel() + hungerDebt;
        if (hungerCost > 0.0D) {
            if (hungerAvailable + 1.0E-4D < hungerCost) {
                hungerOk = false;
            }
        }

        if (energyCost > 0.0D) {
            if (handle == null) {
                energyOk = false;
            } else {
                OptionalDouble current = handle.getJingli();
                if (current.isEmpty() || current.getAsDouble() + 1.0E-4D < energyCost) {
                    energyOk = false;
                }
            }
        }

        if (!hungerOk || !energyOk) {
            return FeedingResult.UNFED;
        }

        double remainingHunger = hungerAvailable - hungerCost;
        int hungerLevel = Math.max(0, (int) Math.floor(remainingHunger));
        double newDebt = clampDebt(remainingHunger - hungerLevel);

        FoodData foodData = player.getFoodData();
        foodData.setFoodLevel(hungerLevel);
        collector.record(state.setDouble(KEY_HUNGER_DEBT, newDebt, JiuYeShengJiCaoOrganBehavior::clampDebt, 0.0D));

        if (energyCost > 0.0D && handle != null) {
            handle.adjustJingli(-energyCost, true);
        }

        return FeedingResult.FED;
    }

    private static boolean maybeAdvanceTier(ServerLevel level,
                                            Player player,
                                            OrganState state,
                                            OrganStateOps.Collector collector,
                                            ResourceHandle handle,
                                            int feedTimer,
                                            int currentTier,
                                            int successfulCasts) {
        TierSettings next = SETTINGS_BY_TIER.get(currentTier + 1);
        if (next == null) {
            return false;
        }
        if (feedTimer < next.stabilityWindowTicks()) {
            return false;
        }
        if (handle == null) {
            return false;
        }
        if (next.tier() == 3 && successfulCasts < REQUIRED_CONSECUTIVE_CASTS_FOR_TIER_THREE) {
            return false;
        }
        double zhuanshu = handle.getZhuanshu().orElse(1.0D);
        if (zhuanshu + 1.0E-4D < next.requiredZhuanshu()) {
            return false;
        }
        OptionalDouble zhenyuanOpt = handle.getZhenyuan();
        OptionalDouble maxOpt = handle.getMaxZhenyuan();
        double ratio = 0.0D;
        if (zhenyuanOpt.isPresent() && maxOpt.isPresent() && maxOpt.getAsDouble() > 0.0D) {
            ratio = zhenyuanOpt.getAsDouble() / maxOpt.getAsDouble();
        }
        if (ratio + 1.0E-4D < next.requiredZhenyuanRatio()) {
            return false;
        }
        collector.record(state.setInt(KEY_TIER, next.tier(), value -> Mth.clamp(value, 1, 3), 1));
        level.playSound(null, player.blockPosition(), SoundEvents.AZALEA_PLACE, SoundSource.PLAYERS, 0.6F, 1.4F);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("text.chestcavity.guzhenren.jiuye.advance"), true);
        collector.record(state.setInt(KEY_SUCCESSFUL_CASTS, 0,
                value -> Mth.clamp(value, 0, REQUIRED_CONSECUTIVE_CASTS_FOR_TIER_THREE), 0));
        return true;
    }

    private static void applyParasiticDrain(ServerLevel level, Player player) {
        DamageSource source = player.damageSources().magic();
        ResourceOps.drainHealth(player, (float) PARASITIC_DAMAGE, 0.0F, source);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.35F, 0.8F);
    }

    private static void spawnPassiveParticles(ServerLevel level, Player player, int tier) {
        double radius = switch (tier) {
            case 2 -> 0.6D;
            case 3 -> 0.75D;
            default -> 0.45D;
        };
        int count = switch (tier) {
            case 2 -> 10;
            case 3 -> 14;
            default -> 6;
        };
        level.sendParticles(PASSIVE_PARTICLE,
                player.getX(),
                player.getY() + player.getBbHeight() * 0.5D,
                player.getZ(),
                count,
                radius,
                radius * 0.6D,
                radius,
                0.006D);
        if (tier >= 3) {
            level.sendParticles(ParticleTypes.GLOW,
                    player.getX(),
                    player.getY() + 0.8D,
                    player.getZ(),
                    3,
                    0.2D, 0.25D, 0.2D,
                    0.01D);
        }
    }

    private static void spawnParasiticParticles(ServerLevel level, Player player) {
        level.sendParticles(PARASITIC_PARTICLE,
                player.getX(),
                player.getY() + player.getBbHeight() * 0.45D,
                player.getZ(),
                8,
                0.3D,
                0.2D,
                0.3D,
                0.004D);
        level.sendParticles(ParticleTypes.ASH, player.getX(), player.getY() + 0.4D, player.getZ(), 4,
                0.2D, 0.2D, 0.2D, 0.0D);
    }

    private static void notifyTierRetreat(ServerLevel level, Player player, int newTier) {
        level.playSound(null, player.blockPosition(), SoundEvents.GRASS_BREAK, SoundSource.PLAYERS, 0.7F, 0.9F);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("text.chestcavity.guzhenren.jiuye.retreat"), true);
    }

    private static void notifyWither(ServerLevel level, Player player) {
        level.playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.6F, 0.8F);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("text.chestcavity.guzhenren.jiuye.wither"), true);
    }

    private static void notifyRevive(ServerLevel level, Player player) {
        level.playSound(null, player.blockPosition(), SoundEvents.AZALEA_PLACE, SoundSource.PLAYERS, 0.8F, 1.2F);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("text.chestcavity.guzhenren.jiuye.revive"), true);
    }

    private static double clampDebt(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Mth.clamp(value, 0.0D, 1.0D);
    }

    private enum FeedingResult {
        FED(true),
        UNFED(false);

        private final boolean fed;

        FeedingResult(boolean fed) {
            this.fed = fed;
        }

        public boolean fed() {
            return fed;
        }
    }

    private record TierSettings(int tier,
                                double hungerCost,
                                double energyCost,
                                double healPerSecond,
                                double zhenyuanPerSecond,
                                int requiredZhuanshu,
                                double requiredZhenyuanRatio,
                                int stabilityWindowTicks) {
    }
}
