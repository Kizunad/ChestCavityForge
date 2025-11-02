package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

/**
 * AI 观察助手：读取实体当前正在运行的 Goal（仅限 Mob）。
 */
public final class AIIntrospection {

  private AIIntrospection() {}

  /** 返回当前 {@code goalSelector} 正在运行的 Goal 的类名。 */
  public static List<String> getRunningGoalNames(LivingEntity entity) {
    Mob mob = asMob(entity);
    if (mob == null) return Collections.emptyList();
    return readRunningWrappedGoals(mob.goalSelector).stream()
        .map(WrappedGoal::getGoal)
        .filter(Objects::nonNull)
        .map(g -> g.getClass().getName())
        .collect(Collectors.toList());
  }

  /** 返回当前 {@code targetSelector} 正在运行的 Goal 的类名。 */
  public static List<String> getRunningTargetGoalNames(LivingEntity entity) {
    Mob mob = asMob(entity);
    if (mob == null) return Collections.emptyList();
    return readRunningWrappedGoals(mob.targetSelector).stream()
        .map(WrappedGoal::getGoal)
        .filter(Objects::nonNull)
        .map(g -> g.getClass().getName())
        .collect(Collectors.toList());
  }

  /**
   * 以可读字符串返回当前正在运行的 Goal（含 goal/target 两组）。
   * 形如：
   * goal=[net.minecraft.world.entity.ai.goal.MeleeAttackGoal, ...]; target=[...]
   */
  public static String debugRunningGoals(LivingEntity entity) {
    List<String> goal = new ArrayList<>(getRunningGoalNames(entity));
    List<String> target = new ArrayList<>(getRunningTargetGoalNames(entity));
    return "goal=" + goal + "; target=" + target;
  }

  private static Mob asMob(LivingEntity e) {
    return (e instanceof Mob m) ? m : null;
  }

  // =====================
  // Attack/Target 识别
  // =====================

  /** 获取正在运行的“攻击类”Goal（Melee/Ranged/Crossbow...）的类名。 */
  public static List<String> getRunningAttackGoalNames(LivingEntity entity) {
    Mob mob = asMob(entity);
    if (mob == null) return Collections.emptyList();
    return readRunningWrappedGoals(mob.goalSelector).stream()
        .map(WrappedGoal::getGoal)
        .filter(Objects::nonNull)
        .map(g -> g.getClass().getName())
        .filter(AIIntrospection::isAttackGoalClassName)
        .collect(Collectors.toList());
  }

  /** 获取正在运行的“攻击类”Goal 的原始对象列表（可用于进一步反射/字段探查）。 */
  public static List<Goal> getRunningAttackGoals(LivingEntity entity) {
    Mob mob = asMob(entity);
    if (mob == null) return Collections.emptyList();
    return readRunningWrappedGoals(mob.goalSelector).stream()
        .map(WrappedGoal::getGoal)
        .filter(Objects::nonNull)
        .filter(g -> isAttackGoalClassName(g.getClass().getName()))
        .collect(Collectors.toList());
  }

  /** 获取可用（已添加）但不一定在运行的“攻击类”Goal 的类名。 */
  public static List<String> getAvailableAttackGoalNames(LivingEntity entity) {
    Mob mob = asMob(entity);
    if (mob == null) return Collections.emptyList();
    return findWrappedGoalsSet(mob.goalSelector).stream()
        .map(WrappedGoal::getGoal)
        .filter(Objects::nonNull)
        .map(g -> g.getClass().getName())
        .filter(AIIntrospection::isAttackGoalClassName)
        .collect(Collectors.toList());
  }

  /** 获取正在运行的“选敌/仇恨”类 TargetGoal 的类名。 */
  public static List<String> getRunningTargetingGoalNames(LivingEntity entity) {
    Mob mob = asMob(entity);
    if (mob == null) return Collections.emptyList();
    return readRunningWrappedGoals(mob.targetSelector).stream()
        .map(WrappedGoal::getGoal)
        .filter(Objects::nonNull)
        .map(g -> g.getClass().getName())
        .filter(AIIntrospection::isTargetingGoalClassName)
        .collect(Collectors.toList());
  }

  private static boolean isAttackGoalClassName(String fqcn) {
    if (fqcn == null) return false;
    // 直接匹配常见攻击 Goal 的类名片段（兼容不同映射与模组的命名惯例）
    String s = fqcn;
    return s.endsWith("MeleeAttackGoal")
        || s.endsWith("RangedBowAttackGoal")
        || s.endsWith("RangedCrossbowAttackGoal")
        || s.endsWith("CrossbowAttackGoal")
        || s.endsWith("RangedAttackGoal")
        || s.contains("AttackGoal");
  }

  private static boolean isTargetingGoalClassName(String fqcn) {
    if (fqcn == null) return false;
    String s = fqcn;
    return s.endsWith("NearestAttackableTargetGoal")
        || s.endsWith("HurtByTargetGoal")
        || s.contains("TargetGoal");
  }

  // 兼容不同映射/版本：反射读取 GoalSelector 中的 WrappedGoal 集合，并筛选 isRunning()
  private static List<WrappedGoal> readRunningWrappedGoals(Object goalSelector) {
    if (goalSelector == null) return Collections.emptyList();
    try {
      Set<WrappedGoal> all = findWrappedGoalsSet(goalSelector);
      if (all == null || all.isEmpty()) return Collections.emptyList();
      List<WrappedGoal> running = new ArrayList<>();
      for (WrappedGoal wg : all) {
        try {
          if (wg != null && wg.isRunning()) {
            running.add(wg);
          }
        } catch (Throwable ignored) {
        }
      }
      return running;
    } catch (Throwable ignored) {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  private static Set<WrappedGoal> findWrappedGoalsSet(Object goalSelector) {
    // 常见字段名：availableGoals/targets（Mojang 映射）；也尝试扫描 Set<WrappedGoal> 类型字段
    String[] candidates = new String[] {"availableGoals", "goals", "targets"};
    Class<?> cls = goalSelector.getClass();

    for (String name : candidates) {
      try {
        Field f = cls.getDeclaredField(name);
        f.setAccessible(true);
        Object val = f.get(goalSelector);
        if (val instanceof Set<?> set) {
          Set<WrappedGoal> out = new HashSet<>();
          for (Object o : set) {
            if (o instanceof WrappedGoal wg) out.add(wg);
          }
          if (!out.isEmpty()) return out;
        }
      } catch (Throwable ignored) {
      }
    }

    // 回退：扫描所有字段，找第一个包含 WrappedGoal 的 Set
    for (Field f : cls.getDeclaredFields()) {
      if (!java.util.Set.class.isAssignableFrom(f.getType())) continue;
      try {
        f.setAccessible(true);
        Object val = f.get(goalSelector);
        if (val instanceof Set<?> set) {
          for (Object o : set) {
            if (o instanceof WrappedGoal) {
              Set<WrappedGoal> out = new HashSet<>();
              for (Object oo : set) {
                if (oo instanceof WrappedGoal wg) out.add(wg);
              }
              return out;
            }
          }
        }
      } catch (Throwable ignored) {
      }
    }
    return Collections.emptySet();
  }
}
