package net.tigereye.chestcavity.skill.effects.builtin;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.AttributeOps;
import net.tigereye.chestcavity.skill.effects.AppliedHandle;
import net.tigereye.chestcavity.skill.effects.Effect;
import net.tigereye.chestcavity.skill.effects.EffectContext;

/** 临时属性修饰效果：以固定 UUID 安装 transient modifier，TTL 到点或失败回滚时移除。 */
public final class AttributeEffect implements Effect {

  private final Holder<Attribute> attribute;
  private final double amount;
  private final AttributeModifier.Operation op;
  private final String key;
  private final int ttlTicks;

  public AttributeEffect(
      Holder<Attribute> attribute, double amount, AttributeModifier.Operation op, String key, int ttlTicks) {
    this.attribute = attribute;
    this.amount = amount;
    this.op = op == null ? AttributeModifier.Operation.ADD_VALUE : op;
    this.key = key == null ? "attr" : key;
    this.ttlTicks = Math.max(0, ttlTicks);
  }

  @Override
  public AppliedHandle applyPre(EffectContext ctx) {
    AttributeInstance inst = ctx.player().getAttribute(attribute);
    if (inst == null) return null;
    ResourceLocation id = ChestCavity.id("skill_effect/attr/" + key);
    AttributeModifier mod = new AttributeModifier(id, amount, op);
    AttributeOps.replaceTransient(inst, id, mod);
    return new AppliedHandle() {
      @Override
      public int ttlTicks() {
        return ttlTicks;
      }

      @Override
      public String debugName() {
        return "Attribute:" + key;
      }

      @Override
      public void revert() {
        AttributeInstance i = ctx.player().getAttribute(attribute);
        if (i != null) {
          AttributeOps.removeById(i, id);
        }
      }
    };
  }
}
