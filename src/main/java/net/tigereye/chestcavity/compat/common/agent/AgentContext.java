package net.tigereye.chestcavity.compat.common.agent;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.GuzhenrenFlowTooltipResolver;

/** 非技能专用、面向 LivingEntity 的通用上下文： - 提供 serverTime、ChestCavity、以及一次性收集的库存“流派标签”摘要 - 玩家与非玩家 NPC 共用 */
public final class AgentContext {

  private final LivingEntity living;
  private final ChestCavityInstance cc;
  private final long serverTime;
  private final ImmutableList<GuzhenrenFlowTooltipResolver.FlowInfo> itemFlows;
  private final ImmutableSet<String> distinctFlows;

  private AgentContext(
      LivingEntity living,
      ChestCavityInstance cc,
      long serverTime,
      ImmutableList<GuzhenrenFlowTooltipResolver.FlowInfo> itemFlows,
      ImmutableSet<String> distinctFlows) {
    this.living = Objects.requireNonNull(living, "living");
    this.cc = cc;
    this.serverTime = serverTime;
    this.itemFlows = itemFlows == null ? ImmutableList.of() : itemFlows;
    this.distinctFlows = distinctFlows == null ? ImmutableSet.of() : distinctFlows;
  }

  public LivingEntity living() {
    return living;
  }

  public ChestCavityInstance chestCavity() {
    return cc;
  }

  public long serverTime() {
    return serverTime;
  }

  public ImmutableList<GuzhenrenFlowTooltipResolver.FlowInfo> itemFlows() {
    return itemFlows;
  }

  public ImmutableSet<String> distinctFlows() {
    return distinctFlows;
  }

  public boolean hasFlow(String keyword) {
    if (keyword == null || keyword.isBlank()) return false;
    String lowered = keyword.toLowerCase(java.util.Locale.ROOT);
    for (String f : distinctFlows) {
      String lf = f.toLowerCase(java.util.Locale.ROOT);
      if (lf.contains(lowered) || lf.replace("道", "").contains(lowered.replace("道", ""))) {
        return true;
      }
    }
    return false;
  }

  public static AgentContext of(LivingEntity living) {
    ChestCavityInstance cc = Agents.chestCavity(living);
    long now = Agents.serverTime(living);
    ImmutableList<GuzhenrenFlowTooltipResolver.FlowInfo> flows = collectInventoryFlows(cc);
    ImmutableSet<String> distinct = collectDistinctFlows(flows);
    return new AgentContext(living, cc, now, flows, distinct);
  }

  private static ImmutableList<GuzhenrenFlowTooltipResolver.FlowInfo> collectInventoryFlows(
      ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ImmutableList.of();
    }
    List<GuzhenrenFlowTooltipResolver.FlowInfo> flows = new java.util.ArrayList<>();
    for (int i = 0, size = cc.inventory.getContainerSize(); i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) continue;
      GuzhenrenFlowTooltipResolver.FlowInfo info = GuzhenrenFlowTooltipResolver.inspect(stack);
      if (info.hasFlow()) flows.add(info);
    }
    return flows.isEmpty() ? ImmutableList.of() : ImmutableList.copyOf(flows);
  }

  private static ImmutableSet<String> collectDistinctFlows(
      List<GuzhenrenFlowTooltipResolver.FlowInfo> itemFlows) {
    if (itemFlows == null || itemFlows.isEmpty()) {
      return ImmutableSet.of();
    }
    Set<String> out = new LinkedHashSet<>();
    for (GuzhenrenFlowTooltipResolver.FlowInfo info : itemFlows) {
      out.addAll(info.flows());
    }
    return out.isEmpty() ? ImmutableSet.of() : ImmutableSet.copyOf(out);
  }
}
