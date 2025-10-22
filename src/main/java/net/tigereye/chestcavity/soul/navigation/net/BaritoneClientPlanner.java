package net.tigereye.chestcavity.soul.navigation.net;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.soul.navigation.barintegrate.BaritoneFacade;
import net.tigereye.chestcavity.soul.util.SoulLog;

final class BaritoneClientPlanner {
  private BaritoneClientPlanner() {}

  static void onRequest(SoulNavPlanRequestPayload req) {
    List<Vec3> points = new ArrayList<>();
    try {
      if (BaritoneFacade.isAvailable()) {
        points = tryPlanWithBaritone(req.from(), req.target());
      }
      if (points.isEmpty()) {
        // 保底：直线分段，尽量不中断服务端逻辑
        points = straightLine(req.from(), req.target(), 24);
      }
    } catch (Throwable t) {
      if (SoulLog.DEBUG_LOGS)
        SoulLog.info("[soul][nav][baritone] client plan err: {}", t.toString());
    }
    var conn = Minecraft.getInstance().getConnection();
    if (conn != null) {
      conn.send(new SoulNavPlanResponsePayload(req.requestId(), points));
    }
  }

  // 反射尝试：调用 BaritoneAPI 获取自定义目标流程，提交目标，并尽力读取当前路径节点。
  // 不接管移动，只做计算；若任一步失败则返回空列表。
  private static List<Vec3> tryPlanWithBaritone(Vec3 from, Vec3 target) throws Exception {
    List<Vec3> out = new ArrayList<>();
    Class<?> api =
        tryLoad(
            "net.tigereye.chestcavity.shadow.baritone.api.BaritoneAPI",
            "net.tigereye.chestcavity.shadow.com.github.cabaletta.baritone.api.BaritoneAPI",
            "baritone.api.BaritoneAPI",
            "com.github.cabaletta.baritone.api.BaritoneAPI");
    if (api == null) return out;
    Object provider = api.getMethod("getProvider").invoke(null);
    Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
    Object custom = baritone.getClass().getMethod("getCustomGoalProcess").invoke(baritone);

    // goal = new GoalBlock(int x,int y,int z) 或 GoalXZ(int x,int z)
    int gx = (int) Math.floor(target.x);
    int gy = (int) Math.floor(target.y);
    int gz = (int) Math.floor(target.z);
    Object goal = null;
    Class<?> goalBlock =
        tryLoad(
            "net.tigereye.chestcavity.shadow.baritone.api.pathing.goals.GoalBlock",
            "net.tigereye.chestcavity.shadow.com.github.cabaletta.baritone.api.pathing.goals.GoalBlock",
            "baritone.api.pathing.goals.GoalBlock",
            "com.github.cabaletta.baritone.api.pathing.goals.GoalBlock");
    if (goalBlock != null) {
      try {
        goal = goalBlock.getConstructor(int.class, int.class, int.class).newInstance(gx, gy, gz);
      } catch (NoSuchMethodException ignored) {
      }
    }
    if (goal == null) {
      Class<?> goalXZ =
          tryLoad(
              "net.tigereye.chestcavity.shadow.baritone.api.pathing.goals.GoalXZ",
              "net.tigereye.chestcavity.shadow.com.github.cabaletta.baritone.api.pathing.goals.GoalXZ",
              "baritone.api.pathing.goals.GoalXZ",
              "com.github.cabaletta.baritone.api.pathing.goals.GoalXZ");
      if (goalXZ != null) {
        goal = goalXZ.getConstructor(int.class, int.class).newInstance(gx, gz);
      }
    }
    if (goal == null) return out;

    // 首选 setGoalAndPath(goal)
    boolean submitted = false;
    try {
      custom.getClass().getMethod("setGoalAndPath", goal.getClass()).invoke(custom, goal);
      submitted = true;
    } catch (NoSuchMethodException ignored) {
      try {
        custom.getClass().getMethod("setGoal", goal.getClass()).invoke(custom, goal);
        submitted = true;
      } catch (NoSuchMethodException ignored2) {
      }
    }
    if (!submitted) return out;

    // 尝试读取 path 对象：优先从 pathingBehavior，其次从 customGoalProcess
    Object path = null;
    try {
      Object behavior = baritone.getClass().getMethod("getPathingBehavior").invoke(baritone);
      try {
        path = behavior.getClass().getMethod("getPath").invoke(behavior);
      } catch (NoSuchMethodException ignored) {
        path = behavior.getClass().getField("path").get(behavior);
      }
    } catch (Throwable ignored) {
    }

    if (path == null) {
      try {
        path = custom.getClass().getMethod("getPath").invoke(custom);
      } catch (Throwable ignored) {
      }
    }
    if (path == null) return out;

    // 读取节点集合：尝试 positions()/getPositions()/path()
    List<Vec3> nodes = extractNodes(path);
    if (!nodes.isEmpty()) out.addAll(nodes);
    return out;
  }

  @SuppressWarnings("unchecked")
  private static List<Vec3> extractNodes(Object path) {
    List<Vec3> points = new ArrayList<>();
    Object list = null;
    try {
      list = path.getClass().getMethod("positions").invoke(path);
    } catch (Throwable ignored) {
    }
    if (list == null) {
      try {
        list = path.getClass().getMethod("getPositions").invoke(path);
      } catch (Throwable ignored) {
      }
    }
    if (list == null) {
      try {
        list = path.getClass().getMethod("path").invoke(path);
      } catch (Throwable ignored) {
      }
    }
    if (list instanceof Iterable<?> it) {
      for (Object o : it) {
        Vec3 v = asVec3(o);
        if (v != null) points.add(v);
      }
    }
    return points;
  }

  private static Vec3 asVec3(Object node) {
    if (node == null) return null;
    try {
      // 支持 BetterBlockPos / Node 等常见字段名
      int x = getInt(node, "x");
      int y = getInt(node, "y");
      int z = getInt(node, "z");
      return new Vec3(x + 0.5, y + 0.0, z + 0.5);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static int getInt(Object obj, String name) throws Exception {
    try {
      return obj.getClass().getField(name).getInt(obj);
    } catch (NoSuchFieldException e) {
      try {
        return obj.getClass().getMethod(name).invoke(obj) instanceof Number n ? n.intValue() : 0;
      } catch (NoSuchMethodException ex) {
        throw e;
      }
    }
  }

  private static Class<?> tryLoad(String... names) {
    for (String n : names) {
      try {
        return Class.forName(n);
      } catch (ClassNotFoundException ignored) {
      }
    }
    return null;
  }

  private static List<Vec3> straightLine(Vec3 from, Vec3 to, int maxSteps) {
    List<Vec3> list = new ArrayList<>();
    Vec3 delta = to.subtract(from);
    double dist = Math.sqrt(delta.lengthSqr());
    int steps = (int) Math.min(maxSteps, Math.max(1, Math.ceil(dist)));
    for (int i = 1; i <= steps; i++) {
      double f = (double) i / (double) steps;
      list.add(new Vec3(from.x + delta.x * f, from.y + delta.y * f, from.z + delta.z * f));
    }
    return list;
  }
}
