package net.tigereye.chestcavity.compat.guzhenren.item.synergy.li_dao;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;

/**
 * 一斤力蛊的联动判定：负责检测胸腔内的白豕蛊/冰肌蛊并提供整合后的数值快照。
 *
 * <p>设计思路：主行为每 tick 查询一次，避免重复扫描同时保证可测试性。
 */
public final class YiJingLiGuSynergyBehavior extends AbstractGuzhenrenOrganBehavior {

  public static final YiJingLiGuSynergyBehavior INSTANCE = new YiJingLiGuSynergyBehavior();

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation BAI_SHI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "bai_shi_gu");
  private static final ResourceLocation BING_JI_GU_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "bing_ji_gu");

  private YiJingLiGuSynergyBehavior() {}

  /** 扫描胸腔并返回联动状态。若胸腔为空或缺少目标器官，返回空快照。 */
  public SynergySnapshot evaluate(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return SynergySnapshot.EMPTY;
    }
    boolean hasBaiShi = false;
    boolean hasBingJi = false;
    int size = cc.inventory.getContainerSize();
    for (int i = 0; i < size && !(hasBaiShi && hasBingJi); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack == null || stack.isEmpty()) {
        continue;
      }
      if (!hasBaiShi && matchesOrgan(stack, BAI_SHI_GU_ID)) {
        hasBaiShi = true;
        continue;
      }
      if (!hasBingJi && matchesOrgan(stack, BING_JI_GU_ID)) {
        hasBingJi = true;
      }
    }
    if (!hasBaiShi && !hasBingJi) {
      return SynergySnapshot.EMPTY;
    }
    return new SynergySnapshot(hasBaiShi, hasBingJi);
  }

  /** 封装联动查询结果，便于主行为读取。 */
  public static final class SynergySnapshot {
    static final SynergySnapshot EMPTY = new SynergySnapshot(false, false);

    private final boolean baiShiGu;
    private final boolean bingJiGu;

    public SynergySnapshot(boolean baiShiGu, boolean bingJiGu) {
      this.baiShiGu = baiShiGu;
      this.bingJiGu = bingJiGu;
    }

    public boolean hasBaiShiGu() {
      return baiShiGu;
    }

    public boolean hasBingJiGu() {
      return bingJiGu;
    }

    /** 白豕蛊/冰肌蛊各自降低 10% 饥饿消耗，可叠加并在上层行为处钳制上限。 */
    public double hungerReduction() {
      double reduction = 0.0D;
      if (baiShiGu) {
        reduction += 0.10D;
      }
      if (bingJiGu) {
        reduction += 0.10D;
      }
      return reduction;
    }
  }
}
