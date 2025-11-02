package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning;

/**
 * 剑心蛊“定心返本”被动的调参。
 */
public final class JianXinGuPassiveTuning {
  private JianXinGuPassiveTuning() {}

  /** 每次受到控制类效果时累积量。*/
  public static final int FOCUS_GAIN_PER_CONTROL = 20;

  /** 自然衰减（每秒）。*/
  public static final int FOCUS_DECAY_PER_SEC = 10;

  /** 上限。*/
  public static final int FOCUS_MAX = 100;

  /** 触发后锁定时长（tick）。*/
  public static final int FOCUS_LOCK_T = 20 * 30; // 30s

  /** 触发时授予的无视打断持续（tick）。*/
  public static final int UNBREAKABLE_FOCUS_T =
      net.tigereye.chestcavity.compat.guzhenren.domain.impl.jianxin.tuning
          .JianXinDomainTuning.UNBREAKABLE_FOCUS_DURATION; // 40

  /** 可清除负面效果列表（以 ResourceLocation 文本定义）。*/
  public static final String[] NEGATIVE_EFFECT_IDS = new String[] {
    "guzhenren:mabi",
    "guzhenren:jifei",
    "guzhenren:baonaogubuff",
    "guzhenren:suwanwanrenwu",
    "guzhenren:chenmo_0",
    "guzhenren:chenmo_2",
    "guzhenren:shengchengdillao",
    "guzhenren:hhanleng",
    "guzhenren:chenmo",
    "guzhenren:chenzhong",
    "guzhenren:shengchengchushiyiji"
  };
}
