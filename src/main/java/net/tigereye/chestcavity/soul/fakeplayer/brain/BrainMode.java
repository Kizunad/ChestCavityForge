package net.tigereye.chestcavity.soul.fakeplayer.brain;

/** High-level behavior modes for SoulPlayer. */
public enum BrainMode {
  AUTO, // 自动：根据上下文与订单选择子脑
  COMBAT, // 战斗子脑
  LLM, // 由 LLM/脚本驱动
  SURVIVAL, // 生存：规避/撤退/自愈
  EXPLORATION, // 探索/巡逻
  IDLE // 待机
}
