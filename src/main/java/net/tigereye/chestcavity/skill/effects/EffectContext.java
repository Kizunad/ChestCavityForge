package net.tigereye.chestcavity.skill.effects;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.util.GuzhenrenFlowTooltipResolver;

/** 提供给 Effect 的只读上下文。 */
public final class EffectContext {

  private final ServerPlayer player;
  private final ResourceLocation skillId;
  private final ChestCavityInstance cc;
  private final long serverTime;
  private final long castId;
  private final ImmutableList<GuzhenrenFlowTooltipResolver.FlowInfo> itemFlows;
  private final ImmutableSet<String> distinctFlows;
  private final UseItemInfo useItem;

  private EffectContext(
      ServerPlayer player,
      ResourceLocation skillId,
      ChestCavityInstance cc,
      long serverTime,
      long castId,
      ImmutableList<GuzhenrenFlowTooltipResolver.FlowInfo> itemFlows,
      ImmutableSet<String> distinctFlows,
      UseItemInfo useItem) {
    this.player = Objects.requireNonNull(player, "player");
    this.skillId = Objects.requireNonNull(skillId, "skillId");
    this.cc = Objects.requireNonNull(cc, "cc");
    this.serverTime = serverTime;
    this.castId = castId;
    this.itemFlows = itemFlows;
    this.distinctFlows = distinctFlows;
    this.useItem = useItem;
  }

  public ServerPlayer player() {
    return player;
  }

  public ResourceLocation skillId() {
    return skillId;
  }

  public ChestCavityInstance chestCavity() {
    return cc;
  }

  public long serverTime() {
    return serverTime;
  }

  public long castId() {
    return castId;
  }

  public ImmutableList<GuzhenrenFlowTooltipResolver.FlowInfo> itemFlows() {
    return itemFlows;
  }

  public ImmutableSet<String> distinctFlows() {
    return distinctFlows;
  }

  /** 若本次触发来自物品使用，返回其使用上下文；否则为 null。 */
  public UseItemInfo useItem() {
    return useItem;
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

  /** 构造上下文并收集一次胸腔物品的流派标签（供整个效果链复用）。 */
  public static EffectContext build(
      ServerPlayer player, ResourceLocation skillId, ChestCavityInstance cc, long castId) {
    long now = player.server.getTickCount();
    ImmutableList<GuzhenrenFlowTooltipResolver.FlowInfo> flows = collectInventoryFlows(cc);
    ImmutableSet<String> distinct = collectDistinctFlows(flows);
    return new EffectContext(player, skillId, cc, now, castId, flows, distinct, null);
  }

  /**
   * 构造包含物品使用上下文的 EffectContext。
   */
  public static EffectContext build(
      ServerPlayer player,
      ResourceLocation skillId,
      ChestCavityInstance cc,
      long castId,
      UseItemInfo useItem) {
    long now = player.server.getTickCount();
    ImmutableList<GuzhenrenFlowTooltipResolver.FlowInfo> flows = collectInventoryFlows(cc);
    ImmutableSet<String> distinct = collectDistinctFlows(flows);
    return new EffectContext(player, skillId, cc, now, castId, flows, distinct, useItem);
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
      if (info.hasFlow()) {
        flows.add(info);
      }
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

  /** 物品使用的上下文信息（仅在 Use-Item Hooks 场景下存在）。 */
  public static final class UseItemInfo {
    public enum Phase {
      START,
      FINISH,
      ABORT
    }

    private final ItemStack snapshot;
    private final ResourceLocation itemId;
    private final GuzhenrenFlowTooltipResolver.FlowInfo usedItemFlows;
    private final Phase phase;
    private final int durationTicks; // START 阶段可用，其余为 0

    private UseItemInfo(
        ItemStack snapshot,
        ResourceLocation itemId,
        GuzhenrenFlowTooltipResolver.FlowInfo usedItemFlows,
        Phase phase,
        int durationTicks) {
      this.snapshot = snapshot;
      this.itemId = itemId;
      this.usedItemFlows = usedItemFlows;
      this.phase = phase;
      this.durationTicks = durationTicks;
    }

    public ItemStack snapshot() { return snapshot; }

    public ResourceLocation itemId() { return itemId; }

    public GuzhenrenFlowTooltipResolver.FlowInfo usedItemFlows() { return usedItemFlows; }

    public Phase phase() { return phase; }

    public int durationTicks() { return durationTicks; }

    public static UseItemInfo ofStart(ItemStack stack, int durationTicks) {
      ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
      return new UseItemInfo(
          stack.copy(),
          id,
          GuzhenrenFlowTooltipResolver.inspect(stack),
          Phase.START,
          Math.max(0, durationTicks));
    }

    public static UseItemInfo ofFinish(ItemStack stack) {
      ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
      return new UseItemInfo(
          stack.copy(), id, GuzhenrenFlowTooltipResolver.inspect(stack), Phase.FINISH, 0);
    }

    public static UseItemInfo ofAbort(ItemStack stack) {
      ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
      return new UseItemInfo(
          stack.copy(), id, GuzhenrenFlowTooltipResolver.inspect(stack), Phase.ABORT, 0);
    }
  }
}
