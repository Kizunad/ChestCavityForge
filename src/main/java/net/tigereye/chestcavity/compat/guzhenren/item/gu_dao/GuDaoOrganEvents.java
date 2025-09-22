package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.GuzhuguOrganBehavior;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;

/**
 * Handles player interactions relevant to 骨道蛊 organs.
 * - 对着方块：保持 vanilla 骨粉逻辑
 * - 对着空气：模拟“吃骨粉”，触发 GuzhuguOrganBehavior
 */
public final class GuDaoOrganEvents {

    private static boolean registered;

    private GuDaoOrganEvents() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(GuDaoOrganEvents::onRightClickItem);
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.isCanceled()) {
            return;
        }
        handleBoneMeal(event);
    }

    private static void handleBoneMeal(PlayerInteractEvent.RightClickItem event) {
        Level level = event.getLevel();
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        InteractionHand hand = event.getHand();

        if (stack.isEmpty() || player == null) {
            return;
        }
        if (!stack.is(Items.BONE_MEAL)) {
            return;
        }

        // 检测是否对准方块
        var hitResult = player.pick(5.0D, 0.0F, false);
        boolean aimingAtBlock = hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK;

        // ---------- 对空气 ----------
        if (!aimingAtBlock) {
            if (level.isClientSide()) {
                // 客户端：手动播动作/声音
                player.swing(hand, true);
                player.playSound(SoundEvents.GENERIC_EAT, 0.6f, 1.2f);
                return;
            }

            // 服务端：应用骨道逻辑
            ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).ifPresent(cc -> {
                boolean applied = GuzhuguOrganBehavior.INSTANCE.onBoneMealCatalyst(player, cc);
                if (!applied) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("[Guzhugu] ACTIVE blocked (cooldown or missing organ)"), true);
                    return;
                }

                ItemStack handStack = player.getItemInHand(hand);
                if (!player.getAbilities().instabuild) {
                    handStack.shrink(1);
                    if (handStack.isEmpty()) {
                        player.setItemInHand(hand, ItemStack.EMPTY);
                    }
                }

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.GENERIC_EAT, player.getSoundSource(), 0.6f, 1.2f);

                player.awardStat(Stats.ITEM_USED.get(Items.BONE_MEAL));

                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            });
        }
        // ---------- 对方块 ----------
        // 不拦截，交给 vanilla 骨粉逻辑
    }
}
