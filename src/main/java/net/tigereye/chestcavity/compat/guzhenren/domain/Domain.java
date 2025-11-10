package net.tigereye.chestcavity.compat.guzhenren.domain;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 领域（Domain）通用接口
 *
 * <p>领域是以某个实体为中心的区域效果系统，可用于：
 *
 * <ul>
 *   <li>剑心域（剑道）
 *   <li>魂域（魂道）
 *   <li>其他道流派的领域效果
 * </ul>
 *
 * <p>核心特性：
 *
 * <ul>
 *   <li>有主人（玩家或生物）
 *   <li>有唯一UUID标识
 *   <li>有等级系统
 *   <li>范围内实体受效果影响
 *   <li>支持敌友判定
 * </ul>
 */
public interface Domain {

  /**
   * 获取领域唯一ID
   *
   * @return 领域UUID
   */
  UUID getDomainId();

  /**
   * 获取领域主人
   *
   * @return 主人实体，可能为null（主人已死亡或离线）
   */
  @Nullable
  LivingEntity getOwner();

  /**
   * 获取领域主人UUID
   *
   * @return 主人UUID
   */
  UUID getOwnerUUID();

  /**
   * 获取领域等级
   *
   * @return 领域等级（1-∞）
   */
  int getLevel();

  /**
   * 设置领域等级
   *
   * @param level 新等级
   */
  void setLevel(int level);

  /**
   * 获取领域中心位置
   *
   * @return 中心坐标
   */
  Vec3 getCenter();

  /**
   * 设置领域中心位置
   *
   * @param center 新中心坐标
   */
  void setCenter(Vec3 center);

  /**
   * 获取领域半径
   *
   * @return 半径（方块单位）
   */
  double getRadius();

  /**
   * 获取领域范围AABB
   *
   * @return 领域边界盒
   */
  default AABB getBounds() {
    Vec3 center = getCenter();
    double radius = getRadius();
    return new AABB(
        center.x - radius,
        center.y - radius,
        center.z - radius,
        center.x + radius,
        center.y + radius,
        center.z + radius);
  }

  /**
   * 获取领域类型标识
   *
   * <p>例如："jianxin"、"hun_domain"等
   *
   * @return 领域类型
   */
  String getDomainType();

  /**
   * 检查实体是否在领域内
   *
   * @param entity 实体
   * @return 是否在领域内
   */
  default boolean isInDomain(Entity entity) {
    return isInDomain(entity.position());
  }

  /**
   * 检查位置是否在领域内
   *
   * @param pos 位置
   * @return 是否在领域内
   */
  default boolean isInDomain(Vec3 pos) {
    return pos.distanceTo(getCenter()) <= getRadius();
  }

  /**
   * 检查位置是否在领域内
   *
   * @param pos 方块坐标
   * @return 是否在领域内
   */
  default boolean isInDomain(BlockPos pos) {
    return isInDomain(Vec3.atCenterOf(pos));
  }

  /**
   * 判断实体是否为友方
   *
   * <p>默认逻辑：
   *
   * <ul>
   *   <li>主人本身 → 友方
   *   <li>主人的队友 → 友方
   *   <li>其他 → 敌方
   * </ul>
   *
   * @param entity 实体
   * @return 是否为友方
   */
  boolean isFriendly(LivingEntity entity);

  /**
   * 每tick更新领域
   *
   * <p>在此方法中：
   *
   * <ul>
   *   <li>更新领域位置（跟随主人）
   *   <li>对范围内实体应用效果
   *   <li>处理领域特有逻辑
   * </ul>
   *
   * @param level 服务端世界
   */
  void tick(ServerLevel level);

  /**
   * 领域是否仍然有效
   *
   * <p>无效条件示例：
   *
   * <ul>
   *   <li>主人已死亡
   *   <li>主人已离线
   *   <li>被主动取消
   *   <li>达到时间限制
   * </ul>
   *
   * @return 是否有效
   */
  boolean isValid();

  /**
   * 销毁领域
   *
   * <p>清理资源、移除标签、触发特效等
   */
  void destroy();

  /**
   * 对实体应用领域效果
   *
   * <p>子类实现具体的buff/debuff逻辑
   *
   * @param level 服务端世界
   * @param entity 实体
   * @param isFriendly 是否为友方
   */
  void applyEffects(ServerLevel level, LivingEntity entity, boolean isFriendly);

  // ========== PNG渲染配置 ==========

  /**
   * 获取领域PNG纹理路径
   *
   * <p>如果返回null，则不渲染PNG纹理
   *
   * @return 纹理资源位置，null表示不渲染
   */
  default ResourceLocation getTexturePath() {
    return null; // 默认不渲染PNG
  }

  /**
   * 获取PNG渲染的高度偏移（相对于领域中心）
   *
   * @return 高度偏移（格），默认20.0
   */
  default double getPngHeightOffset() {
    return net.tigereye.chestcavity.compat.guzhenren.domain.client.DomainRenderer
        .DEFAULT_HEIGHT_OFFSET;
  }

  /**
   * 获取PNG渲染的透明度
   *
   * @return 透明度（0.0-1.0），默认0.5
   */
  default float getPngAlpha() {
    return 0.5f;
  }

  /**
   * 获取PNG旋转速度
   *
   * @return 旋转速度（度/tick），默认0.5
   */
  default float getPngRotationSpeed() {
    return 0.5f;
  }
}
