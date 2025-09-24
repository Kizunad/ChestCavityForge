package net.tigereye.chestcavity.util;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.tigereye.chestcavity.ChestCavity;

/**
 * Provides a safe entry point for manipulating tick behaviour while running
 * the game in singleplayer (integrated server) mode.
 * <p>
 * The helper automatically checks whether a local integrated server is
 * available before attempting to touch tick state, allowing callers to invoke
 * it from common code without worrying about dedicated servers.
 * Supported operations currently include:
 * <ul>
 *     <li>Changing the global tick rate via the integrated server's
 *     {@code tickRateManager}.</li>
 *     <li>Toggling the pause state so that all logic updates are frozen or
 *     resumed.</li>
 * </ul>
 */
public final class SingleplayerTickController {

    private static final float MIN_TICK_RATE = 0.05F;
    private static final SingleplayerTickHandler HANDLER = initializeHandler();

    private SingleplayerTickController() {
    }

    /**
     * Applies the requested tick adjustments if a singleplayer integrated
     * server is currently active.
     *
     * @param targetTickRate Optional tick rate override. {@code null} leaves
     *                       the current tick rate untouched.
     * @param pauseState     Optional pause flag. {@code null} leaves the pause
     *                       state untouched.
     * @return {@code true} if a singleplayer server was found and at least one
     * operation was carried out, otherwise {@code false}.
     */
    public static boolean apply(Float targetTickRate, Boolean pauseState) {
        return HANDLER.apply(targetTickRate, pauseState);
    }

    /**
     * Convenience wrapper around {@link #apply(Float, Boolean)} that only
     * adjusts the tick rate.
     */
    public static boolean setTickRate(float targetTickRate) {
        return apply(targetTickRate, null);
    }

    /**
     * Convenience wrapper around {@link #apply(Float, Boolean)} that only
     * adjusts the pause state.
     */
    public static boolean setPaused(boolean paused) {
        return apply(null, paused);
    }

    private static SingleplayerTickHandler initializeHandler() {
        if (!FMLEnvironment.dist.isClient()) {
            return SingleplayerTickHandler.NO_OP;
        }

        try {
            Class<?> implClass = Class.forName("net.tigereye.chestcavity.util.SingleplayerTickController$ClientActions");
            return (SingleplayerTickHandler) implClass.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException ignored) {
            return SingleplayerTickHandler.NO_OP;
        } catch (ReflectiveOperationException exception) {
            ChestCavity.LOGGER.error("Failed to initialise singleplayer tick controller", exception);
            return SingleplayerTickHandler.NO_OP;
        }
    }

    private interface SingleplayerTickHandler {
        SingleplayerTickHandler NO_OP = (targetTickRate, pauseState) -> false;

        boolean apply(Float targetTickRate, Boolean pauseState);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientActions implements SingleplayerTickHandler {

        private ClientActions() {
        }

        @Override
        public boolean apply(Float targetTickRate, Boolean pauseState) {
            net.minecraft.client.server.IntegratedServer server = getActiveIntegratedServer();
            if (server == null) {
                return false;
            }

            boolean applied = false;

            if (targetTickRate != null) {
                applied |= setTickRateInternal(server, targetTickRate);
            }

            if (pauseState != null) {
                applied |= setPauseInternal(server, pauseState);
            }

            return applied;
        }

        private static net.minecraft.client.server.IntegratedServer getActiveIntegratedServer() {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft == null || !minecraft.hasSingleplayerServer()) {
                return null;
            }

            return minecraft.getSingleplayerServer();
        }

        private static boolean setTickRateInternal(net.minecraft.client.server.IntegratedServer server, float targetTickRate) {
            if (!Float.isFinite(targetTickRate)) {
                ChestCavity.LOGGER.warn("Ignoring non-finite tick rate value: {}", targetTickRate);
                return false;
            }

            float sanitized = Math.max(MIN_TICK_RATE, targetTickRate);
            if (sanitized != targetTickRate) {
                ChestCavity.LOGGER.debug("Clamped tick rate {} to minimum supported value {}", targetTickRate, sanitized);
            }

            server.tickRateManager().setTickRate(sanitized);
            return true;
        }

        private static boolean setPauseInternal(net.minecraft.client.server.IntegratedServer server, boolean pauseState) {
            boolean frozen = server.tickRateManager().isFrozen();
            if (frozen == pauseState) {
                return false;
            }

            server.tickRateManager().setFrozen(pauseState);
            return true;
        }
    }
}
