package net.tigereye.chestcavity.items;


import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.ChestCavityInventory;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.registration.CCItems;
import net.tigereye.chestcavity.registration.CCOrganScores;
import net.tigereye.chestcavity.ui.ChestCavityScreenHandler;
import net.tigereye.chestcavity.util.ChestCavityUtil;

import java.util.Optional;

public class ChestOpener extends Item {
	
	public ChestOpener() {
		super(CCItems.CHEST_OPENER_PROPERTIES);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		LivingEntity target = player;
		if (openChestCavity(player, target,false)) {
			return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), false);
		} else {
			return InteractionResultHolder.pass(player.getItemInHand(hand));
		}
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		return openChestCavity(player, target, true)
				? InteractionResult.sidedSuccess(player.level().isClientSide())
				: InteractionResult.PASS;
	}

	public boolean openChestCavity(Player player, LivingEntity target){
		return openChestCavity(player,target,true);
	}
	public boolean openChestCavity(Player player, LivingEntity target, boolean shouldKnockback){
		Optional<ChestCavityEntity> optional = ChestCavityEntity.of(target);
		ChestCavity.printOnDebug("ChestOpener.openChestCavity() called! Optional: " + optional.isPresent());
		ChestCavity.printOnDebug("Target Entity: " + target.toString());
		if(optional.isPresent()){
			ChestCavityEntity chestCavityEntity = optional.get();
			ChestCavityInstance cc = chestCavityEntity.getChestCavityInstance();
			if(target == player || cc.getChestCavityType().isOpenable(cc)) {
				if (cc.getOrganScore(CCOrganScores.EASE_OF_ACCESS) > 0) {
					if(player.level().isClientSide) {
						player.playNotifySound(SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, .75f, 1);
					}
				}
				else{
					if (!shouldKnockback) {
						//target.hurt(DamageSource.GENERIC, 4f); // this is to prevent self-knockback, as that feels weird.
					} else {
						//target.hurt(DamageSource.playerAttack(player), 4f);
					}
				}
				if (target.isAlive()) {
					ChestCavityInventory inv = ChestCavityUtil.openChestCavity(cc);
					ChestCavityInstance playerCC = CCAttachments.getChestCavity(player);
					playerCC.ccBeingOpened = cc;
                    player.openMenu(new SimpleMenuProvider((i, playerInventory, playerEntity) -> new ChestCavityScreenHandler(i, playerInventory, inv), Component.translatable("gui.chestcavity.chest_cavity", target.getDisplayName())));
				}
				return true;
			}
			else{
					if(player.level().isClientSide) {
						if (!target.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
                        player.displayClientMessage(Component.translatable("message.chestcavity.chest_opener.obstructed"), true);
						player.playNotifySound(SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, .75f, 1);
					} else {
                        player.displayClientMessage(Component.translatable("message.chestcavity.chest_opener.healthy"), true);
						player.playNotifySound(SoundEvents.ARMOR_EQUIP_TURTLE.value(), SoundSource.PLAYERS, .75f, 1);
					}
				}
			}
			return false;
		}
		else{
			return false;
		}
	}
}
