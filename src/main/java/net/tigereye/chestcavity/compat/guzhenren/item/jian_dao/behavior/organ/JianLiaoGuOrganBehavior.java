package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.organ;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tigereye.chestcavity.chestcavities.instance.ChestCavityInstance;
import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.active.JianLiaoGuActive;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.behavior.passive.JianLiaoGuPassive;
import net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.calculator.JianLiaoGuCalc;
import net.tigereye.chestcavity.compat.guzhenren.util.behavior.MultiCooldown;
import net.tigereye.chestcavity.listeners.OrganActivationListeners;
import net.tigereye.chestcavity.listeners.OrganSlowTickListener;

/**
 * 剑疗蛊行为入口：桥接主动技与被动tick。
 *
 * <p>技能概览（guzhenren:jian_xue_hu_ji）
 * <ul>
 *   <li>由 {@link net.tigereye.chestcavity.skill.ActiveSkillRegistry} 注册，客户端热键映射至
 *   {@value #ABILITY_ID}，调用 {@link #activateAbility(LivingEntity, ChestCavityInstance)}。</li>
 *   <li>被动段落：
 *       <ol>
 *         <li>{@code JianLiaoGuHeartbeat}: 每心跳（40t）按剑道道痕与最大生命恢复持有者体力。</li>
 *         <li>{@code JianLiaoGuSwordRepair}: 每 100t 扫描自有飞剑，健康飞剑以 5% 耐久为低耐久飞剑补血，单次上限 80%。</li>
 *       </ol>
 *   </li>
 *   <li>主动段落：
 *       <ol>
 *         <li>{@code JianLiaoGuActive}: 消耗 15% 生命值（保底 1HP），按损耗比例×剑道道痕效率为范围飞剑恢复耐久，并施加冷却。</li>
 *         <li>冷却、效率计算委托 {@code JianLiaoGuCalc} 与 {@code JianLiaoGuTuning} 常量。</li>
 *       </ol>
 *   </li>
 * </ul>
 */
public enum JianLiaoGuOrganBehavior implements OrganSlowTickListener {
  INSTANCE;

  public static final ResourceLocation ORGAN_ID =
      ResourceLocation.parse("guzhenren:jian_liao_gu");

  public static final ResourceLocation ABILITY_ID =
      ResourceLocation.parse("guzhenren:jian_xue_hu_ji");

  static {
    OrganActivationListeners.register(ABILITY_ID, JianLiaoGuOrganBehavior::activateAbility);
  }

  @Override
  public void onSlowTick(LivingEntity entity, ChestCavityInstance cc, ItemStack organ) {
    if (!(entity instanceof ServerPlayer player) || entity.level().isClientSide()) {
      return;
    }
    if (!matchesOrgan(organ)) {
      return;
    }

    OrganState state = OrganState.of(organ, JianLiaoGuState.ROOT);
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    long now = player.serverLevel().getGameTime();
    double swordScar = JianLiaoGuCalc.readSwordScar(player);
    JianLiaoGuPassive.tick(player, cc, organ, state, cooldown, now, swordScar);
  }

  private static void activateAbility(LivingEntity entity, ChestCavityInstance cc) {
    if (!(entity instanceof ServerPlayer player) || cc == null || entity.level().isClientSide()) {
      return;
    }

    Optional<ItemStack> organOpt = findOrgan(cc);
    if (organOpt.isEmpty()) {
      return;
    }

    ItemStack organ = organOpt.get();
    OrganState state = OrganState.of(organ, JianLiaoGuState.ROOT);
    MultiCooldown cooldown = MultiCooldown.builder(state).withSync(cc, organ).build();
    long now = player.serverLevel().getGameTime();
    double swordScar = JianLiaoGuCalc.readSwordScar(player);
    JianLiaoGuActive.activate(player, cc, organ, state, cooldown, now, swordScar);
  }

  private static boolean matchesOrgan(ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return false;
    }
    ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
    return ORGAN_ID.equals(id);
  }

  private static Optional<ItemStack> findOrgan(ChestCavityInstance cc) {
    if (cc == null || cc.inventory == null) {
      return Optional.empty();
    }
    for (int i = 0; i < cc.inventory.getContainerSize(); i++) {
      ItemStack s = cc.inventory.getItem(i);
      if (matchesOrgan(s)) {
        return Optional.of(s);
      }
    }
    return Optional.empty();
  }
}
