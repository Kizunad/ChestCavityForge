package net.tigereye.chestcavity.compat.guzhenren.item.li_dao;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.BaiShiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.HeiShiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.HuaShiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.HuangLuoTianNiuGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.JingLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.LongWanQuQuGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.ManLiTianNiuGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.QuanLiYiFuGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.XiongHaoGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.XuLiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.ZhiZhuangGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior.ZiLiGengShengGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.events.LiDaoOrganEvents;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

/** Registry wiring for 力道（三转）器官。 Declarative registry for 力道（三转） organ behaviours. */
public final class LiDaoOrganRegistry {

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation BAI_SHI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "bai_shi_gu");
  private static final ResourceLocation HEI_SHI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "hei_shi_gu");
  private static final ResourceLocation QUAN_LI_YI_FU_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "quan_li_yi_fu_gu");
  private static final ResourceLocation LONG_WAN_QU_QU_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "long_wan_qu_qu_gu");
  private static final ResourceLocation HUANG_LUO_TIAN_NIU_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "huang_luo_tian_niu_gu");
  private static final ResourceLocation MAN_LI_TIAN_NIU_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "man_li_tian_niu_gu");
  private static final ResourceLocation JING_LI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jing_li_gu");
  private static final ResourceLocation XIONG_HAO_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "xiong_hao_gu");
  private static final ResourceLocation HUA_SHI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "hua_shi_gu");
  private static final ResourceLocation ZI_LI_GENG_SHENG_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "zi_li_geng_sheng_gu_3");
  private static final ResourceLocation XU_LI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "xu_li_gu");
  private static final ResourceLocation ZHI_ZHUANG_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "zhi_zhuang_gu");

  private static final List<OrganIntegrationSpec> SPECS;

  static {
    LiDaoOrganEvents.register();
    SPECS =
        List.of(
            OrganIntegrationSpec.builder(BAI_SHI_GU_ID)
                .addSlowTickListener(BaiShiGuOrganBehavior.INSTANCE)
                .addOnHitListener(BaiShiGuOrganBehavior.INSTANCE)
                .ensureAttached(BaiShiGuOrganBehavior.INSTANCE::ensureAttached)
                .build(),
            OrganIntegrationSpec.builder(HEI_SHI_GU_ID)
                .addSlowTickListener(HeiShiGuOrganBehavior.INSTANCE)
                .addOnHitListener(HeiShiGuOrganBehavior.INSTANCE)
                .ensureAttached(HeiShiGuOrganBehavior.INSTANCE::ensureAttached)
                .build(),
            OrganIntegrationSpec.builder(QUAN_LI_YI_FU_GU_ID)
                .addSlowTickListener(QuanLiYiFuGuOrganBehavior.INSTANCE)
                .build(),
            OrganIntegrationSpec.builder(LONG_WAN_QU_QU_GU_ID)
                .addIncomingDamageListener(LongWanQuQuGuOrganBehavior.INSTANCE)
                .build(),
            OrganIntegrationSpec.builder(HUANG_LUO_TIAN_NIU_GU_ID)
                .addSlowTickListener(HuangLuoTianNiuGuOrganBehavior.INSTANCE)
                .build(),
            OrganIntegrationSpec.builder(MAN_LI_TIAN_NIU_GU_ID)
                .addSlowTickListener(ManLiTianNiuGuOrganBehavior.INSTANCE)
                .addOnHitListener(ManLiTianNiuGuOrganBehavior.INSTANCE)
                .build(),
            OrganIntegrationSpec.builder(XIONG_HAO_GU_ID)
                .addSlowTickListener(XiongHaoGuOrganBehavior.INSTANCE)
                .addOnHitListener(XiongHaoGuOrganBehavior.INSTANCE)
                .build(),
            OrganIntegrationSpec.builder(HUA_SHI_GU_ID)
                .addSlowTickListener(HuaShiGuOrganBehavior.INSTANCE)
                .addOnHitListener(HuaShiGuOrganBehavior.INSTANCE)
                .addIncomingDamageListener(HuaShiGuOrganBehavior.INSTANCE)
                .build(),
            OrganIntegrationSpec.builder(JING_LI_GU_ID)
                .addSlowTickListener(JingLiGuOrganBehavior.INSTANCE)
                .addOnHitListener(JingLiGuOrganBehavior.INSTANCE)
                .build(),
            OrganIntegrationSpec.builder(XU_LI_GU_ID)
                .addOnHitListener(XuLiGuOrganBehavior.INSTANCE)
                .ensureAttached(XuLiGuOrganBehavior.INSTANCE::ensureAttached)
                .build(),
            OrganIntegrationSpec.builder(ZI_LI_GENG_SHENG_GU_ID)
                .addSlowTickListener(ZiLiGengShengGuOrganBehavior.INSTANCE)
                .addRemovalListener(ZiLiGengShengGuOrganBehavior.INSTANCE)
                .ensureAttached(ZiLiGengShengGuOrganBehavior.INSTANCE::ensureAttached)
                .build(),
            OrganIntegrationSpec.builder(ZHI_ZHUANG_GU_ID)
                .addSlowTickListener(ZhiZhuangGuOrganBehavior.INSTANCE)
                .build());
  }

  private LiDaoOrganRegistry() {}

  public static List<OrganIntegrationSpec> specs() {
    return SPECS;
  }
}
