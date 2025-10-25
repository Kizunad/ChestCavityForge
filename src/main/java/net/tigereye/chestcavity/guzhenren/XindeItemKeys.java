package net.tigereye.chestcavity.guzhenren;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.RandomSource;

/**
 * 蛊真人心得物品常量定义
 * 自动从 scripts/xinde_keys.json 提取
 * 所有常量格式: guzhenren:item_name
 */
public final class XindeItemKeys {

  private XindeItemKeys() {}

  // 模组ID前缀
  private static final String MODID = "guzhenren:";

  /** 心得物品配置 - 仅包含物品ID（输出固定为1个） */
  public static class XindeItemConfig {
    private final String itemId;

    public XindeItemConfig(String itemId) {
      this.itemId = itemId;
    }

    public String getItemId() {
      return itemId;
    }
  }

  /** 交易所需物品配置 - 包含物品ID和数量范围 */
  public static class TradeCostItemConfig {
    private final String itemId;
    private final int minCount;
    private final int maxCount;

    public TradeCostItemConfig(String itemId, int minCount, int maxCount) {
      this.itemId = itemId;
      this.minCount = Math.max(1, minCount); // 最小为1
      this.maxCount = Math.max(this.minCount, maxCount);
    }

    public String getItemId() {
      return itemId;
    }

    public int getMinCount() {
      return minCount;
    }

    public int getMaxCount() {
      return maxCount;
    }

    /** 获取随机数量 */
    public int getRandomCount(RandomSource random) {
      if (minCount == maxCount) {
        return minCount;
      }
      return minCount + random.nextInt(maxCount - minCount + 1);
    }
  }

  // ========== 炎道心得 ==========
  /** 炎道心得(准宗师级)—未参悟 */
  public static final String XINDE_YANDAO_ZHUNZONGSHI = MODID + "xindeyandaozhunzongshi";

  /** 炎道心得(准大师级)—未参悟 */
  public static final String XINDE_YANDAO_ZHUNDASHI = MODID + "xindeyandaozhundashi";

  /** 炎道心得(普通级)—未参悟 */
  public static final String XINDE_YANDAO_PUTONG = MODID + "xindeyandaoputong";

  /** 炎道心得(大师级)—未参悟 */
  public static final String XINDE_YANDAO_DASHI = MODID + "xindeyandaodashi";

  // ========== 力道心得 ==========
  /** 力道心得(大师级)—未参悟 */
  public static final String XINDE_LIDAO_DASHI_WEICANWU = MODID + "xindelidaodashiweicanwu";

  /** 力道心得(准大师级)—未参悟 */
  public static final String XINDE_LIDAO_ZHUNDASHI_WEICANWU = MODID + "xindelidaozhundashiweicanwu";

  /** 力道心得(普通级)—未参悟 */
  public static final String XINDE_LIDAO_PUTONG_WEICANWU = MODID + "xindelidaoputongweicanwu";

  /** 力道心得(准宗师级)—未参悟 */
  public static final String XINDE_LIDAO_ZHUNZONGSHI_WEICANWU = MODID + "xindelidaozhunzongshiweicanwu";

  // ========== 月道心得 ==========
  /** 月道心得(准宗师级)—未参悟 */
  public static final String XINDE_YUEDAO_ZHUNZONGSHI = MODID + "xindeyuedaozhunzongshi";

  /** 月道心得(普通级)—未参悟 */
  public static final String XINDE_YUEDAO_PUTONG = MODID + "xindeyuedaoputong";

  /** 月道心得(准大师级)—未参悟 */
  public static final String XINDE_YUEDAO_ZHUNDASHI = MODID + "xindeyuedaozhundashi";

  /** 月道心得(大师级)—未参悟 */
  public static final String XINDE_YUEDAO_DASHI = MODID + "xindeyuedaodashi";

  // ========== 炼道心得 ==========
  /** 炼道心得(普通级)—未参悟 */
  public static final String XINDE_LIANDAO_PUTONG_WEICANWU = MODID + "xindeliandaoputongweicanwu";

  /** 炼道心得(准大师级)—未参悟 */
  public static final String XINDE_LIANDAO_ZHUNDASHI_WEICANWU = MODID + "xindeliandaozhundashiweicanwu";

  /** 炼道心得(大师级)—未参悟 */
  public static final String XINDE_LIANDAO_DASHI_WEICANWU = MODID + "xindeliandaodashiweicanwu";

