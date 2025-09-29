package net.tigereye.chestcavity.guscript.actions;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guscript.runtime.exec.GuScriptContext;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guscript.ast.Action;

/**
 * Deals mental (indirect magic) damage to the current GuScript target if it is within range.
 */
public record MindShockAction(double damage, double range) implements Action {

    public static final String ID = "mind.shock";
    private static final ResourceLocation ZHI_DAO_INCREASE_EFFECT =
            ResourceLocation.fromNamespaceAndPath("guzhenren", "linkage/zhi_dao_increase_effect");

    public MindShockAction {
        double sanitizedDamage = Double.isNaN(damage) ? 0.0D : damage;
        if (sanitizedDamage < 0.0D) {
            sanitizedDamage = 0.0D;
        }
        double sanitizedRange = Double.isNaN(range) ? 16.0D : range;
        if (sanitizedRange <= 0.0D) {
            sanitizedRange = 16.0D;
        }
        damage = sanitizedDamage;
        range = sanitizedRange;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "心灵冲击 " + damage + " (range=" + range + ")";
    }

    @Override
    public void execute(GuScriptContext context) {
        Player performer = context.performer();
        if (performer == null || damage <= 0.0D) {
            return;
        }
        LivingEntity target = context.target();
        if (target == null || target == performer || !target.isAlive()) {
            return;
        }
        double maxDistanceSq = range * range;
        if (!Double.isInfinite(maxDistanceSq) && performer.distanceToSqr(target) > maxDistanceSq) {
            return;
        }
        double scaledDamage = damage * (1.0 + readZhiIncrease(performer));
        double resolvedDamage = context.applyDamageModifiers(scaledDamage);
        if (resolvedDamage <= 0.0D) {
            return;
        }
        DamageSource source = performer.damageSources().indirectMagic(performer, performer);
        target.invulnerableTime = 0;
        target.hurt(source, (float) resolvedDamage);
        target.invulnerableTime = 0;
    }

    private static double readZhiIncrease(Player player) {
        if (player == null) {
            return 0.0D;
        }
        ChestCavityInstance cc = CCAttachments.getChestCavity(player);
        if (cc == null) {
            return 0.0D;
        }
        ActiveLinkageContext linkage = LinkageManager.getContext(cc);
        LinkageChannel channel = linkage.getOrCreateChannel(ZHI_DAO_INCREASE_EFFECT);
        double value = channel.get();
        if (value <= 0.0D) {
            return 0.0D;
        }
        ChestCavity.LOGGER.debug("[GuScript][Damage] mind.shock using zhi increase {} for {}", value, player.getGameProfile().getName());
        return value;
    }
}
