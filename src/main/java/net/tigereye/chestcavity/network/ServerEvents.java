package net.tigereye.chestcavity.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.interfaces.ChestCavityEntity;
import net.tigereye.chestcavity.registration.CCAttachments;
import net.tigereye.chestcavity.guscript.data.GuScriptAttachment;
import net.tigereye.chestcavity.util.ChestCavityUtil;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.tigereye.chestcavity.util.ScoreboardUpgradeManager;

import java.util.List;

public final class ServerEvents {

    private static final int MAX_SYNC_RETRIES = 10;

    private ServerEvents() {}

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            scheduleSync(player, true);
        }
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            scheduleSync(player, false);
        }
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer clone)) {
            return;
        }
        ChestCavityInstance original = CCAttachments.getExistingChestCavity(event.getOriginal()).orElse(null);
        if (original == null) {
            return;
        }
        ChestCavityInstance replacement = CCAttachments.getChestCavity(clone);
        CompoundTag tag = new CompoundTag();
        original.toTag(tag, clone.registryAccess());
        replacement.fromTag(tag, clone, clone.registryAccess());

        CCAttachments.getExistingGuScript(event.getOriginal()).ifPresent(originalAttachment -> {
            GuScriptAttachment replacementAttachment = CCAttachments.getGuScript(clone);
            if (!event.isWasDeath()) {
                replacementAttachment.copyFrom(originalAttachment);
                return;
            }

            boolean keepInventory = clone.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
            if (keepInventory) {
                replacementAttachment.copyFrom(originalAttachment);
            } else {
                dropGuScriptNotebook(event.getOriginal(), originalAttachment);
            }
        });
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            scheduleSync(player, false);
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        ChestCavityEntity.of(event.getEntity()).ifPresent(ChestCavityUtil::onDeath);
    }

    private static void dropGuScriptNotebook(Player original, GuScriptAttachment attachment) {
        if (!(original.level() instanceof ServerLevel level)) {
            return;
        }
        List<ItemStack> drops = attachment.drainAllItems();
        if (drops.isEmpty()) {
            return;
        }
        Vec3 pos = original.position();
        for (ItemStack stack : drops) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemEntity drop = new ItemEntity(level, pos.x, pos.y + 0.5, pos.z, stack);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
    }

    private static void scheduleSync(ServerPlayer player, boolean includeOrganData) {
        scheduleSync(player, includeOrganData, 0);
    }

    private static void scheduleSync(ServerPlayer player, boolean includeOrganData, int attempt) {
        player.server.execute(() -> {
            if (player.isRemoved()) {
                return;
            }
            if (player.connection == null) {
                if (attempt < MAX_SYNC_RETRIES) {
                    scheduleSync(player, includeOrganData, attempt + 1);
                }
                return;
            }

            ChestCavityInstance cc = CCAttachments.getChestCavity(player);
            cc.refreshType();
            if (isChestCavityEmpty(cc)) {
                ChestCavityUtil.insertWelfareOrgans(cc);
                ChestCavityUtil.evaluateChestCavity(cc);
            }
            ScoreboardUpgradeManager.applyAll(player, cc);

            if (includeOrganData) {
                NetworkHandler.sendOrganData(player);
            }
            NetworkUtil.SendS2CChestCavityUpdatePacket(cc);
        });
    }

    private static boolean isChestCavityEmpty(ChestCavityInstance cc) {
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            if (!cc.inventory.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
