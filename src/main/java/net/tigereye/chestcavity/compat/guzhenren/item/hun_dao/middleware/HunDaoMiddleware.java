package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.middleware;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.util.DoTManager;
import net.tigereye.chestcavity.util.DoTManager.FxAnchor;
import net.tigereye.chestcavity.compat.guzhenren.util.SaturationHelper;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.registration.CCSoundEvents;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.Optional;

/**
 * 魂道行为与底层系统（DoT、资源）的中间件桥梁。
 * - 负责调度持续伤害（DoT）效果；
 * - 负责魂魄资源与饱食维护；
 * - 预留玩家/非玩家处理入口（占位符）。
 */
public final class HunDaoMiddleware {

    public static final HunDaoMiddleware INSTANCE = new HunDaoMiddleware();

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation SOUL_FLAME_FX = ChestCavity.id("soulbeast_dot_tick");
    private static final SoundEvent SOUL_FLAME_SOUND = CCSoundEvents.CUSTOM_SOULBEAST_DOT.get();

    private HunDaoMiddleware() {}

    public void applySoulFlame(Player source, LivingEntity target, double perSecondDamage, int seconds) {
        if (source == null || target == null || !target.isAlive()) {
            return;
        }
        if (perSecondDamage <= 0 || seconds <= 0) {
            return;
        }
        DoTManager.schedulePerSecond(
                source,
                target,
                perSecondDamage,
                seconds,
                SOUL_FLAME_SOUND,
                0.6f,
                1.0f,
                net.tigereye.chestcavity.util.DoTTypes.HUN_DAO_SOUL_FLAME,
                SOUL_FLAME_FX,
                FxAnchor.TARGET,
                Vec3.ZERO,
                0.7f
        );
        LOGGER.debug("[hun_dao][middleware] DoT={}s @{} -> {}", seconds, format(perSecondDamage), target.getName().getString());
    }

    public void leakHunpoPerSecond(Player player, double amount) {
        if (player == null || amount <= 0.0D) {
            return;
        }
        adjustHunpo(player, -amount, "leak");
        SaturationHelper.gentlyTopOff(player, 18, 0.5f);
    }

    public boolean consumeHunpo(Player player, double amount) {
        if (player == null || amount <= 0.0D) {
            return false;
        }
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return false;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
        double current = handle.read("hunpo").orElse(0.0D);
        if (current < amount) {
            return false;
        }
        ResourceOps.tryAdjustDouble(handle, "hunpo", -amount, true, "zuida_hunpo");
        return true;
    }

    public void handlerPlayer(Player player) {
        SaturationHelper.gentlyTopOff(player, 18, 0.5f);
    }

    public void handlerNonPlayer(LivingEntity entity) {
        // placeholder for non-player upkeep paths
    }

    private void adjustHunpo(Player player, double amount, String reason) {
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
        ResourceOps.tryAdjustDouble(handle, "hunpo", amount, true, "zuida_hunpo");
        LOGGER.trace("[hun_dao][middleware] adjusted {} hunpo for {} ({})", format(amount), player.getScoreboardName(), reason);
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
