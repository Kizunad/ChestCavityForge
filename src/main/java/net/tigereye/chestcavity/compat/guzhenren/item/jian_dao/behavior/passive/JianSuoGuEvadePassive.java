package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.passive;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.JianSuoGuState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JianSuoCalc;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.runtime.jian_suo.JianSuoRuntime;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.JianSuoGuTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 剑梭蛊被动效果：剑影身法（受击躲避）。
 *
 * <p>核心机制：
 * <ol>
 *   <li>检查冷却（全局冷却）</li>
 *   <li>概率触发（基于道痕）</li>
 *   <li>减免伤害（基于道痕）</li>
 *   <li>反向后退（安全移动）</li>
 *   <li>获得短暂无敌帧</li>
 *   <li>启动冷却</li>
 * </ol>
 *
 * <p>NPC 同样生效。
 */
public final class JianSuoGuEvadePassive {

  private static final Logger LOGGER = LoggerFactory.getLogger(JianSuoGuEvadePassive.class);

  private JianSuoGuEvadePassive() {}

  /**
   * 处理受击伤害（实现 {@link OrganIncomingDamageListener#onIncomingDamage}）。
   *
   * @param source 伤害源
   * @param victim 受击者
   * @param cc 胸腔实例
   * @param organ 剑梭蛊器官物品
   * @param damage 原始伤害
   * @return 修改后的伤害
   */
  public static float onIncomingDamage(
      DamageSource source,
      LivingEntity victim,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {

    // 仅服务端处理
    if (victim.level().isClientSide()) {
      return damage;
    }

    ServerLevel level = (ServerLevel) victim.level();
    long now = level.getGameTime();

    // 构建状态与冷却管理器
    OrganState state = OrganState.of(organ, JianSuoGuState.ROOT);
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();

    // 1. 检查冷却
    MultiCooldown.Entry evadeEntry =
        cooldown.entry(JianSuoGuState.KEY_EVADE_READY_TICK).withDefault(0L);
    if (now < evadeEntry.getReadyTick()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "[JianSuoGuEvadePassive] On cooldown, remaining: {} ticks",
            evadeEntry.getReadyTick() - now);
      }
      return damage; // 冷却中，不触发
    }

    // 2. 读取道痕
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = ResourceOps.openHandle(victim);
    if (handleOpt.isEmpty()) {
      return damage;
    }

    GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
    double daohen = handle.getDouble("jiandao:daohen_jiandao").orElse(0.0);

    // 3. 计算触发几率
    double evadeChance = JianSuoCalc.evadeChance(daohen);
    RandomSource random = RandomSource.create();
    if (random.nextDouble() >= evadeChance) {
      // 未触发
      return damage;
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[JianSuoGuEvadePassive] Triggered! daohen={}, chance={}",
          daohen, evadeChance);
    }

    // 4. 计算减伤
    double reduce = JianSuoCalc.evadeReduce(daohen);
    float remaining = damage * (float) (1.0 - reduce);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "[JianSuoGuEvadePassive] Damage reduced from {} to {} (reduce={})",
          damage, remaining, reduce);
    }

    // 5. 反向后退
    Vec3 backstepDir = JianSuoRuntime.backstepVector(victim, source);
    Vec3 backstepDelta = backstepDir.normalize().scale(JianSuoGuTuning.EVADE_BACKSTEP_DISTANCE);
    JianSuoRuntime.safeSlide(victim, backstepDelta);

    // 6. 无敌帧
    victim.invulnerableTime = Math.max(victim.invulnerableTime, JianSuoGuTuning.EVADE_INVULN_FRAMES);

    // 7. 播放特效
    JianSuoRuntime.spawnEvadeEffect(level, victim.position());

    // 8. 启动冷却
    int cdTicks = JianSuoCalc.secondsToTicks(JianSuoGuTuning.EVADE_COOLDOWN_S);
    evadeEntry.setReadyAt(now + cdTicks);

    return remaining;
  }
}
