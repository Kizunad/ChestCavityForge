package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.storage.BeastSoulStorage;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.storage.OrganBeastSoulStorage;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.guzhenren.resource.GuzhenrenResourceBridge;
import net.tigereye.chestcavity.linkage.ActiveLinkageContext;
import net.tigereye.chestcavity.linkage.LinkageChannel;
import net.tigereye.chestcavity.linkage.LinkageManager;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganRemovalListener;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.util.DoTManager;
import org.slf4j.Logger;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Behaviour scaffold for 小魂蛊，负责魂兽形态的资源流转与攻击挂钩。
 */
public final class HunDaoSoulBeastBehavior extends AbstractGuzhenrenOrganBehavior implements OrganSlowTickListener, OrganOnHitListener, OrganRemovalListener {

    public static final HunDaoSoulBeastBehavior INSTANCE = new HunDaoSoulBeastBehavior();

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String MOD_ID = "guzhenren";
    private static final ResourceLocation HUN_DAO_INCREASE_EFFECT = ResourceLocation.fromNamespaceAndPath(MOD_ID, "linkage/hun_dao_increase_effect");

    private static final double PASSIVE_HUNPO_LEAK = 3.0;
    private static final double ATTACK_HUNPO_COST = 18.0;
    private static final double SOUL_FLAME_PERCENT = 0.01;
    private static final int SOUL_FLAME_DURATION_SECONDS = 5;

    private static final String STATE_ROOT_KEY = "HunDaoSoulBeast";
    private static final String KEY_BOUND = "bound";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_OWNER_MSB = "owner_msb";
    private static final String KEY_OWNER_LSB = "owner_lsb";
    private static final String KEY_BOUND_TIME = "bound_time";
    private static final String KEY_LAST_SYNC_TICK = "last_sync_tick";

    private final BeastSoulStorage beastSoulStorage = new OrganBeastSoulStorage(STATE_ROOT_KEY);

    private HunDaoSoulBeastBehavior() {
    }

    public void ensureAttached(ChestCavityInstance cc) {
        if (cc == null) {
            return;
        }
        ActiveLinkageContext context = LinkageManager.getContext(cc);
        ensureChannel(context, HUN_DAO_INCREASE_EFFECT);
    }

    public void onEquip(ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
        if (cc == null || organ == null || organ.isEmpty()) {
            return;
        }
        registerRemovalHook(cc, organ, this, staleRemovalContexts);
        ensureAttached(cc);
        bindOrganState(cc, organ);
        sendSlotUpdate(cc, organ);
    }

