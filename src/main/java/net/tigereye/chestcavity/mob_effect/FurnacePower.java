package net.tigereye.chestcavity.mob_effect;



import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectType;
import net.minecraft.util.FoodStats;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.registration.CCItems;
import net.tigereye.chestcavity.mob_effect.fx.FurnaceFlowState;
import net.tigereye.chestcavity.mob_effect.fx.FurnacePowerFxHelper;

import java.util.Optional;

public class FurnacePower extends CCStatusEffect{

    public FurnacePower(){
        super(EffectType.BENEFICIAL, 0xC8FF00);
    }

    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if(entity instanceof PlayerEntity){
            if(!(entity.level.isClientSide)) {
                Optional<ChestCavityEntity> optional = ChestCavityEntity.of(entity);
                if (optional.isPresent()) {
                    ChestCavityEntity cce = optional.get();
                    ChestCavityInstance cc = cce.getChestCavityInstance();
                    cc.furnaceProgress++;
                    if (cc.furnaceProgress % FurnacePowerFxHelper.CHARGE_SOUND_INTERVAL == 0) {
                        FurnacePowerFxHelper.playFx(entity, FurnaceFlowState.CHARGING);
                    }
                    if (cc.furnaceProgress >= 200) {
                        cc.furnaceProgress = 0;
                        FoodStats hungerManager = ((PlayerEntity) entity).getFoodData();
                        ItemStack furnaceFuel = new ItemStack(CCItems.FURNACE_POWER.get());
                        for (int i = 0; i <= amplifier; i++) {
                            hungerManager.eat(CCItems.FURNACE_POWER.get(), furnaceFuel);
                        }
                        FurnacePowerFxHelper.playFx(entity, FurnaceFlowState.FEEDING);
                    }
                }
            }
        }
    }
}
