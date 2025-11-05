package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.cooldown;

import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;

/**
 * Phase 4: 飞剑冷却统一管理 (Flying Sword Cooldown Operations)
 *
 * <p>职责:
 * <ul>
 *   <li>封装 MultiCooldown 操作，屏蔽附件获取细节</li>
 *   <li>统一冷却 Key 命名规范：cc:flying_sword/&lt;uuid&gt;/&lt;domain&gt;</li>
 *   <li>提供飞剑专用的冷却读写方法</li>
 * </ul>
 *
 * <p>设计原则:
 * <ul>
 *   <li>所有冷却存储在 owner 的 ChestCavityInstance (MultiCooldown 附件)</li>
 *   <li>冷却基于 EntryInt (倒计时模式)，每 tick 递减</li>
 *   <li>不允许负冷却：所有设置操作自动 clamp 到 0</li>
 * </ul>
 *
 * <p>Key 命名规范:
 * <pre>
 * - 攻击冷却:    cc:flying_sword/&lt;sword_uuid&gt;/attack
 * - 破块冷却:    cc:flying_sword/&lt;sword_uuid&gt;/block_break
 * - 能力冷却:    cc:flying_sword/&lt;sword_uuid&gt;/ability
 * - 全局冷却:    cc:flying_sword/global/&lt;domain&gt;
 * </pre>
 *
 * @see MultiCooldown
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.systems.CombatSystem
 * @see net.tigereye.chestcavity.compat.guzhenren.flyingsword.ops.BlockBreakOps
 */
public final class FlyingSwordCooldownOps {

  /** 冷却 Key 前缀 */
  private static final String KEY_PREFIX = "cc:flying_sword/";

  /** 攻击冷却域 */
  public static final String DOMAIN_ATTACK = "attack";

  /** 破块冷却域 */
  public static final String DOMAIN_BLOCK_BREAK = "block_break";

  /** 能力冷却域 */
  public static final String DOMAIN_ABILITY = "ability";

  private FlyingSwordCooldownOps() {
    // 禁止实例化
  }

  // ========== 核心 API ==========

  /**
   * 获取冷却剩余 tick 数
   *
   * @param sword 飞剑实体
   * @param domain 冷却域（如 "attack", "block_break"）
   * @return 剩余 tick 数，无主人或无冷却时返回 0
   */
  public static int get(FlyingSwordEntity sword, String domain) {
    if (sword == null || domain == null) {
      return 0;
    }

    LivingEntity owner = sword.getOwner();
    if (owner == null) {
      return 0;
    }

    String key = makeKey(sword.getUUID(), domain);
    MultiCooldown cooldown = getMultiCooldown(owner);
    if (cooldown == null) {
      return 0;
    }

    MultiCooldown.EntryInt entry = cooldown.getInt(key);
    if (entry == null) {
      return 0;
    }

    return Math.max(0, entry.get());
  }

  /**
   * 设置冷却 tick 数
   *
   * <p>自动 clamp 到 [0, ticks]，防止负冷却
   *
   * @param sword 飞剑实体
   * @param domain 冷却域
   * @param ticks 冷却 tick 数（会自动 clamp 到 ≥0）
   * @return 是否成功设置（无主人时返回 false）
   */
  public static boolean set(FlyingSwordEntity sword, String domain, int ticks) {
    if (sword == null || domain == null) {
      return false;
    }

    LivingEntity owner = sword.getOwner();
    if (owner == null) {
      return false;
    }

    String key = makeKey(sword.getUUID(), domain);
    MultiCooldown cooldown = getOrCreateMultiCooldown(owner);
    if (cooldown == null) {
      return false;
    }

    MultiCooldown.EntryInt entry = cooldown.getOrCreateInt(key);
    if (entry == null) {
      return false;
    }

    // 防止负冷却
    entry.set(Math.max(0, ticks));
    return true;
  }

  /**
   * 检查冷却是否就绪（冷却 ≤ 0）
   *
   * @param sword 飞剑实体
   * @param domain 冷却域
   * @return 冷却就绪返回 true；无主人或冷却未到返回 false
   */
  public static boolean isReady(FlyingSwordEntity sword, String domain) {
    return get(sword, domain) <= 0;
  }

  /**
   * 尝试触发冷却（如果就绪，则设置冷却）
   *
   * <p>等价于：
   * <pre>
   * if (isReady(sword, domain)) {
   *   set(sword, domain, ticks);
   *   return true;
   * }
   * return false;
   * </pre>
   *
   * @param sword 飞剑实体
   * @param domain 冷却域
   * @param ticks 冷却 tick 数
   * @return 是否成功触发冷却
   */
  public static boolean tryStart(FlyingSwordEntity sword, String domain, int ticks) {
    if (!isReady(sword, domain)) {
      return false;
    }
    return set(sword, domain, ticks);
  }

