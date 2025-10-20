package net.tigereye.chestcavity.compat.guzhenren.item.yan_dao.behavior;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganIncomingDamageListener;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.NetworkUtil;
import net.tigereye.chestcavity.util.reaction.DragonFlameHelper;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 火龙蛊（脊椎）— 精简实现，提供三段主动技与龙焰印记 DoT。
 * 该实现专注于核心循环：吐息叠印、凝空防御、俯冲爆发，与被动吸血/计数的轻量逻辑。
 */
public final class HuoLongGuOrganBehavior implements OrganSlowTickListener, OrganOnHitListener, OrganIncomingDamageListener {

    public static final HuoLongGuOrganBehavior INSTANCE = new HuoLongGuOrganBehavior();

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_long_gu");

    public static final ResourceLocation ABILITY_BREATH = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_long_gu_breath");
    public static final ResourceLocation ABILITY_HOVER = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_long_gu_hover");
    public static final ResourceLocation ABILITY_DIVE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "huo_long_gu_dive");

    private static final String STATE_ROOT = "HuoLongGu";
    private static final String KEY_ASCENT_EXPIRE = "AscentExpire";
    private static final String KEY_DIVE_INVULN = "DiveInvuln";
    private static final String KEY_BREATH_READY = "BreathReady";
    private static final String KEY_HOVER_READY = "HoverReady";
    private static final String KEY_DIVE_READY = "DiveReady";
    private static final String KEY_COUNTER = "Counter";
    private static final String KEY_COUNTER_GATE = "CounterGate";

    private static final double BREATH_COST = 30000.0D;
    private static final double HOVER_COST = 150000.0D;
    private static final double DIVE_COST = 250000.0D;

    private static final int BREATH_COOLDOWN_TICKS = 20 * 20;
    private static final int HOVER_COOLDOWN_TICKS = 16 * 20;
    private static final int DIVE_COOLDOWN_TICKS = 20 * 20;
    private static final int ASCENT_DURATION_TICKS = 30;
    private static final int HOVER_DURATION_TICKS = 3 * 20;
    private static final int DIVE_INVULN_TICKS = 30;
    private static final int COUNTER_DECAY_DELAY_TICKS = 10 * 20;

    private static final int BREATH_DAMAGE = 200;
    private static final int DIVE_DAMAGE = 400;

    private static final Predicate<LivingEntity> VALID_TARGET = entity -> entity != null && entity.isAlive();

    static {
        OrganActivationListeners.register(ABILITY_BREATH, HuoLongGuOrganBehavior::activateBreath);
        OrganActivationListeners.register(ABILITY_HOVER, HuoLongGuOrganBehavior::activateHover);
        OrganActivationListeners.register(ABILITY_DIVE, HuoLongGuOrganBehavior::activateDive);
    }

    private HuoLongGuOrganBehavior() {
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (entity == null || organ == null || organ.isEmpty() || cc == null) {
            return;
        }
        Level level = entity.level();
        if (!(level instanceof ServerLevel) || level.isClientSide()) {
            return;
        }
        if (!matchesOrgan(organ)) {
            return;
        }
        OrganState state = OrganState.of(organ, STATE_ROOT);
        long now = level.getGameTime();

        long ascentExpire = state.getLong(KEY_ASCENT_EXPIRE, 0L);
        if (ascentExpire > 0 && now >= ascentExpire) {
            state.setLong(KEY_ASCENT_EXPIRE, 0L);
            ReactionTagOps.clear(entity, ReactionTagKeys.DRAGON_ASCENT);
        }

        long diveInvuln = state.getLong(KEY_DIVE_INVULN, 0L);
        if (diveInvuln > 0 && now >= diveInvuln) {
            state.setLong(KEY_DIVE_INVULN, 0L);
        }

        long counterGate = state.getLong(KEY_COUNTER_GATE, 0L);
        if (counterGate > 0 && now > counterGate) {
            int counter = Math.max(0, state.getInt(KEY_COUNTER, 0) - 1);
            state.setInt(KEY_COUNTER, counter);
            state.setLong(KEY_COUNTER_GATE, now + COUNTER_DECAY_DELAY_TICKS);
        }
    }

    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target,
                       ChestCavityInstance cc, ItemStack organ, float damage) {
        if (attacker == null || target == null || organ == null || organ.isEmpty()) {
            return damage;
        }
        if (!matchesOrgan(organ) || attacker.level().isClientSide()) {
            return damage;
        }
        int stacks = DragonFlameHelper.getStacks(target);
        if (stacks > 0) {
            attacker.heal(Math.min(0.06F, stacks * 0.01F) * damage);
        }
        return damage;
    }

    @Override
    public float onIncomingDamage(DamageSource source, LivingEntity victim, ChestCavityInstance cc,
                                  ItemStack organ, float damage) {
        if (victim == null || organ == null || organ.isEmpty()) {
            return damage;
        }
        if (!matchesOrgan(organ)) {
            return damage;
        }
        OrganState state = OrganState.of(organ, STATE_ROOT);
        long now = victim.level().getGameTime();
        long invulnExpire = state.getLong(KEY_DIVE_INVULN, 0L);
        if (invulnExpire > now) {
            return 0.0F;
        }
        return damage;
    }

    private static void activateBreath(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof Player player) || cc == null || entity.level().isClientSide()) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        Level level = entity.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        OrganState state = OrganState.of(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ, state);
        long now = level.getGameTime();
        MultiCooldown.Entry ready = cooldown.entry(KEY_BREATH_READY);
        if (!ready.isReady(now)) {
            return;
        }
        if (!ResourceOps.consumeStrict(player, BREATH_COST, 0.0D).succeeded()) {
            return;
        }
        ready.setReadyAt(now + BREATH_COOLDOWN_TICKS);

        LivingEntity target = findPrimaryTarget(serverLevel, player, 10.0D);
        if (target != null) {
            target.hurt(player.damageSources().mobAttack(player), BREATH_DAMAGE);
            DragonFlameHelper.applyStacks(player, target, 2);
            applySplashDamage(serverLevel, player, target.position(), 3.0D, player.getMaxHealth() * 0.10D);
        }

        ReactionTagOps.add(player, ReactionTagKeys.DRAGON_ASCENT, ASCENT_DURATION_TICKS);
        state.setLong(KEY_ASCENT_EXPIRE, now + ASCENT_DURATION_TICKS);
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ASCENT_DURATION_TICKS, 0, false, false, true));
        bumpCounter(state, now, 2);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    private static void activateHover(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof Player player) || cc == null || entity.level().isClientSide()) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        Level level = entity.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        OrganState state = OrganState.of(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ, state);
        long now = level.getGameTime();
        MultiCooldown.Entry ready = cooldown.entry(KEY_HOVER_READY);
        if (!ready.isReady(now)) {
            return;
        }
        if (!ResourceOps.consumeStrict(player, HOVER_COST, 0.0D).succeeded()) {
            return;
        }
        ready.setReadyAt(now + HOVER_COOLDOWN_TICKS);
        ReactionTagOps.add(player, ReactionTagKeys.DRAGON_ASCENT, HOVER_DURATION_TICKS);
        state.setLong(KEY_ASCENT_EXPIRE, now + HOVER_DURATION_TICKS);
        player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, HOVER_DURATION_TICKS, 0, false, false, true));
        bumpCounter(state, now, 2);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    private static void activateDive(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof Player player) || cc == null || entity.level().isClientSide()) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }
        Level level = entity.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        OrganState state = OrganState.of(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ, state);
        long now = level.getGameTime();
        MultiCooldown.Entry ready = cooldown.entry(KEY_DIVE_READY);
        if (!ready.isReady(now)) {
            return;
        }
        if (!ResourceOps.consumeStrict(player, DIVE_COST, 0.0D).succeeded()) {
            return;
        }
        double hpCost = player.getMaxHealth() * 0.30D;
        ResourceOps.drainHealth(player, (float) hpCost, player.damageSources().generic());
        ready.setReadyAt(now + DIVE_COOLDOWN_TICKS);
        double bonus = Math.min(0.60D, Math.floor(hpCost / 100.0D) * 0.15D);
        double damage = DIVE_DAMAGE * (1.0D + bonus);
        applySplashDamage(serverLevel, player, player.position(), 4.0D, damage);
        ReactionTagOps.add(player, ReactionTagKeys.DRAGON_DIVE, 40);
        ReactionTagOps.add(player, ReactionTagKeys.FIRE_IMMUNE, DIVE_INVULN_TICKS);
        state.setLong(KEY_DIVE_INVULN, now + DIVE_INVULN_TICKS);
        bumpCounter(state, now, 5);
        NetworkUtil.sendOrganSlotUpdate(cc, organ);
    }

    private static MultiCooldown createCooldown(ChestCavityInstance cc, ItemStack organ, OrganState state) {
        MultiCooldown.Builder builder = MultiCooldown.builder(state)
                .withLongClamp(value -> Math.max(0L, value), 0L);
        if (cc != null) {
            builder.withSync(cc, organ);
        } else {
            builder.withOrgan(organ);
        }
        return builder.build();
    }

    private static void bumpCounter(OrganState state, long now, int amount) {
        if (amount <= 0) {
            return;
        }
        int current = state.getInt(KEY_COUNTER, 0);
        state.setInt(KEY_COUNTER, current + amount);
        state.setLong(KEY_COUNTER_GATE, now + COUNTER_DECAY_DELAY_TICKS);
    }

    private static LivingEntity findPrimaryTarget(ServerLevel level, LivingEntity user, double range) {
        Vec3 origin = user.position();
        AABB box = new AABB(origin.x - range, origin.y - range, origin.z - range,
                origin.x + range, origin.y + range, origin.z + range);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box, VALID_TARGET);
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (LivingEntity target : candidates) {
            if (target == user) continue;
            double dist = target.distanceToSqr(user);
            if (dist < closestDist) {
                closest = target;
                closestDist = dist;
            }
        }
        return closest;
    }

    private static void applySplashDamage(ServerLevel level, LivingEntity attacker, Vec3 center, double radius, double damage) {
        AABB box = new AABB(center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box, VALID_TARGET);
        for (LivingEntity target : targets) {
            if (target == attacker) continue;
            target.hurt(attacker.damageSources().mobAttack(attacker), (float) damage);
            DragonFlameHelper.applyStacks(attacker, target, 1);
        }
    }

    private static boolean matchesOrgan(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return Objects.equals(id, ORGAN_ID);
    }

    private static ItemStack findOrgan(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return ItemStack.EMPTY;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = cc.inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (Objects.equals(id, ORGAN_ID)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}

