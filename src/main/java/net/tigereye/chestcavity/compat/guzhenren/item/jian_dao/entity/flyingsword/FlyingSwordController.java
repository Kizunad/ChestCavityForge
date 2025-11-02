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
   * <p>注意：RECALL 模式不在循环中，因为它是通过召回功能专门触发的。
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
      case HUNT -> AIMode.HOVER;
      case HOVER -> AIMode.ORBIT;
      case RECALL -> AIMode.ORBIT; // RECALL 被打断时回到 ORBIT
      case SWARM -> AIMode.SWARM; // SWARM 模式不可切换（青莲蛊专用集群模式）
    };

    sword.setAIMode(next);
    return next;
  }

  /**
   * 召回单个飞剑
   *
   * <p>首次调用时，将飞剑设置为召回模式，开始弧形返回动画。
   * 飞剑到达主人后，RecallBehavior 会再次调用此方法完成实际召回。
   *
   * @param sword 飞剑实体
   */
  public static void recall(FlyingSwordEntity sword) {
    if (sword == null || sword.isRemoved()) {
      return;
    }

    // 检查飞剑是否可被召回（主动技能生成的飞剑不可召回）
    if (!sword.isRecallable()) {
      return;
    }

    Player owner = sword.getOwner();
    if (owner == null || sword.level().isClientSide) {
      sword.discard();
      return;
    }

    // 如果尚未进入召回模式，先设置为召回模式开始动画
    if (sword.getAIMode() != AIMode.RECALL) {
      sword.setAIMode(AIMode.RECALL);
      // 播放召回特效（起始特效）
      if (sword.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
        net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.fx.FlyingSwordFX
            .spawnRecallEffect(serverLevel, sword);
      }
      // 音效：召回
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ops.SoundOps
          .playRecall(sword);
      return;
    }

    // 已在召回模式，表示已到达主人，执行实际召回逻辑

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

    // 触发onDespawnOrRecall事件钩子（在discard之前）
    if (sword.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
      // 准备目标ItemStack（如果要回写NBT）
      net.minecraft.world.item.ItemStack targetStack = null;
      if (success) {
        // 召回成功，准备一个空ItemStack作为占位（实际数据已保存到Storage）
        // 钩子可以在这里添加额外NBT到customData
        targetStack =
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
      }

      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events.context
          .DespawnContext despawnCtx =
          new net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events
              .context.DespawnContext(
              sword,
              serverLevel,
              owner,
              net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events
                  .context.DespawnContext.Reason.RECALLED,
              targetStack);
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events
          .FlyingSwordEventRegistry.fireDespawnOrRecall(despawnCtx);

      // 检查是否被钩子阻止消散
      if (despawnCtx.preventDespawn) {
        return;
      }
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

  /** 召回在场飞剑（按列表索引，1-based）。返回是否成功。 */
  public static boolean recallByIndex(ServerLevel level, Player owner, int index1) {
    List<FlyingSwordEntity> swords = getPlayerSwords(level, owner);
    if (index1 < 1 || index1 > swords.size()) {
      return false;
    }
    recall(swords.get(index1 - 1));
    return true;
  }

  /** 设置在场第 index(1-based) 把飞剑的模式。返回是否成功。 */
  public static boolean setModeByIndex(ServerLevel level, Player owner, int index1,
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.ai.AIMode mode) {
    List<FlyingSwordEntity> swords = getPlayerSwords(level, owner);
    if (index1 < 1 || index1 > swords.size()) {
      return false;
    }
    FlyingSwordEntity sword = swords.get(index1 - 1);
    sword.setAIMode(mode);
    return true;
  }

  // ========== 指定飞剑（Selection） ==========
  /** 设置玩家当前“已指定”的飞剑。 */
  public static boolean setSelectedSword(Player owner, FlyingSwordEntity sword) {
    if (owner == null || sword == null || sword.isRemoved()) return false;
    if (!sword.isOwnedBy(owner)) return false;
    var sel = net.tigereye.chestcavity.registration.CCAttachments.getFlyingSwordSelection(owner);
    sel.setSelectedSword(sword.getUUID());
    return true;
  }

  /** 清除玩家“已指定”的飞剑。 */
  public static void clearSelectedSword(Player owner) {
    if (owner == null) return;
    net.tigereye.chestcavity.registration.CCAttachments
        .getFlyingSwordSelection(owner)
        .clear();
  }

  /** 获取玩家“已指定”的飞剑（若仍存活）。 */
  @Nullable
  public static FlyingSwordEntity getSelectedSword(ServerLevel level, Player owner) {
    if (owner == null || level == null) return null;
    var sel = net.tigereye.chestcavity.registration.CCAttachments.getFlyingSwordSelection(owner);
    var selected = sel.getSelectedSword();
    if (selected.isEmpty()) return null;
    var entity = level.getEntity(selected.get());
    return (entity instanceof FlyingSwordEntity fs && fs.isOwnedBy(owner)) ? fs : null;
  }
}
