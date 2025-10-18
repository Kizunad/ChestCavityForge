package net.tigereye.chestcavity.compat.guzhenren.item.xin_dao.behavior;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.TickOps;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;

/**
 * 一星半点蛊：濒死受击时触发“残光护体”。
 */
public enum XingBanDianGuOrganBehavior implements OrganIncomingDamageListener {
    INSTANCE;

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xing_ban_dian_gu");
    private static final ResourceLocation BLEED_EFFECT_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "lliuxue");

    private static final String STATE_ROOT = "XingBanDianGu";
    private static final String READY_AT_KEY = "ReadyAt";           // 时间戳式同类冷却（gameTime）
    private static final String INVULN_UNTIL_KEY = "InvulnUntil";   // 无敌结束时间（gameTime）

    private static final long INVULN_DURATION_TICKS = 40L;           // 2秒
    private static final long SHARED_COOLDOWN_TICKS = 20L * 60L * 10L; // 10分钟
    private static final int HIDDEN_RESIST_AMP = 4;                  // 抗性V（放大器4），隐藏

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (victim == null || victim.level().isClientSide() || cc == null) {
            return damage;
        }
        ItemStack primary = resolvePrimaryOrgan(cc, organ);
        if (primary.isEmpty()) {
            return damage;
        }
        // 确保仅主实例处理，避免多枚重复触发/重复消耗
        if (organ != primary) {
            return damage;
        }

        MultiCooldown cooldown = createSharedCooldown(cc, primary);
        MultiCooldown.Entry ready = cooldown.entry(READY_AT_KEY);
        MultiCooldown.Entry invulnUntil = cooldown.entry(INVULN_UNTIL_KEY);

        long now = victim.level().getGameTime();

        // 已处于无敌窗口：本次伤害直接免疫
        if (now < Math.max(0L, invulnUntil.getReadyTick())) {
            return 0.0f;
        }

        // 触发条件：低于1%生命 或 本次伤害将致死
        double max = Math.max(1.0, victim.getMaxHealth());
        boolean lowHealth = victim.getHealth() <= max * 0.01;
        boolean lethal = victim.getHealth() - damage <= 0.0f;
        if (!lowHealth && !lethal) {
            return damage;
        }

        // 冷却校验
        if (now < Math.max(0L, ready.getReadyTick())) {
            return damage;
        }

        // 仅检查是否存在至少一枚；实际消耗延后到下一tick，避免迭代并发修改
        if (!hasAtLeastOneOrgan(cc)) {
            return damage;
        }

        // 设置无敌与冷却，并立即执行效果（拦截本次伤害）
        invulnUntil.setReadyAt(now + INVULN_DURATION_TICKS);
        ready.setReadyAt(now + SHARED_COOLDOWN_TICKS);

        grantHiddenResistance(victim, (int) INVULN_DURATION_TICKS);
        healBurst(victim);
        clearDebuffs(victim);
        playFx(victim);
        ReactionTagOps.add(victim, ReactionTagKeys.STAR_GLINT, (int) INVULN_DURATION_TICKS + 20);
        // 延迟到下一tick从胸腔背包消耗一枚，规避并发修改异常
        if (victim.level() instanceof ServerLevel server) {
            TickOps.schedule(server, () -> consumeOneOrgan(cc), 1);
        }

        // 本次伤害被完全免疫
        return 0.0f;
    }

    private static ItemStack resolvePrimaryOrgan(ChestCavityInstance cc, ItemStack fallback) {
        if (fallback != null && !fallback.isEmpty()) {
            return fallback;
        }
        if (cc != null && cc.inventory != null) {
            for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
                ItemStack candidate = cc.inventory.getItem(i);
                if (candidate.isEmpty()) continue;
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(candidate.getItem());
                if (ORGAN_ID.equals(id)) {
                    return candidate;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static MultiCooldown createSharedCooldown(ChestCavityInstance cc, ItemStack primary) {
        MultiCooldown.Builder builder = MultiCooldown.builder(OrganState.of(primary, STATE_ROOT))
                .withLongClamp(value -> Math.max(0L, value), 0L);
        if (cc != null) {
            builder.withSync(cc, primary);
        } else {
            builder.withOrgan(primary);
        }
        return builder.build();
    }

    private static boolean consumeOneOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) return false;
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!ORGAN_ID.equals(id)) continue;
            stack.shrink(1);
            if (stack.isEmpty()) {
                cc.inventory.setItem(i, ItemStack.EMPTY);
            }
            cc.inventory.setChanged();
            return true;
        }
        return false;
    }

    private static boolean hasAtLeastOneOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) return false;
        for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (ORGAN_ID.equals(id)) return true;
        }
        return false;
    }

    private static void grantHiddenResistance(LivingEntity victim, int durationTicks) {
        if (durationTicks <= 0) return;
        victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationTicks, HIDDEN_RESIST_AMP, false, false, false));
        victim.invulnerableTime = Math.max(victim.invulnerableTime, durationTicks);
    }

    private static void healBurst(LivingEntity victim) {
        double max = Math.max(1.0, victim.getMaxHealth());
        float cap = 100.0f; // 100HP 上限
        float amount = (float) Math.min(cap, max * 0.15);
        if (amount > 0f) {
            victim.heal(amount);
        }
    }

    private static void clearDebuffs(LivingEntity victim) {
        victim.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        victim.removeEffect(MobEffects.WITHER);
        victim.clearFire();
        try {
            var holder = BuiltInRegistries.MOB_EFFECT.getHolder(BLEED_EFFECT_ID);
            holder.ifPresent(victim::removeEffect);
        } catch (Throwable ignored) {}
    }

    private static void playFx(LivingEntity victim) {
        if (!(victim.level() instanceof ServerLevel server)) {
            return;
        }
        server.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9f, 1.25f);
        server.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.7f, 0.7f);

        for (int i = 0; i < 24; i++) {
            double dx = (victim.getRandom().nextDouble() - 0.5) * 0.6;
            double dy = (victim.getRandom().nextDouble() - 0.2) * 0.4;
            double dz = (victim.getRandom().nextDouble() - 0.5) * 0.6;
            server.sendParticles(ParticleTypes.END_ROD,
                    victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(),
                    1, dx, dy, dz, 0.05);
        }
        int segments = 20;
        double radius = Math.max(0.6, victim.getBbWidth() * 0.8);
        Vec3 center = new Vec3(victim.getX(), victim.getY() + victim.getBbHeight() * 0.55, victim.getZ());
        for (int i = 0; i < segments; i++) {
            double ang = (Math.PI * 2 * i) / segments;
            double x = center.x + Math.cos(ang) * radius;
            double z = center.z + Math.sin(ang) * radius;
            double y = center.y + Math.sin(ang * 2) * 0.05;
            server.sendParticles(ParticleTypes.GLOW, x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }
}
