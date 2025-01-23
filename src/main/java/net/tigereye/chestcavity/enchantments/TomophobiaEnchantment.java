package net.tigereye.chestcavity.enchantments;


import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.tigereye.chestcavity.registration.CCEnchantments;

public class TomophobiaEnchantment extends Enchantment {
    public TomophobiaEnchantment(){
        super(Rarity.VERY_RARE, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    public int getMinCost(int level) {
        return 25;
    }

    public int getMaxCost(int level) {
        return 50;
    }

    public int getMaxLevel() {
        return 1;
    }

    public boolean isTreasureOnly() {
        return true;
    }

    public boolean checkCompatibility(Enchantment other) {
        return super.checkCompatibility(other)
                && other != CCEnchantments.SURGICAL.get()
                && other != CCEnchantments.MALPRACTICE.get();
    }
}