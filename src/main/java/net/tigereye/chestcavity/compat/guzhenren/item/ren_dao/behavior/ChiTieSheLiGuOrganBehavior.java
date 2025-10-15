package net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

import java.util.Optional;

/**
 * 赤铁舍利蛊：
 * - 被动：当魂魄满值时，清除自身缓慢/挖掘疲劳一次（冷却10分钟）
 * - 主动（ATTACKABILITY）：消耗 800 青铜真元与 20 魂魄，立即回复20%生命（上限200），冷却30秒
 */
public enum ChiTieSheLiGuOrganBehavior implements OrganSlowTickListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "chi_tie_she_li_gu");
    public static final ResourceLocation ABILITY_ID = ORGAN_ID;

    private static final String STATE_ROOT = "ChiTieSheLiGu";
    private static final String KEY_PURGE_READY_AT = "PurgeReadyAt";       // 被动净化冷却
    private static final String KEY_COOLDOWN_UNTIL = "CooldownUntil";      // 主动冷却

    private static final long PASSIVE_COOLDOWN_TICKS = 20L * 60L * 10L;     // 10分钟
    private static final int ACTIVE_COOLDOWN_TICKS = 20 * 30;               // 30秒
    private static final double ACTIVE_ZHENYUAN_COST = 800.0;               // 青铜真元（scaled）
    private static final double ACTIVE_HUNPO_COST = 20.0;                   // 魂魄
    private static final double HEAL_RATIO = 0.20;                           // 20% max HP
    private static final float HEAL_CAP = 200.0f;                            // 上限200

    static {
        OrganActivationListeners.register(ABILITY_ID, ChiTieSheLiGuOrganBehavior::activateAbility);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || entity.level().isClientSide() || cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!matchesOrgan(organ)) {
            return;
        }
        ServerLevel server = entity.level() instanceof ServerLevel s ? s : null;
        if (server == null) return;

        MultiCooldown cd = createCooldown(cc, organ);
        MultiCooldown.Entry passiveReady = cd.entry(KEY_PURGE_READY_AT);
        long now = server.getGameTime();
        if (passiveReady.getReadyTick() <= 0L) {
            passiveReady.setReadyAt(0L);
        }

        // 条件：魂魄满值
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(entity);
        if (handleOpt.isEmpty()) {
            return;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
        double hunpo = handle.read("hunpo").orElse(0.0);
        double maxHunpo = handle.read("zuida_hunpo").orElse(0.0);
        if (!(maxHunpo > 0.0) || hunpo < maxHunpo) {
            return;
        }
        if (now < Math.max(0L, passiveReady.getReadyTick())) {
            return;
        }
        // 执行一次“宁心”：清除缓慢与挖掘疲劳
        entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        entity.removeEffect(MobEffects.DIG_SLOWDOWN);
        passiveReady.setReadyAt(now + PASSIVE_COOLDOWN_TICKS);
    }

    public static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (entity == null || cc == null || entity.level().isClientSide()) {
            return;
        }
        ItemStack organ = findPrimaryOrgan(cc);
        if (organ.isEmpty()) return;
        ServerLevel server = entity.level() instanceof ServerLevel s ? s : null;
        if (server == null) return;

        MultiCooldown cd = createCooldown(cc, organ);
        long now = server.getGameTime();
        if (now < Math.max(0L, cd.entry(KEY_COOLDOWN_UNTIL).getReadyTick())) {
            return;
        }

        // 资源扣除：先真元（scaled），再魂魄（strict）。任何一步失败则中断。
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(entity);
        if (handleOpt.isEmpty()) return;
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();

        // 先扣魂魄，保证可退款
        var hp = ResourceOps.consumeHunpoStrict(entity, ACTIVE_HUNPO_COST);
        if (!hp.succeeded()) {
            return;
        }
        var zr = ResourceOps.tryConsumeScaledZhenyuan(handle, ACTIVE_ZHENYUAN_COST);
        if (zr.isEmpty()) {
            // 真元失败，退款魂魄
            if (entity instanceof net.minecraft.world.entity.player.Player p) {
                ResourceOps.refund(p, hp);
            }
            return;
        }

        // 治疗：20%（上限200）
        double max = Math.max(1.0, entity.getMaxHealth());
        float heal = (float) Math.min(HEAL_CAP, max * HEAL_RATIO);
        if (heal > 0.0f) {
            entity.heal(heal);
        }
        cd.entry(KEY_COOLDOWN_UNTIL).setReadyAt(now + ACTIVE_COOLDOWN_TICKS);
    }

    private static boolean matchesOrgan(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return ORGAN_ID.equals(id);
    }

    private static ItemStack findPrimaryOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) return ItemStack.EMPTY;
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack candidate = cc.inventory.getItem(i);
            if (matchesOrgan(candidate)) {
                return candidate;
            }
        }
        return ItemStack.EMPTY;
    }

    private static MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ) {
        MultiCooldown.Builder b = MultiCooldown.builder(OrganState.of(organ, STATE_ROOT))
                .withLongClamp(value -> Math.max(0L, value), 0L);
        if (cc != null) {
            b.withSync(cc, organ);
        } else {
            b.withOrgan(organ);
        }
        return b.build();
    }
}
