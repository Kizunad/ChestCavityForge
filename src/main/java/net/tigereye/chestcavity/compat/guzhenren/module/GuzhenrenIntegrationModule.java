package net.tigereye.chestcavity.compat.guzhenren.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.BianHuaDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.BingXueDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.du_dao.DuDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.feng_dao.FengDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_cai.GuCaiOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.GuDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.guang_dao.GuangDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.HunDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.JiandaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.kongqiao.KongqiaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.lei_dao.LeiDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.LiDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.MuDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.RenDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.san_zhuan.wu_hang.WuHangOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.shi_dao.ShiDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.shui_dao.ShuiDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.tian_dao.TianDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.tu_dao.TuDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.xin_dao.XinDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.XueDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.YanDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.yu_dao.YuDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.yue_dao.YueDaoOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.yun_dao_cloud.YunDaoCloudOrganRegistry;
import net.tigereye.chestcavity.compat.guzhenren.item.zhi_dao.ZhiDaoOrganRegistry;

/** Module entry point that wires up all Guzhenren organ integrations in the legacy order. */
public final class GuzhenrenIntegrationModule {

  private static final List<Supplier<List<OrganIntegrationSpec>>> SPEC_SUPPLIERS =
      List.of(
          GuCaiOrganRegistry::specs,
          BianHuaDaoOrganRegistry::specs,
          FengDaoOrganRegistry::specs,
          BingXueDaoOrganRegistry::specs,
          DuDaoOrganRegistry::specs,
          GuDaoOrganRegistry::specs,
          GuangDaoOrganRegistry::specs,
          LeiDaoOrganRegistry::specs,
          LiDaoOrganRegistry::specs,
          RenDaoOrganRegistry::specs,
          KongqiaoOrganRegistry::specs,
          MuDaoOrganRegistry::specs,
          HunDaoOrganRegistry::specs,
          TuDaoOrganRegistry::specs,
          ShuiDaoOrganRegistry::specs,
          XueDaoOrganRegistry::specs,
          YanDaoOrganRegistry::specs,
          ZhiDaoOrganRegistry::specs,
          WuHangOrganRegistry::specs,
          XinDaoOrganRegistry::specs,
          ShiDaoOrganRegistry::specs,
          JiandaoOrganRegistry::specs,
          YuDaoOrganRegistry::specs,
          TianDaoOrganRegistry::specs,
          YueDaoOrganRegistry::specs,
          YunDaoCloudOrganRegistry::specs);

  private static boolean initialised;
  private static List<ResourceLocation> registrationOrder = List.of();

  private GuzhenrenIntegrationModule() {}

  public static synchronized void bootstrap() {
    if (initialised) {
      return;
    }
    initialised = true;
    List<OrganIntegrationSpec> specs = new ArrayList<>();
    for (Supplier<List<OrganIntegrationSpec>> supplier : SPEC_SUPPLIERS) {
      List<OrganIntegrationSpec> subset = supplier.get();
      if (subset == null || subset.isEmpty()) {
        continue;
      }
      specs.addAll(subset);
    }
    List<ResourceLocation> expectedOrder =
        specs.stream()
            .map(OrganIntegrationSpec::organId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    registrationOrder = GuzhenrenOrganIntegrationRegistry.registerAll(specs);
    if (!registrationOrder.equals(expectedOrder)) {
      throw new IllegalStateException("Guzhenren integration registration order mismatch");
    }
    if (ChestCavity.LOGGER.isDebugEnabled()) {
      ChestCavity.LOGGER.debug(
          "[compat/guzhenren] Integration registration order: {}", registrationOrder);
    }
  }

  public static List<ResourceLocation> registrationOrder() {
    return Collections.unmodifiableList(registrationOrder);
  }
}
