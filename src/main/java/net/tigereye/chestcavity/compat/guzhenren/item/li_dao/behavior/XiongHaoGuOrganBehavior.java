package net.tigereye.chestcavity.compat.guzhenren.item.li_dao.behavior;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.li_dao.AbstractLiDaoOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.guzhenren.util.GuzhenrenResourceCostHelper.ConsumptionResult;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.minecraft.server.level.ServerPlayer;

/**
 * 熊豪蛊（力道·肌肉）：
 * 主动：消耗 300 BASE 真元，10 秒内每次近战命中额外消耗 6 精力并追加 10 点伤害。
 * 冷却：25 秒；仅首个插槽生效。
 */
public final class XiongHaoGuOrganBehavior extends AbstractLiDaoOrganBehavior implements OrganOnHitListener, OrganSlowTickListener {

    public static final XiongHaoGuOrganBehavior INSTANCE = new XiongHaoGuOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "xiong_hao_gu");
    public static final ResourceLocation ABILITY_ID = ORGAN_ID;

    private static final String STATE_ROOT = "XiongHaoGu";
    private static final String KEY_ACTIVE = "Active";
    private static final String KEY_EXPIRE_TICK = "ExpireTick";
    private static final String KEY_NEXT_READY_TICK = "NextReadyTick";

    private static final int ACTIVE_DURATION_TICKS = BehaviorConfigAccess.getInt(XiongHaoGuOrganBehavior.class, "ACTIVE_DURATION_TICKS", 10 * 20);
    private static final int COOLDOWN_TICKS = BehaviorConfigAccess.getInt(XiongHaoGuOrganBehavior.class, "COOLDOWN_TICKS", 25 * 20);
    private static final double ACTIVE_BASE_ZHENYUAN_COST = BehaviorConfigAccess.getFloat(XiongHaoGuOrganBehavior.class, "ACTIVE_BASE_ZHENYUAN_COST", 300.0f);
    private static final double PER_ATTACK_JINGLI_COST = BehaviorConfigAccess.getFloat(XiongHaoGuOrganBehavior.class, "PER_ATTACK_JINGLI_COST", 6.0f);
    private static final float EXTRA_DAMAGE = BehaviorConfigAccess.getFloat(XiongHaoGuOrganBehavior.class, "EXTRA_DAMAGE", 10.0f);

    static {
        OrganActivationListeners.register(ABILITY_ID, XiongHaoGuOrganBehavior::activateAbility);
    }

    private XiongHaoGuOrganBehavior() {}

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || player.level().isClientSide() || cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        if (!isPrimaryOrgan(cc, organ)) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ);
        long now = player.level().getGameTime();
        MultiCooldown.Entry expireEntry = cooldown.entry(KEY_EXPIRE_TICK);
        if (state.getBoolean(KEY_ACTIVE, false) && expireEntry.getReadyTick() <= now) {
            deactivate(cc, organ, state, cooldown);
        }
    }

    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID) || !isPrimaryOrgan(cc, organ)) {
            return damage;
        }
        if (damage <= 0.0f || target == null || !target.isAlive() || target == attacker || target.isAlliedTo(attacker)) {
            return damage;
        }
        if (source == null || source.getDirectEntity() != attacker || source.is(DamageTypeTags.IS_PROJECTILE)) {
            return damage;
        }

        OrganState state = organState(organ, STATE_ROOT);
        MultiCooldown cooldown = createCooldown(cc, organ);
        long now = attacker.level().getGameTime();
        MultiCooldown.Entry expireEntry = cooldown.entry(KEY_EXPIRE_TICK);
        if (!state.getBoolean(KEY_ACTIVE, false) || expireEntry.getReadyTick() <= now) {
            if (state.getBoolean(KEY_ACTIVE, false)) {
                deactivate(cc, organ, state, cooldown);
            }
            return damage;
        }

        ConsumptionResult payment = ResourceOps.consumeStrict(player, 0.0, PER_ATTACK_JINGLI_COST);
        if (!payment.succeeded()) {
            deactivate(cc, organ, state, cooldown);
            return damage;
        }

        return Math.max(0.0f, damage + EXTRA_DAMAGE);
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

        OrganState state = INSTANCE.organState(organ, STATE_ROOT);
        MultiCooldown cooldown = INSTANCE.createCooldown(cc, organ);
        long now = player.level().getGameTime();
        MultiCooldown.Entry expireEntry = cooldown.entry(KEY_EXPIRE_TICK);
        MultiCooldown.Entry readyEntry = cooldown.entry(KEY_NEXT_READY_TICK);

        if (state.getBoolean(KEY_ACTIVE, false) && expireEntry.getReadyTick() > now) {
            return;
        }
        if (readyEntry.getReadyTick() > now) {
            return;
        }

        if (ResourceOps.tryConsumeScaledZhenyuan(player, ACTIVE_BASE_ZHENYUAN_COST).isEmpty()) {
            return;
        }

        boolean dirty = OrganStateOps.setBoolean(state, cc, organ, KEY_ACTIVE, true, false).changed();
        expireEntry.setReadyAt(now + ACTIVE_DURATION_TICKS);
        long readyAt = now + COOLDOWN_TICKS;
        readyEntry.setReadyAt(readyAt);
        if (dirty) {
            INSTANCE.sendSlotUpdate(cc, organ);
        }

        if (player instanceof ServerPlayer sp) {
            ActiveSkillRegistry.scheduleReadyToast(sp, ABILITY_ID, readyAt, now);
        }

        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, player.getSoundSource(), 0.9f, 0.9f);
    }

    private void deactivate(ChestCavityInstance cc, ItemStack organ, OrganState state, MultiCooldown cooldown) {
        boolean dirty = OrganStateOps.setBoolean(state, cc, organ, KEY_ACTIVE, false, false).changed();
        cooldown.entry(KEY_EXPIRE_TICK).setReadyAt(0L);
        if (dirty) {
            sendSlotUpdate(cc, organ);
        }
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
