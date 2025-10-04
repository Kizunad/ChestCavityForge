package net.tigereye.chestcavity.compat.guzhenren.item.li_dao;

import java.util.Locale;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.registration.CCAttachments;

/**
 * 力道相关的通用工具方法（肌肉统计、增益查询等）。
 */
public final class LiDaoHelper {

    private LiDaoHelper() {}

    private static final String MUSCLE_SUFFIX = "_muscle";
    private static final ResourceLocation HUMAN_MUSCLE = ResourceLocation.fromNamespaceAndPath("chestcavity", "muscle");

    public static double getLiDaoIncrease(LivingEntity entity) {
        if (entity == null) {
            return 0.0;
        }
        return CCAttachments.getExistingChestCavity(entity)
                .map(LiDaoHelper::getLiDaoIncrease)
                .orElse(0.0);
    }

    public static double getLiDaoIncrease(ChestCavityInstance cc) {
        if (cc == null) {
            return 0.0;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        if (context == null) {
            return 0.0;
        }
        LinkageChannel channel = context.lookupChannel(LiDaoConstants.LI_DAO_INCREASE_EFFECT)
                .orElseGet(() -> context.getOrCreateChannel(LiDaoConstants.LI_DAO_INCREASE_EFFECT));
        return channel == null ? 0.0 : Math.max(0.0, channel.get());
    }

    public static int countMuscleStacks(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return 0;
        }
        int size = cc.inventory.getContainerSize();
        int total = 0;
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (isMuscle(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static boolean isMuscle(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return false;
        }
        if (!"chestcavity".equals(id.getNamespace())) {
            return false;
        }
        if (HUMAN_MUSCLE.equals(id)) {
            return true;
        }
        String path = id.getPath();
        return path != null && path.endsWith(MUSCLE_SUFFIX);
    }

    public static String describeMuscleContribution(double base, double liDaoIncrease) {
        return String.format(Locale.ROOT, "%.2f (base) * (1 + %.3f)", base, Math.max(0.0, liDaoIncrease));
    }
}
