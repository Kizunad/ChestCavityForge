package net.tigereye.chestcavity.compat.guzhenren.domain;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/**
 * 领域标签系统
 *
 * <p>为实体和坐标附加领域相关的标签，用于：
 *
 * <ul>
 *   <li>标记实体是否在某个领域内
 *   <li>记录领域主人UUID
 *   <li>记录领域来源和等级
 *   <li>特殊状态标记（如无敌、无法打断等）
 * </ul>
 *
 * <p>标签存储在实体的NBT数据中，自动随实体序列化。
 */
public final class DomainTags {

  /** NBT根键 */
  private static final String NBT_ROOT = "guzhenren_domain";

  /** 标签：当前位于剑域内 */
  public static final String TAG_SWORD_DOMAIN = "sword_domain";

  /** 标签：剑域创建者UUID */
  public static final String TAG_SWORD_DOMAIN_OWNER = "sword_domain_owner";

  /** 标签：剑域来源标识 */
  public static final String TAG_SWORD_DOMAIN_SOURCE = "sword_domain_source";

  /** 标签：剑域等级 */
  public static final String TAG_SWORD_DOMAIN_LEVEL = "sword_domain_level";

  /** 标签：实体位于剑域中 */
  public static final String TAG_IN_SWORD_DOMAIN = "in_sword_domain";

  /** 标签：无视打断（定心或杀招赋予） */
  public static final String TAG_UNBREAKABLE_FOCUS = "unbreakable_focus";

  /** 标签：剑道冻结剩余ticks（>0 表示处于冻结期） */
  public static final String TAG_JIANGDAO_FREEZE_TICKS = "jiandao_frozen_ticks";

  /** 标签：剑域·移动速度衰减（0表示关闭，(0,1] 表示倍率 1-衰减。例如0.9 => 90% 衰减） */
  public static final String TAG_JIANXIN_VELOCITY_DECREASEMENT = "jianxin_velocity_decreasement";

  // ===== 剑域“域控系数”广播（存放于领域所有者的 NBT） =====
  public static final String TAG_SD_R = "sword_domain_R";
  public static final String TAG_SD_S = "sword_domain_s";
  public static final String TAG_SD_P_OUT = "sword_domain_p_out";
  public static final String TAG_SD_P_IN = "sword_domain_p_in";
  public static final String TAG_SD_P_MOVE = "sword_domain_p_move";
  public static final String TAG_SD_ENTITY_ENABLED = "sword_domain_entity_enabled";
  public static final String TAG_SD_E = "sword_domain_E";
  public static final String TAG_SD_P_OUT_ENTITY = "sword_domain_p_out_entity";
  public static final String TAG_SD_TIMELEFT_TICKS = "sword_domain_timeleft_ticks";

  private DomainTags() {}

  /**
   * 获取实体的领域NBT数据
   *
   * @param entity 实体
   * @param create 如果不存在是否创建
   * @return NBT数据，如果不存在且create=false则返回null
   */
  private static CompoundTag getDomainNBT(Entity entity, boolean create) {
    CompoundTag persistent = entity.getPersistentData();
    if (!persistent.contains(NBT_ROOT)) {
      if (!create) {
        return null;
      }
      persistent.put(NBT_ROOT, new CompoundTag());
    }
    return persistent.getCompound(NBT_ROOT);
  }

  /**
   * 检查实体是否有指定标签
   *
   * @param entity 实体
   * @param tag 标签名
   * @return 是否有该标签
   */
  public static boolean hasTag(Entity entity, String tag) {
    CompoundTag nbt = getDomainNBT(entity, false);
    return nbt != null && nbt.getBoolean(tag);
  }

  /**
   * 为实体添加标签
   *
   * @param entity 实体
   * @param tag 标签名
   */
  public static void addTag(Entity entity, String tag) {
    CompoundTag nbt = getDomainNBT(entity, true);
    nbt.putBoolean(tag, true);
  }

  /**
   * 移除实体的标签
   *
   * @param entity 实体
   * @param tag 标签名
   */
  public static void removeTag(Entity entity, String tag) {
    CompoundTag nbt = getDomainNBT(entity, false);
    if (nbt != null) {
      nbt.remove(tag);
    }
  }

  /**
   * 获取字符串类型的标签值
   *
   * @param entity 实体
   * @param tag 标签名
   * @return 标签值，如果不存在则返回null
   */
  public static String getStringTag(Entity entity, String tag) {
    CompoundTag nbt = getDomainNBT(entity, false);
    if (nbt == null || !nbt.contains(tag)) {
      return null;
    }
    return nbt.getString(tag);
  }

  /** 获取双精度类型的标签值，不存在返回0.0。 */
  public static double getDoubleTag(Entity entity, String tag) {
    CompoundTag nbt = getDomainNBT(entity, false);
    if (nbt == null) {
      return 0.0;
    }
    return nbt.getDouble(tag);
  }

  /** 设置双精度类型的标签值。 */
  public static void setDoubleTag(Entity entity, String tag, double value) {
    CompoundTag nbt = getDomainNBT(entity, true);
    nbt.putDouble(tag, value);
  }

