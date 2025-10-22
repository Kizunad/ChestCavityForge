package net.tigereye.chestcavity.items;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.registration.CCFoodComponents;
import net.tigereye.chestcavity.registration.CCStatusEffects;
import net.tigereye.chestcavity.util.CommonOrganUtil;

public class VenomGland extends Item implements OrganOnHitListener {

  public VenomGland() {
    super(new Properties().stacksTo(1).food(CCFoodComponents.RAW_TOXIC_ORGAN_MEAT_FOOD_COMPONENT));
  }

  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    Entity directEntity = source.getDirectEntity();
    boolean isNonLlamaProjectile = directEntity != null && !(directEntity instanceof LlamaSpit);
    if (attacker.getItemInHand(attacker.getUsedItemHand()).isEmpty() || !isNonLlamaProjectile) {
      if (isNonLlamaProjectile) {
        return damage;
      }
      // venom glands don't trigger if they are on cooldown,
      // unless that cooldown was applied this same tick
      if (attacker.hasEffect(CCStatusEffects.VENOM_COOLDOWN)) {
        MobEffectInstance cooldown = attacker.getEffect(CCStatusEffects.VENOM_COOLDOWN);
        // this is to check if the cooldown was inflicted this same tick; likely because of other
        // venom glands
        if (cooldown.getDuration() != ChestCavity.config.VENOM_COOLDOWN) {
          return damage;
        }
      }
      // failure conditions passed, the venom gland now delivers its payload
      List<MobEffectInstance> effects = CommonOrganUtil.getStatusEffects(organ);
      if (!effects.isEmpty()) {
        for (MobEffectInstance effect : effects) {
          target.addEffect(effect);
        }
      } else {
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
      }
      attacker.addEffect(
          new MobEffectInstance(
              CCStatusEffects.VENOM_COOLDOWN, ChestCavity.config.VENOM_COOLDOWN, 0));
      if (attacker instanceof Player) {
        ((Player) attacker).causeFoodExhaustion(.1f);
      }
    }
    return damage;
  }

  @Override
  public void appendHoverText(
      ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    super.appendHoverText(stack, context, tooltip, flag);
    CommonOrganUtil.getStatusEffects(stack)
        .forEach(
            effect -> {
              Component name = Component.translatable(effect.getDescriptionId());
              int amplifier = effect.getAmplifier() + 1;
              tooltip.add(
                  Component.translatable("tooltip.chestcavity.venom_gland.effect", name, amplifier)
                      .withStyle(ChatFormatting.GREEN));
            });
  }
}