  /**
   * 冷却递减（每 tick 调用）
   *
   * <p>Phase 4: 由系统（CombatSystem/BlockBreakSystem）每 tick 调用
   *
   * @param sword 飞剑实体
   * @param domain 冷却域
   * @return 递减后的剩余 tick 数
   */
  public static int tickDown(FlyingSwordEntity sword, String domain) {
    if (sword == null || domain == null) {
      return 0;
    }

    LivingEntity owner = sword.getOwner();
    if (owner == null) {
      return 0;
    }

    String key = makeKey(sword.getUUID(), domain);
    MultiCooldown cooldown = getMultiCooldown(owner);
    if (cooldown == null) {
      return 0;
    }

    MultiCooldown.EntryInt entry = cooldown.getInt(key);
    if (entry == null) {
      return 0;
    }

    entry.tickDown();
    return Math.max(0, entry.get());
  }

  // ========== 辅助方法 ==========

  /**
   * 构造冷却 Key
   *
   * @param swordUuid 飞剑 UUID
   * @param domain 冷却域
   * @return 格式化的 Key: cc:flying_sword/&lt;uuid&gt;/&lt;domain&gt;
   */
  private static String makeKey(UUID swordUuid, String domain) {
    return KEY_PREFIX + swordUuid.toString() + "/" + domain;
  }

  /**
   * 获取 owner 的 MultiCooldown 附件
   *
   * @param owner 主人
   * @return MultiCooldown 实例，无附件时返回 null
   */
  private static MultiCooldown getMultiCooldown(LivingEntity owner) {
    if (!(owner instanceof ChestCavityEntity cce)) {
      return null;
    }

    var cc = cce.getChestCavityInstance();
    if (cc == null) {
      return null;
    }

    return cc.getAttachment(MultiCooldown.class);
  }

  /**
   * 获取或创建 owner 的 MultiCooldown 附件
   *
   * @param owner 主人
   * @return MultiCooldown 实例，创建失败时返回 null
   */
  private static MultiCooldown getOrCreateMultiCooldown(LivingEntity owner) {
    if (!(owner instanceof ChestCavityEntity cce)) {
      return null;
    }

    var cc = cce.getChestCavityInstance();
    if (cc == null) {
      return null;
    }

    MultiCooldown existing = cc.getAttachment(MultiCooldown.class);
    if (existing != null) {
      return existing;
    }

    // Phase 4: 创建新的 MultiCooldown 附件
    MultiCooldown newCooldown = MultiCooldown.build().finish();
    cc.setAttachment(newCooldown);
    return newCooldown;
  }

  // ========== 便捷方法（常用域）==========

  /**
   * 获取攻击冷却剩余 tick
   */
  public static int getAttackCooldown(FlyingSwordEntity sword) {
    return get(sword, DOMAIN_ATTACK);
  }

  /**
   * 设置攻击冷却 tick
   */
  public static boolean setAttackCooldown(FlyingSwordEntity sword, int ticks) {
    return set(sword, DOMAIN_ATTACK, ticks);
  }

  /**
   * 检查攻击冷却是否就绪
   */
  public static boolean isAttackReady(FlyingSwordEntity sword) {
    return isReady(sword, DOMAIN_ATTACK);
  }

  /**
   * 攻击冷却递减
   */
  public static int tickDownAttackCooldown(FlyingSwordEntity sword) {
    return tickDown(sword, DOMAIN_ATTACK);
  }

  /**
   * 获取破块冷却剩余 tick
   */
  public static int getBlockBreakCooldown(FlyingSwordEntity sword) {
    return get(sword, DOMAIN_BLOCK_BREAK);
  }

  /**
   * 设置破块冷却 tick
   */
  public static boolean setBlockBreakCooldown(FlyingSwordEntity sword, int ticks) {
    return set(sword, DOMAIN_BLOCK_BREAK, ticks);
  }

  /**
   * 检查破块冷却是否就绪
   */
  public static boolean isBlockBreakReady(FlyingSwordEntity sword) {
    return isReady(sword, DOMAIN_BLOCK_BREAK);
  }

  /**
   * 破块冷却递减
   */
  public static int tickDownBlockBreakCooldown(FlyingSwordEntity sword) {
    return tickDown(sword, DOMAIN_BLOCK_BREAK);
  }
}
