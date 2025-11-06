package net.tigereye.chestcavity.compat.guzhenren.shockfield.api;

/**
 * Shockfield 核心数学函数：伤害公式、振幅衰减、周期拉伸、DPS封顶等纯函数。
 *
 * <p>所有常量来自 docs/jiandanggu/config-constants.md，便于参数调整和单元测试。
 */
public final class ShockfieldMath {

  // ==================== 触发与节奏 ====================
  public static final double A0_PLAYER = 0.10; // 玩家OnHit初始振幅
  public static final double A0_SWORD = 0.50; // 飞剑OnHit初始振幅
  public static final double BASE_PERIOD_SEC = 1.0; // 基础周期（秒）
  public static final double RADIAL_SPEED = 8.0; // 波前外扩速度 (m/s)
  public static final double DAMPING_PER_SEC = 0.15; // 振幅衰减率 (/s)
  public static final double PERIOD_STRETCH_PER_SEC = 0.10; // 周期拉伸率 (/s)
  public static final double MIN_AMPLITUDE = 0.02; // 熄灭阈值
  public static final double MAX_LIFETIME_SEC = 10.0; // 最长寿命（秒）
  public static final double WAVE_SPEED_SCALE = 0.6; // 二级波包速度比例

  // ==================== 干涉相位系数 ====================
  public static final double CONSTRUCT_MULT = 1.25; // 同相增强倍数
  public static final double DESTRUCT_MULT = 0.75; // 反相减弱倍数
  public static final double CONSTRUCT_PHASE_DEG = 60.0; // 同相相位角（度）
  public static final double DESTRUCT_PHASE_DEG = 120.0; // 反相相位角（度）

  // ==================== 伤害公式系数 ====================
  public static final double BASE_DMG = 2.0; // 基础伤害
  public static final double K_JD = 0.015; // 剑道道痕系数
  public static final double K_STR = 0.10; // 力量分数系数
  public static final double K_FLOW = 0.005; // 流派经验系数
  public static final double K_TIER = 1.0; // 武器/飞剑阶乘权
  public static final double RESIST_PCT_CAP = 0.60; // 百分比减伤上限
  public static final double ARMOR_FLAT = 0.04; // 护甲固定减伤换算（每点护甲折0.04）
  public static final double DMG_FLOOR = 0.20; // 伤害地板

  // ==================== 频率限制与软上限 ====================
  public static final double PER_TARGET_WAVE_HIT_CD = 0.25; // 同波对同目标命中CD（秒）
  public static final double DPS_CAP_BASE = 30.0; // DPS软封顶基线

  // ==================== 资源与耐久 ====================
  public static final int BURST_TIER = 4; // Burst真元门槛
  public static final double COST_NIANTOU_PER_SEC = 10.0; // 维持每秒消耗（念头）
  public static final double COST_JINGLI_PER_SEC = 10.0; // 维持每秒消耗（精力）
  public static final double FS_DURA_COST_ON_TOUCH_PCT = 0.005; // 飞剑耐久成本（0.5%最大耐久）

  private ShockfieldMath() {}

  /**
   * 振幅衰减：A(t+Δt) = A(t) · e^(−k·Δt)，k = 0.15/s
   *
   * @param currentAmplitude 当前振幅
   * @param deltaSeconds 时间间隔（秒）
   * @return 衰减后的振幅
   */
  public static double applyDamping(double currentAmplitude, double deltaSeconds) {
    return currentAmplitude * Math.exp(-DAMPING_PER_SEC * deltaSeconds);
  }

  /**
   * 周期拉伸：P(t+Δt) = P(t) · (1 + s·Δt)，s = 0.10/s
   *
   * @param currentPeriod 当前周期
   * @param deltaSeconds 时间间隔（秒）
   * @return 拉伸后的周期
   */
  public static double stretchPeriod(double currentPeriod, double deltaSeconds) {
    return currentPeriod * (1.0 + PERIOD_STRETCH_PER_SEC * deltaSeconds);
  }

  /**
   * 计算相位倍率：根据相位差判定干涉类型，返回对应倍率。
   *
   * @param phaseDiffRad 相位差（弧度）
   * @return 伤害倍率
   */
  public static double phaseMultiplier(double phaseDiffRad) {
    PhaseKind kind = PhaseKind.fromPhaseDiff(phaseDiffRad);
    return kind.getDamageMultiplier();
  }

  /**
   * 核心伤害公式：D_core = A_eff · M_phase · (Base + JD·K_JD + STR·K_STR + FLOW·K_FLOW +
   * WTier·K_TIER)
   *
   * @param effectiveAmplitude 有效振幅
   * @param phaseMultiplier 相位倍率
   * @param jianDaoDaohen 剑道道痕
   * @param strength 力量分数
   * @param flowExperience 流派经验
   * @param weaponTier 武器阶级
   * @return 核心伤害
   */
  public static double computeCoreDamage(
      double effectiveAmplitude,
      double phaseMultiplier,
      double jianDaoDaohen,
      double strength,
      double flowExperience,
      double weaponTier) {
    double baseTerm =
        BASE_DMG
            + jianDaoDaohen * K_JD
            + strength * K_STR
            + flowExperience * K_FLOW
            + weaponTier * K_TIER;
    return effectiveAmplitude * phaseMultiplier * baseTerm;
  }

  /**
   * 最终伤害：D_final = max( D_core · (1 − clamp(Resist,0,0.60)) − Armor·0.04 , 0.20 )
   *
   * @param coreDamage 核心伤害
   * @param resistPct 百分比减伤
   * @param armor 护甲值
   * @return 最终伤害
   */
  public static double computeFinalDamage(double coreDamage, double resistPct, double armor) {
    double clampedResist = Math.max(0.0, Math.min(RESIST_PCT_CAP, resistPct));
    double afterResist = coreDamage * (1.0 - clampedResist);
    double afterArmor = afterResist - armor * ARMOR_FLAT;
    return Math.max(DMG_FLOOR, afterArmor);
  }

  /**
   * DPS软封顶：DPS_agg 超过 30·(1 + JD/500) 的部分按 50% 计入
   *
   * @param rawDps 原始DPS
   * @param jianDaoDaohen 剑道道痕
   * @return 封顶后的DPS
   */
  public static double applySoftCap(double rawDps, double jianDaoDaohen) {
    double cap = DPS_CAP_BASE * (1.0 + jianDaoDaohen / 500.0);
    if (rawDps <= cap) {
      return rawDps;
    }
    double excess = rawDps - cap;
    return cap + excess * 0.5;
  }

  /**
   * 判断振幅是否低于熄灭阈值。
   *
   * @param amplitude 当前振幅
   * @return 是否应熄灭
   */
  public static boolean shouldExtinguish(double amplitude) {
    return amplitude < MIN_AMPLITUDE;
  }

  /**
   * 判断寿命是否超过上限。
   *
   * @param ageSeconds 波场年龄（秒）
   * @return 是否应熄灭
   */
  public static boolean hasExceededLifetime(double ageSeconds) {
    return ageSeconds > MAX_LIFETIME_SEC;
  }

  /**
   * 计算波前半径随时间的变化：R(t) = v · t，v = 8.0 m/s
   *
   * @param ageSeconds 波场年龄（秒）
   * @return 波前半径（米）
   */
  public static double computeRadius(double ageSeconds) {
    return RADIAL_SPEED * ageSeconds;
  }
}
