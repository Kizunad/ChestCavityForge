package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.active;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordController;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.FlyingSwordEventRegistry;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.events.context.DespawnContext;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.common.cost.ResourceCost;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.SuiRenGuState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.SuiRenGuCalc;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.tuning.SuiRenGuBalance;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;

/**
 * 碎刃蛊主动技能实现。
 *
 * <p>核心逻辑：
 * <ol>
 *   <li>牺牲所有在场、可回收、耐久>0的自有飞剑</li>
 *   <li>根据每把飞剑的经验与属性计算道痕增幅（单剑上限 PER_SWORD_CAP，总上限 CAST_TOTAL_CAP）</li>
 *   <li>临时增加玩家的剑道道痕（通过 ResourceHandle）</li>
 *   <li>记录增幅值与结束时间，到期自动回滚</li>
 *   <li>启动冷却</li>
 * </ol>
 *
 * <p>注意：
 * <ul>
 *   <li>若无可用飞剑，静默失败（不消耗资源、不启动冷却）</li>
 *   <li>资源不足时也静默失败</li>
 *   <li>增幅是临时的，不永久改变道痕</li>
 * </ul>
 */
public final class SuiRenGuActive {

  private SuiRenGuActive() {}

  /**
   * 激活碎刃蛊能力。
   *
   * @param player 玩家
   * @param cc 玩家的胸腔实例
   * @param organ 碎刃蛊器官物品
   * @param state 器官状态
   * @param cooldown 冷却管理器
   * @param now 当前游戏时间（tick）
   * @return 是否成功激活
   */
  public static boolean activate(
      ServerPlayer player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      MultiCooldown cooldown,
      long now) {

    ServerLevel level = player.serverLevel();

    // 1. 检查冷却
    MultiCooldown.Entry readyEntry =
        cooldown.entry(SuiRenGuState.KEY_READY_TICK).withDefault(0L);
    if (now < readyEntry.getReadyTick()) {
      return false;
    }

    // 2. 检查并消耗资源
    ResourceCost cost = new ResourceCost(
        SuiRenGuBalance.BASE_COST_ZHENYUAN,
        SuiRenGuBalance.BASE_COST_JINGLI,
        0.0, // hunpo
        SuiRenGuBalance.BASE_COST_NIANTOU,
        0, // hunger
        0.0f // health
    );
    if (!ResourceOps.payCost(player, cost, "碎刃蛊")) {
      return false; // 资源不足，静默失败
    }

    // 3. 获取所有飞剑并过滤可牺牲的
    List<FlyingSwordEntity> allSwords = FlyingSwordController.getPlayerSwords(level, player);
    List<FlyingSwordEntity> sacrificeable = new ArrayList<>();
    for (FlyingSwordEntity sword : allSwords) {
      // 过滤条件：自己的、可召回的、耐久>0
      if (sword.isOwnedBy(player) && sword.isRecallable() && sword.getDurability() > 0) {
        sacrificeable.add(sword);
      }
    }

    // 若无可牺牲的飞剑，退还资源并静默失败
    if (sacrificeable.isEmpty()) {
      // 退还资源
      Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = ResourceOps.openHandle(player);
      if (handleOpt.isPresent()) {
        GuzhenrenResourceBridge.ResourceHandle h = handleOpt.get();
        h.adjustZhenyuan(cost.zhenyuan(), true);
        h.adjustJingli(cost.jingli(), true);
        h.adjustNiantou(cost.niantou(), true);
      }
      return false;
    }

    // 4. 计算总增幅并牺牲飞剑
    List<SuiRenGuCalc.SwordStats> statsList = new ArrayList<>();
    for (FlyingSwordEntity sword : sacrificeable) {
      // 收集飞剑属性
      SuiRenGuCalc.SwordStats stats = new SuiRenGuCalc.SwordStats(
          sword.getExperience(),
          sword.getSwordAttributes().maxDurability,
          sword.getSwordAttributes().damageBase,
          sword.getSwordAttributes().speedMax
      );
      statsList.add(stats);

      // 播放特效（如果有 FX 模块）
      // SuiRenGuFx.playShardBurst(player, sword);

      // 触发飞剑消散事件（不回收）
      DespawnContext ctx = new DespawnContext(
          sword,
          level,
          player,
          DespawnContext.Reason.OTHER,
          null // 不回收
      );
      FlyingSwordEventRegistry.fireDespawnOrRecall(ctx);

      // 移除飞剑实体
      sword.discard();
    }

    // 5. 计算总增幅与持续时间
    int totalDelta = SuiRenGuCalc.totalForCast(statsList);
    int duration = SuiRenGuCalc.durationForCast(sacrificeable.size());

    // 6. 应用临时道痕增幅
    applyTempScarBuff(player, cc, organ, state, totalDelta, duration, now);

    // 7. 启动冷却
    readyEntry.setReadyAt(now + SuiRenGuBalance.COOLDOWN_TICKS);

    // 8. 播放完成特效（如果有）
    // SuiRenGuFx.playActivationComplete(player, sacrificeable.size(), totalDelta);

    return true;
  }

