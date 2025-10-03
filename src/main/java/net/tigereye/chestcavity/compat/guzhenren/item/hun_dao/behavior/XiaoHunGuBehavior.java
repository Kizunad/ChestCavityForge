package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior;

// file moved from HunDaoHeartBehavior.java to match public class name

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.linkage.IncreaseEffectLedger;
import net.tigereye.chestcavity.linkage.policy.ClampPolicy;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

public final class XiaoHunGuBehavior extends AbstractGuzhenrenOrganBehavior implements OrganSlowTickListener, OrganRemovalListener {

    public static final XiaoHunGuBehavior INSTANCE = new XiaoHunGuBehavior();

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "guzhenren";

    private static final ResourceLocation HUN_JI_RECOVERY_EFFECT = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/hun_ji_recovery_effect");

    private static final String STATE_ROOT_KEY = "HunDaoHeart";
    private static final String KEY_LAST_SYNC_TICK = "last_sync_tick";

    private static final double BASE_RECOVERY_PER_SECOND = 1.0;
    private static final double RECOVERY_BONUS = 0.20;

    private static final ClampPolicy NON_NEGATIVE = new ClampPolicy(0.0, Double.MAX_VALUE);

    private XiaoHunGuBehavior() {}

    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        ensureChannel(context, HUN_JI_RECOVERY_EFFECT);
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        registerRemovalHook(cc, organ, this, staleRemovalContexts);
        ensureAttached(cc);
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        LinkageChannel channel = context.getOrCreateChannel(HUN_JI_RECOVERY_EFFECT).addPolicy(NON_NEGATIVE);
        refreshIncreaseContribution(cc, organ, true);
        sendSlotUpdate(cc, organ);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity.level().isClientSide()) {
            return;
        }
        if (entity instanceof Player player) {
            handlerPlayer(player, cc, organ);
        } else {
            handlerNonPlayer(entity, cc, organ);
        }
    }

    private void handlerPlayer(Player player, ChestCavityInstance cc, ItemStack organ) {
        if (cc == null) {
            return;
        }
        ensureAttached(cc);
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        LinkageChannel channel = context.getOrCreateChannel(HUN_JI_RECOVERY_EFFECT).addPolicy(NON_NEGATIVE);
        double bonus = Math.max(0.0, channel.get());
        double amount = BASE_RECOVERY_PER_SECOND * (1.0 + bonus);
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        handleOpt.ifPresent(handle -> {
            handle.adjustDouble("hunpo", amount, true, "zuida_hunpo");
            LOGGER.debug("[compat/guzhenren][hun_dao][heart][player] +{} hunpo to {} (bonus={})",
                String.format("%.2f", amount), player.getScoreboardName(), String.format("%.2f", bonus));
        });
        OrganState state = organState(organ, STATE_ROOT_KEY);
        state.setLong(KEY_LAST_SYNC_TICK, player.level().getGameTime());
    }

    private void handlerNonPlayer(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        LOGGER.debug("[compat/guzhenren][hun_dao][heart][nonplayer] skip entity={} type={}",
            entity.getStringUUID(), entity.getType().toShortString());
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (cc == null) {
            return;
        }
        refreshIncreaseContribution(cc, organ, false);
    }

    private void refreshIncreaseContribution(ChestCavityInstance cc, ItemStack organ, boolean primary) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        LinkageChannel channel = context.getOrCreateChannel(HUN_JI_RECOVERY_EFFECT).addPolicy(NON_NEGATIVE);
        IncreaseEffectLedger ledger = context.increaseEffects();
        ledger.verifyAndRebuildIfNeeded();
        double previous = ledger.adjust(organ, HUN_JI_RECOVERY_EFFECT, 0.0);
        double target = primary ? RECOVERY_BONUS : 0.0;
        double delta = target - previous;
        if (delta != 0.0) {
            channel.adjust(delta);
            ledger.adjust(organ, HUN_JI_RECOVERY_EFFECT, delta);
        }
    }
}

