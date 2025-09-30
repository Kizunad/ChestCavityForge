package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowControllerManager;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgram;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgramRegistry;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.registration.CCItems;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import net.tigereye.chestcavity.util.NetworkUtil;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Behaviour implementation for 冰肌蛊 (Bing Ji Gu).
 */
public final class BingJiGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
        implements OrganSlowTickListener, OrganOnHitListener, OrganRemovalListener {

    public static final BingJiGuOrganBehavior INSTANCE = new BingJiGuOrganBehavior();

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "bing_ji_gu");
    private static final ResourceLocation JADE_BONE_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "yu_gu_gu");
    private static final ResourceLocation ICE_BURST_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "bing_bao_gu");
    private static final ResourceLocation BING_XUE_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/bing_xue_dao_increase_effect");
    private static final ResourceLocation BLEED_EFFECT_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "lliuxue");
    private static final ResourceLocation ICE_COLD_EFFECT_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "hhanleng");

    public static final ResourceLocation ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "bing_ji_gu_iceburst");
    private static final ResourceLocation ICE_BURST_FLOW_ID =
            ResourceLocation.fromNamespaceAndPath(ChestCavity.MODID, "bing_xue_burst");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final String STATE_ROOT = "BingJiGu";
    private static final String ABSORPTION_TIMER_KEY = "AbsorptionTimer";
    private static final String INVULN_COOLDOWN_KEY = "InvulnCooldown";

    private static final double ZHENYUAN_BASE_COST = 200.0;
    private static final double JINGLI_PER_TICK = 1.0;
    private static final float HEAL_PER_TICK = 5.0f;
    private static final int SLOW_TICK_INTERVALS_PER_MINUTE = 60;
    private static final float ABSORPTION_PER_TRIGGER = 20.0f;
    private static final double BONUS_DAMAGE_FRACTION = 0.05;
    private static final double BONUS_TRIGGER_CHANCE = 0.10;
    private static final int ICE_EFFECT_DURATION_TICKS = 30 * 20;
    private static final double ICE_BURST_BASE_DAMAGE = 18.0;
    private static final double ICE_BURST_RADIUS = 6.0;
    private static final double ICE_BURST_RADIUS_PER_STACK = 0.5;
    private static final double ICE_BURST_STACK_DAMAGE_SCALE = 0.65;
    private static final double ICE_BURST_BING_BAO_MULTIPLIER = 0.35;
    private static final float ICE_BURST_SLOW_AMPLIFIER = 1.0f;
    private static final int ICE_BURST_SLOW_DURATION = 8 * 20;
    private static final int INVULN_DURATION_TICKS = 40;
    private static final int INVULN_COOLDOWN_TICKS = 20 * 30;
    private static final double LOW_HEALTH_THRESHOLD = 0.30;

    static {
        OrganActivationListeners.register(ABILITY_ID, BingJiGuOrganBehavior::activateAbility);
    }

    private BingJiGuOrganBehavior() {
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide() || cc == null) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID) && !organ.is(CCItems.GUZHENREN_BING_JI_GU)) {
            return;
        }

        int stackCount = Math.max(1, organ.getCount());
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();

        ActiveLinkageContext context = LinkageManager.getContext(cc);
        double efficiency = 1.0 + lookupIncreaseEffect(context);

        boolean paid = handle.consumeScaledZhenyuan(ZHENYUAN_BASE_COST * stackCount).isPresent();
        OrganState state = organState(organ, STATE_ROOT);
        boolean stateChanged = false;

        if (paid) {
            handle.adjustJingli(JINGLI_PER_TICK * stackCount, true);
            float healAmount = HEAL_PER_TICK * stackCount;
            if (healAmount > 0.0f) {
                ChestCavityUtil.runWithOrganHeal(() -> player.heal(healAmount));
            }
            stateChanged |= tickAbsorption(player, state, stackCount, efficiency);
            if (hasJadeBone(cc)) {
                clearBleed(player);
            }
        }

        stateChanged |= tickInvulnerability(player, state, cc, organ);
        if (stateChanged) {
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }
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
        if (attacker == null || target == null || attacker.level().isClientSide()) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID) && !organ.is(CCItems.GUZHENREN_BING_JI_GU)) {
            return damage;
        }
        if (damage <= 0.0f || attacker.getRandom().nextDouble() >= BONUS_TRIGGER_CHANCE) {
            return damage;
        }

        double efficiency = 1.0 + lookupIncreaseEffect(LinkageManager.getContext(cc));
        float bonus = (float) (damage * BONUS_DAMAGE_FRACTION * Math.max(0.0, efficiency));
        if (bonus > 0.0f) {
            applyColdEffect(target);
            return damage + bonus;
        }
        applyColdEffect(target);
        return damage;
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!matchesOrgan(organ, ORGAN_ID) && !organ.is(CCItems.GUZHENREN_BING_JI_GU)) {
            return;
        }
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID) && !organ.is(CCItems.GUZHENREN_BING_JI_GU)) {
            return;
        }
        RemovalRegistration registration = registerRemovalHook(cc, organ, this, staleRemovalContexts);
        if (!registration.alreadyRegistered()) {
            OrganState state = organState(organ, STATE_ROOT);
            state.setInt(ABSORPTION_TIMER_KEY, 0);
            state.setInt(INVULN_COOLDOWN_KEY, 0);
            NetworkUtil.sendOrganSlotUpdate(cc, organ);
        }
    }

    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        if (context == null) {
            return;
        }
        ensureIncreaseChannel(context, BING_XUE_INCREASE_EFFECT);
    }

    private static void ensureIncreaseChannel(ActiveLinkageContext context, ResourceLocation id) {
        if (context == null || id == null) {
            return;
        }
        context.getOrCreateChannel(id).addPolicy(NON_NEGATIVE);
    }

    private static double lookupIncreaseEffect(ActiveLinkageContext context) {
        if (context == null) {
            return 0.0;
        }
        return context.lookupChannel(BING_XUE_INCREASE_EFFECT).map(LinkageChannel::get).orElse(0.0);
    }

    private static boolean tickAbsorption(Player player, OrganState state, int stacks, double efficiency) {
        if (player == null || state == null) {
            return false;
        }
        int timer = state.getInt(ABSORPTION_TIMER_KEY, 0) + 1;
        boolean changed = false;
        if (timer >= SLOW_TICK_INTERVALS_PER_MINUTE) {
            timer = 0;
            float gain = (float) (ABSORPTION_PER_TRIGGER * Math.max(1, stacks) * Math.max(0.0, efficiency));
            float updated = player.getAbsorptionAmount() + gain;
            player.setAbsorptionAmount(updated);
            changed = true;
        }
        if (changed) {
            state.setInt(ABSORPTION_TIMER_KEY, timer);
            return true;
        }
        if (state.getInt(ABSORPTION_TIMER_KEY, 0) != timer) {
            state.setInt(ABSORPTION_TIMER_KEY, timer);
            return true;
        }
        return false;
    }

    private static boolean tickInvulnerability(Player player, OrganState state, ChestCavityInstance cc, ItemStack organ) {
        if (player == null || state == null) {
            return false;
        }
        int cooldown = Math.max(0, state.getInt(INVULN_COOLDOWN_KEY, 0));
        boolean changed = false;
        if (cooldown > 0) {
            cooldown--;
            state.setInt(INVULN_COOLDOWN_KEY, cooldown);
            changed = true;
        }
        if (cooldown <= 0 && hasJadeBone(cc) && isBeimingConstitution(player)
                && player.getHealth() <= player.getMaxHealth() * LOW_HEALTH_THRESHOLD) {
            player.invulnerableTime = Math.max(player.invulnerableTime, INVULN_DURATION_TICKS);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, INVULN_DURATION_TICKS, 4, false, true, true));
            state.setInt(INVULN_COOLDOWN_KEY, INVULN_COOLDOWN_TICKS);
            changed = true;
        }
        return changed;
    }

    private static void clearBleed(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        Optional<Holder.Reference<MobEffect>> holder = BuiltInRegistries.MOB_EFFECT.getHolder(BLEED_EFFECT_ID);
        holder.ifPresent(entity::removeEffect);
    }

    private static void applyColdEffect(LivingEntity target) {
        if (target == null || target.level().isClientSide()) {
            return;
        }
        Optional<Holder.Reference<MobEffect>> holder = BuiltInRegistries.MOB_EFFECT.getHolder(ICE_COLD_EFFECT_ID);
        holder.ifPresent(effect -> target.addEffect(new MobEffectInstance(effect, ICE_EFFECT_DURATION_TICKS, 0, false, true, true)));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ICE_EFFECT_DURATION_TICKS, 0, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ICE_EFFECT_DURATION_TICKS, 0, false, true, true));
    }

    private static boolean hasJadeBone(ChestCavityInstance cc) {
        return hasOrgan(cc, JADE_BONE_ID);
    }

    private static boolean hasBingBao(ChestCavityInstance cc) {
        return hasOrgan(cc, ICE_BURST_ID);
    }

    private static boolean hasOrgan(ChestCavityInstance cc, ResourceLocation id) {
        if (cc == null || id == null || cc.inventory == null) {
            return false;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (Objects.equals(stackId, id)) {
                return true;
            }
        }
        return false;
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
            if (stack.is(CCItems.GUZHENREN_BING_JI_GU)) {
                return stack;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (Objects.equals(id, ORGAN_ID)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean consumeMuscle(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return false;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null && "chestcavity".equals(id.getNamespace()) && id.getPath().endsWith("_muscle")) {
                cc.inventory.removeItem(i, 1);
                return true;
            }
        }
        return false;
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (entity == null || cc == null || entity.level().isClientSide()) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty() || !hasJadeBone(cc)) {
            return;
        }
        if (!consumeMuscle(cc)) {
            return;
        }
        Level level = entity.level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        int stacks = Math.max(1, organ.getCount());
        double efficiency = 1.0 + lookupIncreaseEffect(LinkageManager.getContext(cc));
        double baseDamage = ICE_BURST_BASE_DAMAGE * Math.pow(ICE_BURST_STACK_DAMAGE_SCALE, stacks - 1);
        baseDamage *= Math.max(0.0, efficiency);
        if (hasBingBao(cc)) {
            baseDamage *= 1.0 + ICE_BURST_BING_BAO_MULTIPLIER;
        }
        double radius = ICE_BURST_RADIUS + Math.max(0, stacks - 1) * ICE_BURST_RADIUS_PER_STACK;

        Vec3 origin = entity.position();
        List<LivingEntity> victims = gatherTargets(entity, server, radius);
        for (LivingEntity target : victims) {
            double distance = Math.sqrt(target.distanceToSqr(origin));
            double falloff = Math.max(0.0, 1.0 - (distance / radius));
            float damage = (float) (baseDamage * falloff);
            if (damage > 0.0f) {
                DamageSource source = entity instanceof Player player
                        ? player.damageSources().playerAttack(player)
                        : server.damageSources().mobAttack(entity);
                target.hurt(source, damage);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ICE_BURST_SLOW_DURATION, (int) ICE_BURST_SLOW_AMPLIFIER, false, true, true));
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ICE_BURST_SLOW_DURATION, (int) ICE_BURST_SLOW_AMPLIFIER, false, true, true));
                applyColdEffect(target);
            }
        }

        triggerBurstFlow(entity, radius, victims.size());
    }

    private static List<LivingEntity> gatherTargets(LivingEntity user, ServerLevel level, double radius) {
        AABB area = user.getBoundingBox().inflate(radius);
        return level.getEntitiesOfClass(LivingEntity.class, area, target ->
                target != user && target.isAlive() && !target.isAlliedTo(user));
    }

    private static void triggerBurstFlow(LivingEntity entity, double radius, int victims) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        Optional<FlowProgram> programOpt = FlowProgramRegistry.get(ICE_BURST_FLOW_ID);
        if (programOpt.isEmpty()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        FlowController controller = FlowControllerManager.get(player);
        Map<String, String> params = new HashMap<>();
        params.put("burst.radius", formatDouble(Math.max(0.0D, radius)));
        double victimContribution = Math.max(0, victims) * 0.25D;
        double radiusContribution = Math.max(0.0D, radius - ICE_BURST_RADIUS) * 0.1D;
        double scale = Math.max(1.0D, Math.min(6.0D, 1.0D + victimContribution + radiusContribution));
        params.put("burst.scale", formatDouble(scale));
        FlowProgram program = programOpt.get();
        controller.start(program, player, 1.0D, params, level.getGameTime(), "bing_ji_gu.iceburst");
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static boolean isBeimingConstitution(Player player) {
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return false;
        }
        return handleOpt.get().hasConstitution("北冥冰魄体");
    }
}