  /** 炼道心得(准宗师级)—未参悟 */
  public static final String XINDE_LIAN_ZHUNZONGSHI_WEICANWU = MODID + "xindelianzhunzongshijiweicanwu";

  // ========== 剑道心得 ==========
  /** 剑道心得(准宗师级)—未参悟 */
  public static final String XINDE_JIANDAO_ZHUNZONGSHI_WEICANWU = MODID + "xindejiandaozhunzongshiweicanwu";

  /** 剑道心得(普通级)—未参悟 */
  public static final String XINDE_JIANDAO_PUTONG_WEICANWU = MODID + "xindejiandaoputongjiweicanwu";

  /** 剑道心得(准大师级)—未参悟 */
  public static final String XINDE_JIANDAO_ZHUNDASHI_WEICANWU = MODID + "xindejiandaozhundashijiweicanwu";

  /** 剑道心得(大师级)—未参悟 */
  public static final String XINDE_JIANDAO_DASHI_WEICANWU = MODID + "xindejiandaodashiweicanwu";

  // ========== 冰雪道心得 ==========
  /** 冰雪道心得(准大师级)—未参悟 */
  public static final String XINDE_BINGXUEDAO_ZHUNDASHI_WEICANWU = MODID + "xindebingxuedaozhundashijiweicanwu";

  /** 冰雪道心得(普通级)—未参悟 */
  public static final String XINDE_BINGXUEDAO_PUTONG_WEICANWU = MODID + "xindebingxuedaoputongjiweicanwu";

  /** 冰雪道心得(大师级)—未参悟 */
  public static final String XINDE_BINGXUEDAO_DASHI_WEICANWU = MODID + "xindebingxuedaodashijiweicanwu";

  /** 冰雪道心得(准宗师级)—未参悟 */
  public static final String XINDE_BINGXUEDAO_ZHUNZONGSHI_WEICANWU = MODID + "xindebingxuedaozhunzongshijiweicanwu";

  // ========== 变化道心得 ==========
  /** 变化道心得(准大师级)—未参悟 */
  public static final String BIANHUADAO_XINDE_ZHUNDAOSHI_WEICANWU = MODID + "bianhuadaoxindezhundaoshiweicanwu";

  /** 变化道心得(准宗师级)—未参悟 */
  public static final String BIANHUADAO_XINDE_ZHUNZONGSHI_WEICANWU = MODID + "bianhuadaoxindezhunzongshiweicanwu";

  /** 变化道心得(普通级)—未参悟 */
  public static final String BIANHUADAO_XINDE_PUTONG_WEICANWU = MODID + "bianhuadaoxindeputongweicanwu";

  /** 变化道心得(大师级)—未参悟 */
  public static final String BIANHUADAO_XINDE_DASHI_WEICANWU = MODID + "bianhuadaoxindedashiweicanwu";

  // ========== 金道心得 ==========
  /** 金道心得(准大师级)—未参悟 */
  public static final String XINDE_JINDAO_ZHUNDASIHI_WEICANWU = MODID + "xindejindaozhundasihiweicanwu";

  /** 金道心得(准宗师级)—未参悟 */
  public static final String XINDE_JINDAO_ZHUNZONGSHI_WEICANWU = MODID + "xindejindaozhunzongshiweicanwu";

  /** 金道心得(大师级)—未参悟 */
  public static final String XINDE_JINDAO_DASHI_WEICANWU = MODID + "xindejindaodashiweicanwu";

  /** 金道心得(普通级)—未参悟 */
  public static final String XINDE_JINDAO_PUTONG_WEICANWU = MODID + "xindejindaoputongweicanwu";

  // ========== 光道心得 ==========
  /** 光道心得(普通级)—未参悟 */
  public static final String XINDE_GUANGDAO_PUTONG_WEICANWU = MODID + "xindeguangdaoputongjiweicanwu";

  /** 光道心得(准宗师级)—未参悟 */
  public static final String XINDE_GUANGDAO_ZHUNZONGSHI_WEICANWU = MODID + "xindeguangdaozhunzongshijiweicanwu";

  /** 光道心得(准大师级)—未参悟 */
  public static final String XINDE_GUANGDAO_PUZHUAN_DASHI_WEICANWU = MODID + "xindeguangdaopuzhuandashiweicanwu";

  /** 光道心得(大师级)—未参悟 */
  public static final String XINDE_GUANGDAO_DASHI_WEICIWU = MODID + "xindeguangdaodashijiweiciwu";

