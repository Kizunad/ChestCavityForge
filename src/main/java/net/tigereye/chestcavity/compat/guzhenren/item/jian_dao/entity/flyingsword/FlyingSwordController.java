package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.AIMode;

/**
 * 飞剑控制接口（Flying Sword Controller）
 *
 * <p>提供对玩家飞剑的控制功能：
 * <ul>
 *   <li>查询玩家的飞剑列表</li>
 *   <li>切换AI模式</li>
 *   <li>召回所有飞剑</li>
 *   <li>获取飞剑状态信息</li>
 * </ul>
 */
public final class FlyingSwordController {

  private FlyingSwordController() {}

  /**
   * 获取玩家的所有飞剑
   *
   * @param level 服务端世界
   * @param owner 主人
   * @return 飞剑列表
   */
  public static List<FlyingSwordEntity> getPlayerSwords(ServerLevel level, Player owner) {
    if (owner == null) {
      return List.of();
    }

    UUID ownerId = owner.getUUID();
    List<FlyingSwordEntity> swords = new ArrayList<>();

    // 搜索范围：以玩家为中心的128格半径
    AABB searchBox = owner.getBoundingBox().inflate(128.0);

    for (Entity entity : level.getEntities(null, searchBox)) {
      if (entity instanceof FlyingSwordEntity sword) {
        if (sword.isOwnedBy(owner)) {
          swords.add(sword);
        }
      }
    }

    return swords;
  }

  /**
   * 切换飞剑的AI模式
   *
   * @param sword 飞剑实体
   * @param mode 目标模式
   */
  public static void setAIMode(FlyingSwordEntity sword, AIMode mode) {
    if (sword == null || mode == null) {
      return;
    }
    sword.setAIMode(mode);
  }

  /**
   * 切换所有飞剑的AI模式
   *
   * @param level 服务端世界
   * @param owner 主人
   * @param mode 目标模式
   * @return 切换的飞剑数量
   */
  public static int setAllAIMode(ServerLevel level, Player owner, AIMode mode) {
    List<FlyingSwordEntity> swords = getPlayerSwords(level, owner);
    for (FlyingSwordEntity sword : swords) {
      sword.setAIMode(mode);
    }
    return swords.size();
  }

  /**
   * 循环切换AI模式
   *
   * @param sword 飞剑实体
   * @return 新的AI模式
   */
  public static AIMode cycleAIMode(FlyingSwordEntity sword) {
    if (sword == null) {
      return AIMode.ORBIT;
    }

    AIMode current = sword.getAIMode();
    AIMode next = switch (current) {
      case ORBIT -> AIMode.GUARD;
      case GUARD -> AIMode.HUNT;
      case HUNT -> AIMode.ORBIT;
    };

    sword.setAIMode(next);
    return next;
  }

  /**
   * 召回单个飞剑
   *
   * @param sword 飞剑实体
   */
  public static void recall(FlyingSwordEntity sword) {
    if (sword == null || sword.isRemoved()) {
      return;
    }

    Player owner = sword.getOwner();
    if (owner == null || sword.level().isClientSide) {
      sword.discard();
      return;
    }

    // 保存飞剑状态到玩家数据
    boolean success =
        net.tigereye.chestcavity.registration.CCAttachments.getFlyingSwordStorage(owner)
            .recallSword(sword);

    if (success) {
      // 发送成功消息
      owner.sendSystemMessage(
          net.minecraft.network.chat.Component.literal(
              String.format(
                  "[飞剑] 召回成功 - 等级%d (经验: %d, 耐久: %.1f/%.1f)",
                  sword.getSwordLevel(),
                  sword.getExperience(),
                  sword.getDurability(),
                  sword.getSwordAttributes().maxDurability)));
    } else {
      // 存储已满
      owner.sendSystemMessage(
          net.minecraft.network.chat.Component.literal("[飞剑] 召回失败 - 存储已满 (最多10个)"));
    }

    // 召回特效
    if (sword.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.fx.FlyingSwordFX
          .spawnRecallEffect(serverLevel, sword);
    }

    sword.discard();
  }

  /**
   * 召回所有飞剑
   *
   * @param level 服务端世界
   * @param owner 主人
   * @return 召回的飞剑数量
   */
  public static int recallAll(ServerLevel level, Player owner) {
    List<FlyingSwordEntity> swords = getPlayerSwords(level, owner);
    for (FlyingSwordEntity sword : swords) {
      recall(sword);
    }
    return swords.size();
  }

  /**
   * 获取最近的飞剑
   *
   * @param level 服务端世界
   * @param owner 主人
   * @return 最近的飞剑，如果没有则返回null
   */
  @Nullable
  public static FlyingSwordEntity getNearestSword(ServerLevel level, Player owner) {
    List<FlyingSwordEntity> swords = getPlayerSwords(level, owner);
    if (swords.isEmpty()) {
      return null;
    }

    FlyingSwordEntity nearest = null;
    double minDistSq = Double.MAX_VALUE;

    for (FlyingSwordEntity sword : swords) {
      double distSq = sword.distanceToSqr(owner);
      if (distSq < minDistSq) {
        minDistSq = distSq;
        nearest = sword;
      }
    }

    return nearest;
  }

  /**
   * 获取飞剑的状态信息
   *
   * @param sword 飞剑实体
   * @return 状态信息字符串
   */
  public static String getSwordStatus(FlyingSwordEntity sword) {
    if (sword == null) {
      return "无效飞剑";
    }

    StringBuilder status = new StringBuilder();
    status.append("飞剑 等级").append(sword.getSwordLevel());
    status.append(" [").append(sword.getAIMode().getDisplayName()).append("]");
    status.append("\n耐久: ")
        .append(String.format("%.1f", sword.getDurability()))
        .append("/")
        .append(String.format("%.1f", sword.getSwordAttributes().maxDurability));
    status.append("\n经验: ")
        .append(sword.getExperience())
        .append("/")
        .append(
            net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.calculator
                .FlyingSwordCalculator.calculateExpToNext(sword.getSwordLevel()));
    status.append("\n速度: ").append(String.format("%.2f", sword.getCurrentSpeed()));

    return status.toString();
  }

  /**
   * 获取玩家的飞剑总数
   *
   * @param level 服务端世界
   * @param owner 主人
   * @return 飞剑数量
   */
  public static int getSwordCount(ServerLevel level, Player owner) {
    return getPlayerSwords(level, owner).size();
  }

  /**
   * 检查玩家是否有飞剑
   *
   * @param level 服务端世界
   * @param owner 主人
   * @return 是否有飞剑
   */
  public static boolean hasSwords(ServerLevel level, Player owner) {
    return getSwordCount(level, owner) > 0;
  }

  /**
   * 设置飞剑目标
   *
   * @param sword 飞剑实体
   * @param target 目标实体（null表示清除目标）
   */
  public static void setTarget(
      FlyingSwordEntity sword, @Nullable net.minecraft.world.entity.LivingEntity target) {
    if (sword == null) {
      return;
    }
    sword.setTargetEntity(target);
  }

  /**
   * 清除所有飞剑的目标
   *
   * @param level 服务端世界
   * @param owner 主人
   * @return 清除目标的飞剑数量
   */
  public static int clearAllTargets(ServerLevel level, Player owner) {
    List<FlyingSwordEntity> swords = getPlayerSwords(level, owner);
    for (FlyingSwordEntity sword : swords) {
      sword.setTargetEntity(null);
    }
    return swords.size();
  }
}
