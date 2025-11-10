package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;

/**
 * 通用的战斗状态检测工具（非玩家实体）。
 *
 * <p>参考「蕴剑青莲」的进战检测范式：
 * <ul>
 *   <li>读取当前运行中的攻击Goal名称</li>
 *   <li>与上次快照对比以识别进入/退出战斗</li>
 *   <li>结合是否拥有目标（mob.getTarget()!=null 且存活 且非友方）判断战斗态</li>
 *   <li>维护脱战时间戳（延迟若干tick后判定应回收/关闭）</li>
 * </ul>
 */
public final class CombatDetectionOps {

  private CombatDetectionOps() {}

  /** 战斗阶段汇总（只读快照）。 */
  public record CombatStatus(
      boolean inCombat, boolean enteredCombat, boolean shouldDespawn, List<String> currentGoals) {}

  /**
   * 进战检测：读取/对比Goal快照并更新器官状态，返回战斗阶段结果。
   *
   * @param mob 非玩家实体
   * @param state 器官状态
   * @param active 当前是否处于“已激活/已生成”状态（由调用方提供）
   * @param now 世界时间（tick）
   * @param keyLastGoals 存放上次攻击Goal快照的键（ListTag of String）
   * @param keyDisengagedAt 存放脱战起始时间戳的键（long）
   * @param disengageDelayTicks 脱战延迟（ticks）
   * @return 战斗阶段结果
   */
  public static CombatStatus detectAndUpdate(
      Mob mob,
      OrganState state,
      boolean active,
      long now,
      String keyLastGoals,
      String keyDisengagedAt,
      int disengageDelayTicks) {

    // 1) 当前攻击Goal
    List<String> currentAttackGoals = AIIntrospection.getRunningAttackGoalNames(mob);

    // 2) 上次Goal快照
    ListTag lastGoalsTag = state.getList(keyLastGoals, Tag.TAG_STRING);
    List<String> lastGoals = new ArrayList<>();
    for (int i = 0; i < lastGoalsTag.size(); i++) {
      lastGoals.add(lastGoalsTag.getString(i));
    }

    // 3) 判定
    boolean goalChanged = !currentAttackGoals.equals(lastGoals);
    boolean hasTarget =
        mob.getTarget() != null
            && mob.getTarget().isAlive()
            && !mob.getTarget().isAlliedTo(mob);
    boolean isAttacking = !currentAttackGoals.isEmpty() || hasTarget;

    boolean enteredCombat = false;
    boolean inCombat = false;
    boolean shouldDespawn = false;

    if (!active && isAttacking && (goalChanged || hasTarget)) {
      // 进入战斗
      enteredCombat = true;
      inCombat = true;
      state.setLong(keyDisengagedAt, 0L);
    } else if (active && isAttacking) {
      // 持续战斗
      inCombat = true;
      state.setLong(keyDisengagedAt, 0L);
    } else if (active && !isAttacking) {
      long disengagedAt = state.getLong(keyDisengagedAt, 0L);
      if (disengagedAt == 0L) {
        state.setLong(keyDisengagedAt, now);
      } else if (now - disengagedAt >= disengageDelayTicks) {
        shouldDespawn = true;
        state.setLong(keyDisengagedAt, 0L);
      }
    }

    // 4) 保存当前Goal快照
    ListTag newGoalsTag = new ListTag();
    for (String g : currentAttackGoals) {
      newGoalsTag.add(StringTag.valueOf(g));
    }
    state.setList(keyLastGoals, newGoalsTag);

    return new CombatStatus(inCombat, enteredCombat, shouldDespawn, currentAttackGoals);
  }
}

