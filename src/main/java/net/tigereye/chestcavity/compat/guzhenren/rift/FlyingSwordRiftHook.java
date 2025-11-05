package net.tigereye.chestcavity.compat.guzhenren.rift;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ.LieJianGuOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events.FlyingSwordEventHook;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events.context.HitEntityContext;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;

/**
 * 飞剑裂隙事件钩子
 *
 * <p>当飞剑命中敌人时，有概率放置微型裂隙。
 *
 * <p>特性：
 * <ul>
 *   <li>最小间距：12格</li>
 *   <li>内部冷却：5秒（每把飞剑）</li>
 *   <li>自动触发共鸣</li>
 * </ul>
 */
public class FlyingSwordRiftHook implements FlyingSwordEventHook {

  private static final FlyingSwordRiftHook INSTANCE = new FlyingSwordRiftHook();

  /** 最小裂隙间距（格） */
  private static final double MIN_RIFT_DISTANCE = 12.0;

  /** 飞剑放置裂隙的冷却时间（tick） */
  private static final int PLACEMENT_COOLDOWN = 5 * 20; // 5秒

  /** 飞剑UUID -> 上次放置裂隙的时间（tick） */
  private final Map<UUID, Long> lastPlacementTimes = new HashMap<>();

  /** 飞剑UUID -> 上次进行“每秒概率”检查的时间（tick） */
  private final Map<UUID, Long> lastPeriodicCheck = new HashMap<>();

  private FlyingSwordRiftHook() {}

  public static FlyingSwordRiftHook getInstance() {
    return INSTANCE;
  }

  @Override
  public void onHitEntity(HitEntityContext ctx) {
    // 检查是否拥有裂剑蛊
    if (!hasLieJianGu(ctx.owner)) {
      return;
    }

    UUID swordId = ctx.sword.getUUID();
    long now = ctx.level.getGameTime();

    // 检查飞剑冷却
    Long lastPlacement = lastPlacementTimes.get(swordId);
    if (lastPlacement != null && now - lastPlacement < PLACEMENT_COOLDOWN) {
      return;
    }

    // 检查最小间距
    Vec3 hitPos = ctx.target.position();
    if (!checkMinDistance(ctx.level, hitPos)) {
      return;
    }

    // 放置微型裂隙
    placeMinorRift(ctx.level, ctx.owner, hitPos);

    // 记录放置时间
    lastPlacementTimes.put(swordId, now);

    // 触发共鸣
    RiftResonanceListener.onFlyingSwordHit(ctx.sword, hitPos);

    ChestCavity.LOGGER.debug(
        "[FlyingSwordRiftHook] Placed minor rift at {} from flying sword hit", hitPos);
  }

  @Override
  public void onTick(
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.events.context
              .TickContext ctx) {
    // 仅服务端执行
    if (ctx.level.isClientSide) return;
    if (!hasLieJianGu(ctx.owner)) return;

    UUID swordId = ctx.sword.getUUID();
    long now = ctx.level.getGameTime();

    // 每秒检查一次
    long last = lastPeriodicCheck.getOrDefault(swordId, 0L);
    if (now - last < 20) {
      return;
    }
    lastPeriodicCheck.put(swordId, now);

    // 内部冷却未结束则不放置
    Long lastPlace = lastPlacementTimes.get(swordId);
    if (lastPlace != null && now - lastPlace < PLACEMENT_COOLDOWN) {
      return;
    }

    // 计算概率：基础1%，随剑道道痕提升，最大20%
    double daoHen = net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps
        .openHandle(ctx.owner)
        .map(h -> net.tigereye.chestcavity.compat.guzhenren.util.behavior.DaoHenResourceOps.get(h, "daohen_jiandao"))
        .orElse(0.0);
    double chance = Math.min(0.20, 0.01 + daoHen * 0.00002);

    if (ctx.owner.getRandom().nextDouble() >= chance) {
      return;
    }

    // 以飞剑当前位置为候选点，遵循最小间距
    Vec3 hitPos = ctx.sword.position();
    if (!checkMinDistance(ctx.level, hitPos)) {
      return;
    }

    // 放置微型裂隙 + 记录冷却 + 触发共鸣
    placeMinorRift(ctx.level, ctx.owner, hitPos);
    lastPlacementTimes.put(swordId, now);
    RiftResonanceListener.onFlyingSwordHit(ctx.sword, hitPos);

    ChestCavity.LOGGER.debug(
        "[FlyingSwordRiftHook] Periodic minor rift at {} (chance={})", hitPos, String.format(java.util.Locale.ROOT, "%.3f", chance));
  }

  /**
   * 检查所有者是否拥有裂剑蛊
   */
  private boolean hasLieJianGu(LivingEntity owner) {
    if (owner == null) {
      return false;
    }

    ChestCavityInstance cc =
        ChestCavityEntity.of(owner)
            .map(ChestCavityEntity::getChestCavityInstance)
            .orElse(null);

    if (cc == null || cc.inventory == null) {
      return false;
    }

    // 检查是否有裂剑蛊
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      net.minecraft.world.item.ItemStack stack = cc.inventory.getItem(i);
      if (stack.isEmpty()) {
        continue;
      }

      net.minecraft.resources.ResourceLocation id =
          net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (id != null && id.equals(LieJianGuOrganBehavior.ORGAN_ID)) {
        return true;
      }
    }

    return false;
  }

  /**
   * 检查最小间距
   *
   * @param level 世界
   * @param pos 目标位置
   * @return 是否满足最小间距要求
   */
  private boolean checkMinDistance(ServerLevel level, Vec3 pos) {
    List<RiftEntity> nearbyRifts =
        RiftManager.getInstance().getRiftsNear(level, pos, MIN_RIFT_DISTANCE);
    return nearbyRifts.isEmpty();
  }

  /**
   * 放置微型裂隙
   *
   * @param level 世界
   * @param caster 施放者
   * @param hitPos 命中位置
   */
  private void placeMinorRift(ServerLevel level, LivingEntity caster, Vec3 hitPos) {
    // 贴地
    BlockPos blockPos = BlockPos.containing(hitPos);
    BlockPos groundPos =
        level.getHeightmapPos(
            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPos);
    Vec3 finalPos = new Vec3(hitPos.x, groundPos.getY() + 0.1, hitPos.z);

    // 创建微型裂隙（道痕增幅在穿刺阶段单独计算）
    RiftEntity rift = RiftEntity.create(level, finalPos, caster, RiftType.MINOR, 0);

    // 添加到世界
    level.addFreshEntity(rift);

    // 注册到RiftManager
    RiftManager.getInstance().registerRift(rift);
  }

  /**
   * 清理过期的冷却记录（定期调用）
   */
  public void cleanupOldCooldowns(long currentTime) {
    lastPlacementTimes.entrySet().removeIf(entry -> currentTime - entry.getValue() > PLACEMENT_COOLDOWN * 10);
  }
}
