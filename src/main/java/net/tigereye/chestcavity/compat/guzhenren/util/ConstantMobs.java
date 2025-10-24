package net.tigereye.chestcavity.compat.guzhenren.util;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Lists additional modded hostile mobs for intimidation checks. */
public final class ConstantMobs {

  private static final Set<ResourceLocation> HOSTILE_IDS =
      Set.of(
          ResourceLocation.fromNamespaceAndPath("guzhenren", "bing_jiao_sha"), // 冰鲛鲨
          ResourceLocation.fromNamespaceAndPath("guzhenren", "bing_lin_sha"), // 冰鳞鲨
          ResourceLocation.fromNamespaceAndPath("guzhenren", "bing_po_long_wen_sha"), // 冰魄龙纹鲨
          ResourceLocation.fromNamespaceAndPath("guzhenren", "dian_lang"), // 电狼
          ResourceLocation.fromNamespaceAndPath("guzhenren", "dian_xiong"), // 电熊
          ResourceLocation.fromNamespaceAndPath("guzhenren", "dilaoleiguantoulang"), // 雷冠头狼(地牢)
          ResourceLocation.fromNamespaceAndPath("guzhenren", "han_yuan_bing_sha"), // 寒渊冰鲨
          ResourceLocation.fromNamespaceAndPath("guzhenren", "hao_dian_lang"), // 豪电狼
          ResourceLocation.fromNamespaceAndPath("guzhenren", "hong_xiong"), // 红熊
          ResourceLocation.fromNamespaceAndPath("guzhenren", "hu"), // 虎
          ResourceLocation.fromNamespaceAndPath("guzhenren", "hui_lang"), // 灰狼
          ResourceLocation.fromNamespaceAndPath("guzhenren", "hui_xiong"), // 灰熊
          ResourceLocation.fromNamespaceAndPath("guzhenren", "huoyanxiong"), // 火焰熊
          ResourceLocation.fromNamespaceAndPath("guzhenren", "jinfenghu"), // 金锋虎
          ResourceLocation.fromNamespaceAndPath("guzhenren", "jinrenwanghu"), // 金刃王虎
          ResourceLocation.fromNamespaceAndPath("guzhenren", "lei_dian_lang"), // 狂电狼
          ResourceLocation.fromNamespaceAndPath("guzhenren", "lei_guan_tou_lang"), // 雷冠头狼
          ResourceLocation.fromNamespaceAndPath("guzhenren", "liaoyuanhuoxiong"), // 燎原火熊
          ResourceLocation.fromNamespaceAndPath("guzhenren", "liaoyuanhuoxiongdilao"), // 燎原火熊(地牢)
          ResourceLocation.fromNamespaceAndPath("guzhenren", "lieyanxiong"), // 烈焰熊
          ResourceLocation.fromNamespaceAndPath("guzhenren", "quan"), // 电狼
          ResourceLocation.fromNamespaceAndPath("guzhenren", "shan_mai_di_yan_quan"), // 山脉地岩犬
          ResourceLocation.fromNamespaceAndPath("guzhenren", "tu_lang"), // 土狼
          ResourceLocation.fromNamespaceAndPath("guzhenren", "tu_quan"), // 土犬
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xiao_dian_lang"), // 小电狼
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xiao_hao_dian_lang"), // 小豪电狼
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xiao_hu"), // 小虎
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xiao_huo_xiong"), // 小电熊
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xiao_huo_yan_xiong"), // 小火焰熊
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xiao_jin_feng_hu"), // 小金锋虎
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xiao_jin_ren_wang_hu"), // 小金刃王虎
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xiao_lei_dian_lang"), // 小雷电狼
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xiao_lei_guan_tou_lang"), // 小雷冠头狼
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xiao_liao_yuan_huo_xiong"), // 小燎原火熊
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xiao_lie_yan_xiong"), // 小烈焰熊
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xiao_shan_mai_di_yan_quan"), // 小山脉地岩犬
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xiao_tu_quan"), // 小土犬
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xiao_xiong_sha_bai_hu"), // 小凶煞白虎
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xiao_yan_xu_quan"), // 小岩戌犬
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xiao_yao_ya_quan"), // 小垚犽犬
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xiao_yue_ao_yan_quan"), // 小岳獒岩犬
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xiong"), // 熊
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xiongshabaihu"), // 凶煞白虎
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xuan_shuang_bing_sha"), // 玄霜鲛鲨
          ResourceLocation.fromNamespaceAndPath("guzhenren", "yan_xu_quan"), // 岩戌犬
          ResourceLocation.fromNamespaceAndPath("guzhenren", "yao_ya_quan"), // 垚犽犬
          ResourceLocation.fromNamespaceAndPath("guzhenren", "yue_ao_yan_quan"), // 岳獒岩犬
          ResourceLocation.fromNamespaceAndPath("guzhenren", "bai_xiang_xian_she"), // 白相仙蛇
          ResourceLocation.fromNamespaceAndPath("guzhenren", "biao"), // 彪
          ResourceLocation.fromNamespaceAndPath("guzhenren", "gong_bing_yi"), // 工兵蚁
          ResourceLocation.fromNamespaceAndPath("guzhenren", "jiang_jun_yi"), // 将军蚁
          ResourceLocation.fromNamespaceAndPath("guzhenren", "wang_yi"), // 王蚁
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xiao_biao"), // 小彪
          ResourceLocation.fromNamespaceAndPath("guzhenren", "xing_jun_yi") // 行军蚁
          );

  private ConstantMobs() {}

  public static boolean isConsideredHostile(LivingEntity entity) {
    if (entity == null) {
      return false;
    }
    ResourceLocation id = entity.getType().builtInRegistryHolder().key().location();
    return HOSTILE_IDS.contains(id);
  }
}
