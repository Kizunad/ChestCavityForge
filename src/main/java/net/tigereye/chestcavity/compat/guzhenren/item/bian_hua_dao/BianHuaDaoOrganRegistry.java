package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.ShouPiGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.YinYangZhuanShenGuBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.YuLinGuBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.skills.YuYueSkill;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

/** Declarative registration for 变化道器官（兽皮蛊、鱼鳞蛊）。 */
public final class BianHuaDaoOrganRegistry {

  /** Mod ID used by all Guzhenren organs. */
  private static final String MOD_ID = "guzhenren";

  /** Resource location that uniquely identifies 兽皮蛊。 */
  private static final ResourceLocation SHOU_PI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "shou_pi_gu");

  /** Resource location that uniquely identifies 鱼鳞蛊。 */
  private static final ResourceLocation YU_LIN_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yu_lin_gu");

  private static final ResourceLocation YIN_YANG_ZHUAN_SHEN_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yin_yang_zhuan_shen_gu");

  /**
   * Aggregated registration specs for the变化道器官家族，确保被动能力与主动技能都完成挂接。
   *
   * <p>兽皮蛊：处理被动减伤、反击与慢速自愈。
   *
   * <p>鱼鳞蛊：处理水域强化、主动技能注册以及装备钩子。
   */
  private static final List<OrganIntegrationSpec> SPECS =
      List.of(
          OrganIntegrationSpec.builder(SHOU_PI_GU_ID)
              .addIncomingDamageListener(ShouPiGuOrganBehavior.INSTANCE)
              .addSlowTickListener(ShouPiGuOrganBehavior.INSTANCE)
              .addOnHitListener(ShouPiGuOrganBehavior.INSTANCE)
              .build(),
          OrganIntegrationSpec.builder(YU_LIN_GU_ID)
              .addSlowTickListener(YuLinGuBehavior.INSTANCE)
              .addOnHitListener(YuLinGuBehavior.INSTANCE)
              .addIncomingDamageListener(YuLinGuBehavior.INSTANCE)
              .ensureAttached(YuLinGuBehavior.INSTANCE::ensureAttached)
              .onEquip(YuLinGuBehavior.INSTANCE::onEquip)
              .addRemovalListener(YuLinGuBehavior.INSTANCE)
              .build(),
          OrganIntegrationSpec.builder(YIN_YANG_ZHUAN_SHEN_GU_ID)
              .addSlowTickListener(YinYangZhuanShenGuBehavior.INSTANCE)
              .addIncomingDamageListener(YinYangZhuanShenGuBehavior.INSTANCE)
              .addOnHitListener(YinYangZhuanShenGuBehavior.INSTANCE)
              .build());

  static {
    // Ensure仅初始化“鱼跃破浪”主动
    YuYueSkill.bootstrap();
  }

  private BianHuaDaoOrganRegistry() {}

  public static List<OrganIntegrationSpec> specs() {
    return SPECS;
  }
}
