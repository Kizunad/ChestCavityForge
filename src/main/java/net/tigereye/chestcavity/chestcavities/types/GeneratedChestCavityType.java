package net.tigereye.chestcavity.chestcavities.types;

import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.ChestCavityInventory;
import net.tigereye.chestcavity.chestcavities.ChestCavityType;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance.RandomFillerEntry;
import net.tigereye.chestcavity.chestcavities.organs.OrganData;
import net.tigereye.chestcavity.chestcavities.organs.OrganManager;
import net.tigereye.chestcavity.registration.CCOrganScores;
import net.tigereye.chestcavity.util.ChestCavityUtil;

public class GeneratedChestCavityType implements ChestCavityType {

  private Map<ResourceLocation, Float> defaultOrganScores = null;
  private ChestCavityInventory defaultChestCavity = null;
  private Map<ResourceLocation, Float> baseOrganScores = null;
  private Map<Ingredient, Map<ResourceLocation, Float>> exceptionalOrganList = null;
  private List<ItemStack> droppableOrgans = null;
  private List<Integer> forbiddenSlots = new ArrayList<>();
  private List<RandomChestCavityFiller> randomFillers = Collections.emptyList();
  private float dropRateMultiplier = 1;
  private boolean bossChestCavity = false;
  private boolean playerChestCavity = false;

  public GeneratedChestCavityType() {}

  @Override
  public Map<ResourceLocation, Float> getDefaultOrganScores() {
    if (defaultOrganScores == null) {
      defaultOrganScores = new HashMap<>();
      if (!ChestCavityUtil.determineDefaultOrganScores(this)) {
        defaultOrganScores = null;
      }
    }
    return defaultOrganScores;
  }

  @Override
  public float getDefaultOrganScore(ResourceLocation id) {
    return getDefaultOrganScores().getOrDefault(id, 0f);
  }

  @Override
  public ChestCavityInventory getDefaultChestCavity() {
    return defaultChestCavity;
  }

  public void setDefaultChestCavity(ChestCavityInventory inv) {
    defaultChestCavity = inv;
  }

  public Map<ResourceLocation, Float> getBaseOrganScores() {
    return baseOrganScores;
  }

  public float getBaseOrganScore(ResourceLocation id) {
    return getBaseOrganScores().getOrDefault(id, 0f);
  }

  public void setBaseOrganScores(Map<ResourceLocation, Float> organScores) {
    baseOrganScores = organScores;
  }

  public void setBaseOrganScore(ResourceLocation id, float score) {
    baseOrganScores.put(id, score);
  }

  public Map<Ingredient, Map<ResourceLocation, Float>> getExceptionalOrganList() {
    return exceptionalOrganList;
  }

  public Map<ResourceLocation, Float> getExceptionalOrganScore(ItemStack itemStack) {
    for (Ingredient ingredient : getExceptionalOrganList().keySet()) {
      if (ingredient.test(itemStack)) {
        return getExceptionalOrganList().get(ingredient);
      }
    }
    return null;
  }

  public void setExceptionalOrganList(Map<Ingredient, Map<ResourceLocation, Float>> list) {
    exceptionalOrganList = list;
  }

  public void setExceptionalOrgan(Ingredient ingredient, Map<ResourceLocation, Float> scores) {
    exceptionalOrganList.put(ingredient, scores);
  }

  public List<ItemStack> getDroppableOrgans() {
    if (droppableOrgans == null) {
      deriveDroppableOrgans();
    }
    return droppableOrgans;
  }

  public void setDroppableOrgans(List<ItemStack> list) {
    droppableOrgans = list;
  }

  private void deriveDroppableOrgans() {
    droppableOrgans = new LinkedList<>();
    for (int i = 0; i < defaultChestCavity.getContainerSize(); i++) {
      ItemStack stack = defaultChestCavity.getItem(i);
      if (OrganManager.isTrueOrgan(stack.getItem())) {
        droppableOrgans.add(stack);
      }
    }
  }

  public List<Integer> getForbiddenSlots() {
    return forbiddenSlots;
  }

  public void setForbiddenSlots(List<Integer> list) {
    forbiddenSlots = list;
  }

  public void forbidSlot(int slot) {
    forbiddenSlots.add(slot);
  }

  public void allowSlot(int slot) {
    int index = forbiddenSlots.indexOf(slot);
    if (index != -1) {
      forbiddenSlots.remove(index);
    }
  }

  public void setRandomFillers(List<RandomChestCavityFiller> fillers) {
    randomFillers = Objects.requireNonNullElseGet(fillers, Collections::emptyList);
  }

  public List<RandomChestCavityFiller> getRandomFillers() {
    return randomFillers;
  }

  @Override
  public boolean isSlotForbidden(int index) {
    return forbiddenSlots.contains(index);
  }

  public boolean isBossChestCavity() {
    return bossChestCavity;
  }

  public void setBossChestCavity(boolean bool) {
    bossChestCavity = bool;
  }

  public boolean isPlayerChestCavity() {
    return playerChestCavity;
  }

  public void setPlayerChestCavity(boolean bool) {
    playerChestCavity = bool;
  }

