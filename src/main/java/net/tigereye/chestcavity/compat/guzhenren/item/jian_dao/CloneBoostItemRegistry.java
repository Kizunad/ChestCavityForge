package net.tigereye.chestcavity.compat.guzhenren.item.jian_dao;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 分身增益物品注册表
 *
 * <p>管理可放入分身增益槽位的物品及其效果。
 *
 * <p><strong>使用示例:</strong>
 * <pre>{@code
 * // 注册增益物品及其效果
 * CloneBoostItemRegistry.register(Items.DIAMOND, (clone, stack) -> {
 *     // 增加20%攻击力
 *     var attr = clone.getAttribute(Attributes.ATTACK_DAMAGE);
 *     if (attr != null) {
 *         attr.addPermanentModifier(new AttributeModifier(
 *             DIAMOND_BOOST_UUID, "diamond_boost", 0.2, Operation.MULTIPLY_BASE));
 *     }
 * });
 * }</pre>
 *
 * @see net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.ui.CloneInventoryMenu.BoostSlot
 * @see ICloneBoostEffect
 */
public final class CloneBoostItemRegistry {

    /**
     * 物品到增益效果的映射表
     */
    private static final Map<Item, ICloneBoostEffect> BOOST_EFFECTS = new HashMap<>();

    // ============ 预定义ResourceLocation（用于属性修改器） ============
    public static final ResourceLocation DIAMOND_BOOST_ATTACK =
        ResourceLocation.fromNamespaceAndPath("chestcavity", "clone_boost_diamond_attack");

    public static final ResourceLocation WITHER_SKULL_BOOST_HEALTH =
        ResourceLocation.fromNamespaceAndPath("chestcavity", "clone_boost_wither_skull_health");
    public static final ResourceLocation WITHER_SKULL_BOOST_ATTACK =
        ResourceLocation.fromNamespaceAndPath("chestcavity", "clone_boost_wither_skull_attack");

    public static final ResourceLocation NETHER_STAR_BOOST_HEALTH =
        ResourceLocation.fromNamespaceAndPath("chestcavity", "clone_boost_nether_star_health");
    public static final ResourceLocation NETHER_STAR_BOOST_ATTACK =
        ResourceLocation.fromNamespaceAndPath("chestcavity", "clone_boost_nether_star_attack");
    public static final ResourceLocation NETHER_STAR_BOOST_SPEED =
        ResourceLocation.fromNamespaceAndPath("chestcavity", "clone_boost_nether_star_speed");

    private CloneBoostItemRegistry() {
        // 工具类，禁止实例化
    }

    /**
     * 注册增益物品及其效果
     *
     * @param item 要注册的物品
     * @param effect 增益效果（null表示无效果，仅占位）
     */
    public static void register(Item item, @Nullable ICloneBoostEffect effect) {
        if (effect != null) {
            BOOST_EFFECTS.put(item, effect);
        } else {
            BOOST_EFFECTS.put(item, (clone, stack) -> {}); // 空效果
        }
    }

    /**
     * 注册增益物品（无效果，仅占位）
     *
     * @param item 要注册的物品
     */
    public static void register(Item item) {
        register(item, null);
    }

    /**
     * 检查物品是否为增益物品
     *
     * @param item 要检查的物品
     * @return 如果该物品可放入增益槽位则返回 true
     */
    public static boolean isBoostItem(Item item) {
        return BOOST_EFFECTS.containsKey(item);
    }

    /**
     * 获取物品的增益效果
     *
     * @param item 要查询的物品
     * @return 增益效果，如果未注册则返回 {@link Optional#empty()}
     */
    public static Optional<ICloneBoostEffect> getBoostEffect(Item item) {
        return Optional.ofNullable(BOOST_EFFECTS.get(item));
    }

    /**
     * 获取所有已注册的增益物品
     *
     * @return 增益物品集合（不可修改）
     */
    public static Set<Item> getRegisteredItems() {
        return Set.copyOf(BOOST_EFFECTS.keySet());
    }

    /**
     * 清空所有注册的增益物品
     * <p><strong>警告:</strong> 仅用于测试或重新加载，生产环境不应调用
     */
    public static void clear() {
        BOOST_EFFECTS.clear();
    }

