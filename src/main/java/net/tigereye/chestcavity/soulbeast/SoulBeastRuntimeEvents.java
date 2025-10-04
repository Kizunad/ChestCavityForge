package net.tigereye.chestcavity.soulbeast;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.middleware.HunDaoMiddleware;
import net.tigereye.chestcavity.soulbeast.state.SoulBeastStateManager;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.registration.CCAttachments;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Global runtime hooks for Soul Beast mechanics.
 * Performance: every handler returns early if the actor is not a soul beast.
 */
@EventBusSubscriber(modid = ChestCavity.MODID)
public final class SoulBeastRuntimeEvents {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double ON_HIT_HUNPO_COST = 18.0;
    private static final double SOUL_FLAME_PERCENT = 0.01; // true damage per second = maxHunpo * percent
    private static final int SOUL_FLAME_SECONDS = 5;
    private static final ResourceLocation HUN_DAO_INCREASE_EFFECT = ResourceLocation.fromNamespaceAndPath("guzhenren", "linkage/hun_dao_increase_effect");

    private SoulBeastRuntimeEvents() {}

    @SubscribeEvent
    public static void onMeleeHit(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity attacker) || attacker.level().isClientSide()) {
            return;
        }
        if (!SoulBeastStateManager.isActive(attacker)) {
            return;
        }
        Entity targetEntity = event.getTarget();
        if (!(targetEntity instanceof LivingEntity target) || !target.isAlive()) {
            return;
        }
        // Strictly consume hunpo first (no HP fallback)
        var res = GuzhenrenResourceCostHelper.consumeStrict(attacker, ON_HIT_HUNPO_COST);
        if (!res.succeeded()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[soulbeast] blocked melee DoT: insufficient hunpo (cost={} reason={})", ON_HIT_HUNPO_COST, res.failureReason());
            }
            return;
        }
        double dot = computeSoulFlameDps(attacker);
        if (dot > 0.0 && attacker instanceof Player player) {
            HunDaoMiddleware.INSTANCE.applySoulFlame(player, target, dot, SOUL_FLAME_SECONDS);
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile().getOwner() instanceof LivingEntity owner) || event.getProjectile().level().isClientSide()) {
            return;
        }
        if (!SoulBeastStateManager.isActive(owner)) {
            return;
        }
        if (!(event.getRayTraceResult() instanceof EntityHitResult ehr)) {
            return;
        }
        Entity hit = ehr.getEntity();
        if (!(hit instanceof LivingEntity target) || !target.isAlive()) {
            return;
        }
        var res = GuzhenrenResourceCostHelper.consumeStrict(owner, ON_HIT_HUNPO_COST);
        if (!res.succeeded()) {
            return;
        }
        double dot = computeSoulFlameDps(owner);
        if (dot > 0.0 && owner instanceof Player player) {
            HunDaoMiddleware.INSTANCE.applySoulFlame(player, target, dot, SOUL_FLAME_SECONDS);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long current = event.getServer().getTickCount();
        for (Player player : event.getServer().getPlayerList().getPlayers()) {
            if (!SoulBeastStateManager.isActive(player)) {
                continue; // fast path when not a soul beast
            }
            // once per second: leak hunpo and keep saturation topped off
            if ((current % 20L) == 0L) {
                HunDaoMiddleware.INSTANCE.leakHunpoPerSecond(player, 3.0);
                HunDaoMiddleware.INSTANCE.handlerPlayer(player);
            }
        }
    }

    private static double computeSoulFlameDps(LivingEntity attacker) {
        double maxHunpo = 0.0;
        if (attacker instanceof Player player) {
            Optional<GuzhenrenResourceBridge.ResourceHandle> handle = GuzhenrenResourceBridge.open(player);
            if (handle.isPresent()) {
                maxHunpo = handle.get().read("zuida_hunpo").orElse(0.0);
            }
        }
        final double[] eff = {1.0};
        CCAttachments.getExistingChestCavity(attacker).ifPresent(cc -> {
            ActiveLinkageContext ctx = LinkageManager.getContext(cc);
            if (ctx != null) {
                LinkageChannel ch = ctx.getOrCreateChannel(HUN_DAO_INCREASE_EFFECT);
                eff[0] += Math.max(0.0, ch.get());
            }
        });
        return Math.max(0.0, maxHunpo * SOUL_FLAME_PERCENT * eff[0]);
    }
}
