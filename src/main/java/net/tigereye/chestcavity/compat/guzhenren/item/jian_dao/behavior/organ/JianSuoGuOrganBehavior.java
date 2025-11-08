package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.active.JianSuoGuActive;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.passive.JianSuoGuEvadePassive;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;

/**
 * 剑梭蛊（3-5转·剑道突进+躲避器官）行为实现。
 *
 * <p>主动技能「剑梭突进」：
 * <ul>
 *   <li>向前突进，沿路径造成伤害</li>
 *   <li>距离与伤害基于剑道道痕放大</li>
 *   <li>友方过滤，命中去重</li>
 *   <li>消耗真元/精力/念头，有冷却时间</li>
 * </ul>
 *
 * <p>被动效果「剑影身法」：
 * <ul>
 *   <li>受击时有几率触发反向后退</li>
 *   <li>减免本次伤害</li>
 *   <li>获得短暂无敌帧</li>
 *   <li>全局冷却</li>
 * </ul>
 *
 * <p>飞剑增强：
 * <ul>
 *   <li>每隔一定时间沿朝向自动短突进</li>
 *   <li>不消耗资源，独立冷却</li>
 * </ul>
 *
 * <p>器官属性（根据转数不同）：
 * <ul>
 *   <li>三转：速度 -2，力量 -5，剑道道痕 +50</li>
 *   <li>四转：速度 -3，力量 -8，剑道道痕 +80</li>
 *   <li>五转：速度 -4，力量 -10，剑道道痕 +120</li>
 * </ul>
 */
public enum JianSuoGuOrganBehavior implements OrganIncomingDamageListener {
  INSTANCE;

  public static final String MOD_ID = "guzhenren";
  public static final ResourceLocation ORGAN_ID_3 =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jiansuogu");
  public static final ResourceLocation ORGAN_ID_4 =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jiansuogusizhuan");
  public static final ResourceLocation ORGAN_ID_5 =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jiansuoguwuzhuan");

  /** 主动技能 ID（用于注册与客户端热键绑定）。*/
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jian_suo_gu_dash");

  static {
    // 注册主动技能激活入口
    OrganActivationListeners.register(ABILITY_ID, JianSuoGuOrganBehavior::activateAbility);
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

    // 查找剑梭蛊器官
    ItemStack organ = findMatchingOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }

    // 构建状态与冷却管理器
    OrganState state = OrganState.of(organ, JianSuoGuState.ROOT);
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();

    long now = player.serverLevel().getGameTime();

    // 调用主动技能实现
    JianSuoGuActive.activate(player, cc, organ, state, cooldown, now);
  }

  /**
   * 在玩家胸腔中查找剑梭蛊器官（支持 3/4/5 转）。
   *
   * @param cc 胸腔实例
   * @return 剑梭蛊物品栈，若未找到返回 EMPTY
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
      if (itemId != null
          && (itemId.equals(ORGAN_ID_3) || itemId.equals(ORGAN_ID_4) || itemId.equals(ORGAN_ID_5))) {
        return stack;
      }
    }

    return ItemStack.EMPTY;
  }

  /**
   * 被动效果：受击躲避（实现 {@link OrganIncomingDamageListener}）。
   *
   * @param source 伤害源
   * @param victim 受击者
   * @param cc 胸腔实例
   * @param organ 剑梭蛊器官物品
   * @param damage 原始伤害
   * @return 修改后的伤害
   */
  @Override
  public float onIncomingDamage(
      DamageSource source,
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
    return JianSuoGuEvadePassive.onIncomingDamage(source, victim, cc, organ, damage);
  }
}
