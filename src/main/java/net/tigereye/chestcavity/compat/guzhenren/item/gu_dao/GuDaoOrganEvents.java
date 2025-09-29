package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

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
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.GuzhuguOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.SteelBoneComboHelper;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.util.ChestCavityUtil;

/**
 * Handles player interactions relevant to 骨道蛊 organs.
 */
public final class GuDaoOrganEvents {

    private static boolean registered;

    private GuDaoOrganEvents() {
    }

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

        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (player == null || stack.isEmpty()) {
            return;
        }

        if (stack.is(Items.BONE_MEAL)) {
            if (player.pick(5.0D, 0.0F, false).getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                return;
            }
            handleBoneMeal(event, player, stack);
            return;
        }

        if (stack.is(Items.IRON_INGOT)) {
            if (player.pick(5.0D, 0.0F, false).getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                return;
            }
            handleIronIngot(event, player, stack);
        }
    }

    private static void handleBoneMeal(PlayerInteractEvent.RightClickItem event, Player player, ItemStack stack) {
        Level level = event.getLevel();
        InteractionHand hand = event.getHand();

        if (level.isClientSide()) {
            player.swing(hand, true);
            player.playSound(SoundEvents.GENERIC_EAT, 0.6f, 1.2f);
            return;
        }

        ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).ifPresent(cc -> {
            boolean applied = GuzhuguOrganBehavior.INSTANCE.onBoneMealCatalyst(player, cc);
            if (!applied) {
                player.displayClientMessage(
                        Component.literal("[Guzhugu] ACTIVE blocked (cooldown or missing organ)"),
                        true
                );
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

    private static void handleIronIngot(PlayerInteractEvent.RightClickItem event, Player player, ItemStack stack) {
        Level level = event.getLevel();
        InteractionHand hand = event.getHand();

        if (level.isClientSide()) {
            player.swing(hand, true);
            player.playSound(SoundEvents.ANVIL_PLACE, 0.6f, 1.2f);
            return;
        }

        ChestCavityEntity.of(player).map(ChestCavityEntity::getChestCavityInstance).ifPresent(cc -> {
            var comboState = SteelBoneComboHelper.analyse(cc);
            if (!SteelBoneComboHelper.hasActiveCombo(comboState)) {
                player.displayClientMessage(
                        Component.literal("[Gangjingu] Iron repair requires steel + iron bone"),
                        true
                );
                return;
            }
            if (player.getHealth() >= player.getMaxHealth() - 0.01f) {
                return;
            }

            ItemStack handStack = player.getItemInHand(hand);
            if (!player.getAbilities().instabuild) {
                if (handStack.isEmpty()) {
                    return;
                }
                handStack.shrink(1);
                if (handStack.isEmpty()) {
                    player.setItemInHand(hand, ItemStack.EMPTY);
                }
            }

            float healAmount = player.getMaxHealth() * 0.10f;
            ChestCavityUtil.runWithOrganHeal(() -> player.heal(healAmount));

            player.getCooldowns().addCooldown(Items.IRON_INGOT, 20);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ANVIL_PLACE, player.getSoundSource(), 0.7f, 1.0f);
            player.playSound(SoundEvents.GRINDSTONE_USE, 0.5f, 1.3f);
            player.awardStat(Stats.ITEM_USED.get(Items.IRON_INGOT));

            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        });
    }
}