    @Override
    public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        ensureAttached(cc);
        ensureActiveState(entity, organ);
        drainHunpo(player, PASSIVE_HUNPO_LEAK, "passive leak");
        maintainSatiation(player);
        OrganState state = organState(organ, STATE_ROOT_KEY);
        logStateChange(LOGGER, prefix(), organ, KEY_LAST_SYNC_TICK, state.setLong(KEY_LAST_SYNC_TICK, entity.level().getGameTime()));
    }

    @Override
    public float onHit(DamageSource source, LivingEntity attacker, LivingEntity target, ChestCavityInstance cc, ItemStack organ, float damage) {
        if (!(attacker instanceof Player player) || attacker.level().isClientSide()) {
            return damage;
        }
        if (source == null || source.is(DamageTypeTags.IS_PROJECTILE) || target == null || !target.isAlive()) {
            return damage;
        }
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return damage;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
        double currentHunpo = handle.read("hunpo").orElse(0.0);
        if (currentHunpo < ATTACK_HUNPO_COST) {
            LOGGER.debug("{} {} lacks hunpo for soul flame ({} / {})", prefix(), describePlayer(player), format(currentHunpo), format(ATTACK_HUNPO_COST));
            return damage;
        }
        handle.adjustDouble("hunpo", -ATTACK_HUNPO_COST, true, "zuida_hunpo");
        double maxHunpo = handle.read("zuida_hunpo").orElse(0.0);
        double efficiency = 1.0;
        if (cc != null) {
            ActiveLinkageContext context = LinkageManager.getContext(cc);
            LinkageChannel channel = ensureChannel(context, HUN_DAO_INCREASE_EFFECT);
            if (channel != null) {
                efficiency += Math.max(0.0, channel.get());
            }
        }
        double dotDamage = Math.max(0.0, maxHunpo * SOUL_FLAME_PERCENT * efficiency);
        if (dotDamage > 0.0) {
            DoTManager.schedulePerSecond(player, target, dotDamage, SOUL_FLAME_DURATION_SECONDS, null, 0.0f, 0.0f);
            LOGGER.debug("{} applied soul flame DoT={}s @{} dmg to {}", prefix(), SOUL_FLAME_DURATION_SECONDS, format(dotDamage), target.getName().getString());
        }
        return damage;
    }

    @Override
    public void onRemoved(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
        if (!(entity instanceof Player player)) {
            return;
        }
        ensureActiveState(entity, organ);
        LOGGER.debug("{} soul beast organ removed but state retained for {}", prefix(), describePlayer(player));
    }

    public BeastSoulStorage beastSoulStorage() {
        return beastSoulStorage;
    }

    private void bindOrganState(ChestCavityInstance cc, ItemStack organ) {
        OrganState state = organState(organ, STATE_ROOT_KEY);
        logStateChange(LOGGER, prefix(), organ, KEY_BOUND, state.setBoolean(KEY_BOUND, true));
        if (cc.owner != null) {
            UUID ownerId = cc.owner.getUUID();
            logStateChange(LOGGER, prefix(), organ, KEY_OWNER_MSB, state.setLong(KEY_OWNER_MSB, ownerId.getMostSignificantBits()));
            logStateChange(LOGGER, prefix(), organ, KEY_OWNER_LSB, state.setLong(KEY_OWNER_LSB, ownerId.getLeastSignificantBits()));
            logStateChange(LOGGER, prefix(), organ, KEY_BOUND_TIME, state.setLong(KEY_BOUND_TIME, cc.owner.level().getGameTime()));
        }
        logStateChange(LOGGER, prefix(), organ, KEY_ACTIVE, state.setBoolean(KEY_ACTIVE, true));
    }

    private void ensureActiveState(LivingEntity entity, ItemStack organ) {
        if (!(entity instanceof Player player) || organ == null || organ.isEmpty()) {
            return;
        }
        OrganState state = organState(organ, STATE_ROOT_KEY);
        logStateChange(LOGGER, prefix(), organ, KEY_ACTIVE, state.setBoolean(KEY_ACTIVE, true));
        if (state.getLong(KEY_OWNER_MSB, 0L) == 0L && state.getLong(KEY_OWNER_LSB, 0L) == 0L) {
            UUID uuid = player.getUUID();
            logStateChange(LOGGER, prefix(), organ, KEY_OWNER_MSB, state.setLong(KEY_OWNER_MSB, uuid.getMostSignificantBits()));
            logStateChange(LOGGER, prefix(), organ, KEY_OWNER_LSB, state.setLong(KEY_OWNER_LSB, uuid.getLeastSignificantBits()));
        }
    }

    private void drainHunpo(Player player, double amount, String reason) {
        Optional<GuzhenrenResourceBridge.ResourceHandle> handleOpt = GuzhenrenResourceBridge.open(player);
        if (handleOpt.isEmpty()) {
            return;
        }
        GuzhenrenResourceBridge.ResourceHandle handle = handleOpt.get();
        handle.adjustDouble("hunpo", -amount, true, "zuida_hunpo");
        LOGGER.trace("{} drained {} hunpo from {} ({})", prefix(), format(amount), describePlayer(player), reason);
    }

    private void maintainSatiation(Player player) {
        FoodData foodData = player.getFoodData();
        if (foodData.getFoodLevel() < 18) {
            foodData.eat(1, 0.6f);
        }
        if (foodData.getSaturationLevel() < foodData.getFoodLevel()) {
            foodData.eat(0, 0.4f);
        }
    }

    private String describePlayer(Player player) {
        return player.getScoreboardName();
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String prefix() {
        return "[compat/guzhenren][hun_dao]";
    }
}
