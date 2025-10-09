package net.tigereye.chestcavity.soul.combat.handlers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.soul.combat.AttackContext;
import net.tigereye.chestcavity.soul.combat.SoulAttackHandler;
import net.tigereye.chestcavity.soul.fakeplayer.SoulPlayer;
import net.tigereye.chestcavity.soul.util.SoulLog;

/**
 * Opportunistically triggers Guzhenren-compatible attack abilities when a SoulPlayer attacks.
 *
 * This handler does not consume the attack; it returns false so downstream handlers (e.g., melee)
 * still execute. Each ability enforces its own cooldown/resource gates.
 */
public final class GuzhenrenAttackAbilityHandler implements SoulAttackHandler {

    // Curated list of Guzhenren active attack abilities to attempt.
    // Referencing the constants ensures their classes are loaded and registered.
    private static final ResourceLocation[] ABILITY_IDS = new ResourceLocation[] {
            // 血道
            net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XieFeiguOrganBehavior.ABILITY_ID,
            net.tigereye.chestcavity.compat.guzhenren.item.xue_dao.behavior.XiediguOrganBehavior.ABILITY_ID,
            // 剑道（分身）
            net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.JianYingGuOrganBehavior.ABILITY_ID,
            // 木道（镰刀波）
            net.tigereye.chestcavity.compat.guzhenren.item.mu_dao.behavior.LiandaoGuOrganBehavior.ABILITY_ID,
            // 炎道（火衣蛊火域）
            net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior.HuoYiGuOrganBehavior.ABILITY_ID,
            // 石道（酒气吐息等主动）
            net.tigereye.chestcavity.compat.guzhenren.item.shi_dao.behavior.JiuChongOrganBehavior.ABILITY_ID,
            // 蛊道（螺旋骨枪、肋骨盾）
            net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.LuoXuanGuQiangguOrganBehavior.ABILITY_ID,
            net.tigereye.chestcavity.compat.guzhenren.item.gu_dao.behavior.LeGuDunGuOrganBehavior.ABILITY_ID,
            // 土道（土墙蛊土牢）
            net.tigereye.chestcavity.compat.guzhenren.item.tu_dao.behavior.TuQiangGuOrganBehavior.ABILITY_ID,
            // 冰雪道（冰棘/霜息）
            net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior.BingJiGuOrganBehavior.ABILITY_ID,
            net.tigereye.chestcavity.compat.guzhenren.item.bing_xue_dao.behavior.ShuangXiGuOrganBehavior.ABILITY_ID,
            // 魂道（鬼气涌动）
            net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior.GuiQiGuOrganBehavior.ABILITY_ID
    };

    private final double range;

    public GuzhenrenAttackAbilityHandler() {
        this(6.0); // Slightly longer than melee to allow short-range actives
    }

    public GuzhenrenAttackAbilityHandler(double range) {
        this.range = Math.max(0.0, range);
    }

    @Override
    public double getRange(SoulPlayer self, LivingEntity target) {
        // Abilities typically use self-facing or small AoE; allow a modest envelope.
        return range + (target.getBbWidth() * 0.25);
    }

    @Override
    public boolean tryAttack(AttackContext ctx) {
        var self = ctx.self();
        if (self.level().isClientSide()) return false; // server only

        ChestCavityInstance cc = CCAttachments.getExistingChestCavity(self).orElse(null);
        if (cc == null) return false;

        int attempts = 0;
        for (ResourceLocation id : ABILITY_IDS) {
            try {
                boolean known = OrganActivationListeners.activate(id, cc);
                if (known) {
                    attempts++;
                }
            } catch (Throwable t) {
                SoulLog.error("[soul][attack] guzhenren ability error id={}", t, id);
            }
        }
        if (attempts > 0) {
            SoulLog.info("[soul][attack] guzhenren-abilities soul={} tried={} target={}",
                    self.getSoulId(), attempts, ctx.target().getUUID());
        }
        // Do not consume; allow melee/ranged handlers to proceed.
        return false;
    }
}

