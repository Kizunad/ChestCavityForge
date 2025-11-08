package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.events;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.runtime.JianmaiAmpOps;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.runtime.JianmaiNBT;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianmaiTuning;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 剑脉蛊被动事件处理类。
 *
 * <p>监听玩家 Tick 事件，每 {@link JianmaiTuning#JME_TICK_INTERVAL} 刷新一次剑脉效率（JME）：
 * <ul>
 *   <li>扫描周围飞剑，计算距离权重贡献</li>
 *   <li>更新 JME 值并刷新临时增幅</li>
 *   <li>动态应用玩家属性修饰（移速、攻击力）</li>
 * </ul>
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class JianmaiPlayerTickEvents {

  private static final Logger LOGGER = LoggerFactory.getLogger(JianmaiPlayerTickEvents.class);

  private JianmaiPlayerTickEvents() {}

  private static final String MOD_ID = "guzhenren";

  // 属性修饰 ID
  private static final ResourceLocation SPEED_MODIFIER_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jianmai_speed");
  private static final ResourceLocation ATK_MODIFIER_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "jianmai_atk");

  /**
   * 玩家 Tick 事件处理器（服务端）。
   *
   * @param event 玩家 Tick 事件
   */
  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }

    if (player.level().isClientSide()) {
      return;
    }

    // 检查玩家是否装备了剑脉蛊
    if (!hasJianmaiGu(player)) {
      return;
    }

    long now = player.serverLevel().getGameTime();

    // 先检查并回滚过期的增幅
    JianmaiAmpOps.rollbackIfExpired(player, now);

    long lastTick = JianmaiNBT.readLastTick(player);

    // 检查刷新间隔（首次触发时 lastTick = 0，应立即执行）
    if (lastTick > 0 && now - lastTick < JianmaiTuning.JME_TICK_INTERVAL) {
      return;
    }

    // 扫描飞剑并计算 JME
    double jme = scanAndCalculateJME(player);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[Jianmai] Player {} JME calculated: jme={}, isFirstTick={}",
          player.getName().getString(),
          jme,
          lastTick == 0);
    }

    // 写回 JME 与刷新时间
    JianmaiNBT.writeJME(player, jme);
    JianmaiNBT.writeLastTick(player, now);

    // 刷新临时增幅
    JianmaiAmpOps.refreshFromJME(player, jme, now);

    // 应用玩家属性修饰
    applyPlayerAttributes(player, jme);
  }

  /**
   * 扫描周围飞剑并计算剑脉效率（JME）。
   *
   * <p>规则：
   * <ul>
   *   <li>扫描半径：{@link JianmaiTuning#JME_RADIUS}</li>
   *   <li>距离权重：w(d) = max(0, 1 - (d/R)^ALPHA)</li>
   *   <li>单剑贡献：BASE_PER_SWORD * w(d) * (1 + LEVEL_BONUS * (level - 1))</li>
   *   <li>聚合并裁剪：clamp(sum, 0, JME_MaxCap)</li>
   * </ul>
   *
   * @param player 玩家
   * @return JME 值
   */
  private static double scanAndCalculateJME(ServerPlayer player) {
    double radius = JianmaiNBT.readRadius(player);
    double alpha = JianmaiNBT.readDistAlpha(player);
    double maxCap = JianmaiNBT.readMaxCap(player);

    // 扫描周围飞剑
    List<FlyingSwordEntity> swords =
        player.level().getEntitiesOfClass(
            FlyingSwordEntity.class,
            player.getBoundingBox().inflate(radius),
            sword -> sword.isAlive() && isOwnedBy(sword, player));

    double totalJME = 0.0;

    for (FlyingSwordEntity sword : swords) {
      double distance = player.position().distanceTo(sword.position());
      if (distance > radius) {
        continue;
      }

      // 距离权重：w(d) = max(0, 1 - (d/R)^ALPHA)
      double ratio = distance / radius;
      double weight = Math.max(0.0, 1.0 - Math.pow(ratio, alpha));

      // 飞剑等级（从 1 开始）
      int level = Math.max(1, sword.getSwordLevel());

      // 单剑贡献：BASE_PER_SWORD * w(d) * (1 + LEVEL_BONUS * (level - 1))
      double contrib =
          JianmaiTuning.BASE_PER_SWORD * weight * (1.0 + JianmaiTuning.LEVEL_BONUS * (level - 1));

      totalJME += contrib;
    }

    // 裁剪到上限
    double finalJME = Math.max(0.0, Math.min(totalJME, maxCap));

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[Jianmai] Player {} scan result: swords={}, totalJME={}, finalJME={}, radius={}",
          player.getName().getString(),
          swords.size(),
          totalJME,
          finalJME,
          radius);
    }

    return finalJME;
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
   * 应用玩家属性修饰（移速、攻击力）。
   *
   * <p>规则：
   * <ul>
   *   <li>移速：ADD_MULTIPLIED_TOTAL，amount = PLAYER_SPEED_K * JME</li>
   *   <li>攻击力：ADD_MULTIPLIED_BASE，amount = PLAYER_ATK_K * JME</li>
   *   <li>当 JME = 0 时，移除修饰</li>
   * </ul>
   *
   * @param player 玩家
   * @param jme JME 值
   */
  private static void applyPlayerAttributes(ServerPlayer player, double jme) {
    // 移速修饰
    AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
    if (speedAttr != null) {
      speedAttr.removeModifier(SPEED_MODIFIER_ID);
      if (jme > 0.0) {
        double amount = JianmaiTuning.PLAYER_SPEED_K * jme;
        AttributeModifier modifier =
            new AttributeModifier(
                SPEED_MODIFIER_ID,
                amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        speedAttr.addTransientModifier(modifier);
      }
    }

    // 攻击力修饰
    AttributeInstance atkAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (atkAttr != null) {
      atkAttr.removeModifier(ATK_MODIFIER_ID);
      if (jme > 0.0) {
        double amount = JianmaiTuning.PLAYER_ATK_K * jme;
        AttributeModifier modifier =
            new AttributeModifier(
                ATK_MODIFIER_ID,
                amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        atkAttr.addTransientModifier(modifier);
      }
    }
  }

  /**
   * 移除玩家属性修饰（卸载时使用）。
   *
   * @param player 玩家
   */
  public static void removePlayerAttributes(ServerPlayer player) {
    AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
    if (speedAttr != null) {
      speedAttr.removeModifier(SPEED_MODIFIER_ID);
    }

    AttributeInstance atkAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
    if (atkAttr != null) {
      atkAttr.removeModifier(ATK_MODIFIER_ID);
    }
  }

  /**
   * 检查玩家是否装备了剑脉蛊。
   *
   * @param player 玩家
   * @return 是否装备了剑脉蛊
   */
  private static boolean hasJianmaiGu(ServerPlayer player) {
    ResourceLocation organId = ResourceLocation.fromNamespaceAndPath(MOD_ID, "jianmaigu");
    return ChestCavityEntity.of(player)
        .map(ChestCavityEntity::getChestCavityInstance)
        .filter(cc -> cc.inventory != null)
        .map(
            cc -> {
              for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
                var stack = cc.inventory.getItem(i);
                if (stack.isEmpty()) {
                  continue;
                }
                ResourceLocation itemId =
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (itemId != null && itemId.equals(organId)) {
                  return true;
                }
              }
              return false;
            })
        .orElse(false);
  }
}
