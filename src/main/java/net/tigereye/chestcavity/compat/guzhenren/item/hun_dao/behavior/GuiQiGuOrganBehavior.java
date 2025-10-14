package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.combat.HunDaoDamageUtil;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.ResourceOps;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.Cooldown;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowControllerManager;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgram;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgramRegistry;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Behaviour for 鬼气蛊 (Gui Qi Gu).
 * <p>
 * Passive effects:
 * <ul>
 *     <li>Regenerates hunpo and jingli every slow tick.</li>
 *     <li>Empowers melee hits with additional true damage equal to 1% of the carrier's maximum hunpo.</li>
 * </ul>
 * <p>
 * Active ability "鬼雾": starts the {@code hun_dao/gui_wu} GuScript flow that emits a black fog
 * in front of the caster and inflicts Slowness IV and Blindness on nearby hostiles.
 */
public final class GuiQiGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
        implements OrganSlowTickListener, OrganOnHitListener {

    public static final GuiQiGuOrganBehavior INSTANCE = new GuiQiGuOrganBehavior();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation ORGAN_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "guiqigu");
    public static final ResourceLocation ABILITY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "gui_wu");

    private static final ResourceLocation GUI_WU_FLOW_ID = ChestCavity.id("hun_dao/gui_wu");

    private static final double PASSIVE_HUNPO_PER_SECOND = 3.0D;
    private static final double PASSIVE_JINGLI_PER_SECOND = 1.0D;
    private static final double TRUE_DAMAGE_RATIO = 0.03D;
    private static final double GUI_WU_RADIUS = 4.0D;
    private static final int GUI_WU_COOLDOWN_TICKS = BehaviorConfigAccess.getInt(GuiQiGuOrganBehavior.class, "GUI_WU_COOLDOWN_TICKS", 160);

    private static final String STATE_ROOT_KEY = "GuiQiGu";
    private static final String KEY_COOLDOWN_UNTIL = "CooldownUntil";

    private static final ThreadLocal<Boolean> REENTRY_GUARD = ThreadLocal.withInitial(() -> Boolean.FALSE);

    static {
        OrganActivationListeners.register(ABILITY_ID, GuiQiGuOrganBehavior::activateAbility);
    }

    private GuiQiGuOrganBehavior() {
    }

    public void ensureAttached(ChestCavityInstance cc) {
        // No dedicated linkage channels needed yet; method kept for future expansion.
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        sendSlotUpdate(cc, organ);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return;
        }

        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
        int stackCount = Math.max(1, organ.getCount());
        double hunpoGain = PASSIVE_HUNPO_PER_SECOND * stackCount;
        double jingliGain = PASSIVE_JINGLI_PER_SECOND * stackCount;
        ResourceOps.tryAdjustDouble(handle, "hunpo", hunpoGain, true, "zuida_hunpo");
        ResourceOps.tryAdjustDouble(handle, "jingli", jingliGain, true, "zuida_jingli");
    }

    @Override
    public float onHit(
            DamageSource source,
            LivingEntity attacker,
            LivingEntity target,
            ChestCavityInstance cc,
            ItemStack organ,
            float damage
    ) {
        if (Boolean.TRUE.equals(REENTRY_GUARD.get())) {
            return damage;
        }
        if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
            return damage;
        }
        if (target == null || !target.isAlive()) {
            return damage;
        }
        if (!matchesOrgan(organ, ORGAN_ID)) {
            return damage;
        }
        if (source == null || source.is(DamageTypeTags.IS_PROJECTILE)) {
            return damage;
        }

        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return damage;
        }
        double maxHunpo = handleOpt.get().read("zuida_hunpo").orElse(0.0D);
        if (!(maxHunpo > 0.0D)) {
            return damage;
        }

        float extraDamage = (float) (maxHunpo * TRUE_DAMAGE_RATIO);
        if (extraDamage <= 0.0F) {
            return damage;
        }

        REENTRY_GUARD.set(Boolean.TRUE);
        try {
            DamageSource trueSource = player.damageSources().magic();
            HunDaoDamageUtil.markHunDaoAttack(trueSource);
            target.hurt(trueSource, extraDamage);
        } finally {
            REENTRY_GUARD.set(Boolean.FALSE);
        }
        return damage;
    }

    public static boolean hasGuiQiGu(ChestCavityInstance cc) {
        if (cc == null || cc.inventory == null) {
            return false;
        }
        int size = cc.inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack candidate = cc.inventory.getItem(i);
            if (candidate.isEmpty()) {
                continue;
            }
            if (BuiltInOrganIds.matches(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
        if (!(entity instanceof Player player) || cc == null || entity.level().isClientSide()) {
            return;
        }
        ItemStack organ = findOrgan(cc);
        if (organ.isEmpty()) {
            return;
        }

        Level level = entity.level();
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) {
            return;
        }

        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }

        long currentTick = level.getGameTime();
        OrganState state = INSTANCE.organState(organ, STATE_ROOT_KEY);
        Cooldown cooldown = Cooldown.of(state, KEY_COOLDOWN_UNTIL);
        if (!cooldown.isReady(currentTick)) {
            return;
        }

        Optional<FlowProgram> programOpt = FlowProgramRegistry.get(GUI_WU_FLOW_ID);
        if (programOpt.isEmpty()) {
            return;
        }

        FlowController controller = FlowControllerManager.get(serverPlayer);
        Map<String, String> params = new HashMap<>();
        params.put("gui_wu.radius", formatDouble(GUI_WU_RADIUS));
        controller.start(programOpt.get(), player, 1.0D, params, server.getGameTime(), "hun_dao.gui_wu");

        long readyAt = currentTick + GUI_WU_COOLDOWN_TICKS;
        OrganStateOps.setLong(state, cc, organ, KEY_COOLDOWN_UNTIL, readyAt, value -> Math.max(0L, value), 0L);
        // schedule a ready-toast at cooldown end
        cooldown.onReady(server, currentTick, () -> {
            try {
                var id = BuiltInRegistries.ITEM.getKey(organ.getItem());
                var payload = new net.tigereye.chestcavity.network.packets.CooldownReadyToastPayload(
                        true,
                        id,
                        "技能就绪",
                        organ.getHoverName().getString()
                );
                net.tigereye.chestcavity.network.NetworkHandler.sendCooldownToast(serverPlayer, payload);
            } catch (Throwable t) {
                ChestCavity.LOGGER.debug("[hun_dao][gui_qi_gu] failed to send ready toast: {}", t.toString());
            }
        });
        INSTANCE.sendSlotUpdate(cc, organ);
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
            if (BuiltInOrganIds.matches(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static final class BuiltInOrganIds {
        private BuiltInOrganIds() {
        }

        private static boolean matches(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return false;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return Objects.equals(id, ORGAN_ID);
        }
    }
}
