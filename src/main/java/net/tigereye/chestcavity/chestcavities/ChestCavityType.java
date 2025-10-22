package net.tigereye.chestcavity.chestcavities;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.chestcavities.organs.OrganData;

public interface ChestCavityType {

  public Map<ResourceLocation, Float> getDefaultOrganScores();

  public float getDefaultOrganScore(ResourceLocation id);

  public ChestCavityInventory getDefaultChestCavity();

  public boolean isSlotForbidden(int index);

  public void fillChestCavityInventory(ChestCavityInventory chestCavity);

  public void loadBaseOrganScores(Map<ResourceLocation, Float> organScores);

  public OrganData catchExceptionalOrgan(ItemStack slot);

  public List<ItemStack> generateLootDrops(RandomSource random, int looting);

  default void applyRandomFillers(
      ChestCavityInstance instance, ChestCavityInventory inventory, RandomSource random) {}

  public void setOrganCompatibility(ChestCavityInstance instance);

  public float getHeartBleedCap();

  public boolean isOpenable(ChestCavityInstance instance);

  public void onDeath(ChestCavityInstance instance);
}
