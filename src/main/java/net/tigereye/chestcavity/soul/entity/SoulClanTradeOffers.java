package net.tigereye.chestcavity.soul.entity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.tigereye.chestcavity.guzhenren.XindeItemKeys;
import net.tigereye.chestcavity.guzhenren.XindeItemKeys.XindeItemConfig;
import net.tigereye.chestcavity.guzhenren.XindeItemKeys.TradeCostItemConfig;

/** 灵魂氏族商人的交易配置管理器 */
public class SoulClanTradeOffers {

  /** 交易稀有度等级 */
  public enum TradeRarity {
    T0, // 最稀有 - 传说级
    T1, // 史诗级
    T2, // 稀有级
    T3, // 普通级
    T4 // 常见级
  }

  /** 带稀有度的交易条目 */
  private static class TradeEntry {
    final Supplier<MerchantOffer> trade;
    final TradeRarity rarity;

    TradeEntry(Supplier<MerchantOffer> trade, TradeRarity rarity) {
      this.trade = trade;
      this.rarity = rarity;
    }
  }

  /** 交易选项池 - 按稀有度分类 */
  private static final Map<TradeRarity, List<TradeEntry>> TRADE_POOL_BY_RARITY =
      new EnumMap<>(TradeRarity.class);

  /** 稀有度权重配置 - 可通过配置文件修改 */
  public static class RarityConfig {
    /** T0（传说级）刷新权重 */
    public static double T0_WEIGHT = 0.5; // 0.5%

    /** T1（史诗级）刷新权重 */
    public static double T1_WEIGHT = 2.0; // 2%

    /** T2（稀有级）刷新权重 */
    public static double T2_WEIGHT = 10.0; // 10%

    /** T3（普通级）刷新权重 */
    public static double T3_WEIGHT = 30.0; // 30%

    /** T4（常见级）刷新权重 */
    public static double T4_WEIGHT = 57.5; // 57.5%

    /** 获取指定稀有度的权重 */
    public static double getWeight(TradeRarity rarity) {
      return switch (rarity) {
        case T0 -> T0_WEIGHT;
        case T1 -> T1_WEIGHT;
        case T2 -> T2_WEIGHT;
        case T3 -> T3_WEIGHT;
        case T4 -> T4_WEIGHT;
      };
    }

    /** 计算总权重 */
    public static double getTotalWeight() {
      return T0_WEIGHT + T1_WEIGHT + T2_WEIGHT + T3_WEIGHT + T4_WEIGHT;
    }
  }

  static {
    // 初始化稀有度池
    for (TradeRarity rarity : TradeRarity.values()) {
      TRADE_POOL_BY_RARITY.put(rarity, new ArrayList<>());
    }
  }

