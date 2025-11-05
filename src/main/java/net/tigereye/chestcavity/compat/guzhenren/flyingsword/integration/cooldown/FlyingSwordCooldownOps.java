package net.tigereye.chestcavity.compat.guzhenren.flyingsword.integration.cooldown;

import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.state.FlyingSwordCooldownAttachment;
import net.tigereye.chestcavity.registration.CCAttachments;

/**
 * Phase 4：飞剑冷却统一入口（Attachment 实现）
 *
 * <p>将冷却存储在 owner 的 Attachment 中，按 Key（uuid/domain）存取。
 */
public final class FlyingSwordCooldownOps {

  private static final String KEY_PREFIX = "cc:flying_sword/";
  public static final String DOMAIN_ATTACK = "attack";
  public static final String DOMAIN_BLOCK_BREAK = "block_break";

  private FlyingSwordCooldownOps() {}

  // ========== 攻击冷却 ==========

  public static int getAttackCooldown(FlyingSwordEntity sword) {
    return get(sword, DOMAIN_ATTACK);
  }

  public static boolean setAttackCooldown(FlyingSwordEntity sword, int ticks) {
    return set(sword, DOMAIN_ATTACK, ticks);
  }

  public static boolean isAttackReady(FlyingSwordEntity sword) {
    return getAttackCooldown(sword) <= 0;
  }

  public static int tickDownAttackCooldown(FlyingSwordEntity sword) {
    return tickDown(sword, DOMAIN_ATTACK);
  }

  // ========== 通用实现 ==========

  public static int get(FlyingSwordEntity sword, String domain) {
    FlyingSwordCooldownAttachment att = getAttachment(sword);
    if (att == null) return 0;
    return att.get(makeKey(sword.getUUID(), domain));
  }

  public static boolean set(FlyingSwordEntity sword, String domain, int ticks) {
    FlyingSwordCooldownAttachment att = getAttachment(sword);
    if (att == null) return false;
    att.set(makeKey(sword.getUUID(), domain), Math.max(0, ticks));
    return true;
  }

  public static int tickDown(FlyingSwordEntity sword, String domain) {
    FlyingSwordCooldownAttachment att = getAttachment(sword);
    if (att == null) return 0;
    return att.tickDown(makeKey(sword.getUUID(), domain));
  }

  private static String makeKey(UUID swordUuid, String domain) {
    return KEY_PREFIX + swordUuid.toString() + "/" + domain;
  }

  private static FlyingSwordCooldownAttachment getAttachment(FlyingSwordEntity sword) {
    if (sword == null) return null;
    LivingEntity owner = sword.getOwner();
    if (owner == null) return null;
    return owner.getData(CCAttachments.FLYING_SWORD_COOLDOWN.get());
  }
}
