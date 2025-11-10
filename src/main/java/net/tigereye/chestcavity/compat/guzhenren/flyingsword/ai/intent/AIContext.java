package net.tigereye.chestcavity.compat.guzhenren.flyingsword.ai.intent;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.compat.guzhenren.flyingsword.FlyingSwordEntity;

/** Intent 评估上下文（仅承载只读信息，便于单元测试与复用）。 */
public record AIContext(
    FlyingSwordEntity sword, LivingEntity owner, ServerLevel level, long gameTime) {}
