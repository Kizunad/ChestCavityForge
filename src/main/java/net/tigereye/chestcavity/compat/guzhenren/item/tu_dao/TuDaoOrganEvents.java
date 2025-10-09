package net.tigereye.chestcavity.compat.guzhenren.item.tu_dao;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
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
import net.tigereye.chestcavity.compat.guzhenren.item.tu_dao.behavior.TuQiangGuOrganBehavior;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import org.slf4j.Logger;

/**
 * Handles consumable triggers for Tu Dao organs (e.g. emerald block upgrades).
 */
public final class TuDaoOrganEvents {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean registered;

    private TuDaoOrganEvents() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(TuDaoOrganEvents::onRightClickItem);
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.isCanceled()) {
            return;
        }

        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (player == null || stack.isEmpty()) {
            return;
        }

        if (stack.is(Items.EMERALD_BLOCK)) {
            if (player.pick(5.0D, 0.0F, false).getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                return;
            }
            handleEmeraldBlock(event, player, stack);
        }
    }

    private static void handleEmeraldBlock(PlayerInteractEvent.RightClickItem event, Player player, ItemStack stack) {
        Level level = event.getLevel();
        InteractionHand hand = event.getHand();

        if (level.isClientSide()) {
            player.swing(hand, true);
            player.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7f, 0.95f);
            return;
        }

        ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).ifPresent(cc -> {
            ItemStack organ = TuQiangGuOrganBehavior.INSTANCE.locateOrgan(cc);
            if (organ.isEmpty()) {
                LOGGER.info("[compat/guzhenren][tu_qiang_gu][emerald_consume] owner={} skipped reason=no_organ", player.getScoreboardName());
                return;
            }

            int remaining = TuQiangGuOrganBehavior.INSTANCE.remainingEmeraldUpgrades(organ);
            if (remaining <= 0) {
                player.displayClientMessage(
                        Component.literal("[TuQiangGu] Jade Prison already unlocked"),
                        true
                );
                LOGGER.info(
                        "[compat/guzhenren][tu_qiang_gu][emerald_consume] owner={} skipped reason=already_unlocked",
                        player.getScoreboardName()
                );
                return;
            }

            TuQiangGuOrganBehavior.INSTANCE.onEmeraldBlockConsumed(cc, organ);
            int consumed = TuQiangGuOrganBehavior.INSTANCE.getEmeraldBlocksConsumed(organ);

            ItemStack handStack = player.getItemInHand(hand);
            if (!player.getAbilities().instabuild) {
                handStack.shrink(1);
                if (handStack.isEmpty()) {
                    player.setItemInHand(hand, ItemStack.EMPTY);
                }
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, player.getSoundSource(), 0.7f, 0.95f);
            player.awardStat(Stats.ITEM_USED.get(Items.EMERALD_BLOCK));

            LOGGER.info(
                    "[compat/guzhenren][tu_qiang_gu][emerald_consume] owner={} consumed=1 total={} remaining={}",
                    player.getScoreboardName(),
                    consumed,
                    Math.max(0, 3 - consumed)
            );

            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        });
    }
}
