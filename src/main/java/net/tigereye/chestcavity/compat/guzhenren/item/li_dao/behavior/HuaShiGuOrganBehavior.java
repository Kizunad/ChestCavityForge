package net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.AbstractLiDaoOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/**
 * 花豕蛊（力道·肌肉）：
 * 被动：每 5 秒尝试消耗 200 BASE 真元，为拥有者恢复 3 精力。
 * 主动：消耗 300 BASE 真元，获得 10 秒力量 III。
 */
public final class HuaShiGuOrganBehavior extends AbstractLiDaoOrganBehavior implements OrganSlowTickListener {

    public static final HuaShiGuOrganBehavior INSTANCE = new HuaShiGuOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "hua_shi_gu");
    public static final ResourceLocation ABILITY_ID = ORGAN_ID;

    private static final String STATE_ROOT = "HuaShiGu";
    private static final String KEY_NEXT_PASSIVE_TICK = "NextPassiveTick";

    private static final int PASSIVE_INTERVAL_TICKS = BehaviorConfigAccess.getInt(HuaShiGuOrganBehavior.class, "PASSIVE_INTERVAL_TICKS", 5 * 20);
    private static final double PASSIVE_BASE_ZHENYUAN_COST = BehaviorConfigAccess.getFloat(HuaShiGuOrganBehavior.class, "PASSIVE_BASE_ZHENYUAN_COST", 200.0f);
    private static final double PASSIVE_JINGLI_GAIN = BehaviorConfigAccess.getFloat(HuaShiGuOrganBehavior.class, "PASSIVE_JINGLI_GAIN", 3.0f);

    private static final double ACTIVE_BASE_ZHENYUAN_COST = BehaviorConfigAccess.getFloat(HuaShiGuOrganBehavior.class, "ACTIVE_BASE_ZHENYUAN_COST", 300.0f);
    private static final int ACTIVE_DURATION_TICKS = BehaviorConfigAccess.getInt(HuaShiGuOrganBehavior.class, "ACTIVE_DURATION_TICKS", 10 * 20);
    private static final int ACTIVE_STRENGTH_LEVEL = BehaviorConfigAccess.getInt(HuaShiGuOrganBehavior.class, "ACTIVE_STRENGTH_LEVEL", 3);

    static {
        OrganActivationListeners.register(ABILITY_ID, HuaShiGuOrganBehavior::activateAbility);
    }

    private HuaShiGuOrganBehavior() {}

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || player.level().isClientSide() || cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!isPrimaryOrgan(cc, organ)) {
            return;
        }

        MultiCooldown cooldown = createCooldown(cc, organ);
        MultiCooldown.Entry passive = cooldown.entry(KEY_NEXT_PASSIVE_TICK);
        long now = player.level().getGameTime();
        if (passive.getReadyTick() > now) {
            return;
        }
        passive.setReadyAt(now + PASSIVE_INTERVAL_TICKS);

        if (ResourceOps.tryConsumeScaledZhenyuan(player, PASSIVE_BASE_ZHENYUAN_COST).isEmpty()) {
            return;
        }
        ResourceOps.tryAdjustJingli(player, PASSIVE_JINGLI_GAIN, true);
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (cc == null) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        if (ResourceOps.tryConsumeScaledZhenyuan(player, ACTIVE_BASE_ZHENYUAN_COST).isEmpty()) {
            return;
        }

        int amplifier = Math.max(0, ACTIVE_STRENGTH_LEVEL - 1);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ACTIVE_DURATION_TICKS, amplifier, false, true, true));
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, player.getSoundSource(), 0.7f, 1.1f);
    }

    private MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ) {
        MultiCooldown.Builder builder = MultiCooldown.builder(OrganState.of(organ, STATE_ROOT))
                .withLongClamp(value -> Math.max(0L, value), 0L);
        if (cc != null) {
            builder.withSync(cc, organ);
        } else {
            builder.withOrgan(organ);
        }
        return builder.build();
    }

    private static ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (INSTANCE.matchesOrgan(stack, ORGAN_ID)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean isPrimaryOrgan(ChestCavityInstance cc, ItemStack organ) {
        if (cc == null || cc.inventory == null || organ == null || organ.isEmpty()) {
            return false;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack slotStack = cc.inventory.getItem(i);
            if (slotStack == null || slotStack.isEmpty()) {
                continue;
            }
            if (!matchesOrgan(slotStack, ORGAN_ID)) {
                continue;
            }
            return slotStack == organ;
        }
        return false;
    }
}
