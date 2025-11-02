package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator;

import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning.JianXinDomainTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

/**
 * 剑心蛊相关计算：资源读取、域等级推导等。
 */
public final class JianXinGuCalc {

  private JianXinGuCalc() {}

  /** 读取玩家的剑道道痕（四舍五入为 int）。*/
  public static int readJiandaoDaohenI(ServerPlayer player) {
    return (int)
        Math.round(
            readResource(player, "daohen_jiandao")
                .orElse(0.0));
  }

  /** 读取玩家的剑道流派经验（四舍五入为 int）。*/
  public static int readJiandaoSchoolExpI(ServerPlayer player) {
    return (int)
        Math.round(
            readResource(player, "liupai_jiandao")
                .orElse(0.0));
  }

  /** 计算领域等级（5/6），纯函数实现，与 DomainTuning 一致。*/
  public static int computeDomainLevel(int daohen, int schoolExp) {
    double totalPower =
        daohen * JianXinDomainTuning.DAOHEN_WEIGHT + schoolExp * JianXinDomainTuning.SCHOOL_EXP_WEIGHT;
    return totalPower > JianXinDomainTuning.LEVEL_THRESHOLD
        ? JianXinDomainTuning.MAX_LEVEL
        : JianXinDomainTuning.MIN_LEVEL;
  }

  /** 计算领域等级（从玩家资源读取）。*/
  public static int computeDomainLevel(ServerPlayer player) {
    int d = readJiandaoDaohenI(player);
    int e = readJiandaoSchoolExpI(player);
    return computeDomainLevel(d, e);
  }

  private static OptionalDouble readResource(ServerPlayer player, String key) {
    Optional<GuzhenrenResourceBridge.ResourceHandle> h = ResourceOps.openHandle(player);
    if (h.isEmpty()) return OptionalDouble.empty();
    return h.get().read(key);
  }
}
