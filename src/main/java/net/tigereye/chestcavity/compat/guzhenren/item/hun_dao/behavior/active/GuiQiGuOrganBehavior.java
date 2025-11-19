package net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior.active;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import net.tigereye.chestcavity.compat.guzhenren.item.common.AbstractGuzhenrenOrganBehavior;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.behavior.common.HunDaoBehaviorContextHelper;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.combat.HunDaoDamageUtil;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.runtime.HunDaoRuntimeContext;
import net.tigereye.chestcavity.compat.guzhenren.item.hun_dao.tuning.HunDaoTuning;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.BehaviorConfigAccess;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.Cooldown;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.OrganStateOps;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowController;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowControllerManager;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgram;
import net.tigereye.chestcavity.guscript.runtime.flow.FlowProgramRegistry;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganOnHitListener;
import net.tigereye.chestcavity.listeners.OrganRemovalContext;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;
import net.tigereye.chestcavity.skill.ActiveSkillRegistry;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagKeys;
import net.tigereye.chestcavity.util.reaction.tag.ReactionTagOps;

/**
 * Behaviour for 鬼气蛊 (Gui Qi Gu).
 *
 * <p>Passive effects:
 *
 * <ul>
 *   <li>Regenerates hunpo and jingli every slow tick.
 *   <li>Empowers melee hits with additional true damage equal to 1% of the carrier's maximum hunpo.
 * </ul>
 *
 * <p>Active ability "鬼雾": starts the {@code hun_dao/gui_wu} GuScript flow that emits a black fog in
 * front of the caster and inflicts Slowness IV and Blindness on nearby hostiles.
 */
public final class GuiQiGuOrganBehavior extends AbstractGuzhenrenOrganBehavior
    implements OrganSlowTickListener, OrganOnHitListener {

  public static final GuiQiGuOrganBehavior INSTANCE = new GuiQiGuOrganBehavior();

  private static final String MOD_ID = "guzhenren";
  private static final String MODULE_NAME = "gui_qi_gu";
  private static final ResourceLocation ORGAN_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "guiqigu");
  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.fromNamespaceAndPath(MOD_ID, "gui_wu");

  private static final ResourceLocation GUI_WU_FLOW_ID = ChestCavity.id("hun_dao/gui_wu");

  // Phase 4: Use Tuning constants instead of hardcoded values
  private static final double PASSIVE_HUNPO_PER_SECOND =
      HunDaoTuning.GuiQiGu.PASSIVE_HUNPO_PER_SECOND;
  private static final double PASSIVE_JINGLI_PER_SECOND =
      HunDaoTuning.GuiQiGu.PASSIVE_JINGLI_PER_SECOND;
  private static final double TRUE_DAMAGE_RATIO = HunDaoTuning.GuiQiGu.TRUE_DAMAGE_RATIO;
  private static final double GUI_WU_RADIUS = HunDaoTuning.GuiQiGu.GUI_WU_RADIUS;
  private static final int GUI_WU_COOLDOWN_TICKS =
      BehaviorConfigAccess.getInt(GuiQiGuOrganBehavior.class, "GUI_WU_COOLDOWN_TICKS", 160);
  private static final int SOUL_SCAR_DURATION_TICKS =
      BehaviorConfigAccess.getInt(GuiQiGuOrganBehavior.class, "SOUL_SCAR_DURATION_TICKS", 160);

  private static final String STATE_ROOT_KEY = "GuiQiGu";
  private static final String KEY_COOLDOWN_UNTIL = "CooldownUntil";

  private static final ThreadLocal<Boolean> REENTRY_GUARD =
      ThreadLocal.withInitial(() -> Boolean.FALSE);

  static {
    OrganActivationListeners.register(ABILITY_ID, GuiQiGuOrganBehavior::activateAbility);
  }

  private GuiQiGuOrganBehavior() {}

  /**
   * Ensures that any necessary data linkages are established.
   *
   * <p>Currently a placeholder for future expansion.
   *
   * @param cc The chest cavity instance.
   */
  public void ensureAttached(ChestCavityInstance cc) {
    // No dedicated linkage channels needed yet; method kept for future expansion.
  }

  /**
   * Called when the organ is equipped.
   *
   * @param cc The chest cavity instance.
   * @param organ The organ being equipped.
   * @param staleRemovalContexts A list of stale removal contexts.
   */
  public void onEquip(
      ChestCavityInstance cc, ItemStack organ, List<OrganRemovalContext> staleRemovalContexts) {
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

    // Use runtime context for all resource operations (Phase 3)
    HunDaoRuntimeContext runtimeContext = HunDaoBehaviorContextHelper.getContext(player);

    int stackCount = Math.max(1, organ.getCount());
    double hunpoGain = PASSIVE_HUNPO_PER_SECOND * stackCount;
    double jingliGain = PASSIVE_JINGLI_PER_SECOND * stackCount;
    runtimeContext.getResourceOps().adjustDouble(player, "hunpo", hunpoGain, true, "zuida_hunpo");
    runtimeContext
        .getResourceOps()
        .adjustDouble(player, "jingli", jingliGain, true, "zuida_jingli");
  }

  @Override
  public float onHit(
      DamageSource source,
      LivingEntity attacker,
      LivingEntity target,
      ChestCavityInstance cc,
      ItemStack organ,
      float damage) {
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

    // Use runtime context for reading max hunpo (Phase 3)
    HunDaoRuntimeContext runtimeContext = HunDaoBehaviorContextHelper.getContext(player);
    double maxHunpo = runtimeContext.getResourceOps().readMaxHunpo(player);
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
      ReactionTagOps.add(target, ReactionTagKeys.SOUL_SCAR, SOUL_SCAR_DURATION_TICKS);
    } finally {
      REENTRY_GUARD.set(Boolean.FALSE);
    }
    return damage;
  }

  /**
   * Checks if the given chest cavity contains a Gui Qi Gu.
   *
   * @param cc The chest cavity instance to check.
   * @return {@code true} if the chest cavity contains a Gui Qi Gu, {@code false} otherwise.
   */
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
    controller.start(
        programOpt.get(), player, 1.0D, params, server.getGameTime(), "hun_dao.gui_wu");

    long readyAt = currentTick + GUI_WU_COOLDOWN_TICKS;
    OrganStateOps.setLong(
        state, cc, organ, KEY_COOLDOWN_UNTIL, readyAt, value -> Math.max(0L, value), 0L);
    ActiveSkillRegistry.scheduleReadyToast(serverPlayer, ABILITY_ID, readyAt, currentTick);
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
    private BuiltInOrganIds() {}

    private static boolean matches(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
        return false;
      }
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      return Objects.equals(id, ORGAN_ID);
    }
  }
}
