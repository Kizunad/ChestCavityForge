package net.tigereye.chestcavity.registration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.GuzhenrenFlowTooltipResolver;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * Guzhenren 主动技能触发后处理器，负责在技能执行成功后采集胸腔流派并通知监听器。
 */
public final class GuzhenrenFlowActivationHooks {

  private static final CopyOnWriteArrayList<ActivationFlowListener> LISTENERS =
      new CopyOnWriteArrayList<>();

  private GuzhenrenFlowActivationHooks() {}

  /**
   * 注册监听器以接收技能触发后的流派快照。
   *
   * @param listener 回调实例，为 null 时忽略
   */
  public static void registerListener(ActivationFlowListener listener) {
    if (listener == null) {
      return;
    }
    LISTENERS.addIfAbsent(listener);
  }

  /**
   * NeoForge SkillActivationHooks 回调：在技能执行成功后收集流派信息并派发给所有监听器。
   *
   * @param player 触发技能的玩家
   * @param skillId 技能标识
   * @param cc 玩家胸腔实例
   * @param entry 技能注册条目，可为空
   * @param result 触发结果，非 SUCCESS 时直接返回
   */
  public static void handleSkillPostActivation(
      @Nullable ServerPlayer player,
      ResourceLocation skillId,
      @Nullable ChestCavityInstance cc,
      @Nullable ActiveSkillRegistry.ActiveSkillEntry entry,
      ActiveSkillRegistry.TriggerResult result) {
    if (result != ActiveSkillRegistry.TriggerResult.SUCCESS) {
      return;
    }
    if (LISTENERS.isEmpty()) {
      return;
    }
    if (player == null || cc == null || cc.inventory == null) {
      return;
    }

    ImmutableList<GuzhenrenFlowTooltipResolver.FlowInfo> itemFlows = collectInventoryFlows(cc);
    ImmutableSet<String> distinctFlows = collectDistinctFlows(itemFlows);
    ActivationFlowSnapshot snapshot =
        new ActivationFlowSnapshot(player, skillId, cc, entry, itemFlows, distinctFlows);

    for (ActivationFlowListener listener : LISTENERS) {
      try {
        listener.onPostActivation(snapshot);
      } catch (Throwable t) {
        ChestCavity.LOGGER.warn(
            "[compat/guzhenren][flow] post handler failed for {}", skillId, t);
      }
    }
  }

  /**
   * 收集胸腔库存中所有具有流派标签的物品。
   *
   * @param cc 玩家胸腔实例
   * @return 具备流派信息的物品快照列表
   */
  private static ImmutableList<GuzhenrenFlowTooltipResolver.FlowInfo> collectInventoryFlows(
      ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ImmutableList.of();
    }
    List<GuzhenrenFlowTooltipResolver.FlowInfo> flows = new ArrayList<>();
    for (int i = 0, size = cc.inventory.getContainerSize(); i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }
      GuzhenrenFlowTooltipResolver.FlowInfo info = GuzhenrenFlowTooltipResolver.inspect(stack);
      if (info.hasFlow()) {
        flows.add(info);
      }
    }
    return ImmutableList.copyOf(flows);
  }

  /**
   * 汇总物品列表中的唯一流派字符串，供监听器快速判断是否包含指定流派。
   *
   * @param itemFlows 胸腔物品流派列表
   * @return 去重后的流派集合
   */
  private static ImmutableSet<String> collectDistinctFlows(
      List<GuzhenrenFlowTooltipResolver.FlowInfo> itemFlows) {
    if (itemFlows == null || itemFlows.isEmpty()) {
      return ImmutableSet.of();
    }
    LinkedHashSet<String> distinct = new LinkedHashSet<>();
    for (GuzhenrenFlowTooltipResolver.FlowInfo info : itemFlows) {
      distinct.addAll(info.flows());
    }
    return ImmutableSet.copyOf(distinct);
  }

  /**
   * 技能触发后传递给监听器的快照数据。
   *
   * @param player 触发技能的玩家
   * @param skillId 技能标识
   * @param chestCavity 胸腔实例
   * @param entry 技能注册条目，可为空
   * @param itemFlows 具备流派信息的物品列表
   * @param distinctFlows 去重后的流派集合
   */
  public record ActivationFlowSnapshot(
      ServerPlayer player,
      ResourceLocation skillId,
      ChestCavityInstance chestCavity,
      @Nullable ActiveSkillRegistry.ActiveSkillEntry entry,
      ImmutableList<GuzhenrenFlowTooltipResolver.FlowInfo> itemFlows,
      ImmutableSet<String> distinctFlows) {}

  /** 监听器接口，在技能执行后被调用以消费流派快照。 */
  @FunctionalInterface
  public interface ActivationFlowListener {
    void onPostActivation(ActivationFlowSnapshot snapshot);
  }
}