  /**
   * 应用临时道痕增幅。
   *
   * @param player 玩家
   * @param cc 胸腔实例
   * @param organ 器官物品
   * @param state 器官状态
   * @param delta 增幅值
   * @param duration 持续时间（ticks）
   * @param now 当前时间
   */
  private static void applyTempScarBuff(
      ServerPlayer player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state,
      int delta,
      int duration,
      long now) {

    // 1. 增加道痕（临时）
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = ResourceOps.openHandle(player);
    if (handleOpt.isEmpty()) {
      return;
    }
    GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
    handle.adjustDouble("daohen_jiandao", delta, true);

    // 2. 记录已应用的增幅值（累加，支持多次叠加）
    int currentApplied = state.getInt(SuiRenGuState.KEY_BUFF_APPLIED_DELTA, 0);
    int newApplied = currentApplied + (int) delta;
    OrganStateOps.setIntSync(
        cc,
        organ,
        SuiRenGuState.ROOT,
        SuiRenGuState.KEY_BUFF_APPLIED_DELTA,
        newApplied,
        v -> newApplied,
        0
    );

    // 3. 更新 buff 结束时间（取最晚的）
    long currentEndAt = state.getLong(SuiRenGuState.KEY_BUFF_END_AT_TICK, 0L);
    long newEndAt = Math.max(currentEndAt, now + duration);
    OrganStateOps.setLongSync(
        cc,
        organ,
        SuiRenGuState.ROOT,
        SuiRenGuState.KEY_BUFF_END_AT_TICK,
        newEndAt,
        v -> newEndAt,
        0L
    );

    // 4. 注册 buff 结束回调（通过 MultiCooldown）
    OrganState stateForCallback = OrganState.of(organ, SuiRenGuState.ROOT);
    MultiCooldown cdForBuff = MultiCooldown.builder(stateForCallback)
        .withSync(cc, organ)
        .build();
    MultiCooldown.Entry buffEndEntry =
        cdForBuff.entry(SuiRenGuState.KEY_BUFF_END_AT_TICK).withDefault(0L);

    buffEndEntry.onReady(player.serverLevel(), now, () -> {
      clearAppliedBuff(player, cc, organ, stateForCallback);
    });
  }

  /**
   * 清除已应用的道痕增幅（buff 结束时调用）。
   *
   * <p>关键：支持多次叠加施放。若玩家在 buff 持续期间再次施放并延长了结束时间，
   * 需要检查当前时间是否真正到达最新的结束时间，避免旧回调提前清除累积增幅。
   *
   * @param player 玩家
   * @param cc 胸腔实例
   * @param organ 器官物品
   * @param state 器官状态
   */
  private static void clearAppliedBuff(
      ServerPlayer player,
      ChestCavityInstance cc,
      ItemStack organ,
      OrganState state) {

    // 0. 时间检查：只有当前时间确实到达或超过结束时间才执行清除
    //    这样可以防止多次施放时旧回调提前清除累积的增幅
    long now = player.serverLevel().getGameTime();
    long buffEndAt = state.getLong(SuiRenGuState.KEY_BUFF_END_AT_TICK, 0L);
    if (now < buffEndAt) {
      // 还未到真正的结束时间，这是旧回调提前触发，直接返回
      return;
    }

    // 1. 读取已应用的增幅值
    int appliedDelta = state.getInt(SuiRenGuState.KEY_BUFF_APPLIED_DELTA, 0);
    if (appliedDelta <= 0) {
      return; // 没有需要回滚的
    }

    // 2. 回滚道痕
    Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = ResourceOps.openHandle(player);
    if (handleOpt.isPresent()) {
      GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
      handle.adjustDouble("daohen_jiandao", -appliedDelta, true);
    }

    // 3. 重置已应用值
    OrganStateOps.setIntSync(
        cc,
        organ,
        SuiRenGuState.ROOT,
        SuiRenGuState.KEY_BUFF_APPLIED_DELTA,
        0,
        v -> 0,
        0
    );

    // 4. 播放 buff 结束特效（如果有）
    // SuiRenGuFx.playBuffExpired(player);
  }
}
