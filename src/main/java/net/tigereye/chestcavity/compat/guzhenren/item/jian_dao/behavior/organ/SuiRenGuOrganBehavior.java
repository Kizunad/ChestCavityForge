package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.active.SuiRenGuActive;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;

/**
 * 碎刃蛊（五转·剑道爆发器官）行为实现。
 *
 * <p>主动技能「碎刃祭痕」：
 * <ul>
 *   <li>牺牲所有在场的自有飞剑（可回收、耐久>0）</li>
 *   <li>临时获得剑道道痕增幅，基于飞剑经验与属性计算</li>
 *   <li>增幅持续一定时间后自动回滚</li>
 *   <li>消耗真元/精力/念头，有冷却时间</li>
 * </ul>
 *
 * <p>器官副作用：
 * <ul>
 *   <li>速度 -5</li>
 *   <li>力量 -20</li>
 *   <li>最大生命 -20</li>
 *   <li>剑道道痕 +100（基础）</li>
 * </ul>
 *
 * <p>本器官无被动效果，仅提供主动技能与基础属性。
 */
public enum SuiRenGuOrganBehavior {
  INSTANCE;

  public static final String MOD_ID = "guzhenren";
  public static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "sui_ren_gu");

  /** 主动技能 ID（用于注册与客户端热键绑定）。*/
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "sui_ren_gu");

  static {
    // 注册主动技能激活入口
    OrganActivationListeners.register(ABILITY_ID, SuiRenGuOrganBehavior::activateAbility);
  }

  /**
   * 主动技能激活入口（由 OrganActivationListeners 调用）。
   *
   * @param entity 激活者
   * @param cc 胸腔实例
   */
  private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    // 仅服务端 ServerPlayer 可激活
    if (!(entity instanceof ServerPlayer player) || entity.level().isClientSide()) {
      return;
    }

    // 查找碎刃蛊器官
    ItemStack organ = findMatchingOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }

    // 构建状态与冷却管理器
    OrganState state = OrganState.of(organ, SuiRenGuState.ROOT);
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();

    long now = player.serverLevel().getGameTime();

    // 调用主动技能实现
    SuiRenGuActive.activate(player, cc, organ, state, cooldown, now);
  }

  /**
   * 在玩家胸腔中查找碎刃蛊器官。
   *
   * @param cc 胸腔实例
   * @return 碎刃蛊物品栈，若未找到返回 EMPTY
   */
  private static ItemStack findMatchingOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }

    for (int i = 0, size = cc.inventory.getContainerSize(); i < size; i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }

      ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (itemId != null && itemId.equals(ORGAN_ID)) {
        return stack;
      }
    }

    return ItemStack.EMPTY;
  }
}
