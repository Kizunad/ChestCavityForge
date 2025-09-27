package net.tigereye.chestcavity.compat.guzhenren;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;

import net.tigereye.chestcavity.compat.guzhenren.ability.Abilities;
import net.tigereye.chestcavity.compat.guzhenren.module.GuzhenrenIntegrationModule;

import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.linkage.effect.GuzhenrenLinkageEffectRegistry;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.util.retention.OrganRetentionRules;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Compatibility helpers that inject Guzhenren-specific organ behaviour without direct class dependencies.
 */
public final class GuzhenrenOrganHandlers {

    private static final String MOD_ID = "guzhenren";
    private GuzhenrenOrganHandlers() {
    }

    static {
        OrganRetentionRules.registerNamespace(MOD_ID);
    }

    public static void registerListeners(
            ChestCavityInstance cc,
            ItemStack stack,
            List<OrganRemovalContext> staleRemovalContexts,
            Map<ResourceLocation, Integer> cachedCounts,
            Map<ResourceLocation, List<ItemStack>> cachedStacks
    ) {
        if (stack.isEmpty()) {
            return;
        }
        if (ChestCavity.LOGGER.isDebugEnabled()) {
            ChestCavity.LOGGER.debug(
                    "[Guzhenren] Registering listeners for item {} on cavity {}",
                    BuiltInRegistries.ITEM.getKey(stack.getItem()),
                    describeChestCavity(cc)
            );
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);

        Abilities.bootstrap();
        GuzhenrenIntegrationModule.bootstrap();

        GuzhenrenLinkageEffectRegistry.applyEffects(
                cc,
                stack,
                staleRemovalContexts,
                cachedCounts,
                cachedStacks
        );
        if (stack.is(Items.WOODEN_SHOVEL)) {
            displayChannelSnapshot(cc, context);
        }
    }

    private static String describeChestCavity(ChestCavityInstance cc) {
        if (cc == null || cc.owner == null) {
            return "<unbound>";
        }
        return cc.owner.getScoreboardName();
    }

    private static void displayChannelSnapshot(ChestCavityInstance cc, ActiveLinkageContext context) {
        if (cc == null || context == null) {
            return;
        }
        Map<ResourceLocation, Double> channels = context.snapshotChannels();
        List<Map.Entry<ResourceLocation, Double>> entries = channels.entrySet().stream()
                .filter(entry -> MOD_ID.equals(entry.getKey().getNamespace()))
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .collect(Collectors.toList());
        if (entries.isEmpty()) {
            if (!broadcastToPlayer(cc, "[compat/guzhenren] No active linkage channels.")) {
                if (ChestCavity.LOGGER.isDebugEnabled()) {
                    ChestCavity.LOGGER.debug("[compat/guzhenren] No active linkage channels for {}", describeChestCavity(cc));
                }
            }
            return;
        }
        String header = String.format(Locale.ROOT, "[compat/guzhenren] Channel snapshot (%d):", entries.size());
        String body = entries.stream()
                .map(entry -> String.format(Locale.ROOT, " - %s = %.3f", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n"));
        String message = header + "\n" + body;
        if (!broadcastToPlayer(cc, message)) {
            ChestCavity.LOGGER.info(message.replace('\n', ' '));
        }
    }

    private static boolean broadcastToPlayer(ChestCavityInstance cc, String message) {
        if (!(cc.owner instanceof Player player) || player.level().isClientSide()) {
            return false;
        }

        if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection == null) {
            ChestCavity.LOGGER.debug("[compat/guzhenren] Skipping message for {} because the network connection is not ready", describeChestCavity(cc));
            return false;
        }

        player.sendSystemMessage(Component.literal(message));
        return true;
    }
}
