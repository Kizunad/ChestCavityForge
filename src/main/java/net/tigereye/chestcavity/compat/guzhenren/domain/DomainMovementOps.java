package net.tigereye.chestcavity.compat.guzhenren.domain;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** 领域相关的移动速度操作（应用/移除“冥想减速”）。 */
public final class DomainMovementOps {

  private DomainMovementOps() {}

  private static final ResourceLocation JIANXIN_SLOW_ID =
      net.tigereye.chestcavity.ChestCavity.id("effects/jianxin_domain_slow");
  private static final String JIANXIN_SLOW_NAME = "JianXinDomainSlow";

  /** 应用减速：amount 为衰减比例(0,1]，例如 0.9 => 最终速度乘以 (1-0.9)=0.1。 */
  public static void applyMeditationSlow(Player player, double amount) {
    if (player == null) return;
    AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
    if (attr == null) return;
    // 先移除旧的
    removeMeditationSlow(player);
    double mult = Math.max(0.0, Math.min(1.0, 1.0 - amount));
    double opAmount = mult - 1.0; // ADD_MULTIPLIED_TOTAL 使用 1+amount 的形式
    // 例如 mult=0.1 => opAmount=-0.9
    net.tigereye.chestcavity.compat.common.agent.Agents.applyTransientAttribute(
        player,
        Attributes.MOVEMENT_SPEED,
        JIANXIN_SLOW_ID,
        opAmount,
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
  }

  /** 移除减速。 */
  public static void removeMeditationSlow(Player player) {
    if (player == null) return;
    AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
    if (attr == null) return;
    net.tigereye.chestcavity.compat.guzhenren.util.behavior.AttributeOps.removeById(
        attr, JIANXIN_SLOW_ID);
  }

  /** 每tick保证状态与标签一致。 */
  public static void tickEnsureMeditationSlow(Player player) {
    if (player == null) return;
    AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
    if (attr == null) return;
    double dec = DomainTags.getJianxinVelocityDecreasement(player);
    boolean hasMod = attr.getModifier(JIANXIN_SLOW_ID) != null;
    if (dec > 0.0) {
      if (!hasMod) {
        applyMeditationSlow(player, dec);
      }
    } else if (hasMod) {
      removeMeditationSlow(player);
    }
  }
}
