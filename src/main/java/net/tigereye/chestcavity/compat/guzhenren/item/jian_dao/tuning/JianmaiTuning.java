package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning;

/**
 * 剑脉蛊（三转·剑道效率型）调参项。
 *
 * <p>包含剑脉效率（JME）系统的所有可调参数：
 * <ul>
 *   <li>JME 计算参数（距离权重、单剑贡献、上限）</li>
 *   <li>临时增幅参数（道痕倍率、主动券参数）</li>
 *   <li>飞剑/玩家增益系数</li>
 *   <li>格挡概率参数</li>
 *   <li>流派经验加成参数</li>
 * </ul>
 */
public final class JianmaiTuning {

  private JianmaiTuning() {}

  // ========== JME 刷新参数 ==========

  /** JME 刷新间隔（tick）：每秒刷新一次。*/
  public static final int JME_TICK_INTERVAL = 20;

  /** JME 扫描半径（格）。*/
  public static final double JME_RADIUS = 12.0;

  /** JME 距离权重指数（Alpha）：w(d) = max(0, 1 - (d/R)^ALPHA)。*/
  public static final double JME_DIST_ALPHA = 1.25;

  /** JME 上限（软上限）。*/
  public static final double JME_MAX_CAP = 2.5;

  // ========== 单剑贡献参数 ==========

  /** 单剑基础贡献（JME per sword）。*/
  public static final double BASE_PER_SWORD = 0.15;

  /** 飞剑等级加成系数：贡献 *= (1 + LEVEL_BONUS * (level - 1))。*/
  public static final double LEVEL_BONUS = 0.1;

  // ========== 临时增幅参数（基于 JME） ==========

  /** 道痕增幅系数：道痕有效倍率 = 1 + DAO_JME_K * JME。*/
  public static final double DAO_JME_K = 0.10;

  /** JME 增幅基础持续时间（tick）：过期后开始衰减。*/
  public static final int AMP_BASE_DURATION_TICKS = 60; // 3秒

  /** JME 增幅宽限期（tick）：在宽限期内线性插值回 1.0。*/
  public static final int AMP_GRACE_TICKS = 40; // 2秒

  // ========== 主动技参数 ==========

  /** 主动技单把飞剑基础道痕增幅。*/
  public static final double ACTIVE_SWORD_BASE = 100.0;

  /** 主动技飞剑经验增幅系数：每把剑额外增幅 = swordExp * ACTIVE_SWORD_EXP_K。*/
  public static final double ACTIVE_SWORD_EXP_K = 0.001;

  /** 主动技资源成本系数：用于缩放消耗的真元对增幅的影响。*/
  public static final double ACTIVE_COST_K = 1.0;

  /** 主动技资源软上限：裁剪消耗到 [0, ACTIVE_SOFTCAP] 用于可选的额外增幅。*/
  public static final double ACTIVE_SOFTCAP = 200.0;

  /** 主动技额外消耗增幅系数：总增幅 += (consumed * ACTIVE_COST_K) * ACTIVE_BONUS_K。*/
  public static final double ACTIVE_BONUS_K = 0.5;

  /** 主动技持续时间（tick）。*/
  public static final int ACTIVE_DURATION_TICKS = 300; // 15秒

  /** 主动技冷却时间（tick）。*/
  public static final int ACTIVE_COOLDOWN_TICKS = 600; // 30秒

  /** 总倍率上限（已弃用，改为直接调整值）。*/
  public static final double AMP_MULT_CAP = 3.0;

  // ========== 飞剑增益系数 ==========

  /** 飞剑速度增益系数：speedMult *= (1 + SWORD_SPD_K * JME)。*/
  public static final double SWORD_SPD_K = 0.15;

  /** 飞剑攻击力增益系数：damageMult *= (1 + SWORD_ATK_K * JME)。*/
  public static final double SWORD_ATK_K = 0.12;

  // ========== 玩家增益系数 ==========

  /** 玩家移速增益系数：speedAttr += PLAYER_SPEED_K * JME。*/
  public static final double PLAYER_SPEED_K = 0.10;

  /** 玩家攻击力增益系数：atkAttr += PLAYER_ATK_K * JME。*/
  public static final double PLAYER_ATK_K = 0.08;

  // ========== 格挡参数（GUARD 模式） ==========

  /** 格挡基础概率。*/
  public static final double BLOCK_BASE = 0.15;

  /** 格挡 JME 加成系数：chance = BLOCK_BASE + BLOCK_K * JME。*/
  public static final double BLOCK_K = 0.20;

  /** 格挡概率上限。*/
  public static final double BLOCK_MAX = 0.95;

  // ========== 流派经验加成参数 ==========

  /** 流派经验加成系数：expMult = 1 + LIUPAI_EXP_K * min(L, LIUPAI_SOFTCAP)^LIUPAI_ALPHA。*/
  public static final double LIUPAI_EXP_K = 0.0005;

  /** 流派经验软上限。*/
  public static final double LIUPAI_SOFTCAP = 50000.0;

  /** 流派经验指数。*/
  public static final double LIUPAI_ALPHA = 0.8;

  // ========== 视觉特效参数（被动） ==========

  /** 脉络扫描间隔（Tick）：每 Tick 更新一次。*/
  public static final int VEIN_RENDER_INTERVAL = 1;

  /** 脉络最大粒子数：防止过载。*/
  public static final int VEIN_MAX_PARTICLES = 50;

  /** 脉络粒子间隔（方块）：每隔1米放置一个粒子。*/
  public static final double VEIN_PARTICLE_SPACING = 1.0;

  /** 脉络透明度（0-1）：能量线的不透明度。*/
  public static final float VEIN_ALPHA = 0.5f;

  /** 脉络颜色（RGB）：淡蓝色。*/
  public static final int[] VEIN_COLOR = {100, 150, 255};

  // ========== 音频特效参数（被动） ==========

  /** 心跳声触发间隔（Tick）：每10秒播放一次。*/
  public static final int HEARTBEAT_INTERVAL = 200;

  /** 心跳声音量：很小声，不打扰。*/
  public static final float HEARTBEAT_VOLUME = 0.3f;

  // ========== 视觉特效参数（主动） ==========

  /** 雷电链持续时间（Tick）：3秒后淡出。*/
  public static final int LIGHTNING_DURATION = 60;

  /** 雷电链粒子生成间隔（Tick）：每0.3秒生成一次粒子。*/
  public static final int LIGHTNING_PARTICLE_INTERVAL = 6;

  /** 雷电链颜色（RGB）：纯蓝色。*/
  public static final int[] LIGHTNING_COLOR = {0, 100, 255};

  /** 雷电链粒子间隔（方块）：每隔0.5米放置一个粒子。*/
  public static final double LIGHTNING_PARTICLE_SPACING = 0.5;

  // ========== 音频特效参数（主动） ==========

  /** 激活音效音量：清晰可听。*/
  public static final float ACTIVATION_VOLUME = 1.0f;

  /** 激活音效范围（格）：附近玩家都能听到。*/
  public static final double ACTIVATION_SOUND_RANGE = 40.0;

  /** 屏幕白闪强度（0-1）：可选效果，强化激活感。*/
  public static final float SCREEN_FLASH_INTENSITY = 0.3f;
}
