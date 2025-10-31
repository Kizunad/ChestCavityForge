package net.tigereye.chestcavity.compat.guzhenren.item.combo.bian_hua.shou_pi.common;

import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.behavior.organ.shou_pi.ShouPiGuOps;
import net.tigereye.chestcavity.compat.guzhenren.item.bian_hua_dao.tuning.ShouPiGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;

/**
 * 兽皮蛊组合杀招的通用工具。
 *
 * <p>封装了器官查找、联动计算等常用逻辑，避免在各个技能里重复编写。
 */
public final class ShouPiComboUtil {

  private ShouPiComboUtil() {}

  /**
   * 查找当前胸腔中的兽皮蛊。
   *
   * @return 存在则返回非空 Optional，否则 Optional.empty()
   */
  public static Optional<ItemStack> findOrgan(ChestCavityInstance cc) {
    ItemStack organ = ShouPiGuOps.findOrgan(cc);
    return organ.isEmpty() ? Optional.empty() : Optional.of(organ);
  }

  /** 判断当前胸腔是否已装备兽皮蛊。 */
  public static boolean hasOrgan(ChestCavityInstance cc) {
    return findOrgan(cc).isPresent();
  }

  /** 计算“虎皮蛊 / 铁骨蛊”联动数量（0~2）。 */
  public static int countArmorSynergy(ChestCavityInstance cc) {
    int count = 0;
    if (ShouPiGuOps.hasOrgan(cc, ShouPiGuTuning.HUPI_GU_ID)) {
      count++;
    }
    if (ShouPiGuOps.hasOrgan(cc, ShouPiGuTuning.TIE_GU_GU_ID)) {
      count++;
    }
    return count;
  }

  /** 至少拥有一件联动蛊虫。 */
  public static boolean hasAnyArmorSynergy(ChestCavityInstance cc) {
    return countArmorSynergy(cc) > 0;
  }

  /** 快速获取 OrganState。假定调用方已确认 organ 非空。 */
  public static OrganState resolveState(ItemStack organ) {
    return ShouPiGuOps.resolveState(organ);
  }
}
