package net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.YuLinGuBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.skills.YuQunSkill;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.skills.YuShiSummonSharkSkill;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.skills.YuYueSkill;
import net.tigereye.chestcavity.compat.guzhenren.module.OrganIntegrationSpec;

/** Declarative registration entry for鱼鳞蛊，负责挂接被动与主动技能。 */
public final class YuLinGuOrganRegistry {

  private static final String MOD_ID = "guzhenren";

  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "yu_lin_gu");

  private static final List<OrganIntegrationSpec> SPECS =
      List.of(
          OrganIntegrationSpec.builder(ORGAN_ID)
              .addSlowTickListener(YuLinGuBehavior.INSTANCE)
              .addOnHitListener(YuLinGuBehavior.INSTANCE)
              .addIncomingDamageListener(YuLinGuBehavior.INSTANCE)
              .ensureAttached(YuLinGuBehavior.INSTANCE::ensureAttached)
              .onEquip(YuLinGuBehavior.INSTANCE::onEquip)
              .addRemovalListener(YuLinGuBehavior.INSTANCE)
              .build());

  static {
    // Ensure the active skill classes are initialised so they can register hotkeys & listeners.
    YuQunSkill.bootstrap();
    YuYueSkill.bootstrap();
    YuShiSummonSharkSkill.bootstrap();
  }

  private YuLinGuOrganRegistry() {}

  public static List<OrganIntegrationSpec> specs() {
    return SPECS;
  }
}
