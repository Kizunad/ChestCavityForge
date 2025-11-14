package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;

/**
 * 多重剑影蛊器官行为
 *
 * <p>实现分身召唤/召回功能，通过器官系统触发。
 *
 * <p><strong>架构迁移说明 (2025-11-14):</strong>
 * <ul>
 *   <li>原方式: DuochongjianyingGuItem (物品模式 - 右键物品触发)
 *   <li>新方式: 器官模式 - 将蛊虫放入胸腔后通过主动技能触发
 *   <li>物品ID: guzhenren:duochongjianying (外部引用)
 *   <li>状态存储: 使用 OrganState 替代物品NBT
 * </ul>
 *
 * <p><strong>功能:</strong>
 * <ul>
 *   <li>主动技能（普通触发）: 召唤/召回分身
 *   <li>主动技能（Shift触发）: 打开分身界面
 *   <li>被动监听: 确保分身状态同步
 * </ul>
 *
 * <p><strong>TODO (待实现):</strong>
 * <ul>
 *   <li>[ ] 从 DuochongjianyingGuItem 迁移召唤/召回逻辑
 *   <li>[ ] 实现 OrganState 状态管理（所有者UUID、分身UUID、分身数据）
 *   <li>[ ] 实现主动技能激活监听器
 *   <li>[ ] 集成分身实体系统 (PersistentGuCultivatorClone)
 *   <li>[ ] 集成分身UI系统 (CloneInventoryMenu)
 *   <li>[ ] 跨维度支持和资源清理
 * </ul>
 *
 * @see net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.PersistentGuCultivatorClone
 * @see net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.ui.CloneInventoryMenu
 */
public enum DuochongjianyingGuOrganBehavior {
  INSTANCE;

  private static final String MOD_ID = "guzhenren";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "duochongjianying");

  /**
   * 主动技能ID: 多重剑影蛊分身
   */
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "duochongjianying_fenshen");

  private static final String STATE_ROOT = "DuochongjianyingGu";
  private static final String OWNER_UUID_KEY = "OwnerUUID";
  private static final String CLONE_UUID_KEY = "CloneUUID";
  private static final String CLONE_DATA_KEY = "CloneData";

  static {
    // 注册主动技能激活监听器
    OrganActivationListeners.register(ABILITY_ID, DuochongjianyingGuOrganBehavior::activateAbility);
  }

  /**
   * 主动技能激活回调
   *
   * <p>触发条件:
   * <ul>
   *   <li>玩家拥有多重剑影蛊器官
   *   <li>玩家手动触发主动技能（按键绑定或其他机制）
   * </ul>
   *
   * <p>行为:
   * <ul>
   *   <li>Shift + 触发: 打开分身界面
   *   <li>普通触发: 召唤/召回分身
   * </ul>
   *
   * <p><strong>TODO:</strong>
   * <ul>
   *   <li>[ ] 实现所有权校验
   *   <li>[ ] 实现召唤逻辑（从OrganState读取分身数据）
   *   <li>[ ] 实现召回逻辑（保存分身数据到OrganState）
   *   <li>[ ] 实现打开界面逻辑（检查分身存在且距离）
   *   <li>[ ] 跨维度分身查找和清理
   * </ul>
   */
  private static void activateAbility(net.minecraft.world.entity.LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || cc == null || entity.level().isClientSide()) {
      return;
    }

    // TODO: 查找器官ItemStack
    // TODO: 检查所有权
    // TODO: 检查玩家是否按下Shift键
    //   - 是: 打开分身界面
    //   - 否: 召唤/召回分身

    // 占位实现
    player.sendSystemMessage(
        net.minecraft.network.chat.Component.literal("§e[TODO] 多重剑影蛊主动技能 - 待实现")
    );
  }

  /**
   * 确保器官附加时的初始化
   *
   * <p>当器官首次放入胸腔时调用。
   *
   * <p><strong>TODO:</strong>
   * <ul>
   *   <li>[ ] 初始化所有者UUID
   *   <li>[ ] 清理旧的分身数据（如果存在）
   * </ul>
   */
  public void ensureAttached(ChestCavityInstance cc) {
    // TODO: 初始化逻辑
  }

  // ============ 私有辅助方法 (待迁移自 DuochongjianyingGuItem) ============

  /**
   * TODO: 从 DuochongjianyingGuItem.summonClone() 迁移
   */
  private static void summonClone(ServerPlayer player, ChestCavityInstance cc, ItemStack organ) {
    // 待实现
  }

  /**
   * TODO: 从 DuochongjianyingGuItem.recallClone() 迁移
   */
  private static void recallClone(ServerPlayer player, ChestCavityInstance cc, ItemStack organ) {
    // 待实现
  }

  /**
   * TODO: 从 DuochongjianyingGuItem.findClone() 迁移
   */
  private static net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.PersistentGuCultivatorClone
      findClone(ServerPlayer player, ChestCavityInstance cc, ItemStack organ) {
    // 待实现
    return null;
  }

  /**
   * TODO: 从 DuochongjianyingGuItem.openInventory() 迁移
   */
  private static void openCloneInventory(ServerPlayer player, ChestCavityInstance cc, ItemStack organ) {
    // 待实现
  }
}
