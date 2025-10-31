package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator;

/** 锥形命中筛选：只处理纯数学，不依赖MC实体。 */
public final class ConeFilter {
  private ConeFilter() {}

  /**
   * @param lx 观察方向向量x（无需单位长度，函数内部会归一化）
   * @param ly 观察方向向量y
   * @param lz 观察方向向量z
   * @param tx 目标相对向量x
   * @param ty 目标相对向量y
   * @param tz 目标相对向量z
   * @param dotThreshold 余弦阈值（-1~1），大于等于阈值视为命中
   */
  public static boolean matches(
      double lx, double ly, double lz, double tx, double ty, double tz, double dotThreshold) {
    double ln = Math.sqrt(lx * lx + ly * ly + lz * lz);
    double tn = Math.sqrt(tx * tx + ty * ty + tz * tz);
    if (ln <= 1e-12 || tn <= 1e-12) return false;
    double dx = lx / ln, dy = ly / ln, dz = lz / ln;
    double ex = tx / tn, ey = ty / tn, ez = tz / tn;
    double dot = dx * ex + dy * ey + dz * ez;
    return dot >= dotThreshold;
  }
}