    /**
     * 注册默认增益物品
     * <p>包含示例物品：钻石、凋零骷髅头、下界之星
     */
    public static void registerDefaults() {
        // ========== 钻石：+20% 攻击力 ==========
        register(Items.DIAMOND, new ICloneBoostEffect() {
            @Override
            public void apply(net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.PersistentGuCultivatorClone clone, ItemStack stack) {
                var attr = clone.getAttribute(Attributes.ATTACK_DAMAGE);
                if (attr != null && !hasModifier(attr, DIAMOND_BOOST_ATTACK)) {
                    attr.addPermanentModifier(new AttributeModifier(
                            DIAMOND_BOOST_ATTACK,
                            0.2,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    ));
                }
            }

            @Override
            public void remove(net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.PersistentGuCultivatorClone clone, ItemStack stack) {
                var attr = clone.getAttribute(Attributes.ATTACK_DAMAGE);
                if (attr != null) {
                    attr.removeModifier(DIAMOND_BOOST_ATTACK);
                }
            }
        });

        // ========== 凋零骷髅头：+30% 生命值 + +10% 攻击力 ==========
        register(Items.WITHER_SKELETON_SKULL, new ICloneBoostEffect() {
            @Override
            public void apply(net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.PersistentGuCultivatorClone clone, ItemStack stack) {
                // +30% 生命值
                var healthAttr = clone.getAttribute(Attributes.MAX_HEALTH);
                if (healthAttr != null && !hasModifier(healthAttr, WITHER_SKULL_BOOST_HEALTH)) {
                    double oldMax = healthAttr.getBaseValue();
                    healthAttr.addPermanentModifier(new AttributeModifier(
                            WITHER_SKULL_BOOST_HEALTH,
                            0.3,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    ));
                    // 治疗到新的上限
                    double newMax = healthAttr.getValue();
                    clone.setHealth((float) Math.min(clone.getHealth() * (newMax / oldMax), newMax));
                }

                // +10% 攻击力
                var attackAttr = clone.getAttribute(Attributes.ATTACK_DAMAGE);
                if (attackAttr != null && !hasModifier(attackAttr, WITHER_SKULL_BOOST_ATTACK)) {
                    attackAttr.addPermanentModifier(new AttributeModifier(
                            WITHER_SKULL_BOOST_ATTACK,
                            0.1,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    ));
                }
            }

            @Override
            public void remove(net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.PersistentGuCultivatorClone clone, ItemStack stack) {
                var healthAttr = clone.getAttribute(Attributes.MAX_HEALTH);
                if (healthAttr != null) {
                    healthAttr.removeModifier(WITHER_SKULL_BOOST_HEALTH);
                }
                var attackAttr = clone.getAttribute(Attributes.ATTACK_DAMAGE);
                if (attackAttr != null) {
                    attackAttr.removeModifier(WITHER_SKULL_BOOST_ATTACK);
                }
            }
        });

        // ========== 下界之星：+50% 生命值 + +30% 攻击力 + +20% 移动速度 ==========
        register(Items.NETHER_STAR, new ICloneBoostEffect() {
            @Override
            public void apply(net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.PersistentGuCultivatorClone clone, ItemStack stack) {
                // +50% 生命值
                var healthAttr = clone.getAttribute(Attributes.MAX_HEALTH);
                if (healthAttr != null && !hasModifier(healthAttr, NETHER_STAR_BOOST_HEALTH)) {
                    double oldMax = healthAttr.getBaseValue();
                    healthAttr.addPermanentModifier(new AttributeModifier(
                            NETHER_STAR_BOOST_HEALTH,
                            0.5,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    ));
                    // 治疗到新的上限
                    double newMax = healthAttr.getValue();
                    clone.setHealth((float) Math.min(clone.getHealth() * (newMax / oldMax), newMax));
                }

                // +30% 攻击力
                var attackAttr = clone.getAttribute(Attributes.ATTACK_DAMAGE);
                if (attackAttr != null && !hasModifier(attackAttr, NETHER_STAR_BOOST_ATTACK)) {
                    attackAttr.addPermanentModifier(new AttributeModifier(
                            NETHER_STAR_BOOST_ATTACK,
                            0.3,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    ));
                }

                // +20% 移动速度
                var speedAttr = clone.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedAttr != null && !hasModifier(speedAttr, NETHER_STAR_BOOST_SPEED)) {
                    speedAttr.addPermanentModifier(new AttributeModifier(
                            NETHER_STAR_BOOST_SPEED,
                            0.2,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    ));
                }
            }

            @Override
            public void remove(net.tigereye.chestcavity.compat.guzhenren.item.jian_dao.entity.PersistentGuCultivatorClone clone, ItemStack stack) {
                var healthAttr = clone.getAttribute(Attributes.MAX_HEALTH);
                if (healthAttr != null) {
                    healthAttr.removeModifier(NETHER_STAR_BOOST_HEALTH);
                }
                var attackAttr = clone.getAttribute(Attributes.ATTACK_DAMAGE);
                if (attackAttr != null) {
                    attackAttr.removeModifier(NETHER_STAR_BOOST_ATTACK);
                }
                var speedAttr = clone.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedAttr != null) {
                    speedAttr.removeModifier(NETHER_STAR_BOOST_SPEED);
                }
            }
        });
    }

    /**
     * 检查属性是否已有指定ResourceLocation的修改器
     */
    private static boolean hasModifier(net.minecraft.world.entity.ai.attributes.AttributeInstance attr, ResourceLocation id) {
        return attr.getModifier(id) != null;
    }
}
