package net.tigereye.chestcavity.items;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.ChestCavityInventory;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guzhenren.nudao.GuzhenrenNudaoBridge;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.registration.CCItems;
import net.tigereye.chestcavity.registration.CCOrganScores;
import net.tigereye.chestcavity.ui.ChestCavityScreenHandler;
import net.tigereye.chestcavity.util.ChestCavityUtil;

/**
 * The chest opener item.
 */
public class ChestOpener extends Item {

  /** Creates a new ChestOpener. */
  public ChestOpener() {
    super(CCItems.CHEST_OPENER_PROPERTIES);
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
    LivingEntity target = player;
    if (openChestCavity(player, target, false)) {
      return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), false);
    } else {
      return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
  }

  /**
   * Opens the chest cavity of a target entity.
   *
   * @param stack the item stack
   * @param target the target entity
   * @param attacker the attacking entity
   * @param hand the hand used
   * @return the interaction result
   */
  @Override
  public InteractionResult interactLivingEntity(
      ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
    return openChestCavity(player, target, true)
        ? InteractionResult.sidedSuccess(player.level().isClientSide())
        : InteractionResult.PASS;
  }

  /**
   * Opens the chest cavity of the target.
   *
   * @param player The player opening the chest cavity.
   * @param target The target whose chest cavity is being opened.
   */
  public boolean openChestCavity(Player player, LivingEntity target, boolean shouldKnockback) {
    Optional<ChestCavityEntity> optional = ChestCavityEntity.of(target);
    ChestCavity.printOnDebug(
        "ChestOpener.openChestCavity() called! Optional: " + optional.isPresent());
    ChestCavity.printOnDebug("Target Entity: " + target.toString());
    if (optional.isPresent()) {
      ChestCavityEntity chestCavityEntity = optional.get();
      ChestCavityInstance cc = chestCavityEntity.getChestCavityInstance();
      if (target == player || isOwnedByPlayer(player, target) || cc.getChestCavityType().isOpenable(cc)) {
        if (cc.getOrganScore(CCOrganScores.EASE_OF_ACCESS) > 0) {
          if (player.level().isClientSide) {
            player.playNotifySound(SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, .75f, 1);
          }
        } else {
          if (!shouldKnockback) {
            // target.hurt(DamageSource.GENERIC, 4f); // this is to prevent self-knockback, as that
            // feels weird.
          } else {
            // target.hurt(DamageSource.playerAttack(player), 4f);
          }
        }
        if (target.isAlive()) {
          ChestCavityInventory inv = ChestCavityUtil.openChestCavity(cc);
          ChestCavityInstance playerCc = CCAttachments.getChestCavity(player);
          playerCc.ccBeingOpened = cc;
          player.openMenu(
              new SimpleMenuProvider(
                  (i, playerInventory, playerEntity) ->
                      new ChestCavityScreenHandler(i, playerInventory, inv),
                  Component.translatable("gui.chestcavity.chest_cavity", target.getDisplayName())));
        }
        return true;
      } else {
        ChestCavity.printOnDebug(
            () ->
                "ChestOpener prevented: target="
                    + target.getUUID()
                    + " health="
                    + target.getHealth()
                    + "/"
                    + target.getMaxHealth()
                    + " easeOfAccess="
                    + cc.getOrganScore(CCOrganScores.EASE_OF_ACCESS));
        if (player.level().isClientSide) {
          player.displayClientMessage(
              Component.translatable("message.chestcavity.chest_opener.healthy"), true);
          player.playNotifySound(
              SoundEvents.ARMOR_EQUIP_TURTLE.value(), SoundSource.PLAYERS, .75f, 1);
        }
      }
      return false;
    } else {
      return false;
    }
  }

  /**
   * Checks if the target entity is owned by the player.
   *
   * @param player The player to check ownership against.
   * @param target The target entity to check.
   * @return Whether the target is owned by the player.
   */
  private boolean isOwnedByPlayer(Player player, LivingEntity target) {
    // Check TamableAnimal (tamed animals like wolves, cats)
    if (target instanceof TamableAnimal tamable) {
      return tamable.isOwnedBy(player);
    }

    // Check OwnableEntity (summonable entities like iron golems)
    if (target instanceof OwnableEntity ownable) {
      // 优先用实体引用比较；若为空或在客户端侧失败，则回退到 UUID 比较
      LivingEntity owner = ownable.getOwner();
      if (owner != null) {
        return owner.getUUID().equals(player.getUUID());
      }
      java.util.UUID id = ownable.getOwnerUUID();
      return id != null && id.equals(player.getUUID());
    }

    // Check Guzhenren mod custom owner relationship
    return GuzhenrenNudaoBridge.openSubject(target)
        .map(handle -> handle.isOwnedBy(player))
        .orElse(false);
  }
}
