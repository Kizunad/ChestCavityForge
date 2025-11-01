package net.tigereye.chestcavity.compat.guzhenren.domain;

import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.state.DomainConfigOps;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning.JianXinDomainMath;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning.JianXinDomainTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;

/**
 * 通用“剑域”系数计算与广播：与具体领域解耦（不依赖剑心域）。
 */
public final class SwordDomainOps {

  private SwordDomainOps() {}

  /** 每秒调用：若玩家装备了“剑域蛊”，根据半径缩放与资源计算域控系数并写入 NBT。*/
  public static void tickPlayer(ServerPlayer player, long gameTime) {
    if (player == null || player.level().isClientSide()) return;
    if ((gameTime % 20L) != 0L) return; // 每秒一次
    if (!hasJianYuGuEquipped(player)) {
      // 未装备则清除域控广播，避免残留
      DomainTags.clearSwordDomainControl(player);
      return;
    }

    double scale = DomainConfigOps.radiusScale(player);
    double R = JianXinDomainTuning.BASE_RADIUS * Math.max(1.0e-6, scale);

    int daohen = (int) Math.round(ResourceOps.openHandle(player)
        .map(h -> h.read("daohen_jiandao").orElse(0.0)).orElse(0.0));
    int school = (int) Math.round(ResourceOps.openHandle(player)
        .map(h -> h.read("liupai_jiandao").orElse(0.0)).orElse(0.0));

    int rmax = JianXinDomainMath.computeRmax(daohen, school);
    double s = JianXinDomainMath.computeS(R, rmax);

    double pOut = JianXinDomainMath.computePout(s);
    // 基础增益：加法模式 (剑道道痕/1000 * 0.5 = 最高+50%)
    double daohenBonus = (daohen / 1000.0) * 0.5;
    pOut = pOut + daohenBonus;

    // 小域偏置：R≤2时，根据层数额外加成（最高+15%）
    int layers = 0; // TODO: 接入层数系统
    pOut = JianXinDomainMath.applySmallDomainBias(pOut, R, layers);

    double pIn = JianXinDomainMath.computePin(s);
    double pMove = JianXinDomainMath.computePmove(s);
    boolean entityEnabled = !(R <= 1.5);
    double E = entityEnabled ? JianXinDomainMath.computeEntityGate(s) : 0.0;
    double pOutEntity = JianXinDomainMath.computePoutEntity(pOut, E);

    DomainTags.setSwordDomainControl(
        player, R, s, pOut, pIn, pMove, entityEnabled, E, pOutEntity, 0);
  }

  /** 是否装备了“剑域蛊”。*/
  public static boolean hasJianYuGuEquipped(ServerPlayer player) {
    var ccOpt = net.tigereye.chestcavity.registration.CCAttachments.getExistingChestCavity(player);
    if (ccOpt.isEmpty()) return false;
    var cc = ccOpt.get();
    if (cc.inventory == null) return false;
    final net.minecraft.resources.ResourceLocation targetId =
        net.minecraft.resources.ResourceLocation.parse("guzhenren:jianyugu");
    for (int i = 0, n = cc.inventory.getContainerSize(); i < n; i++) {
      net.minecraft.world.item.ItemStack s = cc.inventory.getItem(i);
      if (s.isEmpty()) continue;
      net.minecraft.resources.ResourceLocation id =
          net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem());
      if (id != null && id.equals(targetId)) {
        return true;
      }
    }
    return false;
  }
}

