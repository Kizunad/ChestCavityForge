package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Behaviour for 肋骨盾蛊 – a defensive bone Gu that fuels bone growth, grants increase effect and
 * stores "不屈" stacks for the Bone Guard active ability.
 */
public final class LeGuDunGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
        implements OrganSlowTickListener, OrganRemovalListener, IncreaseEffectContributor {

    public static final LeGuDunGuOrganBehavior INSTANCE = new LeGuDunGuOrganBehavior();

    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "le_gu_dun_gu");
    public static final ResourceLocation ABILITY_ID = ORGAN_ID;

    private static final ResourceLocation BONE_GROWTH_CHANNEL =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/bone_growth");
    private static final ResourceLocation GU_DAO_INCREASE_CHANNEL =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/gu_dao_increase_effect");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final String STATE_ROOT = "LeGuDunGu";
    private static final String BU_QU_KEY = "BuQu";

    private static final double BONE_GROWTH_PER_SECOND = 60.0;
    private static final double GU_DAO_INCREASE_PER_STACK = 0.14;
    private static final int MAX_EFFECTIVE_STACKS = BehaviorConfigAccess.getInt(LeGuDunGuOrganBehavior.class, "MAX_EFFECTIVE_STACKS", 1);
    private static final int MAX_BU_QU = BehaviorConfigAccess.getInt(LeGuDunGuOrganBehavior.class, "MAX_BU_QU", 10);

    private static final double BASE_ZHENYUAN_COST_PER_SECOND = 200.0;
    private static final int INVULN_DURATION_TICKS = BehaviorConfigAccess.getInt(LeGuDunGuOrganBehavior.class, "INVULN_DURATION_TICKS", 40);

    static {
        OrganActivationListeners.register(ABILITY_ID, LeGuDunGuOrganBehavior::activateAbility);
    }

    private LeGuDunGuOrganBehavior() {
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }

        boolean primary = isPrimaryOrgan(cc, organ);
        refreshIncreaseContribution(cc, organ, primary);

        OrganState state = organState(organ, STATE_ROOT);
        if (!primary) {
            resetBuQuIfNeeded(cc, organ, state);
            return;
        }

        ActiveLinkageContext context = LinkageManager.getContext(cc);
        ensureChannel(context, BONE_GROWTH_CHANNEL).addPolicy(NON_NEGATIVE).adjust(BONE_GROWTH_PER_SECOND);

        int current = clampBuQu(state.getInt(BU_QU_KEY, 0));
        if (current < MAX_BU_QU) {
            net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps.setIntSync(
                    cc, organ, STATE_ROOT, BU_QU_KEY, current + 1, v -> clampBuQu(v), 0);
        } else if (current != state.getInt(BU_QU_KEY, 0)) {
            net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps.setIntSync(
                    cc, organ, STATE_ROOT, BU_QU_KEY, current, v -> clampBuQu(v), 0);
        }
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        RemovalRegistration registration = registerRemovalHook(cc, organ, this, staleRemovalContexts);
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        IncreaseEffectLedger ledger = context.increaseEffects();
        // Ensure clean ledger state before registering and refreshing contributions
        ledger.verifyAndRebuildIfNeeded();
        ledger.registerContributor(organ, this, GU_DAO_INCREASE_CHANNEL);
        refreshIncreaseContribution(cc, organ, isPrimaryOrgan(cc, organ));
        if (!registration.alreadyRegistered() && ChestCavity.LOGGER.isDebugEnabled()) {
            ChestCavity.LOGGER.debug("[compat/guzhenren][le_gu_dun_gu] registered removal hook for {}", describeStack(organ));
        }
    }

    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        ensureChannel(context, BONE_GROWTH_CHANNEL).addPolicy(NON_NEGATIVE);
        ensureChannel(context, GU_DAO_INCREASE_CHANNEL).addPolicy(NON_NEGATIVE);
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        IncreaseEffectLedger ledger = context.increaseEffects();
        double removed = ledger.remove(organ, GU_DAO_INCREASE_CHANNEL);
        ledger.unregisterContributor(organ);
        if (removed != 0.0) {
            ensureChannel(context, GU_DAO_INCREASE_CHANNEL).addPolicy(NON_NEGATIVE).adjust(-removed);
        }
        ledger.verifyAndRebuildIfNeeded();
        net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps.setIntSync(cc, organ, STATE_ROOT, BU_QU_KEY, 0, value -> 0, 0);
    }

    @Override
    public void rebuildIncreaseEffects(
            ChestCavityInstance cc,
            ActiveLinkageContext context,
            ItemStack organ,
            IncreaseEffectLedger.Registrar registrar
    ) {
        if (cc == null || organ == null || organ.isEmpty() || registrar == null) {
            return;
        }
        boolean active = isPrimaryOrgan(cc, organ);
        int effectiveStacks = active ? Math.min(MAX_EFFECTIVE_STACKS, Math.max(1, organ.getCount())) : 0;
        double effect = effectiveStacks * GU_DAO_INCREASE_PER_STACK;
        registrar.record(GU_DAO_INCREASE_CHANNEL, effectiveStacks, effect);
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof ServerPlayer player) || entity.level().isClientSide()) {
            return;
        }
        INSTANCE.tryActivateBoneGuard(player, cc);
    }

    private void tryActivateBoneGuard(ServerPlayer player, ChestCavityInstance cc) {
        if (player == null || cc == null || cc.inventory == null) {
            return;
        }
        ItemStack organ = findPrimaryOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        int buQu = clampBuQu(state.getInt(BU_QU_KEY, 0));
        if (buQu < MAX_BU_QU) {
            return;
        }

        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }

        double durationSeconds = INVULN_DURATION_TICKS / 20.0;
        double totalCost = BASE_ZHENYUAN_COST_PER_SECOND * durationSeconds;
        OptionalDouble costResult = handleOpt.get().consumeScaledZhenyuan(totalCost);
        if (costResult.isEmpty()) {
            return;
        }

        applyBoneGuardInvulnerability(player);
        net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps.setIntSync(
                cc, organ, STATE_ROOT, BU_QU_KEY, 0, v -> 0, 0);
    }

    private void applyBoneGuardInvulnerability(ServerPlayer player) {
        player.invulnerableTime = Math.max(player.invulnerableTime, INVULN_DURATION_TICKS);
        player.hurtTime = 0;
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.SHIELD_BLOCK,
                SoundSource.PLAYERS,
                1.0f,
                0.85f + player.getRandom().nextFloat() * 0.3f
        );
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, INVULN_DURATION_TICKS, 4, false, true, true));
    }

    private void refreshIncreaseContribution(ChestCavityInstance cc, ItemStack organ, boolean active) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        IncreaseEffectLedger ledger = context.increaseEffects();
        // Guard against stale stacked entries before computing delta
        ledger.verifyAndRebuildIfNeeded();
        double target = active
                ? Math.min(MAX_EFFECTIVE_STACKS, Math.max(1, organ.getCount())) * GU_DAO_INCREASE_PER_STACK
                : 0.0;
        double previous = ledger.adjust(organ, GU_DAO_INCREASE_CHANNEL, 0.0);
        double delta = target - previous;
        if (delta != 0.0) {
            LedgerOps.adjust(cc, organ, GU_DAO_INCREASE_CHANNEL, delta, NON_NEGATIVE, true);
        }
    }

    private void resetBuQuIfNeeded(ChestCavityInstance cc, ItemStack organ, OrganState state) {
        if (state == null) {
            return;
        }
        int current = state.getInt(BU_QU_KEY, 0);
        if (current != 0) {
            OrganStateOps.setInt(state, cc, organ, BU_QU_KEY, 0, value -> 0, 0);
        }
    }

    private static int clampBuQu(int value) {
        return Mth.clamp(value, 0, MAX_BU_QU);
    }

    private static boolean isPrimaryOrgan(ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || cc.inventory == null || organ == null || organ.isEmpty()) {
            return false;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (!matchesOrgan(stack, ORGAN_ID)) {
                continue;
            }
            return stack == organ;
        }
        return false;
    }

    private static ItemStack findPrimaryOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (matchesOrgan(stack, ORGAN_ID)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean matchesOrgan(ItemStack stack, ResourceLocation organId) {
        if (stack == null || stack.isEmpty() || organId == null) {
            return false;
        }
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return organId.equals(id);
    }
}
