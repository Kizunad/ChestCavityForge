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

    // Avoid class-loading optional compat behaviours during static init to prevent crashes when mods are absent.
    // Start conservatively with HuoYiGu; more abilities can be appended as string IDs here without touching classes.
    private static final ResourceLocation[] ABILITY_IDS = new ResourceLocation[] {
            // 血道
            ResourceLocation.fromNamespaceAndPath("guzhenren", "xie_fei_gu"),            // 血肺蛊（主动同 organId）
            ResourceLocation.fromNamespaceAndPath("guzhenren", "xie_di_gu_detonate"),   // 血滴蛊引爆
            // 剑道
            ResourceLocation.fromNamespaceAndPath("guzhenren", "jian_ying_fenshen"),    // 剑影分身
            ResourceLocation.fromNamespaceAndPath("guzhenren", "liandaogu"),            // 镰刀蛊（主动同 organId）
            // 炎道
            ResourceLocation.fromNamespaceAndPath("guzhenren", "huo_gu"),               // 火衣蛊（主动同 organId）
            // 石道
            ResourceLocation.fromNamespaceAndPath("guzhenren", "jiu_chong"),            // 酒虫（主动同 organId）
            // 蛊道
            ResourceLocation.fromNamespaceAndPath("guzhenren", "luo_xuan_gu_qiang_gu"), // 螺旋骨枪蛊（主动同 organId）
            ResourceLocation.fromNamespaceAndPath("guzhenren", "le_gu_dun_gu"),         // 肋骨盾蛊（主动同 organId）
            // 土道
            ResourceLocation.fromNamespaceAndPath("guzhenren", "tu_qiang_gu"),          // 土墙蛊（主动同 organId）
            // 冰雪道
            ResourceLocation.fromNamespaceAndPath("guzhenren", "bing_ji_gu_iceburst"),  // 冰棘蛊冰爆
            ResourceLocation.fromNamespaceAndPath("guzhenren", "shuang_xi_gu_frost_breath"), // 霜息蛊吐息
            // 魂道
            ResourceLocation.fromNamespaceAndPath("guzhenren", "gui_wu"),               // 鬼雾（主动）
            ResourceLocation.fromNamespaceAndPath("guzhenren", "yuan_lao_gu_5_attack") // 元老蛊·五转
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
