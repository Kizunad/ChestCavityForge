package net.tigereye.chestcavity.registration;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tigereye.chestcavity.ChestCavity;

public class CCEnchantments {
  public static final DeferredRegister<Enchantment> ENCHANTMENTS =
      DeferredRegister.create(Registries.ENCHANTMENT, ChestCavity.MODID);

  public static final DeferredHolder<Enchantment, Enchantment> O_NEGATIVE =
      ENCHANTMENTS.register(
          "o_negative",
          () ->
              weaponEnchantment(
                  "o_negative",
                  2,
                  2,
                  4,
                  Enchantment.dynamicCost(50, 50),
                  Enchantment.dynamicCost(100, 50)));

  public static final DeferredHolder<Enchantment, Enchantment> SURGICAL =
      ENCHANTMENTS.register(
          "surgical",
          () ->
              weaponEnchantment(
                  "surgical",
                  1,
                  3,
                  4,
                  Enchantment.dynamicCost(15, 9),
                  Enchantment.dynamicCost(65, 9)));

  public static final DeferredHolder<Enchantment, Enchantment> TOMOPHOBIA =
      ENCHANTMENTS.register(
          "tomophobia",
          () ->
              weaponEnchantment(
                  "tomophobia",
                  1,
                  1,
                  8,
                  Enchantment.constantCost(25),
                  Enchantment.constantCost(50)));

  public static final DeferredHolder<Enchantment, Enchantment> MALPRACTICE =
      ENCHANTMENTS.register(
          "malpractice",
          () ->
              weaponEnchantment(
                  "malpractice",
                  1,
                  1,
                  8,
                  Enchantment.constantCost(25),
                  Enchantment.constantCost(50)));

  public static Optional<Holder<Enchantment>> holder(
      Level level, DeferredHolder<Enchantment, Enchantment> reference) {
    return resolveHolder(level.registryAccess(), reference);
  }

  public static Optional<Holder<Enchantment>> holder(
      HolderLookup.Provider provider, DeferredHolder<Enchantment, Enchantment> reference) {
    return resolveHolder(provider, reference);
  }

  public static Optional<Holder<Enchantment>> resolveHolder(
      HolderLookup.Provider provider, DeferredHolder<Enchantment, Enchantment> reference) {
    return provider
        .lookup(Registries.ENCHANTMENT)
        .flatMap(registry -> registry.get(reference.getKey()).map(holder -> holder));
  }

  private static Enchantment weaponEnchantment(
      String name,
      int weight,
      int maxLevel,
      int anvilCost,
      Enchantment.Cost minCost,
      Enchantment.Cost maxCost) {
    HolderSet<Item> weapons = BuiltInRegistries.ITEM.getOrCreateTag(ItemTags.WEAPON_ENCHANTABLE);
    var definition =
        Enchantment.definition(
            weapons, weight, maxLevel, minCost, maxCost, anvilCost, EquipmentSlotGroup.MAINHAND);
    return Enchantment.enchantment(definition).build(ChestCavity.id(name));
  }
}
