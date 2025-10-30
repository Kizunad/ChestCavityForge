package net.tigereye.chestcavity.skill.effects.builtin;

import net.minecraft.resources.ResourceLocation;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.skill.effects.AppliedHandle;
import net.tigereye.chestcavity.skill.effects.Effect;
import net.tigereye.chestcavity.skill.effects.EffectContext;
import net.tigereye.chestcavity.util.AbsorptionHelper;

/**
 * 吸收护盾窗口：在 pre 阶段将护盾设置为 max(当前, 当前+amount)，post 成功后在 TTL 到点回退至初始值；失败则立刻回退。
 */
public final class AbsorptionEffect implements Effect {

  private final double amount;
  private final int ttlTicks;
  private final String key;

  public AbsorptionEffect(double amount, int ttlTicks, String key) {
    this.amount = Math.max(0.0, amount);
    this.ttlTicks = Math.max(0, ttlTicks);
    this.key = key == null ? "absorb" : key;
  }

  @Override
  public AppliedHandle applyPre(EffectContext ctx) {
    float before = ctx.player().getAbsorptionAmount();
    double desired = Math.max(before, before + amount);
    ResourceLocation modId = ChestCavity.id("skill_effect/" + key);
    AbsorptionHelper.applyAbsorption(ctx.player(), desired, modId, true);
    return new AppliedHandle() {
      @Override
      public int ttlTicks() {
        return ttlTicks;
      }

      @Override
      public String debugName() {
        return "Absorption:" + key;
      }

      @Override
      public void revert() {
        // 回退至 pre 前的吸收值，并清理容量修饰符
        AbsorptionHelper.applyAbsorption(ctx.player(), before, modId, false);
        if (before <= 0.0f) {
          AbsorptionHelper.clearAbsorptionCapacity(ctx.player(), modId);
        }
      }
    };
  }
}

