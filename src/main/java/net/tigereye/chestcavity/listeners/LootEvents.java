package net.tigereye.chestcavity.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.chestcavities.organs.OrganManager;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.recipes.SalvageRecipe;
import net.tigereye.chestcavity.registration.CCEnchantments;
import net.tigereye.chestcavity.registration.CCItems;
import net.tigereye.chestcavity.registration.CCTags;

@EventBusSubscriber(modid = ChestCavity.MODID)
public final class LootEvents {

  private static final ResourceLocation DESERT_PYRAMID =
      ResourceLocation.parse("minecraft:chests/desert_pyramid");

  private LootEvents() {}

  @SubscribeEvent
  public static void onLivingDrops(LivingDropsEvent event) {
    LivingEntity entity = event.getEntity();
    Level level = entity.level();
    if (level.isClientSide()) {
      return;
    }

    Optional<ChestCavityEntity> optional = ChestCavityEntity.of(entity);
    if (optional.isEmpty()) {
      return;
    }

    ChestCavityInstance cc = optional.get().getChestCavityInstance();
    if (cc.opened) {
      return;
    }

    DamageSource source = event.getSource();
    Entity directKiller = source.getEntity();
    LivingEntity killer = directKiller instanceof LivingEntity living ? living : null;

    HolderLookup<Enchantment> enchantments =
        level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
    Holder<Enchantment> lootingHolder = enchantments.getOrThrow(Enchantments.LOOTING);
    Optional<Holder<Enchantment>> surgicalHolder =
        CCEnchantments.holder(level, CCEnchantments.SURGICAL);
    Optional<Holder<Enchantment>> tomophobiaHolder =
        CCEnchantments.holder(level, CCEnchantments.TOMOPHOBIA);
    Optional<Holder<Enchantment>> malpracticeHolder =
        CCEnchantments.holder(level, CCEnchantments.MALPRACTICE);

    RandomSource random = entity.getRandom();
    int lootingLevel = 0;
    boolean hasButcheringTool = false;

    if (killer != null) {
      if (tomophobiaHolder
          .map(holder -> EnchantmentHelper.getEnchantmentLevel(holder, killer) > 0)
          .orElse(false)) {
        return;
      }

      lootingLevel = EnchantmentHelper.getEnchantmentLevel(lootingHolder, killer);
      lootingLevel +=
          surgicalHolder
                  .map(holder -> EnchantmentHelper.getEnchantmentLevel(holder, killer))
                  .orElse(0)
              * 2;

      ItemStack weapon = killer.getItemInHand(killer.getUsedItemHand());
      if (!weapon.isEmpty() && weapon.is(CCTags.BUTCHERING_TOOL)) {
        lootingLevel = 10 + 10 * lootingLevel;
        hasButcheringTool = true;
      }
    }

    // Generate organ loot when the cavity hasn't been opened manually
    for (ItemStack stack : cc.getChestCavityType().generateLootDrops(cc, random, lootingLevel)) {
      event.getDrops().add(createDrop(entity, stack.copy()));
    }

    if (killer != null) {
      // Process salvage recipes when using butchering tool
      if (hasButcheringTool) {
        processSalvage(event, level, entity, killer);
      }

      // Apply malpractice curse to organ drops when applicable
      if (!killer.getItemInHand(killer.getUsedItemHand()).isEmpty()
          && malpracticeHolder
              .map(
                  holder ->
                      killer.getItemInHand(killer.getUsedItemHand()).getEnchantmentLevel(holder)
                          > 0)
              .orElse(false)) {
        event
            .getDrops()
            .forEach(
                drop -> {
                  ItemStack dropStack = drop.getItem();
                  if (OrganManager.isTrueOrgan(dropStack.getItem())) {
                    malpracticeHolder.ifPresent(holder -> dropStack.enchant(holder, 1));
                  }
                });
      }
    }
  }

  @SubscribeEvent
  public static void onLootTableLoad(LootTableLoadEvent event) {
    LootTable table = event.getTable();
    ResourceLocation name = event.getName();
    if (DESERT_PYRAMID.equals(name)) {
      LootPool primary =
          LootPool.lootPool()
              .setRolls(BinomialDistributionGenerator.binomial(4, 0.25f))
              .add(LootItem.lootTableItem(CCItems.ROTTEN_RIB.get()))
              .build();

      LootPool secondary =
          LootPool.lootPool()
              .setRolls(BinomialDistributionGenerator.binomial(1, 0.3f))
              .add(LootItem.lootTableItem(CCItems.ROTTEN_RIB.get()))
              .build();

      table.addPool(primary);
      table.addPool(secondary);
    }
  }

  private static ItemStack getRecipeOutput(SalvageRecipe recipe, HolderLookup.Provider registries) {
    return recipe.getResultItem(registries).copy();
  }

  private static void processSalvage(
      LivingDropsEvent event, Level level, LivingEntity entity, LivingEntity killer) {
    if (level.getServer() == null) {
      return;
    }

    RecipeManager recipeManager = level.getServer().getRecipeManager();
    Collection<ItemEntity> drops = event.getDrops();
    List<RecipeHolder<CraftingRecipe>> recipes =
        recipeManager.getAllRecipesFor(RecipeType.CRAFTING);
    List<SalvageRecipe> salvageRecipes = new ArrayList<>();
    for (RecipeHolder<CraftingRecipe> holder : recipes) {
      CraftingRecipe recipe = holder.value();
      if (recipe instanceof SalvageRecipe salvageRecipe) {
        salvageRecipes.add(salvageRecipe);
      }
    }

    Map<SalvageRecipe, Integer> salvageCounts = new HashMap<>();
    Iterator<ItemEntity> iterator = drops.iterator();
    while (iterator.hasNext()) {
      ItemEntity drop = iterator.next();
      ItemStack stack = drop.getItem();
      if (stack.is(CCTags.SALVAGEABLE)) {
        for (SalvageRecipe salvage : salvageRecipes) {
          if (salvage.getInput().test(stack)) {
            salvageCounts.merge(salvage, stack.getCount(), Integer::sum);
            iterator.remove();
            break;
          }
        }
      }
    }

    HolderLookup.Provider registries = level.registryAccess();
    Vec3 pos = entity.position();
    salvageCounts.forEach(
        (recipe, total) -> {
          int batches = total / recipe.getRequired();
          if (batches <= 0) {
            return;
          }
          ItemStack output = getRecipeOutput(recipe, registries);
          output.setCount(output.getCount() * batches);
          drops.add(createDrop(level, pos, output));
        });
  }

  private static ItemEntity createDrop(LivingEntity entity, ItemStack stack) {
    return createDrop(entity.level(), entity.position(), stack);
  }

  private static ItemEntity createDrop(Level level, Vec3 pos, ItemStack stack) {
    return new ItemEntity(level, pos.x, pos.y, pos.z, stack);
  }
}