  @Override
  public void fillChestCavityInventory(ChestCavityInventory chestCavity) {
    chestCavity.clearContent();
    for (int i = 0; i < chestCavity.getContainerSize(); i++) {
      chestCavity.setItem(i, defaultChestCavity.getItem(i));
    }
  }

  @Override
  public void applyRandomFillers(
      ChestCavityInstance instance, ChestCavityInventory inventory, RandomSource random) {
    if (randomFillers.isEmpty()) {
      return;
    }
    for (RandomChestCavityFiller filler : randomFillers) {
      filler.fill(inventory, random);
    }
  }

  /**
   * 在实体生成或缓存缺失时调用，根据默认胸腔模板预先运行所有随机生成器，并将结果保存到实例中。
   *
   * <p>该方法确保随机生成的物品只决定一次：若实体已经带有缓存数据，则不会重复抽取，从而保证掉落、 打开胸腔等场景看到的内容保持一致。
   */
  public void preGenerateRandomFillers(ChestCavityInstance instance, RandomSource random) {
    if (instance == null || instance.areRandomFillersGenerated() || randomFillers.isEmpty()) {
      return;
    }
    ChestCavityInventory defaults = getDefaultChestCavity();
    if (defaults == null) {
      instance.replacePreGeneratedRandomFillers(Collections.emptyList());
      instance.setRandomFillersGenerated(true);
      return;
    }

    List<Integer> emptySlots = new ArrayList<>();
    for (int i = 0; i < defaults.getContainerSize(); i++) {
      if (defaults.getItem(i).isEmpty()) {
        emptySlots.add(i);
      }
    }

    if (emptySlots.isEmpty()) {
      instance.replacePreGeneratedRandomFillers(Collections.emptyList());
      instance.setRandomFillersGenerated(true);
      return;
    }

    List<RandomFillerEntry> generated = new ArrayList<>();
    List<Integer> availableSlots = new ArrayList<>(emptySlots);
    for (RandomChestCavityFiller filler : randomFillers) {
      filler.generateEntries(random, availableSlots, generated);
      if (availableSlots.isEmpty()) {
        break;
      }
    }

    instance.replacePreGeneratedRandomFillers(generated);
    instance.setRandomFillersGenerated(true);
  }

  /** 将预先抽取到的随机物品放入胸腔库存，供玩家打开或掉落处理复用；若缓存为空则保持默认状态。 */
  public void applyPreGeneratedFillers(
      ChestCavityInstance instance, ChestCavityInventory inventory) {
    if (instance == null
        || inventory == null
        || !instance.areRandomFillersGenerated()
        || randomFillers.isEmpty()) {
      return;
    }

    for (RandomFillerEntry entry : instance.getPreGeneratedRandomFillers()) {
      ItemStack stack = entry.stack();
      if (stack.isEmpty()) {
        continue;
      }
      int slot = entry.slot();
      if (slot >= 0 && slot < inventory.getContainerSize() && inventory.getItem(slot).isEmpty()) {
        inventory.setItem(slot, stack.copy());
        continue;
      }
      for (int i = 0; i < inventory.getContainerSize(); i++) {
        if (inventory.getItem(i).isEmpty()) {
          inventory.setItem(i, stack.copy());
          break;
        }
      }
    }
  }

  @Override
  public void loadBaseOrganScores(Map<ResourceLocation, Float> organScores) {
    organScores.clear();
  }

  @Override
  public OrganData catchExceptionalOrgan(ItemStack slot) {
    Map<ResourceLocation, Float> organMap = getExceptionalOrganScore(slot);
    if (organMap != null) {
      OrganData organData = new OrganData();
      organData.organScores = organMap;
      organData.pseudoOrgan = true;
      return organData;
    }
    return null;
  }

  public float getDropRateMultiplier() {
    return dropRateMultiplier;
  }

  public void setDropRateMultiplier(float multiplier) {
    dropRateMultiplier = multiplier;
  }

  @Override
  public List<ItemStack> generateLootDrops(
      ChestCavityInstance instance, RandomSource random, int looting) {
    List<ItemStack> loot = new ArrayList<>();
    if (playerChestCavity) {
      return loot;
    }
    if (bossChestCavity) {
      generateGuaranteedOrganDrops(instance, random, looting, loot);
      return loot;
    }
    if (random.nextFloat()
        < (ChestCavity.config.UNIVERSAL_DONOR_RATE
                + (ChestCavity.config.ORGAN_BUNDLE_LOOTING_BOOST * looting))
            * getDropRateMultiplier()) {
      generateRareOrganDrops(instance, random, looting, loot);
    }
    return loot;
  }

  public void generateRareOrganDrops(
      ChestCavityInstance instance, RandomSource random, int looting, List<ItemStack> loot) {
    LinkedList<ItemStack> organPile = new LinkedList<>(getDroppableOrgans());
    int rolls = 1 + random.nextInt(3) + random.nextInt(3);
    ChestCavityUtil.drawOrgansFromPile(organPile, rolls, random, loot);
    appendPreGeneratedFillerDrops(instance, random, loot);
  }

