package net.tigereye.chestcavity.compat.common.skillcalc;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.common.agent.Agent;
import net.tigereye.chestcavity.compat.common.agent.Agents;

/** 伤害计算上下文（只读）。 */
public final class DamageComputeContext {

  private final Agent attacker;
  private final Agent defender; // 可为 null
  private final double baseDamage;
  private final ResourceLocation skillId; // 可为 null
  private final long castId; // 0 表示未知
  private final EnumSet<DamageKind> kinds;

  private DamageComputeContext(
      Agent attacker,
      Agent defender,
      double baseDamage,
      ResourceLocation skillId,
      long castId,
      EnumSet<DamageKind> kinds) {
    this.attacker = Objects.requireNonNull(attacker, "attacker");
    this.defender = defender;
    this.baseDamage = baseDamage;
    this.skillId = skillId;
    this.castId = castId;
    this.kinds = kinds == null ? EnumSet.noneOf(DamageKind.class) : kinds.clone();
  }

  public Agent attacker() { return attacker; }
  public Agent defender() { return defender; }
  public double baseDamage() { return baseDamage; }
  public ResourceLocation skillId() { return skillId; }
  public long castId() { return castId; }
  public Set<DamageKind> kinds() { return Collections.unmodifiableSet(kinds); }

  public boolean kind(DamageKind k) { return kinds.contains(k); }

  public static Builder builder(LivingEntity attacker, double baseDamage) {
    return new Builder(attacker, baseDamage);
  }

  public static final class Builder {
    private final Agent attacker;
    private Agent defender;
    private double baseDamage;
    private ResourceLocation skillId;
    private long castId;
    private final EnumSet<DamageKind> kinds = EnumSet.noneOf(DamageKind.class);

    private Builder(LivingEntity attacker, double baseDamage) {
      this.attacker = Agents.of(attacker);
      this.baseDamage = baseDamage;
    }

    public Builder defender(LivingEntity defender) {
      this.defender = defender == null ? null : Agents.of(defender);
      return this;
    }

    public Builder skill(ResourceLocation id) { this.skillId = id; return this; }
    public Builder cast(long castId) { this.castId = castId; return this; }
    public Builder addKind(DamageKind k) { if (k != null) kinds.add(k); return this; }

    public DamageComputeContext build() {
      return new DamageComputeContext(attacker, defender, baseDamage, skillId, castId, kinds);
    }
  }
}