  // ========== 骨道心得 ==========
  /** 骨道心得(大师级)—未参悟 */
  public static final String XINDE_GUDAO_DASHI = MODID + "xindedashiji_1";

  /** 骨道心得(普通级)—未参悟 */
  public static final String XINDE_GUDAO_PUTONG = MODID + "xindegudaoputong_1";

  /** 骨道心得(准大师级)—未参悟 */
  public static final String XINDE_GUDAO_ZHUNDASHI = MODID + "xindegudaozhundaoshi_1";

  /** 骨道心得(准宗师级)—未参悟 */
  public static final String GUDAO_XINDE_ZHUNZONGSHI = MODID + "gudaoxindezhunzongshi_1";

  // ========== 魂道心得 ==========
  /** 魂道心得(普通级)—未参悟 */
  public static final String XINDE_HUNDAO_PUTONG_WEICANWU = MODID + "xindehundaoputongjiweicanwu";

  /** 魂道心得(大师级)—未参悟 */
  public static final String XINDE_HUNDAO_DASHI_WEICANWU = MODID + "xindehundaodashijiweicanwu";

  /** 魂道心得(准大师级)—未参悟 */
  public static final String XINDE_HUNDAO_ZHUNDASHI_WEICANWU = MODID + "xindehundaozhundashijiweicanwu";

  /** 魂道心得(准宗师级)—未参悟 */
  public static final String XINDE_HUNDAO_ZHUNZONGSHI_WEICANWU = MODID + "xindehundaozhunzongshiweicanwu";

  // ========== 水道心得 ==========
  /** 水道心得(大师级)—未参悟 */
  public static final String XINDE_SHUIDAO_DASHI = MODID + "xindeshuidaondashi";

  /** 水道心得(准宗师级)—未参悟 */
  public static final String XINDE_SHUIDAO_ZHUNZONGSHI = MODID + "xindeshuidaozhunzongshi";

  /** 水道心得(普通级)—未参悟 */
  public static final String XINDE_SHUIDAO_PUTONG = MODID + "xindeshuidaoputong";

  /** 水道心得(准大师级)—未参悟 */
  public static final String XINDE_SHUIDAO_ZHUNDASHI = MODID + "xindeshuidaozhundashi";

  // ========== 木道心得 ==========
  /** 木道心得(准大师级)—未参悟 */
  public static final String MUDAO_XINDE_ZHUNDASHI_WEICANWU = MODID + "mudaoxindezhundashiweicanwu";

  /** 木道心得(大师级)—未参悟 */
  public static final String MUDAO_XINDE_DASHI_WEICANWU = MODID + "mudaoxindedashiweicanwu";

  /** 木道心得(普通级)—未参悟 */
  public static final String MUDAO_XINDE_PUTONG_WEICANWU = MODID + "mudaoxindeputongweicanwu";

  /** 木道心得(准宗师级)—未参悟 */
  public static final String MUDAO_XINDE_ZHUNZONGSHI_WEICANWU = MODID + "mudaoxindezhunzongshiweicanwu";

  // ========== 血道心得 ==========
  /** 血道心得(大师级)—未参悟 */
  public static final String XINDE_XUEDAO_DASHI_WEICANWU = MODID + "xindexuedaodashiweicanwu";

  /** 血道心得(准大师级)—未参悟 */
  public static final String XINDE_XUEDAO_ZHUNDASHI_WEICANWU = MODID + "xindexuedaozundashiweicanwu";

  /** 血道心得(普通级)—未参悟 */
  public static final String XINDE_XUEDAO_PUTONG_WEICANWU = MODID + "xindexuedaoputongxindeweicanfu";

  /** 血道心得(准宗师级)—未参悟 */
  public static final String XINDE_XUEDAO_ZHUNZONGSHI_WEICANWU = MODID + "xindexuedaozhunzongshiweicanwu";

  // ========== 雷道心得 ==========
  /** 雷道心得(普通级)—未参悟 */
  public static final String XINDE_LEIDAO_PUTONG_WEICANWU = MODID + "xindeleidaoputongweicanwu";

  /** 雷道心得(准大师级)—未参悟 */
  public static final String XINDE_LEIDAO_ZHUNDASHI_WEICANWU = MODID + "xindeleidaozhundashiweicanwu";

