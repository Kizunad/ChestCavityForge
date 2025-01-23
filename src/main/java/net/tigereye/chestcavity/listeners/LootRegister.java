package net.tigereye.chestcavity.listeners;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.chestcavities.organs.OrganManager;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.recipes.SalvageRecipe;
import net.tigereye.chestcavity.registration.CCEnchantments;
import net.tigereye.chestcavity.registration.CCItems;
import net.tigereye.chestcavity.registration.CCTags;

import java.util.*;

@Mod.EventBusSubscriber(modid = ChestCavity.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LootRegister {

    private static final ResourceLocation DESERT_PYRAMID_LOOT_TABLE_ID = new ResourceLocation("minecraft", "chests/desert_pyramid");
    private static List<SalvageRecipe> salvageRecipeList;

    public static List<ItemStack> addLoot(LootContext lootContext) {
        List<ItemStack> loot = new ArrayList<>();
        if (lootContext.hasParam(LootContextParams.LAST_DAMAGE_PLAYER)) {
            int lootingLevel;
            Random random;
            Entity entity = lootContext.getParam(LootContextParams.THIS_ENTITY);
            Optional<ChestCavityEntity> chestCavityEntity = ChestCavityEntity.of(entity);
            //check that the entity does have a chest cavity
            if (!chestCavityEntity.isPresent()) {
                return loot;
            }
            ChestCavityInstance cc = chestCavityEntity.get().getChestCavityInstance();
            //check if loot is already generated due to having opened the target's chest cavity
            if (cc.opened) {
                return loot;
            }
            //get looting level and random
            if (lootContext.hasParam(LootContextParams.KILLER_ENTITY)) {
                Entity killer_ = lootContext.getParam(LootContextParams.KILLER_ENTITY);
                //check if loot is forbidden due to malpractice
                if (killer_ instanceof LivingEntity killer) {
                    if (EnchantmentHelper.getEnchantmentLevel(CCEnchantments.TOMOPHOBIA.get(), killer) > 0) {
                        return loot;
                    }
                    lootingLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.MOB_LOOTING, killer);
                    lootingLevel += EnchantmentHelper.getEnchantmentLevel(CCEnchantments.SURGICAL.get(), killer) * 2;
                    if (killer.getItemInHand(killer.getUsedItemHand()).is(CCTags.BUTCHERING_TOOL)) {
                        lootingLevel = 10 + 10 * lootingLevel;
                    }
                    random = lootContext.getRandom();
                } else {
                    lootingLevel = 0;
                    random = new Random();
                }
                //with all this passed, finally we ask the chest cavity manager what the loot will actually be.
                loot.addAll(cc.getChestCavityType().generateLootDrops(random, lootingLevel));
            }
        }
        return loot;
    }



    public static List<ItemStack> modifyLoot(List<ItemStack> loot, LootContext lootContext) {
        if (lootContext.hasParam(LootContextParams.KILLER_ENTITY)) {
            Entity killer_ = lootContext.getParam(LootContextParams.KILLER_ENTITY);
            if(killer_ instanceof LivingEntity killer) {
                if (killer.getItemInHand(killer.getUsedItemHand()).is(CCTags.BUTCHERING_TOOL)) {
                    //first, remove everything that can be salvaged from the loot and count them up
                    Map<SalvageRecipe, Integer> salvageResults = new HashMap<>();
                    Iterator<ItemStack> i = loot.iterator();
                    if (salvageRecipeList == null) {
                        salvageRecipeList = new ArrayList<>();
                        List<CraftingRecipe> recipes = killer.level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING);
                        for (CraftingRecipe recipe : recipes) {
                            if (recipe instanceof SalvageRecipe) {
                                salvageRecipeList.add((SalvageRecipe) recipe);
                            }
                        }
                    }
                    while (i.hasNext()) {
                        ItemStack stack = i.next();
                        if (stack.is(CCTags.SALVAGEABLE)) {
                            for (SalvageRecipe recipe : salvageRecipeList) {
                                if (recipe.getInput().test(stack)) {
                                    salvageResults.put(recipe, salvageResults.getOrDefault(recipe, 0) + stack.getCount());
                                    i.remove();
                                    break;
                                }
                            }
                        }
                    }
                    //then, get the output of the salvage and add it to the loot
                    salvageResults.forEach((recipe, count) -> {
                        ItemStack out = recipe.getResultItem();
                        out.setCount(out.getCount() * (count / recipe.getRequired()));
                        loot.add(out);
                    });
                }


                //organs gain malpractice
                if (EnchantmentHelper.getItemEnchantmentLevel(CCEnchantments.MALPRACTICE.get(), killer.getItemInHand(killer.getUsedItemHand())) > 0) {
                    for (ItemStack stack : loot) {
                        if (OrganManager.isTrueOrgan(stack.getItem())) {
                            stack.enchant(CCEnchantments.MALPRACTICE.get(), 1);
                        }
                    }
                }
            }
        }
        return loot;
    }

    @SubscribeEvent
    public static void registerDesertPyramidLoot(LootTableLoadEvent event) {
        //Use a mixin to get when every loot table is loaded? Might have to
        ResourceLocation id = event.getTable().getLootTableId();
        LootTable table = event.getTable();
         if (DESERT_PYRAMID_LOOT_TABLE_ID.equals(id)) {
             LootPool.Builder poolBuilder = new LootPool.Builder()
                     .setRolls(BinomialDistributionGenerator.binomial(4,.25f))
                     .add(LootItem.lootTableItem(CCItems.ROTTEN_RIB.get()));
             table.addPool(poolBuilder.build());
             poolBuilder = new LootPool.Builder()
                     .setRolls(BinomialDistributionGenerator.binomial(1,.3f))
                     .add(LootItem.lootTableItem(CCItems.ROTTEN_RIB.get()));
             table.addPool(poolBuilder.build());
         }
     }
}