  public void generateGuaranteedOrganDrops(
      ChestCavityInstance instance, RandomSource random, int looting, List<ItemStack> loot) {
    LinkedList<ItemStack> organPile = new LinkedList<>(getDroppableOrgans());
    int rolls = 3 + random.nextInt(2 + looting) + random.nextInt(2 + looting);
    ChestCavityUtil.drawOrgansFromPile(organPile, rolls, random, loot);
    appendPreGeneratedFillerDrops(instance, random, loot);
  }

  /**
   * 在掉落结算阶段复用随机生成器配置，使得 randomGenerators 定义的蛊虫或器官也可以在击杀时掉落。
   *
   * <p>若对应 {@link ChestCavityInstance} 已在生成时记录过随机结果，则直接复用缓存以保证与打开胸腔时 看到的内容一致；否则退回到即时抽取逻辑，维持旧版行为。
   */
  private void appendPreGeneratedFillerDrops(
      ChestCavityInstance instance, RandomSource random, List<ItemStack> loot) {
    if (randomFillers.isEmpty()) {
      return;
    }
    if (instance != null && instance.areRandomFillersGenerated()) {
      for (RandomFillerEntry entry : instance.getPreGeneratedRandomFillers()) {
        ItemStack stack = entry.stack();
        if (!stack.isEmpty()) {
          loot.add(stack.copy());
        }
      }
      return;
    }
    addRandomFillerDrops(random, loot);
  }

  /** 在掉落流程中退回即时随机的兜底方法。当实体未曾缓存随机生成器结果时，仍按原有逻辑按概率生成掉落。 */
  private void addRandomFillerDrops(RandomSource random, List<ItemStack> loot) {
    if (randomFillers.isEmpty()) {
      return;
    }
    for (RandomChestCavityFiller filler : randomFillers) {
      filler.collectDrops(random, loot);
    }
  }

  @Override
  public void setOrganCompatibility(ChestCavityInstance instance) {
    ChestCavityInventory chestCavity = instance.inventory;
    // first, make all organs personal
    for (int i = 0; i < chestCavity.getContainerSize(); i++) {
      ItemStack itemStack = chestCavity.getItem(i);
      if (itemStack != null && !itemStack.isEmpty()) {
        CustomData.update(
            DataComponents.CUSTOM_DATA,
            itemStack,
            tag -> {
              CompoundTag compatibility = tag.getCompound(ChestCavity.COMPATIBILITY_TAG.toString());
              compatibility.putUUID("owner", instance.compatibility_id);
              compatibility.putString("name", instance.owner.getDisplayName().getString());
              tag.put(ChestCavity.COMPATIBILITY_TAG.toString(), compatibility);
            });
      }
    }
    if (!playerChestCavity) {
      int universalOrgans = 0;
      RandomSource random = instance.owner.getRandom();
      if (bossChestCavity) {
        universalOrgans = 3 + random.nextInt(2) + random.nextInt(2);
      } else if (random.nextFloat() < ChestCavity.config.UNIVERSAL_DONOR_RATE) {
        universalOrgans = 1 + random.nextInt(3) + random.nextInt(3);
      }
      // each attempt, roll a random slot in the chestcavity and turn that organ, if any, compatible
      while (universalOrgans > 0) {
        int i = random.nextInt(chestCavity.getContainerSize());
        ItemStack itemStack = chestCavity.getItem(i);
        if (itemStack != null
            && !itemStack.isEmpty()
            && OrganManager.isTrueOrgan(itemStack.getItem())) {
          CustomData.update(
              DataComponents.CUSTOM_DATA,
              itemStack,
              tag -> tag.remove(ChestCavity.COMPATIBILITY_TAG.toString()));
        }
        universalOrgans--;
      }
    }
  }

  @Override
  public float getHeartBleedCap() {
    if (bossChestCavity) {
      return 5;
    }
    return Float.MAX_VALUE;
  }

  @Override
  public boolean isOpenable(ChestCavityInstance instance) {
    boolean weakEnough =
        instance.owner.getHealth() <= ChestCavity.config.CHEST_OPENER_ABSOLUTE_HEALTH_THRESHOLD
            || instance.owner.getHealth()
                <= instance.owner.getMaxHealth()
                    * ChestCavity.config.CHEST_OPENER_FRACTIONAL_HEALTH_THRESHOLD;
    boolean easeOfAccess = instance.getOrganScore(CCOrganScores.EASE_OF_ACCESS) > 0;
    return easeOfAccess || weakEnough;
  }

  @Override
  public void onDeath(ChestCavityInstance cc) {
    cc.projectileQueue.clear();
    if (cc.connectedCrystal != null) {
      cc.connectedCrystal.setBeamTarget(null);
      cc.connectedCrystal = null;
    }
    if (cc.opened && !(playerChestCavity && ChestCavity.config.KEEP_CHEST_CAVITY)) {
      ChestCavityUtil.dropUnboundOrgans(cc);
    }
    // if(playerChestCavity){
    //    ChestCavityUtil.insertWelfareOrgans(cc);
    // }
  }
}
