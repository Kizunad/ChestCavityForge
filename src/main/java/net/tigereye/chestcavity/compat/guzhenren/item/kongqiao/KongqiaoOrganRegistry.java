package net.tigereye.chestcavity.compat.guzhenren.item.kongqiao;

import java.util.List;
import net.tigereye.chestcavity.compat.guzhenren.item.kongqiao.behavior.DaoHenBehavior;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

/** Registry hook for 空窍（Kong Qiao） organs. Currently only installs Dao Hen logging behaviour. */
public final class KongqiaoOrganRegistry {

  private KongqiaoOrganRegistry() {}

  private static final List<OrganIntegrationSpec> SPECS;

  static {
    DaoHenBehavior.bootstrap();
    SPECS = List.of();
  }

  public static List<OrganIntegrationSpec> specs() {
    return SPECS;
  }
}