  /** 雷道心得(准宗师级)—未参悟 */
  public static final String XINDE_LEIDAO_ZHUNZONGSHI_WEICANWU = MODID + "xindeleidaozhunzongshiweicanwu";

  /** 雷道心得(大师级)—未参悟 */
  public static final String XINDE_LEIDAO_DASHI = MODID + "xindeleidaodashi";

  // ========== 按等级分类的心得列表 ==========

  /** T0 - 大师级心得列表 (最稀有) */
  public static final List<XindeItemConfig> XINDE_DASHI_LIST;

  /** T1 - 准大师级心得列表 (史诗) */
  public static final List<XindeItemConfig> XINDE_ZHUNDASHI_LIST;

  /** T2 - 普通级心得列表 (稀有) */
  public static final List<XindeItemConfig> XINDE_PUTONG_LIST;

  /** T0 - 交易所需物品池（传说级） */
  public static final List<TradeCostItemConfig> TRADE_COST_T0_LIST;

  /** T1 - 交易所需物品池（史诗级） */
  public static final List<TradeCostItemConfig> TRADE_COST_T1_LIST;

  /** T2 - 交易所需物品池（稀有级） */
  public static final List<TradeCostItemConfig> TRADE_COST_T2_LIST;

  static {
    // 初始化 T0 - 大师级心得列表（输出固定为1个）
    List<XindeItemConfig> dashiList = new ArrayList<>();
    dashiList.add(new XindeItemConfig(XINDE_YANDAO_DASHI));
    dashiList.add(new XindeItemConfig(XINDE_LIDAO_DASHI_WEICANWU));
    dashiList.add(new XindeItemConfig(XINDE_YUEDAO_DASHI));
    dashiList.add(new XindeItemConfig(XINDE_LIANDAO_DASHI_WEICANWU));
    dashiList.add(new XindeItemConfig(XINDE_JIANDAO_DASHI_WEICANWU));
    dashiList.add(new XindeItemConfig(XINDE_BINGXUEDAO_DASHI_WEICANWU));
    dashiList.add(new XindeItemConfig(BIANHUADAO_XINDE_DASHI_WEICANWU));
    dashiList.add(new XindeItemConfig(XINDE_JINDAO_DASHI_WEICANWU));
    dashiList.add(new XindeItemConfig(XINDE_GUANGDAO_DASHI_WEICIWU));
    dashiList.add(new XindeItemConfig(XINDE_GUDAO_DASHI));
    dashiList.add(new XindeItemConfig(XINDE_HUNDAO_DASHI_WEICANWU));
    dashiList.add(new XindeItemConfig(XINDE_SHUIDAO_DASHI));
    dashiList.add(new XindeItemConfig(MUDAO_XINDE_DASHI_WEICANWU));
    dashiList.add(new XindeItemConfig(XINDE_XUEDAO_DASHI_WEICANWU));
    dashiList.add(new XindeItemConfig(XINDE_LEIDAO_DASHI));
    XINDE_DASHI_LIST = Collections.unmodifiableList(dashiList);

    // 初始化 T1 - 准大师级心得列表（输出固定为1个）
    List<XindeItemConfig> zhundashiList = new ArrayList<>();
    zhundashiList.add(new XindeItemConfig(XINDE_YANDAO_ZHUNDASHI));
    zhundashiList.add(new XindeItemConfig(XINDE_LIDAO_ZHUNDASHI_WEICANWU));
    zhundashiList.add(new XindeItemConfig(XINDE_YUEDAO_ZHUNDASHI));
    zhundashiList.add(new XindeItemConfig(XINDE_LIANDAO_ZHUNDASHI_WEICANWU));
    zhundashiList.add(new XindeItemConfig(XINDE_JIANDAO_ZHUNDASHI_WEICANWU));
    zhundashiList.add(new XindeItemConfig(XINDE_BINGXUEDAO_ZHUNDASHI_WEICANWU));
    zhundashiList.add(new XindeItemConfig(BIANHUADAO_XINDE_ZHUNDAOSHI_WEICANWU));
    zhundashiList.add(new XindeItemConfig(XINDE_JINDAO_ZHUNDASIHI_WEICANWU));
    zhundashiList.add(new XindeItemConfig(XINDE_GUANGDAO_PUZHUAN_DASHI_WEICANWU));
    zhundashiList.add(new XindeItemConfig(XINDE_GUDAO_ZHUNDASHI));
    zhundashiList.add(new XindeItemConfig(XINDE_HUNDAO_ZHUNDASHI_WEICANWU));
    zhundashiList.add(new XindeItemConfig(XINDE_SHUIDAO_ZHUNDASHI));
    zhundashiList.add(new XindeItemConfig(MUDAO_XINDE_ZHUNDASHI_WEICANWU));
    zhundashiList.add(new XindeItemConfig(XINDE_XUEDAO_ZHUNDASHI_WEICANWU));
    zhundashiList.add(new XindeItemConfig(XINDE_LEIDAO_ZHUNDASHI_WEICANWU));
    XINDE_ZHUNDASHI_LIST = Collections.unmodifiableList(zhundashiList);

    // 初始化 T2 - 普通级心得列表（输出固定为1个）
    List<XindeItemConfig> putongList = new ArrayList<>();
    putongList.add(new XindeItemConfig(XINDE_YANDAO_PUTONG));
    putongList.add(new XindeItemConfig(XINDE_LIDAO_PUTONG_WEICANWU));
    putongList.add(new XindeItemConfig(XINDE_YUEDAO_PUTONG));
    putongList.add(new XindeItemConfig(XINDE_LIANDAO_PUTONG_WEICANWU));
    putongList.add(new XindeItemConfig(XINDE_JIANDAO_PUTONG_WEICANWU));
    putongList.add(new XindeItemConfig(XINDE_BINGXUEDAO_PUTONG_WEICANWU));
    putongList.add(new XindeItemConfig(BIANHUADAO_XINDE_PUTONG_WEICANWU));
    putongList.add(new XindeItemConfig(XINDE_JINDAO_PUTONG_WEICANWU));
    putongList.add(new XindeItemConfig(XINDE_GUANGDAO_PUTONG_WEICANWU));
    putongList.add(new XindeItemConfig(XINDE_GUDAO_PUTONG));
    putongList.add(new XindeItemConfig(XINDE_HUNDAO_PUTONG_WEICANWU));
    putongList.add(new XindeItemConfig(XINDE_SHUIDAO_PUTONG));
    putongList.add(new XindeItemConfig(MUDAO_XINDE_PUTONG_WEICANWU));
    putongList.add(new XindeItemConfig(XINDE_XUEDAO_PUTONG_WEICANWU));
    putongList.add(new XindeItemConfig(XINDE_LEIDAO_PUTONG_WEICANWU));
    XINDE_PUTONG_LIST = Collections.unmodifiableList(putongList);

    // 初始化 T0 - 交易所需物品池（传说级）
    List<TradeCostItemConfig> t0CostList = new ArrayList<>();
    t0CostList.add(new TradeCostItemConfig("minecraft:nether_star", 1, 64));

    t0CostList.add(new TradeCostItemConfig("guzhenren:gu_cai_wang_you_shi", 32, 64)); 
                                                                                // 4转蛊材-忘忧石
    t0CostList.add(new TradeCostItemConfig("guzhenren:sizhuaneniangu", 1, 2));  // 4转恶念蛊
    t0CostList.add(new TradeCostItemConfig("guzhenren:sizhuanzhanyigu", 1, 2)); // 4转战意蛊
    t0CostList.add(new TradeCostItemConfig("guzhenren:zhi_lu_gu_4", 1, 2));     // 4转智颅蛊
    t0CostList.add(new TradeCostItemConfig("guzhenren:baonaogu", 1, 2));        // 4转爆脑蛊
    t0CostList.add(new TradeCostItemConfig("guzhenren:shexingusizhuan", 1, 2)); // 4转爆脑蛊
    TRADE_COST_T0_LIST = Collections.unmodifiableList(t0CostList);

    // 初始化 T1 - 交易所需物品池（史诗级）
    List<TradeCostItemConfig> t1CostList = new ArrayList<>();
    t1CostList.add(new TradeCostItemConfig("minecraft:gold_block", 16, 64));
    t1CostList.add(new TradeCostItemConfig("minecraft:echo_shard", 8, 32));

    t1CostList.add(new TradeCostItemConfig("guzhenren:gu_cai_wu_gen_shui", 8, 32)); 
                                                                                //3转蛊材-无根水 PS: 为什么不是无垠水...
    t1CostList.add(new TradeCostItemConfig("guzhenren:zhi_lu_gu", 1, 2));       //3转智颅蛊
    t1CostList.add(new TradeCostItemConfig("guzhenren:jizhigu", 1, 2));         //3转极智蛊
    t1CostList.add(new TradeCostItemConfig("guzhenren:sanzhuanzhanyigu", 1, 2));//3转战意蛊
    t1CostList.add(new TradeCostItemConfig("guzhenren:e_nian_gu", 1, 2));       //3转恶念蛊
    t1CostList.add(new TradeCostItemConfig("guzhenren:sanzhuanshexinggu", 1, 2));       
                                                                                //3转摄心蛊
    TRADE_COST_T1_LIST = Collections.unmodifiableList(t1CostList);

    // 初始化 T2 - 交易所需物品池（稀有级）
    List<TradeCostItemConfig> t2CostList = new ArrayList<>();
    t2CostList.add(new TradeCostItemConfig("minecraft:emerald_block", 3, 32));
    t2CostList.add(new TradeCostItemConfig("minecraft:iron_block", 10, 32));
    t2CostList.add(new TradeCostItemConfig("minecraft:gold_ingot", 24, 64));
    t2CostList.add(new TradeCostItemConfig("minecraft:ender_pearl", 8, 32));

    t2CostList.add(new TradeCostItemConfig("guzhenren:zhiliaogu", 1, 2));       //2转智疗蛊
    t2CostList.add(new TradeCostItemConfig("guzhenren:dadahuigu", 1, 2));       //2转大慧蛊
    t2CostList.add(new TradeCostItemConfig("guzhenren:dazhigu", 1, 2));         //2转大智蛊
    t2CostList.add(new TradeCostItemConfig("guzhenren:erzhuanzhanyigu", 1, 2)); //2转战意蛊
    t2CostList.add(new TradeCostItemConfig("guzhenren:erzhuanshexingu", 1, 2)); //2转摄心蛊
    t2CostList.add(new TradeCostItemConfig("guzhenren:erzhuaneniangu", 1, 2));  //2转恶念蛊
    t2CostList.add(new TradeCostItemConfig("guzhenren:erzhansuijigu", 1, 2));   //2转随意蛊
    TRADE_COST_T2_LIST = Collections.unmodifiableList(t2CostList);
  }

