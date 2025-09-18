package net.tigereye.chestcavity.registration;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryObject;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.enchantments.MalpracticeCurseEnchantment;
import net.tigereye.chestcavity.enchantments.ONegativeEnchantment;
import net.tigereye.chestcavity.enchantments.SurgicalEnchantment;
import net.tigereye.chestcavity.enchantments.TomophobiaEnchantment;

public class CCEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(Registries.ENCHANTMENT, ChestCavity.MODID);

    public static final RegistryObject<Enchantment> O_NEGATIVE = ENCHANTMENTS.register("o_negative", ONegativeEnchantment::new);
    public static final RegistryObject<Enchantment> SURGICAL = ENCHANTMENTS.register("surgical", SurgicalEnchantment::new);
    public static final RegistryObject<Enchantment> MALPRACTICE = ENCHANTMENTS.register("malpractice", MalpracticeCurseEnchantment::new);
    public static final RegistryObject<Enchantment> TOMOPHOBIA = ENCHANTMENTS.register("tomophobia", TomophobiaEnchantment::new);
}
