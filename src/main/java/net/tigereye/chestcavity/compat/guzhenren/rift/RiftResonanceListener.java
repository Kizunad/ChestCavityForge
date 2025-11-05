package net.tigereye.chestcavity.compat.guzhenren.rift;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;

/**
 * 裂隙共鸣监听器
 *
 * <p>监听剑道技能释放，在技能释放位置附近触发裂隙共鸣。
 *
 * <p>使用方式：在剑道技能释放时调用 {@link #onSwordQiSkillCast}
 */
public final class RiftResonanceListener {

  private RiftResonanceListener() {}

  /** 共鸣检测范围（格） */
  private static final double RESONANCE_DETECTION_RADIUS = 5.0;

  /**
   * 剑道技能白名单（可选的额外过滤）
   *
   * <p>注意：主要的技能过滤由 ActivationHookRegistry 的正则表达式处理。
   * 此白名单可用于运行时动态添加特殊技能，或用于其他需要判断技能类型的场景。
   *
   * <p>匹配的技能模式（在 ActivationHookRegistry 中定义）：
   * <pre>"^guzhenren:(jiandao/.*|jian_.*|.*_slash|.*_wave|flying_sword_.*)$"</pre>
   */
  private static final Set<ResourceLocation> SWORD_QI_SKILLS = new HashSet<>();

  /**
   * 注册剑气技能
   *
   * <p>允许外部动态注册会触发裂隙共鸣的技能
   *
   * @param skillId 技能ID
   */
  public static void registerSwordQiSkill(ResourceLocation skillId) {
    SWORD_QI_SKILLS.add(skillId);
    ChestCavity.LOGGER.info(
        "[RiftResonanceListener] Registered sword qi skill: {}", skillId);
  }

  /**
   * 当剑气技能释放时调用
   *
   * <p>检测释放位置附近的裂隙，并触发共鸣波
   *
   * <p>注意：技能过滤已由 ActivationHookRegistry 的正则表达式处理，
   * 此方法会接收所有匹配 "^guzhenren:(jiandao/.*|jian_.*|.*_slash|.*_wave|flying_sword_.*)$" 的技能
   *
   * @param caster 施法者
   * @param skillId 技能ID
   * @param castPos 施法位置（剑气攻击的目标位置）
   */
  public static void onSwordQiSkillCast(
      LivingEntity caster, ResourceLocation skillId, Vec3 castPos) {

    if (caster.level().isClientSide || !(caster.level() instanceof ServerLevel level)) {
      return;
    }

    // 查找附近的裂隙
    List<RiftEntity> nearbyRifts =
        RiftManager.getInstance().getRiftsNear(level, castPos, RESONANCE_DETECTION_RADIUS);

    if (nearbyRifts.isEmpty()) {
      return;
    }

    // 触发最近的裂隙共鸣
    RiftEntity closestRift = findClosestRift(nearbyRifts, castPos);
    if (closestRift != null) {
      ChestCavity.LOGGER.debug(
          "[RiftResonanceListener] Triggering resonance from skill {} at {}",
          skillId,
          castPos);

      // 触发共鸣波（会自动链式传播）
      RiftManager.getInstance().triggerResonanceWave(closestRift, caster);
    }
  }

  /**
   * 当飞剑攻击时调用（飞剑也算剑气类攻击）
   *
   * @param sword 飞剑实体
   * @param hitPos 命中位置
   */
  public static void onFlyingSwordHit(
      net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.flyingsword.FlyingSwordEntity sword,
      Vec3 hitPos) {

    if (sword.level().isClientSide || !(sword.level() instanceof ServerLevel level)) {
      return;
    }

    LivingEntity owner = sword.getOwner();
    if (owner == null) {
      return;
    }

    // 查找附近的裂隙
    List<RiftEntity> nearbyRifts =
        RiftManager.getInstance().getRiftsNear(level, hitPos, RESONANCE_DETECTION_RADIUS);

    if (nearbyRifts.isEmpty()) {
      return;
    }

    // 触发最近的裂隙共鸣
    RiftEntity closestRift = findClosestRift(nearbyRifts, hitPos);
    if (closestRift != null) {
      ChestCavity.LOGGER.debug(
          "[RiftResonanceListener] Triggering resonance from flying sword at {}", hitPos);

      RiftManager.getInstance().triggerResonanceWave(closestRift, owner);
    }
  }

  /**
   * 查找最近的裂隙
   *
   * @param rifts 裂隙列表
   * @param pos 位置
   * @return 最近的裂隙
   */
  private static RiftEntity findClosestRift(List<RiftEntity> rifts, Vec3 pos) {
    RiftEntity closest = null;
    double minDistSq = Double.MAX_VALUE;

    for (RiftEntity rift : rifts) {
      double distSq = rift.position().distanceToSqr(pos);
      if (distSq < minDistSq) {
        minDistSq = distSq;
        closest = rift;
      }
    }

    return closest;
  }

  /**
   * 判断技能是否为剑气类技能
   *
   * @param skillId 技能ID
   * @return 是否为剑气技能
   */
  public static boolean isSwordQiSkill(ResourceLocation skillId) {
    return SWORD_QI_SKILLS.contains(skillId);
  }
}