  /**
   * 从指定列表中随机获取一个心得物品配置
   *
   * @param list 心得物品配置列表
   * @param random 随机数生成器
   * @return 随机选中的心得物品配置，如果列表为空则返回 null
   */
  public static XindeItemConfig getRandomXinde(List<XindeItemConfig> list, RandomSource random) {
    if (list == null || list.isEmpty()) {
      return null;
    }
    return list.get(random.nextInt(list.size()));
  }

  /**
   * 从指定列表中随机获取一个交易所需物品配置
   *
   * @param list 交易所需物品配置列表
   * @param random 随机数生成器
   * @return 随机选中的交易所需物品配置，如果列表为空则返回 null
   */
  public static TradeCostItemConfig getRandomTradeCost(
      List<TradeCostItemConfig> list, RandomSource random) {
    if (list == null || list.isEmpty()) {
      return null;
    }
    return list.get(random.nextInt(list.size()));
  }

  /**
   * 根据交易稀有度等级获取对应的心得列表
   *
   * @param rarity 交易稀有度 (T0=大师级, T1=准大师级, T2=普通级)
   * @return 对应等级的心得列表
   */
  public static List<XindeItemConfig> getXindeListByRarity(
      net.tigereye.chestcavity.soul.entity.SoulClanTradeOffers.TradeRarity rarity) {
    return switch (rarity) {
      case T0 -> XINDE_DASHI_LIST; // 大师级
      case T1 -> XINDE_ZHUNDASHI_LIST; // 准大师级
      case T2 -> XINDE_PUTONG_LIST; // 普通级
      default -> Collections.emptyList();
    };
  }

  /**
   * 根据交易稀有度等级获取对应的交易所需物品池
   *
   * @param rarity 交易稀有度 (T0=传说级, T1=史诗级, T2=稀有级)
   * @return 对应等级的交易所需物品池
   */
  public static List<TradeCostItemConfig> getTradeCostListByRarity(
      net.tigereye.chestcavity.soul.entity.SoulClanTradeOffers.TradeRarity rarity) {
    return switch (rarity) {
      case T0 -> TRADE_COST_T0_LIST; // 传说级
      case T1 -> TRADE_COST_T1_LIST; // 史诗级
      case T2 -> TRADE_COST_T2_LIST; // 稀有级
      default -> Collections.emptyList();
    };
  }
}
