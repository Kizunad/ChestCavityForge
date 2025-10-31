package net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.calculator.BingXueDaohenOps;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.skill.effects.AppliedHandle;
import net.tigereye.chestcavity.skill.effects.Effect;
import net.tigereye.chestcavity.skill.effects.EffectContext;
import net.tigereye.chestcavity.skill.effects.SkillEffectBus;

public class ComputedBingXueDaohenEffect implements Effect {

    public static final String DAO_HEN_KEY = "daohen";

    @Override
    public AppliedHandle applyPre(EffectContext ctx) {
        if (ctx.chestCavity() != null) {
            double daohen = BingXueDaohenOps.compute(ctx.chestCavity());
            SkillEffectBus.putMetadata(ctx, DAO_HEN_KEY, daohen);
        }
        // This effect is purely informational, no handle needed for rollback.
        return null;
    }
}