  /**
   * 通过 Item ID 获取物品（支持其他模组）
   *
   * @param itemId 物品的 ResourceLocation 字符串，格式: "modid:item_name"
   * @return Item 对象，如果不存在则返回 null
   */
  private static Item getItemById(String itemId) {
    try {
      ResourceLocation location = ResourceLocation.parse(itemId);
      return BuiltInRegistries.ITEM.get(location);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 添加交易到指定稀有度池
   *
   * @param rarity 稀有度等级
   * @param trade 交易生成器
   */
  private static void addTrade(TradeRarity rarity, Supplier<MerchantOffer> trade) {
    TRADE_POOL_BY_RARITY.get(rarity).add(new TradeEntry(trade, rarity));
  }

  /**
   * 添加自定义交易到交易池（支持其他模组物品）
   *
   * @param rarity 稀有度等级
   * @param inputItemId 输入物品的 ID（格式: "modid:item_name"）
   * @param inputCount 输入物品数量
   * @param outputItemId 输出物品的 ID
   * @param outputCount 输出物品数量
   * @param maxUses 最大使用次数
   * @param xp 给予的经验值
   * @param priceMultiplier 价格倍数
   */
  public static void addCustomTrade(
      TradeRarity rarity,
      String inputItemId,
      int inputCount,
      String outputItemId,
      int outputCount,
      int maxUses,
      int xp,
      float priceMultiplier) {

    addTrade(
        rarity,
        () -> {
          Item inputItem = getItemById(inputItemId);
          Item outputItem = getItemById(outputItemId);

          if (inputItem != null && outputItem != null) {
            return new MerchantOffer(
                new ItemCost(inputItem, inputCount),
                new ItemStack(outputItem, outputCount),
                maxUses,
                xp,
                priceMultiplier);
          }
          // 默认交易
          return new MerchantOffer(
              new ItemCost(Items.COBBLESTONE, 16),
              new ItemStack(Items.BREAD, 4),
              16,
              3,
              0.05f);
        });
  }

  static {
    // ========== T4 常见级交易 ==========
    addTrade(
        TradeRarity.T4,
        () ->
            new MerchantOffer(
                new ItemCost(Items.COBBLESTONE, 16),
                new ItemStack(Items.BREAD, 4),
                16,
                3,
                0.05f));

    addTrade(
        TradeRarity.T4,
        () ->
            new MerchantOffer(
                new ItemCost(Items.WHEAT, 20), new ItemStack(Items.EMERALD, 1), 12, 5, 0.05f));

    addTrade(
        TradeRarity.T4,
        () ->
            new MerchantOffer(
                new ItemCost(Items.COAL, 15), new ItemStack(Items.TORCH, 8), 16, 2, 0.05f));

    addTrade(
        TradeRarity.T4,
        () ->
            new MerchantOffer(
                new ItemCost(Items.STICK, 32), new ItemStack(Items.EMERALD, 1), 16, 2, 0.05f));

    addTrade(
        TradeRarity.T4,
        () ->
            new MerchantOffer(
                new ItemCost(Items.CARROT, 22), new ItemStack(Items.EMERALD, 1), 16, 2, 0.05f));

    addTrade(
        TradeRarity.T4,
        () ->
            new MerchantOffer(
                new ItemCost(Items.POTATO, 26), new ItemStack(Items.EMERALD, 1), 16, 2, 0.05f));

    // ========== T3 普通级交易 ==========
    addTrade(
        TradeRarity.T3,
        () ->
            new MerchantOffer(
                new ItemCost(Items.RAW_IRON, 8), new ItemStack(Items.IRON_INGOT, 1), 12, 10, 0.1f));

    addTrade(
        TradeRarity.T3,
        () ->
            new MerchantOffer(
                new ItemCost(Items.EMERALD, 1), new ItemStack(Items.COOKED_BEEF, 5), 12, 5, 0.05f));

    addTrade(
        TradeRarity.T3,
        () ->
            new MerchantOffer(
                new ItemCost(Items.EMERALD, 1), new ItemStack(Items.GLASS, 8), 12, 3, 0.05f));

    addTrade(
        TradeRarity.T3,
        () ->
            new MerchantOffer(
                new ItemCost(Items.EMERALD, 1),
                new ItemStack(Items.GLOWSTONE, 4),
                12,
                5,
                0.05f));

    addTrade(
        TradeRarity.T3,
        () ->
            new MerchantOffer(
                new ItemCost(Items.EMERALD, 2), new ItemStack(Items.IRON_AXE, 1), 8, 8, 0.1f));

    // ========== T2 稀有级交易 ==========
    addTrade(
        TradeRarity.T2,
        () ->
            new MerchantOffer(
                new ItemCost(Items.EMERALD, 3),
                new ItemStack(Items.IRON_PICKAXE, 1),
                8,
                8,
                0.1f));

    addTrade(
        TradeRarity.T2,
        () ->
            new MerchantOffer(
                new ItemCost(Items.EMERALD, 4), new ItemStack(Items.IRON_SWORD, 1), 8, 8, 0.1f));

    addTrade(
        TradeRarity.T2,
        () ->
            new MerchantOffer(
                new ItemCost(Items.EMERALD, 2),
                new ItemStack(Items.GOLDEN_APPLE, 1),
                8,
                10,
                0.1f));

    addTrade(
        TradeRarity.T2,
        () ->
            new MerchantOffer(
                new ItemCost(Items.EMERALD, 3),
                new ItemStack(Items.ENDER_PEARL, 1),
                8,
                10,
                0.1f));

    addTrade(
        TradeRarity.T2,
        () ->
            new MerchantOffer(
                new ItemCost(Items.DIAMOND, 4), new ItemStack(Items.EMERALD, 5), 8, 12, 0.15f));

    // ========== T1 史诗级交易 ==========
    addTrade(
        TradeRarity.T1,
        () ->
            new MerchantOffer(
                new ItemCost(Items.EMERALD_BLOCK, 5),
                new ItemStack(Items.DIAMOND, 1),
                64,
                15,
                0.15f));

    addTrade(
        TradeRarity.T1,
        () ->
            new MerchantOffer(
                new ItemCost(Items.EMERALD_BLOCK, 5), new ItemStack(Items.NAME_TAG, 1), 6, 15, 0.15f));

    addTrade(
        TradeRarity.T1,
        () ->
            new MerchantOffer(
                new ItemCost(Items.EMERALD_BLOCK, 8),
                new ItemStack(Items.ENCHANTED_BOOK, 1),
                4,
                20,
                0.2f));

    addTrade(
        TradeRarity.T1,
        () ->
            new MerchantOffer(
                new ItemCost(Items.EMERALD_BLOCK, 6),
                new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1),
                3,
                25,
                0.2f));

    // ========== T0 传说级交易 ==========
    addTrade(
        TradeRarity.T0,
        () ->
            new MerchantOffer(
                new ItemCost(Items.EMERALD_BLOCK, 10),
                new ItemStack(Items.TOTEM_OF_UNDYING, 1),
                3,
                30,
                0.25f));

    addTrade(
        TradeRarity.T0,
        () ->
            new MerchantOffer(
                new ItemCost(Items.EMERALD_BLOCK, 15),
                new ItemStack(Items.ELYTRA, 1),
                2,
                40,
                0.3f));

    addTrade(
        TradeRarity.T0,
        () ->
            new MerchantOffer(
                new ItemCost(Items.EMERALD_BLOCK, 20),
                new ItemStack(Items.NETHERITE_INGOT, 1),
                2,
                50,
                0.3f));
    // ========== 蛊真人自定义物品交易示例 ==========

    // ========== 其他模组物品交易示例 ==========
    // 使用 addCustomTrade 方法添加其他模组的物品交易
    // 格式: addCustomTrade(稀有度, "输入物品ID", 数量, "输出物品ID", 数量, 最大次数, 经验, 价格波动)

    // 示例：Create 模组交易
    // addCustomTrade(TradeRarity.T2, "minecraft:emerald", 5, "create:brass_ingot", 2, 10, 8,
    // 0.1f);

    // 示例：Botania 模组交易
    // addCustomTrade(TradeRarity.T1, "botania:mana_diamond", 1, "minecraft:diamond", 2, 8, 10,
    // 0.15f);

    // ========== 蛊真人心得物品随机交易 ==========
    // T0 - 大师级心得交易（传说级）
    addRandomXindeTrade(TradeRarity.T0, XindeItemKeys.XINDE_DASHI_LIST, 64, 50, 0.3f);
    addRandomXindeTrade(TradeRarity.T0, XindeItemKeys.XINDE_DASHI_LIST, 48, 40, 0.25f);
    addRandomXindeTrade(TradeRarity.T0, XindeItemKeys.XINDE_DASHI_LIST, 80, 60, 0.35f);

    // T1 - 准大师级心得交易（史诗级）
    addRandomXindeTrade(TradeRarity.T1, XindeItemKeys.XINDE_ZHUNDASHI_LIST, 32, 30, 0.2f);
    addRandomXindeTrade(TradeRarity.T1, XindeItemKeys.XINDE_ZHUNDASHI_LIST, 24, 25, 0.15f);
    addRandomXindeTrade(TradeRarity.T1, XindeItemKeys.XINDE_ZHUNDASHI_LIST, 40, 35, 0.25f);
    addRandomXindeTrade(TradeRarity.T1, XindeItemKeys.XINDE_ZHUNDASHI_LIST, 28, 28, 0.18f);

    // T2 - 普通级心得交易（稀有级）
    addRandomXindeTrade(TradeRarity.T2, XindeItemKeys.XINDE_PUTONG_LIST, 16, 20, 0.15f);
    addRandomXindeTrade(TradeRarity.T2, XindeItemKeys.XINDE_PUTONG_LIST, 12, 15, 0.1f);
    addRandomXindeTrade(TradeRarity.T2, XindeItemKeys.XINDE_PUTONG_LIST, 20, 25, 0.2f);
    addRandomXindeTrade(TradeRarity.T2, XindeItemKeys.XINDE_PUTONG_LIST, 14, 18, 0.12f);
    addRandomXindeTrade(TradeRarity.T2, XindeItemKeys.XINDE_PUTONG_LIST, 18, 22, 0.15f);
  }

  /**
   * 添加随机心得物品交易（输入随机，输出固定1个心得）
   *
   * @param rarity 稀有度等级
   * @param xindeList 心得物品列表
   * @param maxUses 最大使用次数
   * @param xp 经验值
   * @param priceMultiplier 价格倍数
   */
  private static void addRandomXindeTrade(
      TradeRarity rarity,
      List<XindeItemConfig> xindeList,
      int maxUses,
      int xp,
      float priceMultiplier) {

    addTrade(
        rarity,
        () -> {
          RandomSource random = RandomSource.create();

          // 随机选择一个交易所需物品
          List<TradeCostItemConfig> costList = XindeItemKeys.getTradeCostListByRarity(rarity);
          TradeCostItemConfig costConfig = XindeItemKeys.getRandomTradeCost(costList, random);

          if (costConfig == null) {
            // 如果列表为空，返回默认交易
            return new MerchantOffer(
                new ItemCost(Items.EMERALD_BLOCK, 1),
                new ItemStack(Items.DIAMOND, 1),
                16,
                10,
                0.1f);
          }

          // 获取交易所需物品
          Item costItem = getItemById(costConfig.getItemId());
          if (costItem == null) {
            // 物品不存在，返回默认交易
            return new MerchantOffer(
                new ItemCost(Items.EMERALD_BLOCK, 1),
                new ItemStack(Items.DIAMOND, 1),
                16,
                10,
                0.1f);
          }

          // 随机生成交易所需数量
          int costCount = costConfig.getRandomCount(random);

          // 随机选择一个心得物品
          XindeItemConfig xindeConfig = XindeItemKeys.getRandomXinde(xindeList, random);

          if (xindeConfig == null) {
            // 如果列表为空，返回默认交易
            return new MerchantOffer(
                new ItemCost(Items.EMERALD_BLOCK, 1),
                new ItemStack(Items.DIAMOND, 1),
                16,
                10,
                0.1f);
          }

          // 获取心得物品
          Item xindeItem = getItemById(xindeConfig.getItemId());
          if (xindeItem == null) {
            // 物品不存在，返回默认交易
            return new MerchantOffer(
                new ItemCost(Items.EMERALD_BLOCK, 1),
                new ItemStack(Items.DIAMOND, 1),
                16,
                10,
                0.1f);
          }

          // 创建交易：X个随机物品 -> 1个心得物品
          return new MerchantOffer(
              new ItemCost(costItem, costCount),
              new ItemStack(xindeItem, 1), // 固定输出1个
              maxUses,
              xp,
              priceMultiplier);
        });
  }

  /**
   * 根据稀有度权重随机选择一个稀有度等级
   *
   * @param random 随机数生成器
   * @return 选中的稀有度等级
   */
  private static TradeRarity selectRarityByWeight(RandomSource random) {
    double totalWeight = RarityConfig.getTotalWeight();
    double roll = random.nextDouble() * totalWeight;

    double cumulative = 0;
    for (TradeRarity rarity : TradeRarity.values()) {
      cumulative += RarityConfig.getWeight(rarity);
      if (roll < cumulative) {
        return rarity;
      }
    }
    return TradeRarity.T4; // 默认返回常见级
  }

  /**
   * 从指定稀有度池中随机选择一个交易
   *
   * @param rarity 稀有度等级
   * @param random 随机数生成器
   * @return 交易生成器，如果池为空则返回 null
   */
  private static Supplier<MerchantOffer> getRandomTradeFromRarity(
      TradeRarity rarity, RandomSource random) {
    List<TradeEntry> pool = TRADE_POOL_BY_RARITY.get(rarity);
    if (pool.isEmpty()) {
      return null;
    }
    return pool.get(random.nextInt(pool.size())).trade;
  }

  /**
   * 随机生成指定数量的交易（支持稀有度系统）
   *
   * @param count 需要生成的交易数量
   * @param random 随机数生成器
   * @return 生成的交易列表
   */
  public static MerchantOffers generateRandomOffers(int count, RandomSource random) {
    MerchantOffers offers = new MerchantOffers();

    for (int i = 0; i < count; i++) {
      // 根据权重选择稀有度
      TradeRarity selectedRarity = selectRarityByWeight(random);

      // 从该稀有度池中随机选择一个交易
      Supplier<MerchantOffer> tradeSupplier = getRandomTradeFromRarity(selectedRarity, random);

      // 如果该稀有度池为空，尝试降级到更常见的稀有度
      if (tradeSupplier == null) {
        for (TradeRarity fallback : TradeRarity.values()) {
          tradeSupplier = getRandomTradeFromRarity(fallback, random);
          if (tradeSupplier != null) {
            break;
          }
        }
      }

      // 添加交易
      if (tradeSupplier != null) {
        offers.add(tradeSupplier.get());
      }
    }

    return offers;
  }

  /**
   * 为灵魂氏族商人生成默认的五个交易
   *
   * @param random 随机数生成器
   * @return 包含五个随机交易的交易列表
   */
  public static MerchantOffers generateDefaultOffers(RandomSource random) {
    return generateRandomOffers(5, random);
  }
}
