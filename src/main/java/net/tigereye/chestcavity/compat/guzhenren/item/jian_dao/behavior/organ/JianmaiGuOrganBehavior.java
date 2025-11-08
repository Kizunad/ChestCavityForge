package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

import java.util.List;
import java.util.OptionalDouble;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.events.JianmaiPlayerTickEvents;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx.JianmaiAudioEffects;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.fx.JianmaiVisualEffects;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.runtime.JianmaiAmpOps;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.runtime.JianmaiNBT;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianmaiTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.util.ZhenyuanBaseCosts;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;

/**
 * 剑脉蛊（三转·剑道效率型）器官行为。
 *
 * <p>实现：
 * <ul>
 *   <li>主动技能：剑脉涌流（消耗真元，发放临时倍率增幅券）</li>
 *   <li>被动效果：由 {@link JianmaiPlayerTickEvents} 处理</li>
 *   <li>卸载清理：清空所有 NBT 数据与属性修饰</li>
 * </ul>
 */
public enum JianmaiGuOrganBehavior implements OrganRemovalListener {

  INSTANCE;

  private static final String MOD_ID = "guzhenren";

  /** 器官物品 ID */
  public static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jianmaigu");

  /** 主动技能 ID */
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jianmai_overdrive");

  // ========== OrganState 字段键 ==========
  private static final String STATE_ROOT = "JianmaiGu";
  private static final String K_ACTIVE_READY_AT = "ActiveReadyAt"; // long - 技能就绪时间戳

  // ========== 主动技能注册 ==========
  static {
    OrganActivationListeners.register(ABILITY_ID, JianmaiGuOrganBehavior::activateAbility);
  }

  // ========== 主动技能：剑脉涌流 ==========

  /**
   * 主动技能激活入口。
   *
   * <p>规则：
   * <ul>
   *   <li>消耗真元：五转·一阶段·爆发档（24 单位）</li>
   *   <li>计算增幅：每把飞剑提供基础道痕 + 经验加成，再叠加额外消耗奖励</li>
   *   <li>发放增幅券：直接调整道痕值</li>
   *   <li>持续时间：ACTIVE_DURATION_TICKS</li>
   *   <li>冷却时间：ACTIVE_COOLDOWN_TICKS</li>
   * </ul>
   *
   * @param entity 实体
   * @param cc 胸腔实例
   */
  private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || entity.level().isClientSide()) {
      return;
    }

    ItemStack organ = findMatchingOrgan(cc);
    if (organ.isEmpty()) {
      return;
    }

    OrganState state = OrganState.of(organ, STATE_ROOT);
    long now = player.serverLevel().getGameTime();

    // 检查冷却
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    MultiCooldown.Entry ready = cooldown.entry(K_ACTIVE_READY_AT);
    if (!ready.isReady(now)) {
      long remaining = ready.remaining(now);
      player.displayClientMessage(
          net.minecraft.network.chat.Component.translatable(
              "guzhenren.ability.cooldown", remaining / 20.0),
          true);
      return;
    }

    // 消耗真元：五转·一阶段·爆发档
    OptionalDouble consumed =
        ResourceOps.tryConsumeTieredZhenyuan(player, 5, 1, ZhenyuanBaseCosts.Tier.BURST);
    if (consumed.isEmpty()) {
      player.displayClientMessage(
          net.minecraft.network.chat.Component.translatable(
              "guzhenren.ability.insufficient_resource"),
          true);
      return;
    }

    // 扫描周围飞剑：用于增幅和特效
    List<FlyingSwordEntity> swords = collectOwnedSwords(player);

    // 计算基础增幅：每把飞剑至少提供 ACTIVE_SWORD_BASE，道痕额外随经验线性增加
    double swordBonus = calculateSwordBonus(swords);

    // 计算资源消耗增幅：额外叠加在基础增幅上
    double costRaw = consumed.getAsDouble();
    double costScaled = Math.max(0.0, Math.min(costRaw * JianmaiTuning.ACTIVE_COST_K, JianmaiTuning.ACTIVE_SOFTCAP));
    double resourceBonus = costScaled * JianmaiTuning.ACTIVE_BONUS_K;

    double deltaAmount = swordBonus + resourceBonus;

    // 应用主动增幅（直接调整道痕值）
    JianmaiAmpOps.applyActiveBuff(player, deltaAmount, JianmaiTuning.ACTIVE_DURATION_TICKS, now);

    // ========== 主动特效 ==========

    // 播放激活音效
    JianmaiAudioEffects.playActivation(player, deltaAmount);

    // 渲染雷电链视觉效果
    if (!swords.isEmpty()) {
      JianmaiVisualEffects.renderActiveLightning(player, swords, deltaAmount, now);
    }

    // 启动冷却并安排就绪提示
    long readyAt = now + JianmaiTuning.ACTIVE_COOLDOWN_TICKS;
    ready.setReadyAt(readyAt);
    ActiveSkillRegistry.scheduleReadyToast(player, ABILITY_ID, readyAt, now);

    // 反馈消息
    player.displayClientMessage(
        net.minecraft.network.chat.Component.translatable(
            "guzhenren.ability.jianmai.activated",
            String.format("+%.0f", deltaAmount),
            JianmaiTuning.ACTIVE_DURATION_TICKS / 20.0),
        true);
  }

  /**
   * 查找匹配的器官。
   *
   * @param cc 胸腔实例
   * @return 器官物品（如果没有则返回空）
   */
  private static ItemStack findMatchingOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return ItemStack.EMPTY;
    }

    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }

      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (id == null) {
        continue;
      }

      if (id.equals(ORGAN_ID)) {
        return stack;
      }
    }

    return ItemStack.EMPTY;
  }

  /**
   * 判断飞剑是否属于玩家。
   *
   * @param sword 飞剑
   * @param player 玩家
   * @return 是否属于玩家
   */
  private static boolean isOwnedBy(FlyingSwordEntity sword, ServerPlayer player) {
    return sword.getOwner() != null && sword.getOwner().getUUID().equals(player.getUUID());
  }

  /**
   * 收集玩家周围可用的飞剑。
   */
  private static List<FlyingSwordEntity> collectOwnedSwords(ServerPlayer player) {
    double radius = JianmaiNBT.readRadius(player);
    return player.level().getEntitiesOfClass(
        FlyingSwordEntity.class,
        player.getBoundingBox().inflate(radius),
        sword -> sword.isAlive() && isOwnedBy(sword, player));
  }

  /**
   * 计算飞剑对主动增幅的贡献：每把剑提供基础值 + 经验线性加成。
   */
  private static double calculateSwordBonus(List<FlyingSwordEntity> swords) {
    double total = 0.0;
    for (FlyingSwordEntity sword : swords) {
      double base = JianmaiTuning.ACTIVE_SWORD_BASE;
      double expBonus = sword.getExperience() * JianmaiTuning.ACTIVE_SWORD_EXP_K;
      total += base + expBonus;
    }
    return total;
  }

  // ========== 卸载监听器 ==========

  /**
   * 器官移除时调用（清空所有数据与属性修饰）。
   *
   * @param entity 实体
   * @param cc 胸腔实例
   * @param organ 器官物品
   */
  @Override
  public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof ServerPlayer player) || entity.level().isClientSide()) {
      return;
    }

    // 清空所有 NBT 数据
    JianmaiNBT.clearAll(player);
    JianmaiAmpOps.clearAll(player);

    // 移除属性修饰
    JianmaiPlayerTickEvents.removePlayerAttributes(player);
  }
}
