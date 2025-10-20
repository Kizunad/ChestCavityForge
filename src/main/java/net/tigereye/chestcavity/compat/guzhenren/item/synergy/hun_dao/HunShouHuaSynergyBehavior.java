package net.tigereye.chestcavity.compat.guzhenren.item.synergy.hun_dao;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.HunShouHuaConstants;
import net.tigereye.chestcavity.compat.guzhenren.util.OrganPresenceUtil;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowControllerManager;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgram;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgramRegistry;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.linkage.effect.GuzhenrenLinkageEffectRegistry;
import net.tigereye.chestcavity.registration.CCAttachments;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 联动：小魂蛊 + 大魂蛊 → 魂兽化仪式。
 * <p>
 * - 仅限玩家在服务端触发；
 * - 激活时启动 {@link HunShouHuaConstants#FLOW_ID} 脚本，执行完整的魂兽化流程；
 * - 记录当前是否满足前置条件 / 是否已使用，以便后续扩展展示状态；
 * - 仅允许魂兽化一次，成功后永久标记。
 */
public final class HunShouHuaSynergyBehavior extends AbstractGuzhenrenOrganBehavior implements OrganSlowTickListener {

    public static final HunShouHuaSynergyBehavior INSTANCE = new HunShouHuaSynergyBehavior();

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "guzhenren";

    public static final ResourceLocation XIAO_HUN_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xiao_hun_gu");
    public static final ResourceLocation DA_HUN_GU_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "dahungu");

    public static final ResourceLocation ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "synergy/hun_shou_hua");

    private static final String STATE_ROOT_KEY = "HunShouHuaSynergy";
    private static final String KEY_REQUIREMENT_MET = "RequirementMet";
    private static final String KEY_ALREADY_USED = "AlreadyUsed";
    private static final String KEY_AVAILABLE = "Available";
    private static final String KEY_FLOW_ACTIVE = "FlowActive";

    static {
        OrganActivationListeners.register(ABILITY_ID, HunShouHuaSynergyBehavior::activateAbility);
        GuzhenrenLinkageEffectRegistry.registerEffect(
                DA_HUN_GU_ID,
                List.of(DA_HUN_GU_ID, XIAO_HUN_GU_ID),
                context -> context.addSlowTickListener(INSTANCE)
        );
        GuzhenrenLinkageEffectRegistry.registerEffect(
                XIAO_HUN_GU_ID,
                List.of(XIAO_HUN_GU_ID, DA_HUN_GU_ID),
                context -> context.addSlowTickListener(INSTANCE)
        );
    }

    private HunShouHuaSynergyBehavior() {
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || cc == null) {
            return;
        }
        Level level = player.level();
        if (!(level instanceof ServerLevel) || level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack anchor = findAnchorOrgan(cc);
        if (anchor.isEmpty() || !ItemStack.isSameItemSameComponents(anchor, organ)) {
            return;
        }

        boolean hasRequirement = hasRequiredOrgans(cc);
        boolean alreadyUsed = hasConsumedFlag(player);
        FlowController controller = FlowControllerManager.get(serverPlayer);
        boolean flowActive = controller.isRunning();
        boolean available = hasRequirement && !alreadyUsed && !flowActive;

        OrganState state = organState(anchor, STATE_ROOT_KEY);
        OrganStateOps.Collector collector = OrganStateOps.collector(cc, anchor);
        collector.record(state.setBoolean(KEY_REQUIREMENT_MET, hasRequirement, false));
        collector.record(state.setBoolean(KEY_ALREADY_USED, alreadyUsed, false));
        collector.record(state.setBoolean(KEY_AVAILABLE, available, false));
        collector.record(state.setBoolean(KEY_FLOW_ACTIVE, flowActive, false));
        collector.commit();
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof ServerPlayer player) || cc == null || !player.isAlive()) {
            return;
        }
        boolean requirementMet = hasRequiredOrgans(cc);
        if (!requirementMet) {
            player.displayClientMessage(Component.literal("需要同时佩戴小魂蛊与大魂蛊才能施展魂兽化"), true);
            return;
        }
        if (hasConsumedFlag(player)) {
            player.displayClientMessage(Component.literal("魂兽化已完成，无法再次施展"), true);
            return;
        }

        Optional<FlowProgram> programOpt = FlowProgramRegistry.get(HunShouHuaConstants.FLOW_ID);
        if (programOpt.isEmpty()) {
            LOGGER.warn("[compat/guzhenren][hun_dao][synergy] 缺少魂兽化脚本 {}", HunShouHuaConstants.FLOW_ID);
            player.displayClientMessage(Component.literal("魂兽化脚本缺失，暂时无法施展"), true);
            return;
        }

        FlowController controller = FlowControllerManager.get(player);
        controller.setLong(HunShouHuaConstants.FAIL_REASON_VARIABLE, HunShouHuaConstants.FAILURE_REASON_NONE);

        boolean started = controller.start(programOpt.get(), player, 1.0D, Collections.emptyMap(),
                player.level().getGameTime(), "hun_dao.hun_shou_hua");
        if (started) {
            LOGGER.info("[compat/guzhenren][hun_dao][synergy] {} 开始魂兽化流程", player.getScoreboardName());
            player.displayClientMessage(Component.literal("开始引导魂兽化仪式"), true);
            updateAnchorState(cc, requirementMet, false, true);
        } else {
            boolean running = controller.isRunning();
            boolean queued = controller.hasPending();
            if (running || queued) {
                player.displayClientMessage(Component.literal("魂兽化流程正在排队或执行"), true);
            } else {
                player.displayClientMessage(Component.literal("魂兽化无法启动，请稍后重试"), true);
            }
        }
    }

    private static void updateAnchorState(ChestCavityInstance cc, boolean requirementMet, boolean used, boolean flowActive) {
        ItemStack anchor = findAnchorOrgan(cc);
        if (anchor.isEmpty()) {
            return;
        }
        OrganState state = OrganState.of(anchor, STATE_ROOT_KEY);
        OrganStateOps.Collector collector = OrganStateOps.collector(cc, anchor);
        collector.record(state.setBoolean(KEY_REQUIREMENT_MET, requirementMet, false));
        collector.record(state.setBoolean(KEY_ALREADY_USED, used, false));
        collector.record(state.setBoolean(KEY_AVAILABLE, requirementMet && !used && !flowActive, false));
        collector.record(state.setBoolean(KEY_FLOW_ACTIVE, flowActive, false));
        collector.commit();
    }

    private static boolean hasRequiredOrgans(ChestCavityInstance cc) {
        return OrganPresenceUtil.has(cc, XIAO_HUN_GU_ID) && OrganPresenceUtil.has(cc, DA_HUN_GU_ID);
    }

    private static boolean hasConsumedFlag(Player player) {
        return player != null && CCAttachments.getExistingGuScript(player)
                .map(attachment -> attachment.hasAbilityFlag(HunShouHuaConstants.ABILITY_FLAG))
                .orElse(false);
    }

    private static ItemStack findAnchorOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        Item daHunGu = BuiltInRegistries.ITEM.getOptional(DA_HUN_GU_ID).orElse(null);
        Item xiaoHunGu = BuiltInRegistries.ITEM.getOptional(XIAO_HUN_GU_ID).orElse(null);
        ItemStack fallback = ItemStack.EMPTY;
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            Item item = stack.getItem();
            if (daHunGu != null && item == daHunGu) {
                return stack;
            }
            if (fallback.isEmpty() && xiaoHunGu != null && item == xiaoHunGu) {
                fallback = stack;
            }
        }
        return fallback;
    }
}
