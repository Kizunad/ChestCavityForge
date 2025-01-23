package net.tigereye.chestcavity.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.listeners.EffectiveFoodScores;
import net.tigereye.chestcavity.listeners.OrganFoodCallback;
import net.tigereye.chestcavity.registration.CCOrganScores;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FoodData.class)
public class MixinHungerManager {
        static {
                System.out.println("FoodData Mixin initializing!");
        }

        @Shadow
        private int tickTimer;
        //@Shadow
        //private int foodLevel;
        //@Shadow
        //private float saturationLevel;
        @Shadow
        private float exhaustionLevel;

        private ChestCavityEntity CC_player = null;

        @Inject(at = @At("HEAD"), method = "tick", cancellable = true)
        public void chestCavityUpdateMixin(Player player, CallbackInfo ci) {
                if(CC_player == null){
                        ChestCavityEntity.of(player).ifPresent(ccPlayerEntityInterface -> CC_player = ccPlayerEntityInterface);
                }
                tickTimer = ChestCavityUtil.applySpleenMetabolism(CC_player.getChestCavityInstance(),this.tickTimer);
        }


        @ModifyArgs(method = "eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"))
        public void chestCavityEatMixin(Args args, Item item, ItemStack stack,  LivingEntity entity) {
                if(item.isEdible() && CC_player != null){
                        //saturation gains are equal to hungerValue*saturationModifier*2
                        //this is kinda stupid, if I half the hunger gains from food I don't want to also half saturation gains
                        //so before hunger changes, calculate the saturation gain I intend
                        FoodProperties itemFoodComponent = item.getFoodProperties();
                        if(itemFoodComponent != null) {
                                EffectiveFoodScores efs = new EffectiveFoodScores(
                                        CC_player.getChestCavityInstance().getOrganScore(CCOrganScores.DIGESTION),
                                        CC_player.getChestCavityInstance().getOrganScore(CCOrganScores.NUTRITION));
                                efs = OrganFoodCallback.addFoodListener(item, itemFoodComponent, CC_player, efs);
                                float saturationGain = ChestCavityUtil.applyNutrition(CC_player.getChestCavityInstance(),efs.nutrition,item.getFoodProperties().getSaturationModifier())
                                         * item.getFoodProperties().getNutrition() * 2.0F;
                                //now find the modified hunger gains
                                int hungerGain = ChestCavityUtil.applyDigestion(CC_player.getChestCavityInstance(),efs.digestion,item.getFoodProperties().getNutrition());
                                //now calculate the saturation modifier that gives me what I want
                                float newSaturation = saturationGain / (hungerGain * 2);
                                args.set(0,hungerGain);
                                args.set(1,newSaturation);
                                //now make a dummy food item with the modified stats and feed it to HungerManager.eat();
                                //FoodComponent dummyFood = new FoodComponent.Builder().hunger(hungerGain).saturationModifier(newSaturation).build();
                                //Item.Settings dummySettings = new Item.Settings().food(dummyFood);
                                //return new Item(dummySettings);
                        }
                }
        }

        @ModifyVariable(at = @At("HEAD"), ordinal = 0, method = "addExhaustion", argsOnly = true)
        public float chestCavityAddExhaustionMixin(float exhaustion) {
                if(CC_player != null){
                        if(this.exhaustionLevel != this.exhaustionLevel){
                                //NaN check. Not sure what keep causing it...
                                this.exhaustionLevel = 0;
                        }
                        float enduranceDif = CC_player.getChestCavityInstance().getOrganScore(CCOrganScores.ENDURANCE)-CC_player.getChestCavityInstance().getChestCavityType().getDefaultOrganScore(CCOrganScores.ENDURANCE);
                        //ChestCavity.LOGGER.info("In: "+exhaustion);
                        float out;
                        if(enduranceDif > 0) {
                                out = exhaustion/(1+(enduranceDif/2));
                        }
                        else{
                                out = exhaustion*(1-(enduranceDif/2));
                        }
                        //float out = exhaustion*(float)Math.pow(ChestCavity.config.LUNG_ENDURANCE,enduranceDif/2);
                        //ChestCavity.LOGGER.info("Out: "+out);
                        return out;
                }
                return exhaustion;
        }
}