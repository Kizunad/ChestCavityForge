package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.events;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.domain.DomainTags;
import net.tigereye.chestcavity.compat.guzhenren.util.GuzhenrenFlowTooltipResolver;

/**
 * 冻结期“剑道”物品使用拦截。
 *
 * <p>当玩家处于 {@link DomainTags#TAG_JIANGDAO_FREEZE_TICKS} 冻结状态时，禁止使用任何具有
 * guzhenren:jiandao 顶层标签的物品（或按流派关键字回退命中的“剑道”物品）。
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class JiandaoFreezeUseItemEvents {

  private JiandaoFreezeUseItemEvents() {}

  /** 右键对物品触发（主流入口）。*/
  @net.neoforged.bus.api.SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
  public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
    if (event.isCanceled()) return;
    Player player = event.getEntity();
    ItemStack stack = event.getItemStack();
    if (player == null || stack == null || stack.isEmpty()) return;
    if (!DomainTags.isJiandaoFrozen(player)) return;
    if (!GuzhenrenFlowTooltipResolver.isJiandaoItem(stack)) return;
    event.setCancellationResult(InteractionResult.FAIL);
    event.setCanceled(true);
  }

  /** 右键空气触发（例如空挥但物品可用时）。*/
  @net.neoforged.bus.api.SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
  public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
    Player player = event.getEntity();
    if (player == null) return;
    InteractionHand hand = event.getHand();
    ItemStack stack = player.getItemInHand(hand);
    if (stack == null || stack.isEmpty()) return;
    if (!DomainTags.isJiandaoFrozen(player)) return;
    if (!GuzhenrenFlowTooltipResolver.isJiandaoItem(stack)) return;
    // RightClickEmpty 不可取消；此处不做处理，依赖 RightClickItem/UseStart 桥接拦截。
  }

  /** 使用开始（部分场景作为兜底，如来自其他触发路径的使用）。*/
  @net.neoforged.bus.api.SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
  public static void onUseStart(LivingEntityUseItemEvent.Start event) {
    if (!(event.getEntity() instanceof Player player)) return;
    ItemStack stack = event.getItem();
    if (stack == null || stack.isEmpty()) return;
    if (!DomainTags.isJiandaoFrozen(player)) return;
    if (!GuzhenrenFlowTooltipResolver.isJiandaoItem(stack)) return;
    event.setCanceled(true);
  }
}
