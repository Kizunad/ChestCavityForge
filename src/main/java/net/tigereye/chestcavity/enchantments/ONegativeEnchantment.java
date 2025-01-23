package net.tigereye.chestcavity.enchantments;


import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class ONegativeEnchantment extends Enchantment {

    public ONegativeEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.VANISHABLE, new EquipmentSlot[]{});
    }


    public int getMinCost(int level) {
        return 50*level;
    }

    public int getMaxCost(int level) {
        return 100*level;
    }

    public int getMaxLevel() {
        return 2;
    }

    public boolean canEnchant(ItemStack stack) {
        return true;
    }

    public boolean isTreasureOnly() {
        return true;
    }

    public boolean isTradeable() {
        return false;
    }

    public boolean checkCompatibility(Enchantment other) {
        return super.checkCompatibility(other);
    }



}
