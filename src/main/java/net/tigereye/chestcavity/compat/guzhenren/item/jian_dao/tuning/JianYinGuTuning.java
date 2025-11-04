package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning;

import net.minecraft.resources.ResourceLocation;

/**
 * 剑引蛊占位调参项。
 *
 * <p>仅承载后续实现可调的冷却、资源与状态键常量，当前值为占位配置。
 */
public final class JianYinGuTuning {

  private JianYinGuTuning() {}

  public static final String MOD_ID = "guzhenren";

  /** 物品 ID。 */
  public static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jianyingu");

  /** 主动技能 ID。 */
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_yin_guidance");

  /** 指挥界面快捷开启技能 ID。 */
  public static final ResourceLocation ABILITY_UI_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_yin_command_ui");

  /** 被动逻辑占位技能 ID（用于记账/提示）。 */
  public static final ResourceLocation PASSIVE_SKILL_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_dao/jian_yin/passive_anchor");

  /** OrganState 根键。 */
  public static final String STATE_ROOT = "JianYinGu";

  /** 主动技能冷却时间戳键。 */
  public static final String KEY_READY_TICK = "JianYinReadyAt";

  /** 主动技能最近激活时间。 */
  public static final String KEY_LAST_TRIGGER_TICK = "JianYinLastTriggered";

  /** 被动打点时间戳。 */
  public static final String KEY_PASSIVE_PULSE_TICK = "JianYinPassivePulseAt";

  /** 冷却（tick）。 */
  public static final int ACTIVE_COOLDOWN_T = 20 * 12; // 12s 占位

  /** 真元消耗。 */
  public static final double ACTIVE_ZHENYUAN_COST = 160.0;

  /** 精力消耗。 */
  public static final double ACTIVE_JINGLI_COST = 6.0;

  /** 被动判定范围（未来用于引导效果）。 */
  public static final double PASSIVE_RANGE = 5.0;

  // ===== 指挥棒相关 =====

  /** 指挥扫描最大距离（沿视线）。 */
  public static final double COMMAND_SCAN_DISTANCE = 28.0;

  /** 指挥扫描半径（视线周围的圆柱半径）。 */
  public static final double COMMAND_SCAN_RADIUS = 2.5;

  /** 目标高亮与标记持续时间（tick）。 */
  public static final int COMMAND_MARK_DURATION_T = 20 * 12; // 12s

  /** 指挥执行持续时间（tick）。 */
  public static final int COMMAND_EXECUTE_DURATION_T = 20 * 120; // 120s 2 mins

  /** 指挥提示冷却（避免频繁刷屏）。 */
  public static final int COMMAND_MESSAGE_COOLDOWN_T = 20 * 2;

  /** 指挥优先级（越高越能覆盖原有意图）。 */
  public static final double COMMAND_INTENT_PRIORITY = 120.0;

  /** 指挥默认战术。 */
  public static final String COMMAND_DEFAULT_TACTIC = "focus_fire";

  // ===== 守护格挡被动 =====

  /** 单把守护飞剑触发格挡的基础概率。 */
  public static final double GUARD_BLOCK_BASE_CHANCE = 0.18;

  /** 格挡触发后给予飞剑的移动速度药水等级（0=一级）。 */
  public static final int GUARD_BLOCK_SWORD_SPEED_AMPLIFIER = 1;

  /** 格挡触发后给予飞剑的移动速度持续时间（tick）。 */
  public static final int GUARD_BLOCK_SWORD_SPEED_DURATION_T = 20 * 5;

  /** 格挡触发后给予敌方的减速等级。 */
  public static final int GUARD_BLOCK_ENEMY_SLOW_AMPLIFIER = 0;

  /** 格挡触发后给予敌方的虚弱等级。 */
  public static final int GUARD_BLOCK_ENEMY_WEAKNESS_AMPLIFIER = 0;

  /** 敌方减速/虚弱持续时间（tick）。 */
  public static final int GUARD_BLOCK_ENEMY_DEBUFF_DURATION_T = 20 * 4;

  /** 格挡后消耗飞剑耐久占造成伤害的比例。 */
  public static final double GUARD_BLOCK_DURABILITY_RATIO = 0.6;
}
