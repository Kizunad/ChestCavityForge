package net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;


import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.IncreaseEffectContributor;
import net.tigereye.chestcavity.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.LedgerOps;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import org.slf4j.Logger;

import java.util.List;

/**
 * Behaviour for 血气蛊 (Xue Qi Gu).
 */
public final class XueqiguOrganBehavior extends AbstractGuzhenrenOrganBehavior
        implements OrganSlowTickListener, OrganRemovalListener, IncreaseEffectContributor {

    public static final XueqiguOrganBehavior INSTANCE = new XueqiguOrganBehavior();

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LOG_PREFIX = "[Xue Qi Gu]";

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xueqigu");
    private static final ResourceLocation XUE_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/xue_dao_increase_effect");

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private static final double INCREASE_PER_STACK = 0.1;
    private static final double JINGLI_PER_SECOND = 2.5;
    private static final float HEAL_PER_SECOND = BehaviorConfigAccess.getFloat(XueqiguOrganBehavior.class, "HEAL_PER_SECOND", 3.0f);
    private static final float HEALTH_DRAIN_PER_MINUTE = BehaviorConfigAccess.getFloat(XueqiguOrganBehavior.class, "HEALTH_DRAIN_PER_MINUTE", 8.0f);
    private static final float HEALTH_DRAIN_RESERVE = BehaviorConfigAccess.getFloat(XueqiguOrganBehavior.class, "HEALTH_DRAIN_RESERVE", 1.0f);
    private static final int SLOW_TICKS_PER_MINUTE = BehaviorConfigAccess.getInt(XueqiguOrganBehavior.class, "SLOW_TICKS_PER_MINUTE", 60);
    private static final int FAILURE_RETRY_TICKS = BehaviorConfigAccess.getInt(XueqiguOrganBehavior.class, "FAILURE_RETRY_TICKS", 5);

    private static final String STATE_KEY = "Xueqigu";
    private static final String TIMER_KEY = "DrainTimer";

    private XueqiguOrganBehavior() {
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }

        ActiveLinkageContext context = LinkageManager.getContext(cc);
        context.getOrCreateChannel(XUE_DAO_INCREASE_EFFECT).addPolicy(NON_NEGATIVE);
        IncreaseEffectLedger ledger = context.increaseEffects();
        ledger.registerContributor(organ, this, XUE_DAO_INCREASE_EFFECT);

        RemovalRegistration registration = registerRemovalHook(cc, organ, this, staleRemovalContexts);
        refreshIncreaseContribution(cc, organ);
        if (!registration.alreadyRegistered()) {
            OrganStateOps.setIntSync(cc, organ, STATE_KEY, TIMER_KEY, SLOW_TICKS_PER_MINUTE, v -> Math.max(1, v), SLOW_TICKS_PER_MINUTE);
        }
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        refreshIncreaseContribution(cc, organ);

        if (cc == null || !entity.isAlive()) {
            return;
        }

        int stackCount = Math.max(1, organ.getCount());
        if (entity instanceof Player player) {
            ResourceOps.adjustJingli(player, JINGLI_PER_SECOND * stackCount);
        }

        if (HEAL_PER_SECOND > 0.0f && entity.getHealth() < entity.getMaxHealth()) {
            float heal = HEAL_PER_SECOND * stackCount;
            ChestCavityUtil.runWithOrganHeal(() -> entity.heal(heal));
        }

        OrganState state = organState(organ, STATE_KEY);
        int timer = Math.max(1, state.getInt(TIMER_KEY, SLOW_TICKS_PER_MINUTE));
        timer -= 1;
        boolean drained = false;
        if (timer <= 0) {
            float drainAmount = HEALTH_DRAIN_PER_MINUTE * stackCount;
            drained = GuzhenrenResourceCostHelper.drainHealth(entity, drainAmount, HEALTH_DRAIN_RESERVE, entity.damageSources().generic());
            timer = drained ? SLOW_TICKS_PER_MINUTE : FAILURE_RETRY_TICKS;
        }
        OrganStateOps.setIntSync(cc, organ, STATE_KEY, TIMER_KEY, timer, v -> Math.max(1, v), SLOW_TICKS_PER_MINUTE);

        // 维持血气流转标记，供反应系统加权判定（短时续期）
        net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps.add(
                entity,
                net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys.BLOOD_FLOW,
                60);
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        IncreaseEffectLedger ledger = context.increaseEffects();
        double removed = ledger.remove(organ, XUE_DAO_INCREASE_EFFECT);
        ledger.unregisterContributor(organ);
        context.lookupChannel(XUE_DAO_INCREASE_EFFECT)
                .ifPresent(channel -> channel.adjust(-removed));
    }

    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        LinkageManager.getContext(cc)
                .getOrCreateChannel(XUE_DAO_INCREASE_EFFECT)
                .addPolicy(NON_NEGATIVE);
    }

    @Override
    public void rebuildIncreaseEffects(
            ChestCavityInstance cc,
            ActiveLinkageContext context,
            ItemStack organ,
            IncreaseEffectLedger.Registrar registrar
    ) {
        if (organ == null || organ.isEmpty()) {
            return;
        }
        int stackCount = Math.max(1, organ.getCount());
        double effect = stackCount * INCREASE_PER_STACK;
        registrar.record(XUE_DAO_INCREASE_EFFECT, stackCount, effect);
    }

    private void refreshIncreaseContribution(ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        IncreaseEffectLedger ledger = context.increaseEffects();
        double previous = ledger.adjust(organ, XUE_DAO_INCREASE_EFFECT, 0.0);
        double target = Math.max(1, organ.getCount()) * INCREASE_PER_STACK;
        double delta = target - previous;
        if (delta != 0.0) {
            LedgerOps.adjust(cc, organ, XUE_DAO_INCREASE_EFFECT, delta, NON_NEGATIVE, true);
        }
    }
}