  /**
   * 设置字符串类型的标签
   *
   * @param entity 实体
   * @param tag 标签名
   * @param value 标签值
   */
  public static void setStringTag(Entity entity, String tag, String value) {
    CompoundTag nbt = getDomainNBT(entity, true);
    nbt.putString(tag, value);
  }

  /**
   * 获取整数类型的标签值
   *
   * @param entity 实体
   * @param tag 标签名
   * @return 标签值，如果不存在则返回0
   */
  public static int getIntTag(Entity entity, String tag) {
    CompoundTag nbt = getDomainNBT(entity, false);
    if (nbt == null) {
      return 0;
    }
    return nbt.getInt(tag);
  }

  /**
   * 设置整数类型的标签
   *
   * @param entity 实体
   * @param tag 标签名
   * @param value 标签值
   */
  public static void setIntTag(Entity entity, String tag, int value) {
    CompoundTag nbt = getDomainNBT(entity, true);
    nbt.putInt(tag, value);
  }

  /**
   * 清除实体的所有领域标签
   *
   * @param entity 实体
   */
  public static void clearAllTags(Entity entity) {
    entity.getPersistentData().remove(NBT_ROOT);
  }

  /**
   * 标记实体进入剑心域
   *
   * @param entity 实体
   * @param domainId 领域ID
   * @param ownerUUID 领域主人UUID
   * @param level 领域等级
   */
  public static void markEnterSwordDomain(Entity entity, UUID domainId, UUID ownerUUID, int level) {
    addTag(entity, TAG_SWORD_DOMAIN);
    addTag(entity, TAG_IN_SWORD_DOMAIN);
    setStringTag(entity, TAG_SWORD_DOMAIN_OWNER, ownerUUID.toString());
    setStringTag(entity, TAG_SWORD_DOMAIN_SOURCE, "jianxin");
    setIntTag(entity, TAG_SWORD_DOMAIN_LEVEL, level);
  }

  /**
   * 标记实体离开剑心域
   *
   * @param entity 实体
   */
  public static void markLeaveSwordDomain(Entity entity) {
    removeTag(entity, TAG_SWORD_DOMAIN);
    removeTag(entity, TAG_IN_SWORD_DOMAIN);
    removeTag(entity, TAG_SWORD_DOMAIN_OWNER);
    removeTag(entity, TAG_SWORD_DOMAIN_SOURCE);
    removeTag(entity, TAG_SWORD_DOMAIN_LEVEL);
  }

  /**
   * 检查实体是否在剑域中
   *
   * @param entity 实体
   * @return 是否在剑域中
   */
  public static boolean isInSwordDomain(Entity entity) {
    return hasTag(entity, TAG_IN_SWORD_DOMAIN);
  }

