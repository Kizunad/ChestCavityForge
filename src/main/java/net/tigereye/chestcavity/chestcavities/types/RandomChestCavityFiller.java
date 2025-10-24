package net.tigereye.chestcavity.chestcavities.types;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.ChestCavityInventory;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance.RandomFillerEntry;

public record RandomChestCavityFiller(
    ResourceLocation id, int minCount, int maxCount, List<Item> items, int chance) {

  public RandomChestCavityFiller {
    items = List.copyOf(items);
    if (chance < 0 || chance > 100) {
      throw new IllegalArgumentException("Chance must be between 0 and 100");
    }
  }

  public void fill(ChestCavityInventory inventory, RandomSource random) {
    if (items.isEmpty() || maxCount <= 0) {
      return;
    }

    // 检查 chance 参数，如果设置了且随机数不在范围内，则跳过
    if (chance < 100) {
      int roll = random.nextInt(100);
      if (roll >= chance) {
        return;
      }
    }

    int minimum = Math.max(0, minCount);
    int maximum = Math.max(minimum, maxCount);
    if (maximum == 0) {
      return;
    }
    int rolls = random.nextInt(maximum - minimum + 1) + minimum;
    if (rolls <= 0) {
      return;
    }
    List<Integer> emptySlots = new ArrayList<>();
    for (int i = 0; i < inventory.getContainerSize(); i++) {
      if (inventory.getItem(i).isEmpty()) {
        emptySlots.add(i);
      }
    }
    if (emptySlots.isEmpty()) {
      return;
    }
    rolls = Math.min(rolls, emptySlots.size());
    for (int i = 0; i < rolls; i++) {
      int slotIndex = random.nextInt(emptySlots.size());
      int slot = emptySlots.remove(slotIndex);
      Item item = items.get(random.nextInt(items.size()));
      inventory.setItem(slot, new ItemStack(item));
    }
  }

  /**
   * 将本随机生成器的候选物品直接加入掉落列表，以便在实体死亡时复用与生成胸腔时相同的随机逻辑。
   *
   * <p>掉落物的筛选流程与 {@link #fill(ChestCavityInventory, RandomSource)} 基本一致，只是省略了空槽位的
   * 限制，并直接向提供的集合追加每次抽取到的物品。
   *
   * @param random 用于决定次数与具体物品的随机源
   * @param loot 用于收集掉落物的目标集合
   */
  public void collectDrops(RandomSource random, List<ItemStack> loot) {
    if (items.isEmpty() || maxCount <= 0) {
      return;
    }

    if (chance < 100) {
      int roll = random.nextInt(100);
      if (roll >= chance) {
        return;
      }
    }

    int minimum = Math.max(0, minCount);
    int maximum = Math.max(minimum, maxCount);
    if (maximum == 0) {
      return;
    }

    int rolls = random.nextInt(maximum - minimum + 1) + minimum;
    if (rolls <= 0) {
      return;
    }

    for (int i = 0; i < rolls; i++) {
      Item item = items.get(random.nextInt(items.size()));
      loot.add(new ItemStack(item));
    }
  }

  /**
   * 将当前生成器依据剩余空槽位抽取的结果写入缓存，用于实体生成时的预处理流程。
   *
   * @param random 随机源
   * @param availableSlots 当前仍可使用的空槽列表，会被就地移除已占用的槽位
   * @param generated 用于累积随机结果的目标集合
   */
  public void generateEntries(
      RandomSource random, List<Integer> availableSlots, List<RandomFillerEntry> generated) {
    if (items.isEmpty() || maxCount <= 0 || availableSlots.isEmpty()) {
      return;
    }

    if (chance < 100) {
      int roll = random.nextInt(100);
      if (roll >= chance) {
        return;
      }
    }

    int minimum = Math.max(0, minCount);
    int maximum = Math.max(minimum, maxCount);
    if (maximum == 0) {
      return;
    }

    int rolls = random.nextInt(maximum - minimum + 1) + minimum;
    if (rolls <= 0) {
      return;
    }

    rolls = Math.min(rolls, availableSlots.size());
    for (int i = 0; i < rolls; i++) {
      int slotIndex = random.nextInt(availableSlots.size());
      int slot = availableSlots.remove(slotIndex);
      Item item = items.get(random.nextInt(items.size()));
      generated.add(new RandomFillerEntry(slot, new ItemStack(item)));
      if (availableSlots.isEmpty()) {
        break;
      }
    }
  }
}
