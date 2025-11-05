package net.tigereye.chestcavity.compat.guzhenren.flyingsword.calculator.context;

import net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.AIMode;

/**
 * 飞剑计算上下文（纯数据结构，用于传入计算器/钩子）
 * 不直接依赖 Minecraft 运行时对象，方便单元测试。
 */
public class CalcContext {
  // 基础状态
  public AIMode aiMode = AIMode.ORBIT;
  public boolean ownerSprinting = false;
  public boolean breakingBlocks = false;

  // 速度/机动
  public double currentSpeed = 0.0; // 方块/tick
  public double baseSpeed = 0.0; // 方块/tick
  public double maxSpeed = 0.0; // 方块/tick

  // 伤害与耐久
  public double baseDamage = 0.0;
  public double damageFromVelocity = 0.0; // 仅用于调试，可不填

  // 等级/成长
  public int swordLevel = 1;
  public double ownerSwordPathExp = 0.0; // 剑道流派经验

  // 所有者状态
  public double ownerHpPercent = 1.0; // 0~1
  public double ownerJianDaoScar = 0.0; // 剑道道痕强度（可为0）

  // 其他扩展位
  public long worldTime = 0L;
}