  /**
   * 获取剑域主人UUID
   *
   * @param entity 实体
   * @return 主人UUID，如果不在剑域中则返回null
   */
  public static UUID getSwordDomainOwner(Entity entity) {
    String uuidStr = getStringTag(entity, TAG_SWORD_DOMAIN_OWNER);
    if (uuidStr == null || uuidStr.isEmpty()) {
      return null;
    }
    try {
      return UUID.fromString(uuidStr);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * 获取剑域等级
   *
   * @param entity 实体
   * @return 领域等级，如果不在剑域中则返回0
   */
  public static int getSwordDomainLevel(Entity entity) {
    return getIntTag(entity, TAG_SWORD_DOMAIN_LEVEL);
  }

  /**
   * 赋予无敌焦点状态
   *
   * @param entity 实体
   * @param durationTicks 持续时间（tick）
   */
  public static void grantUnbreakableFocus(Entity entity, int durationTicks) {
    addTag(entity, TAG_UNBREAKABLE_FOCUS);
    setIntTag(entity, TAG_UNBREAKABLE_FOCUS + "_duration", durationTicks);
  }

  /**
   * 检查是否有无敌焦点状态
   *
   * @param entity 实体
   * @return 是否有无敌焦点
   */
  public static boolean hasUnbreakableFocus(Entity entity) {
    return hasTag(entity, TAG_UNBREAKABLE_FOCUS);
  }

  /**
   * 移除无敌焦点状态
   *
   * @param entity 实体
   */
  public static void removeUnbreakableFocus(Entity entity) {
    removeTag(entity, TAG_UNBREAKABLE_FOCUS);
    removeTag(entity, TAG_UNBREAKABLE_FOCUS + "_duration");
  }

  /**
   * 更新无敌焦点状态（每tick调用）
   *
   * @param entity 实体
   */
  public static void tickUnbreakableFocus(Entity entity) {
    if (!hasUnbreakableFocus(entity)) {
      return;
    }

    int duration = getIntTag(entity, TAG_UNBREAKABLE_FOCUS + "_duration");
    duration--;

    if (duration <= 0) {
      removeUnbreakableFocus(entity);
    } else {
      setIntTag(entity, TAG_UNBREAKABLE_FOCUS + "_duration", duration);
    }
  }

  /** 设置“剑道冻结”剩余时长（tick）。 */
  public static void setJiandaoFrozen(Entity entity, int ticks) {
    int clamped = Math.max(0, ticks);
    if (clamped > 0) {
      setIntTag(entity, TAG_JIANGDAO_FREEZE_TICKS, clamped);
    } else {
      removeTag(entity, TAG_JIANGDAO_FREEZE_TICKS);
    }
  }

  /** 是否处于“剑道冻结”状态。 */
  public static boolean isJiandaoFrozen(Entity entity) {
    return getIntTag(entity, TAG_JIANGDAO_FREEZE_TICKS) > 0;
  }

  /** 每tick衰减“剑道冻结”时长。 */
  public static void tickJiandaoFrozen(Entity entity) {
    int cur = getIntTag(entity, TAG_JIANGDAO_FREEZE_TICKS);
    if (cur <= 0) {
      return;
    }
    cur -= 1;
    if (cur <= 0) {
      removeTag(entity, TAG_JIANGDAO_FREEZE_TICKS);
    } else {
      setIntTag(entity, TAG_JIANGDAO_FREEZE_TICKS, cur);
    }
  }

  // ===== 剑域移动减速（玩家开启冥想时设置，结束时清除） =====

  public static void setJianxinVelocityDecreasement(Entity entity, double value) {
    double v = Math.max(0.0, Math.min(10000.0, value));
    if (v <= 0.0) {
      removeTag(entity, TAG_JIANXIN_VELOCITY_DECREASEMENT);
    } else {
      setDoubleTag(entity, TAG_JIANXIN_VELOCITY_DECREASEMENT, v);
    }
  }

  public static double getJianxinVelocityDecreasement(Entity entity) {
    return getDoubleTag(entity, TAG_JIANXIN_VELOCITY_DECREASEMENT);
  }

  public static boolean hasJianxinVelocityDecreasement(Entity entity) {
    return getJianxinVelocityDecreasement(entity) > 0.0;
  }

  // ===== 域控系数（所有者）读写 =====

  public static void setSwordDomainControl(
      Entity owner,
      double R,
      double s,
      double pOut,
      double pIn,
      double pMove,
      boolean entityEnabled,
      double E,
      double pOutEntity,
      int timeLeftTicks) {
    CompoundTag nbt = getDomainNBT(owner, true);
    nbt.putDouble(TAG_SD_R, R);
    nbt.putDouble(TAG_SD_S, s);
    nbt.putDouble(TAG_SD_P_OUT, pOut);
    nbt.putDouble(TAG_SD_P_IN, pIn);
    nbt.putDouble(TAG_SD_P_MOVE, pMove);
    nbt.putBoolean(TAG_SD_ENTITY_ENABLED, entityEnabled);
    nbt.putDouble(TAG_SD_E, E);
    nbt.putDouble(TAG_SD_P_OUT_ENTITY, pOutEntity);
    nbt.putInt(TAG_SD_TIMELEFT_TICKS, Math.max(0, timeLeftTicks));
  }

  public static void clearSwordDomainControl(Entity owner) {
    CompoundTag nbt = getDomainNBT(owner, false);
    if (nbt == null) return;
    nbt.remove(TAG_SD_R);
    nbt.remove(TAG_SD_S);
    nbt.remove(TAG_SD_P_OUT);
    nbt.remove(TAG_SD_P_IN);
    nbt.remove(TAG_SD_P_MOVE);
    nbt.remove(TAG_SD_ENTITY_ENABLED);
    nbt.remove(TAG_SD_E);
    nbt.remove(TAG_SD_P_OUT_ENTITY);
    nbt.remove(TAG_SD_TIMELEFT_TICKS);
  }

  public static double getSwordDomainR(Entity owner) {
    return getDoubleTag(owner, TAG_SD_R);
  }

  public static double getSwordDomainS(Entity owner) {
    return getDoubleTag(owner, TAG_SD_S);
  }

  public static double getSwordDomainPout(Entity owner) {
    return getDoubleTag(owner, TAG_SD_P_OUT);
  }

  public static double getSwordDomainPin(Entity owner) {
    return getDoubleTag(owner, TAG_SD_P_IN);
  }

  public static double getSwordDomainPmove(Entity owner) {
    return getDoubleTag(owner, TAG_SD_P_MOVE);
  }

  public static boolean isSwordDomainEntityEnabled(Entity owner) {
    CompoundTag nbt = getDomainNBT(owner, false);
    return nbt != null && nbt.getBoolean(TAG_SD_ENTITY_ENABLED);
  }

  public static double getSwordDomainE(Entity owner) {
    return getDoubleTag(owner, TAG_SD_E);
  }

  public static double getSwordDomainPoutEntity(Entity owner) {
    return getDoubleTag(owner, TAG_SD_P_OUT_ENTITY);
  }

  public static int getSwordDomainTimeLeftTicks(Entity owner) {
    return getIntTag(owner, TAG_SD_TIMELEFT_TICKS);
  }
}
