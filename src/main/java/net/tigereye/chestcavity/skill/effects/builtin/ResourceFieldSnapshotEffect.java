package net.tigereye.chestcavity.skill.effects.builtin;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge.ResourceHandle;
import net.tigereye.chestcavity.skill.effects.AppliedHandle;
import net.tigereye.chestcavity.skill.effects.Effect;
import net.tigereye.chestcavity.skill.effects.EffectContext;
import net.tigereye.chestcavity.skill.effects.SkillEffectBus;

/**
 * 预先读取 Guzhenren 资源字段并缓存到 SkillEffectBus metadata，供技能行为在激活阶段读取。
 */
public final class ResourceFieldSnapshotEffect implements Effect {

  private final String prefix;
  private final List<String> fields;

  public ResourceFieldSnapshotEffect(String prefix, List<String> fields) {
    this.prefix = prefix == null ? "" : prefix;
    this.fields = fields == null ? List.of() : List.copyOf(fields);
  }

  @Override
  public AppliedHandle applyPre(EffectContext ctx) {
    if (fields.isEmpty()) return null;
    if (!(ctx.player() instanceof ServerPlayer player)) {
      return null;
    }
    ResourceHandle handle =
        GuzhenrenResourceBridge.open(player).orElse(null);
    if (handle == null) {
      return null;
    }
    for (String field : fields) {
      if (field == null || field.isBlank()) {
        continue;
      }
      double value = handle.read(field).orElse(0.0D);
      if (Double.isFinite(value)) {
        SkillEffectBus.putMetadata(ctx, key(field), value);
      }
    }
    return null;
  }

  private String key(String field) {
    return prefix.isBlank() ? field : prefix + field;
  }
}
