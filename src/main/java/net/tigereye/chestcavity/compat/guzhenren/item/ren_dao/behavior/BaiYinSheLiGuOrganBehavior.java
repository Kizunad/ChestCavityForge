package net.tigereye.chestcavity.compat.guzhenren.item.ren_dao.behavior;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.AbsorptionHelper;

import java.util.Optional;

/**
 * 白银舍利蛊（第三转）：
 * - 被动（致命伤触发，20分钟冷却）：吸收100HP（5分钟），恢复50%生命（上限500），力量V 30s，消耗8年寿元
 * - 主动（ATTACKABILITY，60s冷却）：金身化舍：10s 抗性II；期间若受致命伤，保留1血并获得1s无敌（每次激活仅触发一次）
 */
public enum BaiYinSheLiGuOrganBehavior implements OrganSlowTickListener, OrganIncomingDamageListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "bai_yin_she_li_gu");
    public static final ResourceLocation ABILITY_ID = ORGAN_ID;

    private static final String STATE_ROOT = "BaiYinSheLiGu";
    private static final String KEY_PASSIVE_READY_AT = "PassiveReadyAt";
    private static final String KEY_ACTIVE_UNTIL = "ActiveUntil";
    private static final String KEY_ACTIVE_COOLDOWN_UNTIL = "ActiveCooldownUntil";
    private static final String KEY_ACTIVE_CHEAT_USED = "ActiveCheatUsed"; // boolean via OrganState

    private static final long PASSIVE_COOLDOWN_TICKS = 20L * 60L * 20L; // 20min
    private static final int ACTIVE_DURATION_TICKS = 200;               // 10s
    private static final int ACTIVE_COOLDOWN_TICKS = 20 * 60;           // 60s

    private static final float ABSORPTION_AMOUNT = 100.0f;
    private static final int ABSORPTION_DURATION_TICKS = 20 * 60 * 5;   // 5min
    private static final ResourceLocation ABSORPTION_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "modifiers/bai_yin_she_li_absorption");

    private static final double PASSIVE_HEAL_RATIO = 0.50;              // 50% max
    private static final float PASSIVE_HEAL_CAP = 500.0f;               // cap 500
    private static final int STRENGTH_DURATION_TICKS = 20 * 30;         // 30s
    private static final int STRENGTH_AMP = 4;                           // 力量5 -> amp=4
    private static final int CHEAT_INVULN_TICKS = 20;                   // 1s

    static {
        OrganActivationListeners.register(ABILITY_ID, BaiYinSheLiGuOrganBehavior::activateAbility);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        // 无持续被动tick需求
    }

    public static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (entity == null || cc == null || entity.level().isClientSide()) return;
        ItemStack organ = findPrimaryOrgan(cc);
        if (organ.isEmpty()) return;
        ServerLevel server = entity.level() instanceof ServerLevel s ? s : null;
        if (server == null) return;

        MultiCooldown cd = createCooldown(cc, organ);
        long now = server.getGameTime();
        long cdUntil = cd.entry(KEY_ACTIVE_COOLDOWN_UNTIL).getReadyTick();
        if (now < Math.max(0L, cdUntil)) return;

        // 10s 抗性II
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ACTIVE_DURATION_TICKS, 1, false, true, true));
        cd.entry(KEY_ACTIVE_UNTIL).setReadyAt(now + ACTIVE_DURATION_TICKS);
        cd.entry(KEY_ACTIVE_COOLDOWN_UNTIL).setReadyAt(now + ACTIVE_COOLDOWN_TICKS);
        // 重置一次性“保命”标记
        OrganState state = OrganState.of(organ, STATE_ROOT);
        state.setBoolean(KEY_ACTIVE_CHEAT_USED, false, false);
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (victim == null || victim.level().isClientSide() || cc == null || organ == null || organ.isEmpty()) {
            return damage;
        }
        if (!matchesOrgan(organ)) {
            return damage;
        }
        ServerLevel server = victim.level() instanceof ServerLevel s ? s : null;
        if (server == null) {
            return damage;
        }
        long now = server.getGameTime();
        MultiCooldown cd = createCooldown(cc, organ);

        double hp = victim.getHealth();
        boolean lethal = hp - damage <= 0.0f;
        if (!lethal) {
            return damage;
        }

        // 先检查主动窗口的一次性"保命"
        long activeUntil = Math.max(0L, cd.entry(KEY_ACTIVE_UNTIL).getReadyTick());
        if (now < activeUntil) {
            OrganState state = OrganState.of(organ, STATE_ROOT);
            boolean used = state.getBoolean(KEY_ACTIVE_CHEAT_USED, false);
            if (!used) {
                // 留1血 + 1s 无敌，消耗一次标记
                victim.setHealth(1.0f);
                victim.invulnerableTime = Math.max(victim.invulnerableTime, CHEAT_INVULN_TICKS);
                state.setBoolean(KEY_ACTIVE_CHEAT_USED, true, false);
                return 0.0f;
            }
        }

        // 被动保命（冷却20分钟）
        long passiveReadyAt = Math.max(0L, cd.entry(KEY_PASSIVE_READY_AT).getReadyTick());
        if (now >= passiveReadyAt) {
            applyPassiveShield(victim);
            applyPassiveHealAndBuff(victim);
            // 扣除寿元8年（向下不为负）
            Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(victim);
            handleOpt.ifPresent(h -> h.adjustDouble("shouyuan", -8.0, true));
            cd.entry(KEY_PASSIVE_READY_AT).setReadyAt(now + PASSIVE_COOLDOWN_TICKS);
            return 0.0f;
        }
        return damage;
    }

    private static void applyPassiveShield(LivingEntity victim) {
        AbsorptionHelper.applyAbsorption(victim, ABSORPTION_AMOUNT, ABSORPTION_MODIFIER_ID, true);
        if (victim.level() instanceof ServerLevel server) {
            TickOps.schedule(server, () -> AbsorptionHelper.clearAbsorptionCapacity(victim, ABSORPTION_MODIFIER_ID), ABSORPTION_DURATION_TICKS);
        }
    }

    private static void applyPassiveHealAndBuff(LivingEntity victim) {
        double max = Math.max(1.0, victim.getMaxHealth());
        float heal = (float) Math.min(PASSIVE_HEAL_CAP, max * PASSIVE_HEAL_RATIO);
        if (heal > 0.0f) {
            victim.heal(heal);
        }
        victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, STRENGTH_DURATION_TICKS, STRENGTH_AMP, false, true, true));
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

