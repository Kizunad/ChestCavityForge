package net.tigereye.chestcavity.compat.guzhenren.flyingsword;

/**
 * 飞剑类型预设配置
 *
 * <p>为三种飞剑类型提供预设的属性修正器：
 * <ul>
 *   <li>DEFAULT - 通用均衡型</li>
 *   <li>ZHENG_DAO - 正道飞剑，速度快、伤害中等</li>
 *   <li>REN_SHOU_ZANG_SHENG - 魔道飞剑，伤害高、速度慢</li>
 * </ul>
 *
 * <p>这些预设可以作为器官或技能生成飞剑时的基础配置，
 * 也可以在此基础上进一步修改。
 */
public final class FlyingSwordTypePresets {

  private FlyingSwordTypePresets() {}

  /**
   * 获取类型对应的预设修正器
   *
   * @param type 飞剑类型
   * @return 预设的属性修正器
   */
  public static FlyingSwordAttributes.AttributeModifiers getPresetModifiers(FlyingSwordType type) {
    return switch (type) {
      case ZHENG_DAO -> createZhengDaoPreset();
      case REN_SHOU_ZANG_SHENG -> createRenShouZangShengPreset();
      default -> createDefaultPreset();
    };
  }

  /**
   * 默认飞剑预设 - 通用均衡型
   *
   * <p>无额外修正，使用基础值
   */
  private static FlyingSwordAttributes.AttributeModifiers createDefaultPreset() {
    return FlyingSwordAttributes.AttributeModifiers.empty();
  }

  /**
   * 正道飞剑预设 - 速度型
   *
   * <p>特点：
   * <ul>
   *   <li>速度快（+30%最大速度）</li>
   *   <li>伤害中等（+10%基础伤害）</li>
   *   <li>耐久较低（+10%耐久损耗）</li>
   *   <li>加速度快（+25%）</li>
   * </ul>
   */
  private static FlyingSwordAttributes.AttributeModifiers createZhengDaoPreset() {
    var modifiers = FlyingSwordAttributes.AttributeModifiers.empty();

    // 速度优势
    modifiers.speedMax += 0.3; // +30% 最大速度
    modifiers.accel += 0.25; // +25% 加速度

    // 伤害中等
    modifiers.damageBase += 0.1; // +10% 基础伤害
    modifiers.velDmgCoef += 0.05; // 轻微提升速度²系数

    // 耐久较低
    modifiers.duraLossRatioMult *= 1.1; // +10% 耐久损耗

    // 转向灵活
    modifiers.turnRate += 0.15; // +15% 转向速率

    return modifiers;
  }

  /**
   * 人兽葬生飞剑预设 - 力量型（魔道）
   *
   * <p>特点：
   * <ul>
   *   <li>伤害高（+40%基础伤害，+20%速度²系数）</li>
   *   <li>速度慢（-15%最大速度）</li>
   *   <li>耐久高（-15%耐久损耗）</li>
   *   <li>破块能力强（+50%破块效率）</li>
   *   <li>范围攻击（启用横扫）</li>
   * </ul>
   */
  private static FlyingSwordAttributes.AttributeModifiers createRenShouZangShengPreset() {
    var modifiers = FlyingSwordAttributes.AttributeModifiers.empty();

    // 伤害优势
    modifiers.damageBase += 0.4; // +40% 基础伤害
    modifiers.velDmgCoef += 0.2; // +20% 速度²系数

    // 速度劣势
    modifiers.speedMax -= 0.15; // -15% 最大速度
    modifiers.accel -= 0.15; // -15% 加速度

    // 耐久优势
    modifiers.duraLossRatioMult *= 0.85; // -15% 耐久损耗

    // 破块能力
    modifiers.blockBreakEff += 0.5; // +50% 破块效率
    modifiers.toolTier = Math.max(modifiers.toolTier, 2); // 至少铁镐等级

    // 范围攻击
    modifiers.enableSweep = true;
    modifiers.sweepPercent = 0.4; // 40% 范围伤害

    // 转向略慢
    modifiers.turnRate -= 0.1; // -10% 转向速率

    return modifiers;
  }

  /**
   * 创建自定义预设
   *
   * <p>在基础类型预设上应用额外修正
   *
   * @param baseType 基础类型
   * @param customModifiers 自定义修正器
   * @return 合并后的修正器
   */
  public static FlyingSwordAttributes.AttributeModifiers createCustomPreset(
      FlyingSwordType baseType, FlyingSwordAttributes.AttributeModifiers customModifiers) {

    var preset = getPresetModifiers(baseType);

    // 合并修正器（加法操作）
    preset.damageBase += customModifiers.damageBase;
    preset.velDmgCoef += customModifiers.velDmgCoef;
    preset.speedMax += customModifiers.speedMax;
    preset.accel += customModifiers.accel;
    preset.turnRate += customModifiers.turnRate;
    preset.duraLossRatioMult *= customModifiers.duraLossRatioMult;
    preset.upkeepRate += customModifiers.upkeepRate;
    preset.blockBreakEff += customModifiers.blockBreakEff;
    preset.toolTier = Math.max(preset.toolTier, customModifiers.toolTier);
    preset.enableSweep = preset.enableSweep || customModifiers.enableSweep;
    preset.sweepPercent = Math.max(preset.sweepPercent, customModifiers.sweepPercent);

    return preset;
  }

  /**
   * 获取类型的描述信息
   *
   * @param type 飞剑类型
   * @return 类型描述
   */
  public static String getTypeDescription(FlyingSwordType type) {
    return switch (type) {
      case ZHENG_DAO -> "正道飞剑 - 速度快、伤害中等、转向灵活";
      case REN_SHOU_ZANG_SHENG -> "人兽葬生 - 伤害高、速度慢、耐久强、范围攻击";
      default -> "默认飞剑 - 通用均衡型";
    };
  }

  /**
   * 获取类型的特性标签
   *
   * @param type 飞剑类型
   * @return 特性标签数组
   */
  public static String[] getTypeTags(FlyingSwordType type) {
    return switch (type) {
      case ZHENG_DAO -> new String[] {"速度型", "灵活", "正道"};
      case REN_SHOU_ZANG_SHENG -> new String[] {"力量型", "坚固", "魔道", "范围"};
      default -> new String[] {"通用", "均衡"};
    };
  }
}
